package ec.edu.praxis.web.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.application.service.ConvenioService;
import ec.edu.praxis.domain.model.EstadoConvenio;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/** Pruebas unitarias de API-043, sin servidor ni PostgreSQL. */
class ConvenioResourceTest {

    private ConvenioResource resource;

    @BeforeEach
    void preparar() {
        resource = new ConvenioResource();
        resource.service = new ConvenioService() {
            @Override
            public List<ConvenioResumen> listarPorEmpresa(Long empresaId, Long actorId,
                    boolean esInstitucional, boolean esRepresentante) {
                return List.of(new ConvenioResumen(
                        8L, 10L, "C-9", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                        EstadoConvenio.VIGENTE, "VENCIDO", "https://ejemplo.test/c-9.pdf",
                        OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                        OffsetDateTime.parse("2025-06-01T00:00:00Z")));
            }
        };
        resource.identity = identity(99L, Set.of("REPRESENTANTE_EMPRESA"));
    }

    @Test
    void rutaYRolesConcuerdanConElContrato() throws Exception {
        assertEquals("/convenios", ConvenioResource.class.getAnnotation(Path.class).value());
        Method listar = ConvenioResource.class.getDeclaredMethod("listarPorEmpresa", Long.class);
        assertEquals("/listarPorEmpresa/{empresaId}", listar.getAnnotation(Path.class).value());
        assertTrue(listar.isAnnotationPresent(GET.class));
        assertEquals(Set.of("ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA"),
                Set.of(listar.getAnnotation(RolesAllowed.class).value()));
    }

    @Test
    void listarPorEmpresa_publicaLaEstructuraDelContratoSinDependerDelRepository() {
        Response response = resource.listarPorEmpresa(10L);

        assertEquals(200, response.getStatus());
        List<?> entidad = (List<?>) response.getEntity();
        assertEquals(1, entidad.size());
        ConvenioService.ConvenioResumen resumen = assertInstanceOf(
                ConvenioService.ConvenioResumen.class, entidad.get(0));
        assertEquals(8L, resumen.getId());
        assertEquals(10L, resumen.getEmpresaId());
        assertEquals("C-9", resumen.getNumero());
        assertEquals(LocalDate.of(2025, 1, 1), resumen.getFechaInicio());
        assertEquals(LocalDate.of(2025, 12, 31), resumen.getFechaFin());
        assertEquals(EstadoConvenio.VIGENTE, resumen.getEstado());
        assertEquals("VENCIDO", resumen.getEstadoEfectivo());
        assertEquals("https://ejemplo.test/c-9.pdf", resumen.getDocumentoUrl());

        Set<String> esperados = Set.of("id", "empresaId", "numero", "fechaInicio", "fechaFin",
                "estado", "estadoEfectivo", "documentoUrl", "createdAt", "updatedAt");
        Set<String> reales = Arrays.stream(ConvenioService.ConvenioResumen.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
        assertEquals(esperados, reales);
    }

    private static SecurityIdentity identity(Long usuarioId, Set<String> roles) {
        return (SecurityIdentity) Proxy.newProxyInstance(
                SecurityIdentity.class.getClassLoader(),
                new Class<?>[] { SecurityIdentity.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> usuarioId;
                    case "hasRole" -> roles.contains(args[0]);
                    case "getRoles" -> roles;
                    case "toString" -> "SecurityIdentity de prueba";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
