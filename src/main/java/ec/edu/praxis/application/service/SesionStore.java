package ec.edu.praxis.application.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Almacén de sesiones en memoria (SEGURIDAD.md §2.6). La cookie contiene solo el
 * identificador aleatorio; el servidor conserva el mínimo necesario para
 * reconocer la sesión y construir la identidad de la petición.
 *
 * <p>Cada usuario tiene una generación en memoria. Un login lee la generación
 * antes de consultar los roles y la asocia a la sesión que crea; una invalidación
 * incrementa la generación y elimina las sesiones del usuario. La comprobación y
 * la inserción se realizan bajo el mismo cerrojo que la invalidación, de modo que
 * ninguna sesión con una generación anterior puede quedar válida después de una
 * invalidación completada.</p>
 */
@ApplicationScoped
public class SesionStore {

    /** Generación usada por sesiones sin usuario; no participa en invalidaciones. */
    private static final long SIN_USUARIO = -1L;

    @ConfigProperty(name = "praxis.seguridad.sesion.duracion")
    Optional<Duration> duracion;

    private final ConcurrentHashMap<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> generaciones = new ConcurrentHashMap<>();
    private final Object cerrojo = new Object();
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Reloj del servidor. Se mantiene como campo de instancia para poder avanza
     * el tiempo en pruebas sin cambiar la duración configurada ni esperar 30 minutos.
     */
    private Clock reloj = Clock.systemUTC();

    /**
     * Devuelve la generación vigente del usuario, registrándola si es la primera
     * vez que se le consulta. Debe leerse antes de consultar los roles en el login.
     */
    public long generacionActual(Long usuarioId) {
        if (usuarioId == null) {
            return SIN_USUARIO;
        }
        synchronized (cerrojo) {
            return generaciones.computeIfAbsent(usuarioId, id -> 0L);
        }
    }

    /** Crea una sesión con la generación actual del usuario. */
    public String crear(Long usuarioId, String correo, List<String> roles) {
        return crear(usuarioId, correo, roles, generacionActual(usuarioId));
    }

    /**
     * Crea una sesión solo si la generación esperada sigue siendo la vigente.
     * Devuelve {@code null} cuando la generación quedó obsoleta. La comprobación
     * y la inserción son atómicas respecto de {@link #invalidarPorUsuario(Long)}.
     */
    public String crear(Long usuarioId, String correo, List<String> roles, long generacionEsperada) {
        if (usuarioId == null) {
            return generacionEsperada == SIN_USUARIO
                    ? crearSesion(usuarioId, correo, roles, SIN_USUARIO)
                    : null;
        }
        synchronized (cerrojo) {
            Long actual = generaciones.get(usuarioId);
            if (actual == null || actual.longValue() != generacionEsperada) {
                return null;
            }
            return crearSesion(usuarioId, correo, roles, generacionEsperada);
        }
    }

    private String crearSesion(Long usuarioId, String correo, List<String> roles, long generacion) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String identificador = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiraEn = Instant.now(reloj).plus(duracionRequerida());
        sesiones.put(identificador, new Sesion(usuarioId, correo, roles, expiraEn, generacion));
        limpiarVencidas();
        return identificador;
    }

    /**
     * Devuelve la sesión vigente o null. Las vencidas y las que conservan una
     * generación obsoleta se eliminan al consultar.
     */
    public Sesion buscar(String identificador) {
        limpiarVencidas();
        if (identificador == null) {
            return null;
        }
        Sesion sesion = sesiones.get(identificador);
        if (sesion == null) {
            return null;
        }
        Long usuarioId = sesion.getUsuarioId();
        if (usuarioId != null) {
            synchronized (cerrojo) {
                Long actual = generaciones.get(usuarioId);
                if (actual == null || actual.longValue() != sesion.getGeneracion()) {
                    sesiones.remove(identificador, sesion);
                    return null;
                }
            }
        }
        return sesion;
    }

    public void invalidar(String identificador) {
        if (identificador != null) {
            sesiones.remove(identificador);
        }
    }

    /**
     * Invalida todas las sesiones de un usuario, sin afectar a las de otros.
     * Los identificadores nulos o no positivos no producen ningún cambio.
     * El incremento de la generación y la eliminación son atómicos: cualquier
     * login que haya leído una generación anterior verá rechazada su sesión.
     */
    public void invalidarPorUsuario(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            return;
        }
        synchronized (cerrojo) {
            generaciones.merge(usuarioId, 1L, Long::sum);
            sesiones.entrySet().removeIf(entrada -> usuarioId.equals(entrada.getValue().getUsuarioId()));
        }
    }

    private void limpiarVencidas() {
        Instant ahora = Instant.now(reloj);
        sesiones.entrySet().removeIf(entrada -> !entrada.getValue().expiraEn.isAfter(ahora));
    }

    /** Permite avanzar el tiempo en pruebas sin modificar la configuración de producción. */
    void usarReloj(Clock relojParaPruebas) {
        this.reloj = relojParaPruebas;
    }

    /** Número de sesiones vigentes en memoria; apoya la verificación de pruebas. */
    int cantidadSesiones() {
        limpiarVencidas();
        return sesiones.size();
    }

    private Duration duracionRequerida() {
        return duracion.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.sesion.duracion"));
    }

    /** Datos mínimos de la sesión; nunca contiene contraseñas ni hash. */
    public static final class Sesion {

        private final Long usuarioId;
        private final String correo;
        private final List<String> roles;
        private final Instant expiraEn;
        private final long generacion;

        Sesion(Long usuarioId, String correo, List<String> roles, Instant expiraEn, long generacion) {
            this.usuarioId = usuarioId;
            this.correo = correo;
            this.roles = List.copyOf(roles);
            this.expiraEn = expiraEn;
            this.generacion = generacion;
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public String getCorreo() {
            return correo;
        }

        public List<String> getRoles() {
            return roles;
        }

        public long getGeneracion() {
            return generacion;
        }
    }
}
