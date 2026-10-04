package com.educaflow.subsystem.sms.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.base.infrastructure.sms.SmsSender;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.ExceptionUtil;
import com.educaflow.base.util.MensajeSmsUtil;
import com.educaflow.base.util.NumeroTelefono;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.sms.db.EstadoSms;
import com.educaflow.subsystem.sms.db.Sms;
import com.educaflow.subsystem.sms.service.SmsService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SmsServiceImpl extends DefaultModelService<Sms> implements SmsService {

    @Inject
    Provider<SmsSender> smsSenderProvider;

    @Inject
    EjecutorAsincrono ejecutorAsincrono;

    public SmsServiceImpl(Class<Sms> model, Repository<Sms> repository) {
        super(model, repository);
    }

    @Override
    public Sms insert(Sms sms) {
        validateInsert(sms).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_NormalizarTelefono(sms);
        fireActionRule_AsignarValoresIniciales(sms);
        sms = repository.save(sms);
        fireActionRule_ProgramarEnvioAsincrono(sms);
        return sms;
    }

    @Override
    public Sms update(Sms nuevo, Sms original) {
        throw new UnsupportedOperationException(I18n.get("El SMS es inmutable tras su creación."));
    }

    @Override
    public void remove(Sms sms) {
        throw new UnsupportedOperationException(I18n.get("Los SMS no se pueden borrar."));
    }

    @Override
    public Sms reenviar(Sms entidad, Sms entidadOriginal) {
        validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        // Sin repository.save: el cambio de estado lo hace enviarSms en la transacción del hilo del pool.
        fireActionRule_ProgramarEnvioAsincrono(entidadOriginal);
        return entidadOriginal;
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Sms sms) {
        BusinessMessages messages = new BusinessMessages();

        validarDestinatario(sms, messages);
        validarTelefono(sms, messages);
        validarMensaje(sms, messages);
        validarCentro(sms, messages);
        if (sms.getCentro() != null && sms.getHistorialEstado() != null) {
            validarHistorialEstado(sms, messages);
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarDestinatario(Sms sms, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(sms.getDniDestinatario())) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario es obligatorio")));
        } else if (!DniUtil.isValid(sms.getDniDestinatario())) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario no es válido; compruebe la letra")));
        }

        if (TextUtil.isNullOrBlank(sms.getNombre())) {
            messages.add(new BusinessMessage(I18n.get("El nombre es obligatorio")));
        }

        if (TextUtil.isNullOrBlank(sms.getApellidos())) {
            messages.add(new BusinessMessage(I18n.get("Los apellidos son obligatorios")));
        }
    }

    private void validarTelefono(Sms sms, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(sms.getTelefono())) {
            messages.add(new BusinessMessage(I18n.get("El teléfono es obligatorio")));
        } else if (!new NumeroTelefono(sms.getTelefono()).esMovilDeEspana()) {
            messages.add(new BusinessMessage(I18n.get(
                    "El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)")));
        }
    }

    private void validarMensaje(Sms sms, BusinessMessages messages) {
        // Blanco incluido: es la única guarda de cabeEnUnSms, que lanza ante un mensaje en blanco.
        if (TextUtil.isNullOrBlank(sms.getMensaje())) {
            messages.add(new BusinessMessage(I18n.get("El mensaje es obligatorio")));
        } else if (!MensajeSmsUtil.cabeEnUnSms(sms.getMensaje())) {
            messages.add(new BusinessMessage(I18n.get(
                    "El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales")));
        }
    }

    private void validarCentro(Sms sms, BusinessMessages messages) {
        if (sms.getCentro() == null) {
            messages.add(new BusinessMessage(I18n.get("El centro es obligatorio")));
        } else if (!SecurityUtil.isAdmin(SecurityUtil.getUser())
                && !SecurityUtil.getUser().perteneceAlCentro(sms.getCentro())) {
            messages.add(new BusinessMessage(I18n.get("No puede crear SMS para un centro que no es suyo")));
        }
    }

    private void validarHistorialEstado(Sms sms, BusinessMessages messages) {
        // Sin releer por repositorio: JPA.edit ya resolvió la referencia; un padre sin id es una cáscara del cliente.
        HistorialEstado padre = sms.getHistorialEstado();
        if (padre.getId() == null
                || padre.getExpediente() == null
                || !Objects.equals(padre.getExpediente().getCentro(), sms.getCentro())) {
            messages.add(new BusinessMessage(I18n.get("El estado del expediente indicado no existe")));
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Sms nuevo, Sms original) {
        return Optional.of(BusinessMessages.single(I18n.get("El SMS es inmutable tras su creación.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Sms sms) {
        return Optional.of(BusinessMessages.single(I18n.get("Los SMS no se pueden borrar.")));
    }

    @Override
    public Optional<BusinessMessages> validateReenviar(Sms entidad, Sms entidadOriginal) {
        if (entidadOriginal == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Solo se pueden reenviar SMS que han fallado")));
        }

        BusinessMessages messages = new BusinessMessages();

        if (entidadOriginal.getEstado() != EstadoSms.FALLIDO) {
            messages.add(new BusinessMessage(I18n.get("Solo se pueden reenviar SMS que han fallado")));
        }

        User user = SecurityUtil.getUser();
        Centro centro = entidadOriginal.getCentro();
        if (!SecurityUtil.isAdmin(user)
                && !(user.tieneTipoUsuario(centro, TipoUsuarioCodigo.SUPERVISOR)
                        || user.tieneTipoUsuario(centro, TipoUsuarioCodigo.ADMINISTRATIVO))) {
            messages.add(new BusinessMessage(I18n.get("No puede reenviar SMS de un centro que no es suyo")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "dniDestinatario", Map.of(),
                "nombre", Map.of(),
                "apellidos", Map.of(),
                "telefono", Map.of(),
                "mensaje", Map.of(),
                "centro", Map.of(),
                "historialEstado", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesReenviar() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesRemove() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_NormalizarTelefono(Sms sms) {
        sms.setTelefono(new NumeroTelefono(sms.getTelefono()).enFormatoE164());
    }

    private void fireActionRule_AsignarValoresIniciales(Sms sms) {
        sms.setEstado(EstadoSms.PENDIENTE);
        sms.setFechaCreacion(LocalDateTime.now(Convert.defaultZoneId));
        sms.setNumeroReintentos(0);
        sms.setFechaPrimerIntentoEnvio(null);
        sms.setFechaUltimoIntentoEnvio(null);
        sms.setFechaEnvio(null);
        sms.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_ProgramarEnvioAsincrono(Sms sms) {
        // Solo el id: la entidad pertenece al EntityManager de esta transacción y la tarea corre en otro hilo.
        Long smsId = sms.getId();
        ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarSms(smsId));
    }

    private void fireActionRule_RegistrarIntentoEnvio(Sms sms) {
        LocalDateTime ahora = LocalDateTime.now(Convert.defaultZoneId);
        sms.setFechaUltimoIntentoEnvio(ahora);
        if (sms.getFechaPrimerIntentoEnvio() == null) {
            sms.setFechaPrimerIntentoEnvio(ahora);
        }
        sms.setNumeroReintentos(sms.getNumeroReintentos() + 1);
    }

    private void fireActionRule_MarcarEnvioCorrecto(Sms sms) {
        sms.setEstado(EstadoSms.ENVIADO);
        sms.setFechaEnvio(LocalDateTime.now(Convert.defaultZoneId));
        sms.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_MarcarEnvioFallido(Sms sms, RuntimeException excepcion) {
        sms.setEstado(EstadoSms.FALLIDO);
        sms.setDescripcionUltimoFallo(ExceptionUtil.getTraceAsString(excepcion));
        sms.setFechaEnvio(null);
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private void enviarSms(Long smsId) {
        JPA.runInTransaction(() -> {
            // Bloqueo pesimista: dos «Reenviar» del mismo SMS pueden correr a la vez en el pool (reenviar no
            // cambia el estado, así que el SMS sigue FALLIDO hasta que acaba el envío); así el segundo espera al
            // commit del primero y, si este lo consiguió, ve ENVIADO en lugar de mandar (y pagar) el SMS otra vez.
            Sms sms = JPA.em().find(Sms.class, smsId, LockModeType.PESSIMISTIC_WRITE);
            if (sms == null) {
                throw new IllegalStateException("No existe el SMS " + smsId);
            }
            if (sms.getEstado() == EstadoSms.ENVIADO) {
                return;
            }

            fireActionRule_RegistrarIntentoEnvio(sms);
            try {
                // El Provider se resuelve aquí para que unas credenciales ausentes dejen el SMS FALLIDO.
                smsSenderProvider.get().send(
                        new com.educaflow.base.infrastructure.sms.Sms(sms.getTelefono(), sms.getMensaje()));
                fireActionRule_MarcarEnvioCorrecto(sms);
            } catch (RuntimeException ex) {
                fireActionRule_MarcarEnvioFallido(sms, ex);
            }

            repository.save(sms);
        });
    }
}
