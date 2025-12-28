package com.educaflow.common.evaluator.impl;

import com.educaflow.common.evaluator.Evaluator;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluatorImplGroovy implements Evaluator {

    public Map<String,Object> evaluate(List<String> expressions, Map<String,Object> context) {
        Map<String,Object> results = new HashMap<>();
        StringBuilder errores = new StringBuilder();

        Binding binding = new Binding();
        for(Map.Entry<String,Object> entry : context.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        GroovyShell shell = new GroovyShell(binding);

        for (String expression : expressions) {
            System.out.println("Evaluating expression: " + expression);


            try {
                if (expression == null || expression.trim().length() == 0) {
                    continue;
                }

                Object result = shell.evaluate(expression);
                results.put(expression, result);
            } catch (Exception e) {
                errores.append(expression).append(":").append(e.getMessage()).append("\n");
            }

        }

        if (errores.toString().length() > 0) {
            System.out.println(errores.toString());
        }

        return results;
    }

}
