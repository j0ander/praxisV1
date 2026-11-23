package ec.edu.praxis.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ec.edu.praxis.domain.model.Carrera;

/** Pruebas unitarias de las validaciones de Carrera que no requieren PostgreSQL. */
class CarreraServiceTest {

    @Test
    void validarDatos_datosCompletos_noLanza() {
        assertDoesNotThrow(() -> CarreraService.validarDatos(carrera(1L, "CAR-1", "Ingeniería")));
    }

    @Test
    void validarDatos_sinFacultad_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> CarreraService.validarDatos(carrera(null, "CAR-1", "Ingeniería")));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_sinCodigo_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> CarreraService.validarDatos(carrera(1L, "  ", "Ingeniería")));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_sinNombre_devuelve400() {
        ApiException error = assertThrows(ApiException.class,
                () -> CarreraService.validarDatos(carrera(1L, "CAR-1", null)));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    @Test
    void validarDatos_datosNulos_devuelve400() {
        ApiException error = assertThrows(ApiException.class, () -> CarreraService.validarDatos(null));
        assertEquals(400, error.getStatus());
        assertEquals("DATOS_INVALIDOS", error.getCodigo());
    }

    private static Carrera carrera(Long facultadId, String codigo, String nombre) {
        Carrera carrera = new Carrera();
        carrera.setFacultadId(facultadId);
        carrera.setCodigo(codigo);
        carrera.setNombre(nombre);
        return carrera;
    }
}
