package com.educaflow.base.infrastructure.numeradores.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.numeradores.db.Numerador;

import java.util.Optional;

public class NumeradorServiceImpl extends DefaultModelService<Numerador> {

    public NumeradorServiceImpl(Class<Numerador> model, Repository<Numerador> repository) {
        super(model, repository);
    }

    @Override
    public Numerador insert(Numerador numerador) {
        throw new UnsupportedOperationException(I18n.get("Los numeradores solo los gestiona el servidor."));
    }

    @Override
    public Numerador update(Numerador nuevo, Numerador original) {
        throw new UnsupportedOperationException(I18n.get("Los numeradores solo los gestiona el servidor."));
    }

    /**
     * Borrar un contador lo reiniciaría y el siguiente número se repetiría.
     */
    @Override
    public void remove(Numerador numerador) {
        throw new UnsupportedOperationException(I18n.get("Los numeradores no se pueden borrar."));
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Numerador numerador) {
        return Optional.of(BusinessMessages.single(I18n.get("Los numeradores solo los gestiona el servidor.")));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Numerador nuevo, Numerador original) {
        return Optional.of(BusinessMessages.single(I18n.get("Los numeradores solo los gestiona el servidor.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Numerador numerador) {
        return Optional.of(BusinessMessages.single(I18n.get("Los numeradores no se pueden borrar.")));
    }


    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    /**
     * Numerador solo lo gestiona el servidor con NumeradorRepository.
     */
    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    /**
     * Numerador solo lo gestiona el servidor con NumeradorRepository.
     */
    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

}
