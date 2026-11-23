package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Facultad;
import ec.edu.praxis.infrastructure.repository.FacultadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FacultadService {

    @Inject
    private FacultadRepository repository;

    public List<Facultad> listar() {
        return repository.listAll();
    }

    public Facultad buscar(Long id) {
        Facultad facultad = repository.findById(id);
        if (facultad == null) {
            throw ApiException.noEncontrado("Facultad no encontrada.");
        }
        return facultad;
    }

    @Transactional
    public Facultad crear(Facultad datos) {
        validarDatos(datos);
        validarDuplicados(datos.getCodigo(), datos.getNombre(), null);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Facultad actualizar(Facultad datos) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id de la facultad es obligatorio.");
        }
        Facultad facultad = repository.findById(datos.getId());
        if (facultad == null) {
            throw ApiException.noEncontrado("Facultad no encontrada.");
        }
        validarDatos(datos);
        validarDuplicados(datos.getCodigo(), datos.getNombre(), facultad.getId());
        facultad.setCodigo(datos.getCodigo());
        facultad.setNombre(datos.getNombre());
        facultad.setActiva(datos.isActiva());
        repository.flush();
        return facultad;
    }

    @Transactional
    public void eliminar(Long id) {
        if (repository.findById(id) == null) {
            throw ApiException.noEncontrado("Facultad no encontrada.");
        }
        repository.deleteById(id);
    }

    private void validarDatos(Facultad datos) {
        if (datos == null || datos.getCodigo() == null || datos.getNombre() == null) {
            throw ApiException.datosInvalidos("Código y nombre son obligatorios.");
        }
    }

    private void validarDuplicados(String codigo, String nombre, Long idActual) {
        Facultad porCodigo = repository.buscarPorCodigo(codigo);
        if (porCodigo != null && !porCodigo.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe una facultad con ese código.");
        }
        Facultad porNombre = repository.buscarPorNombre(nombre);
        if (porNombre != null && !porNombre.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe una facultad con ese nombre.");
        }
    }
}
