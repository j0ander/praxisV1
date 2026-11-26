package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Docente;
import ec.edu.praxis.infrastructure.repository.DocenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class DocenteService {

    private static final int LONGITUD_CODIGO = 20;
    private static final int LONGITUD_DEDICACION = 50;

    @Inject
    DocenteRepository repository;

    public List<Docente> listar() {
        return repository.listAll();
    }

    public void crearPerfilPorRol(Long usuarioId) {
        if (usuarioId == null) {
            throw ApiException.datosInvalidos("El usuario es obligatorio.");
        }
        if (repository.buscarPorUsuarioId(usuarioId) != null) {
            return;
        }
        Docente docente = new Docente();
        docente.setUsuarioId(usuarioId);
        repository.persistAndFlush(docente);
    }

    @Transactional
    public Docente actualizar(Long id, Docente datos) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id del perfil de docente es obligatorio.");
        }
        Docente docente = repository.findById(id);
        if (docente == null) {
            throw ApiException.noEncontrado("Perfil de docente no encontrado.");
        }
        validarDatos(datos);

        String codigo = datos.getCodigoDocente();
        if (codigo != null) {
            Docente porCodigo = repository.buscarPorCodigoDocente(codigo);
            if (porCodigo != null && !porCodigo.getId().equals(docente.getId())) {
                throw ApiException.duplicado("Ya existe un docente con ese código.");
            }
        }

        docente.setCodigoDocente(codigo);
        docente.setDedicacion(datos.getDedicacion());
        repository.flush();
        return docente;
    }

    static void validarDatos(Docente datos) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del perfil son obligatorios.");
        }
        String codigo = datos.getCodigoDocente();
        if (codigo != null && (codigo.isBlank() || codigo.length() > LONGITUD_CODIGO)) {
            throw ApiException.datosInvalidos("El código de docente no es válido.");
        }
        String dedicacion = datos.getDedicacion();
        if (dedicacion != null && dedicacion.length() > LONGITUD_DEDICACION) {
            throw ApiException.datosInvalidos("La dedicación no puede superar 50 caracteres.");
        }
    }
}
