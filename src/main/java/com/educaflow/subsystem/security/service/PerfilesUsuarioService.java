package com.educaflow.subsystem.security.service;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.Set;

/**
 * Responde a "qué perfiles tiene este usuario sobre esto", que es la pregunta que hace falta para
 * autorizar una transición de la máquina de estados: cada estado declara el perfil del actor que lo
 * atiende, y solo puede disparar sus eventos quien tenga ese perfil.
 *
 * <p>Devuelve un <b>conjunto</b> a propósito: un usuario puede tener legítimamente varios perfiles a
 * la vez sobre el mismo expediente (varias filas de {@code AceProfile*}), así que la pregunta es de
 * pertenencia, no de derivación de "el" perfil del usuario. El {@code _profile} que envía el cliente
 * sigue siendo solo una pista para elegir qué vista se pinta, nunca la fuente de autorización.
 * El conjunto devuelto es siempre inmutable.
 *
 * <p>No es un {@code ModelService}: no gestiona el ciclo de vida de ninguna entidad, solo consulta.
 * Su binding está en {@code SecurityModule}.
 */
public interface PerfilesUsuarioService {

    /**
     * Los perfiles que el usuario tiene sobre el expediente, en el centro del expediente; ninguno si
     * no pertenece a ese centro. {@code CREADOR} solo lo tiene quien registró el expediente (o una
     * fila de {@code AceProfileExpediente}), igual que hace el permiso {@code Expediente.creador}: el
     * {@code CREADOR} del resto de tablas solo sirve para crear.
     */
    Set<Profile> getPerfilesSobreExpediente(Expediente expediente, User user);

    /**
     * Los perfiles que el usuario tiene sobre el trámite en un centro, antes de que exista el
     * expediente: el centro lo elige quien crea el expediente, así que no se puede deducir del usuario.
     */
    Set<Profile> getPerfilesSobreTramite(Tramite tramite, User user, Centro centro);

    /**
     * Los perfiles que el usuario tiene sobre el trámite en un centro, antes de que exista el
     * expediente, y que permiten iniciar un expediente: {@code CREADOR} y {@code INICIADOR}.
     */
    Set<Profile> getPerfilesDeInicioSobreTramite(Tramite tramite, User user, Centro centro);

    Profile getPerfil(Tramite tramite, User user, Centro centro, boolean presentadoEnPapel);

    /**
     * Si el usuario tramita algún trámite en alguno de sus centros: tiene algún perfil de tramitación
     * (todos salvo {@code CREADOR}) sobre algún trámite con tipo de expediente activo.
     */
    boolean isTramitador(User user);

    /** Como {@link #isTramitador(User)}, pero solo en los trámites de esa unidad tramitadora. */
    boolean isTramitador(User user, String codigoUnidadTramitadora);
}
