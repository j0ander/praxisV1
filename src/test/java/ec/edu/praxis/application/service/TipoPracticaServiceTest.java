package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.TipoPractica;

/** Pruebas unitarias de las reglas de TipoPractica que no requieren PostgreSQL. */
class TipoPracticaServiceTest {

    @Test
    void validarDatos_datosValidos_noLanza() {
        assertDoesNotThrow(() -> TipoPracticaService.validarDatos(tipo("480.00", "14.00")));
    }

    @Test
    void validarDatos_limitesInferiores_noLanza() {
        assertDoesNotThrow(() -> TipoPracticaService.validarDatos(tipo("0.01", "0.00")));
    }

    @Test
    void validarDatos_limiteSuperiorDeNota_noLanza() {
        assertDoesNotThrow(() -> TipoPracticaService.validarDatos(tipo("100.00", "20.00")));
    }

    @Test
    void validarDatos_sinDatos_devuelve400() {
        ApiException error = assertThrows(ApiException.class, () -> TipoPracticaService.validarDatos(null));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_horasCero_devuelve409() {
        ApiException error = assertThrows(ApiException.class, () -> TipoPracticaService.validarDatos(tipo("0", "14")));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void validarDatos_horasNegativas_devuelve409() {
        ApiException error = assertThrows(ApiException.class, () -> TipoPracticaService.validarDatos(tipo("-1", "14")));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void validarDatos_notaNegativa_devuelve409() {
        ApiException error = assertThrows(ApiException.class, () -> TipoPracticaService.validarDatos(tipo("480", "-0.01")));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    @Test
    void validarDatos_notaMayorAVeinte_devuelve409() {
        ApiException error = assertThrows(ApiException.class, () -> TipoPracticaService.validarDatos(tipo("480", "20.01")));
        assertEquals(409, error.getStatus());
        assertEquals("REGLA_NEGOCIO", error.getCodigo());
    }

    private static TipoPractica tipo(String horas, String nota) {
        TipoPractica tipo = new TipoPractica();
        tipo.setNombre("Práctica sintética");
        tipo.setHorasRequeridas(new BigDecimal(horas));
        tipo.setNotaMinima(new BigDecimal(nota));
        return tipo;
    }
}
