package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import java.util.LinkedHashSet;
import java.util.Set;

public class AceProfileTramiteRepository extends AbstractAceProfileTramiteRepository {

    /**
     * Los perfiles que el usuario tiene sobre un trámite, por su tipo de usuario o su cargo en el centro.
     */
    public Set<Profile> findPerfiles(Tramite tramite, CentroUsuario centroUsuario) {
        String jpql = """
                SELECT DISTINCT a.perfil
                FROM com.educaflow.subsystem.security.db.AceProfileTramite a
                WHERE a.tramite = :tramite AND
                (
                    a.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = :centroUsuario)
                    OR a.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = :centroUsuario)
                )
                """;

        return new LinkedHashSet<>(JPA.em().createQuery(jpql, Profile.class)
                .setParameter("tramite", tramite)
                .setParameter("centroUsuario", centroUsuario)
                .getResultList());
    }

}
