package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.util.MensajeSmsUtil;
import com.educaflow.base.util.NumeroTelefono;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.service.SmsService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Map;
import java.util.Optional;

public class SmsServiceImpl extends NotificacionServiceImpl<Sms> implements SmsService {

    // Provider: unas credenciales ausentes deben dejar el SMS FALLIDO al enviar, no impedir crear el servicio.
    @Inject
    Provider<SmsSender> smsSenderProvider;

    public SmsServiceImpl(Class<Sms> model, Repository<Sms> repository) {
        super(model, repository);
    }

    @Override
    protected void antesDeGuardar(Sms sms) {
        fireActionRule_NormalizarTelefono(sms);
    }

    @Override
    protected void enviar(Sms sms) {
        smsSenderProvider.get().send(new com.educaflow.base.infrastructure.sms.Sms(sms.getTelefono(), sms.getMensaje()));
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Sms sms) {
        BusinessMessages messages = validateDatosComunes(sms);

        validarTelefono(sms, messages);
        validarMensaje(sms, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarTelefono(Sms sms, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(sms.getTelefono())) {
            messages.add(new BusinessMessage(I18n.get("El teléfono es obligatorio")));
        } else if (new NumeroTelefono(sms.getTelefono()).esMovilDeEspana() == false) {
            messages.add(new BusinessMessage(I18n.get(
                    "El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)")));
        }
    }

    private void validarMensaje(Sms sms, BusinessMessages messages) {
        // Blanco incluido: es la única guarda de cabeEnUnSms, que lanza ante un mensaje en blanco.
        if (TextUtil.isNullOrBlank(sms.getMensaje())) {
            messages.add(new BusinessMessage(I18n.get("El mensaje es obligatorio")));
        } else if (MensajeSmsUtil.cabeEnUnSms(sms.getMensaje()) == false) {
            messages.add(new BusinessMessage(I18n.get(
                    "El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales")));
        }
    }

    /**************************************************************************************/
    /********************************** AllowProperties ***********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        Map<String, Object> propiedades = allowPropertiesInsertComunes();
        propiedades.putAll(Map.of(
                "telefono", Map.of(),
                "mensaje", Map.of()
        ));
        return AllowProperties.createAllowProperties(propiedades);
    }

    /**************************************************************************************/
    /************************************ Action Rules ************************************/
    /**************************************************************************************/

    private void fireActionRule_NormalizarTelefono(Sms sms) {
        sms.setTelefono(new NumeroTelefono(sms.getTelefono()).enFormatoE164());
    }

    /**************************************************************************************/
    /********************************** Otras funciones ***********************************/
    /**************************************************************************************/
}
