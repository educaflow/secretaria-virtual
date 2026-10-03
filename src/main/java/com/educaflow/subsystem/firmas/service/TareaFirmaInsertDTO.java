package com.educaflow.subsystem.firmas.service;

import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Centro;

import java.util.List;
import java.util.Objects;

/**
 * Los datos de una tarea de firma nueva. Dónde se firma cada documento se dice de una de estas dos formas:
 * con {@code nombreCampoFirma}, el campo de firma vacío que tienen <b>todos</b> los documentos; o con
 * {@code areaFirma} y {@code page}, un rectángulo de una página. Hay un constructor para cada una.
 */
public record TareaFirmaInsertDTO(User firmante, Centro centro, List<MetaFile> documentos, String motivoFirma, Rectangulo areaFirma, Integer page, String nombreCampoFirma, Class<? extends TareaFirmaNotifier> firmaNotifierClass, Object callBackData) {

    /** Se firma en el campo de firma vacío {@code nombreCampoFirma}, que deben tener todos los documentos. */
    public TareaFirmaInsertDTO(User firmante, Centro centro, List<MetaFile> documentos, String motivoFirma, String nombreCampoFirma, Class<? extends TareaFirmaNotifier> firmaNotifierClass, Object callBackData) {
        this(firmante, centro, documentos, motivoFirma, null, null, TextUtil.requireNonBlank(nombreCampoFirma, "nombreCampoFirma no puede ser null ni blank"), firmaNotifierClass, callBackData);
    }

    /** Se firma en el rectángulo {@code areaFirma} de la página {@code page} de cada documento. */
    public TareaFirmaInsertDTO(User firmante, Centro centro, List<MetaFile> documentos, String motivoFirma, Rectangulo areaFirma, Integer page, Class<? extends TareaFirmaNotifier> firmaNotifierClass, Object callBackData) {
        this(firmante, centro, documentos, motivoFirma, Objects.requireNonNull(areaFirma, "areaFirma no puede ser null"), Objects.requireNonNull(page, "page no puede ser null"), null, firmaNotifierClass, callBackData);
    }

    public TareaFirmaInsertDTO {
        Objects.requireNonNull(firmante, "firmante no puede ser null");
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(documentos, "documentos no pueden ser null");
        TextUtil.requireNonBlank(motivoFirma, "motivoFirma no puede ser null ni blank");
        Objects.requireNonNull(firmaNotifierClass, "firmaNotifierClass no puede ser null");

        if (documentos.isEmpty()) {
            throw new IllegalArgumentException("documentos no puede estar vacío");
        }

        for(int i=0;i<documentos.size();i++) {
            MetaFile documento=documentos.get(i);
            Objects.requireNonNull(documento, "El documento " +  i  + " no pueden ser null");
            if (MetaFileHelper.isPdf(documento) == false) {
                throw new IllegalArgumentException("El documento " +  i  + " debe ser un pdf");
            }
        }

        if (nombreCampoFirma == null) {
            Objects.requireNonNull(areaFirma, "Hay que indicar dónde se firma: nombreCampoFirma o areaFirma y page");
            Objects.requireNonNull(page, "page no puede ser null si se firma en areaFirma");
        } else {
            exigeFirmarSoloEnElCampo(nombreCampoFirma, areaFirma, page, documentos);
        }

    }

    private static void exigeFirmarSoloEnElCampo(String nombreCampoFirma, Rectangulo areaFirma, Integer page, List<MetaFile> documentos) {
        TextUtil.requireNonBlank(nombreCampoFirma, "nombreCampoFirma no puede ser null ni blank");
        if (areaFirma != null || page != null) {
            throw new IllegalArgumentException("Solo se puede indicar una forma de situar la firma: nombreCampoFirma o areaFirma y page, no las dos");
        }
        for (int i = 0; i < documentos.size(); i++) {
            List<String> camposFirmaVacios = MetaFileHelper.getDocumentoPdf(documentos.get(i)).getNombreCamposFirmaVacios();
            if (camposFirmaVacios.contains(nombreCampoFirma) == false) {
                throw new IllegalArgumentException("El documento " + i + " no tiene ningún campo de firma vacío que se llame '" + nombreCampoFirma + "'. Los que tiene son: " + camposFirmaVacios);
            }
        }
    }

}
