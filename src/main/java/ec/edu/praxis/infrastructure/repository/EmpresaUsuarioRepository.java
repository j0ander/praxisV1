package ec.edu.praxis.infrastructure.repository;

import java.util.ArrayList;
import java.util.List;

import ec.edu.praxis.domain.model.EmpresaUsuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/** Persistencia de membresías de empresa (API-048 a API-050). */
@ApplicationScoped
public class EmpresaUsuarioRepository implements PanacheRepositoryBase<EmpresaUsuario, Long> {

    /**
     * Justificación de SQL nativo (ARQUITECTURA.md §7.1): reproduce la función
     * `empresa_rol_activo()` de la base con `EXISTS` sobre `usuario_rol`/`rol`.
     * Las entidades `UsuarioRol` (clave compuesta) y `Rol` no están asociadas por
     * `@ManyToOne` desde `EmpresaUsuario`, por lo que JPQL exigiría un producto
     * cartesiano con tres entidades y condiciones de unión manuales, más costoso y
     * menos claro. Consulta de solo lectura, sin escritura ni cambio de permisos.
     */
    static final String SQL_ROL_ACTIVO =
            "SELECT eu.rol_empresa FROM empresa_usuario eu "
                    + "JOIN usuario u ON u.id = eu.usuario_id AND u.activo "
                    + "WHERE eu.empresa_id = :empresaId AND eu.usuario_id = :usuarioId AND eu.activo "
                    + "AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id "
                    + "WHERE ur.usuario_id = eu.usuario_id AND r.codigo = 'REPRESENTANTE_EMPRESA')";

    /**
     * Justificación de SQL nativo (ARQUITECTURA.md §7.1): proyección de solo
     * lectura de `empresa_usuario` con `EXISTS` sobre `usuario_rol`/`rol`; evita
     * cargar entidades y reproducir uniones entre tres entidades sin relación
     * JPA declarada. No filtra `empresa.activa` por decisión de API-086.
     */
    static final String SQL_MI_EMPRESA_ADMIN =
            "SELECT eu.empresa_id, eu.rol_empresa FROM empresa_usuario eu "
                    + "JOIN usuario u ON u.id = eu.usuario_id AND u.activo "
                    + "WHERE eu.usuario_id = :usuarioId AND eu.activo "
                    + "AND eu.rol_empresa = 'ADMIN_EMPRESA' "
                    + "AND EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id = ur.rol_id "
                    + "WHERE ur.usuario_id = eu.usuario_id AND r.codigo = 'REPRESENTANTE_EMPRESA')";

    /**
     * Justificación de SQL nativo (ARQUITECTURA.md §7.1): proyección plana que
     * mezcla columnas de `empresa_usuario` y `usuario`; JPQL obligaría a crear
     * una clase de proyección adicional y a gestionar la relación entre entidades
     * que hoy no existe. Solo lectura y sin campos sensibles.
     */
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

    /** Cuenta miembros activos; si se excluye un id, se usa para validar una actualización. */
    public long contarActivos(Long empresaId, Long idExcluido) {
        Long cantidad = em.createQuery(
                "SELECT COUNT(eu) FROM EmpresaUsuario eu WHERE eu.empresaId = :empresaId AND eu.activo = TRUE "
                        + "AND (:idExcluido IS NULL OR eu.id <> :idExcluido)", Long.class)
                .setParameter("empresaId", empresaId)
                .setParameter("idExcluido", idExcluido)
                .getSingleResult();
        return cantidad == null ? 0 : cantidad;
    }

    /** Cuenta ADMIN_EMPRESA activos; complementa la guarda del trigger aun con empresa inactiva. */
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

    /** Rol efectivo del representante, o null si la membresía no es válida. */
    public String rolActivo(Long empresaId, Long usuarioId) {
        try {
            Object valor = em.createNativeQuery(SQL_ROL_ACTIVO)
                    .setParameter("empresaId", empresaId)
                    .setParameter("usuarioId", usuarioId)
                    .getSingleResult();
            return valor == null ? null : valor.toString();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Solo lectura: empresas que el usuario administra con membresía efectiva. */
    public List<Administracion> listarAdministracionesActivas(Long usuarioId) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_MI_EMPRESA_ADMIN)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
        List<Administracion> respuesta = new ArrayList<>(filas.size());
        for (Object[] fila : filas) {
            respuesta.add(new Administracion(((Number) fila[0]).longValue(), (String) fila[1]));
        }
        return respuesta;
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

    /** Proyección interna de solo lectura para API-086. */
    public static class Administracion {

        private final Long empresaId;
        private final String rolEmpresa;

        public Administracion(Long empresaId, String rolEmpresa) {
            this.empresaId = empresaId;
            this.rolEmpresa = rolEmpresa;
        }

        public Long getEmpresaId() {
            return empresaId;
        }

        public String getRolEmpresa() {
            return rolEmpresa;
        }
    }

    /** Proyección interna de solo lectura para API-048 a API-050. */
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
