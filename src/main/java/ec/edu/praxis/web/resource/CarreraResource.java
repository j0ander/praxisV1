package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.CarreraService;
import ec.edu.praxis.domain.model.Carrera;
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


@Path("/carreras")
@Produces(MediaType.APPLICATION_JSON)
public class CarreraResource {

    @Inject
    CarreraService service;

    @GET
    @Path("/listarTodos")
    public Response listar(@QueryParam("facultadId") Long facultadId) {
        List<Carrera> carreras = facultadId == null ? service.listar() : service.listarPorFacultad(facultadId);
        List<CarreraResponse> respuesta = carreras.stream().map(CarreraResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Response.ok(new CarreraResponse(service.buscar(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(Carrera carrera) {
        return Response.status(Response.Status.CREATED)
                .entity(new CarreraResponse(service.crear(carrera)))
                .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(Carrera carrera) {
        return Response.ok(new CarreraResponse(service.actualizar(carrera))).build();
    }

    public static class CarreraResponse {

        private final Long id;
        private final Long facultadId;
        private final String codigo;
        private final String nombre;
        private final boolean activa;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        CarreraResponse(Carrera carrera) {
            this.id = carrera.getId();
            this.facultadId = carrera.getFacultadId();
            this.codigo = carrera.getCodigo();
            this.nombre = carrera.getNombre();
            this.activa = carrera.isActiva();
            this.createdAt = carrera.getCreatedAt();
            this.updatedAt = carrera.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public Long getFacultadId() {
            return facultadId;
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
