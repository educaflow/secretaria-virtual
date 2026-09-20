package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import java.util.LinkedHashSet;
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

}
