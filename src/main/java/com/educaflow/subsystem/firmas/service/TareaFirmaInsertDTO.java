package com.educaflow.subsystem.firmas.service;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.Rectangulo;

import java.util.List;
import java.util.Objects;

public record TareaFirmaInsertDTO(User firmante, List<MetaFile> documentos, String motivoFirma, Rectangulo areaFirma, Integer page, Class<? extends TareaFirmaNotifier> firmaNotifierClass, Object callBackData) {

    public TareaFirmaInsertDTO {
        Objects.requireNonNull(firmante, "firmante no puede ser null");
        Objects.requireNonNull(documentos, "documentos no pueden ser null");
        Objects.requireNonNull(motivoFirma, "motivoFirma no puede ser null");
        Objects.requireNonNull(areaFirma, "areaFirma no puede ser null");
        Objects.requireNonNull(page, "page no puede ser null");
        Objects.requireNonNull(firmaNotifierClass, "firmaNotifierClass no puede ser null");

        if (documentos.isEmpty()) {
            throw new IllegalArgumentException("documentos no puede estar vacío");
        }
        if (motivoFirma.isBlank()) {
            throw new IllegalArgumentException("motivoFirma no puede ser blank");
        }

        for(int i=0;i<documentos.size();i++) {
            MetaFile documento=documentos.get(i);
            Objects.requireNonNull(documento, "El documento " +  i  + " no pueden ser null");
            if (MetaFileHelper.isPdf(documento) == false) {
                throw new IllegalArgumentException("El documento " +  i  + " debe ser un pdf");
            }
        }

    }

}
