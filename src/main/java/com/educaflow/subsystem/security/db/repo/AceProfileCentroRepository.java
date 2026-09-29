package com.educaflow.subsystem.security.db.repo;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.security.db.AceProfileCentro;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AceProfileCentroRepository extends AbstractAceProfileCentroRepository {

    /**
     * Los perfiles que el centro del usuario le da sobre un trámite: por su tipo de usuario, por su
     * cargo o a él mismo. El centro es el del {@code CentroUsuario}, así que un usuario solo recibe los
     * perfiles que da un centro al que pertenece.
     */
    public Set<Profile> findPerfiles(Tramite tramite, CentroUsuario centroUsuario) {
        String jpql = """
                SELECT DISTINCT a.perfil
                FROM com.educaflow.subsystem.security.db.AceProfileCentro a
                WHERE a.tramite = :tramite
                AND a.centro = :centro
                AND (
                    a.usuario = :usuario
                    OR
                    (
                        a.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = :centroUsuario)
                        OR a.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = :centroUsuario)
                    )
                )
                """;

        return new LinkedHashSet<>(JPA.em().createQuery(jpql, Profile.class)
                .setParameter("tramite", tramite)
                .setParameter("centro", centroUsuario.getCentro())
                .setParameter("usuario", centroUsuario.getUsuario())
                .setParameter("centroUsuario", centroUsuario)
                .getResultList());
    }

    public List<Centro> findCentrosSupervisados(User usuario) {
        String jpql = """
                SELECT DISTINCT cu.centro
                FROM com.educaflow.subsystem.common.db.CentroUsuario cu
                JOIN cu.centroUsuarioTipoUsuario cut
                JOIN cut.tipoUsuario tu
                WHERE cu.usuario = :usuario
                AND tu.codigo = :codigoSupervisor
                """;

        return JPA.em().createQuery(jpql, Centro.class)
                .setParameter("usuario", usuario)
                .setParameter("codigoSupervisor", TipoUsuario.SUPERVISOR)
                .getResultList();
    }

    public boolean existeOtraIgual(AceProfileCentro fila) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("centro", fila.getCentro());
        parametros.put("tramite", fila.getTramite());
        parametros.put("perfil", fila.getPerfil());

        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(a)
                FROM com.educaflow.subsystem.security.db.AceProfileCentro a
                WHERE a.centro = :centro
                AND a.tramite = :tramite
                AND a.perfil = :perfil
                """);
        jpql.append(condicionIgualONula("tipoUsuario", fila.getTipoUsuario(), parametros));
        jpql.append(condicionIgualONula("cargo", fila.getCargo(), parametros));
        jpql.append(condicionIgualONula("usuario", fila.getUsuario(), parametros));
        if (fila.getId() != null) {
            jpql.append(" AND a.id <> :id");
            parametros.put("id", fila.getId());
        }

        TypedQuery<Long> query = JPA.em().createQuery(jpql.toString(), Long.class);
        parametros.forEach(query::setParameter);
        return query.getSingleResult() > 0;
    }

    // Un parámetro nulo en «a.x = :x» nunca coincide en SQL, así que el destinatario vacío se compara con IS NULL.
    private String condicionIgualONula(String campo, Object valor, Map<String, Object> parametros) {
        if (valor == null) {
            return " AND a." + campo + " IS NULL";
        }
        parametros.put(campo, valor);
        return " AND a." + campo + " = :" + campo;
    }

}
