package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.repo.DispositivoCriptograficoRepository;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;

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
        DispositivoCriptografico resultado = super.insert(dispositivo);
        recargarDispositivosEnEntornoCriptografico();
        return resultado;
    }

    @Override
    public DispositivoCriptografico update(DispositivoCriptografico dispositivo, DispositivoCriptografico original) {
        DispositivoCriptografico resultado = super.update(dispositivo, original);
        recargarDispositivosEnEntornoCriptografico();
        return resultado;
    }

    @Override
    public void remove(DispositivoCriptografico dispositivo) {
        super.remove(dispositivo);
        recargarDispositivosEnEntornoCriptografico();
    }

    @Override
    public void recargarDispositivosEnEntornoCriptografico() {
        List<DispositivoCriptografico> todos = ((DispositivoCriptograficoRepository) repository).findAll();
        List<DispositivoCriptograficoConfig> configs = todos.stream()
                .map(d -> new DispositivoCriptograficoConfig(
                        Path.of(d.getPkcs11LibraryPath()),
                        d.getSlot(),
                        d.getPin()
                ))
                .toList();
        EntornoCriptografico.configureDispositivosCriptograficos(configs);
    }

    @Override
    public Optional<BusinessMessages> validateInsert(DispositivoCriptografico dispositivo) {
        return validateDispositivo(dispositivo);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(DispositivoCriptografico dispositivo, DispositivoCriptografico dispositivoOriginal) {
        return validateDispositivo(dispositivo);
    }

    @Override
    public Optional<BusinessMessages> validateRemove(DispositivoCriptografico dispositivo) {
        return Optional.empty();
    }

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
