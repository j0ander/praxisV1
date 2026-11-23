package ec.edu.praxis.application.service;

/**
 * Excepción de negocio con el código HTTP y el código funcional definidos en
 * CONTRATOS_REST.md §6. El mensaje es siempre seguro para el cliente.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String codigo;

    private ApiException(int status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public static ApiException datosInvalidos(String mensaje) {
        return new ApiException(400, "DATOS_INVALIDOS", mensaje);
    }

    public static ApiException sinSesion(String mensaje) {
        return new ApiException(401, "SIN_SESION", mensaje);
    }

    public static ApiException sinPermiso(String mensaje) {
        return new ApiException(403, "SIN_PERMISO", mensaje);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(404, "NO_ENCONTRADO", mensaje);
    }

    public static ApiException reglaNegocio(String mensaje) {
        return new ApiException(409, "REGLA_NEGOCIO", mensaje);
    }

    public static ApiException duplicado(String mensaje) {
        return new ApiException(409, "DUPLICADO", mensaje);
    }

    public static ApiException conflictoConcurrencia(String mensaje) {
        return new ApiException(409, "CONFLICTO_CONCURRENCIA", mensaje);
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
