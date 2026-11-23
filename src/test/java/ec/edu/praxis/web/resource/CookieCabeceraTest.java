package ec.edu.praxis.web.resource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.UriInfo;

/** Pruebas unitarias de la cabecera Set-Cookie generada por AuthResource. */
class CookieCabeceraTest {

    private AuthResource resource;
    private Method cabeceraCookie;

    @BeforeEach
    void preparar() throws Exception {
        resource = new AuthResource();
        resource.nombreCookie = Optional.of("PRAXIS_SESSION");
        resource.sameSite = Optional.of("Lax");
        resource.uriInfo = uriInfo("http://localhost:8081/auth/login");
        cabeceraCookie = AuthResource.class.getDeclaredMethod("cabeceraCookie", String.class, long.class);
        cabeceraCookie.setAccessible(true);
    }

    @Test
    void cookie_http_incluyeHttpOnlyPathSameSiteYMaxAge1800() throws Exception {
        String cabecera = cabecera("identificador-opaco", 1800L);

        assertTrue(cabecera.startsWith("PRAXIS_SESSION=identificador-opaco"));
        assertTrue(cabecera.contains("; Path=/"));
        assertTrue(cabecera.contains("; HttpOnly"));
        assertTrue(cabecera.contains("; SameSite=Lax"));
        assertTrue(cabecera.contains("; Max-Age=1800"));
        assertFalse(cabecera.contains("; Secure"));
    }

    @Test
    void cookie_https_incluyeSecure() throws Exception {
        resource.uriInfo = uriInfo("https://praxis.example/auth/login");

        String cabecera = cabecera("identificador-opaco", 1800L);

        assertTrue(cabecera.contains("; Secure"));
        assertTrue(cabecera.contains("; HttpOnly"));
    }

    @Test
    void logout_expiraLaCookieConMaxAgeCero() throws Exception {
        String cabecera = cabecera("", 0L);

        assertTrue(cabecera.startsWith("PRAXIS_SESSION=;"));
        assertTrue(cabecera.contains("; Max-Age=0"));
        assertTrue(cabecera.contains("; HttpOnly"));
    }

    private String cabecera(String valor, long maxAge) throws Exception {
        return (String) cabeceraCookie.invoke(resource, valor, maxAge);
    }

    private static UriInfo uriInfo(String uri) {
        return (UriInfo) java.lang.reflect.Proxy.newProxyInstance(
                UriInfo.class.getClassLoader(),
                new Class<?>[] { UriInfo.class },
                (proxy, method, args) -> "getRequestUri".equals(method.getName())
                        ? URI.create(uri)
                        : null);
    }
}
