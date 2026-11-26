package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.EmpresaService;
import ec.edu.praxis.domain.model.Empresa;
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


@Path("/empresas")
@Produces(MediaType.APPLICATION_JSON)
public class EmpresaResource {

    @Inject
    EmpresaService service;

    @GET
    @Path("/listarTodos")
    public Response listar(@QueryParam("texto") String texto) {
        List<EmpresaResponse> respuesta = service.listar(texto).stream()
                .map(EmpresaResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response buscar(@PathParam("id") Long id) {
        return Response.ok(new EmpresaResponse(service.buscar(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(Empresa empresa) {
        service.crear(validar(empresa));
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(Empresa empresa) {
        service.actualizar(validar(empresa));
        return Response.ok().build();
    }

    private Empresa validar(Empresa empresa) {
        if (empresa == null) {
            throw ApiException.datosInvalidos("Los datos de la empresa son obligatorios.");
        }
        return empresa;
    }

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
