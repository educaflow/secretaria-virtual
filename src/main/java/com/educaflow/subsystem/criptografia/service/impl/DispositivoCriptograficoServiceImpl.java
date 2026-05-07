package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.secretariavirtual.startup.AppEventObserver;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class DispositivoCriptograficoServiceImpl extends DefaultModelService<DispositivoCriptografico> implements DispositivoCriptograficoService {
    private final Logger logger = LoggerFactory.getLogger(DispositivoCriptograficoServiceImpl.class);


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
        List<DispositivoCriptografico> todos = repository.all().fetch();
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

        logger.info("A________________________");

        String pkcs11LibraryPath = dispositivo.getPkcs11LibraryPath();

        if (pkcs11LibraryPath == null || pkcs11LibraryPath.isBlank()) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "La ruta de la librería PKCS#11 no puede estar vacía"));
            return Optional.of(messages);
        }
        logger.info("B________________________");
        Path libraryPath = Path.of(pkcs11LibraryPath);
        logger.info("C________________________");
        if (!Files.exists(libraryPath)) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "La librería PKCS#11 no existe en la ruta indicada: " + pkcs11LibraryPath));
            return Optional.of(messages);
        }
        logger.info("D________________________");

        List<SlotInfo> slotsInfo;
        try {
            slotsInfo = SlotInfoFactory.getSlotsInfo(libraryPath);
        } catch (Exception e) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "No se puede acceder a la librería PKCS#11: " + e.getMessage()));
            return Optional.of(messages);
        }
        logger.info("E________________________");
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
