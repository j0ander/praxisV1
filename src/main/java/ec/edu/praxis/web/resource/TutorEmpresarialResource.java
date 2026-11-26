package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.TutorEmpresarialService;
import ec.edu.praxis.domain.model.TutorEmpresarial;
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


@Path("/empresas")
@Produces(MediaType.APPLICATION_JSON)
public class TutorEmpresarialResource {

    @Inject
    TutorEmpresarialService service;

    @GET
    @Path("/{empresaId}/tutores")
    public Response listar(@PathParam("empresaId") Long empresaId) {
        List<TutorEmpresarialResponse> respuesta = service.listar(empresaId).stream()
                .map(TutorEmpresarialResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @POST
    @Path("/{empresaId}/tutores")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(@PathParam("empresaId") Long empresaId, TutorEmpresarial datos) {
        TutorEmpresarial creado = service.crear(validar(datos), empresaId);
        return Response.status(Response.Status.CREATED).entity(new TutorEmpresarialResponse(creado)).build();
    }

    @PUT
    @Path("/{empresaId}/tutores")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(@PathParam("empresaId") Long empresaId, TutorEmpresarial datos) {
        TutorEmpresarial actualizado = service.actualizar(validar(datos), empresaId);
        return Response.ok(new TutorEmpresarialResponse(actualizado)).build();
    }

    private static TutorEmpresarial validar(TutorEmpresarial datos) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del tutor son obligatorios.");
        }
        return datos;
    }

    public static class TutorEmpresarialResponse {

        private final Long id;
        private final Long usuarioId;
        private final Long empresaId;
        private final String cargo;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        TutorEmpresarialResponse(TutorEmpresarial tutor) {
            this.id = tutor.getId();
            this.usuarioId = tutor.getUsuarioId();
            this.empresaId = tutor.getEmpresaId();
            this.cargo = tutor.getCargo();
            this.createdAt = tutor.getCreatedAt();
            this.updatedAt = tutor.getUpdatedAt();
        }

        public Long getId() {
            return id;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public Long getEmpresaId() {
            return empresaId;
        }

        public String getCargo() {
            return cargo;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
