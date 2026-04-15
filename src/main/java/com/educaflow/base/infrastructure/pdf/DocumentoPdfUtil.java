package com.educaflow.base.infrastructure.pdf;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.infrastructure.evaluator.impl.EvaluatorImplGroovy;
import com.educaflow.base.util.Convert;

import java.util.*;
import java.util.Iterator;

public class DocumentoPdfUtil {


    public static DocumentoPdf generate(DocumentoPdf documentoPdf,Map<String,Object> context) {

        List<String> expressions= documentoPdf.getNombreCamposFormulario();

        Evaluator evaluator= new EvaluatorImplGroovy();
        Map<String,Object> result=evaluator.evaluate(expressions, context);

        Map<String, String> resultString = getStringMap(result);


        DocumentoPdf documentoPdfDatos= documentoPdf.setValorCamposFormularioAndFlatten(resultString);

        return documentoPdfDatos;

    }


    public static Optional<String> validateFirmaPdf(DocumentoPdf documentoOriginal, DocumentoPdf documentoFirmado, String dni) {
        if (documentoOriginal == null) {
            throw new IllegalArgumentException("El documento original no puede ser nulo");
        }
        if (documentoFirmado == null) {
            throw new IllegalArgumentException("El documento firmado no puede ser nulo");
        }
        if (dni==null) {
            throw new IllegalArgumentException("El DNI no puede ser nulo");
        }
        if  (dni.isBlank()) {
            throw new IllegalArgumentException("El DNI no puede estar vacio");
        }

        List<ResultadoFirma> resultadosFirmaOriginales=new ArrayList<>(documentoOriginal.getFirmasPdf());
        List<ResultadoFirma> resultadosFirma=new ArrayList<>(documentoFirmado.getFirmasPdf());

        for (ResultadoFirma resultadoFirmaOriginal:resultadosFirmaOriginales) {
            Optional<String> errorRemoveResultadoFirma=removeResultadoFirma(resultadosFirma,resultadoFirmaOriginal);
            if (errorRemoveResultadoFirma.isPresent()) {
                return errorRemoveResultadoFirma;
            }
        }
        if (resultadosFirma.size()>1) {
            return Optional.of("No es posible firmar el documento por más de una persona");
        }

        if (documentoOriginal.getPlainText().equals(documentoFirmado.getPlainText())==false) {
            return Optional.of("El documento firmado no es igual al documento original");
        }


        //Validación de la nueva firma

        if (resultadosFirma.size() == 0) {
            return Optional.of("El documento no se ha firmado");
        }

        ResultadoFirma resultadoFirmaNueva = resultadosFirma.get(0);
        if (resultadoFirmaNueva.isCorrecta() == false) {
            return Optional.of("La firma no es correcta. Hay un error en ella");
        }
        if (resultadoFirmaNueva.getDatosCertificado().isValidoEnListaCertificadosConfiables() == false) {
            return Optional.of("La firma no es valida según la lista de certificados aceptados por la aplicación");
        }
        if (resultadoFirmaNueva.getDatosCertificado().isSelloTiempo() == true) {
            return Optional.of("La firma no puede ser un sello de tiempo");
        }
        if (Objects.equals(resultadoFirmaNueva.getDatosCertificado().getDNI(), dni) == false) {
            return Optional.of("Se debe firmar con el DNI/NIE '" + dni + "' sin embargo se ha firmado con '" + resultadoFirmaNueva.getDatosCertificado().getDNI()+"'");
        }

        return Optional.empty();
    }

    /*****************************************************************************************/
    /******************************    Funciones de utilidad    ******************************/
    /*****************************************************************************************/

    private static Optional<String> removeResultadoFirma(List<ResultadoFirma> resultadosFirma, ResultadoFirma resultadoFirma) {
        Iterator<ResultadoFirma> it = resultadosFirma.iterator();
        while (it.hasNext()) {
            if (it.next().equals(resultadoFirma)) {
                it.remove();
                return Optional.empty();
            }
        }

        return Optional.of("Falta la firma " + resultadoFirma.getDatosCertificado().getCnSubject() + " en el documento");
    }

    private static Map<String, String> getStringMap(Map<String, Object> result) {
        Map<String,String> resultString= new HashMap<>();
        for(Map.Entry<String,Object> entry : result.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                resultString.put(entry.getKey(), (Boolean)entry.getValue() ? "Yes" : "Off");
            } else {
                resultString.put(entry.getKey(), Convert.objectToUserString(entry.getValue()));
            }
        }
        return resultString;
    }





}
