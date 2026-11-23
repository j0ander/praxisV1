package ec.edu.praxis.application.service;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** API-001 a API-003 y protección de FacultadResource contra la base aislada. */
@QuarkusTest
class AutenticacionIntegracionTest {

    private static final String ADMIN = "admin.prueba@example.test";
    private static final String ESTUDIANTE = "estudiante.prueba@example.test";
    private static final String CLAVE = "ClavePrueba123!";
    private static final String COOKIE = "PRAXIS_SESSION";

    @Inject
    DataSource dataSource;

    @Inject
    SesionStore sesionStore;

    @Inject
    Config config;

    @BeforeEach
    void reiniciarEstado() throws SQLException {
        ejecutar("UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL "
                + "WHERE correo IN ('" + ADMIN + "','" + ESTUDIANTE + "')");
        sesionStore.usarReloj(Clock.systemUTC());
    }

    @AfterEach
    void restaurarReloj() {
        sesionStore.usarReloj(Clock.systemUTC());
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

        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement()) {
            try (ResultSet rs = s.executeQuery(
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
    }

    // ------------------------------------------------------------------
    // Autenticación
    // ------------------------------------------------------------------

    @Test
    void loginAdmin_correcto_devuelveCuatroCamposYCookie() {
        Response response = login(ADMIN, CLAVE);

        assertEquals(200, response.statusCode());
        Map<?, ?> cuerpo = response.jsonPath().getMap("");
        assertNotNull(cuerpo);
        assertEquals(4, cuerpo.size(), "SesionResponse debe tener exactamente cuatro campos");
        assertTrue(cuerpo.keySet().containsAll(java.util.Set.of(
                "usuarioId", "correo", "nombres", "roles")));
        assertEquals(ADMIN, cuerpo.get("correo"));
        assertEquals("Admin", cuerpo.get("nombres"));
        assertEquals(java.util.List.of("ADMINISTRADOR"), cuerpo.get("roles"));
        assertNotNull(cuerpo.get("usuarioId"));

        String texto = response.getBody().asString();
        assertFalse(texto.toLowerCase().contains("hash"));
        assertFalse(texto.contains("intentos"));
        assertFalse(texto.toLowerCase().contains("bloque"));
        assertFalse(texto.contains(CLAVE));
    }

    @Test
    void loginEstudiante_correcto_devuelveRolEstudiante() {
        Response response = login(ESTUDIANTE, CLAVE);

        assertEquals(200, response.statusCode());
        assertEquals(java.util.List.of("ESTUDIANTE"), response.jsonPath().getList("roles"));
        assertEquals(ESTUDIANTE, response.jsonPath().getString("correo"));
        assertEquals("Estudiante", response.jsonPath().getString("nombres"));
    }

    @Test
    void login_passwordIncorrecta_devuelve401Generico() {
        Response response = login(ADMIN, "ClaveIncorrecta123!");

        assertError(response, 401, "SIN_SESION");
        assertEquals("Credenciales inválidas.", response.jsonPath().getString("mensaje"));
    }

    @Test
    void login_correoInexistente_devuelveLaMismaRespuesta() {
        Response inexistente = login("no.existe@example.test", CLAVE);
        Response passwordIncorrecta = login(ADMIN, "ClaveIncorrecta123!");

        assertError(inexistente, 401, "SIN_SESION");
        assertEquals(passwordIncorrecta.getBody().asString(), inexistente.getBody().asString(),
                "La respuesta no debe revelar si el correo existe");
    }

    @Test
    void login_mayusculasYEspacios_normalizaElCorreo() {
        Response response = login("   ADMIN.PRUEBA@EXAMPLE.TEST   ", CLAVE);

        assertEquals(200, response.statusCode());
        assertEquals(ADMIN, response.jsonPath().getString("correo"));
    }

    @Test
    void login_fallido_noCreaSesionesNiCookie() {
        int antes = sesionStore.cantidadSesiones();

        Response response = login(ADMIN, "ClaveIncorrecta123!");

        assertError(response, 401, "SIN_SESION");
        assertNull(response.getCookie(COOKIE));
        assertEquals(antes, sesionStore.cantidadSesiones(),
                "Un fallo de autenticación no debe crear sesiones");
    }

    @Test
    void login_datosIncompletos_devuelve400() {
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("correo", ADMIN))
                .when().post("/auth/login");

        assertError(response, 400, "DATOS_INVALIDOS");
    }

    // ------------------------------------------------------------------
    // Intentos y bloqueo
    // ------------------------------------------------------------------

    @Test
    void cuatroFallosNoBloquean_quintoFalloBloquea() throws SQLException {
        for (int i = 0; i < 4; i++) {
            assertError(login(ESTUDIANTE, "ClaveIncorrecta123!"), 401, "SIN_SESION");
        }
        String estado = consultar("SELECT intentos_fallidos || '|' || "
                + "(bloqueado_hasta IS NULL) FROM usuario WHERE correo = '" + ESTUDIANTE + "'");
        assertEquals("4|true", estado, "Con cuatro fallos todavía no debe estar bloqueado");

        Response quinto = login(ESTUDIANTE, "ClaveIncorrecta123!");
        assertError(quinto, 401, "SIN_SESION");

        estado = consultar("SELECT intentos_fallidos || '|' || "
                + "(bloqueado_hasta IS NOT NULL) FROM usuario WHERE correo = '" + ESTUDIANTE + "'");
        assertEquals("5|true", estado);
    }

    @Test
    void duranteBloqueo_elLoginCorrectoNoIncrementaIntentos() throws SQLException {
        for (int i = 0; i < 5; i++) {
            login(ESTUDIANTE, "ClaveIncorrecta123!");
        }

        Response duranteBloqueo = login(ESTUDIANTE, CLAVE);
        assertError(duranteBloqueo, 401, "SIN_SESION");
        assertEquals("Credenciales inválidas.", duranteBloqueo.jsonPath().getString("mensaje"));
        assertEquals("5", consultar("SELECT intentos_fallidos::text FROM usuario WHERE correo = '"
                + ESTUDIANTE + "'"));
    }

    @Test
    void bloqueoVencido_permiteNuevoIntentoYReiniciaContador() throws SQLException {
        for (int i = 0; i < 5; i++) {
            login(ESTUDIANTE, "ClaveIncorrecta123!");
        }
        ejecutar("UPDATE usuario SET bloqueado_hasta = NOW() - INTERVAL '1 minute' "
                + "WHERE correo = '" + ESTUDIANTE + "'");

        Response response = login(ESTUDIANTE, CLAVE);

        assertEquals(200, response.statusCode());
        assertEquals("0|true", consultar("SELECT intentos_fallidos || '|' || "
                + "(bloqueado_hasta IS NULL) FROM usuario WHERE correo = '" + ESTUDIANTE + "'"));
    }

    // ------------------------------------------------------------------
    // Sesiones y cookies
    // ------------------------------------------------------------------

    @Test
    void loginYoLogout_elCicloCompletoInvalidaLaSesion() {
        Response entrada = login(ADMIN, CLAVE);
        assertEquals(200, entrada.statusCode());
        String cookie = entrada.getCookie(COOKIE);
        assertNotNull(cookie);

        Response yo = given().cookie(COOKIE, cookie).when().get("/auth/yo");
        assertEquals(200, yo.statusCode());
        assertEquals(4, yo.jsonPath().getMap("").size());
        assertEquals(ADMIN, yo.jsonPath().getString("correo"));
        assertEquals(java.util.List.of("ADMINISTRADOR"), yo.jsonPath().getList("roles"));

        Response salida = given().cookie(COOKIE, cookie).when().post("/auth/logout");
        assertEquals(204, salida.statusCode());
        cabeceraExpiraCookie(salida);

        Response despues = given().cookie(COOKIE, cookie).when().get("/auth/yo");
        assertError(despues, 401, "SIN_SESION");
    }

    @Test
    void login_cookieIncluyeAtributosDeSeguridad() {
        Response response = login(ADMIN, CLAVE);
        String cabecera = cabeceraSetCookie(response);

        assertTrue(cabecera.startsWith(COOKIE + "="));
        assertTrue(cabecera.contains("; Path=/"));
        assertTrue(cabecera.contains("; HttpOnly"));
        assertTrue(cabecera.contains("; SameSite=Lax"));
        assertTrue(cabecera.contains("; Max-Age=1800"));
        assertFalse(cabecera.contains("; Secure"),
                "La prueba corre por HTTP; Secure se comprueba en la prueba unitaria HTTPS");
    }

    @Test
    void yo_sinCookie_devuelve401() {
        assertError(given().when().get("/auth/yo"), 401, "SIN_SESION");
    }

    @Test
    void yo_conCookieInvalida_devuelve401() {
        assertError(given().cookie(COOKIE, "cookie-invalida").when().get("/auth/yo"),
                401, "SIN_SESION");
    }

    @Test
    void sesionVencidaPorDuracion_devuelve401() {
        String cookie = login(ADMIN, CLAVE).getCookie(COOKIE);
        assertNotNull(cookie);

        sesionStore.usarReloj(Clock.offset(Clock.systemUTC(), Duration.ofMinutes(31)));

        assertError(given().cookie(COOKIE, cookie).when().get("/auth/yo"), 401, "SIN_SESION");
        assertEquals(0, sesionStore.cantidadSesiones());
    }

    @Test
    void logout_sinSesion_devuelve401() {
        assertError(given().when().post("/auth/logout"), 401, "SIN_SESION");
    }

    // ------------------------------------------------------------------
    // Autorización
    // ------------------------------------------------------------------

    @Test
    void facultades_sinAutenticacion_devuelve401() {
        assertError(given().when().get("/facultades/listarTodos"), 401, "SIN_SESION");
    }

    @Test
    void facultades_estudianteNoPuedeEscribir_devuelve403() {
        String cookie = login(ESTUDIANTE, CLAVE).getCookie(COOKIE);

        assertError(given().cookie(COOKIE, cookie).contentType(ContentType.JSON).body("{}")
                .when().post("/facultades"), 403, "SIN_PERMISO");
        assertError(given().cookie(COOKIE, cookie).contentType(ContentType.JSON).body("{}")
                .when().put("/facultades"), 403, "SIN_PERMISO");
        assertError(given().cookie(COOKIE, cookie)
                .when().delete("/facultades/eliminar/999999"), 403, "SIN_PERMISO");
    }

    @Test
    void facultades_adminPuedeLeer_ySuperaLaAutorizacionDeEscritura() {
        String cookie = login(ADMIN, CLAVE).getCookie(COOKIE);

        Response lectura = given().cookie(COOKIE, cookie).when().get("/facultades/listarTodos");
        assertEquals(200, lectura.statusCode());
        assertNotNull(lectura.jsonPath().getList("$"));

        // Se usan solicitudes sin datos válidos para demostrar que la autorización
        // pasa, sin insertar datos institucionales.
        Response crear = given().cookie(COOKIE, cookie).contentType(ContentType.JSON).body("{}")
                .when().post("/facultades");
        assertEquals(400, crear.statusCode());
        assertEquals("DATOS_INVALIDOS", crear.jsonPath().getString("codigo"));

        Response actualizar = given().cookie(COOKIE, cookie).contentType(ContentType.JSON).body("{}")
                .when().put("/facultades");
        assertEquals(400, actualizar.statusCode());
        assertEquals("DATOS_INVALIDOS", actualizar.jsonPath().getString("codigo"));

        Response eliminar = given().cookie(COOKIE, cookie)
                .when().delete("/facultades/eliminar/999999");
        assertEquals(404, eliminar.statusCode());
        assertEquals("NO_ENCONTRADO", eliminar.jsonPath().getString("codigo"));
    }

    @Test
    void errores401y403_noExponenDetallesInternos() {
        Response sinSesion = given().when().get("/auth/yo");
        String cookieEstudiante = login(ESTUDIANTE, CLAVE).getCookie(COOKIE);
        Response sinPermiso = given().cookie(COOKIE, cookieEstudiante)
                .contentType(ContentType.JSON).body("{}").when().post("/facultades");

        for (Response response : java.util.List.of(sinSesion, sinPermiso)) {
            Map<?, ?> cuerpo = response.jsonPath().getMap("");
            assertNotNull(cuerpo, "El error debe ser JSON con codigo y mensaje");
            assertEquals(2, cuerpo.size());
            assertTrue(cuerpo.keySet().containsAll(java.util.Set.of("codigo", "mensaje")));
            String texto = response.getBody().asString().toLowerCase();
            assertFalse(texto.contains("exception"));
            assertFalse(texto.contains("stack"));
            assertFalse(texto.contains("hash"));
            assertFalse(texto.contains("claveprueba"));
            assertFalse(texto.contains("bloqueado hasta"));
            assertFalse(texto.contains("intentos_fallidos"));
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Response login(String correo, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("correo", correo, "password", password))
                .when().post("/auth/login");
    }

    private static void assertError(Response response, int status, String codigo) {
        assertEquals(status, response.statusCode(),
                "Cuerpo: " + response.getBody().asString());
        assertEquals(codigo, response.jsonPath().getString("codigo"),
                "Cuerpo: " + response.getBody().asString());
    }

    private static String cabeceraSetCookie(Response response) {
        String cabecera = response.getHeader("Set-Cookie");
        assertNotNull(cabecera, "Debe existir la cabecera Set-Cookie");
        return cabecera;
    }

    private static void cabeceraExpiraCookie(Response response) {
        String cabecera = cabeceraSetCookie(response);
        assertTrue(cabecera.startsWith(COOKIE + "=;"));
        assertTrue(cabecera.contains("; Max-Age=0"));
        assertTrue(cabecera.contains("; Path=/"));
        assertTrue(cabecera.contains("; HttpOnly"));
        assertTrue(cabecera.contains("; SameSite=Lax"));
    }

    private void ejecutar(String sql) throws SQLException {
        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    private String consultar(String sql) throws SQLException {
        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement();
                ResultSet rs = s.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getString(1);
        }
    }
}
