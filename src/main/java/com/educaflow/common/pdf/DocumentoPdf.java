package com.educaflow.common.pdf;


import com.educaflow.common.criptografia.AlmacenClave;

import java.util.List;
import java.util.Map;

public interface DocumentoPdf {


    List<String> getNombreCamposFormulario();
    List<ResultadoFirma> getFirmasPdf();
    int getNumeroPaginas();
    String getFileName();
    
    
    DocumentoPdf setValorCamposFormularioAndFlatten(Map<String,String> valores);
    DocumentoPdf firmar(AlmacenClave almacenClave, CampoFirma campoFirma);
    DocumentoPdf anyadirDocumentoPdf(DocumentoPdf documentoPdf);
    DocumentoPdf anyadirDocumentoPdf(DocumentoPdf documentoPdf,String fileName);

    byte[] getDatos();
}