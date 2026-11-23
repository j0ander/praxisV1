package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del almacén de sesiones en memoria, sin PostgreSQL. */
class SesionStoreTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T12:00:00Z");

    private SesionStore store;

    @BeforeEach
    void preparar() {
        store = new SesionStore();
        store.duracion = Optional.of(Duration.ofMinutes(30));
        store.usarReloj(Clock.fixed(INICIO, ZoneOffset.UTC));
    }

    @Test
    void crear_recuperaSesionValida() {
        String id = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        assertNotNull(id);
        assertFalse(id.isBlank());
        SesionStore.Sesion sesion = store.buscar(id);
        assertNotNull(sesion);
        assertEquals(1L, sesion.getUsuarioId());
        assertEquals("admin.prueba@example.test", sesion.getCorreo());
        assertEquals(List.of("ADMINISTRADOR"), sesion.getRoles());
        assertEquals(1, store.cantidadSesiones());
    }

    @Test
    void crear_generaIdentificadoresAleatoriosYDiferentes() {
        List<String> identificadores = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            identificadores.add(store.crear((long) i, "u" + i + "@example.test", List.of("ESTUDIANTE")));
        }

        assertEquals(200, identificadores.size());
        assertEquals(200, Set.copyOf(identificadores).size(),
                "Los identificadores de sesión deben ser únicos");
        assertTrue(identificadores.stream().noneMatch(String::isBlank));
    }

    @Test
    void buscar_identificadorInexistente_devuelveNull() {
        store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        assertNull(store.buscar("no-existe"));
        assertNull(store.buscar(null));
    }

    @Test
    void buscar_sesionDentroDeLaDuracion_sigueVigente() {
        String id = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        store.usarReloj(Clock.fixed(INICIO.plus(Duration.ofMinutes(29)), ZoneOffset.UTC));

        assertNotNull(store.buscar(id), "A los 29 minutos la sesión de 30 minutos sigue vigente");
    }

    @Test
    void buscar_sesionVencida_devuelveNull() {
        String id = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        store.usarReloj(Clock.fixed(INICIO.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));

        assertNull(store.buscar(id), "A los 31 minutos la sesión debe estar vencida");
        assertEquals(0, store.cantidadSesiones());
    }

    @Test
    void crear_limpiaSesionesVencidas() {
        String vencida = store.crear(1L, "uno@example.test", List.of("ESTUDIANTE"));
        store.usarReloj(Clock.fixed(INICIO.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));
        String vigente = store.crear(2L, "dos@example.test", List.of("ADMINISTRADOR"));

        assertNull(store.buscar(vencida));
        assertNotNull(store.buscar(vigente));
        assertEquals(1, store.cantidadSesiones());
    }

    @Test
    void invalidar_logoutEliminaSesion() {
        String id = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));
        assertNotNull(store.buscar(id));

        store.invalidar(id);

        assertNull(store.buscar(id));
        assertEquals(0, store.cantidadSesiones());
    }

    @Test
    void invalidar_identificadorNulo_noAfectaOtrasSesiones() {
        String id = store.crear(1L, "admin.prueba@example.test", List.of("ADMINISTRADOR"));

        store.invalidar(null);

        assertNotNull(store.buscar(id));
    }

    @Test
    void invalidarPorUsuario_eliminaTodasLasSesionesDelUsuario_yConservaLasDemas() {
        String u1a = store.crear(1L, "u1a@example.test", List.of("ADMINISTRADOR"));
        String u1b = store.crear(1L, "u1b@example.test", List.of("ADMINISTRADOR"));
        String u2a = store.crear(2L, "u2a@example.test", List.of("ESTUDIANTE"));
        String u2b = store.crear(2L, "u2b@example.test", List.of("ESTUDIANTE"));
        assertEquals(4, store.cantidadSesiones());

        store.invalidarPorUsuario(1L);

        assertNull(store.buscar(u1a));
        assertNull(store.buscar(u1b));
        assertNotNull(store.buscar(u2a));
        assertNotNull(store.buscar(u2b));
        assertEquals(2, store.cantidadSesiones());
    }

    @Test
    void invalidarPorUsuario_usuarioSinSesiones_noModificaElAlmacen() {
        String id = store.crear(1L, "uno@example.test", List.of("ESTUDIANTE"));

        store.invalidarPorUsuario(999L);

        assertNotNull(store.buscar(id));
        assertEquals(1, store.cantidadSesiones());
    }

    @Test
    void invalidarPorUsuario_identificadorInvalido_noModificaElAlmacen() {
        String id = store.crear(1L, "uno@example.test", List.of("ESTUDIANTE"));

        store.invalidarPorUsuario(null);
        store.invalidarPorUsuario(0L);
        store.invalidarPorUsuario(-5L);

        assertNotNull(store.buscar(id));
        assertEquals(1, store.cantidadSesiones());
    }

    @Test
    void sesion_copiaRolesParaEvitarContaminacion() {
        List<String> roles = new ArrayList<>(List.of("ADMINISTRADOR"));
        String id = store.crear(1L, "admin.prueba@example.test", roles);

        roles.add("ESTUDIANTE");

        assertEquals(List.of("ADMINISTRADOR"), store.buscar(id).getRoles());
    }

    @Test
    void crear_sesionesDiferentes_noCompartenInstancia() {
        String primero = store.crear(1L, "uno@example.test", List.of("ADMINISTRADOR"));
        String segundo = store.crear(2L, "dos@example.test", List.of("ESTUDIANTE"));

        SesionStore.Sesion sesionUno = store.buscar(primero);
        SesionStore.Sesion sesionDos = store.buscar(segundo);

        assertNotSame(sesionUno, sesionDos);
        assertEquals(List.of("ADMINISTRADOR"), sesionUno.getRoles());
        assertEquals(List.of("ESTUDIANTE"), sesionDos.getRoles());
    }

    // --- Generaciones de sesión (carrera login ↔ invalidación) ---

    @Test
    void crear_generacionObsoleta_rechazaLaSesion() {
        long generacionLeida = store.generacionActual(1L);

        store.invalidarPorUsuario(1L);

        assertNull(store.crear(1L, "u@example.test", List.of("ADMINISTRADOR"), generacionLeida));
        assertEquals(0, store.cantidadSesiones());
    }

    @Test
    void crear_generacionVigente_registraSesionQueSeInvalidaDespues() {
        long generacion = store.generacionActual(1L);
        String id = store.crear(1L, "u@example.test", List.of("ADMINISTRADOR"), generacion);

        assertNotNull(id);
        SesionStore.Sesion sesion = store.buscar(id);
        assertNotNull(sesion);
        assertEquals(generacion, sesion.getGeneracion());

        store.invalidarPorUsuario(1L);

        assertNull(store.buscar(id));
        assertEquals(0, store.cantidadSesiones());
    }

    @Test
    void invalidarPorUsuario_concreacionConcurrente_noDejaSesionesObsoletas() throws Exception {
        long generacionLeida = store.generacionActual(1L);
        CountDownLatch generacionLeidaLatch = new CountDownLatch(1);
        CountDownLatch invalidacionListaLatch = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> identificadores = new ConcurrentLinkedQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var creador = executor.submit(() -> {
                generacionLeidaLatch.countDown();
                esperar(invalidacionListaLatch);
                for (int i = 0; i < 5; i++) {
                    String id = store.crear(1L, "u" + i + "@example.test",
                            List.of("ADMINISTRADOR"), generacionLeida);
                    if (id != null) {
                        identificadores.add(id);
                    }
                }
            });
            var invalidador = executor.submit(() -> {
                esperar(generacionLeidaLatch);
                store.invalidarPorUsuario(1L);
                invalidacionListaLatch.countDown();
            });

            creador.get(10, TimeUnit.SECONDS);
            invalidador.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(identificadores.isEmpty(),
                "La invalidación ya confirmada impide registrar sesiones con la generación antigua");
        assertEquals(0, store.cantidadSesiones());
        assertTrue(store.generacionActual(1L) > generacionLeida);
    }

    @Test
    void invalidarPorUsuario_noCambiaGeneracionDeOtrosUsuarios() {
        long generacionUno = store.generacionActual(1L);
        long generacionDos = store.generacionActual(2L);
        String idDos = store.crear(2L, "dos@example.test", List.of("ESTUDIANTE"), generacionDos);

        store.invalidarPorUsuario(1L);

        assertEquals(generacionDos, store.generacionActual(2L));
        assertNotNull(store.buscar(idDos));
        assertTrue(store.generacionActual(1L) > generacionUno);
    }

    @Test
    void invalidarPorUsuario_conVariasSesiones_delUsuario_invalidaTodas() {
        long generacion = store.generacionActual(1L);
        String a = store.crear(1L, "a@example.test", List.of("ESTUDIANTE"), generacion);
        String b = store.crear(1L, "b@example.test", List.of("ESTUDIANTE"), generacion);
        String c = store.crear(1L, "c@example.test", List.of("ESTUDIANTE"), generacion);

        store.invalidarPorUsuario(1L);

        assertNull(store.buscar(a));
        assertNull(store.buscar(b));
        assertNull(store.buscar(c));
        assertEquals(0, store.cantidadSesiones());
    }

    @Test
    void invalidar_logoutNoCambiaGeneracion_niAfectaOtrasSesionesDelUsuario() {
        long generacion = store.generacionActual(1L);
        String a = store.crear(1L, "a@example.test", List.of("ESTUDIANTE"), generacion);
        String b = store.crear(1L, "b@example.test", List.of("ESTUDIANTE"), generacion);

        store.invalidar(a);

        assertNull(store.buscar(a));
        assertNotNull(store.buscar(b));
        assertEquals(generacion, store.generacionActual(1L));
    }

    @Test
    void buscar_sesionVencidaConGeneracionVigente_devuelveNull() {
        long generacion = store.generacionActual(1L);
        String id = store.crear(1L, "u@example.test", List.of("ESTUDIANTE"), generacion);

        store.usarReloj(Clock.fixed(INICIO.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));

        assertNull(store.buscar(id));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_sesionConGeneracionObsoletaEnElMapa_laDescarta() throws Exception {
        long generacion = store.generacionActual(1L);
        String id = store.crear(1L, "u@example.test", List.of("ESTUDIANTE"), generacion);

        Field campoGeneraciones = SesionStore.class.getDeclaredField("generaciones");
        campoGeneraciones.setAccessible(true);
        ConcurrentHashMap<Long, Long> generaciones =
                (ConcurrentHashMap<Long, Long>) campoGeneraciones.get(store);
        generaciones.put(1L, generacion + 1);

        assertNull(store.buscar(id), "Una sesión con generación anterior no debe recuperarse");

        Field campoSesiones = SesionStore.class.getDeclaredField("sesiones");
        campoSesiones.setAccessible(true);
        ConcurrentHashMap<String, SesionStore.Sesion> sesiones =
                (ConcurrentHashMap<String, SesionStore.Sesion>) campoSesiones.get(store);
        assertFalse(sesiones.containsKey(id), "La sesión obsoleta debe eliminarse del almacén");
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("La sincronización entre hilos no se completó");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Hilo interrumpido durante la sincronización", e);
        }
    }
}
