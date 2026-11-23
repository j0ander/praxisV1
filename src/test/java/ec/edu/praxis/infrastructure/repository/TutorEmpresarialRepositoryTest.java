package ec.edu.praxis.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.TutorEmpresarial;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

/** Pruebas unitarias de las consultas de tutores, sin PostgreSQL. */
class TutorEmpresarialRepositoryTest {

    private Captura captura;
    private TutorEmpresarialRepository repository;

    @BeforeEach
    void preparar() {
        captura = new Captura();
        EntityManager em = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[] { EntityManager.class }, captura);
        repository = new TutorEmpresarialRepository();
        repository.em = em;
    }

    @Test
    void consultas_filtranSiemprePorLaEmpresaDeLaRuta() {
        assertTrue(TutorEmpresarialRepository.JPQ_LISTAR.contains("t.empresaId = :empresaId"));
        assertTrue(TutorEmpresarialRepository.JPQ_BUSCAR.contains("t.empresaId = :empresaId"));
        assertFalse(TutorEmpresarialRepository.JPQ_BUSCAR.contains("t.usuarioId"));
        assertTrue(TutorEmpresarialRepository.JPQ_EXISTE_USUARIO.contains("t.usuarioId = :usuarioId"));
        for (String consulta : List.of(TutorEmpresarialRepository.JPQ_LISTAR,
                TutorEmpresarialRepository.JPQ_BUSCAR, TutorEmpresarialRepository.JPQ_EXISTE_USUARIO)) {
            assertFalse(escritura(consulta));
        }
    }

    @Test
    void listarPorEmpresa_pasaElParametroDeEmpresa() {
        TutorEmpresarial tutor = new TutorEmpresarial();
        tutor.setId(5L);
        tutor.setEmpresaId(10L);
        captura.resultado = List.of(tutor);

        List<TutorEmpresarial> tutores = repository.listarPorEmpresa(10L);

        assertEquals(1, tutores.size());
        assertEquals(10L, captura.parametros.get("empresaId"));
    }

    @Test
    void buscarPorIdYEmpresa_sinResultado_devuelveNull() {
        captura.vacio = true;

        assertNull(repository.buscarPorIdYEmpresa(5L, 10L));
        assertEquals(5L, captura.parametros.get("id"));
        assertEquals(10L, captura.parametros.get("empresaId"));
    }

    private static boolean escritura(String consulta) {
        String minusculas = consulta.toLowerCase(Locale.ROOT);
        return minusculas.matches("(?s).*\\b(insert|update|delete)\\b.*");
    }

    private static final class Captura implements java.lang.reflect.InvocationHandler {

        private String consulta;
        private Object resultado;
        private boolean vacio;
        private final Map<String, Object> parametros = new LinkedHashMap<>();

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "createQuery" -> {
                    consulta = (String) args[0];
                    yield consulta();
                }
                case "toString" -> "EntityManager de prueba";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private TypedQuery<?> consulta() {
            return (TypedQuery<?>) Proxy.newProxyInstance(TypedQuery.class.getClassLoader(),
                    new Class<?>[] { TypedQuery.class }, (proxy, method, args) -> switch (method.getName()) {
                        case "setParameter" -> {
                            parametros.put((String) args[0], args[1]);
                            yield proxy;
                        }
                        case "getResultList" -> vacio ? List.of() : (List<?>) resultado;
                        case "getSingleResult" -> {
                            if (vacio || resultado == null) {
                                throw new NoResultException();
                            }
                            yield resultado;
                        }
                        case "toString" -> "Query de prueba";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
