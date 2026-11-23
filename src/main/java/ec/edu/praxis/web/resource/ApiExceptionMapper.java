package ec.edu.praxis.web.resource;

import java.sql.SQLException;

import ec.edu.praxis.application.service.ApiException;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduce las excepciones al formato único de CONTRATOS_REST.md §6.
 * Solo clasifica la causa real: no convierte toda excepción en 409.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ApiException api) {
            return respuesta(api.getStatus(), api.getCodigo(), api.getMessage());
        }
        if (exception instanceof WebApplicationException web) {
            return mapearWeb(web);
        }
        if (esConflictoConcurrencia(exception)) {
            return respuesta(409, "CONFLICTO_CONCURRENCIA",
                    "El registro fue modificado por otra operación.");
        }
        SQLException sql = encontrarSQLException(exception);
        if (sql != null) {
            return mapearSql(sql);
        }
        return respuesta(500, "ERROR_INTERNO", "Error interno del servidor.");
    }

    private Response mapearWeb(WebApplicationException web) {
        int status = web.getResponse().getStatus();
        return switch (status) {
            case 400 -> respuesta(400, "DATOS_INVALIDOS", "Datos inválidos.");
            case 401 -> respuesta(401, "SIN_SESION", "Se requiere sesión.");
            case 403 -> respuesta(403, "SIN_PERMISO", "Sin permiso para la operación.");
            case 404 -> respuesta(404, "NO_ENCONTRADO", "Recurso no encontrado.");
            default -> respuesta(status, "ERROR_INTERNO", "Error interno del servidor.");
        };
    }

    private Response mapearSql(SQLException sql) {
        String state = sql.getSQLState();
        if (state == null) {
            return respuesta(500, "ERROR_INTERNO", "Error interno del servidor.");
        }
        // Unicidad de la base.
        if (state.equals("23505")) {
            return respuesta(409, "DUPLICADO", "Ya existe un registro con esos datos.");
        }
        // Integridad referencial: existen registros dependientes.
        if (state.equals("23503")) {
            return respuesta(409, "REGLA_NEGOCIO",
                    "Existen registros dependientes que impiden la operación.");
        }
        // Campo obligatorio ausente o longitud superior a la columna.
        if (state.equals("23502") || state.equals("22001")) {
            return respuesta(400, "DATOS_INVALIDOS",
                    "Faltan datos obligatorios o su longitud no es válida.");
        }
        // Resto de restricciones y errores lanzados por triggers (P0001).
        if (state.startsWith("23") || state.equals("P0001")) {
            return respuesta(409, "REGLA_NEGOCIO",
                    "La operación no cumple una regla de negocio.");
        }
        return respuesta(500, "ERROR_INTERNO", "Error interno del servidor.");
    }

    private static boolean esConflictoConcurrencia(Throwable exception) {
        Throwable actual = exception;
        for (int i = 0; actual != null && i < 32; i++) {
            String nombre = actual.getClass().getName();
            if (actual instanceof OptimisticLockException
                    || nombre.endsWith("StaleStateException")
                    || nombre.endsWith("StaleObjectStateException")) {
                return true;
            }
            Throwable siguiente = actual.getCause();
            if (siguiente == actual) {
                break;
            }
            actual = siguiente;
        }
        return false;
    }

    private static SQLException encontrarSQLException(Throwable exception) {
        Throwable actual = exception;
        for (int i = 0; actual != null && i < 32; i++) {
            if (actual instanceof SQLException sql) {
                return sql;
            }
            Throwable siguiente = actual.getCause();
            if (siguiente == actual) {
                break;
            }
            actual = siguiente;
        }
        return null;
    }

    static Response respuesta(int status, String codigo, String mensaje) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(codigo, mensaje))
                .build();
    }

    /** Estructura de salida: no se devuelve una entidad JPA. */
    public static class ErrorResponse {

        private final String codigo;
        private final String mensaje;

        ErrorResponse(String codigo, String mensaje) {
            this.codigo = codigo;
            this.mensaje = mensaje;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
