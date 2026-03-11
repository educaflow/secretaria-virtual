package com.educaflow.base.infrastructure.importer.util;

import com.axelor.data.Listener;

public class ListenerUtil {

    public static Listener getListener() {
        return new CustomListener();
    }
}
