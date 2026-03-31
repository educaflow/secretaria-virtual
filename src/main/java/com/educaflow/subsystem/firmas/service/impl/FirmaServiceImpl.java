package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
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
import com.educaflow.subsystem.firmas.service.FirmaNotifier;
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
    public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException {
        fireConstraintRule_DocumentosValidos(tareaFirma);

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);
        tareaFirma.setMotivoRechazo(null);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma=tareaFirmaRepository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) throws BusinessException {
        fireConstraintRule_MotivoRechazoRequerido(tareaFirma);

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.RECHAZADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma=tareaFirmaRepository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }



    /************************************************************************************/
    /********************************    Action Rules    ********************************/
    /************************************************************************************/

    @SuppressWarnings("unchecked")
    private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma) {
        try {
            Class<? extends FirmaNotifier> firmaNotifierClass = (Class<? extends FirmaNotifier>) Class.forName(tareaFirma.getFqcnFirmaNotifier());
            FirmaNotifier firmaNotifier = Beans.get(firmaNotifierClass);

            Object callBackData = null;
            if (tareaFirma.getFqcnCallBackData() != null) {
                Class<?> callBackDataClass = Class.forName(tareaFirma.getFqcnCallBackData());
                callBackData = JsonUtil.fromJson(tareaFirma.getCallBackData(), callBackDataClass);
            }

            firmaNotifier.notify(tareaFirma, callBackData);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /****************************************************************************************/
    /********************************    Constraint Rules    ********************************/
    /****************************************************************************************/

    private void fireConstraintRule_DocumentosValidos(TareaFirma tareaFirma) throws BusinessException {
        BusinessMessages businessMessages=new BusinessMessages();

        int i=0;
        for(DocumentoFirma documentoFirma:tareaFirma.getDocumentosFirma()) {
            DocumentoPdf documentoOriginal=MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());
            DocumentoPdf documentoFirmado=MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoFirmado());

            Optional<String> errorFirma=DocumentoPdfUtil.validateFirmaPdf(documentoOriginal,documentoFirmado,tareaFirma.getFirmante().getDni());
            if (errorFirma.isPresent()) {
                businessMessages.add(new BusinessMessage(null, errorFirma.get(),documentoFirmado.getFileName()+" "+i));
            }
            i++;
        }

        if (businessMessages.size()>0) {
            throw new BusinessException(businessMessages);
        }
    }

    private void fireConstraintRule_MotivoRechazoRequerido(TareaFirma tareaFirma) throws BusinessException {
        if (tareaFirma.getMotivoRechazo()==null || tareaFirma.getMotivoRechazo().isBlank()) {
            throw new BusinessException("motivoRechazo","Es requerido","Motivo del rechazo de la firma de los documentos" );
        }
    }





}
