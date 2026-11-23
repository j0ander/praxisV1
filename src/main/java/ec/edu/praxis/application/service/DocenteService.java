package ec.edu.praxis.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ec.edu.praxis.domain.model.Docente;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.DocenteRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioCarreraRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Perfiles de docente (API-037 y API-038) y creación automática desde API-031. */
@ApplicationScoped
public class DocenteService {

    private static final int LONGITUD_CODIGO = 20;
    private static final int LONGITUD_DEDICACION = 50;

    @Inject
    DocenteRepository repository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    UsuarioCarreraRepository usuarioCarreraRepository;

    public List<Docente> listar(Long actorId, boolean esAdmin) {
        Set<Long> permitidas = carrerasPermitidas(actorId, esAdmin);
        if (permitidas == null) {
            return repository.listAll();
        }
        if (permitidas.isEmpty()) {
            return List.of();
        }
        return repository.listarPorCarreras(permitidas);
    }

    /**
     * Garantiza el perfil al asignar el rol DOCENTE. Se invoca dentro de la
     * transacción de API-031; no crea un endpoint adicional ni usa V4 en runtime.
     */
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
    public Docente actualizar(Long id, Docente datos, Long actorId, boolean esAdmin) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id del perfil de docente es obligatorio.");
        }
        Docente docente = repository.findById(id);
        if (docente == null) {
            throw ApiException.noEncontrado("Perfil de docente no encontrado.");
        }
        validarDatos(datos);

        Set<Long> permitidas = carrerasPermitidas(actorId, esAdmin);
        Set<Long> objetivo = Set.copyOf(usuarioCarreraRepository.listarCarrerasPorUsuario(docente.getUsuarioId()));
        if (!EstudianteService.dentroDeAmbito(permitidas, objetivo)) {
            throw ApiException.noEncontrado("Perfil de docente no encontrado.");
        }

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

    private Set<Long> carrerasPermitidas(Long actorId, boolean esAdmin) {
        if (esAdmin) {
            return null;
        }
        Usuario actor = usuarioRepository.findById(actorId);
        if (actor == null) {
            throw ApiException.noEncontrado("Usuario no encontrado.");
        }
        if (actor.isAlcanceGlobal()) {
            return null;
        }
        Set<Long> carreras = new LinkedHashSet<>(usuarioCarreraRepository.listarCarrerasPorUsuario(actorId));
        return carreras.isEmpty() ? Set.of() : carreras;
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
