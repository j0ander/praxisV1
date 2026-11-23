package ec.edu.praxis.application.service;

import ec.edu.praxis.infrastructure.repository.ContextoActorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Fija el usuario autenticado como actor de la transacción (soporte de TK-019).
 *
 * <p>El identificador NO se toma del cliente: el Resource de cada operación lo
 * obtiene de la identidad de sesión ({@code praxis.usuarioId}) y lo pasa al
 * Service, que es quien controla la transacción y llama a este método.
 *
 * <p>{@code MANDATORY} garantiza dos cosas: el contexto se establece dentro de
 * una transacción ya activa y se comparte la conexión con las escrituras sobre
 * {@code empresa_usuario}; si se invoca fuera de una transacción, Quarkus
 * rechaza la llamada. El valor es local a la transacción, por lo que no se
 * limpia manualmente ni permanece en una conexión reutilizada por el pool.
 *
 * <p>Alcance: únicamente el soporte de actor para TK-019. No implementa
 * auditoría genérica, recursos REST ni reglas de negocio de empresas.
 */
@ApplicationScoped
public class ContextoActorService {

    @Inject
    ContextoActorRepository repository;

    @Transactional(TxType.MANDATORY)
    public void fijarActor(Long actorId) {
        if (actorId == null || actorId <= 0) {
            throw ApiException.sinSesion(
                    "Se requiere un usuario autenticado para esta operación.");
        }
        repository.fijarActor(actorId);
    }
}
