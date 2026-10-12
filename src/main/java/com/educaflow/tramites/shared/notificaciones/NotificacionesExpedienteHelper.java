package com.educaflow.tramites.shared.notificaciones;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.notificaciones.db.*;
import com.educaflow.subsystem.notificaciones.service.CorreoService;
import com.educaflow.subsystem.notificaciones.service.SmsService;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.google.inject.Inject;

public class NotificacionesExpedienteHelper {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    public void notificarRegistroEntrada(Expediente expediente, RegistroEntrada registroEntrada)  {
        String numeroExpediente = expediente.getNumeroExpediente();
        String numeroRegistro = registroEntrada.getNumeroRegistro();
        TextosNotificacion aviso = new TextosNotificacion(I18n.get("Registro de entrada %s").formatted(numeroRegistro), I18n.get("Registro de entrada %s del expediente %s").formatted(numeroRegistro, numeroExpediente), I18n.get("Su solicitud «%s» (expediente %s) se ha registrado de entrada con el número %s.\n\nSe adjunta el resguardo de presentación.").formatted(expediente.getName(), numeroExpediente, numeroRegistro), I18n.get("Expediente %s: registro de entrada num. %s").formatted(numeroExpediente, numeroRegistro));

        enviarCorreo(expediente, aviso, registroEntrada.getDocumentoResguardoPresentacion());
        enviarSms(expediente, aviso);
    }

    public void notificarRegistroSalida(Expediente expediente, RegistroSalida registroSalida)  {
        String numeroExpediente = expediente.getNumeroExpediente();
        String numeroRegistro = registroSalida.getNumeroRegistro();
        TextosNotificacion aviso = new TextosNotificacion(I18n.get("Registro de salida %s").formatted(numeroRegistro), I18n.get("Registro de salida %s del expediente %s").formatted(numeroRegistro, numeroExpediente), I18n.get("Se ha emitido un documento de su expediente «%s» (expediente %s) con el número de registro de salida %s.\n\nSe adjunta el documento.").formatted(expediente.getName(), numeroExpediente, numeroRegistro), I18n.get("Expediente %s: registro de salida num. %s").formatted(numeroExpediente, numeroRegistro));

        enviarCorreo(expediente, aviso, registroSalida.getDocumento());
        enviarSms(expediente, aviso);
    }

    public void notificarSubsanacion(Expediente expediente, String textoSubsanacion)  {
        String numeroExpediente = expediente.getNumeroExpediente();
        TextosNotificacion aviso = new TextosNotificacion(I18n.get("Subsanación del expediente %s").formatted(numeroExpediente), I18n.get("Tiene que subsanar su solicitud del expediente %s").formatted(numeroExpediente), I18n.get("Se ha revisado su solicitud «%s» (expediente %s) y hay que subsanarla antes de continuar con su tramitación:\n\n%s\n\nEntre en la secretaría virtual, corrija la solicitud en ese mismo expediente y vuelva a presentarla.").formatted(expediente.getName(), numeroExpediente, textoSubsanacion), I18n.get("Expediente %s: tiene que subsanar su solicitud").formatted(numeroExpediente));

        enviarCorreo(expediente, aviso);
        enviarSms(expediente, aviso);
    }


    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/

    private void enviarCorreo(Expediente expediente, TextosNotificacion aviso, MetaFile... documentosAdjuntos) {
        CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
        String para = expediente.getPersonaSolicitante().getEmail();

        if (TextUtil.isNullOrBlank(para)) {
            return;
        }

        Correo correo = correoService.create();
        rellenarDatosComunes(correo, expediente, aviso);
        correo.setPara(para);
        correo.setAsunto(aviso.asunto());
        correo.setCuerpo(aviso.cuerpo());
        for (MetaFile documentoAdjunto : documentosAdjuntos) {
            Adjunto adjunto = new Adjunto();
            adjunto.setNombreFichero(documentoAdjunto.getFileName());
            adjunto.setContenido(documentoAdjunto);
            correo.addAdjunto(adjunto);
        }

        correoService.insert(correo);
    }

    private void enviarSms(Expediente expediente, TextosNotificacion aviso) {
        SmsService smsService = (SmsService) modelServiceFactory.resolve(Sms.class);
        String telefono = expediente.getPersonaSolicitante().getTelefono();

        if (TextUtil.isNullOrBlank(telefono)) {
            return;
        }

        Sms sms = smsService.create();
        rellenarDatosComunes(sms, expediente, aviso);
        sms.setTelefono(telefono);
        sms.setMensaje(aviso.cuerpoCorto());

        smsService.insert(sms);
    }

    private void rellenarDatosComunes(Notificacion notificacion, Expediente expediente, TextosNotificacion aviso) {
        Persona solicitante = expediente.getPersonaSolicitante();

        notificacion.setName(aviso.motivo());
        // Se llama desde un trigger*, antes de que el Tramitador añada el historial del estado nuevo: el último es el del estado desde el que se ha disparado el evento.
        notificacion.setHistorialEstado(expediente.getHistorialEstados().getLast());
        notificacion.setDniDestinatario(solicitante.getDni());
        notificacion.setNombre(solicitante.getNombre());
        notificacion.setApellidos(solicitante.getApellidos());
        notificacion.setCentro(expediente.getCentro());
    }


    private record TextosNotificacion(String motivo, String asunto, String cuerpo, String cuerpoCorto) {
    }
}
