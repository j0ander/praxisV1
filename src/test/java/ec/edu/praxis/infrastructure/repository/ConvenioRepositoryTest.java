package ec.edu.praxis.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** Verificación del uso de la vista de estado efectivo, sin PostgreSQL. */
class ConvenioRepositoryTest {

    @Test
    void listarResumen_usaLaVistaYNoLaEscribe() {
        String sql = ConvenioRepository.SQL_LISTAR_RESUMEN.toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("v_convenio_estado"));
        assertTrue(sql.contains("estado_efectivo"));
        assertFalse(sql.matches("(?s).*\\b(insert|update|delete)\\b.*"));
    }

    @Test
    void estadoEfectivo_leeUnicamenteLaVista() {
        String sql = ConvenioRepository.SQL_ESTADO_EFECTIVO.toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("v_convenio_estado"));
        assertTrue(sql.contains("estado_efectivo"));
        assertFalse(sql.matches("(?s).*\\b(insert|update|delete)\\b.*"));
    }
}
