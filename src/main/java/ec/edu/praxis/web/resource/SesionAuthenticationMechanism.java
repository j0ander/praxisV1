package ec.edu.praxis.web.resource;

import java.security.Principal;
import java.security.Permission;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ec.edu.praxis.application.service.SesionStore;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adaptador único entre la cookie de sesión y el mecanismo de autenticación de
 * Quarkus. Las APIs internas quedan confinadas a esta clase (ARQUITECTURA.md §11).
 */
@ApplicationScoped
public class SesionAuthenticationMechanism implements HttpAuthenticationMechanism {

    /** Atributo propio para recuperar el identificador de usuario en cada petición. */
    private static final String USUARIO_ID_ATTRIBUTE = "praxis.usuarioId";

    @ConfigProperty(name = "praxis.seguridad.cookie.nombre")
    Optional<String> nombreCookie;

    @Inject
    SesionStore sesionStore;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            io.quarkus.security.identity.IdentityProviderManager manager) {
        String valor = nombreCookieRequerida();
        io.vertx.core.http.Cookie cookie = context.request().getCookie(valor);
        if (cookie == null) {
            return Uni.createFrom().nullItem();
        }
        SesionStore.Sesion sesion = sesionStore.buscar(cookie.getValue());
        if (sesion == null) {
            return Uni.createFrom().nullItem();
        }
        // Instancia nueva por petición: nunca se guarda en un campo del adaptador.
        return Uni.createFrom().item(() -> new IdentidadSesion(sesion));
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401));
    }

    private String nombreCookieRequerida() {
        return nombreCookie.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.cookie.nombre"));
    }

    /** Identidad mínima y por petición: identificador, correo y roles. */
    private static final class IdentidadSesion implements SecurityIdentity {

        private final SesionStore.Sesion sesion;
        private final Principal principal;

        IdentidadSesion(SesionStore.Sesion sesion) {
            this.sesion = sesion;
            this.principal = new Principal() {
                @Override
                public String getName() {
                    return sesion.getCorreo();
                }
            };
        }

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public Set<String> getRoles() {
            return Set.copyOf(sesion.getRoles());
        }

        @Override
        public boolean hasRole(String rol) {
            return sesion.getRoles().contains(rol);
        }

        @Override
        public Set<Permission> getPermissions() {
            return Set.of();
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> tipo) {
            return null;
        }

        @Override
        public Set<Credential> getCredentials() {
            return Set.of();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getAttribute(String nombre) {
            if (USER_ATTRIBUTE.equals(nombre)) {
                return (T) sesion.getCorreo();
            }
            if (USUARIO_ID_ATTRIBUTE.equals(nombre)) {
                return (T) sesion.getUsuarioId();
            }
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Map.of(USER_ATTRIBUTE, sesion.getCorreo(),
                    USUARIO_ID_ATTRIBUTE, sesion.getUsuarioId());
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permiso) {
            return Uni.createFrom().item(false);
        }
    }
}
