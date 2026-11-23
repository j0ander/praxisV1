package ec.edu.praxis.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** Verificación de las consultas de ámbito, sin PostgreSQL. */
class EmpresaRepositoryTest {

    @Test
    void pertenece_leeMembresiaEfectivaSinEscribir() {
        String sql = EmpresaRepository.SQL_PERTENECE.toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("empresa_usuario"));
        assertTrue(sql.contains("eu.activo"));
        assertTrue(sql.contains("u.activo"));
        assertTrue(sql.contains("usuario_rol"));
        assertTrue(sql.contains("'representante_empresa'"));
        assertFalse(sql.matches("(?s).*\\b(insert|update|delete)\\b.*"));
    }

    @Test
    void administra_exigeMembresiaEfectivaYRolInternoADMIN_EMPRESA() {
        String sql = EmpresaRepository.SQL_ADMINISTRA;

        assertTrue(sql.contains("empresa_usuario"));
        assertTrue(sql.contains("eu.activo"));
        assertTrue(sql.contains("u.activo"));
        assertTrue(sql.contains("usuario_rol"));
        assertTrue(sql.contains("'REPRESENTANTE_EMPRESA'"));
        assertTrue(sql.contains("'ADMIN_EMPRESA'"));
        assertFalse(sql.toLowerCase(Locale.ROOT).matches("(?s).*\\b(insert|update|delete)\\b.*"));
    }
}
