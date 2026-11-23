package ec.edu.praxis.domain.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Entidad de la tabla usuario_rol (MODELO_DATOS.md §4.3), con clave primaria compuesta. */
@Entity
@Table(name = "usuario_rol")
public class UsuarioRol {

    @EmbeddedId
    private Id id;

    public UsuarioRol() {
    }

    public UsuarioRol(Long usuarioId, Long rolId) {
        this.id = new Id(usuarioId, rolId);
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    @Embeddable
    public static class Id implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "usuario_id", nullable = false)
        private Long usuarioId;

        @Column(name = "rol_id", nullable = false)
        private Long rolId;

        public Id() {
        }

        public Id(Long usuarioId, Long rolId) {
            this.usuarioId = usuarioId;
            this.rolId = rolId;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public void setUsuarioId(Long usuarioId) {
            this.usuarioId = usuarioId;
        }

        public Long getRolId() {
            return rolId;
        }

        public void setRolId(Long rolId) {
            this.rolId = rolId;
        }

        @Override
        public boolean equals(Object otro) {
            if (this == otro) {
                return true;
            }
            if (!(otro instanceof Id otraId)) {
                return false;
            }
            return Objects.equals(usuarioId, otraId.usuarioId)
                    && Objects.equals(rolId, otraId.rolId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, rolId);
        }
    }
}
