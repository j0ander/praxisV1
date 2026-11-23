package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.Empresa;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

/** Persistencia de empresas (API-039 a API-042). */
@ApplicationScoped
public class EmpresaRepository implements PanacheRepositoryBase<Empresa, Long> {

    /** Membresía efectiva: activa, de usuario activo y con rol global REPRESENTANTE_EMPRESA. */
    static final String SQL_PERTENECE =
            "SELECT EXISTS (SELECT 1 FROM empresa_usuario eu "
                    + "JOIN usuario u ON u.id = eu.usuario_id AND u.activo "
                    + "WHERE eu.empresa_id = :empresaId AND eu.usuario_id = :usuarioId AND eu.activo "
                    + "AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id "
                    + "WHERE ur.usuario_id = eu.usuario_id AND r.codigo = 'REPRESENTANTE_EMPRESA'))";

    static final String SQL_ADMINISTRA =
            "SELECT EXISTS (SELECT 1 FROM empresa_usuario eu "
                    + "JOIN usuario u ON u.id = eu.usuario_id AND u.activo "
                    + "WHERE eu.empresa_id = :empresaId AND eu.usuario_id = :usuarioId AND eu.activo "
                    + "AND eu.rol_empresa = 'ADMIN_EMPRESA' "
                    + "AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id "
                    + "WHERE ur.usuario_id = eu.usuario_id AND r.codigo = 'REPRESENTANTE_EMPRESA'))";

    @Inject
    EntityManager em;

    /** Texto vacío significa sin filtro; si no, busca por RUC o razón social. */
    public List<Empresa> listarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listAll();
        }
        String patron = "%" + texto.trim() + "%";
        return em.createQuery(
                "SELECT e FROM Empresa e WHERE e.ruc LIKE :patron OR LOWER(e.razonSocial) LIKE LOWER(:patron) "
                        + "ORDER BY e.razonSocial, e.id", Empresa.class)
                .setParameter("patron", patron)
                .getResultList();
    }

    public Empresa buscarPorRuc(String ruc) {
        try {
            return em.createQuery("SELECT e FROM Empresa e WHERE e.ruc = :ruc", Empresa.class)
                    .setParameter("ruc", ruc)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Solo lectura: pertenencia activa del representante a la empresa. */
    public boolean perteneceAUsuario(Long empresaId, Long usuarioId) {
        return Boolean.TRUE.equals(em.createNativeQuery(SQL_PERTENECE)
                .setParameter("empresaId", empresaId)
                .setParameter("usuarioId", usuarioId)
                .getSingleResult());
    }

    /** Solo lectura: el representante es ADMIN_EMPRESA activo de la empresa. */
    public boolean esAdministradorDeUsuario(Long empresaId, Long usuarioId) {
        return Boolean.TRUE.equals(em.createNativeQuery(SQL_ADMINISTRA)
                .setParameter("empresaId", empresaId)
                .setParameter("usuarioId", usuarioId)
                .getSingleResult());
    }

    /**
     * Bloqueo pesimista de la fila de empresa. Serializa la modificación de sus
     * miembros dentro de la misma transacción; el trigger de `empresa_usuario`
     * bloquea esa misma fila, por lo que no se invierte el orden de adquisición.
     */
    public Empresa buscarParaBloqueo(Long id) {
        return em.find(Empresa.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}
