package com.educaflow.common.evaluator;


import java.util.List;
import java.util.Map;

public interface Evaluator {

    Map<String,Object> evaluate(List<String> expressions, Map<String,Object> context);

}
