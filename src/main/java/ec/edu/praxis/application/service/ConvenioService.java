package ec.edu.praxis.application.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import ec.edu.praxis.domain.model.Convenio;
import ec.edu.praxis.domain.model.EstadoConvenio;
import ec.edu.praxis.infrastructure.repository.ConvenioRepository;
import ec.edu.praxis.infrastructure.repository.ConvenioRepository.Resumen;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class ConvenioService {

    private static final String ESTADO_VIGENTE = EstadoConvenio.VIGENTE.name();

    @Inject
    ConvenioRepository repository;

    @Inject
    EmpresaRepository empresaRepository;

    public List<ConvenioResumen> listarPorEmpresa(Long empresaId) {
        if (empresaId == null) {
            throw ApiException.datosInvalidos("El id de la empresa es obligatorio.");
        }
        validarEmpresa(empresaId);
        return repository.listarResumenPorEmpresa(empresaId).stream()
                .map(ConvenioResumen::de).toList();
    }

    @Transactional
    public Convenio crear(Convenio datos) {
        validarDatos(datos);
        validarCamposInternos(datos, false);
        validarFechas(datos);
        validarEmpresa(datos.getEmpresaId());
        validarNumeroDuplicado(datos.getNumero(), null);
        datos.setEstado(EstadoConvenio.VIGENTE);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public Convenio actualizar(Convenio datos) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id del convenio es obligatorio.");
        }
        validarCamposInternos(datos, true);
        Convenio actual = obtener(datos.getId());
        validarEstadoEfectivo(actual.getId(), "Solo se pueden editar convenios VIGENTE.");
        validarDatos(datos);
        validarFechas(datos);
        validarEmpresa(datos.getEmpresaId());
        validarNumeroDuplicado(datos.getNumero(), actual.getId());

        actual.setEmpresaId(datos.getEmpresaId());
        actual.setNumero(datos.getNumero());
        actual.setFechaInicio(datos.getFechaInicio());
        actual.setFechaFin(datos.getFechaFin());
        actual.setDocumentoUrl(datos.getDocumentoUrl());
        repository.flush();
        return actual;
    }

    @Transactional
    public void suspender(Long id) {
        Convenio convenio = obtener(id);
        validarEstadoEfectivo(id, "Solo se puede suspender un convenio VIGENTE.");
        convenio.setEstado(EstadoConvenio.SUSPENDIDO);
        repository.flush();
    }

    @Transactional
    public void terminar(Long id) {
        Convenio convenio = obtener(id);

        
        if (convenio.getEstado() != EstadoConvenio.VIGENTE
                && convenio.getEstado() != EstadoConvenio.SUSPENDIDO) {
            throw ApiException.reglaNegocio("El convenio ya está TERMINADO.");
        }
        convenio.setEstado(EstadoConvenio.TERMINADO);
        repository.flush();
    }

    private void validarEstadoEfectivo(Long id, String mensaje) {
        if (!ESTADO_VIGENTE.equals(repository.estadoEfectivoPorId(id))) {
            throw ApiException.reglaNegocio(mensaje);
        }
    }

    private Convenio obtener(Long id) {
        if (id == null) {
            throw ApiException.datosInvalidos("El id del convenio es obligatorio.");
        }
        Convenio convenio = repository.findById(id);
        if (convenio == null) {
            throw ApiException.noEncontrado("Convenio no encontrado.");
        }
        return convenio;
    }

    private void validarEmpresa(Long empresaId) {
        if (empresaId == null) {
            throw ApiException.datosInvalidos("La empresa es obligatoria.");
        }
        if (empresaRepository.findById(empresaId) == null) {
            throw ApiException.noEncontrado("Empresa no encontrada.");
        }
    }

    private void validarNumeroDuplicado(String numero, Long idActual) {
        Convenio porNumero = repository.buscarPorNumero(numero);
        if (porNumero != null && !porNumero.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un convenio con ese número.");
        }
    }

    private static void validarCamposInternos(Convenio datos, boolean permiteId) {
        if (!permiteId && datos.getId() != null) {
            throw ApiException.datosInvalidos("El id no se admite al crear un convenio.");
        }
        if (datos.getCreatedAt() != null || datos.getUpdatedAt() != null) {
            throw ApiException.datosInvalidos("createdAt y updatedAt no se admiten en la solicitud.");
        }
    }

    static void validarDatos(Convenio datos) {
        if (datos == null || datos.getEmpresaId() == null) {
            throw ApiException.datosInvalidos("La empresa es obligatoria.");
        }
        if (datos.getNumero() == null || datos.getNumero().isBlank()) {
            throw ApiException.datosInvalidos("El número de convenio es obligatorio.");
        }
    }

    static void validarFechas(Convenio datos) {
        if (datos == null || datos.getFechaInicio() == null || datos.getFechaFin() == null) {
            throw ApiException.datosInvalidos("Las fechas de vigencia son obligatorias.");
        }
        if (datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw ApiException.reglaNegocio("La fecha de fin no puede ser anterior a la de inicio.");
        }
    }

    public static class ConvenioResumen {

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

        public ConvenioResumen(Long id, Long empresaId, String numero, LocalDate fechaInicio,
                LocalDate fechaFin, EstadoConvenio estado, String estadoEfectivo, String documentoUrl,
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

        private static ConvenioResumen de(Resumen resumen) {
            return new ConvenioResumen(
        resumen.getId(), resumen.getEmpresaId(), resumen.getNumero(),
        resumen.getFechaInicio(), resumen.getFechaFin(), resumen.getEstado(),
        resumen.getEstadoEfectivo(), resumen.getDocumentoUrl(),
        resumen.getCreatedAt(), resumen.getUpdatedAt());
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
