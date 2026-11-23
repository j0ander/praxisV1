package ec.edu.praxis.web.resource;

import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Sustituye al mapper interno de Quarkus, que responde 403 sin cuerpo.
 * Mantiene el formato único de CONTRATOS_REST.md §6.
 */
@Provider
public class PermisoExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {
        return ApiExceptionMapper.respuesta(403, "SIN_PERMISO", "Sin permiso para la operación.");
    }
}
