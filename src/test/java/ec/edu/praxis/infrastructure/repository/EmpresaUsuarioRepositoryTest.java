package ec.edu.praxis.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Miembro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

/** Pruebas unitarias de las consultas de membresías, sin PostgreSQL. */
class EmpresaUsuarioRepositoryTest {

    private Captura captura;
    private EmpresaUsuarioRepository repository;

    @BeforeEach
    void preparar() {
        captura = new Captura();
        EntityManager em = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[] { EntityManager.class }, captura);
        repository = new EmpresaUsuarioRepository();
        repository.em = em;
    }

    @Test
    void rolActivo_reproduzceMembresiaEfectivaSinEscribir() {
        String sql = EmpresaUsuarioRepository.SQL_ROL_ACTIVO;

        assertTrue(sql.contains("eu.activo"));
        assertTrue(sql.contains("u.activo"));
        assertTrue(sql.contains("'REPRESENTANTE_EMPRESA'"));
        assertTrue(sql.contains("eu.empresa_id = :empresaId"));
        assertTrue(sql.contains("eu.usuario_id = :usuarioId"));
        assertFalse(escritura(sql));
    }

    @Test
    void consultasNativas_mantienenElAlcanceDeSoloLectura() {
        assertTrue(EmpresaUsuarioRepository.SQL_ROL_ACTIVO.contains("EXISTS"));
        assertTrue(EmpresaUsuarioRepository.SQL_MI_EMPRESA_ADMIN.contains("EXISTS"));
        assertTrue(EmpresaUsuarioRepository.SQL_MIEMBRO.contains("u.correo"));
        assertFalse(escritura(EmpresaUsuarioRepository.SQL_ROL_ACTIVO));
        assertFalse(escritura(EmpresaUsuarioRepository.SQL_MI_EMPRESA_ADMIN));
        assertFalse(escritura(EmpresaUsuarioRepository.SQL_MIEMBRO));
    }

    @Test
    void miEmpresaAdmin_exigeMembresiaUsuarioActivoYRolGlobalSinFiltrarEmpresaInactiva() {
        String sql = EmpresaUsuarioRepository.SQL_MI_EMPRESA_ADMIN;

        assertTrue(sql.contains("eu.usuario_id = :usuarioId"));
        assertTrue(sql.contains("eu.activo"));
        assertTrue(sql.contains("u.activo"));
        assertTrue(sql.contains("'ADMIN_EMPRESA'"));
        assertTrue(sql.contains("'REPRESENTANTE_EMPRESA'"));
        assertFalse(sql.contains("e.activa"));
        assertFalse(sql.contains("empresa.activa"));
        assertFalse(escritura(sql));
    }

    @Test
    void listarAdministracionesActivas_devuelveSoloEmpresasDelUsuario() {
        captura.resultado = List.<Object[]>of(
                new Object[] { 10L, "ADMIN_EMPRESA" },
                new Object[] { 20L, "ADMIN_EMPRESA" });

        var administraciones = repository.listarAdministracionesActivas(99L);

        assertEquals(2, administraciones.size());
        assertEquals(10L, administraciones.get(0).getEmpresaId());
        assertEquals("ADMIN_EMPRESA", administraciones.get(1).getRolEmpresa());
        assertEquals(99L, captura.parametros.get("usuarioId"));
        assertEquals(1, captura.parametros.size());
    }

    @Test
    void proyeccionDeMiembro_noExponeCamposSensiblesDeUsuario() {
        String sql = EmpresaUsuarioRepository.SQL_MIEMBRO;

        assertTrue(sql.contains("u.nombres"));
        assertTrue(sql.contains("u.apellidos"));
        assertTrue(sql.contains("u.correo"));
        for (String sensible : List.of("hash_password", "intentos_fallidos", "bloqueado_hasta")) {
            assertFalse(sql.contains(sensible));
        }
        assertFalse(escritura(sql));
    }

    @Test
    void listarMiembros_mapeaLaProyeccionYAplicaElFiltroDeEmpresa() {
        captura.resultado = List.<Object[]>of(fila());

        List<Miembro> miembros = repository.listarMiembrosPorEmpresa(10L);

        assertEquals(1, miembros.size());
        Miembro miembro = miembros.get(0);
        assertEquals(1L, miembro.getId());
        assertEquals(2L, miembro.getUsuarioId());
        assertEquals("Nombres", miembro.getNombres());
        assertEquals("Apellidos", miembro.getApellidos());
        assertEquals("correo@example.test", miembro.getCorreo());
        assertEquals("ADMIN_EMPRESA", miembro.getRolEmpresa());
        assertTrue(miembro.isActivo());
        assertEquals(10L, captura.parametros.get("empresaId"));
        assertTrue(captura.sql.contains("WHERE eu.empresa_id = :empresaId"));
    }

    @Test
    void buscarMiembro_sinResultado_devuelveNull() {
        captura.vacio = true;

        assertNull(repository.buscarMiembroPorId(5L));
    }

    @Test
    void rolActivo_devuelveElRolDeLaFila() {
        captura.resultado = "ADMIN_EMPRESA";

        assertEquals("ADMIN_EMPRESA", repository.rolActivo(10L, 99L));
        assertEquals(99L, captura.parametros.get("usuarioId"));
    }

    private static Object[] fila() {
        return new Object[] { 1L, 2L, "Nombres", "Apellidos", "correo@example.test", "ADMIN_EMPRESA",
                Boolean.TRUE };
    }

    private static boolean escritura(String sql) {
        String minusculas = sql.toLowerCase(Locale.ROOT);
        return minusculas.matches("(?s).*\\b(insert|update|delete)\\b.*");
    }

    private static final class Captura implements InvocationHandler {

        private String sql;
        private Object resultado;
        private boolean vacio;
        private final Map<String, Object> parametros = new LinkedHashMap<>();

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "createNativeQuery" -> {
                    sql = (String) args[0];
                    yield consulta();
                }
                case "createQuery" -> consulta();
                case "toString" -> "EntityManager de prueba";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private Query consulta() {
            return (Query) Proxy.newProxyInstance(Query.class.getClassLoader(),
                    new Class<?>[] { Query.class }, (proxy, method, args) -> switch (method.getName()) {
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
