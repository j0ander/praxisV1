package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.PeriodoAcademico;

/** Pruebas unitarias de las reglas de PeriodoAcademico que no requieren PostgreSQL. */
class PeriodoAcademicoServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 5);
    private static final LocalDate FIN = LocalDate.of(2026, 6, 30);

    @Test
    void validarDatos_datosCompletos_noLanza() {
        assertDoesNotThrow(() -> PeriodoAcademicoService.validarDatos(periodo("TK3-P1", INICIO, FIN)));
    }

    @Test
    void validarDatos_fechasIguales_noLanza() {
        assertDoesNotThrow(() -> PeriodoAcademicoService.validarDatos(periodo("TK3-P1", INICIO, INICIO)));
    }

    @Test
    void validarDatos_sinDatos_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> PeriodoAcademicoService.validarDatos(null));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_sinCodigo_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> PeriodoAcademicoService.validarDatos(periodo("  ", INICIO, FIN)));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_sinFechaInicio_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> PeriodoAcademicoService.validarDatos(periodo("TK3-P1", null, FIN)));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_sinFechaFin_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> PeriodoAcademicoService.validarDatos(periodo("TK3-P1", INICIO, null)));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_fechaFinAnterior_devuelve409() {
        ApiException error = assertThrows(ApiException.class,
                () -> PeriodoAcademicoService.validarDatos(periodo("TK3-P1", FIN, INICIO)));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    private static PeriodoAcademico periodo(String codigo, LocalDate inicio, LocalDate fin) {
        PeriodoAcademico periodo = new PeriodoAcademico();
        periodo.setCodigo(codigo);
        periodo.setNombre("Periodo sintético");
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        return periodo;
    }
}
