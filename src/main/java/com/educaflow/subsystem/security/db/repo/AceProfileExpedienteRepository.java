package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import java.util.LinkedHashSet;
import java.util.Set;

public class AceProfileExpedienteRepository extends AbstractAceProfileExpedienteRepository {

    /**
     * Los perfiles que el usuario tiene sobre un expediente concreto. Recibe el {@code CentroUsuario}
     * del usuario en el centro del expediente, y no solo el usuario, para que nadie tenga perfiles
     * sobre un expediente de un centro al que no pertenece.
     */
    public Set<Profile> findPerfiles(Expediente expediente, CentroUsuario centroUsuario) {
        String jpql = """
                SELECT DISTINCT a.perfil
                FROM com.educaflow.subsystem.security.db.AceProfileExpediente a
                WHERE a.expediente = :expediente
                AND a.usuario = :usuario
                """;

        return new LinkedHashSet<>(JPA.em().createQuery(jpql, Profile.class)
                .setParameter("expediente", expediente)
                .setParameter("usuario", centroUsuario.getUsuario())
                .getResultList());
    }

}
