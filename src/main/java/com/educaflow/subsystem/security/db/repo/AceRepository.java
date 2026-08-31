package com.educaflow.subsystem.security.db.repo;

import com.axelor.db.JPA;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Consultas sobre {@code Ace} que responden a "qué perfiles tiene este usuario sobre esto".
 *
 * <p>El filtro por usuario es el mismo que aplican las condiciones de los permisos declarados en
 * {@code subsystem/expedientes/data-init/input/auth-expedientes.xml}: el {@code Ace} vale si es del
 * centro activo (o no tiene centro) y si alcanza al usuario por alguna de las tres vías de
 * pertenencia — su tipo de usuario, él mismo o alguno de sus cargos.
 */
public class AceRepository extends AbstractAceRepository {

    /**
     * El trozo de JPQL que acota los {@code Ace} al usuario. Réplica literal de la condición de los
     * permisos {@code Expediente.*} de {@code auth-expedientes.xml}: si cambia allí, MUST cambiar aquí.
     */
    private static final String FILTRO_USUARIO = """
             AND (aa.centro IS NULL OR aa.centro = :centro)
             AND (
                 aa.tipoUsuario IN (SELECT cut.tipoUsuario FROM com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario cut WHERE cut.centroUsuario = :centroUsuario)
                 OR aa.centroUsuario = :centroUsuario
                 OR aa.cargo IN (SELECT cuc.cargo FROM com.educaflow.subsystem.common.db.CentroUsuarioCargo cuc WHERE cuc.centroUsuario = :centroUsuario)
             )
            """;

    /**
     * Los nombres de perfil que el usuario tiene sobre un expediente concreto, por cualquiera de las
     * tres vías de asignación: el propio expediente, su tipo de expediente o su trámite.
     *
     * <p>La vía del trámite excluye el perfil {@code CREADOR} igual que hace el permiso
     * {@code Expediente.porTramite}: ser creador de un trámite habilita a dar de alta expedientes,
     * no a tramitar los que ya existen.
     */
    public Set<String> findNombresPerfilesByExpediente(Expediente expediente, Centro centro, CentroUsuario centroUsuario) {
        if ((expediente == null) || (centroUsuario == null)) {
            return Set.of();
        }

        String jpql = """
                SELECT DISTINCT aa.perfil.name
                FROM com.educaflow.subsystem.security.db.Ace aa
                WHERE (
                    aa.expediente = :expediente
                    OR aa.tipoExpediente = :tipoExpediente
                    OR (aa.tramite = :tramite AND aa.perfil.name <> 'CREADOR')
                )
                """ + FILTRO_USUARIO;

        List<String> nombres = JPA.em().createQuery(jpql, String.class)
                .setParameter("expediente", expediente)
                .setParameter("tipoExpediente", expediente.getTipoExpediente())
                .setParameter("tramite", (expediente.getTipoExpediente() != null)
                        ? expediente.getTipoExpediente().getTramite() : null)
                .setParameter("centro", centro)
                .setParameter("centroUsuario", centroUsuario)
                .getResultList();

        return new LinkedHashSet<>(nombres);
    }

    /**
     * Los nombres de perfil que el usuario tiene sobre un trámite. Es lo que decide si puede crear
     * un expediente de ese trámite, cuando todavía no hay expediente contra el que preguntar.
     */
    public Set<String> findNombresPerfilesByTramite(Tramite tramite, Centro centro, CentroUsuario centroUsuario) {
        if ((tramite == null) || (centroUsuario == null)) {
            return Set.of();
        }

        String jpql = """
                SELECT DISTINCT aa.perfil.name
                FROM com.educaflow.subsystem.security.db.Ace aa
                WHERE aa.tramite = :tramite
                """ + FILTRO_USUARIO;

        List<String> nombres = JPA.em().createQuery(jpql, String.class)
                .setParameter("tramite", tramite)
                .setParameter("centro", centro)
                .setParameter("centroUsuario", centroUsuario)
                .getResultList();

        return new LinkedHashSet<>(nombres);
    }

}
