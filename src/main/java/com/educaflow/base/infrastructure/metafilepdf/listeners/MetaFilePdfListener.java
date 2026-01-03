package com.educaflow.base.infrastructure.metafilepdf.listeners;

import jakarta.persistence.PostLoad;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.ResultadoFirma;

import java.util.List;
import java.util.ArrayList;

import com.educaflow.base.infrastructure.metafilepdf.db.MetaFilePdf;
import com.educaflow.base.infrastructure.metafilepdf.db.DatosFirma;

public class MetaFilePdfListener {
    @PostLoad
    private void onPostLoad(MetaFilePdf metaFilePdf) {

        DocumentoPdf documentoPdf = metaFilePdf.getDocumentoPdf();

        List<DatosFirma> datosFirmas = new ArrayList<>();

        for (ResultadoFirma resultadoFirma : documentoPdf.getFirmasPdf()) {
            DatosFirma datosFirma = new DatosFirma();
            datosFirma.setValida(resultadoFirma.isCorrecta() && resultadoFirma.getDatosCertificado().isValidoEnListaCertificadosConfiables());
            datosFirma.setFechaFirma(resultadoFirma.getFechaFirma());
            datosFirma.setCnSubject(resultadoFirma.getDatosCertificado().getCnSubject());
            datosFirma.setCnIssuer(resultadoFirma.getDatosCertificado().getCnIssuer());

            datosFirmas.add(datosFirma);
        }

        metaFilePdf.setDatosFirmas(datosFirmas);


    }
}
