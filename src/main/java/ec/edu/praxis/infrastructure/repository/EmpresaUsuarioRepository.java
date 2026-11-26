package ec.edu.praxis.infrastructure.repository;

import java.util.ArrayList;
import java.util.List;

import ec.edu.praxis.domain.model.EmpresaUsuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;


@ApplicationScoped
public class EmpresaUsuarioRepository implements PanacheRepositoryBase<EmpresaUsuario, Long> {

    static final String SQL_MIEMBRO =
            "SELECT eu.id, eu.usuario_id, u.nombres, u.apellidos, u.correo, eu.rol_empresa, eu.activo "
        + "FROM empresa_usuario eu JOIN usuario u ON u.id = eu.usuario_id ";

    @Inject
    EntityManager em;

    public List<EmpresaUsuario> listarPorEmpresa(Long empresaId) {
        return em.createQuery(
                "SELECT eu FROM EmpresaUsuario eu WHERE eu.empresaId = :empresaId ORDER BY eu.id",
                EmpresaUsuario.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    public EmpresaUsuario buscarPorIdYEmpresa(Long id, Long empresaId) {
        try {
            return em.createQuery(
        "SELECT eu FROM EmpresaUsuario eu WHERE eu.id = :id AND eu.empresaId = :empresaId",
        EmpresaUsuario.class)
        .setParameter("id", id)
        .setParameter("empresaId", empresaId)
        .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public boolean existeDuplicado(Long empresaId, Long usuarioId) {
        Long cantidad = em.createQuery(
                "SELECT COUNT(eu) FROM EmpresaUsuario eu "
        + "WHERE eu.empresaId = :empresaId AND eu.usuarioId = :usuarioId", Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("usuarioId", usuarioId)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }

    public long contarActivos(Long empresaId, Long idExcluido) {
        Long cantidad = em.createQuery(
                "SELECT COUNT(eu) FROM EmpresaUsuario eu WHERE eu.empresaId = :empresaId AND eu.activo = TRUE "
        + "AND (:idExcluido IS NULL OR eu.id <> :idExcluido)", Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("idExcluido", idExcluido)
                .getSingleResult();
        return cantidad == null ? 0 : cantidad;
    }


    public long contarAdministradoresActivos(Long empresaId, Long idExcluido) {
        Long cantidad = em.createQuery(
                "SELECT COUNT(eu) FROM EmpresaUsuario eu WHERE eu.empresaId = :empresaId AND eu.activo = TRUE "
        + "AND eu.rolEmpresa = 'ADMIN_EMPRESA' "
        + "AND (:idExcluido IS NULL OR eu.id <> :idExcluido)", Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("idExcluido", idExcluido)
                .getSingleResult();
        return cantidad == null ? 0 : cantidad;
    }

    public List<Miembro> listarMiembrosPorEmpresa(Long empresaId) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_MIEMBRO + "WHERE eu.empresa_id = :empresaId ORDER BY eu.id")
                .setParameter("empresaId", empresaId)
                .getResultList();
        return aMiembros(filas);
    }

    public Miembro buscarMiembroPorId(Long id) {
        try {
            Object[] fila = (Object[]) em.createNativeQuery(SQL_MIEMBRO + "WHERE eu.id = :id")
        .setParameter("id", id)
        .getSingleResult();
            return aMiembro(fila);
        } catch (NoResultException e) {
            return null;
        }
    }

    private static List<Miembro> aMiembros(List<Object[]> filas) {
        List<Miembro> respuesta = new ArrayList<>(filas.size());
        for (Object[] fila : filas) {
            respuesta.add(aMiembro(fila));
        }
        return respuesta;
    }

    private static Miembro aMiembro(Object[] fila) {
        return new Miembro(
                ((Number) fila[0]).longValue(),
                ((Number) fila[1]).longValue(),
                (String) fila[2],
                (String) fila[3],
                (String) fila[4],
                (String) fila[5],
                Boolean.TRUE.equals(fila[6]));
    }

    public static class Miembro {

        private final Long id;
        private final Long usuarioId;
        private final String nombres;
        private final String apellidos;
        private final String correo;
        private final String rolEmpresa;
        private final boolean activo;

        public Miembro(Long id, Long usuarioId, String nombres, String apellidos, String correo,
                String rolEmpresa, boolean activo) {
            this.id = id;
            this.usuarioId = usuarioId;
            this.nombres = nombres;
            this.apellidos = apellidos;
            this.correo = correo;
            this.rolEmpresa = rolEmpresa;
            this.activo = activo;
        }

        public Long getId() {
            return id;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public String getNombres() {
            return nombres;
        }

        public String getApellidos() {
            return apellidos;
        }

        public String getCorreo() {
            return correo;
        }

        public String getRolEmpresa() {
            return rolEmpresa;
        }

        public boolean isActivo() {
            return activo;
        }
    }
}
