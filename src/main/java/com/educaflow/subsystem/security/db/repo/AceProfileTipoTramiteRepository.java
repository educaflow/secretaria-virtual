package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoTramite;
import java.util.LinkedHashSet;
import java.util.Set;

public class AceProfileTipoTramiteRepository extends AbstractAceProfileTipoTramiteRepository {

    /**
     * Los perfiles que el usuario tiene sobre los trámites de un tipo de trámite, por su tipo de usuario o su cargo en el centro.
     */
    public Set<Profile> findPerfiles(TipoTramite tipoTramite, CentroUsuario centroUsuario) {
        String jpql = """
                SELECT DISTINCT a.perfil
                FROM com.educaflow.subsystem.security.db.AceProfileTipoTramite a
                WHERE a.tipoTramite = :tipoTramite AND
                (
                    a.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = :centroUsuario)
                    OR a.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = :centroUsuario)
                )
                """;

        return new LinkedHashSet<>(JPA.em().createQuery(jpql, Profile.class)
                .setParameter("tipoTramite", tipoTramite)
                .setParameter("centroUsuario", centroUsuario)
                .getResultList());
    }

}
