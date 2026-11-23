package ec.edu.praxis.infrastructure.repository;

import java.util.Collection;
import java.util.List;

import ec.edu.praxis.domain.model.Docente;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class DocenteRepository implements PanacheRepositoryBase<Docente, Long> {

    @Inject
    EntityManager em;

    public Docente buscarPorUsuarioId(Long usuarioId) {
        try {
            return em.createQuery("SELECT d FROM Docente d WHERE d.usuarioId = :usuarioId", Docente.class)
                    .setParameter("usuarioId", usuarioId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Docente buscarPorCodigoDocente(String codigo) {
        try {
            return em.createQuery("SELECT d FROM Docente d WHERE d.codigoDocente = :codigo", Docente.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Docente> listarPorCarreras(Collection<Long> carreras) {
        return em.createQuery(
                "SELECT DISTINCT d FROM Docente d WHERE EXISTS "
                        + "(SELECT uc FROM UsuarioCarrera uc WHERE uc.id.usuarioId = d.usuarioId "
                        + "AND uc.id.carreraId IN :carreras) ORDER BY d.id",
                Docente.class)
                .setParameter("carreras", carreras)
                .getResultList();
    }
}
