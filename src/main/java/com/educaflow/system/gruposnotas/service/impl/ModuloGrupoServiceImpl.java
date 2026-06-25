package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.service.ModuloGrupoInsertDTO;
import com.educaflow.system.gruposnotas.service.ModuloGrupoService;

import java.util.Optional;

public class ModuloGrupoServiceImpl extends DefaultModelService<ModuloGrupo> implements ModuloGrupoService {

    public ModuloGrupoServiceImpl(Class<ModuloGrupo> model, Repository<ModuloGrupo> repository) {
        super(model, repository);
    }

    @Override
    public ModuloGrupo insert(ModuloGrupoInsertDTO dto) {
        validateInsert(dto).ifPresent(BusinessMessages::throwIfInvalid);

        ModuloGrupo moduloGrupo = new ModuloGrupo();
        moduloGrupo.setGrupo(dto.grupo());
        moduloGrupo.setModulo(dto.modulo());
        return repository.save(moduloGrupo);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto) {
        if (dto.grupo() == null) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El grupo del módulo del grupo es obligatorio.")));
        }
        if (dto.modulo() == null) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El módulo del módulo del grupo es obligatorio.")));
        }
        return Optional.empty();
    }
}
