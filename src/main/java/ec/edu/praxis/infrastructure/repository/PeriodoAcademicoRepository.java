package ec.edu.praxis.infrastructure.repository;

import ec.edu.praxis.domain.model.PeriodoAcademico;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class PeriodoAcademicoRepository implements PanacheRepositoryBase<PeriodoAcademico, Long> {

    @Inject
    EntityManager em;

    public PeriodoAcademico buscarPorCodigo(String codigo) {
        try {
            return em.createQuery(
                    "SELECT p FROM PeriodoAcademico p WHERE p.codigo = :codigo",
                    PeriodoAcademico.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public PeriodoAcademico buscarActivo() {
        try {
            return em.createQuery(
                    "SELECT p FROM PeriodoAcademico p WHERE p.activo = :activo",
                    PeriodoAcademico.class)
                    .setParameter("activo", true)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
