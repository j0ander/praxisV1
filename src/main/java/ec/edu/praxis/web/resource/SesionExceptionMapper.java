package ec.edu.praxis.web.resource;

import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Sustituye al mapper interno de Quarkus, que responde 401 sin cuerpo.
 * Mantiene el formato único de CONTRATOS_REST.md §6.
 */
@Provider
public class SesionExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    @Override
    public Response toResponse(UnauthorizedException exception) {
        return ApiExceptionMapper.respuesta(401, "SIN_SESION", "Se requiere sesión.");
    }
}
