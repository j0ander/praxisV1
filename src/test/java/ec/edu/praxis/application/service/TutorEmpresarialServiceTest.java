package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.TutorEmpresarial;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository;
import ec.edu.praxis.infrastructure.repository.TutorEmpresarialRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;

/** Pruebas unitarias de tutores empresariales, sin PostgreSQL. */
class TutorEmpresarialServiceTest {

    private static final Long EMPRESA = 10L;
    private static final Long ACTOR = 99L;

    private TutorEmpresarialService service;
    private RepositorioFalso repository;
    private EmpresaRepositoryFalso empresaRepository;
    private EmpresaUsuarioRepositoryFalso empresaUsuarioRepository;
    private UsuarioRepositoryFalso usuarioRepository;
    private UsuarioRolRepositoryFalso usuarioRolRepository;

    @BeforeEach
    void preparar() {
        service = new TutorEmpresarialService();
        repository = new RepositorioFalso();
        empresaRepository = new EmpresaRepositoryFalso();
        empresaUsuarioRepository = new EmpresaUsuarioRepositoryFalso();
        usuarioRepository = new UsuarioRepositoryFalso();
        usuarioRolRepository = new UsuarioRolRepositoryFalso();
        service.repository = repository;
        service.empresaRepository = empresaRepository;
        service.empresaUsuarioRepository = empresaUsuarioRepository;
        service.usuarioRepository = usuarioRepository;
        service.usuarioRolRepository = usuarioRolRepository;

        empresaRepository.empresa = new Empresa();
        asignarId(empresaRepository.empresa, EMPRESA);
        empresaRepository.empresa.setActiva(true);
        usuarioRepository.usuario = usuario(true);
        usuarioRolRepository.codigos = List.of("TUTOR_EMPRESARIAL");
    }

    @Test
    void crear_tomaEmpresaDeLaRutaYPersisteElCargo() {
        TutorEmpresarial datos = new TutorEmpresarial();
        datos.setUsuarioId(777L);
        datos.setCargo("Jefe de prácticas");

        TutorEmpresarial creado = service.crear(datos, EMPRESA);

        assertEquals(EMPRESA, repository.persistido.getEmpresaId());
        assertEquals(777L, creado.getUsuarioId());
        assertEquals("Jefe de prácticas", creado.getCargo());
    }

    @Test
    void crear_camposInternosDelCuerpo_seRechazanCon400() {
        TutorEmpresarial conId = datosCreacion(777L, null);
        conId.setId(5L);
        TutorEmpresarial conEmpresa = datosCreacion(777L, null);
        conEmpresa.setEmpresaId(88L);
        TutorEmpresarial conFechas = datosCreacion(777L, null);
        conFechas.setCreatedAt(OffsetDateTime.now());

        for (TutorEmpresarial datos : List.of(conId, conEmpresa, conFechas)) {
            assertError(400, "DATOS_INVALIDOS", () -> service.crear(datos, EMPRESA));
        }
        assertNull(repository.persistido);
    }

    @Test
    void crear_usuarioInactivoOSinRol_seRechazaCon409() {
        usuarioRepository.usuario = usuario(false);
        assertError(409, "REGLA_NEGOCIO", () -> service.crear(datosCreacion(777L, null), EMPRESA));

        usuarioRepository.usuario = usuario(true);
        usuarioRolRepository.codigos = List.of("REPRESENTANTE_EMPRESA");
        assertError(409, "REGLA_NEGOCIO", () -> service.crear(datosCreacion(777L, null), EMPRESA));
        assertNull(repository.persistido);
    }

    @Test
    void crear_usuarioYaTutorOtroUsuarioInexistente_seRechaza() {
        repository.existente = true;
        assertError(409, "DUPLICADO", () -> service.crear(datosCreacion(777L, null), EMPRESA));

        repository.existente = false;
        usuarioRepository.usuario = null;
        assertError(404, "NO_ENCONTRADO", () -> service.crear(datosCreacion(777L, null), EMPRESA));
        assertNull(repository.persistido);
    }

    @Test
    void crear_cargoFueraDeLongitud_seRechazaCon400() {
        assertError(400, "DATOS_INVALIDOS", () -> service.crear(datosCreacion(777L, "x".repeat(101)), EMPRESA));
        assertNull(repository.persistido);
    }

    @Test
    void actualizar_idObligatorioYCamposInmutables_seRechazanCon400() {
        TutorEmpresarial sinId = datosActualizacion("Nuevo cargo");
        assertError(400, "DATOS_INVALIDOS", () -> service.actualizar(sinId, EMPRESA));

        TutorEmpresarial conEmpresa = datosActualizacion("Nuevo cargo");
        conEmpresa.setId(5L);
        conEmpresa.setEmpresaId(88L);
        TutorEmpresarial conUsuario = datosActualizacion("Nuevo cargo");
        conUsuario.setId(5L);
        conUsuario.setUsuarioId(777L);
        TutorEmpresarial conFechas = datosActualizacion("Nuevo cargo");
        conFechas.setId(5L);
        conFechas.setUpdatedAt(OffsetDateTime.now());

        for (TutorEmpresarial datos : List.of(conEmpresa, conUsuario, conFechas)) {
            assertError(400, "DATOS_INVALIDOS", () -> service.actualizar(datos, EMPRESA));
        }
    }

    @Test
    void actualizar_tutorDeOtraEmpresa_devuelve404() {
        assertError(404, "NO_ENCONTRADO", () -> service.actualizar(datosActualizacionConId(5L, "Nuevo cargo"), EMPRESA));
    }

    @Test
    void actualizar_soloCambiaElCargoDelTutorPropio() {
        repository.tutor = tutor(5L, EMPRESA, "Cargo anterior");

        TutorEmpresarial resultado = service.actualizar(datosActualizacionConId(5L, "Cargo nuevo"), EMPRESA);

        assertEquals("Cargo nuevo", repository.tutor.getCargo());
        assertEquals(777L, resultado.getUsuarioId());
        assertEquals(EMPRESA, resultado.getEmpresaId());
    }

    @Test
    void listar_representanteDeLaPropiaEmpresaSinAdminPuedeConsultar() {
        empresaUsuarioRepository.rol = "PUBLICADOR";

        assertEquals(1, service.listar(EMPRESA, ACTOR, false, true).size());
    }

    @Test
    void listar_representanteDeOtraEmpresa_devuelve404() {
        empresaUsuarioRepository.rol = null;

        assertError(404, "NO_ENCONTRADO", () -> service.listar(EMPRESA, ACTOR, false, true));
        assertEquals(1, service.listar(EMPRESA, ACTOR, true, false).size());
    }

    private static void assertError(int status, String codigo, Runnable accion) {
        ApiException error = assertThrows(ApiException.class, accion::run);
        assertEquals(status, error.getStatus());
        assertEquals(codigo, error.getCodigo());
    }

    private static Usuario usuario(boolean activo) {
        Usuario usuario = new Usuario();
        asignarId(usuario, 777L);
        usuario.setActivo(activo);
        return usuario;
    }

    private static void asignarId(Object entidad, Long id) {
        try {
            Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static TutorEmpresarial datos(Long id, String cargo) {
        TutorEmpresarial datos = datosCreacion(777L, cargo);
        datos.setId(id);
        return datos;
    }

    private static TutorEmpresarial datosCreacion(Long usuarioId, String cargo) {
        TutorEmpresarial datos = new TutorEmpresarial();
        datos.setUsuarioId(usuarioId);
        datos.setCargo(cargo);
        return datos;
    }

    private static TutorEmpresarial datosActualizacion(String cargo) {
        TutorEmpresarial datos = new TutorEmpresarial();
        datos.setCargo(cargo);
        return datos;
    }

    private static TutorEmpresarial datosActualizacionConId(Long id, String cargo) {
        TutorEmpresarial datos = datosActualizacion(cargo);
        datos.setId(id);
        return datos;
    }

    private static TutorEmpresarial tutor(Long id, Long empresaId, String cargo) {
        TutorEmpresarial tutor = datos(id, cargo);
        tutor.setUsuarioId(777L);
        tutor.setEmpresaId(empresaId);
        return tutor;
    }

    private static final class RepositorioFalso extends TutorEmpresarialRepository {

        private TutorEmpresarial persistido;
        private TutorEmpresarial tutor;
        private boolean existente;

        @Override
        public List<TutorEmpresarial> listarPorEmpresa(Long empresaId) {
            return List.of(tutor(5L, empresaId, "Cargo"));
        }

        @Override
        public TutorEmpresarial buscarPorIdYEmpresa(Long id, Long empresaId) {
            return tutor != null && id.equals(tutor.getId()) && empresaId.equals(tutor.getEmpresaId())
                    ? tutor : null;
        }

        @Override
        public boolean existePorUsuario(Long usuarioId) {
            return existente;
        }

        @Override
        public void persistAndFlush(TutorEmpresarial entidad) {
            persistido = entidad;
            entidad.setId(5L);
        }

        @Override
        public void flush() {
            assertTrue(tutor != null);
        }
    }

    private static final class EmpresaRepositoryFalso extends EmpresaRepository {

        private Empresa empresa;

        @Override
        public Empresa findById(Long id) {
            return empresa;
        }
    }

    private static final class EmpresaUsuarioRepositoryFalso extends EmpresaUsuarioRepository {

        private String rol = "ADMIN_EMPRESA";

        @Override
        public String rolActivo(Long empresaId, Long usuarioId) {
            return rol;
        }
    }

    private static final class UsuarioRepositoryFalso extends UsuarioRepository {

        private Usuario usuario;

        @Override
        public Usuario findById(Long id) {
            return usuario;
        }
    }

    private static final class UsuarioRolRepositoryFalso extends UsuarioRolRepository {

        private List<String> codigos = List.of();

        @Override
        public List<String> listarCodigosPorUsuario(Long usuarioId) {
            return codigos;
        }
    }
}
