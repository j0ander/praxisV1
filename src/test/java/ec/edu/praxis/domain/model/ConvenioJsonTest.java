package ec.edu.praxis.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Pruebas unitarias del enlace JSON de Convenio, sin PostgreSQL. */
class ConvenioJsonTest {

    private ObjectMapper mapper;

    @BeforeEach
    void preparar() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void estado_noSeAsignaDesdeJson() throws Exception {
        String json = """
                {"empresaId":1,"numero":"C-1","fechaInicio":"2026-01-01","fechaFin":"2026-12-31",
                 "estado":"TERMINADO","estadoEfectivo":"VENCIDO"}
                """;

        Convenio convenio = mapper.readValue(json, Convenio.class);

        assertEquals(EstadoConvenio.VIGENTE, convenio.getEstado());
    }

    @Test
    void estado_siguePresenteEnLaSerializacion() throws Exception {
        Convenio convenio = new Convenio();
        convenio.setEstado(EstadoConvenio.SUSPENDIDO);

        String json = mapper.writeValueAsString(convenio);

        assertTrue(json.contains("\"estado\":\"SUSPENDIDO\""));
    }
}
