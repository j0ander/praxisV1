package ec.edu.praxis.web.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.TutorEmpresarialService;
import ec.edu.praxis.domain.model.TutorEmpresarial;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Pruebas unitarias del Resource de tutores, sin servidor ni PostgreSQL. */
class TutorEmpresarialResourceTest {

    private TutorEmpresarialResource resource;
    private ServicioFalso service;

    @BeforeEach
    void preparar() {
        resource = new TutorEmpresarialResource();
        service = new ServicioFalso();
        resource.service = service;
        resource.identity = identity(99L, Set.of("ADMINISTRADOR"));
    }

    @Test
    void rutasYRolesConcuerdanConElContrato() throws Exception {
        assertEquals("/empresas", TutorEmpresarialResource.class.getAnnotation(Path.class).value());

        Method listar = TutorEmpresarialResource.class.getDeclaredMethod("listar", Long.class);
        Method crear = TutorEmpresarialResource.class.getDeclaredMethod(
                "crear", Long.class, TutorEmpresarial.class);
        Method actualizar = TutorEmpresarialResource.class.getDeclaredMethod(
                "actualizar", Long.class, TutorEmpresarial.class);

        assertEquals("/{empresaId}/tutores", listar.getAnnotation(Path.class).value());
        assertEquals("/{empresaId}/tutores", crear.getAnnotation(Path.class).value());
        assertEquals("/{empresaId}/tutores", actualizar.getAnnotation(Path.class).value());
        assertTrue(listar.isAnnotationPresent(GET.class));
        assertTrue(crear.isAnnotationPresent(POST.class));
        assertTrue(actualizar.isAnnotationPresent(PUT.class));
        assertEquals(MediaType.APPLICATION_JSON, crear.getAnnotation(Consumes.class).value()[0]);
        assertEquals(Set.of("ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA"),
                Set.of(listar.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of("ADMINISTRADOR", "COORDINADOR"),
                Set.of(crear.getAnnotation(RolesAllowed.class).value()));
        assertEquals(Set.of("ADMINISTRADOR", "COORDINADOR"),
                Set.of(actualizar.getAnnotation(RolesAllowed.class).value()));
    }

    @Test
    void listar_devuelveTutorEmpresarialResponseYPasaLaIdentidadDeSesion() {
        Response response = resource.listar(10L);

        assertEquals(200, response.getStatus());
        List<?> entidad = (List<?>) response.getEntity();
        assertEquals(1, entidad.size());
        TutorEmpresarialResource.TutorEmpresarialResponse resumen = assertInstanceOf(
                TutorEmpresarialResource.TutorEmpresarialResponse.class, entidad.get(0));
        assertEquals(1L, resumen.getId());
        assertEquals(2L, resumen.getUsuarioId());
        assertEquals(10L, resumen.getEmpresaId());
        assertEquals("Cargo", resumen.getCargo());
        assertEquals(10L, service.empresaId);
        assertEquals(99L, service.actorId);
        assertTrue(service.esInstitucional);
        assertFalse(service.esRepresentante);
    }

    @Test
    void crear_devuelve201ConElRecursoCreado() {
        TutorEmpresarial datos = new TutorEmpresarial();
        datos.setUsuarioId(2L);
        datos.setCargo("Cargo");

        Response response = resource.crear(10L, datos);

        assertEquals(201, response.getStatus());
        assertInstanceOf(TutorEmpresarialResource.TutorEmpresarialResponse.class, response.getEntity());
        assertEquals(datos, service.datos);
        assertEquals(10L, service.empresaId);
    }

    @Test
    void actualizar_devuelve200ConElRecursoActualizado() {
        TutorEmpresarial datos = new TutorEmpresarial();

        Response response = resource.actualizar(10L, datos);

        assertEquals(200, response.getStatus());
        assertInstanceOf(TutorEmpresarialResource.TutorEmpresarialResponse.class, response.getEntity());
        assertEquals(10L, service.empresaId);
    }

    @Test
    void cuerpoNulo_seRechazaCon400AntesDeLlamarAlServicio() {
        ApiException error = assertThrows(ApiException.class, () -> resource.crear(10L, null));

        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
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

    private static final class ServicioFalso extends TutorEmpresarialService {

        private Long empresaId;
        private Long actorId;
        private boolean esInstitucional;
        private boolean esRepresentante;
        private TutorEmpresarial datos;

        private final TutorEmpresarial tutor = tutor(1L, 10L);

        @Override
        public List<TutorEmpresarial> listar(Long empresaId, Long actorId, boolean esInstitucional,
                boolean esRepresentante) {
            capturar(empresaId, null, actorId, esInstitucional, esRepresentante);
            return List.of(tutor);
        }

        @Override
        public TutorEmpresarial crear(TutorEmpresarial datos, Long empresaId) {
            capturar(empresaId, datos, null, false, false);
            return tutor;
        }

        @Override
        public TutorEmpresarial actualizar(TutorEmpresarial datos, Long empresaId) {
            capturar(empresaId, datos, null, false, false);
            return tutor;
        }

        private void capturar(Long empresaId, TutorEmpresarial datos, Long actorId,
                boolean esInstitucional, boolean esRepresentante) {
            this.empresaId = empresaId;
            this.datos = datos;
            this.actorId = actorId;
            this.esInstitucional = esInstitucional;
            this.esRepresentante = esRepresentante;
        }

        private static TutorEmpresarial tutor(Long id, Long empresaId) {
            TutorEmpresarial tutor = new TutorEmpresarial();
            tutor.setId(id);
            tutor.setUsuarioId(2L);
            tutor.setEmpresaId(empresaId);
            tutor.setCargo("Cargo");
            return tutor;
        }
    }
}
