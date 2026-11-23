package ec.edu.praxis.application.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ec.edu.praxis.domain.model.Usuario;
import ec.edu.praxis.infrastructure.repository.UsuarioRepository;
import ec.edu.praxis.infrastructure.repository.UsuarioRolRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Flujo de autenticación con credenciales, bloqueo temporal y sesión en memoria. */
@ApplicationScoped
public class AutenticacionService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Guayaquil");

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    UsuarioRolRepository usuarioRolRepository;

    @Inject
    SesionStore sesionStore;

    @ConfigProperty(name = "praxis.seguridad.intentos.maximo")
    Optional<Integer> maximoIntentos;

    @ConfigProperty(name = "praxis.seguridad.bloqueo.duracion")
    Optional<Duration> duracionBloqueo;

    /**
     * Verifica credenciales. Los fallos se registran y confirman antes de
     * devolver el resultado: así el incremento de intentos no se pierde por un
     * rollback. El Resource decide el 401 genérico.
     */
    @Transactional
    public ResultadoAutenticacion login(String correo, String password) {
        if (correo == null || correo.isBlank() || password == null || password.isEmpty()) {
            throw ApiException.datosInvalidos("Correo y contraseña son obligatorios.");
        }

        Usuario usuario = usuarioRepository.buscarPorCorreo(normalizar(correo));
        if (usuario == null || !usuario.isActivo() || estaBloqueado(usuario)) {
            return ResultadoAutenticacion.fallo();
        }
        if (!BcryptUtil.matches(password, usuario.getHashPassword())) {
            registrarFallo(usuario);
            return ResultadoAutenticacion.fallo();
        }

        OffsetDateTime ahora = OffsetDateTime.now(ZONA_NEGOCIO);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoLogin(ahora);
        usuarioRepository.flush();

        // La generación se lee ANTES de los roles: si una invalidación se confirma
        // mientras se consultan, la sesión resultante se rechaza en crear(...).
        long generacionEsperada = sesionStore.generacionActual(usuario.getId());
        List<String> roles = usuarioRolRepository.listarCodigosPorUsuario(usuario.getId());
        String identificador = sesionStore.crear(usuario.getId(), usuario.getCorreo(), roles,
                generacionEsperada);
        if (identificador == null) {
            return ResultadoAutenticacion.fallo();
        }
        return ResultadoAutenticacion.exito(identificador, usuario, roles);
    }

    public void logout(String identificador) {
        sesionStore.invalidar(identificador);
    }

    /** Usuario asociado a la sesión de la petición, para GET /auth/yo. */
    public Long usuarioDeSesion(String identificador) {
        SesionStore.Sesion sesion = sesionStore.buscar(identificador);
        if (sesion == null) {
            throw ApiException.sinSesion("La sesión no es válida.");
        }
        return sesion.getUsuarioId();
    }

    /** Datos del usuario actual para GET /auth/yo. */
    public ResultadoAutenticacion obtenerDatos(Long usuarioId) {
        if (usuarioId == null) {
            throw ApiException.sinSesion("La sesión no es válida.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId);
        if (usuario == null || !usuario.isActivo()) {
            throw ApiException.sinSesion("La sesión no es válida.");
        }
        List<String> roles = usuarioRolRepository.listarCodigosPorUsuario(usuarioId);
        return ResultadoAutenticacion.exito(null, usuario, roles);
    }

    public static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private boolean estaBloqueado(Usuario usuario) {
        OffsetDateTime hasta = usuario.getBloqueadoHasta();
        return hasta != null && hasta.isAfter(OffsetDateTime.now(ZONA_NEGOCIO));
    }

    private void registrarFallo(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= maximoIntentosRequerido()) {
            usuario.setBloqueadoHasta(OffsetDateTime.now(ZONA_NEGOCIO).plus(duracionBloqueoRequerida()));
        }
        usuarioRepository.flush();
    }

    private int maximoIntentosRequerido() {
        return maximoIntentos.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.intentos.maximo"));
    }

    private Duration duracionBloqueoRequerida() {
        return duracionBloqueo.orElseThrow(() -> new IllegalStateException(
                "Falta la configuración obligatoria: praxis.seguridad.bloqueo.duracion"));
    }

    /** Estructura interna para pasar el resultado de la capa de aplicación al Resource. */
    public static final class ResultadoAutenticacion {

        private final boolean autenticado;
        private final String identificadorSesion;
        private final Usuario usuario;
        private final List<String> roles;

        private ResultadoAutenticacion(boolean autenticado, String identificadorSesion,
                Usuario usuario, List<String> roles) {
            this.autenticado = autenticado;
            this.identificadorSesion = identificadorSesion;
            this.usuario = usuario;
            this.roles = roles;
        }

        static ResultadoAutenticacion fallo() {
            return new ResultadoAutenticacion(false, null, null, List.of());
        }

        static ResultadoAutenticacion exito(String identificadorSesion, Usuario usuario, List<String> roles) {
            return new ResultadoAutenticacion(true, identificadorSesion, usuario, roles);
        }

        public boolean isAutenticado() {
            return autenticado;
        }

        public String getIdentificadorSesion() {
            return identificadorSesion;
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
