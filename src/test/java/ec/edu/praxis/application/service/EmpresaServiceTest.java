package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Administracion;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;

/** Pruebas unitarias de empresas, sin PostgreSQL. */
class EmpresaServiceTest {

    private static final Long ACTOR = 99L;

    private EmpresaService service;
    private EmpresaRepoFalso repository;
    private UsuarioRepositoryFalso usuarioRepository;
    private UsuarioRolRepositoryFalso usuarioRolRepository;
    private EmpresaUsuarioRepositoryFalso empresaUsuarioRepository;

    @BeforeEach
    void preparar() {
        service = new EmpresaService();
        repository = new EmpresaRepoFalso();
        usuarioRepository = new UsuarioRepositoryFalso();
        usuarioRolRepository = new UsuarioRolRepositoryFalso();
        empresaUsuarioRepository = new EmpresaUsuarioRepositoryFalso();
        service.repository = repository;
        service.usuarioRepository = usuarioRepository;
        service.usuarioRolRepository = usuarioRolRepository;
        service.empresaUsuarioRepository = empresaUsuarioRepository;

        usuarioRepository.usuario = usuario(true);
        usuarioRolRepository.codigos = List.of("REPRESENTANTE_EMPRESA");
    }

    @Test
    void validarRuc_soloAcepta13Digitos_devuelve400() {
        assertDoesNotThrow(() -> EmpresaService.validarRuc("0999999999001"));

        for (String invalido : List.of("099999999900", "abcdefghijklm", " ")) {
            ApiException error = assertThrows(ApiException.class,
                    () -> EmpresaService.validarRuc(invalido));
            assertEquals(400, error.getStatus());
            assertEquals("DATOS_INVALIDOS", error.getCodigo());
        }
    }

    @Test
    void crear_rucDuplicado_devuelve409() {
        repository.agregar(empresa("0999999999001", "Existente"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.crear(empresa("0999999999001", "Otra")));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
    }

    @Test
    void crear_rucValido_quedaActivaPorDefecto() {
        Empresa creada = service.crear(empresa("0999999999002", "Nueva"));

        assertTrue(Boolean.TRUE.equals(creada.getActiva()));
        assertEquals("0999999999002", repository.porRuc.get(creada.getRuc()).getRuc());
    }

    @Test
    void crear_conActiva_devuelve400() {
        Empresa datos = empresa("0999999999009", "Con activa");
        datos.setActiva(Boolean.TRUE);

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void crear_conId_devuelve400() {
        Empresa datos = empresa("0999999999010", "Con id");
        asignarId(datos, 77L);

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void crear_conCreatedAt_devuelve400() {
        Empresa datos = empresa("0999999999011", "Con createdAt");
        asignarFecha(datos, "createdAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void crear_conUpdatedAt_devuelve400() {
        Empresa datos = empresa("0999999999012", "Con updatedAt");
        asignarFecha(datos, "updatedAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void actualizar_sinId_devuelve400() {
        Empresa datos = empresa("0999999999003", "Sin id");
        datos.setActiva(Boolean.TRUE);

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(datos, 10L, true, false));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void actualizar_conCreatedAt_devuelve400() {
        Empresa original = repository.agregar(empresa("0999999999013", "Original"));
        Empresa datos = empresa("0999999999013", "Actualizada");
        asignarId(datos, original.getId());
        datos.setActiva(Boolean.TRUE);
        asignarFecha(datos, "createdAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(datos, 10L, true, false));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals("Original", repository.datos.get(original.getId()).getRazonSocial());
    }

    @Test
    void actualizar_conUpdatedAt_devuelve400() {
        Empresa original = repository.agregar(empresa("0999999999014", "Original"));
        Empresa datos = empresa("0999999999014", "Actualizada");
        asignarId(datos, original.getId());
        datos.setActiva(Boolean.TRUE);
        asignarFecha(datos, "updatedAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(datos, 10L, true, false));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals("Original", repository.datos.get(original.getId()).getRazonSocial());
    }

    @Test
    void actualizar_conActivaFalse_desactivaSinEliminar() {
        Empresa original = repository.agregar(empresa("0999999999003", "Original"));

        Empresa datos = empresa("0999999999003", "Actualizada");
        asignarId(datos, original.getId());
        datos.setActiva(Boolean.FALSE);
        service.actualizar(datos, 10L, true, false);

        assertFalse(repository.datos.get(original.getId()).getActiva());
        assertEquals(1, repository.datos.size());
    }

    @Test
    void actualizar_publicadorConMembresiaActiva_devuelve404() {
        Empresa original = repository.agregar(empresa("0999999999004", "Original"));
        repository.miembros.add(20L);

        Empresa datos = empresa("0999999999004", "Actualizada");
        asignarId(datos, original.getId());
        datos.setActiva(Boolean.TRUE);

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(datos, 20L, false, true));
        assertEquals(404, error.getStatus());
    }

    @Test
    void actualizar_adminEmpresaConMembresiaEfectiva_actualiza() {
        Empresa original = repository.agregar(empresa("0999999999004", "Original"));
        repository.administradores.add(21L);

        Empresa datos = empresa("0999999999004", "Actualizada");
        asignarId(datos, original.getId());
        datos.setActiva(Boolean.TRUE);

        service.actualizar(datos, 21L, false, true);

        assertEquals("Actualizada", repository.datos.get(original.getId()).getRazonSocial());
    }

    @Test
    void buscar_representanteDeOtraEmpresa_devuelve404() {
        Empresa original = repository.agregar(empresa("0999999999005", "Ajena"));
        repository.administradores.add(30L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.buscar(original.getId(), 30L, false, true));
        assertEquals(404, error.getStatus());
    }

    @Test
    void buscar_representanteDeLaPropiaEmpresa_devuelveEmpresa() {
        Empresa original = repository.agregar(empresa("0999999999006", "Propia"));
        repository.miembros.add(40L);

        Empresa encontrada = service.buscar(original.getId(), 40L, false, true);

        assertEquals(original.getId(), encontrada.getId());
    }

    @Test
    void buscar_usuarioInactivoConMembresia_devuelve404() {
        Empresa original = repository.agregar(empresa("0999999999015", "Propia"));
        repository.miembros.add(41L);
        repository.usuariosInactivos.add(41L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.buscar(original.getId(), 41L, false, true));
        assertEquals(404, error.getStatus());
    }

    @Test
    void buscar_sinRolGlobalRepresentante_devuelve404() {
        Empresa original = repository.agregar(empresa("0999999999016", "Propia"));
        repository.miembros.add(42L);
        repository.sinRolRep.add(42L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.buscar(original.getId(), 42L, false, true));
        assertEquals(404, error.getStatus());
    }

    @Test
    void actualizar_rucDuplicado_devuelve409() {
        Empresa primera = repository.agregar(empresa("0999999999007", "Primera"));
        Empresa segunda = repository.agregar(empresa("0999999999008", "Segunda"));

        Empresa datos = empresa("0999999999007", "Segunda editada");
        asignarId(datos, segunda.getId());
        datos.setActiva(Boolean.TRUE);

        ApiException error = assertThrows(ApiException.class,
                () -> service.actualizar(datos, 50L, true, false));
        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
        assertTrue(repository.datos.containsKey(primera.getId()));
    }

    @Test
    void miEmpresa_administradorActivo_conUnaMembresia_devuelveEmpresa() {
        Empresa propia = repository.agregar(empresa("0999999999001", "Propia"));
        empresaUsuarioRepository.administraciones.add(administracion(propia.getId(), "ADMIN_EMPRESA"));

        Empresa resultado = service.miEmpresa(ACTOR);

        assertEquals(propia.getId(), resultado.getId());
        assertEquals(ACTOR, empresaUsuarioRepository.ultimoUsuarioId);
    }

    @Test
    void miEmpresa_publicadorSinAdministracion_devuelve404() {
        empresaUsuarioRepository.rolMembresia = "PUBLICADOR";

        assertError(404, "NO_ENCONTRADO", () -> service.miEmpresa(ACTOR));
    }

    @Test
    void miEmpresa_usuarioInactivo_devuelve403() {
        usuarioRepository.usuario = usuario(false);
        Empresa propia = repository.agregar(empresa("0999999999002", "Propia"));
        empresaUsuarioRepository.administraciones.add(administracion(propia.getId(), "ADMIN_EMPRESA"));

        assertError(403, "SIN_PERMISO", () -> service.miEmpresa(ACTOR));
    }

    @Test
    void miEmpresa_sinRolGlobalRepresentante_devuelve403() {
        usuarioRolRepository.codigos = List.of("ESTUDIANTE");

        assertError(403, "SIN_PERMISO", () -> service.miEmpresa(ACTOR));
    }

    @Test
    void miEmpresa_sinMembresiaActiva_admin_devuelve404() {
        assertError(404, "NO_ENCONTRADO", () -> service.miEmpresa(ACTOR));
    }

    @Test
    void miEmpresa_dosMembresiasActivas_devuelve409SinRevelarEmpresas() {
        repository.agregar(empresa("0999999999003", "Una"));
        repository.agregar(empresa("0999999999004", "Otra"));
        empresaUsuarioRepository.administraciones.add(administracion(101L, "ADMIN_EMPRESA"));
        empresaUsuarioRepository.administraciones.add(administracion(202L, "ADMIN_EMPRESA"));

        ApiException error = assertThrows(ApiException.class, () -> service.miEmpresa(ACTOR));

        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
        for (String dato : List.of("101", "202", "Una", "Otra")) {
            assertFalse(error.getMessage().contains(dato));
        }
        assertTrue(repository.idsConsultados.isEmpty());
    }

    @Test
    void miEmpresa_empresaInactiva_devuelveElEstadoReal() {
        Empresa inactiva = repository.agregar(empresa("0999999999005", "Inactiva"));
        inactiva.setActiva(false);
        empresaUsuarioRepository.administraciones.add(administracion(inactiva.getId(), "ADMIN_EMPRESA"));

        Empresa resultado = service.miEmpresa(ACTOR);

        assertEquals(inactiva.getId(), resultado.getId());
        assertFalse(resultado.getActiva());
    }

    @Test
    void miEmpresa_sinUsuarioIdEnElContexto_devuelve401() {
        assertError(401, "SIN_SESION", () -> service.miEmpresa(null));
    }

    @Test
    void miEmpresa_soloConsultaLaEmpresaDeLaMembresia() {
        Empresa propia = repository.agregar(empresa("0999999999006", "Propia"));
        repository.agregar(empresa("0999999999007", "Ajena"));
        empresaUsuarioRepository.administraciones.add(administracion(propia.getId(), "ADMIN_EMPRESA"));

        Empresa resultado = service.miEmpresa(ACTOR);

        assertEquals(propia.getId(), resultado.getId());
        assertEquals(List.of(propia.getId()), repository.idsConsultados);
    }

    private static void assertError(int status, String codigo, Runnable accion) {
        ApiException error = assertThrows(ApiException.class, accion::run);
        assertEquals(status, error.getStatus());
        assertEquals(codigo, error.getCodigo());
    }

    private static Administracion administracion(Long empresaId, String rolEmpresa) {
        return new Administracion(empresaId, rolEmpresa);
    }

    private static Usuario usuario(boolean activo) {
        Usuario usuario = new Usuario();
        asignarId(usuario, ACTOR);
        usuario.setActivo(activo);
        return usuario;
    }

    private static Empresa empresa(String ruc, String razonSocial) {
        Empresa empresa = new Empresa();
        empresa.setRuc(ruc);
        empresa.setRazonSocial(razonSocial);
        return empresa;
    }

    private static void asignarId(Object entidad, Long id) {
        try {
            Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void asignarFecha(Object entidad, String campo, Object valor) {
        try {
            Field campoFecha = entidad.getClass().getDeclaredField(campo);
            campoFecha.setAccessible(true);
            campoFecha.set(entidad, valor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class EmpresaRepoFalso extends EmpresaRepository {

        private final Map<Long, Empresa> datos = new LinkedHashMap<>();
        private final Map<String, Empresa> porRuc = new LinkedHashMap<>();
        private final Set<Long> miembros = new HashSet<>();
        private final Set<Long> administradores = new HashSet<>();
        private final Set<Long> usuariosInactivos = new HashSet<>();
        private final Set<Long> sinRolRep = new HashSet<>();
        private final List<Long> idsConsultados = new ArrayList<>();
        private long siguiente = 1;

        private boolean membresiaEfectiva(Long usuarioId) {
            return !usuariosInactivos.contains(usuarioId) && !sinRolRep.contains(usuarioId);
        }

        Empresa agregar(Empresa empresa) {
            asignarId(empresa, siguiente++);
            datos.put(empresa.getId(), empresa);
            porRuc.put(empresa.getRuc(), empresa);
            return empresa;
        }

        @Override
        public Empresa findById(Long id) {
            idsConsultados.add(id);
            return datos.get(id);
        }

        @Override
        public Empresa buscarPorRuc(String ruc) {
            return porRuc.get(ruc);
        }

        @Override
        public boolean perteneceAUsuario(Long empresaId, Long usuarioId) {
            return membresiaEfectiva(usuarioId) && miembros.contains(usuarioId);
        }

        @Override
        public boolean esAdministradorDeUsuario(Long empresaId, Long usuarioId) {
            return membresiaEfectiva(usuarioId) && administradores.contains(usuarioId);
        }

        @Override
        public void persistAndFlush(Empresa entidad) {
            if (entidad.getId() == null) {
                asignarId(entidad, siguiente++);
            }
            datos.put(entidad.getId(), entidad);
            porRuc.put(entidad.getRuc(), entidad);
        }

        @Override
        public void flush() {
        }

        @Override
        public List<Empresa> listAll() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public List<Empresa> listarPorTexto(String texto) {
            return listAll();
        }
    }

    private static final class UsuarioRepositoryFalso extends UsuarioRepository {

        private Usuario usuario;

        @Override
        public Usuario findById(Long id) {
            return id.equals(ACTOR) ? usuario : null;
        }
    }

    private static final class UsuarioRolRepositoryFalso extends UsuarioRolRepository {

        private List<String> codigos = List.of();

        @Override
        public List<String> listarCodigosPorUsuario(Long usuarioId) {
            return codigos;
        }
    }

    private static final class EmpresaUsuarioRepositoryFalso extends EmpresaUsuarioRepository {

        private final List<Administracion> administraciones = new ArrayList<>();
        private String rolMembresia = "ADMIN_EMPRESA";
        private Long ultimoUsuarioId;

        @Override
        public List<Administracion> listarAdministracionesActivas(Long usuarioId) {
            ultimoUsuarioId = usuarioId;
            if (!"ADMIN_EMPRESA".equals(rolMembresia)) {
                return List.of();
            }
            return new ArrayList<>(administraciones);
        }
    }
}
