package com.educaflow.tramites.util.firma;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.criptografia.service.SituacionFirma;
import com.educaflow.subsystem.criptografia.service.CredentialsFailureException;
import com.educaflow.subsystem.criptografia.service.FirmaEnServidorService;
import com.educaflow.subsystem.criptografia.util.CertificadoDigitalHelper;
import com.google.inject.Inject;


public class FirmaServidorHelper {

    @Inject
    private FirmaEnServidorService firmaEnServidorService;


    public MetaFile firmarEnServidor(String dni, SituacionFirma situacionFirma, String clave, MetaFile documentoOriginal, Rectangulo posicion, int pagina) throws BusinessException {

        if ((dni==null) || dni.isBlank()) {
            throw new IllegalArgumentException("No hay DNI para el firmante");
        }
        if (DniUtil.isValid(dni) == false) {
            throw new IllegalArgumentException("El DNI no es válido:"+dni);
        }
        if (situacionFirma==null) {
            throw new NullPointerException("No hay situación de firma para el firmante con DNI " + dni);
        }
        if (situacionFirma.isFirmaEnServidor() == false) {
            throw new IllegalStateException("No corresponde firmar en el servidor con la situación de firma " + situacionFirma);
        }
        if (documentoOriginal == null) {
            throw new IllegalStateException("No hay documento de entrada que firmar");
        }

        CampoFirma campoFirma = new CampoFirma(posicion).setNumeroPagina(pagina);

        try {
            DocumentoPdf documentoFirmado = firmaEnServidorService.firmar(dni, clave, MetaFileHelper.getDocumentoPdf(documentoOriginal), campoFirma);
            return MetaFileHelper.createMetaFile(documentoFirmado);
        } catch (CredentialsFailureException ex) {
            throw new BusinessException(BusinessMessages.single(I18n.get("No se ha podido firmar la solicitud: %s").formatted(CertificadoDigitalHelper.motivoClaveErronea(situacionFirma))));
        }
    }



}
