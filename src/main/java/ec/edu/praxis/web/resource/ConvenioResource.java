package ec.edu.praxis.web.resource;

import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.ConvenioService;
import ec.edu.praxis.domain.model.Convenio;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** API-043 a API-047: convenios. */
@Authenticated
@Path("/convenios")
@Produces(MediaType.APPLICATION_JSON)
public class ConvenioResource {

    @Inject
    ConvenioService service;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/listarPorEmpresa/{empresaId}")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA" })
    public Response listarPorEmpresa(@PathParam("empresaId") Long empresaId) {
        List<ConvenioService.ConvenioResumen> respuesta = service.listarPorEmpresa(
                empresaId, actorId(), esInstitucional(), esRepresentante());
        return Response.ok(respuesta).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response crear(Convenio convenio) {
        service.crear(validar(convenio));
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response actualizar(Convenio convenio) {
        service.actualizar(validar(convenio));
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/suspender")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response suspender(@PathParam("id") Long id) {
        service.suspender(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/terminar")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR" })
    public Response terminar(@PathParam("id") Long id) {
        service.terminar(id);
        return Response.ok().build();
    }

    private Convenio validar(Convenio convenio) {
        if (convenio == null) {
            throw ApiException.datosInvalidos("Los datos del convenio son obligatorios.");
        }
        return convenio;
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
}
