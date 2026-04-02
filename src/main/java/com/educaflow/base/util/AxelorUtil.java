package com.educaflow.base.util;

import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.views.AbstractView;

public class AxelorUtil {
    public static boolean existsView(String name, String type, String model) {
        AbstractView data = XMLViews.findView(name, type, model);
        if (data==null) {
            return false;
        } else {
            return true;
        }
    }
}
