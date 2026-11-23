package ec.edu.praxis.infrastructure.repository;

import ec.edu.praxis.domain.model.Facultad;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class FacultadRepository implements PanacheRepositoryBase<Facultad, Long> {

    @Inject
    EntityManager em;

    public Facultad buscarPorCodigo(String codigo) {
        try {
            return em.createQuery("SELECT f FROM Facultad f WHERE f.codigo = :codigo", Facultad.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Facultad buscarPorNombre(String nombre) {
        try {
            return em.createQuery("SELECT f FROM Facultad f WHERE f.nombre = :nombre", Facultad.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
