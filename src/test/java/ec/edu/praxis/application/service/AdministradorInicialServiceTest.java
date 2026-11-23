package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del observador de arranque, sin PostgreSQL. */
class AdministradorInicialServiceTest {

    private static final String VALOR_SINTETICO = "valor-sintetico-no-real";

    private AdministradorInicialService service;
    private UsuarioServiceFalso usuarios;

    @BeforeEach
    void preparar() {
        service = new AdministradorInicialService();
        usuarios = new UsuarioServiceFalso();
        service.usuarioService = usuarios;
        service.habilitado = false;
        service.correo = Optional.empty();
        service.password = Optional.empty();
    }

    @Test
    void deshabilitado_noLlamaAlServicio() {
        service.alIniciar(null);

        assertFalse(usuarios.llamado);
    }

    @Test
    void habilitadoSinCorreo_detieneElArranqueConMensajeSeguro() {
        service.habilitado = true;
        service.correo = Optional.empty();
        service.password = Optional.of(VALOR_SINTETICO);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.alIniciar(null));

        assertTrue(error.getMessage().contains("ADMIN_CORREO"));
        assertTrue(error.getMessage().contains("ADMIN_PASSWORD"));
        assertFalse(error.getMessage().contains(VALOR_SINTETICO));
        assertFalse(usuarios.llamado);
    }

    @Test
    void habilitadoSinPassword_detieneElArranqueConMensajeSeguro() {
        service.habilitado = true;
        service.correo = Optional.of("admin@example.test");
        service.password = Optional.empty();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.alIniciar(null));

        assertTrue(error.getMessage().contains("ADMIN_CORREO"));
        assertTrue(error.getMessage().contains("ADMIN_PASSWORD"));
        assertFalse(usuarios.llamado);
    }

    @Test
    void habilitadoConVariablesEnBlanco_detieneElArranque() {
        service.habilitado = true;
        service.correo = Optional.of("   ");
        service.password = Optional.of("   ");

        assertThrows(IllegalStateException.class, () -> service.alIniciar(null));

        assertFalse(usuarios.llamado);
    }

    @Test
    void habilitadoConCredenciales_delegaLaCreacion() {
        service.habilitado = true;
        service.correo = Optional.of("admin@example.test");
        service.password = Optional.of(VALOR_SINTETICO);

        service.alIniciar(null);

        assertTrue(usuarios.llamado);
        assertEquals("admin@example.test", usuarios.correoRecibido);
    }

    @Test
    void administradorExistente_noPropagaError() {
        service.habilitado = true;
        service.correo = Optional.of("admin@example.test");
        service.password = Optional.of(VALOR_SINTETICO);
        usuarios.resultado = false;

        service.alIniciar(null);

        assertTrue(usuarios.llamado);
    }

    private static final class UsuarioServiceFalso extends UsuarioService {

        private boolean llamado;
        private String correoRecibido;
        private boolean resultado = true;

        @Override
        public boolean crearAdministradorInicial(String correo, String password) {
            llamado = true;
            correoRecibido = correo;
            return resultado;
        }
    }
}
