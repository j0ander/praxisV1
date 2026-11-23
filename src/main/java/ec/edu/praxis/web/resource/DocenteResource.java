package ec.edu.praxis.web.resource;

import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.DocenteService;
import ec.edu.praxis.domain.model.Docente;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** API-037 y API-038: consulta y actualización de perfiles de docente. */
@Authenticated
@Path("/docentes")
@Produces(MediaType.APPLICATION_JSON)
public class DocenteResource {

    @Inject
    DocenteService service;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/listarTodos")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response listar() {
        List<DocenteResponse> respuesta = service.listar(actorId(), esAdmin()).stream()
                .map(DocenteResponse::new).toList();
        return Response.ok(respuesta).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response actualizar(Docente docente) {
        if (docente == null) {
            throw ApiException.datosInvalidos("Los datos del perfil son obligatorios.");
        }
        service.actualizar(docente.getId(), docente, actorId(), esAdmin());
        return Response.ok().build();
    }

    private Long actorId() {
        return identity.getAttribute("praxis.usuarioId");
    }

    private boolean esAdmin() {
        return identity.hasRole("ADMINISTRADOR");
    }

    /** Estructura de salida definida en CONTRATOS_REST.md §4.2. */
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
