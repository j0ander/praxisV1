package ec.edu.praxis.web.resource;

import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.EmpresaUsuarioService;
import ec.edu.praxis.domain.model.EmpresaUsuario;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Miembro;
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

/** API-048 a API-050: miembros de una empresa. */
@Authenticated
@Path("/empresas")
@Produces(MediaType.APPLICATION_JSON)
public class EmpresaUsuarioResource {

    @Inject
    EmpresaUsuarioService service;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/{empresaId}/miembros")
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA" })
    public Response listar(@PathParam("empresaId") Long empresaId) {
        List<MiembroResumen> respuesta = service
                .listar(empresaId, actorId(), esInstitucional(), esRepresentante())
                .stream().map(MiembroResumen::new).toList();
        return Response.ok(respuesta).build();
    }

    @POST
    @Path("/{empresaId}/miembros")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA" })
    public Response crear(@PathParam("empresaId") Long empresaId, EmpresaUsuario datos) {
        Miembro creado = service.crear(validar(datos), empresaId, actorId(), esInstitucional(), esRepresentante());
        return Response.status(Response.Status.CREATED).entity(new MiembroResumen(creado)).build();
    }

    @PUT
    @Path("/{empresaId}/miembros/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "ADMINISTRADOR", "COORDINADOR", "REPRESENTANTE_EMPRESA" })
    public Response actualizar(@PathParam("empresaId") Long empresaId, @PathParam("id") Long id,
            EmpresaUsuario datos) {
        Miembro actualizado = service.actualizar(validar(datos), empresaId, id, actorId(),
                esInstitucional(), esRepresentante());
        return Response.ok(new MiembroResumen(actualizado)).build();
    }

    private static EmpresaUsuario validar(EmpresaUsuario datos) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del miembro son obligatorios.");
        }
        return datos;
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

    /** Estructura de salida definida en CONTRATOS_REST.md §4.2. */
    public static class MiembroResumen {

        private final Long id;
        private final Long usuarioId;
        private final String nombres;
        private final String apellidos;
        private final String correo;
        private final String rolEmpresa;
        private final boolean activo;

        MiembroResumen(Miembro miembro) {
            this.id = miembro.getId();
            this.usuarioId = miembro.getUsuarioId();
            this.nombres = miembro.getNombres();
            this.apellidos = miembro.getApellidos();
            this.correo = miembro.getCorreo();
            this.rolEmpresa = miembro.getRolEmpresa();
            this.activo = miembro.isActivo();
        }

        public Long getId() {
            return id;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public String getNombres() {
            return nombres;
        }

        public String getApellidos() {
            return apellidos;
        }

        public String getCorreo() {
            return correo;
        }

        public String getRolEmpresa() {
            return rolEmpresa;
        }

        public boolean isActivo() {
            return activo;
        }
    }
}
