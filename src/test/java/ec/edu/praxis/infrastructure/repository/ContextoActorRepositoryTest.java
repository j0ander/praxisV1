package ec.edu.praxis.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/** Pruebas unitarias de la sentencia de contexto, sin PostgreSQL. */
class ContextoActorRepositoryTest {

    private ContextoActorRepository repository;
    private Captura captura;

    @BeforeEach
    void preparar() {
        captura = new Captura();
        EntityManager em = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[] { EntityManager.class }, captura);
        Query query = (Query) Proxy.newProxyInstance(
                Query.class.getClassLoader(),
                new Class<?>[] { Query.class }, captura);
        captura.query = query;

        repository = new ContextoActorRepository();
        repository.em = em;
    }

    @Test
    void fijarActor_usaSetConfigConAmbitoSoloDeTransaccion() {
        repository.fijarActor(42L);

        assertEquals(ContextoActorRepository.SQL_FIJAR_ACTOR, captura.sql);
        assertTrue(captura.sql.contains("set_config"));
        assertTrue(captura.sql.contains("'praxis.usuario_id'"));
        assertTrue(captura.sql.contains(", true)"));
        assertEquals("42", captura.parametros.get("actorId"));
    }

    @Test
    void fijarActor_noEmiteLimpiezaNiSentenciasAdicionales() {
        repository.fijarActor(9L);

        assertEquals(List.of("createNativeQuery", "setParameter", "getSingleResult"),
                captura.metodos);
    }

    private static final class Captura implements InvocationHandler {

        private Query query;
        private String sql;
        private final Map<String, Object> parametros = new LinkedHashMap<>();
        private final List<String> metodos = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            metodos.add(method.getName());
            return switch (method.getName()) {
                case "createNativeQuery" -> {
                    sql = (String) args[0];
                    yield query;
                }
                case "setParameter" -> {
                    parametros.put((String) args[0], args[1]);
                    yield proxy;
                }
                case "getSingleResult" -> parametros.get("actorId");
                case "toString" -> "CapturaEntityManager";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }
}
