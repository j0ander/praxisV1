package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.FacultadService;
import ec.edu.praxis.domain.model.Facultad;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Authenticated
@Path("/facultades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacultadResource {

    @Inject
    FacultadService service;

    @GET
    @Path("/listarTodos")
    public Response listar() {
        List<FacultadResponse> respuesta = service.listar().stream()
                .map(FacultadResponse::new)
                .toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Response.ok(new FacultadResponse(service.buscar(id))).build();
    }

    @POST
    @RolesAllowed("ADMINISTRADOR")
    public Response crear(Facultad facultad) {
        return Response.status(Response.Status.CREATED)
                .entity(new FacultadResponse(service.crear(facultad)))
                .build();
    }

    @PUT
    @RolesAllowed("ADMINISTRADOR")
    public Response actualizar(Facultad facultad) {
        return Response.ok(new FacultadResponse(service.actualizar(facultad))).build();
    }

    @DELETE
    @RolesAllowed("ADMINISTRADOR")
    @Path("/eliminar/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        service.eliminar(id);
        return Response.noContent().build();
    }

    /** Estructura de salida: no se devuelve la entidad JPA directamente. */
    public static class FacultadResponse {

        private final Long id;
        private final String codigo;
        private final String nombre;
        private final boolean activa;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        FacultadResponse(Facultad facultad) {
            this.id = facultad.getId();
            this.codigo = facultad.getCodigo();
            this.nombre = facultad.getNombre();
            this.activa = facultad.isActiva();
            this.createdAt = facultad.getCreatedAt();
            this.updatedAt = facultad.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getNombre() {
            return nombre;
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
