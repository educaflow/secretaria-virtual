package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.Alias;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.repo.DispositivoCriptograficoRepository;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;
import com.educaflow.subsystem.criptografia.util.DispositivoCriptograficoInfoBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class DispositivoCriptograficoServiceImpl extends DefaultModelService<DispositivoCriptografico> implements DispositivoCriptograficoService {

    public DispositivoCriptograficoServiceImpl(Class<DispositivoCriptografico> model, Repository<DispositivoCriptografico> repository) {
        super(model, repository);
    }

    @Override
    public DispositivoCriptografico insert(DispositivoCriptografico dispositivo) {
        validateInsert(dispositivo).ifPresent(BusinessMessages::throwIfInvalid);
        for (String nombreAlias : DispositivoCriptograficoInfoBuilder.listarAlias(dispositivo.getPkcs11LibraryPath(), dispositivo.getSlot(), dispositivo.getPin())) {
            Alias alias = new Alias();
            alias.setName(nombreAlias);
            dispositivo.addAlias(alias);
        }
        DispositivoCriptografico resultado = repository.save(dispositivo);
        fireActionRule_RecargarDispositivos();
        return resultado;
    }

    @Override
    public DispositivoCriptografico update(DispositivoCriptografico dispositivo, DispositivoCriptografico original) {
        validateUpdate(dispositivo, original).ifPresent(BusinessMessages::throwIfInvalid);
        DispositivoCriptografico resultado = repository.save(dispositivo);
        fireActionRule_RecargarDispositivos();
        return resultado;
    }

    @Override
    public void remove(DispositivoCriptografico dispositivo) {
        validateRemove(dispositivo).ifPresent(BusinessMessages::throwIfInvalid);
        repository.remove(dispositivo);
        fireActionRule_RecargarDispositivos();
    }

    @Override
    public void recargarDispositivosEnEntornoCriptografico() {
        validateRecargarDispositivosEnEntornoCriptografico().ifPresent(BusinessMessages::throwIfInvalid);
        fireActionRule_RecargarDispositivos();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(DispositivoCriptografico dispositivo) {
        return validateDispositivo(dispositivo);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(DispositivoCriptografico dispositivo, DispositivoCriptografico dispositivoOriginal) {
        return validateDispositivo(dispositivo);
    }

    @Override
    public Optional<BusinessMessages> validateRecargarDispositivosEnEntornoCriptografico() {
        return Optional.empty();
    }

    /****************************************************************************/
    /******************************* Action Rules *******************************/
    /****************************************************************************/

    private void fireActionRule_RecargarDispositivos() {
        List<DispositivoCriptografico> todos = ((DispositivoCriptograficoRepository) repository).all().fetch();
        List<DispositivoCriptograficoConfig> configs = todos.stream()
                .map(d -> new DispositivoCriptograficoConfig(
                        Path.of(d.getPkcs11LibraryPath()),
                        d.getSlot(),
                        d.getPin()
                ))
                .toList();
        EntornoCriptografico.configureDispositivosCriptograficos(configs);
    }

    /*****************************************************************************/
    /****************************** Otras funciones ******************************/
    /*****************************************************************************/

    private Optional<BusinessMessages> validateDispositivo(DispositivoCriptografico dispositivo) {
        BusinessMessages messages = new BusinessMessages();

        String pkcs11LibraryPath = dispositivo.getPkcs11LibraryPath();

        if (pkcs11LibraryPath == null || pkcs11LibraryPath.isBlank()) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "La ruta de la librería PKCS#11 no puede estar vacía"));
            return Optional.of(messages);
        }
        Path libraryPath = Path.of(pkcs11LibraryPath);
        if (!Files.exists(libraryPath)) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "La librería PKCS#11 no existe en la ruta indicada: " + pkcs11LibraryPath));
            return Optional.of(messages);
        }

        List<SlotInfo> slotsInfo;
        try {
            slotsInfo = SlotInfoFactory.getSlotsInfo(libraryPath);
        } catch (Exception e) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "No se puede acceder a la librería PKCS#11: " + e.getMessage()));
            return Optional.of(messages);
        }
        int slotSolicitado = dispositivo.getSlot() != null ? dispositivo.getSlot() : 0;
        boolean slotValido = slotsInfo.stream().anyMatch(s -> s.index == slotSolicitado);
        if (!slotValido) {
            messages.add(new BusinessMessage("slot", "El slot " + slotSolicitado + " no existe en la librería PKCS#11. Slots disponibles: " + slotsInfo.size()));
        }

        Long id = dispositivo.getId();
        boolean slotOcupado = ((DispositivoCriptograficoRepository) repository).findBySlot(slotSolicitado).stream().anyMatch(otro -> !otro.getId().equals(id));
        if (slotOcupado) {
            messages.add(new BusinessMessage("slot", "Ya existe un dispositivo criptográfico configurado en el slot " + slotSolicitado + ". Cada slot solo puede tener un dispositivo."));
        }

        if (messages.isValid()) {
            try {
                SlotInfoFactory.validatePin(libraryPath, slotSolicitado, dispositivo.getPin());
            } catch (Exception e) {
                messages.add(new BusinessMessage("pin", "El PIN no es correcto: " + e.getMessage()));
            }
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

}
