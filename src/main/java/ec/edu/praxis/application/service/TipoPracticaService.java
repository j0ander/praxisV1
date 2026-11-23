package ec.edu.praxis.application.service;

import java.math.BigDecimal;
import java.util.List;

import ec.edu.praxis.domain.model.TipoPractica;
import ec.edu.praxis.infrastructure.repository.TipoPracticaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TipoPracticaService {

    private static final BigDecimal NOTA_MAXIMA = new BigDecimal("20");

    @Inject
    private TipoPracticaRepository repository;

    public List<TipoPractica> listar() {
        return repository.listAll();
    }

    public TipoPractica buscar(Long id) {
        TipoPractica tipo = repository.findById(id);
        if (tipo == null) {
            throw ApiException.noEncontrado("Tipo de práctica no encontrado.");
        }
        return tipo;
    }

    @Transactional
    public TipoPractica crear(TipoPractica datos) {
        validarDatos(datos);
        validarDuplicados(datos.getNombre(), null);
        repository.persistAndFlush(datos);
        return datos;
    }

    @Transactional
    public TipoPractica actualizar(TipoPractica datos) {
        if (datos == null || datos.getId() == null) {
            throw ApiException.datosInvalidos("El id del tipo de práctica es obligatorio.");
        }
        TipoPractica tipo = repository.findById(datos.getId());
        if (tipo == null) {
            throw ApiException.noEncontrado("Tipo de práctica no encontrado.");
        }
        validarDatos(datos);
        validarDuplicados(datos.getNombre(), tipo.getId());
        tipo.setNombre(datos.getNombre());
        tipo.setHorasRequeridas(datos.getHorasRequeridas());
        tipo.setNotaMinima(datos.getNotaMinima());
        tipo.setActivo(datos.isActivo());
        repository.flush();
        return tipo;
    }

    static void validarDatos(TipoPractica datos) {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank()
                || datos.getHorasRequeridas() == null || datos.getNotaMinima() == null) {
            throw ApiException.datosInvalidos("Nombre, horas requeridas y nota mínima son obligatorios.");
        }
        if (datos.getHorasRequeridas().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.reglaNegocio("Las horas requeridas deben ser mayores que cero.");
        }
        if (datos.getNotaMinima().compareTo(BigDecimal.ZERO) < 0
                || datos.getNotaMinima().compareTo(NOTA_MAXIMA) > 0) {
            throw ApiException.reglaNegocio("La nota mínima debe estar entre 0 y 20.");
        }
    }

    private void validarDuplicados(String nombre, Long idActual) {
        TipoPractica porNombre = repository.buscarPorNombre(nombre);
        if (porNombre != null && !porNombre.getId().equals(idActual)) {
            throw ApiException.duplicado("Ya existe un tipo de práctica con ese nombre.");
        }
    }
}
