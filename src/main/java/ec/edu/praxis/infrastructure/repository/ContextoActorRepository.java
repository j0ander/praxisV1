package ec.edu.praxis.infrastructure.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Persistencia del contexto de actor de la transacción (soporte de TK-019).
 *
 * <p>Ejecuta {@code set_config('praxis.usuario_id', actorId, true)}. El tercer
 * argumento es {@code true}: el valor es local a la transacción, por lo que
 * PostgreSQL lo restaura al terminar {@code COMMIT} o {@code ROLLBACK}. No se
 * emite ninguna sentencia {@code SET} ni limpieza manual, de modo que una
 * conexión devuelta al pool no conserva el actor de una operación anterior.
 *
 * <p>Debe invocarse desde un Service con transacción activa; el
 * {@code EntityManager} inyectado se asocia al mismo contexto de persistencia
 * y a la misma conexión JDBC que el resto de la operación.
 */
@ApplicationScoped
public class ContextoActorRepository {

    static final String SQL_FIJAR_ACTOR =
            "SELECT set_config('praxis.usuario_id', :actorId, true)";

    @Inject
    EntityManager em;

    public void fijarActor(Long actorId) {
        em.createNativeQuery(SQL_FIJAR_ACTOR)
                .setParameter("actorId", Long.toString(actorId))
                .getSingleResult();
    }
}
