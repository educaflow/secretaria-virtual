package com.educaflow.subsystem.smoketest.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.smoketest.db.SmokeTest;
import com.educaflow.subsystem.smoketest.service.SmokeTestService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class SmokeTestServiceImpl extends DefaultModelService<SmokeTest> implements SmokeTestService {

    public SmokeTestServiceImpl(Class<SmokeTest> model, Repository<SmokeTest> repository) {
        super(model, repository);
    }

    @Override
    public SmokeTest insert(SmokeTest entity) {
        validateInsert(entity).ifPresent(BusinessMessages::throwIfInvalid);
        fireActionRule_AsignarFechaCreacion(entity);
        fireActionRule_ActualizarFechaUltimaModificacion(entity);
        return repository.save(entity);
    }

    @Override
    public SmokeTest update(SmokeTest entity, SmokeTest original) {
        validateUpdate(entity, original).ifPresent(BusinessMessages::throwIfInvalid);
        entity.setFechaCreacion(original.getFechaCreacion());
        fireActionRule_ActualizarFechaUltimaModificacion(entity);
        return repository.save(entity);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(SmokeTest entity) {
        if (entity.getTexto() == null || entity.getTexto().isBlank()) {
            return Optional.of(BusinessMessages.single(I18n.get("El texto es obligatorio")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(SmokeTest entity, SmokeTest original) {
        if (entity.getTexto() == null || entity.getTexto().isBlank()) {
            return Optional.of(BusinessMessages.single(I18n.get("El texto es obligatorio")));
        }
        return Optional.empty();
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of("texto", Map.of()));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of("texto", Map.of()));
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    /**
     * R-SmokeTest-001: asigna la fecha de creación en el momento del insert.
     * Asignación INCONDICIONAL: siempre se sobreescribe independientemente del valor previo.
     */
    private void fireActionRule_AsignarFechaCreacion(SmokeTest entity) {
        entity.setFechaCreacion(LocalDateTime.now());
    }

    /**
     * R-SmokeTest-002: actualiza la fecha de última modificación en cada insert/update.
     * Asignación INCONDICIONAL: siempre se sobreescribe independientemente del valor previo.
     */
    private void fireActionRule_ActualizarFechaUltimaModificacion(SmokeTest entity) {
        entity.setFechaUltimaModificacion(LocalDateTime.now());
    }
}
