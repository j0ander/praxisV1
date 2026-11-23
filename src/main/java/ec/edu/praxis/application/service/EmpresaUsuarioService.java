package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.EmpresaUsuario;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Miembro;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Miembros de empresa: API-048 a API-050. */
@ApplicationScoped
public class EmpresaUsuarioService {

    private static final String ROL_REPRESENTANTE = "REPRESENTANTE_EMPRESA";

    @Inject
    EmpresaUsuarioRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    UsuarioRolRepository usuarioRolRepository;

    @Inject
    ContextoActorService contextoActor;

    public List<Miembro> listar(Long empresaId, Long actorId, boolean esInstitucional,
            boolean esRepresentante) {
        obtenerEmpresa(empresaId);
        verificarAmbito(empresaId, actorId, esInstitucional, esRepresentante, true);
        return repository.listarMiembrosPorEmpresa(empresaId);
    }

    @Transactional
    public Miembro crear(EmpresaUsuario datos, Long empresaId, Long actorId,
            boolean esInstitucional, boolean esRepresentante) {
        validarCamposInternos(datos, false);
        validarCreacion(datos);
        verificarAmbito(empresaId, actorId, esInstitucional, esRepresentante, true);

        Empresa empresa = obtenerEmpresa(empresaId);
        if (!Boolean.TRUE.equals(empresa.getActiva())) {
            throw ApiException.reglaNegocio("La empresa debe estar activa para vincular miembros.");
        }

        Usuario usuario = obtenerUsuario(datos.getUsuarioId());
        validarRolGlobal(usuario, ROL_REPRESENTANTE,
                "El usuario debe estar activo y tener el rol REPRESENTANTE_EMPRESA.");

        if (repository.existeDuplicado(empresaId, usuario.getId())) {
            throw ApiException.duplicado("El usuario ya es miembro de esta empresa.");
        }
        if (repository.contarActivos(empresaId, null) == 0
                && !EmpresaUsuario.ADMIN_EMPRESA.equals(datos.getRolEmpresa())) {
            throw ApiException.reglaNegocio("El primer miembro activo debe ser ADMIN_EMPRESA.");
        }

        contextoActor.fijarActor(actorId);

        datos.setEmpresaId(empresaId);
        datos.setActivo(Boolean.TRUE);
        repository.persistAndFlush(datos);
        return repository.buscarMiembroPorId(datos.getId());
    }

    @Transactional
    public Miembro actualizar(EmpresaUsuario datos, Long empresaId, Long id, Long actorId,
            boolean esInstitucional, boolean esRepresentante) {
        validarCamposInternos(datos, true);
        validarActualizacion(datos);
        verificarAmbito(empresaId, actorId, esInstitucional, esRepresentante, true);
        // La comprobación del último ADMIN y la escritura comparten el bloqueo de
        // la empresa: dos API-050 concurrentes no pueden dejarla sin administrador.
        Empresa empresa = empresaRepository.buscarParaBloqueo(empresaId);
        if (empresa == null) {
            throw ApiException.noEncontrado("Empresa no encontrada.");
        }

        EmpresaUsuario actual = repository.buscarPorIdYEmpresa(id, empresaId);
        if (actual == null) {
            throw ApiException.noEncontrado("Miembro no encontrado en la empresa.");
        }

        boolean dejaDeSerAdmin = Boolean.TRUE.equals(actual.getActivo())
                && EmpresaUsuario.ADMIN_EMPRESA.equals(actual.getRolEmpresa())
                && (!Boolean.TRUE.equals(datos.getActivo())
                        || !EmpresaUsuario.ADMIN_EMPRESA.equals(datos.getRolEmpresa()));
        if (dejaDeSerAdmin && repository.contarAdministradoresActivos(empresaId, id) == 0) {
            throw ApiException.reglaNegocio("La empresa no puede quedar sin un ADMIN_EMPRESA activo.");
        }
        if (Boolean.TRUE.equals(datos.getActivo())
                && !EmpresaUsuario.ADMIN_EMPRESA.equals(datos.getRolEmpresa())
                && repository.contarActivos(empresaId, id) == 0) {
            throw ApiException.reglaNegocio("El primer miembro activo debe ser ADMIN_EMPRESA.");
        }
        if (Boolean.TRUE.equals(datos.getActivo())
                && !Boolean.TRUE.equals(empresa.getActiva())) {
            throw ApiException.reglaNegocio("No se pueden mantener miembros activos en una empresa inactiva.");
        }

        contextoActor.fijarActor(actorId);

        actual.setRolEmpresa(datos.getRolEmpresa());
        actual.setActivo(datos.getActivo());
        repository.flush();
        return repository.buscarMiembroPorId(actual.getId());
    }

    private Empresa obtenerEmpresa(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId);
        if (empresa == null) {
            throw ApiException.noEncontrado("Empresa no encontrada.");
        }
        return empresa;
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId);
        if (usuario == null) {
            throw ApiException.noEncontrado("Usuario no encontrado.");
        }
        return usuario;
    }

    private void validarRolGlobal(Usuario usuario, String codigo, String mensaje) {
        if (!usuario.isActivo() || !usuarioRolRepository.listarCodigosPorUsuario(usuario.getId()).contains(codigo)) {
            throw ApiException.reglaNegocio(mensaje);
        }
    }

    private void verificarAmbito(Long empresaId, Long actorId, boolean esInstitucional,
            boolean esRepresentante, boolean requiereAdmin) {
        if (esInstitucional) {
            return;
        }
        if (esRepresentante && actorId != null) {
            String rol = repository.rolActivo(empresaId, actorId);
            if (rol != null && (!requiereAdmin || EmpresaUsuario.ADMIN_EMPRESA.equals(rol))) {
                return;
            }
        }
        throw ApiException.noEncontrado("Empresa no encontrada.");
    }

    /** El id y la empresa se toman de la ruta; el resto de campos gestionados no se admiten. */
    private static void validarCamposInternos(EmpresaUsuario datos, boolean esActualizacion) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del miembro son obligatorios.");
        }
        if (datos.getId() != null) {
            throw ApiException.datosInvalidos("El id se toma de la ruta.");
        }
        if (datos.getEmpresaId() != null) {
            throw ApiException.datosInvalidos("empresaId no se admite en la solicitud.");
        }
        if (esActualizacion && datos.getUsuarioId() != null) {
            throw ApiException.datosInvalidos("usuarioId no se admite al actualizar un miembro.");
        }
        if (datos.getCreatedAt() != null || datos.getUpdatedAt() != null) {
            throw ApiException.datosInvalidos("createdAt y updatedAt no se admiten en la solicitud.");
        }
    }

    private static void validarCreacion(EmpresaUsuario datos) {
        if (datos.getUsuarioId() == null) {
            throw ApiException.datosInvalidos("El usuario es obligatorio.");
        }
        if (!esRolValido(datos.getRolEmpresa())) {
            throw ApiException.datosInvalidos("El rol de empresa debe ser ADMIN_EMPRESA o PUBLICADOR.");
        }
        if (!Boolean.TRUE.equals(datos.getActivo())) {
            throw ApiException.datosInvalidos("Un miembro debe crearse activo.");
        }
    }

    private static void validarActualizacion(EmpresaUsuario datos) {
        if (!esRolValido(datos.getRolEmpresa())) {
            throw ApiException.datosInvalidos("El rol de empresa debe ser ADMIN_EMPRESA o PUBLICADOR.");
        }
        if (datos.getActivo() == null) {
            throw ApiException.datosInvalidos("El estado activo del miembro es obligatorio.");
        }
    }

    private static boolean esRolValido(String rol) {
        return EmpresaUsuario.ADMIN_EMPRESA.equals(rol) || EmpresaUsuario.PUBLICADOR.equals(rol);
    }
}
