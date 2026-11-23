package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import io.quarkus.elytron.security.common.BcryptUtil;

/** Pruebas unitarias de las reglas que no requieren PostgreSQL. */
class AutenticacionServiceTest {

    private AutenticacionService service;

    @BeforeEach
    void preparar() {
        service = new AutenticacionService();
        service.maximoIntentos = Optional.of(5);
        service.duracionBloqueo = Optional.of(Duration.ofMinutes(15));
        service.sesionStore = new SesionStore();
        service.sesionStore.duracion = Optional.of(Duration.ofMinutes(30));
    }

    @Test
    void normalizar_recortaEspaciosyConvierteAMinusculas() {
        assertEquals("admin.prueba@example.test",
                AutenticacionService.normalizar("  Admin.Prueba@Example.Test  "));
    }

    @Test
    void normalizar_correoYaNormalizado_noCambia() {
        assertEquals("estudiante.prueba@example.test",
                AutenticacionService.normalizar("estudiante.prueba@example.test"));
    }

    @Test
    void login_correoNulo_lanzaDatosInvalidos() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.login(null, "ClavePrueba123!"));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void login_correoEnBlanco_lanzaDatosInvalidos() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.login("   ", "ClavePrueba123!"));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void login_passwordNula_lanzaDatosInvalidos() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.login("admin.prueba@example.test", null));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void login_passwordVacia_lanzaDatosInvalidos() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.login("admin.prueba@example.test", ""));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void resultadoFallo_noAutenticadoYSinDatosDeSesion() {
        AutenticacionService.ResultadoAutenticacion resultado =
                AutenticacionService.ResultadoAutenticacion.fallo();

        assertFalse(resultado.isAutenticado());
        assertNull(resultado.getIdentificadorSesion());
        assertNull(resultado.getUsuario());
        assertTrue(resultado.getRoles().isEmpty());
    }

    @Test
    void resultadoExito_exponeSoloDatosNecesarios() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("admin.prueba@example.test");
        usuario.setNombres("Admin");
        usuario.setHashPassword("hash-que-no-debe-salir");
        usuario.setIntentosFallidos(3);

        AutenticacionService.ResultadoAutenticacion resultado =
                AutenticacionService.ResultadoAutenticacion.exito("id-opaco", usuario,
                        List.of("ADMINISTRADOR"));

        assertTrue(resultado.isAutenticado());
        assertEquals("id-opaco", resultado.getIdentificadorSesion());
        assertNotNull(resultado.getUsuario());
        assertEquals("admin.prueba@example.test", resultado.getUsuario().getCorreo());
        assertEquals(3, resultado.getUsuario().getIntentosFallidos(),
                "El resultado conserva la entidad para el Resource; la salida JSON la filtra");
        assertEquals(List.of("ADMINISTRADOR"), resultado.getRoles());
    }

    @Test
    void usuarioDeSesion_sinSesion_lanzaSinSesion() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.usuarioDeSesion("no-existe"));

        assertEquals(401, error.getStatus());
        assertEquals("SIN_SESION", error.getCodigo());
    }

    @Test
    void logout_sinIdentificador_noLanza() {
        service.logout(null);
    }

    @Test
    void login_sinInvalidacion_registraSesionConLaGeneracionLeida() {
        prepararLogin(new UsuarioRolRepositoryFalso(List.of("ADMINISTRADOR"), null));

        AutenticacionService.ResultadoAutenticacion resultado =
                service.login("admin.prueba@example.test", CLAVE);

        assertTrue(resultado.isAutenticado());
        assertNotNull(resultado.getIdentificadorSesion());
        assertNotNull(service.sesionStore.buscar(resultado.getIdentificadorSesion()));
        assertEquals(1, service.sesionStore.cantidadSesiones());
    }

    @Test
    void login_invalidacionEntreLecturaDeGeneracionYRoles_devuelveFalloYSinSesion() {
        SesionStore store = new SesionStore();
        store.duracion = Optional.of(Duration.ofMinutes(30));
        service.sesionStore = store;
        service.usuarioRepository = new UsuarioRepositoryFalso(usuarioParaLogin());
        service.usuarioRolRepository = new UsuarioRolRepositoryFalso(
                List.of("ADMINISTRADOR"), () -> store.invalidarPorUsuario(1L));

        AutenticacionService.ResultadoAutenticacion resultado =
                service.login("admin.prueba@example.test", CLAVE);

        assertFalse(resultado.isAutenticado(), "La generación quedó obsoleta antes de crear la sesión");
        assertNull(resultado.getIdentificadorSesion());
        assertTrue(resultado.getRoles().isEmpty(), "El fallo no revela roles ni datos de sesión");
        assertEquals(0, service.sesionStore.cantidadSesiones());
    }

    private static final String CLAVE = "ClavePrueba123!";

    private void prepararLogin(UsuarioRolRepositoryFalso repositorioRoles) {
        service.usuarioRepository = new UsuarioRepositoryFalso(usuarioParaLogin());
        service.usuarioRolRepository = repositorioRoles;
    }

    private static Usuario usuarioParaLogin() {
        Usuario usuario = new Usuario() {
            @Override
            public Long getId() {
                return 1L;
            }
        };
        usuario.setCorreo("admin.prueba@example.test");
        usuario.setHashPassword(BcryptUtil.bcryptHash(CLAVE));
        usuario.setNombres("Admin");
        usuario.setApellidos("Prueba");
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        return usuario;
    }

    /** Repositorio falso: evita PostgreSQL y mantiene el flujo de login determinista. */
    private static final class UsuarioRepositoryFalso extends UsuarioRepository {

        private final Usuario usuario;

        UsuarioRepositoryFalso(Usuario usuario) {
            this.usuario = usuario;
        }

        @Override
        public Usuario buscarPorCorreo(String correo) {
            return usuario;
        }

        @Override
        public void flush() {
            // Sin base de datos.
        }
    }

    /** Dispara una acción al consultar roles, para simular un commit concurrent. */
    private static final class UsuarioRolRepositoryFalso extends UsuarioRolRepository {

        private final List<String> roles;
        private final Runnable accionAlConsultar;

        UsuarioRolRepositoryFalso(List<String> roles, Runnable accionAlConsultar) {
            this.roles = roles;
            this.accionAlConsultar = accionAlConsultar;
        }

        @Override
        public List<String> listarCodigosPorUsuario(Long usuarioId) {
            if (accionAlConsultar != null) {
                accionAlConsultar.run();
            }
            return roles;
        }
    }
}
