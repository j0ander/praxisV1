package ec.edu.praxis.web.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.application.service.SesionStore;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/** Pruebas unitarias de la identidad por petición del mecanismo de autenticación. */
class IdentidadSesionTest {

    private static final String COOKIE = "PRAXIS_SESSION";

    private SesionStore store;
    private SesionAuthenticationMechanism mecanismo;

    @BeforeEach
    void preparar() throws Exception {
        store = new SesionStore();
        Field duracion = SesionStore.class.getDeclaredField("duracion");
        duracion.setAccessible(true);
        duracion.set(store, Optional.of(Duration.ofMinutes(30)));

        mecanismo = new SesionAuthenticationMechanism();
        mecanismo.nombreCookie = Optional.of(COOKIE);
        mecanismo.sesionStore = store;
    }

    @Test
    void autenticar_dosSesiones_devuelveIdentidadesIndependientes() throws Exception {
        String idAdmin = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));
        String idEstudiante = store.crear(2L, "estudiante.prueba@example.test",
                List.of("ESTUDIANTE"));

        SecurityIdentity admin = resolver(mecanismo.authenticate(contexto(idAdmin), null));
        SecurityIdentity estudiante = resolver(mecanismo.authenticate(contexto(idEstudiante), null));

        assertNotNull(admin);
        assertNotNull(estudiante);
        assertNotSame(admin, estudiante);
        assertNotSame(admin.getPrincipal(), estudiante.getPrincipal());
        assertEquals("admin.prueba@example.test", admin.getPrincipal().getName());
        assertEquals("estudiante.prueba@example.test", estudiante.getPrincipal().getName());
        assertEquals(Long.valueOf(1L), (Long) admin.getAttribute("praxis.usuarioId"));
        assertEquals(Long.valueOf(2L), (Long) estudiante.getAttribute("praxis.usuarioId"));
    }

    @Test
    void autenticar_rolesDeUnaSesionNoContaminanOtra() throws Exception {
        String idAdmin = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));
        String idEstudiante = store.crear(2L, "estudiante.prueba@example.test",
                List.of("ESTUDIANTE"));

        SecurityIdentity admin = resolver(mecanismo.authenticate(contexto(idAdmin), null));
        SecurityIdentity estudiante = resolver(mecanismo.authenticate(contexto(idEstudiante), null));

        assertTrue(admin.getRoles().contains("ADMINISTRADOR"));
        assertFalse(admin.getRoles().contains("ESTUDIANTE"));
        assertTrue(estudiante.getRoles().contains("ESTUDIANTE"));
        assertFalse(estudiante.getRoles().contains("ADMINISTRADOR"));
        assertTrue(admin.hasRole("ADMINISTRADOR"));
        assertFalse(admin.hasRole("ESTUDIANTE"));
    }

    @Test
    void autenticar_sinCookie_devuelveIdentidadNula() throws Exception {
        assertNull(resolver(mecanismo.authenticate(contexto(null), null)));
    }

    @Test
    void autenticar_cookieInvalida_devuelveIdentidadNula() throws Exception {
        store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        assertNull(resolver(mecanismo.authenticate(contexto("no-existe"), null)));
    }

    @Test
    void autenticar_dosPeticionesConLaMismaCookie_creaIdentidadesDistintas() throws Exception {
        String id = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        SecurityIdentity primera = resolver(mecanismo.authenticate(contexto(id), null));
        SecurityIdentity segunda = resolver(mecanismo.authenticate(contexto(id), null));

        assertNotSame(primera, segunda);
        assertNotSame(primera.getPrincipal(), segunda.getPrincipal());
        assertEquals(primera.getRoles(), segunda.getRoles());
    }

    private static SecurityIdentity resolver(Uni<SecurityIdentity> uni) throws InterruptedException {
        AtomicReference<SecurityIdentity> resultado = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        uni.subscribe().with(identidad -> {
            resultado.set(identidad);
            latch.countDown();
        }, error -> latch.countDown());
        assertTrue(latch.await(5, TimeUnit.SECONDS), "La autenticación debía completarse");
        return resultado.get();
    }

    private static RoutingContext contexto(String valorCookie) {
        io.vertx.core.http.Cookie cookie = (io.vertx.core.http.Cookie) java.lang.reflect.Proxy
                .newProxyInstance(io.vertx.core.http.Cookie.class.getClassLoader(),
                        new Class<?>[] { io.vertx.core.http.Cookie.class },
                        (proxy, method, args) -> "getValue".equals(method.getName()) ? valorCookie : valorPorDefecto(method));
        HttpServerRequest request = (HttpServerRequest) java.lang.reflect.Proxy
                .newProxyInstance(HttpServerRequest.class.getClassLoader(),
                        new Class<?>[] { HttpServerRequest.class },
                        (proxy, method, args) -> "getCookie".equals(method.getName()) ? cookie : valorPorDefecto(method));
        return (RoutingContext) java.lang.reflect.Proxy
                .newProxyInstance(RoutingContext.class.getClassLoader(),
                        new Class<?>[] { RoutingContext.class },
                        (proxy, method, args) -> "request".equals(method.getName()) ? request : valorPorDefecto(method));
    }

    private static Object valorPorDefecto(java.lang.reflect.Method method) {
        Class<?> tipo = method.getReturnType();
        if (tipo == boolean.class) {
            return false;
        }
        if (tipo == int.class) {
            return 0;
        }
        if (tipo == long.class) {
            return 0L;
        }
        return null;
    }
}
