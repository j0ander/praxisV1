package ec.edu.praxis.web.resource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.TipoPracticaService;
import ec.edu.praxis.domain.model.TipoPractica;
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


@Path("/tiposPractica")
@Produces(MediaType.APPLICATION_JSON)
public class TipoPracticaResource {

    @Inject
    TipoPracticaService service;

    @GET
    @Path("/listarTodos")
    public Response listar() {
        List<TipoPracticaResponse> respuesta = service.listar().stream()
                .map(TipoPracticaResponse::new)
                .toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Response.ok(new TipoPracticaResponse(service.buscar(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(TipoPractica tipo) {
        service.crear(tipo);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(TipoPractica tipo) {
        service.actualizar(tipo);
        return Response.ok().build();
    }

    public static class TipoPracticaResponse {

        private final Long id;
        private final String nombre;
        private final BigDecimal horasRequeridas;
        private final BigDecimal notaMinima;
        private final boolean activo;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        TipoPracticaResponse(TipoPractica tipo) {
            this.id = tipo.getId();
            this.nombre = tipo.getNombre();
            this.horasRequeridas = tipo.getHorasRequeridas();
            this.notaMinima = tipo.getNotaMinima();
            this.activo = tipo.isActivo();
            this.createdAt = tipo.getCreatedAt();
            this.updatedAt = tipo.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public BigDecimal getHorasRequeridas() {
            return horasRequeridas;
        }

        public BigDecimal getNotaMinima() {
            return notaMinima;
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
