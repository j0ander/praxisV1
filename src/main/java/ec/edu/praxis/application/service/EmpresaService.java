package ec.edu.praxis.application.service;

import java.util.List;
import java.util.regex.Pattern;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class EmpresaService {

    private static final Pattern RUC_PATTERN = Pattern.compile("^\\d{13}$");

    @Inject
    EmpresaRepository repository;

    public List<Empresa> listar(String texto) {
        return repository.listarPorTexto(texto);
    }

    public Empresa buscar(Long id) {
        return obtener(id);
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
    public Empresa actualizar(Empresa datos) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id de la empresa es obligatorio.");
        }
        validarCamposInternos(datos, true);
        Empresa actual = obtener(datos.getId());
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
