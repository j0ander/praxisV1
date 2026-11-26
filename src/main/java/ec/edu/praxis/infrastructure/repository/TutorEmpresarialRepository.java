package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.TutorEmpresarial;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;


@ApplicationScoped
public class TutorEmpresarialRepository implements PanacheRepositoryBase<TutorEmpresarial, Long> {

    static final String JPQ_LISTAR =
            "SELECT t FROM TutorEmpresarial t WHERE t.empresaId = :empresaId ORDER BY t.id";

    static final String JPQ_BUSCAR =
            "SELECT t FROM TutorEmpresarial t WHERE t.id = :id AND t.empresaId = :empresaId";

    static final String JPQ_EXISTE_USUARIO =
            "SELECT COUNT(t) FROM TutorEmpresarial t WHERE t.usuarioId = :usuarioId";

    @Inject
    EntityManager em;

    public List<TutorEmpresarial> listarPorEmpresa(Long empresaId) {
        return em.createQuery(JPQ_LISTAR, TutorEmpresarial.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    public TutorEmpresarial buscarPorIdYEmpresa(Long id, Long empresaId) {
        try {
            return em.createQuery(JPQ_BUSCAR, TutorEmpresarial.class)
        .setParameter("id", id)
        .setParameter("empresaId", empresaId)
        .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public boolean existePorUsuario(Long usuarioId) {
        Long cantidad = em.createQuery(JPQ_EXISTE_USUARIO, Long.class)
                .setParameter("usuarioId", usuarioId)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }
}
