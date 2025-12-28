package com.educaflow.common.pdf;

import com.educaflow.common.evaluator.Evaluator;
import com.educaflow.common.evaluator.impl.EvaluatorImplGroovy;
import com.educaflow.common.util.Convert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentoPdfUtil {


    public static DocumentoPdf generate(DocumentoPdf documentoPdf,Map<String,Object> context) {

        List<String> expressions= documentoPdf.getNombreCamposFormulario();

        Evaluator evaluator= new EvaluatorImplGroovy();
        Map<String,Object> result=evaluator.evaluate(expressions, context);

        Map<String, String> resultString = getStringMap(result);


        DocumentoPdf documentoPdfDatos= documentoPdf.setValorCamposFormularioAndFlatten(resultString);

        return documentoPdfDatos;

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
