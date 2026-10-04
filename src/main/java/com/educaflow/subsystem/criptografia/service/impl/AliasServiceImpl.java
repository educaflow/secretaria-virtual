package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.criptografia.db.Alias;

import java.util.Optional;

public class AliasServiceImpl extends DefaultModelService<Alias> {

    public AliasServiceImpl(Class<Alias> model, Repository<Alias> repository) {
        super(model, repository);
    }

    @Override
    public Alias insert(Alias alias) {
        throw new UnsupportedOperationException(I18n.get("Los alias solo los crea el servidor al dar de alta el dispositivo criptográfico."));
    }

    @Override
    public Alias update(Alias nuevo, Alias original) {
        throw new UnsupportedOperationException(I18n.get("Los alias no se pueden modificar."));
    }

    @Override
    public void remove(Alias alias) {
        // Se borran en cascada al borrar su DispositivoCriptografico, sin pasar por aquí.
        throw new UnsupportedOperationException(I18n.get("Los alias no se pueden borrar; se borran al borrar su dispositivo criptográfico."));
    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Alias alias) {
        return Optional.of(BusinessMessages.single(I18n.get("Los alias solo los crea el servidor al dar de alta el dispositivo criptográfico.")));
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Alias nuevo, Alias original) {
        return Optional.of(BusinessMessages.single(I18n.get("Los alias no se pueden modificar.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Alias alias) {
        return Optional.of(BusinessMessages.single(I18n.get("Los alias no se pueden borrar; se borran al borrar su dispositivo criptográfico.")));
    }


    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    /**
     * Alias solo lo crea el servidor desde PKCS#11 (DispositivoCriptograficoServiceImpl); el anidado en
     * DispositivoCriptografico se filtra con las AllowProperties del padre.
     */
    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    /**
     * Alias solo lo crea el servidor desde PKCS#11 (DispositivoCriptograficoServiceImpl); el anidado en
     * DispositivoCriptografico se filtra con las AllowProperties del padre.
     */
    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

}
