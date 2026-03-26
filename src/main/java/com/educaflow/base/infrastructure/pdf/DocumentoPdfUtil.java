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


    public static Optional<String> validateFirmaPdf(DocumentoPdf documentoOriginal, DocumentoPdf documentoFirmado, String nif) {
        if (documentoOriginal == null) {
            throw new IllegalArgumentException("El documento original no puede ser nulo");
        }
        if (documentoFirmado == null) {
            throw new IllegalArgumentException("El documento firmado no puede ser nulo");
        }
        if (nif == null) {
            throw new IllegalArgumentException("El NIF no puede ser nulo");
        }

        List<ResultadoFirma> resultadosFirmaOriginales=new ArrayList<>(documentoOriginal.getFirmasPdf());
        List<ResultadoFirma> resultadosFirma=new ArrayList<>(documentoFirmado.getFirmasPdf());

        for (ResultadoFirma resultadoFirmaOriginal:resultadosFirmaOriginales) {
            removeResultadoFirma(resultadosFirma,resultadoFirmaOriginal);
        }

        if (resultadosFirma.size()>1) {
            return Optional.of("El documento se ha firmado más de una vez");
        }
        if (resultadosFirma.size()==0) {
            return Optional.of("El documento no se ha firmado");
        }

        ResultadoFirma resultadoFirmaNueva=resultadosFirma.get(0);

        if (resultadoFirmaNueva.isCorrecta()==false) {
            return Optional.of("La firma no es correcta. Hay un error en ella");
        }

        if (resultadoFirmaNueva.getDatosCertificado().isValidoEnListaCertificadosConfiables()==false) {
            return Optional.of("La firma no es valida según la lista de certificados aceptados por la aplicación");
        }
        if (resultadoFirmaNueva.getDatosCertificado().isSelloTiempo()==true) {
            return Optional.of("La firma no puede ser un sello de tiempo");
        }

        if (Objects.equals(resultadoFirmaNueva.getDatosCertificado().getDNI(), nif)==false) {
            return Optional.of("El documento no ha sido firmado con el DNI/NIF/NIE "+nif+ " sino con el "+resultadoFirmaNueva.getDatosCertificado().getDNI());
        }

        if (documentoOriginal.getPlainText().equals(documentoFirmado.getPlainText())==false) {
            return Optional.of("El documento firmado no es igual al documento original");
        }

        return Optional.empty();
    }

    /*****************************************************************************************/
    /******************************    Funciones de utilidad    ******************************/
    /*****************************************************************************************/

    private static void removeResultadoFirma(List<ResultadoFirma> resultadosFirma, ResultadoFirma resultadoFirma) {
        Iterator<ResultadoFirma> it = resultadosFirma.iterator();
        while (it.hasNext()) {
            if (it.next().equals(resultadoFirma)) {
                it.remove();
                return;
            }
        }
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
