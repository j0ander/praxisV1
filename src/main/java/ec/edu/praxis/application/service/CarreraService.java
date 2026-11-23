package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Carrera;
import ec.edu.praxis.infrastructure.repository.CarreraRepository;
import ec.edu.praxis.infrastructure.repository.FacultadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CarreraService {

    @Inject
    private CarreraRepository repository;

    @Inject
    private FacultadRepository facultadRepository;

    public List<Carrera> listar() {
        return repository.listAll();
    }

    public List<Carrera> listarPorFacultad(Long facultadId) {
        return repository.listarPorFacultad(facultadId);
    }

    public Carrera buscar(Long id) {
        Carrera carrera = repository.findById(id);
        if (carrera == null) {
            throw ApiException.noEncontrado("Carrera no encontrada.");
        }
        return carrera;
    }

    @Transactional
    public Carrera crear(Carrera datos) {
        validarDatos(datos);
        validarFacultad(datos.getFacultadId());
        validarDuplicados(datos.getCodigo(), datos.getFacultadId(), datos.getNombre(), null);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Carrera actualizar(Carrera datos) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id de la carrera es obligatorio.");
        }
        Carrera carrera = repository.findById(datos.getId());
        if (carrera == null) {
            throw ApiException.noEncontrado("Carrera no encontrada.");
        }
        validarDatos(datos);
        validarFacultad(datos.getFacultadId());
        validarDuplicados(datos.getCodigo(), datos.getFacultadId(), datos.getNombre(), carrera.getId());
        carrera.setFacultadId(datos.getFacultadId());
        carrera.setCodigo(datos.getCodigo());
        carrera.setNombre(datos.getNombre());
        carrera.setActiva(datos.isActiva());
        repository.flush();
        return carrera;
    }

    static void validarDatos(Carrera datos) {
        if (datos == null || datos.getFacultadId() == null
                || datos.getCodigo() == null || datos.getCodigo().isBlank()
                || datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw ApiException.datosInvalidos("Facultad, código y nombre son obligatorios.");
        }
    }

    private void validarFacultad(Long facultadId) {
        if (facultadRepository.findById(facultadId) == null) {
            throw ApiException.reglaNegocio("La facultad no existe.");
        }
    }

    private void validarDuplicados(String codigo, Long facultadId, String nombre, Long idActual) {
        Carrera porCodigo = repository.buscarPorCodigo(codigo);
        if (porCodigo != null && !porCodigo.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe una carrera con ese código.");
        }
        Carrera porNombre = repository.buscarPorNombreEnFacultad(facultadId, nombre);
        if (porNombre != null && !porNombre.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe una carrera con ese nombre en la facultad.");
        }
    }
}
