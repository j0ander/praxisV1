package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.DocenteService;
import ec.edu.praxis.domain.model.Docente;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/docentes")
@Produces(MediaType.APPLICATION_JSON)
public class DocenteResource {

    @Inject
    DocenteService service;

    @GET
    @Path("/listarTodos")
    public Response listar() {
        List<DocenteResponse> respuesta = service.listar().stream()
                .map(DocenteResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(Docente docente) {
        if (docente == null) {
            throw ApiException.datosInvalidos("Los datos del perfil son obligatorios.");
        }
        service.actualizar(docente.getId(), docente);
        return Response.ok().build();
    }

    public static class DocenteResponse {

        private final Long id;
        private final Long usuarioId;
        private final String codigoDocente;
        private final String dedicacion;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        DocenteResponse(Docente docente) {
            this.id = docente.getId();
            this.usuarioId = docente.getUsuarioId();
            this.codigoDocente = docente.getCodigoDocente();
            this.dedicacion = docente.getDedicacion();
            this.createdAt = docente.getCreatedAt();
            this.updatedAt = docente.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public String getCodigoDocente() {
            return codigoDocente;
        }

        public String getDedicacion() {
            return dedicacion;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
