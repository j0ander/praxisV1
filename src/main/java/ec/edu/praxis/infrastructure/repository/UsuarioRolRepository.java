package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.UsuarioRol;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class UsuarioRolRepository implements PanacheRepositoryBase<UsuarioRol, UsuarioRol.Id> {

    @Inject
    EntityManager em;

    public List<String> listarCodigosPorUsuario(Long usuarioId) {
        return em.createQuery(
                "SELECT r.codigo FROM Rol r, UsuarioRol ur "
        + "WHERE ur.id.rolId = r.id AND ur.id.usuarioId = :usuarioId "
        + "ORDER BY r.codigo", String.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }

    public List<UsuarioRol> listarPorUsuario(Long usuarioId) {
        return em.createQuery("SELECT ur FROM UsuarioRol ur WHERE ur.id.usuarioId = :usuarioId",
                UsuarioRol.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }
}
