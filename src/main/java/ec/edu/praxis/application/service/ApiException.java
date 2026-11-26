package ec.edu.praxis.application.service;

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

    public static ApiException credencialesInvalidas() {
        return new ApiException(401, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
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
