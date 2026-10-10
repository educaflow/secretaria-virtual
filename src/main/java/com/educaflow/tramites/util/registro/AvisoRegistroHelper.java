package com.educaflow.tramites.util.registro;

import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.db.Notificacion;
import com.educaflow.subsystem.notificaciones.db.Sms;
import com.educaflow.subsystem.notificaciones.service.CorreoService;
import com.educaflow.subsystem.notificaciones.service.SmsService;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.google.inject.Inject;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Avisa a quien presentó la solicitud, por correo y por SMS, de cada documento de su expediente que se asienta en el
 * registro de entrada o de salida. El correo lleva adjunto el documento registrado; el SMS, solo el número.
 *
 * <p>El aviso es una cortesía, no parte del trámite: el documento queda en el expediente. Por eso cada canal se
 * valida por separado y, si no lo supera (p. ej. en papel no hay correo ni móvil del solicitante), no se envía, se
 * registra el motivo y el evento sigue adelante.
 */
public class AvisoRegistroHelper {

    private static final Logger log = LoggerFactory.getLogger(AvisoRegistroHelper.class);

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /** Avisa del registro de entrada de la solicitud y le adjunta al correo el resguardo de presentación. */
    public void avisarDeRegistroEntrada(Expediente expediente, RegistroEntrada registroEntrada) {
        String numeroExpediente = expediente.getNumeroExpediente();
        String numeroRegistro = registroEntrada.getNumeroRegistro();

        avisar(expediente, new Aviso(
                I18n.get("Registro de entrada %s").formatted(numeroRegistro),
                I18n.get("Registro de entrada %s del expediente %s").formatted(numeroRegistro, numeroExpediente),
                I18n.get("Su solicitud «%s» (expediente %s) se ha registrado de entrada con el número %s.\n\nSe adjunta el resguardo de presentación.")
                        .formatted(expediente.getName(), numeroExpediente, numeroRegistro),
                I18n.get("Expediente %s: registro de entrada num. %s").formatted(numeroExpediente, numeroRegistro),
                registroEntrada.getDocumentoResguardoPresentacion()));
    }

    /** Avisa del registro de salida de un documento del expediente y se lo adjunta al correo. */
    public void avisarDeRegistroSalida(Expediente expediente, RegistroSalida registroSalida) {
        String numeroExpediente = expediente.getNumeroExpediente();
        String numeroRegistro = registroSalida.getNumeroRegistro();

        avisar(expediente, new Aviso(
                I18n.get("Registro de salida %s").formatted(numeroRegistro),
                I18n.get("Registro de salida %s del expediente %s").formatted(numeroRegistro, numeroExpediente),
                I18n.get("Se ha emitido un documento de su expediente «%s» (expediente %s) con el número de registro de salida %s.\n\nSe adjunta el documento.")
                        .formatted(expediente.getName(), numeroExpediente, numeroRegistro),
                I18n.get("Expediente %s: registro de salida num. %s").formatted(numeroExpediente, numeroRegistro),
                registroSalida.getDocumento()));
    }

    private void avisar(Expediente expediente, Aviso aviso) {
        CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
        SmsService smsService = (SmsService) modelServiceFactory.resolve(Sms.class);

        insertarSiEsValida(correoService, crearCorreo(correoService, expediente, aviso), expediente);
        insertarSiEsValida(smsService, crearSms(smsService, expediente, aviso), expediente);
    }

    private Correo crearCorreo(CorreoService correoService, Expediente expediente, Aviso aviso) {
        Correo correo = correoService.create();
        rellenarDatosComunes(correo, expediente, aviso);
        correo.setPara(expediente.getPersonaSolicitante().getEmail());
        correo.setAsunto(aviso.asunto());
        correo.setCuerpo(aviso.cuerpo());

        Adjunto adjunto = new Adjunto();
        adjunto.setNombreFichero(aviso.documento().getFileName());
        adjunto.setContenido(aviso.documento());
        correo.addAdjunto(adjunto);

        return correo;
    }

    private Sms crearSms(SmsService smsService, Expediente expediente, Aviso aviso) {
        Sms sms = smsService.create();
        rellenarDatosComunes(sms, expediente, aviso);
        sms.setTelefono(expediente.getPersonaSolicitante().getTelefono());
        sms.setMensaje(aviso.mensajeSms());

        return sms;
    }

    private static void rellenarDatosComunes(Notificacion notificacion, Expediente expediente, Aviso aviso) {
        Persona solicitante = expediente.getPersonaSolicitante();

        notificacion.setName(aviso.motivo());
        notificacion.setHistorialEstado(estadoActual(expediente));
        notificacion.setDniDestinatario(solicitante.getDni());
        notificacion.setNombre(solicitante.getNombre());
        notificacion.setApellidos(solicitante.getApellidos());
        notificacion.setCentro(expediente.getCentro());
    }

    private static <T extends Notificacion> void insertarSiEsValida(ModelService<T> service, T notificacion, Expediente expediente) {
        Optional<BusinessMessages> errores = service.validateInsert(notificacion);
        if (errores.isPresent()) {
            String motivos = errores.get().stream()
                    .map(BusinessMessage::getMessage)
                    .collect(Collectors.joining("; "))
                    .replaceAll("[\\r\\n]", " ");
            log.info("No se avisa por {} del registro del expediente id={}: no supera la validación: {}",
                    notificacion.getClass().getSimpleName(), expediente.getId(), motivos);
            return;
        }

        service.insert(notificacion);
    }

    // Se llama desde un trigger*, antes de que el Tramitador añada el historial del estado nuevo.
    private static HistorialEstado estadoActual(Expediente expediente) {
        return expediente.getHistorialEstados().stream()
                .max(Comparator.comparing(HistorialEstado::getFecha))
                .orElseThrow();
    }

    private record Aviso(String motivo, String asunto, String cuerpo, String mensajeSms, MetaFile documento) {
    }
}
