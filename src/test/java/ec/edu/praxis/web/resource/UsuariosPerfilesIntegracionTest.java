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
import java.util.List;
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

/** API-024 a API-038 contra praxis_test en 127.0.0.1:55432, sin DDL. */
@QuarkusTest
class UsuariosPerfilesIntegracionTest {

    private static final String ADMIN = "admin.prueba@example.test";
    private static final String CLAVE = "ClavePrueba123!";
    private static final String TEMPORAL = "TemporalTK4!";
    private static final String COOKIE = "PRAXIS_SESSION";

    @Inject
    DataSource dataSource;

    @Inject
    Config config;

    @BeforeEach
    @AfterEach
    void limpiarSinteticos() throws SQLException {
        ejecutar("DELETE FROM docente WHERE usuario_id IN "
                + "(SELECT id FROM usuario WHERE correo LIKE 'tk4-%')");
        ejecutar("DELETE FROM estudiante WHERE usuario_id IN "
                + "(SELECT id FROM usuario WHERE correo LIKE 'tk4-%')");
        ejecutar("DELETE FROM usuario_carrera WHERE usuario_id IN "
                + "(SELECT id FROM usuario WHERE correo LIKE 'tk4-%')");
        ejecutar("DELETE FROM usuario_rol WHERE usuario_id IN "
                + "(SELECT id FROM usuario WHERE correo LIKE 'tk4-%')");
        ejecutar("DELETE FROM usuario WHERE correo LIKE 'tk4-%'");
        ejecutar("DELETE FROM carrera WHERE codigo LIKE 'TK4-%'");
        ejecutar("DELETE FROM facultad WHERE codigo LIKE 'TK4-F-%'");
    }

    @Test
    void configuracionEfectiva_apuntaExclusivamenteAlClusterAislado() throws SQLException {
        assertEquals("jdbc:postgresql://127.0.0.1:55432/praxis_test",
                config.getValue("quarkus.datasource.jdbc.url", String.class));
        assertEquals("praxis_test", config.getValue("quarkus.datasource.username", String.class));
        assertEquals("none", config.getValue("quarkus.hibernate-orm.schema-management.strategy", String.class));

        try (Connection conexion = dataSource.getConnection(); Statement s = conexion.createStatement();
                ResultSet rs = s.executeQuery("SELECT current_database(), host(inet_server_addr())::text, "
                        + "inet_server_port(), current_user")) {
            assertTrue(rs.next());
            assertEquals("praxis_test", rs.getString(1));
            assertEquals("127.0.0.1", rs.getString(2));
            assertEquals(55432, rs.getInt(3));
            assertEquals("praxis_test", rs.getString(4));
        }
    }

    @Test
    void api024_listarUsuarios_conFiltrosYSinDatosSensibles() {
        String admin = cookie(ADMIN, CLAVE);
        crearUsuario(admin, "TK4-LISTAR@Example.Test", "0999000010");

        Response todos = given().cookie(COOKIE, admin).when().get("/usuarios/listarTodos");
        assertEquals(200, todos.statusCode(), todos.asString());
        assertTrue(todos.jsonPath().getList("").size() >= 3);
        assertSinCredenciales(todos);

        Response filtrado = given().cookie(COOKIE, admin).queryParam("texto", "tk4-listar")
                .when().get("/usuarios/listarTodos");
        assertEquals(200, filtrado.statusCode());
        assertEquals(1, filtrado.jsonPath().getList("").size());
        assertEquals("tk4-listar@example.test", filtrado.jsonPath().getString("[0].correo"));
        assertEquals(Set.of("id", "correo", "nombres", "apellidos", "tipoDocumento", "numeroDocumento",
                "telefono", "activo", "alcanceGlobal", "roles", "carreraIds"),
                ((Map<?, ?>) filtrado.jsonPath().getList("").get(0)).keySet());
    }

    @Test
    void api025_buscar_devuelveRolesYCarreras() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long carrera = crearCarrera("TK4-C-DETALLE", true);
        long id = crearUsuario(admin, "tk4-detalle@example.test", "0999000011");
        asignarRoles(admin, id, List.of("ESTUDIANTE"));
        asignarCarreras(admin, id, List.of(carrera));

        Response response = given().cookie(COOKIE, admin).when().get("/usuarios/buscar/" + id);

        assertEquals(200, response.statusCode(), response.asString());
        assertEquals(List.of("ESTUDIANTE"), response.jsonPath().getList("roles"));
        assertEquals(List.of((int) carrera), response.jsonPath().getList("carreraIds"));
        assertSinCredenciales(response);
    }

    @Test
    void api026_crear_normalizaCorreoYGuardaContrasenaTemporal() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);

        Response response = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(solicitudUsuario("TK4-CREAR@Example.Test", "0999000012"))
                .when().post("/usuarios");

        assertEquals(201, response.statusCode(), response.asString());
        assertEquals("tk4-crear@example.test", response.jsonPath().getString("correo"));
        assertTrue(response.jsonPath().getBoolean("activo"));
        assertEquals(11, response.jsonPath().getMap("").size());
        assertSinCredenciales(response);
        assertEquals("true", consultar("SELECT debe_cambiar_password::text FROM usuario "
                + "WHERE correo = 'tk4-crear@example.test'"));
        assertFalse(TEMPORAL.equals(consultar("SELECT hash_password FROM usuario "
                + "WHERE correo = 'tk4-crear@example.test'")));
    }

    @Test
    void api026_correoODocumentoDuplicado_devuelve409() {
        String admin = cookie(ADMIN, CLAVE);
        crearUsuario(admin, "tk4-duplicado@example.test", "0999000013");

        Response correo = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(solicitudUsuario("TK4-DUPLICADO@example.test", "0999000014"))
                .when().post("/usuarios");
        Response documento = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(solicitudUsuario("tk4-otro@example.test", "0999000013"))
                .when().post("/usuarios");

        assertError(correo, 409, "DUPLICADO");
        assertError(documento, 409, "DUPLICADO");
    }

    @Test
    void api027ActualizaYApi028029CambianEstado() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long id = crearUsuario(admin, "tk4-actualizar@example.test", "0999000015");
        Map<String, Object> datos = new java.util.LinkedHashMap<>(
                solicitudUsuario("tk4-actualizado@example.test", "0999000016"));
        datos.put("id", id);
        datos.put("telefono", "0977123456");

        Response actualizacion = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(datos).when().put("/usuarios");
        assertEquals(200, actualizacion.statusCode(), actualizacion.asString());
        assertEquals("tk4-actualizado@example.test", actualizacion.jsonPath().getString("correo"));
        assertEquals("0977123456", actualizacion.jsonPath().getString("telefono"));

        Response desactivar = given().cookie(COOKIE, admin).when().post("/usuarios/" + id + "/desactivar");
        assertEquals(200, desactivar.statusCode());
        assertEquals("false", consultar("SELECT activo::text FROM usuario WHERE id = " + id));

        Response activar = given().cookie(COOKIE, admin).when().post("/usuarios/" + id + "/activar");
        assertEquals(200, activar.statusCode());
        assertEquals("true", consultar("SELECT activo::text FROM usuario WHERE id = " + id));
    }

    @Test
    void api030_restablecerPassword_devuelve204YPermiteIngresar() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long id = crearUsuario(admin, "tk4-reset@example.test", "0999000017");
        String hashAnterior = consultar("SELECT hash_password FROM usuario WHERE id = " + id);

        Response response = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(Map.of("passwordTemporal", "NuevaTemporalTK4!"))
                .when().post("/usuarios/" + id + "/restablecerPassword");

        assertEquals(204, response.statusCode());
        assertNotEquals(hashAnterior, consultar("SELECT hash_password FROM usuario WHERE id = " + id));
        assertEquals("true", consultar("SELECT debe_cambiar_password::text FROM usuario WHERE id = " + id));
        assertNotNull(cookie("tk4-reset@example.test", "NuevaTemporalTK4!"));
    }

    @Test
    void api031_asignarRoles_creaPerfilDocenteUnaSolaVez() {
        String admin = cookie(ADMIN, CLAVE);
        long id = crearUsuario(admin, "tk4-docente@example.test", "0999000018");

        Response primera = asignarRoles(admin, id, List.of("DOCENTE"));
        assertEquals(200, primera.statusCode(), primera.asString());
        List<?> docentes = given().cookie(COOKIE, admin).when().get("/docentes/listarTodos")
                .jsonPath().getList("");
        long cantidad = docentes.stream().filter(d -> ((Map<?, ?>) d).get("usuarioId").equals((int) id)).count();
        assertEquals(1, cantidad);

        Response segunda = asignarRoles(admin, id, List.of("DOCENTE"));
        assertEquals(200, segunda.statusCode());
        assertEquals(1, given().cookie(COOKIE, admin).when().get("/docentes/listarTodos")
                .jsonPath().getList("").stream()
                .filter(d -> ((Map<?, ?>) d).get("usuarioId").equals((int) id)).count());
    }

    @Test
    void api032_asignarCarreras_reemplazaConjuntoYValidaExistencia() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long una = crearCarrera("TK4-C-UNO", true);
        long dos = crearCarrera("TK4-C-DOS", true);
        long id = crearUsuario(admin, "tk4-carreras@example.test", "0999000019");

        assertEquals(200, asignarCarreras(admin, id, List.of(una)).statusCode());
        assertEquals(List.of((int) una), detalle(admin, id).jsonPath().getList("carreraIds"));

        assertEquals(200, asignarCarreras(admin, id, List.of(dos)).statusCode());
        assertEquals(List.of((int) dos), detalle(admin, id).jsonPath().getList("carreraIds"));

        assertError(asignarCarreras(admin, id, List.of(999999L)), 409, "REGLA_NEGOCIO");
    }

    @Test
    void api033034_listarYBuscarEstudianteConCamposOpcionales() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long carrera = crearCarrera("TK4-C-EST", true);
        long usuario = crearUsuario(admin, "tk4-estudiante@example.test", "0999000020");

        Response creacion = crearEstudiante(admin, usuario, carrera, "TK4-EST-1", "2001-03-15");
        assertEquals(201, creacion.statusCode(), creacion.asString());
        assertTrue(given().cookie(COOKIE, admin).when().get("/estudiantes/listarTodos")
                .jsonPath().getList("codigoEstudiante").contains("TK4-EST-1"));

        Response busqueda = given().cookie(COOKIE, admin).when().get("/estudiantes/buscar/"
                + idEstudiante(usuario));
        assertEquals(200, busqueda.statusCode(), busqueda.asString());
        assertEquals((int) usuario, busqueda.jsonPath().getInt("usuarioId"));
        assertEquals((int) carrera, busqueda.jsonPath().getInt("carreraId"));
        assertEquals("TK4-EST-1", busqueda.jsonPath().getString("codigoEstudiante"));
        assertEquals("2001-03-15", busqueda.jsonPath().getString("fechaNacimiento"));
        assertNotNull(busqueda.jsonPath().getString("createdAt"));
        assertNotNull(busqueda.jsonPath().getString("updatedAt"));
    }

    @Test
    void api035_carreraInactivaPerfilDuplicadoYCodigoDuplicado_devuelve409() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long activa = crearCarrera("TK4-C-ACTIVA", true);
        long inactiva = crearCarrera("TK4-C-INACTIVA", false);
        long u1 = crearUsuario(admin, "tk4-est-uno@example.test", "0999000021");
        long u2 = crearUsuario(admin, "tk4-est-dos@example.test", "0999000022");

        assertError(crearEstudiante(admin, u1, inactiva, "TK4-E-INACT", null), 409, "REGLA_NEGOCIO");
        assertEquals(201, crearEstudiante(admin, u1, activa, "TK4-E-COD", null).statusCode());
        assertError(crearEstudiante(admin, u1, activa, "TK4-E-OTRO", null), 409, "DUPLICADO");
        assertError(crearEstudiante(admin, u2, activa, "TK4-E-COD", null), 409, "DUPLICADO");
    }

    @Test
    void api036_actualizarEstudiante_validaUsuarioYCodigo() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long una = crearCarrera("TK4-C-UPD1", true);
        long dos = crearCarrera("TK4-C-UPD2", true);
        long u1 = crearUsuario(admin, "tk4-upd-uno@example.test", "0999000023");
        long u2 = crearUsuario(admin, "tk4-upd-dos@example.test", "0999000024");
        assertEquals(201, crearEstudiante(admin, u1, una, "TK4-E-UPD1", null).statusCode());
        assertEquals(201, crearEstudiante(admin, u2, una, "TK4-E-UPD2", null).statusCode());
        long id = idEstudiante(u1);

        Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("id", id);
        datos.put("usuarioId", u1);
        datos.put("carreraId", dos);
        datos.put("codigoEstudiante", "TK4-E-NUEVO");
        datos.put("fechaNacimiento", "1999-12-31");

        Response actualizacion = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(datos).when().put("/estudiantes");
        assertEquals(200, actualizacion.statusCode(), actualizacion.asString());
        assertEquals((int) dos, given().cookie(COOKIE, admin).when().get("/estudiantes/buscar/" + id)
                .jsonPath().getInt("carreraId"));

        datos.put("usuarioId", u2);
        assertError(given().cookie(COOKIE, admin).contentType(ContentType.JSON).body(datos)
                .when().put("/estudiantes"), 400, "DATOS_INVALIDOS");

        datos.put("usuarioId", u2);
        datos.put("id", idEstudiante(u2));
        datos.put("codigoEstudiante", "TK4-E-NUEVO");
        assertError(given().cookie(COOKIE, admin).contentType(ContentType.JSON).body(datos)
                .when().put("/estudiantes"), 409, "DUPLICADO");
    }

    @Test
    void coordinador_soloOperaDentroDeSuAmbito() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long propia = crearCarrera("TK4-C-COO1", true);
        long ajena = crearCarrera("TK4-C-COO2", true);
        long idCoo = crearUsuario(admin, "tk4-coo@example.test", "0999000025");
        long idDentro = crearUsuario(admin, "tk4-dentro@example.test", "0999000026");
        long idFuera = crearUsuario(admin, "tk4-fuera@example.test", "0999000027");
        asignarRoles(admin, idCoo, List.of("COORDINADOR"));
        asignarCarreras(admin, idCoo, List.of(propia));
        crearEstudiante(admin, idDentro, propia, "TK4-E-DENTRO", null);
        crearEstudiante(admin, idFuera, ajena, "TK4-E-FUERA", null);
        String coo = cookie("tk4-coo@example.test", TEMPORAL);

        Response listado = given().cookie(COOKIE, coo).when().get("/estudiantes/listarTodos");
        assertEquals(200, listado.statusCode(), listado.asString());
        assertEquals(1, listado.jsonPath().getList("").size());
        assertEquals("TK4-E-DENTRO", listado.jsonPath().getString("[0].codigoEstudiante"));

        assertError(given().cookie(COOKIE, coo).when().get("/estudiantes/buscar/" + idEstudiante(idFuera)),
                404, "NO_ENCONTRADO");
        assertError(crearEstudiante(coo, idFuera, ajena, "TK4-E-BLOQ", null), 404, "NO_ENCONTRADO");

        Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("id", idEstudiante(idFuera));
        datos.put("usuarioId", idFuera);
        datos.put("carreraId", ajena);
        assertError(given().cookie(COOKIE, coo).contentType(ContentType.JSON).body(datos)
                .when().put("/estudiantes"), 404, "NO_ENCONTRADO");
    }

    @Test
    void api037038_listarYActualizarDocente() throws SQLException {
        String admin = cookie(ADMIN, CLAVE);
        long carrera = crearCarrera("TK4-C-DOC", true);
        long id = crearUsuario(admin, "tk4-doc-edit@example.test", "0999000028");
        asignarRoles(admin, id, List.of("DOCENTE"));
        asignarCarreras(admin, id, List.of(carrera));
        long idDocente = idDocente(id);

        Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("id", idDocente);
        datos.put("codigoDocente", "TK4-DOC-1");
        datos.put("dedicacion", "MEDIA");
        Response actualizacion = given().cookie(COOKIE, admin).contentType(ContentType.JSON)
                .body(datos).when().put("/docentes");
        assertEquals(200, actualizacion.statusCode(), actualizacion.asString());

        Response listado = given().cookie(COOKIE, admin).when().get("/docentes/listarTodos");
        assertEquals(200, listado.statusCode(), listado.asString());
        Map<?, ?> docente = (Map<?, ?>) listado.jsonPath().getList("").stream()
                .filter(d -> ((Map<?, ?>) d).get("usuarioId").equals((int) id))
                .findFirst().orElseThrow();
        assertEquals("TK4-DOC-1", docente.get("codigoDocente"));
        assertEquals("MEDIA", docente.get("dedicacion"));
        assertEquals(6, docente.size());
        assertNotNull(docente.get("createdAt"));
        assertNotNull(docente.get("updatedAt"));
    }

    @Test
    void seguridad_401SinSesionY403SinRol_conFormatoAprobado() {
        Response sinSesion = given().when().get("/usuarios/listarTodos");
        Response sinPermiso = given().cookie(COOKIE, cookie("estudiante.prueba@example.test", CLAVE))
                .when().get("/usuarios/listarTodos");

        assertError(sinSesion, 401, "SIN_SESION");
        assertError(sinPermiso, 403, "SIN_PERMISO");
        for (Response response : List.of(sinSesion, sinPermiso)) {
            assertEquals(2, response.jsonPath().getMap("").size());
            String texto = response.getBody().asString().toLowerCase();
            assertFalse(texto.contains("exception"));
            assertFalse(texto.contains("stack"));
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Response detalle(String cookie, long id) {
        Response response = given().cookie(COOKIE, cookie).when().get("/usuarios/buscar/" + id);
        assertEquals(200, response.statusCode(), response.asString());
        return response;
    }

    private long crearUsuario(String cookie, String correo, String documento) {
        Response response = given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(solicitudUsuario(correo, documento)).when().post("/usuarios");
        assertEquals(201, response.statusCode(), response.asString());
        return response.jsonPath().getLong("id");
    }

    private static Map<String, Object> solicitudUsuario(String correo, String documento) {
        return Map.of("correo", correo, "nombres", "TK4", "apellidos", "Prueba",
                "tipoDocumento", "CEDULA", "numeroDocumento", documento,
                "telefono", "0987654321", "alcanceGlobal", false, "passwordTemporal", TEMPORAL);
    }

    private Response asignarRoles(String cookie, long id, List<String> roles) {
        return given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("roles", roles)).when().put("/usuarios/" + id + "/roles");
    }

    private Response asignarCarreras(String cookie, long id, List<Long> carreras) {
        return given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(Map.of("carreraIds", carreras)).when().put("/usuarios/" + id + "/carreras");
    }

    private Response crearEstudiante(String cookie, long usuarioId, long carreraId, String codigo, String fecha) {
        Map<String, Object> datos = new java.util.LinkedHashMap<>();
        datos.put("usuarioId", usuarioId);
        datos.put("carreraId", carreraId);
        datos.put("codigoEstudiante", codigo);
        if (fecha != null) {
            datos.put("fechaNacimiento", fecha);
        }
        return given().cookie(COOKIE, cookie).contentType(ContentType.JSON)
                .body(datos).when().post("/estudiantes");
    }

    private String cookie(String correo, String password) {
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("correo", correo, "password", password)).when().post("/auth/login");
        assertEquals(200, response.statusCode(), response.asString());
        String cookie = response.getCookie(COOKIE);
        assertNotNull(cookie);
        return cookie;
    }

    private long crearCarrera(String codigo, boolean activa) throws SQLException {
        ejecutar("INSERT INTO facultad (codigo, nombre, activa) VALUES ('TK4-F-" + codigo + "', 'Facultad "
                + codigo + "', true)");
        ejecutar("INSERT INTO carrera (facultad_id, codigo, nombre, activa) VALUES "
                + "((SELECT id FROM facultad WHERE codigo = 'TK4-F-" + codigo + "'), '" + codigo
                + "', 'Carrera " + codigo + "', " + activa + ")");
        return Long.parseLong(consultar("SELECT id::text FROM carrera WHERE codigo = '" + codigo + "'"));
    }

    private long idEstudiante(long usuarioId) throws SQLException {
        return Long.parseLong(consultar("SELECT id::text FROM estudiante WHERE usuario_id = " + usuarioId));
    }

    private long idDocente(long usuarioId) throws SQLException {
        return Long.parseLong(consultar("SELECT id::text FROM docente WHERE usuario_id = " + usuarioId));
    }

    private static void assertError(Response response, int status, String codigo) {
        assertEquals(status, response.statusCode(), "Cuerpo: " + response.getBody().asString());
        assertEquals(codigo, response.jsonPath().getString("codigo"),
                "Cuerpo: " + response.getBody().asString());
        assertNotNull(response.jsonPath().getString("mensaje"));
    }

    private static void assertSinCredenciales(Response response) {
        String cuerpo = response.getBody().asString().toLowerCase();
        assertFalse(cuerpo.contains("hashpassword"));
        assertFalse(cuerpo.contains("hash_password"));
        assertFalse(cuerpo.contains("intentosfallidos"));
        assertFalse(cuerpo.contains("intentos_fallidos"));
        assertFalse(cuerpo.contains("bloqueadohasta"));
        assertFalse(cuerpo.contains("bloqueado_hasta"));
    }

    private static void assertNotEquals(Object esperado, Object real) {
        org.junit.jupiter.api.Assertions.assertNotEquals(esperado, real);
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
