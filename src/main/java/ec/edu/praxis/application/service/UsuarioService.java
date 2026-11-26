package ec.edu.praxis.application.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ec.edu.praxis.domain.model.Rol;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.domain.model.UsuarioCarrera;
import ec.edu.praxis.domain.model.UsuarioRol;
import ec.edu.praxis.infrastructure.repository.CarreraRepository;
import ec.edu.praxis.infrastructure.repository.RolRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioCarreraRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class UsuarioService {

    private static final Pattern CORREO = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    private static final Pattern CEDULA = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PASAPORTE = Pattern.compile("^[A-Z0-9]{5,20}$");
    private static final Set<String> TIPOS_DOCUMENTO = Set.of("CEDULA", "PASAPORTE");
    private static final String ROL_COORDINADOR = "COORDINADOR";

    @Inject
    UsuarioRepository repository;

    @Inject
    RolRepository rolRepository;

    @Inject
    UsuarioRolRepository usuarioRolRepository;

    @Inject
    CarreraRepository carreraRepository;

    @Inject
    UsuarioCarreraRepository usuarioCarreraRepository;

    @Inject
    DocenteService docenteService;

    public List<Usuario> listar(String rol, Boolean activo, String texto) {
        return repository.listarConFiltros(rol, activo, texto);
    }

    public Usuario buscar(Long id) {
        return buscarUsuario(id);
    }

    public List<String> rolesDe(Long usuarioId) {
        return usuarioRolRepository.listarCodigosPorUsuario(usuarioId);
    }

    public Usuario autenticar(String correo, String password) {
        String normalizado = correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = normalizado == null ? null : repository.buscarPorCorreo(normalizado);
        if (usuario == null || !usuario.isActivo()
                || password == null || password.isBlank()
                || !BcryptUtil.matches(password, usuario.getHashPassword())) {
            throw ApiException.credencialesInvalidas();
        }
        return usuario;
    }

    public List<Long> carrerasDe(Long usuarioId) {
        return usuarioCarreraRepository.listarCarrerasPorUsuario(usuarioId);
    }

    @Transactional
    public Usuario crear(Usuario datos, String passwordTemporal) {
        validarDatos(datos);
        validarPasswordTemporal(passwordTemporal);
        String correo = validarCorreo(datos.getCorreo());
        validarDuplicados(correo, datos.getNumeroDocumento(), null);

        datos.setCorreo(correo);
        datos.setHashPassword(BcryptUtil.bcryptHash(passwordTemporal));
        datos.setActivo(true);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Usuario actualizar(Long id, Usuario datos) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id del usuario es obligatorio.");
        }
        Usuario usuario = buscarUsuario(id);
        validarDatos(datos);
        String correo = validarCorreo(datos.getCorreo());
        validarDuplicados(correo, datos.getNumeroDocumento(), usuario.getId());

        usuario.setCorreo(correo);
        usuario.setNombres(datos.getNombres());
        usuario.setApellidos(datos.getApellidos());
        usuario.setTipoDocumento(datos.getTipoDocumento());
        usuario.setNumeroDocumento(datos.getNumeroDocumento());
        usuario.setTelefono(datos.getTelefono());
        usuario.setAlcanceGlobal(datos.isAlcanceGlobal());
        repository.flush();
        return usuario;
    }

    @Transactional
    public void activar(Long id) {
        buscarUsuario(id).setActivo(true);
        repository.flush();
    }

    @Transactional
    public void desactivar(Long id) {
        buscarUsuario(id).setActivo(false);
        repository.flush();
    }

    @Transactional
    public void restablecerPassword(Long id, String passwordTemporal) {
        validarPasswordTemporal(passwordTemporal);
        Usuario usuario = buscarUsuario(id);
        usuario.setHashPassword(BcryptUtil.bcryptHash(passwordTemporal));
        repository.flush();
    }

    @Transactional
    public void asignarRoles(Long id, List<String> codigos) {
        buscarUsuario(id);
        Map<String, Rol> roles = new LinkedHashMap<>();
        for (String codigo : codigos == null ? List.<String>of() : codigos) {
            if (codigo == null || codigo.isBlank()) {
                throw ApiException.datosInvalidos("Los códigos de rol son obligatorios.");
            }
            String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
            if (!roles.containsKey(normalizado)) {
                Rol rol = rolRepository.buscarPorCodigo(normalizado);
                if (rol == null) {
        throw ApiException.reglaNegocio("El rol no existe.");
                }
                roles.put(normalizado, rol);
            }
        }

        if (roles.containsKey(ROL_COORDINADOR)
                && usuarioCarreraRepository.listarCarrerasPorUsuario(id).size() > 1) {
            throw ApiException.reglaNegocio(
                    "Un coordinador solo puede tener una carrera asignada. Ajuste las carreras antes de asignar el rol.");
        }

        for (UsuarioRol relacion : usuarioRolRepository.listarPorUsuario(id)) {
            usuarioRolRepository.delete(relacion);
        }
        usuarioRolRepository.flush();
        for (Rol rol : roles.values()) {
            usuarioRolRepository.persist(new UsuarioRol(id, rol.getId()));
        }
        usuarioRolRepository.flush();

        if (roles.containsKey("DOCENTE")) {
            docenteService.crearPerfilPorRol(id);
        }
    }

    @Transactional
    public void asignarCarreras(Long id, List<Long> carreraIds) {
        buscarUsuario(id);
        Set<Long> carreras = new LinkedHashSet<>();
        for (Long carreraId : carreraIds == null ? List.<Long>of() : carreraIds) {
            if (carreraId == null) {
                throw ApiException.datosInvalidos("Los identificadores de carrera son obligatorios.");
            }
            if (carreraRepository.findById(carreraId) == null) {
                throw ApiException.reglaNegocio("La carrera no existe.");
            }
            carreras.add(carreraId);
        }
        if (usuarioRolRepository.listarCodigosPorUsuario(id).contains(ROL_COORDINADOR)
                && carreras.size() > 1) {
            throw ApiException.reglaNegocio("Un coordinador solo puede tener una carrera asignada.");
        }

        for (UsuarioCarrera relacion : usuarioCarreraRepository.listarPorUsuario(id)) {
            usuarioCarreraRepository.delete(relacion);
        }
        usuarioCarreraRepository.flush();
        for (Long carreraId : carreras) {
            usuarioCarreraRepository.persist(new UsuarioCarrera(id, carreraId));
        }
        usuarioCarreraRepository.flush();
    }

    @Transactional
    public void asignarCarreraCoordinador(Long id, Long carreraId) {
        buscarUsuario(id);
        if (!usuarioRolRepository.listarCodigosPorUsuario(id).contains(ROL_COORDINADOR)) {
            throw ApiException.reglaNegocio(
                    "El usuario debe tener el rol COORDINADOR para asignar una carrera de coordinación.");
        }
        if (carreraId == null) {
            throw ApiException.datosInvalidos("La carrera del coordinador es obligatoria.");
        }
        if (carreraRepository.findById(carreraId) == null) {
            throw ApiException.reglaNegocio("La carrera no existe.");
        }

        for (UsuarioCarrera relacion : usuarioCarreraRepository.listarPorUsuario(id)) {
            usuarioCarreraRepository.delete(relacion);
        }
        usuarioCarreraRepository.flush();
        usuarioCarreraRepository.persist(new UsuarioCarrera(id, carreraId));
        usuarioCarreraRepository.flush();
    }

    public Long carreraDeCoordinador(Long id) {
        if (id == null) {
            throw ApiException.datosInvalidos("El usuario del coordinador es obligatorio.");
        }
        if (!usuarioRolRepository.listarCodigosPorUsuario(id).contains(ROL_COORDINADOR)) {
            throw ApiException.reglaNegocio("El usuario no tiene el rol COORDINADOR.");
        }
        List<Long> carreras = usuarioCarreraRepository.listarCarrerasPorUsuario(id);
        if (carreras.isEmpty()) {
            throw ApiException.reglaNegocio("El coordinador no tiene una carrera asignada.");
        }
        if (carreras.size() > 1) {
            throw ApiException.reglaNegocio("El coordinador tiene más de una carrera asignada.");
        }
        return carreras.get(0);
    }

    private Usuario buscarUsuario(Long id) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id del usuario es obligatorio.");
        }
        Usuario usuario = repository.findById(id);
        if (usuario == null) {
            throw ApiException.noEncontrado("Usuario no encontrado.");
        }
        return usuario;
    }

    private void validarDuplicados(String correo, String numeroDocumento, Long idActual) {
        Usuario porCorreo = repository.buscarPorCorreo(correo);
        if (porCorreo != null && !porCorreo.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un usuario con ese correo.");
        }
        if (numeroDocumento == null) {
            return;
        }
        Usuario porDocumento = repository.buscarPorDocumento(numeroDocumento);
        if (porDocumento != null && !porDocumento.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un usuario con ese documento.");
        }
    }

    static void validarPasswordTemporal(String password) {
        if (password == null || password.isBlank()) {
            throw ApiException.datosInvalidos("La contraseña temporal es obligatoria.");
        }
    }

    static String validarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw ApiException.datosInvalidos("El correo es obligatorio.");
        }
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 120 || !CORREO.matcher(normalizado).matches()) {
            throw ApiException.datosInvalidos("El correo no es válido.");
        }
        return normalizado;
    }

    static void validarDatos(Usuario datos) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del usuario son obligatorios.");
        }
        validarTexto(datos.getNombres(), 100, "Los nombres son obligatorios.", "Los nombres son demasiado largos.");
        validarTexto(datos.getApellidos(), 100, "Los apellidos son obligatorios.",
                "Los apellidos son demasiado largos.");
        if (datos.getTelefono() != null && datos.getTelefono().length() > 20) {
            throw ApiException.datosInvalidos("El teléfono es demasiado largo.");
        }
        String tipo = datos.getTipoDocumento();
        if (!TIPOS_DOCUMENTO.contains(tipo)) {
            throw ApiException.datosInvalidos("El tipo de documento no es válido.");
        }
        String documento = datos.getNumeroDocumento();
        if (documento == null) {
            return;
        }
        boolean valido = switch (tipo) {
            case "CEDULA" -> CEDULA.matcher(documento).matches();
            case "PASAPORTE" -> PASAPORTE.matcher(documento).matches();
            default -> false;
        };
        if (!valido) {
            throw ApiException.datosInvalidos("El número de documento no es válido.");
        }
    }

    private static void validarTexto(String valor, int longitud, String mensajeVacio, String mensajeLargo) {
        if (valor == null || valor.isBlank()) {
            throw ApiException.datosInvalidos(mensajeVacio);
        }
        if (valor.length() > longitud) {
            throw ApiException.datosInvalidos(mensajeLargo);
        }
    }
}
