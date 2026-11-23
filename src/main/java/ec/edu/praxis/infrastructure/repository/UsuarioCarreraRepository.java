package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.UsuarioCarrera;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class UsuarioCarreraRepository implements PanacheRepositoryBase<UsuarioCarrera, UsuarioCarrera.Id> {

    @Inject
    EntityManager em;

    public List<Long> listarCarrerasPorUsuario(Long usuarioId) {
        return em.createQuery(
                "SELECT uc.id.carreraId FROM UsuarioCarrera uc "
                        + "WHERE uc.id.usuarioId = :usuarioId ORDER BY uc.id.carreraId",
                Long.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }

    /** Relaciones vigentes del usuario, para reemplazarlas gestionando entidades. */
    public List<UsuarioCarrera> listarPorUsuario(Long usuarioId) {
        return em.createQuery("SELECT uc FROM UsuarioCarrera uc WHERE uc.id.usuarioId = :usuarioId",
                UsuarioCarrera.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }
}
