package com.educaflow.subsystem.common.importar;

import com.axelor.data.adapter.Adapter;

import java.util.Map;
import java.util.regex.Pattern;

public class DocumentAdapter extends Adapter {

    // 0XXXXXXXXL (DNI con cero inicial)
    private static final Pattern PATTERN_DNI_PREFIXED =
            Pattern.compile("^0[0-9]{8}[A-Z]$");

    // 0YXXXXXXXL (NIE con cero antes de la letra)
    private static final Pattern PATTERN_NIE_PREFIXED =
            Pattern.compile("^0[XYZ][0-9]{7}[A-Z]$");

    // Y0XXXXXXXL (NIE con cero después de la letra)
    private static final Pattern PATTERN_NIE_INTERNAL_ZERO =
            Pattern.compile("^[XYZ]0[0-9]{7}[A-Z]$");


    @Override
    public Object adapt(Object value, Map<String, Object> context) {
        if (!(value instanceof String)) {
            return value;
        }

        String val = ((String) value).toUpperCase().trim();

        // Caso: 0XXXXXXXXL -> XXXXXXXXL
        if (PATTERN_DNI_PREFIXED.matcher(val).matches()) {
            return val.substring(1);
        }

        // Caso: 0YXXXXXXXL -> YXXXXXXXL
        if (PATTERN_NIE_PREFIXED.matcher(val).matches()) {
            return val.substring(1);
        }

        // Caso: Y0XXXXXXXL -> YXXXXXXXL
        if (PATTERN_NIE_INTERNAL_ZERO.matcher(val).matches()) {
            return val.charAt(0) + val.substring(2);
        }

        return val;
    }

}
