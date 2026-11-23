package ec.edu.praxis.domain.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Entidad de la tabla usuario_carrera (MODELO_DATOS.md §4.6), con clave primaria compuesta. */
@Entity
@Table(name = "usuario_carrera")
public class UsuarioCarrera {

    @EmbeddedId
    private Id id;

    public UsuarioCarrera() {
    }

    public UsuarioCarrera(Long usuarioId, Long carreraId) {
        this.id = new Id(usuarioId, carreraId);
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

        @Column(name = "carrera_id", nullable = false)
        private Long carreraId;

        public Id() {
        }

        public Id(Long usuarioId, Long carreraId) {
            this.usuarioId = usuarioId;
            this.carreraId = carreraId;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public void setUsuarioId(Long usuarioId) {
            this.usuarioId = usuarioId;
        }

        public Long getCarreraId() {
            return carreraId;
        }

        public void setCarreraId(Long carreraId) {
            this.carreraId = carreraId;
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
                    && Objects.equals(carreraId, otraId.carreraId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, carreraId);
        }
    }
}
