package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.EmpresaService;
import ec.edu.praxis.domain.model.Empresa;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** API-039 a API-042 y API-086: empresas. */
@Authenticated
@Path("/empresas")
@Produces(MediaType.APPLICATION_JSON)
public class EmpresaResource {

    @Inject
    EmpresaService service;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/listarTodos")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response listar(@QueryParam("texto") String texto) {
        List<EmpresaResponse> respuesta = service.listar(texto).stream()
                .map(EmpresaResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA" })
    public Response buscar(@PathParam("id") Long id) {
        return Response.ok(new EmpresaResponse(service.buscar(id, actorId(), esInstitucional(),
                esRepresentante()))).build();
    }

    /** API-086: sin parámetros de cliente; el usuario se toma de la identidad de la sesión. */
    @GET
    @Path("/miEmpresa")
    @RolesAllowed("REPRESENTANTE_EMPRESA")
    public Response miEmpresa() {
        return Response.ok(new MiEmpresaResponse(service.miEmpresa(actorId()))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response crear(Empresa empresa) {
        service.crear(validar(empresa));
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA" })
    public Response actualizar(Empresa empresa) {
        service.actualizar(validar(empresa), actorId(), esInstitucional(), esRepresentante());
        return Response.ok().build();
    }

    private Empresa validar(Empresa empresa) {
        if (empresa == null) {
            throw ApiException.datosInvalidos("Los datos de la empresa son obligatorios.");
        }
        return empresa;
    }

    private Long actorId() {
        return identity.getAttribute("praxis.usuarioId");
    }

    private boolean esInstitucional() {
        return identity.hasRole("ADMINISTRADOR") || identity.hasRole("COORDINADOR");
    }

    private boolean esRepresentante() {
        return identity.hasRole("REPRESENTANTE_EMPRESA");
    }

    /** Estructura de salida de API-086 definida en CONTRATOS_REST.md §4.2.
     *  {@code rolEmpresa} es {@code ADMIN_EMPRESA}, la única membresía que autoriza la operación. */
    public static class MiEmpresaResponse {

        private final Long empresaId;
        private final String ruc;
        private final String razonSocial;
        private final boolean activa;
        private final String rolEmpresa;

        MiEmpresaResponse(Empresa empresa) {
            this.empresaId = empresa.getId();
            this.ruc = empresa.getRuc();
            this.razonSocial = empresa.getRazonSocial();
            this.activa = Boolean.TRUE.equals(empresa.getActiva());
            this.rolEmpresa = "ADMIN_EMPRESA";
        }

        public Long getEmpresaId() {
            return empresaId;
        }

        public String getRuc() {
            return ruc;
        }

        public String getRazonSocial() {
            return razonSocial;
        }

        public boolean isActiva() {
            return activa;
        }

        public String getRolEmpresa() {
            return rolEmpresa;
        }
    }

    /** Estructura de salida definida en CONTRATOS_REST.md §4.2. */
    public static class EmpresaResponse {

        private final Long id;
        private final String ruc;
        private final String razonSocial;
        private final String nombreComercial;
        private final String direccion;
        private final String telefono;
        private final String correo;
        private final boolean activa;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        EmpresaResponse(Empresa empresa) {
            this.id = empresa.getId();
            this.ruc = empresa.getRuc();
            this.razonSocial = empresa.getRazonSocial();
            this.nombreComercial = empresa.getNombreComercial();
            this.direccion = empresa.getDireccion();
            this.telefono = empresa.getTelefono();
            this.correo = empresa.getCorreo();
            this.activa = Boolean.TRUE.equals(empresa.getActiva());
            this.createdAt = empresa.getCreatedAt();
            this.updatedAt = empresa.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public String getRuc() {
            return ruc;
        }

        public String getRazonSocial() {
            return razonSocial;
        }

        public String getNombreComercial() {
            return nombreComercial;
        }

        public String getDireccion() {
            return direccion;
        }

        public String getTelefono() {
            return telefono;
        }

        public String getCorreo() {
            return correo;
        }

        public boolean isActiva() {
            return activa;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
