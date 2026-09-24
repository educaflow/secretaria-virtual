package com.educaflow.base.infrastructure.evaluator.impl;

import com.educaflow.base.infrastructure.evaluator.Evaluator;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluatorImplGroovy implements Evaluator {

    @Override
    public Map<String,Object> evaluate(List<String> expressions, Map<String,Object> context) {
        Map<String,Object> results = new HashMap<>();
        StringBuilder errores = new StringBuilder();

        GroovyShell shell = createShell(context);

        for (String expression : expressions) {



            try {
                if (expression == null || expression.trim().length() == 0) {
                    continue;
                }

                Object result = shell.evaluate(expression);
                results.put(expression, result);
                System.out.println("Evaluating expression: " + expression+" => " + result);
            } catch (Exception e) {
                errores.append(expression).append(":").append(e.getMessage()).append("\n");
            }

        }

        if (errores.toString().length() > 0) {
            System.out.println(errores.toString());
        }

        return results;
    }

    @Override
    public Map<String, Object> evaluateStrict(List<String> expressions, Map<String, Object> context) {
        Map<String, Object> results = new HashMap<>();

        GroovyShell shell = createShell(context);

        for (String expression : expressions) {
            if (expression == null || expression.isBlank()) {
                continue;
            }
            try {
                results.put(expression, shell.evaluate(expression));
            } catch (Exception e) {
                throw new RuntimeException("Error evaluando la expresión '" + expression + "': " + e.getMessage(), e);
            }
        }

        return results;
    }

    private GroovyShell createShell(Map<String, Object> context) {
        Binding binding = new Binding();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        return new GroovyShell(binding);
    }

}
