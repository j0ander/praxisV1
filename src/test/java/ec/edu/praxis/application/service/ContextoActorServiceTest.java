package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.infrastructure.repository.ContextoActorRepository;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/** Pruebas unitarias del soporte de actor para TK-019, sin PostgreSQL. */
class ContextoActorServiceTest {

    private ContextoActorService service;
    private RepositorioFalso repository;

    @BeforeEach
    void preparar() {
        service = new ContextoActorService();
        repository = new RepositorioFalso();
        service.repository = repository;
    }

    @Test
    void fijarActor_conActorValido_delegaAlRepositorio() {
        service.fijarActor(42L);

        assertEquals(List.of(42L), repository.actores);
    }

    @Test
    void fijarActor_sinActorValido_seRechazaAntesDeTocarLaBase() {
        for (Long invalido : new Long[] { null, 0L, -5L }) {
            ApiException error = assertThrows(ApiException.class,
                    () -> service.fijarActor(invalido));
            assertEquals(401, error.getStatus());
            assertEquals("SIN_SESION", error.getCodigo());
        }

        assertEquals(List.of(), repository.actores);
    }

    @Test
    void fijarActor_dosActoresDistintos_noConservaEstadoEntreLlamadas() {
        service.fijarActor(42L);
        service.fijarActor(7L);

        assertEquals(List.of(42L, 7L), repository.actores);
    }

    @Test
    void fijarActor_exigeTransaccionPreviaYNoAbreOtra() throws Exception {
        Method metodo = ContextoActorService.class.getMethod("fijarActor", Long.class);
        Transactional anotacion = metodo.getAnnotation(Transactional.class);

        assertNotNull(anotacion);
        assertEquals(TxType.MANDATORY, anotacion.value());
    }

    private static final class RepositorioFalso extends ContextoActorRepository {

        private final List<Long> actores = new ArrayList<>();

        @Override
        public void fijarActor(Long actorId) {
            actores.add(actorId);
        }
    }
}
