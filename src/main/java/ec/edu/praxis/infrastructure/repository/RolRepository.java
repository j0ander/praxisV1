package ec.edu.praxis.infrastructure.repository;

import ec.edu.praxis.domain.model.Rol;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class RolRepository implements PanacheRepositoryBase<Rol, Long> {

    @Inject
    EntityManager em;

    public Rol buscarPorCodigo(String codigo) {
        try {
            return em.createQuery("SELECT r FROM Rol r WHERE r.codigo = :codigo", Rol.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
