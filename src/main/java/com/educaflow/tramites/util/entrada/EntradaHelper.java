package com.educaflow.tramites.util.entrada;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.registroentradasalida.db.RegistroEntrada;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.tramites.util.firma.FirmaServidorHelper;
import com.google.inject.Inject;

import java.util.List;

/**
 * Lo que hacen igual todos los tipos de expediente en su fase {@code ENTRADA}: firmar la solicitud y
 * presentarla en el registro de entrada. A qué estado se pasa después lo decide cada tipo, que es el único
 * que puede nombrar los estados de su {@code States}.
 */
public class EntradaHelper {

    @Inject
    private FirmaServidorHelper firmaServidorHelper;

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /**
     * Firma en el servidor la solicitud generada si a quien presenta le corresponde firmar así. Con AutoFirma no
     * hace nada: la solicitud firmada ya llegó del equipo del usuario y la validó el evento.
     *
     * @param nombreCampoFirma el {@code campoFirma} del hueco de la firma en el documento de la solicitud
     */
    public <T extends Expediente> void firmarSolicitudSiEsEnServidor(T expediente, CamposEntrada<T> campos, String nombreCampoFirma) throws BusinessException {
        final CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        String dniFirmante = SecurityUtil.getUser().getDni();
        SituacionFirma situacionFirma = certificadoDigitalService.getSituacionFirmaByDni(dniFirmante);

        if (situacionFirma.isFirmaEnServidor()) {
            campos.setPdfSolicitudFirmada().accept(expediente, firmaServidorHelper.firmarEnServidor(
                    dniFirmante,
                    situacionFirma,
                    expediente.getClaveCertificado(),
                    campos.getPdfSolicitud().apply(expediente),
                    nombreCampoFirma));
        }
    }

    /**
     * Asienta en el registro de entrada la solicitud que hay en {@code pdfSolicitudFirmada} (la firmada o, en
     * papel, la escaneada), guarda el resguardo y da por atendida la subsanación que se hubiera pedido.
     */
    public <T extends Expediente> void presentar(T expediente, CamposEntrada<T> campos, List<MetaFile> anexos, EventContext eventContext) {
        RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(campos.getPdfSolicitudFirmada().apply(expediente), anexos);
        campos.setPdfJustificanteRegistroEntrada().accept(expediente, registroEntrada.getDocumentoResguardoPresentacion());

        campos.borrarSubsanacion().accept(expediente);
    }

    /**
     * Si el expediente está en ese estado. Sirve a los eventos de la fase que se disparan desde más de un estado
     * (como {@code BACK}) para saber desde cuál: se le pasa el expediente original y el estado del {@code States}
     * del tipo.
     */
    public static boolean estaEn(Expediente expediente, State estado) {
        return estado.getPhase().getCode().equals(expediente.getCodePhase())
                && estado.getCode().equals(expediente.getCodeState());
    }

    /**
     * El camino en papel y el telemático comparten estados pero no eventos: la vista solo ofrece a cada modo los
     * suyos, así que si llega el del otro modo se registraría sin firmar una solicitud que había que firmar, o
     * se pediría firmar a quien solo la registra.
     */
    public static void exigePresentadoEnPapel(Expediente expediente, boolean presentadoEnPapel) {
        if (Boolean.TRUE.equals(expediente.getPresentadoEnPapel()) != presentadoEnPapel) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " (presentadoEnPapel="
                    + expediente.getPresentadoEnPapel() + ") no puede disparar este evento desde el estado '"
                    + expediente.getCodePhase() + "/" + expediente.getCodeState() + "'.");
        }
    }
}
