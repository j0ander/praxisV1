package ec.edu.praxis.web.resource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.PeriodoAcademicoService;
import ec.edu.praxis.domain.model.PeriodoAcademico;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** API-014 a API-019: consulta y administración de periodos académicos. */
@Authenticated
@Path("/periodos")
@Produces(MediaType.APPLICATION_JSON)
public class PeriodoAcademicoResource {

    @Inject
    PeriodoAcademicoService service;

    @GET
    @Path("/listarTodos")
    public Response listar() {
        List<PeriodoAcademicoResponse> respuesta = service.listar().stream()
                .map(PeriodoAcademicoResponse::new)
                .toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Response.ok(new PeriodoAcademicoResponse(service.buscar(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response crear(PeriodoAcademico periodo) {
        service.crear(periodo);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response actualizar(PeriodoAcademico periodo) {
        service.actualizar(periodo);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/activar")
    @RolesAllowed("ADMINISTRADOR")
    public Response activar(@PathParam("id") Long id) {
        service.activar(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/desactivar")
    @RolesAllowed("ADMINISTRADOR")
    public Response desactivar(@PathParam("id") Long id) {
        service.desactivar(id);
        return Response.ok().build();
    }

    /** Estructura de salida: no se devuelve la entidad JPA directamente. */
    public static class PeriodoAcademicoResponse {

        private final Long id;
        private final String codigo;
        private final String nombre;
        private final LocalDate fechaInicio;
        private final LocalDate fechaFin;
        private final boolean activo;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        PeriodoAcademicoResponse(PeriodoAcademico periodo) {
            this.id = periodo.getId();
            this.codigo = periodo.getCodigo();
            this.nombre = periodo.getNombre();
            this.fechaInicio = periodo.getFechaInicio();
            this.fechaFin = periodo.getFechaFin();
            this.activo = periodo.isActivo();
            this.createdAt = periodo.getCreatedAt();
            this.updatedAt = periodo.getUpdatedAt();
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

        public LocalDate getFechaInicio() {
            return fechaInicio;
        }

        public LocalDate getFechaFin() {
            return fechaFin;
        }

        public boolean isActivo() {
            return activo;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
