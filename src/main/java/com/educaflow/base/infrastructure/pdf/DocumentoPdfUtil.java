package com.educaflow.base.infrastructure.pdf;

import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.evaluator.Evaluator;
import com.educaflow.base.infrastructure.evaluator.impl.EvaluatorImplGroovy;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.TextUtil;

import java.util.*;
import java.util.Iterator;
import java.util.stream.Collectors;

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
        Objects.requireNonNull(documentoOriginal, "El documento original no puede ser nulo");
        Objects.requireNonNull(documentoFirmado, "El documento firmado no puede ser nulo");
        TextUtil.requireNonBlank(dni, "El DNI no puede ser nulo ni estar vacio");

        List<ResultadoFirma> resultadosFirmaOriginales=new ArrayList<>(documentoOriginal.getFirmasPdf());
        List<ResultadoFirma> resultadosFirma=new ArrayList<>(documentoFirmado.getFirmasPdf());

        for (ResultadoFirma resultadoFirmaOriginal:resultadosFirmaOriginales) {
            Optional<String> errorRemoveResultadoFirma=removeResultadoFirma(resultadosFirma,resultadoFirmaOriginal);
            if (errorRemoveResultadoFirma.isPresent()) {
                return errorRemoveResultadoFirma;
            }
        }
        if (resultadosFirma.size()>1) {
            return Optional.of(I18n.get("No es posible firmar el documento por más de una persona"));
        }

        if (documentoOriginal.getPlainText().equals(documentoFirmado.getPlainText())==false) {
            return Optional.of(I18n.get("El documento firmado no es igual al documento original"));
        }


        //Validación de la nueva firma

        if (resultadosFirma.size() == 0) {
            return Optional.of(I18n.get("El documento no se ha firmado"));
        }

        ResultadoFirma resultadoFirmaNueva = resultadosFirma.get(0);
        if (resultadoFirmaNueva.isCorrecta() == false) {
            return Optional.of(I18n.get("La firma no es correcta. Hay un error en ella"));
        }
        if (resultadoFirmaNueva.isCubreDocumentoCompleto() == false) {
            return Optional.of(I18n.get("El documento se ha modificado después de firmarlo"));
        }
        if (resultadoFirmaNueva.getDatosCertificado().isValidoEnListaCertificadosConfiables() == false) {
            return Optional.of(I18n.get("La firma no es valida según la lista de certificados aceptados por la aplicación"));
        }
        if (resultadoFirmaNueva.getDatosCertificado().isSelloTiempo() == true) {
            return Optional.of(I18n.get("La firma no puede ser un sello de tiempo"));
        }
        if (Objects.equals(resultadoFirmaNueva.getDatosCertificado().getDNI(), dni) == false) {
            return Optional.of(I18n.get("Se debe firmar con el DNI/NIE '%s' sin embargo se ha firmado con '%s'").formatted(dni, resultadoFirmaNueva.getDatosCertificado().getDNI()));
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

        return Optional.of(I18n.get("Falta la firma %s en el documento").formatted(resultadoFirma.getDatosCertificado().getCnSubject()));
    }

    private static Map<String, String> getStringMap(Map<String, Object> result) {
        return result.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() instanceof Boolean valor ? (valor ? "Yes" : "Off") : Convert.objectToUserString(entry.getValue())
                ));
    }





}
