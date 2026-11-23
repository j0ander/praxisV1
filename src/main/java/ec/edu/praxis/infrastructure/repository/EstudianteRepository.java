package ec.edu.praxis.infrastructure.repository;

import java.util.Collection;
import java.util.List;

import ec.edu.praxis.domain.model.Estudiante;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class EstudianteRepository implements PanacheRepositoryBase<Estudiante, Long> {

    @Inject
    EntityManager em;

    public Estudiante buscarPorUsuarioId(Long usuarioId) {
        try {
            return em.createQuery("SELECT e FROM Estudiante e WHERE e.usuarioId = :usuarioId", Estudiante.class)
                    .setParameter("usuarioId", usuarioId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Estudiante buscarPorCodigoEstudiante(String codigo) {
        try {
            return em.createQuery(
                    "SELECT e FROM Estudiante e WHERE e.codigoEstudiante = :codigo", Estudiante.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Estudiante> listarPorCarrera(Long carreraId) {
        return em.createQuery("SELECT e FROM Estudiante e WHERE e.carreraId = :carreraId ORDER BY e.id",
                Estudiante.class)
                .setParameter("carreraId", carreraId)
                .getResultList();
    }

    public List<Estudiante> listarPorCarreras(Collection<Long> carreras) {
        return em.createQuery("SELECT e FROM Estudiante e WHERE e.carreraId IN :carreras ORDER BY e.id",
                Estudiante.class)
                .setParameter("carreras", carreras)
                .getResultList();
    }
}
