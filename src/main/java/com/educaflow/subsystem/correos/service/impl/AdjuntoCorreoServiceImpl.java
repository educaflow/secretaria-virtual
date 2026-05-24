package com.educaflow.subsystem.correos.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.correos.db.AdjuntoCorreo;
import com.educaflow.subsystem.correos.service.AdjuntoCorreoService;

import java.util.Map;
import java.util.Optional;

public class AdjuntoCorreoServiceImpl extends DefaultModelService<AdjuntoCorreo> implements AdjuntoCorreoService {

    public AdjuntoCorreoServiceImpl(Class<AdjuntoCorreo> model, Repository<AdjuntoCorreo> repository) {
        super(model, repository);
    }

    @Override
    public AdjuntoCorreo update(AdjuntoCorreo adjuntoCorreo, AdjuntoCorreo original) {
        // El adjunto es inmutable tras su creación: no hay flujo normal de update.
        // Cualquier llamada que llegue aquí se saltó validateUpdate (k-secure-coding §9.2).
        throw new UnsupportedOperationException(
                I18n.get("El adjunto del correo es inmutable tras su creación."));
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(AdjuntoCorreo adjuntoCorreo) {
        BusinessMessages messages = new BusinessMessages();

        // V-AdjuntoCorreo-001 — nombreFichero obligatorio
        if (adjuntoCorreo.getNombreFichero() == null || adjuntoCorreo.getNombreFichero().isBlank()) {
            messages.add(new BusinessMessage("nombreFichero", I18n.get("El nombre del fichero es obligatorio.")));
        }

        // V-AdjuntoCorreo-002 — contenido obligatorio
        if (adjuntoCorreo.getContenido() == null) {
            messages.add(new BusinessMessage("contenido", I18n.get("El contenido del adjunto es obligatorio.")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(AdjuntoCorreo adjuntoCorreo, AdjuntoCorreo original) {
        // V-AdjuntoCorreo-003 — el adjunto es inmutable: SIEMPRE se rechaza (k-secure-coding §9.2).
        return Optional.of(BusinessMessages.single(
                I18n.get("El adjunto del correo es inmutable tras su creación.")));
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        // Solo campos cliente. El campo servidor 'correo' lo fija el onNew __parent__
        // del modal hijo, queda FUERA de la whitelist (k-secure-coding §3.2).
        return AllowProperties.createAllowProperties(Map.of(
                "nombreFichero", Map.of(),
                "contenido", Map.of()
        ));
    }
}
