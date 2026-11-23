package ec.edu.praxis.application.service;

/**
 * notificación de capa de aplicación: el usuario indicado debe perder todas sus
 * sesiones activas. Se publica dentro de la transacción que modificó la cuenta
 * y se atiende solo después de que esa transacción confirme.
 */
public final class SesionInvalidacionEvent {

    private final Long usuarioId;

    public SesionInvalidacionEvent(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }
}
