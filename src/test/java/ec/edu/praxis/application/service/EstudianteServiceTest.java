package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Carrera;
import ec.edu.praxis.domain.model.Estudiante;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.domain.model.UsuarioCarrera;
import ec.edu.praxis.infrastructure.repository.CarreraRepository;
import ec.edu.praxis.infrastructure.repository.EstudianteRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioCarreraRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;

/** Pruebas unitarias de EstudianteService sin PostgreSQL. */
class EstudianteServiceTest {

    private EstudianteService service;
    private EstudianteRepoFalso estudiantes;
    private UsuarioRepoFalso usuarios;
    private CarreraRepoFalso carreras;
    private UsuarioCarreraRepoFalso usuarioCarreras;

    @BeforeEach
    void preparar() {
        service = new EstudianteService();
        estudiantes = new EstudianteRepoFalso();
        usuarios = new UsuarioRepoFalso();
        carreras = new CarreraRepoFalso();
        usuarioCarreras = new UsuarioCarreraRepoFalso();

        service.repository = estudiantes;
        service.usuarioRepository = usuarios;
        service.carreraRepository = carreras;
        service.usuarioCarreraRepository = usuarioCarreras;

        usuarios.agregar(1L, false);
        usuarios.agregar(2L, false);
        usuarios.agregar(3L, false);
        carreras.agregar(10L, true);
        carreras.agregar(20L, true);
    }

    @Test
    void validarDatos_sinCarrera_devuelve400() {
        Estudiante datos = new Estudiante();
        datos.setUsuarioId(1L);

        ApiException error = assertThrows(ApiException.class, () -> EstudianteService.validarDatos(datos));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void crear_camposOpcionalesAusentes_seAceptan() {
        Estudiante creado = service.crear(estudiante(1L, 10L, null), 1L, true);

        assertEquals(1L, creado.getUsuarioId());
        assertEquals(10L, creado.getCarreraId());
        assertNull(creado.getCodigoEstudiante());
        assertNull(creado.getFechaNacimiento());
    }

    @Test
    void crear_carreraInactiva_devuelve409() {
        carreras.agregar(30L, false);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crear(estudiante(1L, 30L, "TK4-INACTIVO"), 1L, true));

        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void crear_perfilDuplicadoPorUsuario_devuelve409() {
        service.crear(estudiante(1L, 10L, "TK4-A"), 1L, true);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crear(estudiante(1L, 20L, "TK4-B"), 1L, true));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
    }

    @Test
    void crear_codigoDuplicado_devuelve409() {
        service.crear(estudiante(1L, 10L, "TK4-COD"), 1L, true);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crear(estudiante(2L, 20L, "TK4-COD"), 1L, true));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
    }

    @Test
    void actualizar_noPermiteCambiarUsuario() {
        Estudiante creado = service.crear(estudiante(1L, 10L, "TK4-U"), 1L, true);
        Estudiante datos = estudiante(2L, 10L, "TK4-U");

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(creado.getId(), datos, 1L, true));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void actualizar_actualizaCarreraCodigoYFecha() {
        Estudiante creado = service.crear(estudiante(1L, 10L, "TK4-UPD"), 1L, true);
        Estudiante datos = estudiante(1L, 20L, "TK4-UPD2");
        datos.setFechaNacimiento(LocalDate.of(2000, 5, 20));

        Estudiante actualizado = service.actualizar(creado.getId(), datos, 1L, true);

        assertEquals(20L, actualizado.getCarreraId());
        assertEquals("TK4-UPD2", actualizado.getCodigoEstudiante());
        assertEquals(LocalDate.of(2000, 5, 20), actualizado.getFechaNacimiento());
    }

    @Test
    void coordinador_fueraDeAmbitoNoVeNiConsultaPerfiles() {
        Estudiante propio = service.crear(estudiante(1L, 10L, "TK4-PROPIO"), 1L, true);
        Estudiante ajeno = service.crear(estudiante(2L, 20L, "TK4-AJENO"), 1L, true);
        usuarioCarreras.agregar(3L, 10L);

        assertEquals(List.of(propio), service.listar(3L, false, null));
        assertTrue(service.listar(3L, false, 20L).isEmpty());
        assertEquals(propio.getId(), service.buscar(propio.getId(), 3L, false).getId());

        ApiException error = assertThrows(ApiException.class,
                () -> service.buscar(ajeno.getId(), 3L, false));
        assertEquals(404, error.getStatus());
        assertEquals("NO_ENCONTRADO", error.getCodigo());
    }

    @Test
    void coordinadorConAlcanceGlobal_veTodosLosPerfiles() {
        service.crear(estudiante(1L, 10L, "TK4-G1"), 1L, true);
        service.crear(estudiante(2L, 20L, "TK4-G2"), 1L, true);
        usuarios.agregar(3L, true);

        assertEquals(2, service.listar(3L, false, null).size());
    }

    private static Estudiante estudiante(Long usuarioId, Long carreraId, String codigo) {
        Estudiante estudiante = new Estudiante();
        estudiante.setUsuarioId(usuarioId);
        estudiante.setCarreraId(carreraId);
        estudiante.setCodigoEstudiante(codigo);
        return estudiante;
    }

    private static void asignarId(Object entidad, Long id) throws Exception {
        Field campo = entidad.getClass().getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(entidad, id);
    }

    private static final class EstudianteRepoFalso extends EstudianteRepository {

        private final Map<Long, Estudiante> datos = new LinkedHashMap<>();
        private long siguiente = 1;

        @Override
        public Estudiante findById(Long id) {
            return datos.get(id);
        }

        @Override
        public List<Estudiante> listAll() {
            return List.copyOf(datos.values());
        }

        @Override
        public Estudiante buscarPorUsuarioId(Long usuarioId) {
            return datos.values().stream().filter(e -> e.getUsuarioId().equals(usuarioId)).findFirst().orElse(null);
        }

        @Override
        public Estudiante buscarPorCodigoEstudiante(String codigo) {
            return datos.values().stream().filter(e -> codigo.equals(e.getCodigoEstudiante())).findFirst()
                    .orElse(null);
        }

        @Override
        public List<Estudiante> listarPorCarrera(Long carreraId) {
            return datos.values().stream().filter(e -> e.getCarreraId().equals(carreraId)).toList();
        }

        @Override
        public List<Estudiante> listarPorCarreras(Collection<Long> carreras) {
            return datos.values().stream().filter(e -> carreras.contains(e.getCarreraId())).toList();
        }

        @Override
        public void persistAndFlush(Estudiante entidad) {
            try {
                asignarId(entidad, siguiente++);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            datos.put(entidad.getId(), entidad);
        }

        @Override
        public void flush() {
        }
    }

    private static final class UsuarioRepoFalso extends UsuarioRepository {

        private final Map<Long, Usuario> datos = new LinkedHashMap<>();

        void agregar(Long id, boolean global) {
            Usuario usuario = new Usuario();
            usuario.setAlcanceGlobal(global);
            try {
                asignarId(usuario, id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            datos.put(id, usuario);
        }

        @Override
        public Usuario findById(Long id) {
            return datos.get(id);
        }
    }

    private static final class CarreraRepoFalso extends CarreraRepository {

        private final Map<Long, Carrera> datos = new LinkedHashMap<>();

        void agregar(Long id, boolean activa) {
            Carrera carrera = new Carrera();
            carrera.setCodigo("CAR-" + id);
            carrera.setNombre("Carrera " + id);
            carrera.setActiva(activa);
            try {
                asignarId(carrera, id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            datos.put(id, carrera);
        }

        @Override
        public Carrera findById(Long id) {
            return datos.get(id);
        }
    }

    private static final class UsuarioCarreraRepoFalso extends UsuarioCarreraRepository {

        private final List<UsuarioCarrera> relaciones = new ArrayList<>();

        void agregar(Long usuarioId, Long carreraId) {
            relaciones.add(new UsuarioCarrera(usuarioId, carreraId));
        }

        @Override
        public List<Long> listarCarrerasPorUsuario(Long usuarioId) {
            return relaciones.stream().filter(r -> r.getId().getUsuarioId().equals(usuarioId))
                    .map(r -> r.getId().getCarreraId()).toList();
        }
    }
}
