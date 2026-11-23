package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.PeriodoAcademico;
import ec.edu.praxis.infrastructure.repository.PeriodoAcademicoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PeriodoAcademicoService {

    @Inject
    private PeriodoAcademicoRepository repository;

    public List<PeriodoAcademico> listar() {
        return repository.listAll();
    }

    public PeriodoAcademico buscar(Long id) {
        PeriodoAcademico periodo = repository.findById(id);
        if (periodo == null) {
            throw ApiException.noEncontrado("Periodo académico no encontrado.");
        }
        return periodo;
    }

    @Transactional
    public PeriodoAcademico crear(PeriodoAcademico datos) {
        validarDatos(datos);
        validarDuplicados(datos.getCodigo(), null);
        datos.setActivo(false);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public PeriodoAcademico actualizar(PeriodoAcademico datos) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id del periodo es obligatorio.");
        }
        PeriodoAcademico periodo = repository.findById(datos.getId());
        if (periodo == null) {
            throw ApiException.noEncontrado("Periodo académico no encontrado.");
        }
        validarDatos(datos);
        validarDuplicados(datos.getCodigo(), periodo.getId());
        periodo.setCodigo(datos.getCodigo());
        periodo.setNombre(datos.getNombre());
        periodo.setFechaInicio(datos.getFechaInicio());
        periodo.setFechaFin(datos.getFechaFin());
        repository.flush();
        return periodo;
    }

    @Transactional
    public PeriodoAcademico activar(Long id) {
        PeriodoAcademico periodo = buscar(id);
        PeriodoAcademico activo = repository.buscarActivo();
        if (activo != null && !activo.getId().equals(id)) {
            throw ApiException.reglaNegocio("Ya existe otro periodo activo.");
        }
        periodo.setActivo(true);
        repository.flush();
        return periodo;
    }

    @Transactional
    public PeriodoAcademico desactivar(Long id) {
        PeriodoAcademico periodo = buscar(id);
        periodo.setActivo(false);
        repository.flush();
        return periodo;
    }

    static void validarDatos(PeriodoAcademico datos) {
        if (datos == null || datos.getCodigo() == null || datos.getCodigo().isBlank()
                || datos.getNombre() == null || datos.getNombre().isBlank()
                || datos.getFechaInicio() == null || datos.getFechaFin() == null) {
            throw ApiException.datosInvalidos("Código, nombre y fechas son obligatorios.");
        }
        if (datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw ApiException.reglaNegocio("La fecha de fin no puede ser anterior a la de inicio.");
        }
    }

    private void validarDuplicados(String codigo, Long idActual) {
        PeriodoAcademico porCodigo = repository.buscarPorCodigo(codigo);
        if (porCodigo != null && !porCodigo.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un periodo con ese código.");
        }
    }
}
