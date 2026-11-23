package ec.edu.praxis.web.resource;

import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.UsuarioService;
import ec.edu.praxis.domain.model.Usuario;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** API-024 a API-032: administración de cuentas, roles y carreras. */
@Authenticated
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioService service;

    @GET
    @Path("/listarTodos")
    @RolesAllowed("ADMINISTRADOR")
    public Response listar(@QueryParam("rol") String rol, @QueryParam("activo") Boolean activo,
            @QueryParam("texto") String texto) {
        List<UsuarioResumen> respuesta = service.listar(rol, activo, texto).stream()
                .map(this::resumen).toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    @RolesAllowed("ADMINISTRADOR")
    public Response buscar(@PathParam("id") Long id) {
        return Response.ok(resumen(service.buscar(id))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response crear(UsuarioRequest solicitud) {
        if (solicitud == null) {
            throw ApiException.datosInvalidos("Los datos del usuario son obligatorios.");
        }
        Usuario creado = service.crear(entidad(solicitud), solicitud.passwordTemporal);
        return Response.status(Response.Status.CREATED).entity(resumen(creado)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response actualizar(UsuarioRequest solicitud) {
        if (solicitud == null || solicitud.id == null) {
            throw ApiException.datosInvalidos("El id del usuario es obligatorio.");
        }
        Usuario actualizado = service.actualizar(solicitud.id, entidad(solicitud));
        return Response.ok(resumen(actualizado)).build();
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

    @POST
    @Path("/{id}/restablecerPassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response restablecerPassword(@PathParam("id") Long id, RestablecerPasswordRequest solicitud) {
        service.restablecerPassword(id, solicitud == null ? null : solicitud.passwordTemporal);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response asignarRoles(@PathParam("id") Long id, AsignarRolesRequest solicitud) {
        service.asignarRoles(id, solicitud == null ? null : solicitud.roles);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/carreras")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response asignarCarreras(@PathParam("id") Long id, AsignarCarrerasRequest solicitud) {
        service.asignarCarreras(id, solicitud == null ? null : solicitud.carreraIds);
        return Response.ok().build();
    }

    private UsuarioResumen resumen(Usuario usuario) {
        return new UsuarioResumen(usuario, service.rolesDe(usuario.getId()),
                service.carrerasDe(usuario.getId()));
    }

    private static Usuario entidad(UsuarioRequest solicitud) {
        Usuario usuario = new Usuario();
        usuario.setCorreo(solicitud.correo);
        usuario.setNombres(solicitud.nombres);
        usuario.setApellidos(solicitud.apellidos);
        usuario.setTipoDocumento(solicitud.tipoDocumento);
        usuario.setNumeroDocumento(solicitud.numeroDocumento);
        usuario.setTelefono(solicitud.telefono);
        usuario.setAlcanceGlobal(solicitud.alcanceGlobal);
        return usuario;
    }

    /** Entrada de API-026 y API-027; passwordTemporal solo se usa al crear. */
    public static class UsuarioRequest {

        private Long id;
        private String correo;
        private String nombres;
        private String apellidos;
        private String tipoDocumento;
        private String numeroDocumento;
        private String telefono;
        private boolean alcanceGlobal;
        private String passwordTemporal;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }

        public String getNombres() {
            return nombres;
        }

        public void setNombres(String nombres) {
            this.nombres = nombres;
        }

        public String getApellidos() {
            return apellidos;
        }

        public void setApellidos(String apellidos) {
            this.apellidos = apellidos;
        }

        public String getTipoDocumento() {
            return tipoDocumento;
        }

        public void setTipoDocumento(String tipoDocumento) {
            this.tipoDocumento = tipoDocumento;
        }

        public String getNumeroDocumento() {
            return numeroDocumento;
        }

        public void setNumeroDocumento(String numeroDocumento) {
            this.numeroDocumento = numeroDocumento;
        }

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }

        public boolean isAlcanceGlobal() {
            return alcanceGlobal;
        }

        public void setAlcanceGlobal(boolean alcanceGlobal) {
            this.alcanceGlobal = alcanceGlobal;
        }

        public String getPasswordTemporal() {
            return passwordTemporal;
        }

        public void setPasswordTemporal(String passwordTemporal) {
            this.passwordTemporal = passwordTemporal;
        }
    }

    public static class RestablecerPasswordRequest {

        private String passwordTemporal;

        public String getPasswordTemporal() {
            return passwordTemporal;
        }

        public void setPasswordTemporal(String passwordTemporal) {
            this.passwordTemporal = passwordTemporal;
        }
    }

    public static class AsignarRolesRequest {

        private List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    public static class AsignarCarrerasRequest {

        private List<Long> carreraIds;

        public List<Long> getCarreraIds() {
            return carreraIds;
        }

        public void setCarreraIds(List<Long> carreraIds) {
            this.carreraIds = carreraIds;
        }
    }

    /** Estructura de salida; nunca expone credenciales. */
    public static class UsuarioResumen {

        private final Long id;
        private final String correo;
        private final String nombres;
        private final String apellidos;
        private final String tipoDocumento;
        private final String numeroDocumento;
        private final String telefono;
        private final boolean activo;
        private final boolean alcanceGlobal;
        private final List<String> roles;
        private final List<Long> carreraIds;

        UsuarioResumen(Usuario usuario, List<String> roles, List<Long> carreraIds) {
            this.id = usuario.getId();
            this.correo = usuario.getCorreo();
            this.nombres = usuario.getNombres();
            this.apellidos = usuario.getApellidos();
            this.tipoDocumento = usuario.getTipoDocumento();
            this.numeroDocumento = usuario.getNumeroDocumento();
            this.telefono = usuario.getTelefono();
            this.activo = usuario.isActivo();
            this.alcanceGlobal = usuario.isAlcanceGlobal();
            this.roles = roles;
            this.carreraIds = carreraIds;
        }

        public Long getId() {
            return id;
        }

        public String getCorreo() {
            return correo;
        }

        public String getNombres() {
            return nombres;
        }

        public String getApellidos() {
            return apellidos;
        }

        public String getTipoDocumento() {
            return tipoDocumento;
        }

        public String getNumeroDocumento() {
            return numeroDocumento;
        }

        public String getTelefono() {
            return telefono;
        }

        public boolean isActivo() {
            return activo;
        }

        public boolean isAlcanceGlobal() {
            return alcanceGlobal;
        }

        public List<String> getRoles() {
            return roles;
        }

        public List<Long> getCarreraIds() {
            return carreraIds;
        }
    }
}
