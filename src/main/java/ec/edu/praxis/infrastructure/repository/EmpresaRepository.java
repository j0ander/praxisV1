package ec.edu.praxis.infrastructure.repository;

import java.util.List;

import ec.edu.praxis.domain.model.Empresa;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;


@ApplicationScoped
public class EmpresaRepository implements PanacheRepositoryBase<Empresa, Long> {

    @Inject
    EntityManager em;

    public List<Empresa> listarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listAll();
        }
        String patron = "%" + texto.trim() + "%";
        return em.createQuery(
                "SELECT e FROM Empresa e WHERE e.ruc LIKE :patron OR LOWER(e.razonSocial) LIKE LOWER(:patron) "
        + "ORDER BY e.razonSocial, e.id", Empresa.class)
                .setParameter("patron", patron)
                .getResultList();
    }

    public Empresa buscarPorRuc(String ruc) {
        try {
            return em.createQuery("SELECT e FROM Empresa e WHERE e.ruc = :ruc", Empresa.class)
        .setParameter("ruc", ruc)
        .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Empresa buscarParaBloqueo(Long id) {
        return em.find(Empresa.class, id, LockModeType.PESSIMISTIC_WRITE);
    }
}
