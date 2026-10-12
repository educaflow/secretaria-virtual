package com.educaflow.tramites.shared.registro;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.expedientes.db.ExpedienteResoluble;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.registroentradasalida.db.RegistroSalida;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.tramites.shared.notificaciones.NotificacionesExpedienteHelper;
import com.google.inject.Inject;

import java.util.List;

/**
 * Lo que hacen igual todos los expedientes resolubles al registrar sus documentos: la solicitud firmada en el
 * registro de entrada y la resolución firmada en el registro de salida. Trabaja con los campos que declara
 * {@code ExpedienteResoluble}. Guarda el documento que devuelve el registro y la fecha del asiento; la fecha de la
 * firma ({@code fechaHoraPdf*Firmada}) no es suya: la pone el tipo antes de llamar, del PDF o del escaneado.
 * A qué estado se pasa después lo decide cada tipo, que es el único que puede nombrar los estados de su
 * {@code States}.
 */
public class RegistroEntradaSalidaExpedienteHelper {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    @Inject
    private NotificacionesExpedienteHelper notificacionesExpedienteHelper;


    public void registroEntradaSolicitud(ExpedienteResoluble expediente, List<MetaFile> anexos, EventContext eventContext) throws BusinessException {
        RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(expediente.getPdfSolicitudFirmada(), anexos);
        expediente.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
        expediente.setFechaHoraPdfJustificanteRegistroEntrada(registroEntrada.getFecha());
        notificacionesExpedienteHelper.notificarRegistroEntrada(expediente, registroEntrada);

        expediente.setResultadoVerificacion(null);
        expediente.setTextoSubsanacion(null);
    }


    public void registroSalidaResolucion(ExpedienteResoluble expediente, List<MetaFile> anexos, EventContext eventContext) throws BusinessException {
        RegistroSalida registroSalida = eventContext.createRegistroSalida(expediente.getPdfResolucionFirmada(), anexos);
        expediente.setPdfResolucionFirmadaRegistroSalida(registroSalida.getDocumento());
        expediente.setFechaHoraPdfResolucionFirmadaRegistroSalida(registroSalida.getFecha());
        notificacionesExpedienteHelper.notificarRegistroSalida(expediente, registroSalida);
    }
}
