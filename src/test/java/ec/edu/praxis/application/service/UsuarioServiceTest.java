package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Carrera;
import ec.edu.praxis.domain.model.Docente;
import ec.edu.praxis.domain.model.Rol;
import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.domain.model.UsuarioCarrera;
import ec.edu.praxis.domain.model.UsuarioRol;
import ec.edu.praxis.infrastructure.repository.CarreraRepository;
import ec.edu.praxis.infrastructure.repository.DocenteRepository;
import ec.edu.praxis.infrastructure.repository.RolRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioCarreraRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

/** Pruebas unitarias de UsuarioService sin PostgreSQL. */
class UsuarioServiceTest {

    private UsuarioService service;
    private UsuarioRepoFalso usuarios;
    private RolRepoFalso roles;
    private UsuarioRolRepoFalso usuarioRoles;
    private CarreraRepoFalso carreras;
    private UsuarioCarreraRepoFalso usuarioCarreras;
    private DocenteRepoFalso docentes;
    private EventoFalso eventos;

    @BeforeEach
    void preparar() {
        service = new UsuarioService();
        usuarios = new UsuarioRepoFalso();
        roles = new RolRepoFalso();
        usuarioRoles = new UsuarioRolRepoFalso(roles);
        usuarios.usarUsuarioRoles(usuarioRoles);
        carreras = new CarreraRepoFalso();
        usuarioCarreras = new UsuarioCarreraRepoFalso();
        docentes = new DocenteRepoFalso();

        service.repository = usuarios;
        service.rolRepository = roles;
        service.usuarioRolRepository = usuarioRoles;
        service.carreraRepository = carreras;
        service.usuarioCarreraRepository = usuarioCarreras;

        DocenteService docenteService = new DocenteService();
        docenteService.repository = docentes;
        service.docenteService = docenteService;

        eventos = new EventoFalso();
        service.eventosInvalidacion = eventos;
    }

    @Test
    void validarCorreo_conMayusculasYEspacios_devuelveMinusculas() {
        assertEquals("admin.prueba@example.test",
                UsuarioService.validarCorreo("  Admin.Prueba@Example.Test "));
    }

    @Test
    void validarCorreo_formatoInvalido_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> UsuarioService.validarCorreo("correo-sin-arroba"));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void crear_datosValidos_normalizaCorreoYGuardaHashTemporal() {
        Usuario creado = service.crear(usuario("  TK4-NUEVO@Example.Test ", "0999999999"), "ClaveTemporal123!");

        assertEquals("tk4-nuevo@example.test", creado.getCorreo());
        assertTrue(creado.isActivo());
        assertTrue(creado.isDebeCambiarPassword());
        assertEquals(0, creado.getIntentosFallidos());
        assertNotNull(creado.getHashPassword());
        assertTrue(BcryptUtil.matches("ClaveTemporal123!", creado.getHashPassword()));
    }

    @Test
    void crear_correoDuplicado_devuelve409() {
        service.crear(usuario("tk4-uno@example.test", "0999999991"), "ClaveTemporal123!");

        ApiException error = assertThrows(ApiException.class,
                () -> service.crear(usuario("TK4-UNO@example.test", "0999999992"), "ClaveTemporal123!"));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
    }

    @Test
    void crear_documentoDuplicado_devuelve409() {
        service.crear(usuario("tk4-dos@example.test", "0999999993"), "ClaveTemporal123!");

        ApiException error = assertThrows(ApiException.class,
                () -> service.crear(usuario("tk4-tres@example.test", "0999999993"), "ClaveTemporal123!"));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
    }

    @Test
    void crearAdministradorInicial_datosValidos_creaUnicoRolYCredencialTemporal() {
        roles.agregar("ADMINISTRADOR", 3L);

        boolean creado = service.crearAdministradorInicial(
                "  TK4-ADMIN-INICIAL@Example.Test ", "ClaveInicial123!");

        assertTrue(creado);
        Usuario usuario = usuarios.buscarPorCorreo("tk4-admin-inicial@example.test");
        assertNotNull(usuario);
        assertTrue(usuario.isActivo());
        assertTrue(usuario.isDebeCambiarPassword());
        assertTrue(usuario.isAlcanceGlobal());
        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
        assertNull(usuario.getUltimoLogin());
        assertTrue(BcryptUtil.matches("ClaveInicial123!", usuario.getHashPassword()));
        assertEquals(List.of("ADMINISTRADOR"), service.rolesDe(usuario.getId()));
    }

    @Test
    void crearAdministradorInicial_yaExisteAdmin_noModificaNada() {
        roles.agregar("ADMINISTRADOR", 3L);
        Usuario existente = service.crear(usuario("tk4-admin-existente@example.test", "0999000091"),
                "ClaveOriginal123!");
        service.asignarRoles(existente.getId(), List.of("ADMINISTRADOR"));
        String hashOriginal = existente.getHashPassword();

        boolean creado = service.crearAdministradorInicial("tk4-otro-admin@example.test", "ClaveNueva123!");

        assertFalse(creado);
        assertNull(usuarios.buscarPorCorreo("tk4-otro-admin@example.test"));
        assertEquals(1, usuarios.cantidad());
        assertEquals(hashOriginal, existente.getHashPassword());
        assertEquals(List.of("ADMINISTRADOR"), service.rolesDe(existente.getId()));
    }

    @Test
    void crearAdministradorInicial_rolInexistente_noCreaUsuario() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.crearAdministradorInicial("tk4-admin-sin-rol@example.test", "ClaveInicial123!"));

        assertTrue(error.getMessage().contains("ADMINISTRADOR"));
        assertNull(usuarios.buscarPorCorreo("tk4-admin-sin-rol@example.test"));
    }

    @Test
    void crearAdministradorInicial_correoInvalido_devuelve400() {
        roles.agregar("ADMINISTRADOR", 3L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearAdministradorInicial("correo-invalido", "ClaveInicial123!"));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertTrue(usuarios.cantidad() == 0);
    }

    @Test
    void crearAdministradorInicial_passwordVacio_devuelve400() {
        roles.agregar("ADMINISTRADOR", 3L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearAdministradorInicial("tk4-admin-clave@example.test", "   "));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertTrue(usuarios.cantidad() == 0);
    }

    @Test
    void crearAdministradorInicial_correoDuplicado_noSobrescribeCuentaExistente() {
        roles.agregar("ADMINISTRADOR", 3L);
        roles.agregar("ESTUDIANTE", 2L);
        Usuario existente = service.crear(usuario("tk4-no-admin@example.test", "0999000092"),
                "ClaveOriginal123!");
        service.asignarRoles(existente.getId(), List.of("ESTUDIANTE"));
        String hashOriginal = existente.getHashPassword();

        ApiException error = assertThrows(ApiException.class,
                () -> service.crearAdministradorInicial("TK4-NO-ADMIN@Example.Test", "ClaveNueva123!"));

        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
        assertEquals(1, usuarios.cantidad());
        assertEquals(hashOriginal, existente.getHashPassword());
        assertEquals(List.of("ESTUDIANTE"), service.rolesDe(existente.getId()));
    }

    @Test
    void crearAdministradorInicial_falloAlPersistir_noAsignaRol() {
        roles.agregar("ADMINISTRADOR", 3L);
        usuarios.fallarPersistencia();

        assertThrows(IllegalStateException.class,
                () -> service.crearAdministradorInicial("tk4-admin-fallo@example.test", "ClaveInicial123!"));

        assertNull(usuarios.buscarPorCorreo("tk4-admin-fallo@example.test"));
        assertFalse(usuarioRoles.tieneRol("ADMINISTRADOR"));
    }

    @Test
    void actualizar_noModificaCredencialesNiEstado() {
        Usuario creado = service.crear(usuario("tk4-cuatro@example.test", "0999999994"), "ClaveTemporal123!");
        String hashOriginal = creado.getHashPassword();
        creado.setIntentosFallidos(3);
        Usuario datos = usuario("TK4-CUATRO@example.test", "0999999995");
        datos.setTelefono("0987654321");

        Usuario actualizado = service.actualizar(creado.getId(), datos);

        assertEquals("tk4-cuatro@example.test", actualizado.getCorreo());
        assertEquals("0987654321", actualizado.getTelefono());
        assertEquals(hashOriginal, actualizado.getHashPassword());
        assertEquals(3, actualizado.getIntentosFallidos());
        assertTrue(actualizado.isDebeCambiarPassword());
    }

    @Test
    void asignarRoles_reemplazaRolesYCreaPerfilDocenteUnaSolaVez() {
        Usuario creado = service.crear(usuario("tk4-docente@example.test", "0999999996"), "ClaveTemporal123!");
        roles.agregar("DOCENTE", 1L);
        roles.agregar("ESTUDIANTE", 2L);

        service.asignarRoles(creado.getId(), List.of("DOCENTE", "ESTUDIANTE"));

        assertEquals(List.of("DOCENTE", "ESTUDIANTE"), service.rolesDe(creado.getId()));
        assertEquals(1, docentes.creados.size());
        assertEquals(creado.getId(), docentes.creados.get(0).getUsuarioId());

        service.asignarRoles(creado.getId(), List.of("docente"));
        assertEquals(List.of("DOCENTE"), service.rolesDe(creado.getId()));
        assertEquals(1, docentes.creados.size(), "El perfil existente no se duplica");
    }

    @Test
    void asignarRoles_rolInexistente_devuelve409() {
        Usuario creado = service.crear(usuario("tk4-roles@example.test", "0999999997"), "ClaveTemporal123!");

        ApiException error = assertThrows(ApiException.class,
                () -> service.asignarRoles(creado.getId(), List.of("ROL_FANTASMA")));

        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void asignarCarreras_reemplazaConjuntoCompleto() {
        Usuario creado = service.crear(usuario("tk4-carreras@example.test", "0999999998"), "ClaveTemporal123!");
        carreras.agregar(10L);
        carreras.agregar(20L);
        carreras.agregar(30L);

        service.asignarCarreras(creado.getId(), List.of(10L, 20L));
        assertEquals(List.of(10L, 20L), service.carrerasDe(creado.getId()));

        service.asignarCarreras(creado.getId(), List.of(30L));
        assertEquals(List.of(30L), service.carrerasDe(creado.getId()));
        assertFalse(usuarioCarreras.tiene(creado.getId(), 10L));
    }

    @Test
    void asignarCarreras_carreraInexistente_devuelve409() {
        Usuario creado = service.crear(usuario("tk4-carreras-invalidas@example.test", "0999999999"),
                "ClaveTemporal123!");

        ApiException error = assertThrows(ApiException.class,
                () -> service.asignarCarreras(creado.getId(), List.of(999L)));

        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void asignarRoles_modificacionValida_publicaUnaInvalidacionDelUsuarioModificado() {
        Usuario creado = service.crear(usuario("tk4-sesion-roles@example.test", "0999100001"),
                "ClaveTemporal123!");
        roles.agregar("ESTUDIANTE", 2L);

        service.asignarRoles(creado.getId(), List.of("ESTUDIANTE"));

        assertEquals(List.of("ESTUDIANTE"), service.rolesDe(creado.getId()));
        assertEquals(List.of(creado.getId()), eventos.usuariosNotificados);
    }

    @Test
    void asignarRoles_rolInexistente_noPublicaInvalidacion() {
        Usuario creado = service.crear(usuario("tk4-sesion-roles-error@example.test", "0999100002"),
                "ClaveTemporal123!");
        roles.agregar("ESTUDIANTE", 2L);
        service.asignarRoles(creado.getId(), List.of("ESTUDIANTE"));
        eventos.usuariosNotificados.clear();

        ApiException error = assertThrows(ApiException.class,
                () -> service.asignarRoles(creado.getId(), List.of("ROL_FANTASMA")));

        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
        assertEquals(List.of("ESTUDIANTE"), service.rolesDe(creado.getId()),
                "La validación fallida no debe modificar los roles");
        assertTrue(eventos.usuariosNotificados.isEmpty(), "Una operación fallida no publica invalidación");
    }

    @Test
    void desactivar_modificacionValida_publicaUnaInvalidacionDelUsuarioModificado() {
        Usuario creado = service.crear(usuario("tk4-sesion-baja@example.test", "0999100003"),
                "ClaveTemporal123!");
        assertTrue(creado.isActivo());

        service.desactivar(creado.getId());

        assertFalse(creado.isActivo());
        assertEquals(List.of(creado.getId()), eventos.usuariosNotificados);
    }

    @Test
    void desactivar_usuarioInexistente_noPublicaInvalidacion() {
        ApiException error = assertThrows(ApiException.class, () -> service.desactivar(12345L));

        assertEquals(404, error.getStatus());
        assertTrue(eventos.usuariosNotificados.isEmpty());
    }

    @Test
    void restablecerPassword_modificacionValida_publicaUnaInvalidacionDelUsuarioModificado() {
        Usuario creado = service.crear(usuario("tk4-sesion-clave@example.test", "0999100004"),
                "ClaveTemporal123!");
        String hashAnterior = creado.getHashPassword();

        service.restablecerPassword(creado.getId(), "ClaveNueva123!");

        assertNotEquals(hashAnterior, creado.getHashPassword());
        assertTrue(BcryptUtil.matches("ClaveNueva123!", creado.getHashPassword()));
        assertEquals(List.of(creado.getId()), eventos.usuariosNotificados);
    }

    @Test
    void restablecerPassword_passwordVacio_noPublicaInvalidacion() {
        Usuario creado = service.crear(usuario("tk4-sesion-clave-error@example.test", "0999100005"),
                "ClaveTemporal123!");
        String hashAnterior = creado.getHashPassword();

        ApiException error = assertThrows(ApiException.class,
                () -> service.restablecerPassword(creado.getId(), "   "));

        assertEquals(400, error.getStatus());
        assertEquals(hashAnterior, creado.getHashPassword());
        assertTrue(eventos.usuariosNotificados.isEmpty());
    }

    @Test
    void operacionesFueraDeAlcance_noPublicanInvalidacion() {
        Usuario creado = service.crear(usuario("tk4-sesion-alcance@example.test", "0999100006"),
                "ClaveTemporal123!");

        service.asignarCarreras(creado.getId(), List.of());
        service.activar(creado.getId());

        assertTrue(eventos.usuariosNotificados.isEmpty());
    }

    private static Usuario usuario(String correo, String documento) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setNombres("Usuario");
        usuario.setApellidos("Sintético");
        usuario.setTipoDocumento("CEDULA");
        usuario.setNumeroDocumento(documento);
        return usuario;
    }

    private static void asignarId(Object entidad, Long id) throws Exception {
        Field campo = entidad.getClass().getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(entidad, id);
    }

    private static final class UsuarioRepoFalso extends UsuarioRepository {

        private final Map<Long, Usuario> datos = new LinkedHashMap<>();
        private final Map<String, Usuario> porCorreo = new LinkedHashMap<>();
        private final Map<String, Usuario> porDocumento = new LinkedHashMap<>();
        private long siguiente = 1;
        private UsuarioRolRepoFalso usuarioRoles;
        private boolean fallarPersistencia;

        void usarUsuarioRoles(UsuarioRolRepoFalso usuarioRoles) {
            this.usuarioRoles = usuarioRoles;
        }

        void fallarPersistencia() {
            this.fallarPersistencia = true;
        }

        int cantidad() {
            return datos.size();
        }

        @Override
        public boolean existeUsuarioConRol(String codigoRol) {
            return usuarioRoles != null && usuarioRoles.tieneRol(codigoRol);
        }

        @Override
        public Usuario findById(Long id) {
            return datos.get(id);
        }

        @Override
        public Usuario buscarPorCorreo(String correo) {
            return porCorreo.get(correo);
        }

        @Override
        public Usuario buscarPorDocumento(String documento) {
            return porDocumento.get(documento);
        }

        @Override
        public void persistAndFlush(Usuario entidad) {
            if (fallarPersistencia) {
                throw new IllegalStateException("Falla sintética de persistencia.");
            }
            try {
                if (entidad.getId() == null) {
                    asignarId(entidad, siguiente++);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            datos.put(entidad.getId(), entidad);
            porCorreo.put(entidad.getCorreo(), entidad);
            if (entidad.getNumeroDocumento() != null) {
                porDocumento.put(entidad.getNumeroDocumento(), entidad);
            }
        }

        @Override
        public void flush() {
        }
    }

    private static final class RolRepoFalso extends RolRepository {

        private final Map<String, Rol> porCodigo = new LinkedHashMap<>();

        void agregar(String codigo, Long id) {
            Rol rol = new Rol();
            rol.setCodigo(codigo);
            try {
                asignarId(rol, id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            porCodigo.put(codigo, rol);
        }

        @Override
        public Rol buscarPorCodigo(String codigo) {
            return porCodigo.get(codigo);
        }

        @Override
        public Rol buscarPorCodigoBloqueante(String codigo) {
            return porCodigo.get(codigo);
        }
    }

    private static final class UsuarioRolRepoFalso extends UsuarioRolRepository {

        private final RolRepoFalso rolesRepo;
        private final List<UsuarioRol> relaciones = new ArrayList<>();

        UsuarioRolRepoFalso(RolRepoFalso rolesRepo) {
            this.rolesRepo = rolesRepo;
        }

        @Override
        public List<UsuarioRol> listarPorUsuario(Long usuarioId) {
            return relaciones.stream().filter(r -> r.getId().getUsuarioId().equals(usuarioId)).toList();
        }

        @Override
        public List<String> listarCodigosPorUsuario(Long usuarioId) {
            return listarPorUsuario(usuarioId).stream()
                    .map(r -> rolesRepo.porCodigo.entrySet().stream()
                            .filter(e -> e.getValue().getId().equals(r.getId().getRolId()))
                            .map(Map.Entry::getKey).findFirst().orElse("?"))
                    .sorted().toList();
        }

        boolean tieneRol(String codigo) {
            Rol rol = rolesRepo.porCodigo.get(codigo);
            if (rol == null) {
                return false;
            }
            return relaciones.stream().anyMatch(r -> r.getId().getRolId().equals(rol.getId()));
        }

        @Override
        public void persist(UsuarioRol entidad) {
            relaciones.add(entidad);
        }

        @Override
        public void delete(UsuarioRol entidad) {
            relaciones.remove(entidad);
        }

        @Override
        public void flush() {
        }
    }

    private static final class CarreraRepoFalso extends CarreraRepository {

        private final Map<Long, Carrera> datos = new LinkedHashMap<>();

        void agregar(Long id) {
            Carrera carrera = new Carrera();
            carrera.setCodigo("CAR-" + id);
            carrera.setNombre("Carrera " + id);
            carrera.setActiva(true);
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

        @Override
        public List<UsuarioCarrera> listarPorUsuario(Long usuarioId) {
            return relaciones.stream().filter(r -> r.getId().getUsuarioId().equals(usuarioId)).toList();
        }

        @Override
        public List<Long> listarCarrerasPorUsuario(Long usuarioId) {
            return listarPorUsuario(usuarioId).stream().map(r -> r.getId().getCarreraId()).sorted().toList();
        }

        boolean tiene(Long usuarioId, Long carreraId) {
            return relaciones.stream().anyMatch(r -> r.getId().getUsuarioId().equals(usuarioId)
                    && r.getId().getCarreraId().equals(carreraId));
        }

        @Override
        public void persist(UsuarioCarrera entidad) {
            relaciones.add(entidad);
        }

        @Override
        public void delete(UsuarioCarrera entidad) {
            relaciones.remove(entidad);
        }

        @Override
        public void flush() {
        }
    }

    private static final class DocenteRepoFalso extends DocenteRepository {

        private final List<Docente> creados = new ArrayList<>();

        @Override
        public Docente buscarPorUsuarioId(Long usuarioId) {
            return creados.stream().filter(d -> d.getUsuarioId().equals(usuarioId)).findFirst().orElse(null);
        }

        @Override
        public void persistAndFlush(Docente entidad) {
            try {
                asignarId(entidad, (long) (creados.size() + 1));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            creados.add(entidad);
        }
    }

    /** Evento CDI simulado: registra los usuarios notificados sin contenedor ni base de datos. */
    private static final class EventoFalso implements Event<SesionInvalidacionEvent> {

        private final List<Long> usuariosNotificados = new ArrayList<>();

        @Override
        public void fire(SesionInvalidacionEvent evento) {
            usuariosNotificados.add(evento.getUsuarioId());
        }

        @Override
        public <U extends SesionInvalidacionEvent> CompletionStage<U> fireAsync(U evento) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends SesionInvalidacionEvent> CompletionStage<U> fireAsync(U evento,
                NotificationOptions opciones) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Event<SesionInvalidacionEvent> select(Annotation... calificadores) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends SesionInvalidacionEvent> Event<U> select(Class<U> subtipo,
                Annotation... calificadores) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <U extends SesionInvalidacionEvent> Event<U> select(TypeLiteral<U> subtipo,
                Annotation... calificadores) {
            throw new UnsupportedOperationException();
        }
    }
}
