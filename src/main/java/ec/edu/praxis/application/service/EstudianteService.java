package ec.edu.praxis.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import ec.edu.praxis.domain.model.Carrera;
import ec.edu.praxis.domain.model.Estudiante;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.CarreraRepository;
import ec.edu.praxis.infrastructure.repository.EstudianteRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioCarreraRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Perfiles de estudiante (API-033 a API-036). */
@ApplicationScoped
public class EstudianteService {

    private static final int LONGITUD_CODIGO = 30;

    @Inject
    EstudianteRepository repository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    CarreraRepository carreraRepository;

    @Inject
    UsuarioCarreraRepository usuarioCarreraRepository;

    public List<Estudiante> listar(Long actorId, boolean esAdmin, Long carreraFiltro) {
        Set<Long> permitidas = carrerasPermitidas(actorId, esAdmin);
        if (permitidas == null) {
            return carreraFiltro == null ? repository.listAll() : repository.listarPorCarrera(carreraFiltro);
        }
        if (carreraFiltro != null) {
            return permitidas.contains(carreraFiltro) ? repository.listarPorCarrera(carreraFiltro) : List.of();
        }
        return permitidas.isEmpty() ? List.of() : repository.listarPorCarreras(permitidas);
    }

    public Estudiante buscar(Long id, Long actorId, boolean esAdmin) {
        Estudiante estudiante = repository.findById(id);
        if (estudiante == null) {
            throw ApiException.noEncontrado("Perfil de estudiante no encontrado.");
        }
        verificarAmbito(carrerasPermitidas(actorId, esAdmin), Set.of(estudiante.getCarreraId()));
        return estudiante;
    }

    @Transactional
    public Estudiante crear(Estudiante datos, Long actorId, boolean esAdmin) {
        validarDatos(datos);
        Set<Long> permitidas = carrerasPermitidas(actorId, esAdmin);
        verificarAmbito(permitidas, Set.of(datos.getCarreraId()));
        validarUsuario(datos.getUsuarioId());
        validarCarrera(datos.getCarreraId());
        validarDuplicados(datos.getCodigoEstudiante(), datos.getUsuarioId(), null);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Estudiante actualizar(Long id, Estudiante datos, Long actorId, boolean esAdmin) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id del perfil de estudiante es obligatorio.");
        }
        Estudiante estudiante = repository.findById(id);
        if (estudiante == null) {
            throw ApiException.noEncontrado("Perfil de estudiante no encontrado.");
        }
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del perfil son obligatorios.");
        }
        if (datos.getUsuarioId() != null && !datos.getUsuarioId().equals(estudiante.getUsuarioId())) {
            throw ApiException.datosInvalidos("El usuario del perfil no se puede cambiar.");
        }
        datos.setUsuarioId(estudiante.getUsuarioId());
        validarDatos(datos);

        Set<Long> permitidas = carrerasPermitidas(actorId, esAdmin);
        verificarAmbito(permitidas, Set.of(estudiante.getCarreraId()));
        verificarAmbito(permitidas, Set.of(datos.getCarreraId()));
        validarCarrera(datos.getCarreraId());
        validarDuplicados(datos.getCodigoEstudiante(), estudiante.getUsuarioId(), estudiante.getId());

        estudiante.setCarreraId(datos.getCarreraId());
        estudiante.setCodigoEstudiante(datos.getCodigoEstudiante());
        estudiante.setFechaNacimiento(datos.getFechaNacimiento());
        repository.flush();
        return estudiante;
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
        return Set.copyOf(usuarioCarreraRepository.listarCarrerasPorUsuario(actorId));
    }

    private void verificarAmbito(Set<Long> permitidas, Set<Long> objetivo) {
        if (!dentroDeAmbito(permitidas, objetivo)) {
            throw ApiException.noEncontrado("Perfil de estudiante no encontrado.");
        }
    }

    /** Las carreras permitidas en null significan acceso institucional sin restricción. */
    static boolean dentroDeAmbito(Set<Long> permitidas, Set<Long> objetivo) {
        if (permitidas == null) {
            return true;
        }
        if (objetivo == null || objetivo.isEmpty()) {
            return false;
        }
        return !Collections.disjoint(permitidas, objetivo);
    }

    static void validarDatos(Estudiante datos) {
        if (datos == null || datos.getUsuarioId() == null || datos.getCarreraId() == null) {
            throw ApiException.datosInvalidos("El usuario y la carrera son obligatorios.");
        }
        String codigo = datos.getCodigoEstudiante();
        if (codigo != null && (codigo.isBlank() || codigo.length() > LONGITUD_CODIGO)) {
            throw ApiException.datosInvalidos("El código de estudiante no es válido.");
        }
    }

    private void validarUsuario(Long usuarioId) {
        if (usuarioRepository.findById(usuarioId) == null) {
            throw ApiException.noEncontrado("Usuario no encontrado.");
        }
    }

    private void validarCarrera(Long carreraId) {
        Carrera carrera = carreraRepository.findById(carreraId);
        if (carrera == null) {
            throw ApiException.reglaNegocio("La carrera no existe.");
        }
        if (!carrera.isActiva()) {
            throw ApiException.reglaNegocio("La carrera no está activa.");
        }
    }

    private void validarDuplicados(String codigo, Long usuarioId, Long idActual) {
        Estudiante porUsuario = repository.buscarPorUsuarioId(usuarioId);
        if (porUsuario != null && !porUsuario.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un perfil de estudiante para ese usuario.");
        }
        if (codigo == null) {
            return;
        }
        Estudiante porCodigo = repository.buscarPorCodigoEstudiante(codigo);
        if (porCodigo != null && !porCodigo.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un estudiante con ese código.");
        }
    }
}
