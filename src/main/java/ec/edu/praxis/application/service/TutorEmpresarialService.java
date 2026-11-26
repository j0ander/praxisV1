package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.TutorEmpresarial;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import ec.edu.praxis.infrastructure.repository.TutorEmpresarialRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class TutorEmpresarialService {

    private static final String ROL_TUTOR = "TUTOR_EMPRESARIAL";

    @Inject
    TutorEmpresarialRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    UsuarioRolRepository usuarioRolRepository;

    public List<TutorEmpresarial> listar(Long empresaId) {
        obtenerEmpresa(empresaId);
        return repository.listarPorEmpresa(empresaId);
    }

    @Transactional
    public TutorEmpresarial crear(TutorEmpresarial datos, Long empresaId) {
        validarCamposInternos(datos, false);
        if (datos.getUsuarioId() == null) {
            throw ApiException.datosInvalidos("El usuario es obligatorio.");
        }
        validarCargo(datos.getCargo());
        obtenerEmpresa(empresaId);

        Usuario usuario = usuarioRepository.findById(datos.getUsuarioId());
        if (usuario == null) {
            throw ApiException.noEncontrado("Usuario no encontrado.");
        }
        if (!usuario.isActivo()
                || !usuarioRolRepository.listarCodigosPorUsuario(usuario.getId()).contains(ROL_TUTOR)) {
            throw ApiException.reglaNegocio(
        "El usuario debe estar activo y tener el rol TUTOR_EMPRESARIAL.");
        }
        if (repository.existePorUsuario(usuario.getId())) {
            throw ApiException.duplicado("El usuario ya es tutor de otra empresa.");
        }

        datos.setEmpresaId(empresaId);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public TutorEmpresarial actualizar(TutorEmpresarial datos, Long empresaId) {
        validarCamposInternos(datos, true);
        if (datos.getId() == null) {
            throw ApiException.datosInvalidos("El id del tutor es obligatorio.");
        }
        validarCargo(datos.getCargo());

        TutorEmpresarial actual = repository.buscarPorIdYEmpresa(datos.getId(), empresaId);
        if (actual == null) {
            throw ApiException.noEncontrado("Tutor no encontrado en la empresa.");
        }
        actual.setCargo(datos.getCargo());
        repository.flush();
        return actual;
    }

    private Empresa obtenerEmpresa(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId);
        if (empresa == null) {
            throw ApiException.noEncontrado("Empresa no encontrada.");
        }
        return empresa;
    }

    private static void validarCamposInternos(TutorEmpresarial datos, boolean permiteId) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del tutor son obligatorios.");
        }
        if (!permiteId && datos.getId() != null) {
            throw ApiException.datosInvalidos("El id no se admite al crear un tutor.");
        }
        if (datos.getEmpresaId() != null) {
            throw ApiException.datosInvalidos("empresaId no se admite en la solicitud.");
        }
        if (datos.getUsuarioId() != null && permiteId) {
            throw ApiException.datosInvalidos("usuarioId no se admite al actualizar un tutor.");
        }
        if (datos.getCreatedAt() != null || datos.getUpdatedAt() != null) {
            throw ApiException.datosInvalidos("createdAt y updatedAt no se admiten en la solicitud.");
        }
    }

    private static void validarCargo(String cargo) {
        if (cargo != null && cargo.length() > 100) {
            throw ApiException.datosInvalidos("El cargo no puede superar los 100 caracteres.");
        }
    }
}
