package ec.edu.praxis.application.service;

import java.util.List;

import ec.edu.praxis.domain.model.Carrera;
import ec.edu.praxis.domain.model.Estudiante;
import ec.edu.praxis.infrastructure.repository.CarreraRepository;
import ec.edu.praxis.infrastructure.repository.EstudianteRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


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
    UsuarioService usuarioService;

    public List<Estudiante> listar(Long carreraFiltro) {
        return carreraFiltro == null ? repository.listAll() : repository.listarPorCarrera(carreraFiltro);
    }

    public List<Estudiante> listarPorCoordinador(Long coordinadorId) {
        Long carreraId = usuarioService.carreraDeCoordinador(coordinadorId);
        return repository.listarPorCarrera(carreraId);
    }

    public Estudiante buscar(Long id) {
        Estudiante estudiante = repository.findById(id);
        if (estudiante == null) {
            throw ApiException.noEncontrado("Perfil de estudiante no encontrado.");
        }
        return estudiante;
    }

    @Transactional
    public Estudiante crear(Estudiante datos) {
        validarDatos(datos);
        validarUsuario(datos.getUsuarioId());
        validarCarrera(datos.getCarreraId());
        validarDuplicados(datos.getCodigoEstudiante(), datos.getUsuarioId(), null);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Estudiante actualizar(Long id, Estudiante datos) {
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

        validarCarrera(datos.getCarreraId());
        validarDuplicados(datos.getCodigoEstudiante(), estudiante.getUsuarioId(), estudiante.getId());

        estudiante.setCarreraId(datos.getCarreraId());
        estudiante.setCodigoEstudiante(datos.getCodigoEstudiante());
        estudiante.setFechaNacimiento(datos.getFechaNacimiento());
        repository.flush();
        return estudiante;
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
