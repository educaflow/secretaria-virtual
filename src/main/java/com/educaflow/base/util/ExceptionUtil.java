package com.educaflow.base.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

    public static String getTraceAsString(Throwable excepcion) {
        StringWriter sw = new StringWriter();
        excepcion.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
