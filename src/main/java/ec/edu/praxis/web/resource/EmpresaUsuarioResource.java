package ec.edu.praxis.web.resource;

import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.EmpresaUsuarioService;
import ec.edu.praxis.domain.model.EmpresaUsuario;
import ec.edu.praxis.infrastructure.repository.EmpresaUsuarioRepository.Miembro;
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
public class EmpresaUsuarioResource {

    @Inject
    EmpresaUsuarioService service;

    @GET
    @Path("/{empresaId}/miembros")
    public Response listar(@PathParam("empresaId") Long empresaId) {
        List<MiembroResumen> respuesta = service.listar(empresaId).stream()
                .map(MiembroResumen::new).toList();
        return Response.ok(respuesta).build();
    }

    @POST
    @Path("/{empresaId}/miembros")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(@PathParam("empresaId") Long empresaId, EmpresaUsuario datos) {
        Miembro creado = service.crear(validar(datos), empresaId);
        return Response.status(Response.Status.CREATED).entity(new MiembroResumen(creado)).build();
    }

    @PUT
    @Path("/{empresaId}/miembros/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(@PathParam("empresaId") Long empresaId, @PathParam("id") Long id,
            EmpresaUsuario datos) {
        Miembro actualizado = service.actualizar(validar(datos), empresaId, id);
        return Response.ok(new MiembroResumen(actualizado)).build();
    }

    private static EmpresaUsuario validar(EmpresaUsuario datos) {
        if (datos == null) {
            throw ApiException.datosInvalidos("Los datos del miembro son obligatorios.");
        }
        return datos;
    }

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
