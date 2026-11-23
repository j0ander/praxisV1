package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/**
 * Pruebas unitarias del efecto del observador. No simulan el ciclo de vida
 * transaccional de CDI: verifican qué hace el observador cuando es invocado y
 * que su anotación solicita AFTER_SUCCESS.
 */
class SesionInvalidacionObserverTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T12:00:00Z");

    private SesionStore store;
    private SesionInvalidacionObserver observer;

    @BeforeEach
    void preparar() {
        store = new SesionStore();
        store.duracion = Optional.of(Duration.ofMinutes(30));
        store.usarReloj(Clock.fixed(INICIO, ZoneOffset.UTC));
        observer = new SesionInvalidacionObserver();
        observer.sesionStore = store;
    }

    @Test
    void alConfirmar_invalidaSoloLasSesionesDelUsuarioDelEvento() {
        String u1a = store.crear(1L, "u1a@example.test", List.of("ADMINISTRADOR"));
        String u1b = store.crear(1L, "u1b@example.test", List.of("ADMINISTRADOR"));
        String u2 = store.crear(2L, "u2@example.test", List.of("ESTUDIANTE"));

        observer.alConfirmar(new SesionInvalidacionEvent(1L));

        assertNull(store.buscar(u1a));
        assertNull(store.buscar(u1b));
        assertNotNull(store.buscar(u2));
        assertEquals(1, store.cantidadSesiones());
    }

    @Test
    void alConfirmar_eventoDeUsuarioSinSesiones_noAfectaOtrasSesiones() {
        String u1 = store.crear(1L, "u1@example.test", List.of("ESTUDIANTE"));

        observer.alConfirmar(new SesionInvalidacionEvent(777L));

        assertNotNull(store.buscar(u1));
        assertEquals(1, store.cantidadSesiones());
    }

    @Test
    void alConfirmar_observadorSolicitaAfterSuccess() throws Exception {
        Method metodo = SesionInvalidacionObserver.class.getMethod(
                "alConfirmar", SesionInvalidacionEvent.class);

        Observes observa = metodo.getParameters()[0].getAnnotation(Observes.class);

        assertNotNull(observa, "El parámetro debe declararse como observador CDI");
        assertEquals(TransactionPhase.AFTER_SUCCESS, observa.during());
    }
}
