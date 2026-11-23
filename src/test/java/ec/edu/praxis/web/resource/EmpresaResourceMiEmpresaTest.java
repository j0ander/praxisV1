package ec.edu.praxis.web.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.application.service.EmpresaService;
import ec.edu.praxis.domain.model.Empresa;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/** Pruebas unitarias de API-086, sin servidor ni PostgreSQL. */
class EmpresaResourceMiEmpresaTest {

    private EmpresaResource resource;
    private ServicioFalso service;

    @BeforeEach
    void preparar() {
        resource = new EmpresaResource();
        service = new ServicioFalso();
        resource.service = service;
        resource.identity = identity(99L);
    }

    @Test
    void rutaYRolesConcuerdanConElContrato() throws Exception {
        assertEquals("/empresas", EmpresaResource.class.getAnnotation(Path.class).value());

        Method miEmpresa = EmpresaResource.class.getDeclaredMethod("miEmpresa");

        assertEquals("/miEmpresa", miEmpresa.getAnnotation(Path.class).value());
        assertTrue(miEmpresa.isAnnotationPresent(GET.class));
        assertEquals(Set.of("REPRESENTANTE_EMPRESA"),
                Set.of(miEmpresa.getAnnotation(RolesAllowed.class).value()));
        assertEquals(0, miEmpresa.getParameterCount());
    }

    @Test
    void miEmpresa_devuelveSoloLosCamposDelContrato() {
        Response response = resource.miEmpresa();

        assertEquals(200, response.getStatus());
        EmpresaResource.MiEmpresaResponse datos = assertInstanceOf(
                EmpresaResource.MiEmpresaResponse.class, response.getEntity());
        assertEquals(10L, datos.getEmpresaId());
        assertEquals("0999999999001", datos.getRuc());
        assertEquals("Propia", datos.getRazonSocial());
        assertFalse(datos.isActiva());
        assertEquals("ADMIN_EMPRESA", datos.getRolEmpresa());
        assertEquals(99L, service.ultimoUsuarioId);

        Set<String> esperados = Set.of("empresaId", "ruc", "razonSocial", "activa", "rolEmpresa");
        Set<String> reales = Arrays.stream(EmpresaResource.MiEmpresaResponse.class.getDeclaredFields())
                .map(Field::getName).collect(java.util.stream.Collectors.toSet());
        assertEquals(esperados, reales);
    }

    @Test
    void miEmpresa_sinUsuarioIdEnLaIdentidad_noInventaUnActor() {
        resource.identity = identity(null);

        resource.miEmpresa();

        assertEquals(null, service.ultimoUsuarioId);
    }

    private static SecurityIdentity identity(Long usuarioId) {
        return (SecurityIdentity) Proxy.newProxyInstance(
                SecurityIdentity.class.getClassLoader(),
                new Class<?>[] { SecurityIdentity.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> usuarioId;
                    case "hasRole" -> "REPRESENTANTE_EMPRESA".equals(args[0]);
                    case "getRoles" -> Set.of("REPRESENTANTE_EMPRESA");
                    case "toString" -> "SecurityIdentity de prueba";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class ServicioFalso extends EmpresaService {

        private Long ultimoUsuarioId;

        private final Empresa empresa = empresa();

        @Override
        public Empresa miEmpresa(Long usuarioId) {
            ultimoUsuarioId = usuarioId;
            return empresa;
        }

        private static Empresa empresa() {
            Empresa empresa = new Empresa();
            try {
                Field campo = Empresa.class.getDeclaredField("id");
                campo.setAccessible(true);
                campo.set(empresa, 10L);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            empresa.setRuc("0999999999001");
            empresa.setRazonSocial("Propia");
            empresa.setActiva(false);
            return empresa;
        }
    }
}
