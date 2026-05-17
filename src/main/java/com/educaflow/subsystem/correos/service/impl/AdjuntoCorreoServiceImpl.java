package com.educaflow.subsystem.correos.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.AdjuntoCorreo;
import com.educaflow.subsystem.correos.service.AdjuntoCorreoService;

import java.util.Optional;

public class AdjuntoCorreoServiceImpl extends DefaultModelService<AdjuntoCorreo> implements AdjuntoCorreoService {

    public AdjuntoCorreoServiceImpl(Class<AdjuntoCorreo> model, Repository<AdjuntoCorreo> repository) {
        super(model, repository);
    }

    @Override
    public AdjuntoCorreo insert(AdjuntoCorreo entidad) {
        fireActionRule_clonarMetaFile(entidad);
        return super.insert(entidad);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(AdjuntoCorreo entidad, AdjuntoCorreo original) {
        BusinessMessages businessMessages = new BusinessMessages();
        businessMessages.add(new BusinessMessage("",
                I18n.get("Los adjuntos de correo son inmutables y no pueden modificarse.")));
        return Optional.of(businessMessages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(AdjuntoCorreo entidad) {
        BusinessMessages businessMessages = new BusinessMessages();
        businessMessages.add(new BusinessMessage("",
                I18n.get("Los adjuntos de correo son inmutables y no pueden borrarse.")));
        return Optional.of(businessMessages);
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_clonarMetaFile(AdjuntoCorreo entidad) {
        // R-AdjuntoCorreo-001
        if (entidad.getId() == null && entidad.getContenidoFichero() != null) {
            entidad.setContenidoFichero(MetaFileUtil.cloneMetaFile(entidad.getContenidoFichero()));
        }
    }
}
