package com.educaflow.subsystem.criptografia.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.criptografia.EntornoCriptografico;
import com.educaflow.base.infrastructure.criptografia.config.DispositivoCriptograficoConfig;
import com.educaflow.base.infrastructure.criptografia.slot.SlotFlag;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfo;
import com.educaflow.base.infrastructure.criptografia.slot.SlotInfoFactory;
import com.educaflow.base.util.TextUtil;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.criptografia.db.Alias;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;
import com.educaflow.subsystem.criptografia.db.repo.CertificadoDigitalRepository;
import com.educaflow.subsystem.criptografia.db.repo.DispositivoCriptograficoRepository;
import com.educaflow.subsystem.criptografia.service.DispositivoCriptograficoService;
import com.educaflow.subsystem.criptografia.util.DispositivoCriptograficoInfoBuilder;

import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DispositivoCriptograficoServiceImpl extends DefaultModelService<DispositivoCriptografico> implements DispositivoCriptograficoService {

    @Inject
    CertificadoDigitalRepository certificadoDigitalRepository;

    public DispositivoCriptograficoServiceImpl(Class<DispositivoCriptografico> model, Repository<DispositivoCriptografico> repository) {
        super(model, repository);
    }

    @Override
    public DispositivoCriptografico insert(DispositivoCriptografico dispositivo) {
        validateInsert(dispositivo).ifPresent(BusinessMessages::throwIfInvalid);
        fireActionRule_AsignarNuevoPin(dispositivo);
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
        fireActionRule_AsignarNuevoPin(dispositivo);
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

    @Override
    public List<Integer> getSlotsDisponibles(String pkcs11LibraryPath) {
        validateGetSlotsDisponibles(pkcs11LibraryPath).ifPresent(BusinessMessages::throwIfInvalid);

        return getSlotsInfo(pkcs11LibraryPath).stream().map(slot -> slot.index).toList();
    }

    @Override
    public String getDescripcionSlotsDisponibles(String pkcs11LibraryPath) {
        validateGetDescripcionSlotsDisponibles(pkcs11LibraryPath).ifPresent(BusinessMessages::throwIfInvalid);

        return getSlotsInfo(pkcs11LibraryPath).stream().map(DispositivoCriptograficoServiceImpl::getSlotDescription).collect(Collectors.joining("\n"));
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    /**
     * Pasa el PIN tecleado ({@code nuevoPin}) al campo {@code pin} del JSON, una vez filtrado por
     * {@code allowProperties}, para que la entidad nazca ya con su PIN.
     *
     * <p>Es necesario porque {@code Resource.save} hace {@code JPA.manage(bean)} —un {@code persist} con
     * {@code flush}— <strong>antes</strong> de llamar a {@code insert}, y {@code pin} es {@code required}: sin esto
     * llegaría nulo y el alta fallaría con «pin - no debe ser nulo» antes de que {@code fireActionRule_AsignarNuevoPin}
     * pudiera copiarlo. No abre ninguna puerta al cliente: {@code pin} sigue fuera de {@code allowProperties} y lo
     * copia el servidor desde {@code nuevoPin}.
     */
    @Override
    public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
        Map<String, Object> validated = new HashMap<>(super.validate(json, context));

        if (validated.get("nuevoPin") instanceof String nuevoPin && TextUtil.isNullOrBlank(nuevoPin) == false) {
            validated.put("pin", nuevoPin);
        }

        return validated;
    }

    @Override
    public Optional<BusinessMessages> validateInsert(DispositivoCriptografico dispositivo) {
        // El pin es obligatorio y en el alta solo puede llegar por el transitorio nuevoPin.
        if (isNuevoPinIndicado(dispositivo) == false) {
            BusinessMessages messages = new BusinessMessages();
            messages.add(new BusinessMessage("nuevoPin", "El PIN es obligatorio"));
            return Optional.of(messages);
        }
        return validateDispositivo(dispositivo, true);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(DispositivoCriptografico dispositivo, DispositivoCriptografico dispositivoOriginal) {
        if (dispositivoOriginal != null) {
            BusinessMessages messages = new BusinessMessages();
            if (!Objects.equals(dispositivo.getPkcs11LibraryPath(), dispositivoOriginal.getPkcs11LibraryPath())) {
                messages.add(new BusinessMessage("pkcs11LibraryPath", "No se puede cambiar la librería PKCS#11 de un dispositivo criptográfico. Para cambiar de token hay que borrar el dispositivo y crear otro."));
            }
            if (!Objects.equals(dispositivo.getSlot(), dispositivoOriginal.getSlot())) {
                messages.add(new BusinessMessage("slot", "No se puede cambiar el slot de un dispositivo criptográfico. Para cambiar de token hay que borrar el dispositivo y crear otro."));
            }
            if (!messages.isValid()) {
                return Optional.of(messages);
            }
        }
        boolean comprobarPin = dispositivoOriginal == null || isNuevoPinIndicado(dispositivo);
        return validateDispositivo(dispositivo, comprobarPin);
    }

    /**
     * Sus alias caen en cascada con él, y un certificado digital que apunte al dispositivo o a uno de sus alias
     * haría fallar el borrado contra la clave ajena.
     */
    @Override
    public Optional<BusinessMessages> validateRemove(DispositivoCriptografico dispositivo) {
        boolean usadoPorCertificados = certificadoDigitalRepository.all()
                .filter("self.dispositivoCriptografico = :dispositivo OR self.alias.dispositivoCriptografico = :dispositivo")
                .bind("dispositivo", dispositivo)
                .count() > 0;
        if (usadoPorCertificados) {
            return Optional.of(BusinessMessages.single(I18n.get("No se puede borrar el dispositivo criptográfico porque lo usan certificados digitales. Bórralos o cámbialos de dispositivo antes.")));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRecargarDispositivosEnEntornoCriptografico() {
        return Optional.empty();
    }


    @Override
    public Optional<BusinessMessages> validateGetSlotsDisponibles(String pkcs11LibraryPath) {
        return validateLibreriaConSlots(pkcs11LibraryPath);
    }

    @Override
    public Optional<BusinessMessages> validateGetDescripcionSlotsDisponibles(String pkcs11LibraryPath) {
        return validateLibreriaConSlots(pkcs11LibraryPath);
    }

    private Optional<BusinessMessages> validateLibreriaConSlots(String pkcs11LibraryPath) {
        BusinessMessages messages = new BusinessMessages();

        if (TextUtil.isNullOrBlank(pkcs11LibraryPath)) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "Indica la ruta de la librería PKCS#11 para ver los slots disponibles"));
            return Optional.of(messages);
        }
        List<SlotInfo> slotsInfo;
        try {
            Path libraryPath = Path.of(pkcs11LibraryPath);
            if (!Files.isRegularFile(libraryPath)) {
                messages.add(new BusinessMessage("pkcs11LibraryPath", "La librería PKCS#11 no existe en la ruta indicada: " + pkcs11LibraryPath));
                return Optional.of(messages);
            }
            slotsInfo = SlotInfoFactory.getSlotsInfo(libraryPath);
        } catch (Exception e) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "No se puede acceder a la librería PKCS#11: " + e.getMessage()));
            return Optional.of(messages);
        }
        if (slotsInfo.isEmpty()) {
            messages.add(new BusinessMessage("pkcs11LibraryPath", "No hay ningún dispositivo conectado en la librería PKCS#11 indicada"));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private Optional<BusinessMessages> validateDispositivo(DispositivoCriptografico dispositivo, boolean comprobarPin) {
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
        Optional<SlotInfo> slotInfo = slotsInfo.stream().filter(s -> s.index == slotSolicitado).findFirst();
        if (slotInfo.isEmpty()) {
            messages.add(new BusinessMessage("slot", "El slot " + slotSolicitado + " no existe en la librería PKCS#11. Slots disponibles: " + slotsInfo.size()));
        }

        Long id = dispositivo.getId();
        boolean slotOcupado = ((DispositivoCriptograficoRepository) repository).findBySlot(slotSolicitado).stream().anyMatch(otro -> !otro.getId().equals(id));
        if (slotOcupado) {
            messages.add(new BusinessMessage("slot", "Ya existe un dispositivo criptográfico configurado en el slot " + slotSolicitado + ". Cada slot solo puede tener un dispositivo."));
        }

        // Solo se prueba el PIN si todo lo demás es válido: probarlo gasta un intento del token cuando es incorrecto.
        if (messages.isValid() && comprobarPin) {
            validatePinContraElToken(libraryPath, slotInfo.get(), getPinAGuardar(dispositivo), messages);
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /**
     * Un PIN incorrecto cuenta como fallo en el token y, al llegar a su límite (normalmente 3), lo bloquea. Por eso
     * no se prueba cuando al token solo le queda un intento: se avisa en su lugar, para que el último intento no lo
     * gaste el servidor. Un login correcto reinicia el contador, así que verificar el PIN por otra vía basta para
     * que el aviso desaparezca.
     */
    private void validatePinContraElToken(Path libraryPath, SlotInfo slotInfo, String pin, BusinessMessages messages) {
        if (slotInfo.tokenFlags.contains(SlotFlag.USER_PIN_LOCKED)) {
            messages.add(new BusinessMessage("nuevoPin", "El PIN del dispositivo está bloqueado. Hay que desbloquearlo con el PUK antes de poder usarlo"));
            return;
        }
        if (slotInfo.tokenFlags.contains(SlotFlag.USER_PIN_FINAL_TRY)) {
            messages.add(new BusinessMessage("nuevoPin", "Al dispositivo solo le queda un intento de PIN y otro fallo lo bloquearía, así que no se ha comprobado el PIN. Verifícalo con otra herramienta (p. ej. pkcs11-tool) y vuelve a guardar"));
            return;
        }

        try {
            SlotInfoFactory.validatePin(libraryPath, slotInfo.index, pin);
        } catch (Exception e) {
            messages.add(new BusinessMessage("nuevoPin", "El PIN no es correcto: " + e.getMessage()));
        }
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/


    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "name", Map.of(),
                "pkcs11LibraryPath", Map.of(),
                "slot", Map.of(),
                "nuevoPin", Map.of()
        ));
    }


    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of(
                "name", Map.of(),
                "pkcs11LibraryPath", Map.of(),
                "slot", Map.of(),
                "nuevoPin", Map.of()
        ));
    }

    /****************************************************************************/
    /******************************* Action Rules *******************************/
    /****************************************************************************/

    /**
     * Vacío, el PIN guardado se conserva. El transitorio se vacía después de copiarlo para que la respuesta del
     * guardado no devuelva el secreto al navegador.
     */
    private void fireActionRule_AsignarNuevoPin(DispositivoCriptografico dispositivo) {
        if (isNuevoPinIndicado(dispositivo)) {
            dispositivo.setPin(dispositivo.getNuevoPin());
        }

        dispositivo.setNuevoPin(null);
    }

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

    /**************************************************************************************/
    /********************************    Otras funciones    *******************************/
    /**************************************************************************************/

    /**
     * Los slots de la librería, que ya ha comprobado {@code validateLibreriaConSlots}: si aun así no se puede leer,
     * es un fallo de la aplicación y no del administrador.
     */
    private static List<SlotInfo> getSlotsInfo(String pkcs11LibraryPath) {
        try {
            return SlotInfoFactory.getSlotsInfo(Path.of(pkcs11LibraryPath));
        } catch (Exception e) {
            throw new RuntimeException("No se puede acceder a la librería PKCS#11 " + pkcs11LibraryPath, e);
        }
    }

    private static String getSlotDescription(SlotInfo slot) {
        return "Slot " + slot.index + ": " + slot.tokenLabel + " — " + slot.tokenManufacturer + " " + slot.tokenModel + " (nº serie " + slot.tokenSerial + ")";
    }

    private static boolean isNuevoPinIndicado(DispositivoCriptografico dispositivo) {
        return TextUtil.isNullOrBlank(dispositivo.getNuevoPin()) == false;
    }

    /**
     * El PIN con el que quedará el dispositivo si se guarda: el nuevo si se ha indicado y, si no, el guardado.
     */
    private static String getPinAGuardar(DispositivoCriptografico dispositivo) {
        return isNuevoPinIndicado(dispositivo) ? dispositivo.getNuevoPin() : dispositivo.getPin();
    }

}
