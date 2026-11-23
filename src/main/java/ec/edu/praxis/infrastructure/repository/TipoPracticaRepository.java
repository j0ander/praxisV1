package ec.edu.praxis.infrastructure.repository;

import ec.edu.praxis.domain.model.TipoPractica;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class TipoPracticaRepository implements PanacheRepositoryBase<TipoPractica, Long> {

    @Inject
    EntityManager em;

    public TipoPractica buscarPorNombre(String nombre) {
        try {
            return em.createQuery("SELECT t FROM TipoPractica t WHERE t.nombre = :nombre", TipoPractica.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
