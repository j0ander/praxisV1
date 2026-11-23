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
import ec.edu.praxis.application.service.EmpresaUsuarioService;
import ec.edu.praxis.domain.model.EmpresaUsuario;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Miembro;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Pruebas unitarias del Resource de miembros, sin servidor ni PostgreSQL. */
class EmpresaUsuarioResourceTest {

    private EmpresaUsuarioResource resource;
    private ServicioFalso service;

    @BeforeEach
    void preparar() {
        resource = new EmpresaUsuarioResource();
        service = new ServicioFalso();
        resource.service = service;
        resource.identity = identity(99L, Set.of("REPRESENTANTE_EMPRESA"));
    }

    @Test
    void rutasYRolesConcuerdanConElContrato() throws Exception {
        assertEquals("/empresas", EmpresaUsuarioResource.class.getAnnotation(Path.class).value());

        Method listar = EmpresaUsuarioResource.class.getDeclaredMethod("listar", Long.class);
        Method crear = EmpresaUsuarioResource.class.getDeclaredMethod("crear", Long.class, EmpresaUsuario.class);
        Method actualizar = EmpresaUsuarioResource.class.getDeclaredMethod(
                "actualizar", Long.class, Long.class, EmpresaUsuario.class);

        assertEquals("/{empresaId}/miembros", listar.getAnnotation(Path.class).value());
        assertEquals("/{empresaId}/miembros", crear.getAnnotation(Path.class).value());
        assertEquals("/{empresaId}/miembros/{id}", actualizar.getAnnotation(Path.class).value());
        assertTrue(listar.isAnnotationPresent(GET.class));
        assertTrue(crear.isAnnotationPresent(POST.class));
        assertTrue(actualizar.isAnnotationPresent(PUT.class));
        assertEquals(MediaType.APPLICATION_JSON, crear.getAnnotation(Consumes.class).value()[0]);
        for (Method metodo : List.of(listar, crear, actualizar)) {
            assertEquals(Set.of("ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA"),
                    Set.of(metodo.getAnnotation(RolesAllowed.class).value()));
        }
    }

    @Test
    void listar_devuelveMiembroResumenYPasaLaIdentidadDeSesion() {
        Response response = resource.listar(10L);

        assertEquals(200, response.getStatus());
        List<?> entidad = (List<?>) response.getEntity();
        assertEquals(1, entidad.size());
        EmpresaUsuarioResource.MiembroResumen resumen = assertInstanceOf(
                EmpresaUsuarioResource.MiembroResumen.class, entidad.get(0));
        assertEquals(1L, resumen.getId());
        assertEquals(2L, resumen.getUsuarioId());
        assertEquals("Nombres", resumen.getNombres());
        assertEquals("Apellidos", resumen.getApellidos());
        assertEquals("correo@example.test", resumen.getCorreo());
        assertEquals("ADMIN_EMPRESA", resumen.getRolEmpresa());
        assertTrue(resumen.isActivo());
        assertEquals(10L, service.empresaId);
        assertEquals(99L, service.actorId);
        assertFalse(service.esInstitucional);
        assertTrue(service.esRepresentante);
    }

    @Test
    void crear_devuelve201ConElRecursoCreado() {
        EmpresaUsuario datos = new EmpresaUsuario();
        datos.setUsuarioId(2L);
        datos.setRolEmpresa("ADMIN_EMPRESA");
        datos.setActivo(true);

        Response response = resource.crear(10L, datos);

        assertEquals(201, response.getStatus());
        assertInstanceOf(EmpresaUsuarioResource.MiembroResumen.class, response.getEntity());
        assertEquals(datos, service.datos);
        assertEquals(10L, service.empresaId);
    }

    @Test
    void actualizar_devuelve200ConElRecursoActualizado() {
        EmpresaUsuario datos = new EmpresaUsuario();

        Response response = resource.actualizar(10L, 1L, datos);

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmpresaUsuarioResource.MiembroResumen.class, response.getEntity());
        assertEquals(1L, service.id);
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

    private static final class ServicioFalso extends EmpresaUsuarioService {

        private Long empresaId;
        private Long id;
        private Long actorId;
        private boolean esInstitucional;
        private boolean esRepresentante;
        private EmpresaUsuario datos;

        private final Miembro miembro = new Miembro(1L, 2L, "Nombres", "Apellidos",
                "correo@example.test", "ADMIN_EMPRESA", true);

        @Override
        public List<Miembro> listar(Long empresaId, Long actorId, boolean esInstitucional,
                boolean esRepresentante) {
            capturar(empresaId, null, null, actorId, esInstitucional, esRepresentante);
            return List.of(miembro);
        }

        @Override
        public Miembro crear(EmpresaUsuario datos, Long empresaId, Long actorId,
                boolean esInstitucional, boolean esRepresentante) {
            capturar(empresaId, null, datos, actorId, esInstitucional, esRepresentante);
            return miembro;
        }

        @Override
        public Miembro actualizar(EmpresaUsuario datos, Long empresaId, Long id, Long actorId,
                boolean esInstitucional, boolean esRepresentante) {
            capturar(empresaId, id, datos, actorId, esInstitucional, esRepresentante);
            return miembro;
        }

        private void capturar(Long empresaId, Long id, EmpresaUsuario datos, Long actorId,
                boolean esInstitucional, boolean esRepresentante) {
            this.empresaId = empresaId;
            this.id = id;
            this.datos = datos;
            this.actorId = actorId;
            this.esInstitucional = esInstitucional;
            this.esRepresentante = esRepresentante;
        }
    }
}
