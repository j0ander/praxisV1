package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.Usuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class UsuarioRepository implements PanacheRepositoryBase<Usuario, Long> {

    @Inject
    EntityManager em;

    public Usuario buscarPorCorreo(String correo) {
        try {
            return em.createQuery("SELECT u FROM Usuario u WHERE u.correo = :correo", Usuario.class)
        .setParameter("correo", correo)
        .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Usuario buscarPorDocumento(String numeroDocumento) {
        try {
            return em.createQuery(
        "SELECT u FROM Usuario u WHERE u.numeroDocumento = :numeroDocumento", Usuario.class)
        .setParameter("numeroDocumento", numeroDocumento)
        .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public boolean existeUsuarioConRol(String codigoRol) {
        Long cantidad = em.createQuery(
                "SELECT COUNT(DISTINCT u) FROM Usuario u "
        + "JOIN UsuarioRol ur ON ur.id.usuarioId = u.id "
        + "JOIN Rol r ON r.id = ur.id.rolId "
        + "WHERE r.codigo = :codigoRol", Long.class)
                .setParameter("codigoRol", codigoRol)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }

    public List<Usuario> listarConFiltros(String rol, Boolean activo, String texto) {
        return em.createQuery(
                "SELECT DISTINCT u FROM Usuario u "
        + "LEFT JOIN UsuarioRol ur ON ur.id.usuarioId = u.id "
        + "LEFT JOIN Rol r ON r.id = ur.id.rolId "
        + "WHERE (:rol IS NULL OR r.codigo = :rol) "
        + "AND (:activo IS NULL OR u.activo = :activo) "
        + "AND (:texto IS NULL OR LOWER(u.correo) LIKE :texto "
        + "OR LOWER(u.nombres) LIKE :texto OR LOWER(u.apellidos) LIKE :texto "
        + "OR LOWER(COALESCE(u.numeroDocumento, '')) LIKE :texto) "
        + "ORDER BY u.id",
                Usuario.class)
                .setParameter("rol", rol)
                .setParameter("activo", activo)
                .setParameter("texto", texto == null ? null : "%" + texto.toLowerCase() + "%")
                .getResultList();
    }
}
