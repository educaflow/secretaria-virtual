package com.educaflow.common.util;

import com.axelor.common.Inflector;

public class TextUtil {
    public static String humanize(String screamingSnakeCase) {
        return Inflector.getInstance().humanize(screamingSnakeCase);
    }


    public static String toFirstsLetterToUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
