package com.educaflow.subsystem.importacion.adapters;

import com.axelor.data.adapter.Adapter;
import com.educaflow.base.util.DniUtil;
import org.w3c.dom.Node;

import java.util.Map;

public class DniAdapter extends Adapter {


    @Override
    public Object adapt(Object value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }

        String strValue;
        if (value instanceof Node) {
            strValue = ((Node) value).getTextContent();
        } else if (value instanceof String) {
            strValue = (String) value;
        } else {
            throw new IllegalArgumentException("Value must be a String or an XML Node (DNI)");
        }

        return DniUtil.clean(strValue);
    }

}
