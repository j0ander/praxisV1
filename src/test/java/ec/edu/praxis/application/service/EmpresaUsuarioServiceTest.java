package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.EmpresaUsuario;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Miembro;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;

/** Pruebas unitarias de miembros de empresa, sin PostgreSQL. */
class EmpresaUsuarioServiceTest {

    private static final Long EMPRESA = 10L;
    private static final Long ACTOR = 99L;

    private EmpresaUsuarioService service;
    private RepositorioFalso repository;
    private EmpresaRepositoryFalso empresaRepository;
    private UsuarioRepositoryFalso usuarioRepository;
    private UsuarioRolRepositoryFalso usuarioRolRepository;
    private ContextoFalso contexto;
    private List<String> eventos;

    @BeforeEach
    void preparar() {
        service = new EmpresaUsuarioService();
        eventos = new ArrayList<>();
        repository = new RepositorioFalso(eventos);
        empresaRepository = new EmpresaRepositoryFalso(eventos);
        usuarioRepository = new UsuarioRepositoryFalso();
        usuarioRolRepository = new UsuarioRolRepositoryFalso();
        contexto = new ContextoFalso(eventos);
        service.repository = repository;
        service.empresaRepository = empresaRepository;
        service.usuarioRepository = usuarioRepository;
        service.usuarioRolRepository = usuarioRolRepository;
        service.contextoActor = contexto;

        empresaRepository.empresa = empresa(true);
        usuarioRepository.usuario = usuario(true);
        usuarioRolRepository.codigos = List.of("REPRESENTANTE_EMPRESA");
        repository.activos = 1L;
        repository.administradores = 1L;
    }

    @Test
    void crear_fijaActorDeSesionAntesDePersistirYFuerzaActivo() {
        repository.activos = 0L;

        Miembro creado = service.crear(datos(777L, "ADMIN_EMPRESA", true), EMPRESA, ACTOR, true, false);

        assertEquals(List.of("fijar", "persistir"), eventos);
        assertEquals(List.of(ACTOR), contexto.actores);
        assertEquals(EMPRESA, repository.persistido.getEmpresaId());
        assertEquals(Boolean.TRUE, repository.persistido.getActivo());
        assertEquals(777L, creado.getUsuarioId());
        assertTrue(creado.isActivo());
    }

    @Test
    void crear_datosNulos_oActivoFalse_seRechazaCon400SinEscribir() {
        assertError(400, "DATOS_INVALIDOS", () -> service.crear(null, EMPRESA, ACTOR, true, false));
        assertError(400, "DATOS_INVALIDOS",
                () -> service.crear(datos(777L, "ADMIN_EMPRESA", false), EMPRESA, ACTOR, true, false));

        assertTrue(eventos.isEmpty());
    }

    @Test
    void crear_camposInternosEnElCuerpo_seRechazanCon400() {
        EmpresaUsuario conId = datos(777L, "ADMIN_EMPRESA", true);
        conId.setId(5L);
        EmpresaUsuario conEmpresa = datos(777L, "ADMIN_EMPRESA", true);
        conEmpresa.setEmpresaId(88L);
        EmpresaUsuario conCreated = datos(777L, "ADMIN_EMPRESA", true);
        conCreated.setCreatedAt(OffsetDateTime.now());
        EmpresaUsuario conUpdated = datos(777L, "ADMIN_EMPRESA", true);
        conUpdated.setUpdatedAt(OffsetDateTime.now());

        for (EmpresaUsuario datos : List.of(conId, conEmpresa, conCreated, conUpdated)) {
            assertError(400, "DATOS_INVALIDOS", () -> service.crear(datos, EMPRESA, ACTOR, true, false));
        }
        assertTrue(eventos.isEmpty());
    }

    @Test
    void crear_primerMiembroNoAdministrador_seRechazaCon409() {
        repository.activos = 0L;

        assertError(409, "REGLA_NEGOCIO",
                () -> service.crear(datos(777L, "PUBLICADOR", true), EMPRESA, ACTOR, true, false));
        assertTrue(eventos.isEmpty());
    }

    @Test
    void crear_usuarioInactivoOSinRol_seRechazaCon409() {
        usuarioRepository.usuario = usuario(false);
        assertError(409, "REGLA_NEGOCIO",
                () -> service.crear(datos(777L, "ADMIN_EMPRESA", true), EMPRESA, ACTOR, true, false));

        usuarioRepository.usuario = usuario(true);
        usuarioRolRepository.codigos = List.of("TUTOR_EMPRESARIAL");
        assertError(409, "REGLA_NEGOCIO",
                () -> service.crear(datos(777L, "ADMIN_EMPRESA", true), EMPRESA, ACTOR, true, false));
        assertTrue(eventos.isEmpty());
    }

    @Test
    void crear_empresaInactivaOMiembroDuplicado_seRechazaCon409() {
        empresaRepository.empresa = empresa(false);
        assertError(409, "REGLA_NEGOCIO",
                () -> service.crear(datos(777L, "ADMIN_EMPRESA", true), EMPRESA, ACTOR, true, false));

        empresaRepository.empresa = empresa(true);
        repository.duplicado = true;
        assertError(409, "DUPLICADO",
                () -> service.crear(datos(777L, "ADMIN_EMPRESA", true), EMPRESA, ACTOR, true, false));
        assertTrue(eventos.isEmpty());
    }

    @Test
    void crear_representanteNoAdministrador_devuelve404SinEscribir() {
        repository.rol = "PUBLICADOR";

        assertError(404, "NO_ENCONTRADO",
                () -> service.crear(datos(777L, "ADMIN_EMPRESA", true), EMPRESA, ACTOR, false, true));
        assertTrue(eventos.isEmpty());
    }

    @Test
    void actualizar_camposInmutablesDelCuerpo_seRechazanCon400() {
        EmpresaUsuario conId = datosActualizacion("ADMIN_EMPRESA", true);
        conId.setId(5L);
        EmpresaUsuario conEmpresa = datosActualizacion("ADMIN_EMPRESA", true);
        conEmpresa.setEmpresaId(88L);
        EmpresaUsuario conUsuario = datosActualizacion("ADMIN_EMPRESA", true);
        conUsuario.setUsuarioId(777L);
        EmpresaUsuario conFechas = datosActualizacion("ADMIN_EMPRESA", true);
        conFechas.setUpdatedAt(OffsetDateTime.now());

        for (EmpresaUsuario datos : List.of(conId, conEmpresa, conUsuario, conFechas)) {
            assertError(400, "DATOS_INVALIDOS",
                    () -> service.actualizar(datos, EMPRESA, 5L, ACTOR, true, false));
        }
        assertTrue(eventos.isEmpty());
    }

    @Test
    void actualizar_ultimoAdministradorActivo_seRechazaAunqueEmpresaEsteInactiva() {
        empresaRepository.empresa = empresa(false);
        repository.administradores = 0L;
        repository.actual = membresia(5L, "ADMIN_EMPRESA", true);

        assertError(409, "REGLA_NEGOCIO", () -> service.actualizar(
                datosActualizacion("PUBLICADOR", true), EMPRESA, 5L, ACTOR, true, false));
        assertError(409, "REGLA_NEGOCIO", () -> service.actualizar(
                datosActualizacion("ADMIN_EMPRESA", false), EMPRESA, 5L, ACTOR, true, false));
        assertEquals(List.of("bloquear", "bloquear"), eventos);
    }

    @Test
    void actualizar_miembroAjeno_oSinActivo_seRechaza() {
        assertError(404, "NO_ENCONTRADO", () -> service.actualizar(
                datosActualizacion("PUBLICADOR", true), EMPRESA, 5L, ACTOR, true, false));

        repository.actual = membresia(5L, "PUBLICADOR", true);
        assertError(400, "DATOS_INVALIDOS", () -> service.actualizar(
                datosActualizacion("PUBLICADOR", null), EMPRESA, 5L, ACTOR, true, false));
        assertEquals(List.of("bloquear"), eventos);
    }

    @Test
    void actualizar_cambioPermitido_bloqueaEmpresaFijaActorYDevuelveValorFinal() {
        repository.actual = membresia(5L, "ADMIN_EMPRESA", true);
        repository.administradores = 1L;
        Miembro resultado = service.actualizar(
                datosActualizacion("PUBLICADOR", true), EMPRESA, 5L, ACTOR, true, false);

        assertEquals(List.of("bloquear", "fijar", "flush"), eventos);
        assertEquals("PUBLICADOR", repository.actual.getRolEmpresa());
        assertEquals(5L, resultado.getId());
    }

    @Test
    void listar_ámbitoDeRepresentanteExigeAdministradorActivo() {
        repository.rol = "ADMIN_EMPRESA";
        assertEquals(1, service.listar(EMPRESA, ACTOR, false, true).size());

        repository.rol = "PUBLICADOR";
        assertError(404, "NO_ENCONTRADO", () -> service.listar(EMPRESA, ACTOR, false, true));

        assertEquals(1, service.listar(EMPRESA, ACTOR, true, false).size());
    }

    private static void assertError(int status, String codigo, Runnable accion) {
        ApiException error = assertThrows(ApiException.class, accion::run);
        assertEquals(status, error.getStatus());
        assertEquals(codigo, error.getCodigo());
    }

    private static Empresa empresa(boolean activa) {
        Empresa empresa = new Empresa();
        asignarId(empresa, EMPRESA);
        empresa.setActiva(activa);
        return empresa;
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

    private static EmpresaUsuario datos(Long usuarioId, String rol, Boolean activo) {
        EmpresaUsuario datos = new EmpresaUsuario();
        datos.setUsuarioId(usuarioId);
        datos.setRolEmpresa(rol);
        datos.setActivo(activo);
        return datos;
    }

    private static EmpresaUsuario datosActualizacion(String rol, Boolean activo) {
        EmpresaUsuario datos = new EmpresaUsuario();
        datos.setRolEmpresa(rol);
        datos.setActivo(activo);
        return datos;
    }

    private static Miembro miembro(Long id, String rol, boolean activo) {
        return new Miembro(id, 777L, "Nombres", "Apellidos", "correo@example.test", rol, activo);
    }

    private static EmpresaUsuario membresia(Long id, String rol, Boolean activo) {
        EmpresaUsuario membresia = datos(777L, rol, activo);
        membresia.setId(id);
        return membresia;
    }

    private static final class RepositorioFalso extends EmpresaUsuarioRepository {

        private final List<String> eventos;
        private EmpresaUsuario persistido;
        private EmpresaUsuario actual;
        private boolean duplicado;
        private long activos;
        private long administradores;
        private String rol = "ADMIN_EMPRESA";

        RepositorioFalso(List<String> eventos) {
            this.eventos = eventos;
        }

        @Override
        public List<Miembro> listarMiembrosPorEmpresa(Long empresaId) {
            return List.of(miembro(5L, "ADMIN_EMPRESA", true));
        }

        @Override
        public EmpresaUsuario buscarPorIdYEmpresa(Long id, Long empresaId) {
            return actual != null && actual.getId() != null && actual.getId().equals(id) ? actual : null;
        }

        @Override
        public boolean existeDuplicado(Long empresaId, Long usuarioId) {
            return duplicado;
        }

        @Override
        public long contarActivos(Long empresaId, Long idExcluido) {
            return activos;
        }

        @Override
        public long contarAdministradoresActivos(Long empresaId, Long idExcluido) {
            return administradores;
        }

        @Override
        public String rolActivo(Long empresaId, Long usuarioId) {
            return rol;
        }

        @Override
        public Miembro buscarMiembroPorId(Long id) {
            return miembro(id, "ADMIN_EMPRESA", true);
        }

        @Override
        public void persistAndFlush(EmpresaUsuario entidad) {
            eventos.add("persistir");
            persistido = entidad;
            entidad.setId(5L);
        }

        @Override
        public void flush() {
            eventos.add("flush");
        }
    }

    private static final class EmpresaRepositoryFalso extends EmpresaRepository {

        private final List<String> eventos;
        private Empresa empresa;

        EmpresaRepositoryFalso(List<String> eventos) {
            this.eventos = eventos;
        }

        @Override
        public Empresa findById(Long id) {
            return empresa;
        }

        @Override
        public Empresa buscarParaBloqueo(Long id) {
            eventos.add("bloquear");
            return findById(id);
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

    private static final class ContextoFalso extends ContextoActorService {

        private final List<String> eventos;
        private final List<Long> actores = new ArrayList<>();

        ContextoFalso(List<String> eventos) {
            this.eventos = eventos;
        }

        @Override
        public void fijarActor(Long actorId) {
            eventos.add("fijar");
            actores.add(actorId);
        }
    }
}
