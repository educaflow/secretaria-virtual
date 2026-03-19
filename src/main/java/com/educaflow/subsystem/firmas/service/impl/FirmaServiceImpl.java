package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.meta.db.MetaFile;
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
import java.util.ArrayList;
import java.util.List;

public class FirmaServiceImpl implements FirmaService {

    @Inject
    TareaFirmaRepository tareaFirmaRepository;

    @Override
    public TareaFirma insert(DatosFirma datosFirma) {
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
    public TareaFirma firmar(TareaFirma tareaFirma, MetaFile documentoFirmado) {
        return null;
    }

    @Override
    public TareaFirma rechazarFirma(TareaFirma tareaFirma, String motivoRechazo) {
        return null;
    }
}
