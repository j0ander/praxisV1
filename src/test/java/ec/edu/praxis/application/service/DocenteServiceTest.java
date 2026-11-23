package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Docente;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.domain.model.UsuarioCarrera;
import ec.edu.praxis.infrastructure.repository.DocenteRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioCarreraRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;

/** Pruebas unitarias de DocenteService sin PostgreSQL. */
class DocenteServiceTest {

    private DocenteService service;
    private DocenteRepoFalso docentes;
    private UsuarioRepoFalso usuarios;
    private UsuarioCarreraRepoFalso usuarioCarreras;

    @BeforeEach
    void preparar() {
        service = new DocenteService();
        docentes = new DocenteRepoFalso();
        usuarios = new UsuarioRepoFalso();
        usuarioCarreras = new UsuarioCarreraRepoFalso();

        service.repository = docentes;
        service.usuarioRepository = usuarios;
        service.usuarioCarreraRepository = usuarioCarreras;
        docentes.usuarioCarreras = usuarioCarreras;

        usuarios.agregar(1L, false);
        usuarios.agregar(2L, false);
        usuarios.agregar(3L, false);
    }

    @Test
    void crearPerfilPorRol_variasLlamadas_creaUnSoloPerfil() {
        service.crearPerfilPorRol(1L);
        service.crearPerfilPorRol(1L);
        service.crearPerfilPorRol(1L);

        assertEquals(1, docentes.datos.size());
        assertEquals(1L, docentes.datos.values().iterator().next().getUsuarioId());
    }

    @Test
    void validarDatos_codigoYDedicacionDemasiadoLargos_devuelve400() {
        Docente datos = new Docente();
        datos.setCodigoDocente("X".repeat(21));
        ApiException codigo = assertThrows(ApiException.class, () -> DocenteService.validarDatos(datos));
        assertEquals(400, codigo.getStatus());

        datos.setCodigoDocente("DOC-1");
        datos.setDedicacion("D".repeat(51));
        ApiException dedicacion = assertThrows(ApiException.class, () -> DocenteService.validarDatos(datos));
        assertEquals(400, dedicacion.getStatus());
        assertEquals("DATOS_INVALIDOS", dedicacion.getCodigo());
    }

    @Test
    void actualizar_codigoDuplicado_devuelve409() {
        Docente primero = docentes.crear(1L, "DOC-1");
        Docente segundo = docentes.crear(2L, null);
        Docente datos = new Docente();
        datos.setCodigoDocente("DOC-1");

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(segundo.getId(), datos, 1L, true));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
        assertEquals("DOC-1", primero.getCodigoDocente());
    }

    @Test
    void actualizar_datosValidos_actualizaPerfil() {
        Docente docente = docentes.crear(1L, "DOC-ANTIGUO");
        Docente datos = new Docente();
        datos.setCodigoDocente("DOC-NUEVO");
        datos.setDedicacion("MEDIA");

        Docente actualizado = service.actualizar(docente.getId(), datos, 1L, true);

        assertEquals("DOC-NUEVO", actualizado.getCodigoDocente());
        assertEquals("MEDIA", actualizado.getDedicacion());
    }

    @Test
    void listar_coordinadorSoloVeDocentesDeSuAmbito() {
        Docente docente = docentes.crear(1L, "DOC-1");
        docentes.crear(2L, "DOC-2");

        assertTrue(service.listar(3L, false).isEmpty());

        usuarioCarreras.agregar(3L, 10L);
        usuarioCarreras.agregar(1L, 10L);

        assertEquals(List.of(docente), service.listar(3L, false));
    }

    private static void asignarId(Object entidad, Long id) throws Exception {
        Field campo = entidad.getClass().getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(entidad, id);
    }

    private static final class DocenteRepoFalso extends DocenteRepository {

        private final Map<Long, Docente> datos = new LinkedHashMap<>();
        private long siguiente = 1;
        private UsuarioCarreraRepoFalso usuarioCarreras;

        Docente crear(Long usuarioId, String codigo) {
            Docente docente = new Docente();
            docente.setUsuarioId(usuarioId);
            docente.setCodigoDocente(codigo);
            persistAndFlush(docente);
            return docente;
        }

        @Override
        public Docente findById(Long id) {
            return datos.get(id);
        }

        @Override
        public List<Docente> listAll() {
            return List.copyOf(datos.values());
        }

        @Override
        public Docente buscarPorUsuarioId(Long usuarioId) {
            return datos.values().stream().filter(d -> d.getUsuarioId().equals(usuarioId)).findFirst().orElse(null);
        }

        @Override
        public Docente buscarPorCodigoDocente(String codigo) {
            return datos.values().stream().filter(d -> codigo.equals(d.getCodigoDocente())).findFirst().orElse(null);
        }

        @Override
        public List<Docente> listarPorCarreras(Collection<Long> carreras) {
            return datos.values().stream()
                    .filter(d -> usuarioCarreras.listarCarrerasPorUsuario(d.getUsuarioId()).stream()
                            .anyMatch(carreras::contains))
                    .toList();
        }

        @Override
        public void persistAndFlush(Docente entidad) {
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
