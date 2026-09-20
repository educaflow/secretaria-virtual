package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;

import java.util.LinkedHashSet;
import java.util.Set;

public class AceProfileGlobalRepository extends AbstractAceProfileGlobalRepository {

    /**
     * Los perfiles que el usuario tiene sobre todos los trámites, por su tipo de usuario o su cargo en el centro.
     */
    public Set<Profile> findPerfiles(CentroUsuario centroUsuario) {
        String jpql = """
                SELECT DISTINCT a.perfil
                FROM com.educaflow.subsystem.security.db.AceProfileGlobal a
                WHERE
                (
                    a.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = :centroUsuario)
                    OR a.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = :centroUsuario)
                )
                """;

        return new LinkedHashSet<>(JPA.em().createQuery(jpql, Profile.class)
                .setParameter("centroUsuario", centroUsuario)
                .getResultList());
    }

}
