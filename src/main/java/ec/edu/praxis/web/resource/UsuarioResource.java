package ec.edu.praxis.web.resource;

import java.util.List;

import ec.edu.praxis.application.service.ApiException;
import ec.edu.praxis.application.service.UsuarioService;
import ec.edu.praxis.domain.model.Usuario;
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


@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioService service;

    @GET
    @Path("/listarTodos")
    public Response listar(@QueryParam("rol") String rol, @QueryParam("activo") Boolean activo,
            @QueryParam("texto") String texto) {
        List<UsuarioResumen> respuesta = service.listar(rol, activo, texto).stream()
                .map(this::resumen).toList();
        return Response.ok(respuesta).build();
    }

    @GET
    @Path("/buscar/{id}")
    public Response buscar(@PathParam("id") Long id) {
        return Response.ok(resumen(service.buscar(id))).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest solicitud) {
        if (solicitud == null) {
            throw ApiException.credencialesInvalidas();
        }
        Usuario usuario = service.autenticar(solicitud.getCorreo(), solicitud.getPassword());
        return Response.ok(new LoginResponse(usuario, service.rolesDe(usuario.getId()))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(UsuarioRequest solicitud) {
        if (solicitud == null) {
            throw ApiException.datosInvalidos("Los datos del usuario son obligatorios.");
        }
        Usuario creado = service.crear(entidad(solicitud), solicitud.passwordTemporal);
        return Response.status(Response.Status.CREATED).entity(resumen(creado)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizar(UsuarioRequest solicitud) {
        if (solicitud == null || solicitud.id == null) {
            throw ApiException.datosInvalidos("El id del usuario es obligatorio.");
        }
        Usuario actualizado = service.actualizar(solicitud.id, entidad(solicitud));
        return Response.ok(resumen(actualizado)).build();
    }

    @POST
    @Path("/{id}/activar")
    public Response activar(@PathParam("id") Long id) {
        service.activar(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/desactivar")
    public Response desactivar(@PathParam("id") Long id) {
        service.desactivar(id);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/restablecerPassword")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response restablecerPassword(@PathParam("id") Long id, RestablecerPasswordRequest solicitud) {
        service.restablecerPassword(id, solicitud == null ? null : solicitud.passwordTemporal);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response asignarRoles(@PathParam("id") Long id, AsignarRolesRequest solicitud) {
        service.asignarRoles(id, solicitud == null ? null : solicitud.roles);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/carreras")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response asignarCarreras(@PathParam("id") Long id, AsignarCarrerasRequest solicitud) {
        service.asignarCarreras(id, solicitud == null ? null : solicitud.carreraIds);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/coordinador-carrera")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response asignarCarreraCoordinador(@PathParam("id") Long id,
            AsignarCoordinadorCarreraRequest solicitud) {
        service.asignarCarreraCoordinador(id, solicitud == null ? null : solicitud.carreraId);
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


    public static class LoginRequest {

        private String correo;
        private String password;

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginResponse {

        private final Long id;
        private final String nombres;
        private final String apellidos;
        private final String correo;
        private final List<String> roles;

        LoginResponse(Usuario usuario, List<String> roles) {
            this.id = usuario.getId();
            this.nombres = usuario.getNombres();
            this.apellidos = usuario.getApellidos();
            this.correo = usuario.getCorreo();
            this.roles = roles;
        }

        public Long getId() {
            return id;
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

        public List<String> getRoles() {
            return roles;
        }
    }


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

    public static class AsignarCoordinadorCarreraRequest {

        private Long carreraId;

        public Long getCarreraId() {
            return carreraId;
        }

        public void setCarreraId(Long carreraId) {
            this.carreraId = carreraId;
        }
    }

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
