package ec.edu.praxis.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Atiende las invalidaciones de sesión derivadas de cambios en la cuenta. El
 * observador es transaccional de tipo AFTER_SUCCESS: se ejecuta después del
 * commit y se omite si la transacción se revierte. Solo descarta sesiones en
 * memoria; no accede a PostgreSQL ni abre transacción.
 */
@ApplicationScoped
public class SesionInvalidacionObserver {

    @Inject
    SesionStore sesionStore;

    public void alConfirmar(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) SesionInvalidacionEvent evento) {
        sesionStore.invalidarPorUsuario(evento.getUsuarioId());
    }
}
