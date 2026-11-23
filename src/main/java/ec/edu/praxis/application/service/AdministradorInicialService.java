package ec.edu.praxis.application.service;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Inicialización opcional del primer administrador durante el arranque. */
@ApplicationScoped
public class AdministradorInicialService {

    @Inject
    UsuarioService usuarioService;

    @ConfigProperty(name = "praxis.seguridad.admin-inicial.enabled", defaultValue = "false")
    boolean habilitado;

    /* Punto de prueba: en producción se resuelven después de validar 'habilitado'. */
    Optional<String> correo;
    Optional<String> password;

    public void alIniciar(@Observes StartupEvent evento) {
        if (!habilitado) {
            return;
        }
        Config config = ConfigProvider.getConfig();
        if (correo == null) {
            correo = config.getOptionalValue("ADMIN_CORREO", String.class);
        }
        if (password == null) {
            password = config.getOptionalValue("ADMIN_PASSWORD", String.class);
        }
        if (correo.isEmpty() || password.isEmpty()
                || correo.get().isBlank() || password.get().isBlank()) {
            throw new IllegalStateException("Inicialización de administrador habilitada sin "
                    + "ADMIN_CORREO o ADMIN_PASSWORD. No se creó ninguna credencial.");
        }

        usuarioService.crearAdministradorInicial(correo.get(), password.get());
    }
}
