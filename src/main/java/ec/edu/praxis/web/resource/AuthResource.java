package ec.edu.praxis.web.resource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.AutenticacionService;
import ec.edu.praxis.application.service.AutenticacionService.ResultadoAutenticacion;
import ec.edu.praxis.domain.model.Usuario;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/** API-001 a API-003: inicio y cierre de sesión y datos del usuario actual. */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AutenticacionService service;

    @Context
    HttpHeaders headers;

    @Context
    UriInfo uriInfo;

    @ConfigProperty(name = "praxis.seguridad.cookie.nombre")
    Optional<String> nombreCookie;

    @ConfigProperty(name = "praxis.seguridad.cookie.max-age")
    Optional<Duration> maxAgeCookie;

    @ConfigProperty(name = "praxis.seguridad.cookie.same-site")
    Optional<String> sameSite;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(CredencialesRequest datos) {
        if (datos == null || datos.correo == null || datos.password == null) {
            throw ApiException.datosInvalidos("Correo y contraseña son obligatorios.");
        }
        ResultadoAutenticacion resultado = service.login(datos.correo, datos.password);
        if (!resultado.isAutenticado()) {
            // Mensaje genérico: no distingue cuenta inexistente, inactiva ni bloqueada.
            throw ApiException.sinSesion("Credenciales inválidas.");
        }
        return Response.ok(new SesionResponse(resultado.getUsuario(), resultado.getRoles()))
                .header("Set-Cookie", cabeceraCookie(resultado.getIdentificadorSesion(), maxAgeRequerido()))
                .build();
    }

    @POST
    @Path("/logout")
    @Authenticated
    public Response logout() {
        service.logout(identificadorCookie());
        return Response.noContent()
                .header("Set-Cookie", cabeceraCookie("", 0))
                .build();
    }

    @GET
    @Path("/yo")
    @Authenticated
    public Response yo() {
        Long usuarioId = service.usuarioDeSesion(identificadorCookie());
        ResultadoAutenticacion resultado = service.obtenerDatos(usuarioId);
        return Response.ok(new SesionResponse(resultado.getUsuario(), resultado.getRoles())).build();
    }

    private String identificadorCookie() {
        Map<String, Cookie> cookies = headers.getCookies();
        if (cookies == null) {
            return null;
        }
        Cookie cookie = cookies.get(nombreCookieRequerida());
        return cookie == null ? null : cookie.getValue();
    }

    private String cabeceraCookie(String valor, long maxAgeSegundos) {
        StringBuilder cabecera = new StringBuilder(nombreCookieRequerida())
                .append('=').append(valor)
                .append("; Path=/; HttpOnly")
                .append("; Max-Age=").append(maxAgeSegundos)
                .append("; SameSite=").append(sameSiteRequerida());
        if ("https".equalsIgnoreCase(uriInfo.getRequestUri().getScheme())) {
            cabecera.append("; Secure");
        }
        return cabecera.toString();
    }

    private String nombreCookieRequerida() {
        return nombreCookie.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.cookie.nombre"));
    }

    private String sameSiteRequerida() {
        return sameSite.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.cookie.same-site"));
    }

    private long maxAgeRequerido() {
        return maxAgeCookie.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.cookie.max-age")).toSeconds();
    }

    /** Entrada sencilla: nunca se recibe la entidad Usuario. */
    public static class CredencialesRequest {

        private String correo;
        private String password;

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Salida básica y explícita: usuarioId, correo, nombres y roles. No se
     * serializa la entidad Usuario, por lo que no se exponen hash, intentos,
     * bloqueo ni datos de otras tablas.
     */
    public static class SesionResponse {

        private final Long usuarioId;
        private final String correo;
        private final String nombres;
        private final List<String> roles;

        SesionResponse(Usuario usuario, List<String> roles) {
            this.usuarioId = usuario.getId();
            this.correo = usuario.getCorreo();
            this.nombres = usuario.getNombres();
            this.roles = List.copyOf(roles);
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public String getCorreo() {
            return correo;
        }

        public String getNombres() {
            return nombres;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
