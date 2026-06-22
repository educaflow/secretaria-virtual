package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.service.ModuloGrupoService;
import jakarta.inject.Inject;

import java.util.Optional;

public class ModuloGrupoServiceImpl extends DefaultModelService<ModuloGrupo> implements ModuloGrupoService {

    @Inject
    public ModuloGrupoServiceImpl(Class<ModuloGrupo> model, Repository<ModuloGrupo> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Operaciones genéricas ********************************/
    /****************************************************************************************/

    @Override
    public ModuloGrupo update(ModuloGrupo moduloGrupo, ModuloGrupo original) {
        throw new UnsupportedOperationException(I18n.get("El módulo del grupo no es editable."));
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(ModuloGrupo moduloGrupo) {
        if (existeModuloEnGrupo(moduloGrupo)) {
            return Optional.of(BusinessMessages.single(I18n.get("El módulo ya está en el grupo.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(ModuloGrupo moduloGrupo, ModuloGrupo original) {
        return Optional.of(BusinessMessages.single(I18n.get("El módulo del grupo no es editable.")));
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private boolean existeModuloEnGrupo(ModuloGrupo moduloGrupo) {
        return repository.all()
                .filter("self.grupo = :grupo AND self.modulo = :modulo")
                .bind("grupo", moduloGrupo.getGrupo())
                .bind("modulo", moduloGrupo.getModulo())
                .count() > 0;
    }

}
