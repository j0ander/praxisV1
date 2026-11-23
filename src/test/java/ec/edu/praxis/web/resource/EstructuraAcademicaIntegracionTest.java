package ec.edu.praxis.web.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** API-010 a API-023 contra la base aislada praxis_test en 127.0.0.1:55432. */
@QuarkusTest
class EstructuraAcademicaIntegracionTest {

    private static final String ADMIN = "admin.prueba@example.test";
    private static final String ESTUDIANTE = "estudiante.prueba@example.test";
    private static final String CLAVE = "ClavePrueba123!";
    private static final String COOKIE = "PRAXIS_SESSION";
    private static final String PREFIJO = "TK3-";

    @Inject
    DataSource dataSource;

    @Inject
    Config config;

    @BeforeEach
    @AfterEach
    void limpiarSinteticos() throws SQLException {
        ejecutar("DELETE FROM tipo_practica WHERE nombre LIKE '" + PREFIJO + "%'");
        ejecutar("DELETE FROM periodo_academico WHERE codigo LIKE '" + PREFIJO + "%'");
        ejecutar("DELETE FROM carrera WHERE codigo LIKE '" + PREFIJO + "%'");
        ejecutar("DELETE FROM facultad WHERE codigo LIKE '" + PREFIJO + "%'");
    }

    // ------------------------------------------------------------------
    // Aislamiento
    // ------------------------------------------------------------------

    @Test
    void configuracionEfectiva_apuntaExclusivamenteAlClusterAislado() throws SQLException {
        assertEquals("jdbc:postgresql://127.0.0.1:55432/praxis_test",
                config.getValue("quarkus.datasource.jdbc.url", String.class));
        assertEquals("praxis_test", config.getValue("quarkus.datasource.username", String.class));
        assertEquals("none", config.getValue("quarkus.hibernate-orm.schema-management.strategy", String.class));
        assertEquals("America/Guayaquil", config.getValue("quarkus.hibernate-orm.jdbc.timezone", String.class));

        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT current_database(), host(inet_server_addr())::text, "
                                + "inet_server_port(), current_user, current_setting('TimeZone')")) {
            assertTrue(rs.next());
            assertEquals("praxis_test", rs.getString(1));
            assertEquals("127.0.0.1", rs.getString(2));
            assertEquals(55432, rs.getInt(3));
            assertEquals("praxis_test", rs.getString(4));
            assertEquals("America/Guayaquil", rs.getString(5));
        }
    }

    // ------------------------------------------------------------------
    // Carrera
    // ------------------------------------------------------------------

    @Test
    void carrera_creacionCorrecta_devuelve201YResponseExplicita() throws SQLException {
        long facultadId = crearFacultad("TK3-F1", "Facultad sintética 1");
        String cookie = cookieAdmin();

        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("facultadId", facultadId, "codigo", "TK3-C1", "nombre", "Carrera sintética 1"))
                .when().post("/carreras");

        assertEquals(201, response.statusCode(), response.getBody().asString());
        assertEquals(facultadId, response.jsonPath().getLong("facultadId"));
        assertEquals("TK3-C1", response.jsonPath().getString("codigo"));
        assertEquals("Carrera sintética 1", response.jsonPath().getString("nombre"));
        assertTrue(response.jsonPath().getBoolean("activa"));
        assertNotNull(response.jsonPath().getString("createdAt"));
        assertNotNull(response.jsonPath().getString("updatedAt"));
        assertEquals(7, response.jsonPath().getMap("").size());
        assertEquals("true|true", consultar("SELECT activa::text || '|' || "
                + "(facultad_id = " + facultadId + ")::text FROM carrera WHERE codigo = 'TK3-C1'"));
    }

    @Test
    void carrera_facultadInexistente_devuelve409() {
        String cookie = cookieAdmin();

        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("facultadId", 999999999L, "codigo", "TK3-C2", "nombre", "Carrera sin facultad"))
                .when().post("/carreras");

        assertError(response, 409, "REGLA_NEGOCIO");
        assertEquals("La facultad no existe.", response.jsonPath().getString("mensaje"));
    }

    @Test
    void carrera_codigoDuplicadoGlobalmente_devuelve409() throws SQLException {
        long f1 = crearFacultad("TK3-F2", "Facultad sintética 2");
        long f2 = crearFacultad("TK3-F3", "Facultad sintética 3");
        String cookie = cookieAdmin();

        Response primera = postCarrera(cookie, f1, "TK3-COD-DUP", "Nombre A");
        Response segunda = postCarrera(cookie, f2, "TK3-COD-DUP", "Nombre B");

        assertEquals(201, primera.statusCode());
        assertError(segunda, 409, "DUPLICADO");
        assertEquals("1", consultar("SELECT count(*)::text FROM carrera WHERE codigo = 'TK3-COD-DUP'"));
    }

    @Test
    void carrera_nombreDuplicadoEnMismaFacultad_devuelve409() throws SQLException {
        long facultadId = crearFacultad("TK3-F4", "Facultad sintética 4");
        String cookie = cookieAdmin();

        Response primera = postCarrera(cookie, facultadId, "TK3-C3", "Nombre repetido");
        Response segunda = postCarrera(cookie, facultadId, "TK3-C4", "Nombre repetido");

        assertEquals(201, primera.statusCode());
        assertError(segunda, 409, "DUPLICADO");
        assertEquals("Ya existe una carrera con ese nombre en la facultad.",
                segunda.jsonPath().getString("mensaje"));
        assertEquals("1", consultar("SELECT count(*)::text FROM carrera WHERE nombre = 'Nombre repetido'"));
    }

    @Test
    void carrera_nombreIgualEnFacultadesDistintas_sePermite() throws SQLException {
        long f1 = crearFacultad("TK3-F5", "Facultad sintética 5");
        long f2 = crearFacultad("TK3-F6", "Facultad sintética 6");
        String cookie = cookieAdmin();

        Response primera = postCarrera(cookie, f1, "TK3-C5", "Nombre compartido");
        Response segunda = postCarrera(cookie, f2, "TK3-C6", "Nombre compartido");

        assertEquals(201, primera.statusCode(), primera.getBody().asString());
        assertEquals(201, segunda.statusCode(), segunda.getBody().asString());
        assertEquals("2", consultar("SELECT count(*)::text FROM carrera WHERE nombre = 'Nombre compartido'"));
    }

    @Test
    void carrera_buscarYListar_conFiltroPorFacultad() throws SQLException {
        long f1 = crearFacultad("TK3-F7", "Facultad sintética 7");
        long f2 = crearFacultad("TK3-F8", "Facultad sintética 8");
        String cookie = cookieAdmin();
        assertEquals(201, postCarrera(cookie, f1, "TK3-C7", "Carrera uno").statusCode());
        assertEquals(201, postCarrera(cookie, f2, "TK3-C8", "Carrera dos").statusCode());

        long id = Long.parseLong(consultar("SELECT id::text FROM carrera WHERE codigo = 'TK3-C7'"));
        Response buscar = given().cookie(COOKIE, cookie).when().get("/carreras/buscar/" + id);
        assertEquals(200, buscar.statusCode());
        assertEquals("TK3-C7", buscar.jsonPath().getString("codigo"));

        Response todos = given().cookie(COOKIE, cookie).when().get("/carreras/listarTodos");
        assertEquals(200, todos.statusCode());
        assertTrue(todos.jsonPath().getList("$").size() >= 2);

        Response filtrados = given().cookie(COOKIE, cookie)
                .when().get("/carreras/listarTodos?facultadId=" + f2);
        assertEquals(200, filtrados.statusCode());
        assertEquals(1, filtrados.jsonPath().getList("$").size());
        assertEquals("TK3-C8", filtrados.jsonPath().getString("[0].codigo"));
    }

    @Test
    void carrera_actualizacionYRegistroInexistente() throws SQLException {
        long facultadId = crearFacultad("TK3-F9", "Facultad sintética 9");
        String cookie = cookieAdmin();
        assertEquals(201, postCarrera(cookie, facultadId, "TK3-C9", "Carrera original").statusCode());
        long id = Long.parseLong(consultar("SELECT id::text FROM carrera WHERE codigo = 'TK3-C9'"));

        Response actualizar = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("id", id, "facultadId", facultadId, "codigo", "TK3-C9",
                        "nombre", "Carrera actualizada", "activa", false))
                .when().put("/carreras");
        assertEquals(200, actualizar.statusCode(), actualizar.getBody().asString());
        assertEquals("Carrera actualizada", actualizar.jsonPath().getString("nombre"));
        assertFalse(actualizar.jsonPath().getBoolean("activa"));
        assertEquals("false", consultar("SELECT activa::text FROM carrera WHERE id = " + id));

        Response inexistente = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("id", 999999999L, "facultadId", facultadId,
                        "codigo", "TK3-C10", "nombre", "No existe"))
                .when().put("/carreras");
        assertError(inexistente, 404, "NO_ENCONTRADO");
        assertError(given().cookie(COOKIE, cookie).when().get("/carreras/buscar/999999999"),
                404, "NO_ENCONTRADO");
    }

    @Test
    void carrera_camposObligatoriosAusentes_devuelve400() {
        String cookie = cookieAdmin();

        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("facultadId", 1L, "nombre", "Sin código"))
                .when().post("/carreras");

        assertError(response, 400, "DATOS_INVALIDOS");
    }

    // ------------------------------------------------------------------
    // Periodo académico
    // ------------------------------------------------------------------

    @Test
    void periodo_creacionSiempreInactiva_aunSiElClienteEnviaActivo() throws SQLException {
        String cookie = cookieAdmin();

        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("codigo", "TK3-P1", "nombre", "Periodo sintético 1",
                        "fechaInicio", "2026-01-05", "fechaFin", "2026-06-30", "activo", true))
                .when().post("/periodos");

        assertEquals(201, response.statusCode(), response.getBody().asString());
        assertEquals("false", consultar("SELECT activo::text FROM periodo_academico WHERE codigo = 'TK3-P1'"));

        long id = Long.parseLong(consultar("SELECT id::text FROM periodo_academico WHERE codigo = 'TK3-P1'"));
        Response buscar = given().cookie(COOKIE, cookie).when().get("/periodos/buscar/" + id);
        assertEquals(200, buscar.statusCode());
        assertFalse(buscar.jsonPath().getBoolean("activo"));
        assertEquals("2026-01-05", buscar.jsonPath().getString("fechaInicio"));
        assertEquals("2026-06-30", buscar.jsonPath().getString("fechaFin"));
        assertEquals(8, buscar.jsonPath().getMap("").size());
    }

    @Test
    void periodo_fechaFinAnterior_devuelve409() {
        String cookie = cookieAdmin();

        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("codigo", "TK3-P2", "nombre", "Periodo inválido",
                        "fechaInicio", "2026-06-30", "fechaFin", "2026-01-05"))
                .when().post("/periodos");

        assertError(response, 409, "REGLA_NEGOCIO");
    }

    @Test
    void periodo_codigoDuplicado_devuelve409() {
        String cookie = cookieAdmin();
        assertEquals(201, postPeriodo(cookie, "TK3-P3").statusCode());

        Response response = postPeriodo(cookie, "TK3-P3");

        assertError(response, 409, "DUPLICADO");
    }

    @Test
    void periodo_activacionSegundoPeriodoYDesactivacion() throws SQLException {
        String cookie = cookieAdmin();
        assertEquals(201, postPeriodo(cookie, "TK3-P4").statusCode());
        assertEquals(201, postPeriodo(cookie, "TK3-P5").statusCode());
        long p4 = idPeriodo("TK3-P4");
        long p5 = idPeriodo("TK3-P5");

        Response activar = given().cookie(COOKIE, cookie).when().post("/periodos/" + p4 + "/activar");
        assertEquals(200, activar.statusCode(), activar.getBody().asString());
        assertEquals("true", consultar("SELECT activo::text FROM periodo_academico WHERE id = " + p4));

        Response segundo = given().cookie(COOKIE, cookie).when().post("/periodos/" + p5 + "/activar");
        assertError(segundo, 409, "REGLA_NEGOCIO");
        assertEquals("Ya existe otro periodo activo.", segundo.jsonPath().getString("mensaje"));
        assertEquals("false", consultar("SELECT activo::text FROM periodo_academico WHERE id = " + p5));
        assertEquals("1", consultar("SELECT count(*)::text FROM periodo_academico WHERE activo"));

        Response desactivar = given().cookie(COOKIE, cookie).when().post("/periodos/" + p4 + "/desactivar");
        assertEquals(200, desactivar.statusCode(), desactivar.getBody().asString());
        assertEquals("false", consultar("SELECT activo::text FROM periodo_academico WHERE id = " + p4));
        assertEquals("0", consultar("SELECT count(*)::text FROM periodo_academico WHERE activo"));
    }

    @Test
    void periodo_buscarListarYActualizar() throws SQLException {
        String cookie = cookieAdmin();
        assertEquals(201, postPeriodo(cookie, "TK3-P6").statusCode());
        long id = idPeriodo("TK3-P6");

        Response buscar = given().cookie(COOKIE, cookie).when().get("/periodos/buscar/" + id);
        assertEquals(200, buscar.statusCode());
        assertEquals("TK3-P6", buscar.jsonPath().getString("codigo"));

        Response listar = given().cookie(COOKIE, cookie).when().get("/periodos/listarTodos");
        assertEquals(200, listar.statusCode());
        assertTrue(listar.jsonPath().getList("$").size() >= 1);

        Response actualizar = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("id", id, "codigo", "TK3-P6", "nombre", "Periodo actualizado",
                        "fechaInicio", "2026-01-05", "fechaFin", "2026-07-31"))
                .when().put("/periodos");
        assertEquals(200, actualizar.statusCode(), actualizar.getBody().asString());
        assertEquals("Periodo actualizado",
                consultar("SELECT nombre FROM periodo_academico WHERE id = " + id));

        assertError(given().cookie(COOKIE, cookie).when().get("/periodos/buscar/999999999"),
                404, "NO_ENCONTRADO");
    }

    // ------------------------------------------------------------------
    // Tipo de práctica
    // ------------------------------------------------------------------

    @Test
    void tipo_creacionCorrecta() throws SQLException {
        String cookie = cookieAdmin();

        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("nombre", "TK3-Tipo 1", "horasRequeridas", "480.00", "notaMinima", "14.00"))
                .when().post("/tiposPractica");

        assertEquals(201, response.statusCode(), response.getBody().asString());
        assertEquals("480.00|14.00|true", consultar("SELECT horas_requeridas::text || '|' || "
                + "nota_minima::text || '|' || activo::text FROM tipo_practica WHERE nombre = 'TK3-Tipo 1'"));
    }

    @Test
    void tipo_nombreDuplicado_devuelve409() {
        String cookie = cookieAdmin();
        assertEquals(201, postTipo(cookie, "TK3-Tipo 2").statusCode());

        Response response = postTipo(cookie, "TK3-Tipo 2");

        assertError(response, 409, "DUPLICADO");
    }

    @Test
    void tipo_horasIgualesOMenoresQueCero_devuelve409() throws SQLException {
        String cookie = cookieAdmin();

        assertError(postTipo(cookie, "TK3-Tipo 3", "0", "14"), 409, "REGLA_NEGOCIO");
        assertError(postTipo(cookie, "TK3-Tipo 4", "-1", "14"), 409, "REGLA_NEGOCIO");
        assertEquals("0", consultar("SELECT count(*)::text FROM tipo_practica "
                + "WHERE nombre LIKE 'TK3-Tipo 3%' OR nombre LIKE 'TK3-Tipo 4%'"));
    }

    @Test
    void tipo_notaFueraDeRango_devuelve409() {
        String cookie = cookieAdmin();

        assertError(postTipo(cookie, "TK3-Tipo 5", "480", "-0.01"), 409, "REGLA_NEGOCIO");
        assertError(postTipo(cookie, "TK3-Tipo 6", "480", "20.01"), 409, "REGLA_NEGOCIO");
    }

    @Test
    void tipo_actualizacionBuscarYListar() throws SQLException {
        String cookie = cookieAdmin();
        assertEquals(201, postTipo(cookie, "TK3-Tipo 7").statusCode());
        long id = Long.parseLong(consultar(
                "SELECT id::text FROM tipo_practica WHERE nombre = 'TK3-Tipo 7'"));

        Response buscar = given().cookie(COOKIE, cookie).when().get("/tiposPractica/buscar/" + id);
        assertEquals(200, buscar.statusCode());
        assertEquals("TK3-Tipo 7", buscar.jsonPath().getString("nombre"));
        assertEquals(7, buscar.jsonPath().getMap("").size());

        Response listar = given().cookie(COOKIE, cookie).when().get("/tiposPractica/listarTodos");
        assertEquals(200, listar.statusCode());
        assertTrue(listar.jsonPath().getList("$").size() >= 1);

        Response actualizar = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("id", id, "nombre", "TK3-Tipo 7 actualizado",
                        "horasRequeridas", "500.00", "notaMinima", "15.00", "activo", false))
                .when().put("/tiposPractica");
        assertEquals(200, actualizar.statusCode(), actualizar.getBody().asString());
        assertEquals("500.00|15.00|false", consultar("SELECT horas_requeridas::text || '|' || "
                + "nota_minima::text || '|' || activo::text FROM tipo_practica WHERE id = " + id));

        assertError(given().cookie(COOKIE, cookie).when().get("/tiposPractica/buscar/999999999"),
                404, "NO_ENCONTRADO");
    }

    // ------------------------------------------------------------------
    // Autorización y formato de error
    // ------------------------------------------------------------------

    @Test
    void estructuraAcademica_sinAutenticacion_devuelve401EnLecturas() {
        for (String ruta : java.util.List.of("/carreras/listarTodos", "/periodos/listarTodos",
                "/tiposPractica/listarTodos")) {
            assertError(given().when().get(ruta), 401, "SIN_SESION");
        }
    }

    @Test
    void estructuraAcademica_estudianteNoPuedeEscribir_devuelve403() {
        String cookie = login(ESTUDIANTE).getCookie(COOKIE);

        for (String ruta : java.util.List.of("/carreras", "/periodos", "/tiposPractica")) {
            assertError(given().cookie(COOKIE, cookie).contentType(ContentType.JSON).body("{}")
                    .when().post(ruta), 403, "SIN_PERMISO");
            assertError(given().cookie(COOKIE, cookie).contentType(ContentType.JSON).body("{}")
                    .when().put(ruta), 403, "SIN_PERMISO");
        }
        assertError(given().cookie(COOKIE, cookie).when().post("/periodos/999999999/activar"),
                403, "SIN_PERMISO");
        assertError(given().cookie(COOKIE, cookie).when().post("/periodos/999999999/desactivar"),
                403, "SIN_PERMISO");
    }

    @Test
    void errores401y403_conservanFormatoCodigoMensaje() {
        Response sinSesion = given().when().get("/carreras/listarTodos");
        Response sinPermiso = given().cookie(COOKIE, login(ESTUDIANTE).getCookie(COOKIE))
                .contentType(ContentType.JSON).body("{}").when().post("/carreras");

        for (Response response : java.util.List.of(sinSesion, sinPermiso)) {
            Map<?, ?> cuerpo = response.jsonPath().getMap("");
            assertNotNull(cuerpo);
            assertEquals(2, cuerpo.size());
            assertEquals(Set.of("codigo", "mensaje"), cuerpo.keySet());
            String texto = response.getBody().asString().toLowerCase();
            assertFalse(texto.contains("exception"));
            assertFalse(texto.contains("stack"));
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private String cookieAdmin() {
        String cookie = login(ADMIN).getCookie(COOKIE);
        assertNotNull(cookie);
        return cookie;
    }

    private Response login(String correo) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("correo", correo, "password", CLAVE))
                .when().post("/auth/login");
    }

    private Response postCarrera(String cookie, long facultadId, String codigo, String nombre) {
        return given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("facultadId", facultadId, "codigo", codigo, "nombre", nombre))
                .when().post("/carreras");
    }

    private Response postPeriodo(String cookie, String codigo) {
        return given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("codigo", codigo, "nombre", "Periodo sintético",
                        "fechaInicio", "2026-01-05", "fechaFin", "2026-06-30"))
                .when().post("/periodos");
    }

    private Response postTipo(String cookie, String nombre) {
        return postTipo(cookie, nombre, "480", "14");
    }

    private Response postTipo(String cookie, String nombre, String horas, String nota) {
        return given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("nombre", nombre, "horasRequeridas", horas, "notaMinima", nota))
                .when().post("/tiposPractica");
    }

    private long crearFacultad(String codigo, String nombre) throws SQLException {
        ejecutar("INSERT INTO facultad (codigo, nombre) VALUES ('" + codigo + "', '" + nombre + "')");
        return Long.parseLong(consultar("SELECT id::text FROM facultad WHERE codigo = '" + codigo + "'"));
    }

    private long idPeriodo(String codigo) throws SQLException {
        return Long.parseLong(consultar("SELECT id::text FROM periodo_academico WHERE codigo = '" + codigo + "'"));
    }

    private static void assertError(Response response, int status, String codigo) {
        assertEquals(status, response.statusCode(), "Cuerpo: " + response.getBody().asString());
        assertEquals(codigo, response.jsonPath().getString("codigo"),
                "Cuerpo: " + response.getBody().asString());
    }

    private void ejecutar(String sql) throws SQLException {
        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    private String consultar(String sql) throws SQLException {
        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement();
                ResultSet rs = s.executeQuery(sql)) {
            assertTrue(rs.next(), "Sin fila para: " + sql);
            return rs.getString(1);
        }
    }
}
