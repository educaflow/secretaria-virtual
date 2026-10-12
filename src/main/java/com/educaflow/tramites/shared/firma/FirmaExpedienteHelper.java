package com.educaflow.tramites.shared.firma;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.ResultadoFirma;
import com.educaflow.subsystem.criptografia.db.CertificadoDigital;
import com.educaflow.subsystem.criptografia.service.CertificadoDigitalService;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.expedientes.db.ExpedienteResoluble;
import com.google.inject.Inject;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

public class FirmaExpedienteHelper {

    @Inject
    private FirmaEnServidorService firmaEnServidorService;

    @Inject
    private ModelServiceFactory modelServiceFactory;


    public void firmarSolicitudEnServidor(String dniFirmante, ExpedienteResoluble expediente, String nombreCampoFirma) {
        firmarEnServidor(dniFirmante, expediente.getClaveCertificado(), expediente.getPdfSolicitud(), nombreCampoFirma).ifPresent(expediente::setPdfSolicitudFirmada);
    }


    public void firmarResolucionEnServidor(String dniFirmante, ExpedienteResoluble expediente, String nombreCampoFirma) {
        firmarEnServidor(dniFirmante, expediente.getClaveCertificado(), expediente.getPdfResolucion(), nombreCampoFirma).ifPresent(expediente::setPdfResolucionFirmada);
    }


    public void actualizarFechaHoraPdfSolicitudFirmada(ExpedienteResoluble expediente) {
        LocalDateTime fechaUltimaFirma = getFechaUltimaFirmaInPdf(MetaFileHelper.getDocumentoPdf(expediente.getPdfSolicitudFirmada()));
        expediente.setFechaHoraPdfSolicitudFirmada(fechaUltimaFirma);
    }


    public void actualizarFechaHoraPdfResolucionFirmada(ExpedienteResoluble expediente) {
        LocalDateTime fechaUltimaFirma = getFechaUltimaFirmaInPdf(MetaFileHelper.getDocumentoPdf(expediente.getPdfResolucionFirmada()));
        expediente.setFechaHoraPdfResolucionFirmada(fechaUltimaFirma);
    }


    /****************************************************************************/
    /***************************** Métodos privados *****************************/
    /****************************************************************************/


    private LocalDateTime getFechaUltimaFirmaInPdf(DocumentoPdf documentoPdf) {
        return documentoPdf.getFirmasPdf().stream()
                .map(ResultadoFirma::getFechaFirma)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("El documento " + documentoPdf.getFileName() + " no tiene ninguna firma"));
    }

    private Optional<MetaFile> firmarEnServidor(String dniFirmante, String claveCertificado, MetaFile documentoOriginal, String nombreCampoFirma) {
        final CertificadoDigitalService certificadoDigitalService = (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class);

        SituacionFirma situacionFirma = certificadoDigitalService.getSituacionFirmaByDni(dniFirmante);
        if (situacionFirma.isFirmaEnServidor()) {
            DocumentoPdf documentoPdfOriginal = MetaFileHelper.getDocumentoPdf(documentoOriginal);
            DocumentoPdf documentoPdfFirmado = firmaEnServidorService.firmar(dniFirmante, claveCertificado, documentoPdfOriginal, new CampoFirma(nombreCampoFirma));
            MetaFile metaFileFirmado = MetaFileHelper.createMetaFile(documentoPdfFirmado);
            return Optional.of(metaFileFirmado);
        } else {
            return Optional.empty();
        }
    }

}
