package com.educaflow.subsystem.firmas.service.impl;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.JsonUtil;
import com.educaflow.subsystem.firma.db.EstadoFirma;
import com.educaflow.subsystem.firma.db.Firma;
import com.educaflow.subsystem.firma.db.repo.FirmaRepository;
import com.educaflow.subsystem.firmas.service.DatosFirma;
import com.educaflow.subsystem.firmas.service.FirmaService;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FirmaServiceImpl implements FirmaService {

    @Inject
    FirmaRepository firmaRepository;

    @Override
    public Firma insert(DatosFirma datosFirma) {
        Firma firma=new Firma();
        firma.setFirmante(datosFirma.firmante());
        firma.setDocumento(datosFirma.documento());
        firma.setFechaSolicitud(LocalDateTime.now());
        firma.setEstadoFirma(EstadoFirma.PENDIENTE);
        firma.setMotivoFirma(datosFirma.motivoFirma());
        firma.setMotivoRechazo(null);

        firma.setFqcnFirmaNotifier(datosFirma.firmaNotifierClass().getName());
        Object callBackData=datosFirma.callBackData();
        if(callBackData!=null){
            firma.setFqcnCallBackData(callBackData.getClass().getName());
            firma.setCallBackData(JsonUtil.toJson(callBackData));
        } else {
            firma.setFqcnCallBackData(null);
            firma.setCallBackData(null);
        }



        firma.setX(BigDecimal.valueOf(datosFirma.areaFirma().x()));
        firma.setY(BigDecimal.valueOf(datosFirma.areaFirma().y()));
        firma.setWidth(BigDecimal.valueOf(datosFirma.areaFirma().width()));
        firma.setHeight(BigDecimal.valueOf(datosFirma.areaFirma().height()));



        firma=firmaRepository.save(firma);

        return firma;
    }

    @Override
    public Firma firmar(Firma firma, MetaFile documentoFirmado) {
        return null;
    }

    @Override
    public Firma rechazarFirma(Firma firma, String motivoRechazo) {
        return null;
    }
}
