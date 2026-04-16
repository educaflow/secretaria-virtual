package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;
import com.educaflow.subsystem.sistemaeducativo.service.LeyEducativaService;

import java.util.Optional;

public class LeyEducativaServiceImpl extends DefaultModelService<LeyEducativa> implements LeyEducativaService {

    public LeyEducativaServiceImpl(Class<LeyEducativa> model, Repository repository) {
        super(LeyEducativa.class, repository);
    }


    @Override
    public LeyEducativa insert(LeyEducativa leyEducativa) {
        return super.insert(leyEducativa);
    }

    @Override
    public LeyEducativa update(LeyEducativa leyEducativa, LeyEducativa original) {
        return super.update(leyEducativa, original);
    }

    @Override
    public void remove(LeyEducativa leyEducativa) {
        super.remove(leyEducativa);
    }

    @Override
    public Optional<BusinessMessages> validateInsert(LeyEducativa leyEducativa) {
        BusinessMessages messages = new BusinessMessages();

        if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("aa")) {
            messages.add(new BusinessMessage("name", "No puede ser 'aa'"));
        }
        if (leyEducativa.getCode() != null && leyEducativa.getCode().trim().equalsIgnoreCase("aa")) {
            messages.add(new BusinessMessage("code", "No puede ser 'aa'"));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(LeyEducativa leyEducativa) {
        BusinessMessages messages = new BusinessMessages();

        if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("bb")) {
            messages.add(new BusinessMessage("name", "No puede ser 'bb'"));
        }
        if (leyEducativa.getCode() != null && leyEducativa.getCode().trim().equalsIgnoreCase("bb")) {
            messages.add(new BusinessMessage("code", "No puede ser 'bb'"));
        }


        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(LeyEducativa leyEducativa) {
        BusinessMessages messages = new BusinessMessages();

        if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("cc")) {
            messages.add(new BusinessMessage("name", "No puede ser 'cc'"));
        }
        if (leyEducativa.getCode() != null && leyEducativa.getCode().trim().equalsIgnoreCase("cc")) {
            messages.add(new BusinessMessage("code", "No puede ser 'cc'"));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }


}