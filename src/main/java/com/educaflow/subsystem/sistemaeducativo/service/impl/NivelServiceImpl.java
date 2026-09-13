package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.Nivel;
import com.educaflow.subsystem.sistemaeducativo.service.NivelService;

import java.util.Map;
import java.util.Optional;

public class NivelServiceImpl extends DefaultModelService<Nivel> implements NivelService {

    public NivelServiceImpl(Class<Nivel> model, Repository<Nivel> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Nivel nivel) {
        return validateGradoIndicado(nivel);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Nivel nivel, Nivel original) {
        return validateGradoIndicado(nivel);
    }

    private Optional<BusinessMessages> validateGradoIndicado(Nivel nivel) {
        BusinessMessages messages = new BusinessMessages();

        if (nivel.getGrado() == null) {
            messages.add(new BusinessMessage("grado", I18n.get("El grado es obligatorio"),
                    I18n.get(Mapper.of(Nivel.class).getProperty("grado").getTitle())));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return allowPropertiesEditables();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return allowPropertiesEditables();
    }

    private AllowProperties allowPropertiesEditables() {
        return AllowProperties.createAllowProperties(Map.of(
                "code", Map.of(),
                "name", Map.of(),
                "grado", Map.of()
        ));
    }

}
