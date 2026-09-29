package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import java.util.LinkedHashSet;
import java.util.Set;

public class AceProfileTipoUsuarioTramiteRepository extends AbstractAceProfileTipoUsuarioTramiteRepository {

    /**
     * Los perfiles que el usuario tiene sobre los trámites dirigidos a un tipo de usuario, por su tipo de usuario o su cargo en el centro.
     */
    public Set<Profile> findPerfiles(TipoUsuario tipoUsuarioTramite, CentroUsuario centroUsuario) {
        String jpql = """
                SELECT DISTINCT a.perfil
                FROM com.educaflow.subsystem.security.db.AceProfileTipoUsuarioTramite a
                WHERE a.tipoUsuarioTramite = :tipoUsuarioTramite AND
                (
                    a.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = :centroUsuario)
                    OR a.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = :centroUsuario)
                )
                """;

        return new LinkedHashSet<>(JPA.em().createQuery(jpql, Profile.class)
                .setParameter("tipoUsuarioTramite", tipoUsuarioTramite)
                .setParameter("centroUsuario", centroUsuario)
                .getResultList());
    }

}
