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

public class SmokeTestServiceImpl extends DefaultModelService<SmokeTest>
        implements SmokeTestService {

    public SmokeTestServiceImpl(Class<SmokeTest> model, Repository<SmokeTest> repository) {
        super(model, repository);
    }

    @Override
    public SmokeTest insert(SmokeTest smokeTest) {
        validateInsert(smokeTest).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarFechaCreacion(smokeTest);
        fireActionRule_AsignarFechaUltimaModificacion(smokeTest);

        return repository.save(smokeTest);
    }

    @Override
    public SmokeTest update(SmokeTest smokeTest, SmokeTest original) {
        validateUpdate(smokeTest, original).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_RefrescarFechaModificacion(smokeTest, original);

        return repository.save(smokeTest);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(SmokeTest smokeTest) {
        // V-SmokeTest-001 (RES-001): texto obligatorio.
        if (smokeTest.getTexto() == null || smokeTest.getTexto().isBlank()) {
            return Optional.of(BusinessMessages.single(I18n.get("El texto es obligatorio")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(SmokeTest smokeTest, SmokeTest original) {
        // V-SmokeTest-001 (RES-001): texto obligatorio.
        if (smokeTest.getTexto() == null || smokeTest.getTexto().isBlank()) {
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

    private void fireActionRule_AsignarFechaCreacion(SmokeTest smokeTest) {
        // CC-001 (momento escritura): la fecha de creación la dicta el servidor.
        smokeTest.setFechaCreacion(LocalDateTime.now());
    }

    private void fireActionRule_AsignarFechaUltimaModificacion(SmokeTest smokeTest) {
        // CC-002 (momento escritura): la fecha de última modificación la dicta el servidor.
        smokeTest.setFechaUltimaModificacion(LocalDateTime.now());
    }

    private void fireActionRule_RefrescarFechaModificacion(SmokeTest smokeTest, SmokeTest original) {
        // CC-002: refresca la fecha de modificación; fechaCreacion es inmutable (se restaura del original).
        smokeTest.setFechaUltimaModificacion(LocalDateTime.now());
        smokeTest.setFechaCreacion(original.getFechaCreacion());
    }
}
