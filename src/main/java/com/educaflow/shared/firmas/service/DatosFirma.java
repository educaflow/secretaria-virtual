package com.educaflow.shared.firmas.service;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.Rectangulo;

public record DatosFirma(User firmante, MetaFile documento,String motivoFirma, Rectangulo areaFirma,Class<? extends FirmaNotifier> firmaNotifierClass,Object callBackData) {

    public DatosFirma {
        if (firmante == null) {
            throw new IllegalArgumentException("firmante no puede ser null");
        }
        if (documento == null) {
            throw new IllegalArgumentException("documento no puede ser null");
        }
        if ((motivoFirma==null) || motivoFirma.isBlank()) {
            throw new IllegalArgumentException("motivoFirma no puede ser null ni blank");
        }
        if (areaFirma == null) {
            throw new IllegalArgumentException("areaFirma no puede ser null");
        }
        if (firmaNotifierClass == null) {
            throw new IllegalArgumentException("firmaNotifierClass no puede ser null");
        }

        if (MetaFileHelper.isPdf(documento)==false) {
            throw new IllegalArgumentException("documento debe ser un pdf");
        }

    }

}
