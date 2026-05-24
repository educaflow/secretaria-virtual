package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.base.util.JsonUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.firmas.db.DocumentoFirma;
import com.educaflow.subsystem.firmas.db.EstadoTareaFirma;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO;
import com.educaflow.subsystem.firmas.service.TareaFirmaNotifier;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class TareaFirmaServiceImpl extends DefaultModelService<TareaFirma> implements TareaFirmaService {

    public TareaFirmaServiceImpl(Class<TareaFirma> model, Repository<TareaFirma> repository) {
        super(model, repository);
    }

    @Override
    public TareaFirma insert(TareaFirmaInsertDTO tareaFirmaInsertDTO)  {
        validateInsert(tareaFirmaInsertDTO).ifPresent(BusinessMessages::throwIfInvalid);

        TareaFirma tareaFirma=new TareaFirma();
        tareaFirma.setFirmante(tareaFirmaInsertDTO.firmante());
        tareaFirma.setFechaSolicitud(LocalDateTime.now());
        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.PENDIENTE);
        tareaFirma.setMotivoFirma(tareaFirmaInsertDTO.motivoFirma());
        tareaFirma.setMotivoRechazo(null);


        List<DocumentoFirma> documentosFirma=new ArrayList<>();
        for(MetaFile documento: tareaFirmaInsertDTO.documentos()) {
            DocumentoFirma documentoFirma = new DocumentoFirma();
            documentoFirma.setDocumentoOriginal(MetaFileUtil.cloneMetaFile(documento));
            documentoFirma.setTareaFirma(tareaFirma);
            documentosFirma.add(documentoFirma);
        }
        tareaFirma.setDocumentosFirma(documentosFirma);



        tareaFirma.setFqcnFirmaNotifier(tareaFirmaInsertDTO.firmaNotifierClass().getName());
        Object callBackData= tareaFirmaInsertDTO.callBackData();
        if(callBackData!=null){
            tareaFirma.setFqcnCallBackData(callBackData.getClass().getName());
            tareaFirma.setCallBackData(JsonUtil.toJson(callBackData));
        } else {
            tareaFirma.setFqcnCallBackData(null);
            tareaFirma.setCallBackData(null);
        }



        tareaFirma.setX(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().x()));
        tareaFirma.setY(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().y()));
        tareaFirma.setWidth(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().width()));
        tareaFirma.setHeight(BigDecimal.valueOf(tareaFirmaInsertDTO.areaFirma().height()));

        tareaFirma.setPage(tareaFirmaInsertDTO.page());

        tareaFirma = repository.save(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)  {
        validateMarcarComoFirmada(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.FIRMADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma = repository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal)  {
        validateMarcarComoRechazada(tareaFirma, tareaFirmaOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        tareaFirma.setEstadoTareaFirma(EstadoTareaFirma.RECHAZADO);
        tareaFirma.setFechaResolucion(LocalDateTime.now());

        tareaFirma = repository.save(tareaFirma);

        fireActionRule_NotificarFirmaResuelta(tareaFirma);

        return tareaFirma;
    }

    @Override
    public Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma) {
        validateValidarDocumentosFirmados(tareaFirma).ifPresent(BusinessMessages::throwIfInvalid);

        BusinessMessages businessMessages=new BusinessMessages();

        for (DocumentoFirma documentoFirma : tareaFirma.getDocumentosFirma()) {
            DocumentoPdf documentoOriginal = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoOriginal());
            DocumentoPdf documentoFirmado = MetaFileHelper.getDocumentoPdf(documentoFirma.getDocumentoFirmado());
            Optional<String> errorFirma = DocumentoPdfUtil.validateFirmaPdf(documentoOriginal, documentoFirmado, tareaFirma.getFirmante().getDni());
            if (errorFirma.isPresent()) {
                businessMessages.add(new BusinessMessage(documentoFirmado.getFileName(),errorFirma.get()));
            }
        }

        if (businessMessages.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(businessMessages);
        }

    }


    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    public Optional<BusinessMessages> validateInsert(TareaFirmaInsertDTO tareaFirmaInsertDTO) {
        return Optional.empty();
    }
    public Optional<BusinessMessages> validateMarcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        return Optional.empty();
    }
    public Optional<BusinessMessages> validateMarcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal) {
        return Optional.empty();
    }
    public Optional<BusinessMessages> validateValidarDocumentosFirmados(TareaFirma tareaFirma) { return Optional.empty();}


    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    public AllowProperties allowPropertiesMarcarComoFirmada() {
        return AllowProperties.createAllowProperties(Map.of("documentosFirma", Map.of("documentoFirmado", Map.of())));
    };
    public AllowProperties allowPropertiesMarcarComoRechazada() {
        return AllowProperties.createAllowProperties(Map.of("motivoRechazo", Map.of()));
    };
    public AllowProperties allowPropertiesValidarDocumentosFirmados(){
        return AllowProperties.createAllowAllProperties();
    };


    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    @SuppressWarnings("unchecked")
    private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma) {
        try {
            Class<? extends TareaFirmaNotifier> firmaNotifierClass = (Class<? extends TareaFirmaNotifier>) Class.forName(tareaFirma.getFqcnFirmaNotifier());
            TareaFirmaNotifier tareaFirmaNotifier = Beans.get(firmaNotifierClass);

            Object callBackData = null;
            if (tareaFirma.getFqcnCallBackData() != null) {
                Class<?> callBackDataClass = Class.forName(tareaFirma.getFqcnCallBackData());
                callBackData = JsonUtil.fromJson(tareaFirma.getCallBackData(), callBackDataClass);
            }

            tareaFirmaNotifier.notify(tareaFirma, callBackData);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró la clase necesaria para notificar la firma resuelta: " + e.getMessage(), e);
        }
    }



}