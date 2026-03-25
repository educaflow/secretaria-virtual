package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.ResultadoFirma;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.JsonUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.firma.db.DocumentoFirma;
import com.educaflow.subsystem.firma.db.EstadoTareaFirma;
import com.educaflow.subsystem.firma.db.TareaFirma;
import com.educaflow.subsystem.firma.db.repo.TareaFirmaRepository;
import com.educaflow.subsystem.firmas.service.DatosFirma;
import com.educaflow.subsystem.firmas.service.FirmaService;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class FirmaServiceImpl implements FirmaService {

    @Inject
    TareaFirmaRepository tareaFirmaRepository;

    @Override
    public TareaFirma insert(DatosFirma datosFirma) throws BusinessException {
        TareaFirma tareaFirma=new TareaFirma();
        tareaFirma.setFirmante(datosFirma.firmante());
        tareaFirma.setFechaSolicitud(LocalDateTime.now());
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.PENDIENTE);
        tareaFirma.setMotivoFirma(datosFirma.motivoFirma());
        tareaFirma.setMotivoRechazo(null);


        List<DocumentoFirma> documentosFirma=new ArrayList<>();
        for(MetaFile documento:datosFirma.documentos()) {
            DocumentoFirma documentoFirma = new DocumentoFirma();
            documentoFirma.setDocumentoOriginal(MetaFileUtil.cloneMetaFile(documento));
            documentoFirma.setTareaFirma(tareaFirma);
            documentosFirma.add(documentoFirma);
        }
        tareaFirma.setDocumentosFirma(documentosFirma);



        tareaFirma.setFqcnFirmaNotifier(datosFirma.firmaNotifierClass().getName());
        Object callBackData=datosFirma.callBackData();
        if(callBackData!=null){
            tareaFirma.setFqcnCallBackData(callBackData.getClass().getName());
            tareaFirma.setCallBackData(JsonUtil.toJson(callBackData));
        } else {
            tareaFirma.setFqcnCallBackData(null);
            tareaFirma.setCallBackData(null);
        }



        tareaFirma.setX(BigDecimal.valueOf(datosFirma.areaFirma().x()));
        tareaFirma.setY(BigDecimal.valueOf(datosFirma.areaFirma().y()));
        tareaFirma.setWidth(BigDecimal.valueOf(datosFirma.areaFirma().width()));
        tareaFirma.setHeight(BigDecimal.valueOf(datosFirma.areaFirma().height()));



        tareaFirma=tareaFirmaRepository.save(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma kk) throws BusinessException {
        BusinessMessages businessMessages=new BusinessMessages();

        int i=0;
        for(DocumentoFirma documentoFirma:tareaFirma.getDocumentosFirma()) {
            DocumentoPdf documentoOriginal=MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());
            DocumentoPdf documentoFirmado=MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoFirmado());

            Optional<String> errorFirma=validateFirmaPdf(documentoOriginal,documentoFirmado,tareaFirma.getFirmante().getDni());
            if (errorFirma.isPresent()) {
                businessMessages.add(new BusinessMessage(null, errorFirma.get(),documentoFirmado.getFileName()+" "+i));
            }
            i++;
        }

        if (businessMessages.size()>0) {
            throw new BusinessException(businessMessages);
        }

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);
        tareaFirma.setMotivoRechazo(null);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma=tareaFirmaRepository.save(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException {
        if (tareaFirma.getMotivoRechazo()==null || tareaFirma.getMotivoRechazo().isBlank()) {
            throw new BusinessException("motivoRechazo","Es requerido","Motivo del rechazo de la firma de los documentos" );
        }

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.RECHAZADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma=tareaFirmaRepository.save(tareaFirma);

        return tareaFirma;
    }


    private Optional<String> validateFirmaPdf(DocumentoPdf documentoOriginal, DocumentoPdf documentoFirmado, String nif)   {
        if (documentoOriginal == null) {
            throw new IllegalArgumentException("El documento original no puede ser nulo");
        }
        if (documentoFirmado == null) {
            throw new IllegalArgumentException("El documento firmado no puede ser nulo");
        }
        if (nif == null) {
            throw new IllegalArgumentException("El NIF no puede ser nulo");
        }

        List<ResultadoFirma> resultadosFirmaOriginales=new ArrayList<>(documentoOriginal.getFirmasPdf());
        List<ResultadoFirma> resultadosFirma=new ArrayList<>(documentoFirmado.getFirmasPdf());

        for (ResultadoFirma resultadoFirmaOriginal:resultadosFirmaOriginales) {
            removeResultadoFirma(resultadosFirma,resultadoFirmaOriginal);
        }

        if (resultadosFirma.size()>1) {
            return Optional.of("El documento se ha firmado más de una vez");
        }
        if (resultadosFirma.size()==0) {
            return Optional.of("El documento no se ha firmado");
        }

        ResultadoFirma resultadoFirmaNueva=resultadosFirma.get(0);

        if (resultadoFirmaNueva.isCorrecta()==false) {
            return Optional.of("La firma no es correcta. Hay un error en ella");
        }

        if (resultadoFirmaNueva.getDatosCertificado().isValidoEnListaCertificadosConfiables()==false) {
            return Optional.of("La firma no es valida según la lista de certificados aceptados por la aplicación");
        }
        if (resultadoFirmaNueva.getDatosCertificado().isSelloTiempo()==true) {
            return Optional.of("La firma no puede ser un sello de tiempo");
        }

        if (Objects.equals(resultadoFirmaNueva.getDatosCertificado().getDNI(), nif)==false) {
            return Optional.of("El documento no ha sido firmado con el DNI/NIF/NIE "+nif+ " sino con el "+resultadoFirmaNueva.getDatosCertificado().getDNI());
        }

        if (documentoOriginal.getPlainText().equals(documentoFirmado.getPlainText())==false) {
            return Optional.of("El documento firmado no es igual al documento original");
        }

        return Optional.empty();
    }

    private void removeResultadoFirma(List<ResultadoFirma> resultadosFirma,ResultadoFirma resultadoFirma) {
        Iterator<ResultadoFirma> it = resultadosFirma.iterator();
        while (it.hasNext()) {
            if (it.next().equals(resultadoFirma)) {
                it.remove();
                return;
            }
        }
    }


}
