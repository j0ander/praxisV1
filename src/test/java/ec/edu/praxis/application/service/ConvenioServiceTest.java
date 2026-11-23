package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Convenio;
import ec.edu.praxis.domain.model.Empresa;
import ec.edu.praxis.domain.model.EstadoConvenio;
import ec.edu.praxis.infrastructure.repository.ConvenioRepository;
import ec.edu.praxis.infrastructure.repository.ConvenioRepository.Resumen;
import ec.edu.praxis.infrastructure.repository.EmpresaRepository;

/** Pruebas unitarias de convenios, sin PostgreSQL. */
class ConvenioServiceTest {

    private ConvenioService service;
    private ConvenioRepoFalso repository;
    private EmpresaRepoFalso empresas;

    @BeforeEach
    void preparar() {
        service = new ConvenioService();
        repository = new ConvenioRepoFalso();
        empresas = new EmpresaRepoFalso();
        service.repository = repository;
        service.empresaRepository = empresas;
        empresas.agregar(1L);
        empresas.agregar(2L);
    }

    @Test
    void validarFechas_finAnteriorAlInicio_devuelve409() {
        Convenio datos = convenio(null, 1L, "C-1", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 9),
                EstadoConvenio.VIGENTE);
        ApiException error = assertThrows(ApiException.class, () -> ConvenioService.validarFechas(datos));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());

        Convenio iguales = convenio(null, 1L, "C-2", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10),
                EstadoConvenio.VIGENTE);
        assertDoesNotThrow(() -> ConvenioService.validarFechas(iguales));
    }

    @Test
    void crear_numeroDuplicado_devuelve409() {
        repository.agregar(convenio(1L, 1L, "C-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE));

        Convenio datos = convenio(null, 1L, "C-1", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31),
                EstadoConvenio.VIGENTE);
        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(409, error.getStatus());
        assertEquals("DUPLICADO", error.getCodigo());
    }

    @Test
    void crear_conEstadoEnviado_siemprePersisteVigente() {
        Convenio datos = convenio(null, 1L, "C-2", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.TERMINADO);

        Convenio creado = service.crear(datos);

        assertEquals(EstadoConvenio.VIGENTE, creado.getEstado());
        assertEquals(EstadoConvenio.VIGENTE, repository.datos.get(creado.getId()).getEstado());
    }

    @Test
    void crear_sinNumero_devuelve400() {
        Convenio datos = convenio(null, 1L, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void crear_conId_devuelve400() {
        Convenio datos = convenio(88L, 1L, "C-20", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void crear_conCreatedAt_devuelve400() {
        Convenio datos = convenio(null, 1L, "C-21", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);
        asignarFecha(datos, "createdAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void crear_conUpdatedAt_devuelve400() {
        Convenio datos = convenio(null, 1L, "C-22", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);
        asignarFecha(datos, "updatedAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class, () -> service.crear(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(0, repository.datos.size());
    }

    @Test
    void actualizar_sinId_devuelve400() {
        Convenio datos = convenio(null, 1L, "C-10", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);

        ApiException error = assertThrows(ApiException.class, () -> service.actualizar(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void actualizar_conCreatedAt_devuelve400() {
        repository.agregar(convenio(10L, 1L, "C-23", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE));
        Convenio datos = convenio(10L, 1L, "C-23", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);
        asignarFecha(datos, "createdAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class, () -> service.actualizar(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(LocalDate.of(2026, 1, 1), repository.datos.get(10L).getFechaInicio());
    }

    @Test
    void actualizar_conUpdatedAt_devuelve400() {
        repository.agregar(convenio(11L, 1L, "C-24", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE));
        Convenio datos = convenio(11L, 1L, "C-24", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);
        asignarFecha(datos, "updatedAt", OffsetDateTime.parse("2020-01-01T00:00:00Z"));

        ApiException error = assertThrows(ApiException.class, () -> service.actualizar(datos));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
        assertEquals(LocalDate.of(2026, 1, 1), repository.datos.get(11L).getFechaInicio());
    }

    @Test
    void actualizar_conEstadoEnviado_noModificaElEstado() {
        repository.agregar(convenio(9L, 1L, "C-11", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE));

        Convenio datos = convenio(9L, 1L, "C-11", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.TERMINADO);
        service.actualizar(datos);

        assertEquals(EstadoConvenio.VIGENTE, repository.datos.get(9L).getEstado());
        assertEquals(LocalDate.of(2026, 2, 1), repository.datos.get(9L).getFechaInicio());
    }

    @Test
    void actualizar_convenioSuspendido_devuelve409() {
        repository.agregar(convenio(2L, 1L, "C-3", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.SUSPENDIDO));

        Convenio datos = convenio(2L, 1L, "C-3", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE);
        ApiException error = assertThrows(ApiException.class, () -> service.actualizar(datos));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void actualizar_convenioVencido_devuelve409() {
        repository.agregar(convenio(12L, 1L, "C-25", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                EstadoConvenio.VIGENTE));
        repository.estadosEfectivos.put(12L, "VENCIDO");

        Convenio datos = convenio(12L, 1L, "C-25", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31),
                EstadoConvenio.VIGENTE);
        ApiException error = assertThrows(ApiException.class, () -> service.actualizar(datos));

        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
        assertEquals(LocalDate.of(2025, 1, 1), repository.datos.get(12L).getFechaInicio());
    }

    @Test
    void suspender_convenioVencido_devuelve409() {
        repository.agregar(convenio(3L, 1L, "C-4", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                EstadoConvenio.VIGENTE));
        repository.estadosEfectivos.put(3L, "VENCIDO");

        ApiException error = assertThrows(ApiException.class, () -> service.suspender(3L));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
        assertEquals(EstadoConvenio.VIGENTE, repository.datos.get(3L).getEstado());
    }

    @Test
    void suspender_convenioVigente_cambiaEstado() {
        repository.agregar(convenio(4L, 1L, "C-5", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.VIGENTE));
        repository.estadosEfectivos.put(4L, "VIGENTE");

        service.suspender(4L);

        assertEquals(EstadoConvenio.SUSPENDIDO, repository.datos.get(4L).getEstado());
    }

    @Test
    void terminar_convenioSuspendido_cambiaEstado() {
        repository.agregar(convenio(5L, 1L, "C-6", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.SUSPENDIDO));

        service.terminar(5L);

        assertEquals(EstadoConvenio.TERMINADO, repository.datos.get(5L).getEstado());
    }

    @Test
    void terminar_convenioVencido_cambiaEstado() {
        repository.agregar(convenio(6L, 1L, "C-7", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                EstadoConvenio.VIGENTE));
        repository.estadosEfectivos.put(6L, "VENCIDO");

        service.terminar(6L);

        assertEquals(EstadoConvenio.TERMINADO, repository.datos.get(6L).getEstado());
    }

    @Test
    void terminar_convenioYaTerminado_devuelve409() {
        repository.agregar(convenio(7L, 1L, "C-8", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoConvenio.TERMINADO));

        ApiException error = assertThrows(ApiException.class, () -> service.terminar(7L));
        assertEquals(409, error.getStatus());
    }

    @Test
    void listarPorEmpresa_representanteOtraEmpresa_devuelve404() {
        ApiException error = assertThrows(ApiException.class,
                () -> service.listarPorEmpresa(1L, 99L, false, true));
        assertEquals(404, error.getStatus());
    }

    @Test
    void listarPorEmpresa_estadoEfectivoDeLaVista_sePropaga() {
        repository.resumenes.add(new Resumen(8L, 1L, "C-9", LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31), EstadoConvenio.VIGENTE, "VENCIDO", null, null, null));

        List<ConvenioService.ConvenioResumen> respuesta =
                service.listarPorEmpresa(1L, 77L, true, false);

        assertEquals(1, respuesta.size());
        ConvenioService.ConvenioResumen resumen = respuesta.get(0);
        assertEquals("VENCIDO", resumen.getEstadoEfectivo());
        assertEquals(8L, resumen.getId());
        assertEquals(1L, resumen.getEmpresaId());
        assertEquals("C-9", resumen.getNumero());
        assertEquals(LocalDate.of(2025, 1, 1), resumen.getFechaInicio());
        assertEquals(LocalDate.of(2025, 12, 31), resumen.getFechaFin());
        assertEquals(EstadoConvenio.VIGENTE, resumen.getEstado());
        assertEquals("VENCIDO", resumen.getEstadoEfectivo());
        assertEquals(Set.of("id", "empresaId", "numero", "fechaInicio", "fechaFin", "estado",
                "estadoEfectivo", "documentoUrl", "createdAt", "updatedAt"),
                Arrays.stream(ConvenioService.ConvenioResumen.class.getDeclaredFields())
                        .map(Field::getName).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void suspender_convenioInexistente_devuelve404() {
        ApiException error = assertThrows(ApiException.class, () -> service.suspender(999L));
        assertEquals(404, error.getStatus());
    }

    private static Convenio convenio(Long id, Long empresaId, String numero, LocalDate inicio, LocalDate fin,
            EstadoConvenio estado) {
        Convenio convenio = new Convenio();
        if (id != null) {
            asignarId(convenio, id);
        }
        convenio.setEmpresaId(empresaId);
        convenio.setNumero(numero);
        convenio.setFechaInicio(inicio);
        convenio.setFechaFin(fin);
        convenio.setEstado(estado);
        return convenio;
    }

    private static void asignarId(Object entidad, Long id) {
        try {
            Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void asignarFecha(Object entidad, String campo, Object valor) {
        try {
            Field campoFecha = entidad.getClass().getDeclaredField(campo);
            campoFecha.setAccessible(true);
            campoFecha.set(entidad, valor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class EmpresaRepoFalso extends EmpresaRepository {

        private final Set<Long> existentes = new HashSet<>();
        private final Set<Long> miembros = new HashSet<>();

        void agregar(Long id) {
            existentes.add(id);
        }

        @Override
        public Empresa findById(Long id) {
            if (!existentes.contains(id)) {
                return null;
            }
            Empresa empresa = new Empresa();
            asignarId(empresa, id);
            return empresa;
        }

        @Override
        public boolean perteneceAUsuario(Long empresaId, Long usuarioId) {
            return miembros.contains(usuarioId);
        }
    }

    private static final class ConvenioRepoFalso extends ConvenioRepository {

        private final Map<Long, Convenio> datos = new LinkedHashMap<>();
        private final Map<String, Convenio> porNumero = new LinkedHashMap<>();
        private final Map<Long, String> estadosEfectivos = new LinkedHashMap<>();
        private final List<Resumen> resumenes = new ArrayList<>();
        private long siguiente = 1;

        void agregar(Convenio convenio) {
            if (convenio.getId() == null) {
                asignarId(convenio, siguiente++);
            }
            datos.put(convenio.getId(), convenio);
            porNumero.put(convenio.getNumero(), convenio);
        }

        @Override
        public Convenio findById(Long id) {
            return datos.get(id);
        }

        @Override
        public Convenio buscarPorNumero(String numero) {
            return porNumero.get(numero);
        }

        @Override
        public List<Resumen> listarResumenPorEmpresa(Long empresaId) {
            return resumenes;
        }

        @Override
        public String estadoEfectivoPorId(Long id) {
            if (estadosEfectivos.containsKey(id)) {
                return estadosEfectivos.get(id);
            }
            Convenio convenio = datos.get(id);
            return convenio == null || convenio.getEstado() == null
                    ? null : convenio.getEstado().name();
        }

        @Override
        public void persistAndFlush(Convenio entidad) {
            if (entidad.getId() == null) {
                asignarId(entidad, siguiente++);
            }
            datos.put(entidad.getId(), entidad);
            porNumero.put(entidad.getNumero(), entidad);
        }

        @Override
        public void flush() {
        }
    }
}
