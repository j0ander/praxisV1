package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.Carrera;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class CarreraRepository implements PanacheRepositoryBase<Carrera, Long> {

    @Inject
    EntityManager em;

    public Carrera buscarPorCodigo(String codigo) {
        try {
            return em.createQuery("SELECT c FROM Carrera c WHERE c.codigo = :codigo", Carrera.class)
                    .setParameter("codigo", codigo)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Carrera buscarPorNombreEnFacultad(Long facultadId, String nombre) {
        try {
            return em.createQuery(
                    "SELECT c FROM Carrera c WHERE c.facultadId = :facultadId AND c.nombre = :nombre",
                    Carrera.class)
                    .setParameter("facultadId", facultadId)
                    .setParameter("nombre", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Carrera> listarPorFacultad(Long facultadId) {
        return em.createQuery(
                "SELECT c FROM Carrera c WHERE c.facultadId = :facultadId ORDER BY c.id",
                Carrera.class)
                .setParameter("facultadId", facultadId)
                .getResultList();
    }
}
