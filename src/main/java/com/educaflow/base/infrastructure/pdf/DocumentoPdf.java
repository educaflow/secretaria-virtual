package com.educaflow.base.infrastructure.pdf;


import com.educaflow.base.infrastructure.criptografia.AlmacenClave;

import java.util.List;
import java.util.Map;

public interface DocumentoPdf {


    List<String> getNombreCamposFormulario();
    List<String> getNombreCamposFirmaVacios();
    List<ResultadoFirma> getFirmasPdf();
    int getNumeroPaginas();
    /**
     * @param numeroPagina Como en {@link CampoFirma#setNumeroPagina(int)}: si es negativo se cuenta desde el final
     * @return el recuadro que ocupa la página, en puntos PDF y tal y como se ve (con su rotación aplicada)
     */
    Rectangulo getTamanyoPagina(int numeroPagina);
    String getFileName();
    /**
     * @param nombre El nombre del metadato, que es una clave propia del diccionario de información del PDF
     * @return el valor del metadato o {@code null} si el documento no lo tiene
     */
    String getMetadato(String nombre);
    
    
    DocumentoPdf setValorCamposFormularioAndFlatten(Map<String,String> valores);
    DocumentoPdf firmar(AlmacenClave almacenClave, CampoFirma campoFirma);
    DocumentoPdf anyadirDocumentoPdf(DocumentoPdf documentoPdf);
    DocumentoPdf anyadirDocumentoPdf(DocumentoPdf documentoPdf,String fileName);
    /**
     * Añade el metadato sin reescribir el documento, así que no invalida las firmas que ya tenga.
     * @param nombre El nombre del metadato: una clave propia del diccionario de información del PDF
     */
    DocumentoPdf setMetadato(String nombre, String valor);
    DocumentoPdf estamparTextoConAppend(String texto, int numeroPagina,Rectangulo rectangulo);
    DocumentoPdf addNewPage();
    DocumentoPdf removePdfAConformance();

    String getPlainText();

    byte[] getDatos();
}