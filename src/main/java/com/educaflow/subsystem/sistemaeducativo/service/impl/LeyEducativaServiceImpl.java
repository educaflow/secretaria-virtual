package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;
import com.educaflow.subsystem.sistemaeducativo.service.LeyEducativaService;

import java.util.Optional;

public class LeyEducativaServiceImpl extends DefaultModelService<LeyEducativa> implements LeyEducativaService {

    public LeyEducativaServiceImpl(Class<LeyEducativa> model, Repository<LeyEducativa> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(LeyEducativa leyEducativa) {
        BusinessMessages messages = new BusinessMessages();

        if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("aa")) {
            messages.add(new BusinessMessage("name", "No puede ser 'aa'", I18n.get(Mapper.of(LeyEducativa.class).getProperty("name").getTitle())));
        }
        if (leyEducativa.getCode() != null && leyEducativa.getCode().trim().equalsIgnoreCase("aa")) {
            messages.add(new BusinessMessage("code", "No puede ser 'aa'", I18n.get(Mapper.of(LeyEducativa.class).getProperty("code").getTitle())));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(LeyEducativa leyEducativa, LeyEducativa original) {
        BusinessMessages messages = new BusinessMessages();

        if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("bb")) {
            messages.add(new BusinessMessage("name", "No puede ser 'bb'", I18n.get(Mapper.of(LeyEducativa.class).getProperty("name").getTitle())));
        }
        if (leyEducativa.getCode() != null && leyEducativa.getCode().trim().equalsIgnoreCase("bb")) {
            messages.add(new BusinessMessage("code", "No puede ser 'bb'", I18n.get(Mapper.of(LeyEducativa.class).getProperty("code").getTitle())));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(LeyEducativa leyEducativa) {
        BusinessMessages messages = new BusinessMessages();

        if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("cc")) {
            messages.add(new BusinessMessage("name", "No puede ser 'cc'", I18n.get(Mapper.of(LeyEducativa.class).getProperty("name").getTitle())));
        }
        if (leyEducativa.getCode() != null && leyEducativa.getCode().trim().equalsIgnoreCase("cc")) {
            messages.add(new BusinessMessage("code", "No puede ser 'cc'", I18n.get(Mapper.of(LeyEducativa.class).getProperty("code").getTitle())));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

}
