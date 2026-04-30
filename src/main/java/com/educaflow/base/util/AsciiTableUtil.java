package com.educaflow.base.util;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;

import java.util.ArrayList;
import java.util.List;

public class AsciiTableUtil {
    public static  String renderTable(String tableName,Exception ex) {
        List<List<Object>> rows=new ArrayList<>();

        for(String trace:getStackTrace(ex,0)) {
            List<Object> row=new ArrayList<>();
            row.add(trace);
            rows.add(row);
        }

        return renderTable(tableName,List.of("Error"),rows);
    }

    public static  String renderTable(String tableName,List<String> heads,List<List<Object>> rows) {

        List<String> titulo=new ArrayList<>();
        if ((heads!=null) && (heads.isEmpty()==false)) {
            for(int i=0;i<heads.size()-1;i++) {
                titulo.add(null);
            }
        }

        titulo.add(tableName);

        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow(titulo.toArray());
        if ((heads!=null) && (heads.isEmpty()==false)) {
            at.addRule();
            at.addRow(heads.toArray());
        }
        at.addRule();

        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (row.get(i) == null) {
                    row.set(i, "__null__");
                }
            }
        }

        if (rows.size()>0) {
            for(List<Object> row:rows) {
                at.addRow(row.toArray());
            }
            at.addRule();
        }
        at.getRenderer().setCWC(new CWC_LongestLine());

        return at.render();
    }

    private static List<String> getStackTrace(Throwable ex,int deep) {
        String tabulador="\u00B7".repeat(4);

        List<String> stackTrace=new ArrayList<>();

        if (deep==0) {
            stackTrace.add(ex.getLocalizedMessage());
        }

        for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
            stackTrace.add(tabulador.repeat(deep)+stackTraceElement.toString());
        }

        if (ex.getCause()!=null) {
            stackTrace.add(tabulador.repeat(deep)+"Caused by:"+ex.getCause().getLocalizedMessage());
            stackTrace.addAll(getStackTrace(ex.getCause(),deep+1));
        }

        return stackTrace;
    }

}
