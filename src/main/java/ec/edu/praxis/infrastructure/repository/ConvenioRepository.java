package ec.edu.praxis.infrastructure.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import ec.edu.praxis.domain.model.Convenio;
import ec.edu.praxis.domain.model.EstadoConvenio;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;


@ApplicationScoped
public class ConvenioRepository implements PanacheRepositoryBase<Convenio, Long> {

    static final String SQL_LISTAR_RESUMEN =
            "SELECT c.id, c.empresa_id, c.numero, c.fecha_inicio, c.fecha_fin, c.estado, "
        + "v.estado_efectivo, c.documento_url, c.created_at, c.updated_at "
        + "FROM convenio c JOIN v_convenio_estado v ON v.id = c.id "
        + "WHERE c.empresa_id = :empresaId ORDER BY c.fecha_inicio DESC, c.id DESC";

    static final String SQL_ESTADO_EFECTIVO =
            "SELECT estado_efectivo FROM v_convenio_estado WHERE id = :id";

    @Inject
    EntityManager em;

    public List<Convenio> listarPorEmpresa(Long empresaId) {
        return em.createQuery(
                "SELECT c FROM Convenio c WHERE c.empresaId = :empresaId ORDER BY c.id", Convenio.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    public Convenio buscarPorNumero(String numero) {
        try {
            return em.createQuery("SELECT c FROM Convenio c WHERE c.numero = :numero", Convenio.class)
        .setParameter("numero", numero)
        .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Resumen> listarResumenPorEmpresa(Long empresaId) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createNativeQuery(SQL_LISTAR_RESUMEN)
                .setParameter("empresaId", empresaId)
                .getResultList();
        List<Resumen> respuesta = new ArrayList<>(filas.size());
        for (Object[] fila : filas) {
            respuesta.add(new Resumen(
        ((Number) fila[0]).longValue(),
        ((Number) fila[1]).longValue(),
        (String) fila[2],
        aLocalDate(fila[3]),
        aLocalDate(fila[4]),
        EstadoConvenio.valueOf((String) fila[5]),
        (String) fila[6],
        (String) fila[7],
        aOffsetDateTime(fila[8]),
        aOffsetDateTime(fila[9])));
        }
        return respuesta;
    }

    public String estadoEfectivoPorId(Long id) {
        try {
            Object valor = em.createNativeQuery(SQL_ESTADO_EFECTIVO)
        .setParameter("id", id)
        .getSingleResult();
            return valor == null ? null : valor.toString();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static LocalDate aLocalDate(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof Date fecha) {
            return fecha.toLocalDate();
        }
        return LocalDate.parse(valor.toString());
    }

    private static OffsetDateTime aOffsetDateTime(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof OffsetDateTime fecha) {
            return fecha;
        }
        return OffsetDateTime.parse(valor.toString());
    }


    public static class Resumen {

        private final Long id;
        private final Long empresaId;
        private final String numero;
        private final LocalDate fechaInicio;
        private final LocalDate fechaFin;
        private final EstadoConvenio estado;
        private final String estadoEfectivo;
        private final String documentoUrl;
        private final OffsetDateTime createdAt;
        private final OffsetDateTime updatedAt;

        public Resumen(Long id, Long empresaId, String numero, LocalDate fechaInicio, LocalDate fechaFin,
                EstadoConvenio estado, String estadoEfectivo, String documentoUrl,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
            this.id = id;
            this.empresaId = empresaId;
            this.numero = numero;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.estado = estado;
            this.estadoEfectivo = estadoEfectivo;
            this.documentoUrl = documentoUrl;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() {
            return id;
        }

        public Long getEmpresaId() {
            return empresaId;
        }

        public String getNumero() {
            return numero;
        }

        public LocalDate getFechaInicio() {
            return fechaInicio;
        }

        public LocalDate getFechaFin() {
            return fechaFin;
        }

        public EstadoConvenio getEstado() {
            return estado;
        }

        public String getEstadoEfectivo() {
            return estadoEfectivo;
        }

        public String getDocumentoUrl() {
            return documentoUrl;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}
