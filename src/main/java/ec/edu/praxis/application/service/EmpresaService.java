package ec.edu.praxis.application.service;

import java.util.List;
import java.util.regex.Pattern;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Administracion;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Empresas: API-039 a API-042 y API-086. */
@ApplicationScoped
public class EmpresaService {

    private static final Pattern RUC_PATTERN = Pattern.compile("^\\d{13}$");
    private static final String ROL_REPRESENTANTE = "REPRESENTANTE_EMPRESA";

    @Inject
    EmpresaRepository repository;

    @Inject
    EmpresaUsuarioRepository empresaUsuarioRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    UsuarioRolRepository usuarioRolRepository;

    public List<Empresa> listar(String texto) {
        return repository.listarPorTexto(texto);
    }

    public Empresa buscar(Long id, Long actorId, boolean esInstitucional, boolean esRepresentante) {
        Empresa empresa = obtener(id);
        verificarAmbito(empresa.getId(), actorId, esInstitucional, esRepresentante, false);
        return empresa;
    }

    /** API-086: empresa del representante con membresía activa ADMIN_EMPRESA. */
    public Empresa miEmpresa(Long usuarioId) {
        if (usuarioId == null) {
            throw ApiException.sinSesion("Se requiere sesión para consultar la empresa.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId);
        if (usuario == null || !usuario.isActivo()
                || !usuarioRolRepository.listarCodigosPorUsuario(usuarioId).contains(ROL_REPRESENTANTE)) {
            throw ApiException.sinPermiso("El usuario no está autorizado para consultar su empresa.");
        }

        List<Administracion> administraciones = empresaUsuarioRepository.listarAdministracionesActivas(usuarioId);
        if (administraciones.isEmpty()) {
            throw ApiException.noEncontrado("No se encontró una empresa asociada con permisos ADMIN_EMPRESA.");
        }
        if (administraciones.size() > 1) {
            throw ApiException.reglaNegocio("Existe más de una empresa asociada con permisos ADMIN_EMPRESA.");
        }

        Empresa empresa = repository.findById(administraciones.get(0).getEmpresaId());
        if (empresa == null) {
            throw ApiException.noEncontrado("No se encontró una empresa asociada con permisos ADMIN_EMPRESA.");
        }
        return empresa;
    }

    @Transactional
    public Empresa crear(Empresa datos) {
        validarCreacion(datos);
        validarCamposInternos(datos, false);
        validarRuc(datos.getRuc());
        validarRucDuplicado(datos.getRuc(), null);
        datos.setActiva(Boolean.TRUE);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Empresa actualizar(Empresa datos, Long actorId, boolean esInstitucional, boolean esRepresentante) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id de la empresa es obligatorio.");
        }
        validarCamposInternos(datos, true);
        Empresa actual = obtener(datos.getId());
        verificarAmbito(actual.getId(), actorId, esInstitucional, esRepresentante, true);
        validarRuc(datos.getRuc());
        validarActualizacion(datos);
        validarRucDuplicado(datos.getRuc(), actual.getId());

        actual.setRuc(datos.getRuc());
        actual.setRazonSocial(datos.getRazonSocial());
        actual.setNombreComercial(datos.getNombreComercial());
        actual.setDireccion(datos.getDireccion());
        actual.setTelefono(datos.getTelefono());
        actual.setCorreo(datos.getCorreo());
        actual.setActiva(datos.getActiva());
        repository.flush();
        return actual;
    }

    private Empresa obtener(Long id) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id de la empresa es obligatorio.");
        }
        Empresa empresa = repository.findById(id);
        if (empresa == null) {
            throw ApiException.noEncontrado("Empresa no encontrada.");
        }
        return empresa;
    }

    private void verificarAmbito(Long empresaId, Long actorId, boolean esInstitucional,
            boolean esRepresentante, boolean requiereAdmin) {
        if (esInstitucional) {
            return;
        }
        if (esRepresentante) {
            boolean autorizado = requiereAdmin
                    ? repository.esAdministradorDeUsuario(empresaId, actorId)
                    : repository.perteneceAUsuario(empresaId, actorId);
            if (autorizado) {
                return;
            }
        }
        throw ApiException.noEncontrado("Empresa no encontrada.");
    }

    static void validarRuc(String ruc) {
        if (ruc == null || ruc.isBlank()) {
            throw ApiException.datosInvalidos("El RUC es obligatorio.");
        }
        if (!RUC_PATTERN.matcher(ruc).matches()) {
            throw ApiException.datosInvalidos("El RUC debe tener exactamente 13 dígitos.");
        }
    }

    private void validarRucDuplicado(String ruc, Long idActual) {
        Empresa porRuc = repository.buscarPorRuc(ruc);
        if (porRuc != null && !porRuc.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe una empresa con ese RUC.");
        }
    }

    private static void validarCamposInternos(Empresa datos, boolean permiteId) {
        if (!permiteId && datos.getId() != null) {
            throw ApiException.datosInvalidos("El id no se admite al crear una empresa.");
        }
        if (datos.getCreatedAt() != null || datos.getUpdatedAt() != null) {
            throw ApiException.datosInvalidos("createdAt y updatedAt no se admiten en la solicitud.");
        }
    }

    private static void validarCreacion(Empresa datos) {
        if (datos == null || datos.getRazonSocial() == null || datos.getRazonSocial().isBlank()) {
            throw ApiException.datosInvalidos("La razón social es obligatoria.");
        }
        if (datos.getActiva() != null) {
            throw ApiException.datosInvalidos("El campo activa no se admite al crear una empresa.");
        }
    }

    private static void validarActualizacion(Empresa datos) {
        if (datos.getRazonSocial() == null || datos.getRazonSocial().isBlank()) {
            throw ApiException.datosInvalidos("La razón social es obligatoria.");
        }
        if (datos.getActiva() == null) {
            throw ApiException.datosInvalidos("El estado activo de la empresa es obligatorio.");
        }
    }
}
