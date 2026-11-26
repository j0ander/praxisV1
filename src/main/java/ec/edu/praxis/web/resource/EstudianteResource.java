package ec.edu.praxis.web.resource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.EstudianteService;
import ec.edu.praxis.domain.model.Estudiante;
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


@Path("/estudiantes")
@Produces(MediaType.APPLICATION_JSON)
public class EstudianteResource {

    @Inject
    EstudianteService service;

    @GET
    @Path("/listarTodos")
    public Response listar(@QueryParam("carreraId") Long carreraId) {
        List<EstudianteResponse> respuesta = service.listar(carreraId).stream()
                .map(EstudianteResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response buscar(@PathParam("id") Long id) {
        return Response.ok(new EstudianteResponse(service.buscar(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(Estudiante estudiante) {
        service.crear(validar(estudiante));
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(Estudiante estudiante) {
        Estudiante datos = validar(estudiante);
        service.actualizar(datos.getId(), datos);
        return Response.ok().build();
    }

    private Estudiante validar(Estudiante estudiante) {
        if (estudiante == null) {
            throw ApiException.datosInvalidos("Los datos del perfil son obligatorios.");
        }
        return estudiante;
    }

    public static class EstudianteResponse {

        private final Long id;
        private final Long usuarioId;
        private final Long carreraId;
        private final String codigoEstudiante;
        private final LocalDate fechaNacimiento;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        EstudianteResponse(Estudiante estudiante) {
            this.id = estudiante.getId();
            this.usuarioId = estudiante.getUsuarioId();
            this.carreraId = estudiante.getCarreraId();
            this.codigoEstudiante = estudiante.getCodigoEstudiante();
            this.fechaNacimiento = estudiante.getFechaNacimiento();
            this.createdAt = estudiante.getCreatedAt();
            this.updatedAt = estudiante.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public Long getCarreraId() {
            return carreraId;
        }

        public String getCodigoEstudiante() {
            return codigoEstudiante;
        }

        public LocalDate getFechaNacimiento() {
            return fechaNacimiento;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
