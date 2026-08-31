package com.educaflow.subsystem.security.service;

import com.axelor.auth.db.User;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.Tramite;

import java.util.Set;

/**
 * Responde a "qué perfiles tiene este usuario sobre esto", que es la pregunta que hace falta para
 * autorizar una transición de la máquina de estados: cada estado declara el perfil del actor que lo
 * atiende, y solo puede disparar sus eventos quien tenga ese perfil.
 *
 * <p>Devuelve un <b>conjunto</b> a propósito: un usuario puede tener legítimamente varios perfiles a
 * la vez sobre el mismo expediente (varias filas de {@code Ace}), así que la pregunta es de
 * pertenencia, no de derivación de "el" perfil del usuario. El {@code _profile} que envía el cliente
 * sigue siendo solo una pista para elegir qué vista se pinta, nunca la fuente de autorización.
 *
 * <p>No es un {@code ModelService}: no gestiona el ciclo de vida de ninguna entidad, solo consulta.
 * Su binding está en {@code SecurityModule}.
 */
public interface PerfilesUsuarioService {

    /**
     * Los nombres de perfil que el usuario tiene sobre el expediente. Incluye {@code CREADOR} cuando
     * el usuario es quien registró el expediente, igual que hace el permiso {@code Expediente.creador}.
     */
    Set<String> getPerfilesSobreExpediente(Expediente expediente, User user);

    /** Los nombres de perfil que el usuario tiene sobre el trámite, antes de que exista el expediente. */
    Set<String> getPerfilesSobreTramite(Tramite tramite, User user);

}
