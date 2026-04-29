package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.Listener;
import com.axelor.db.Model;

import java.util.List;

public class ListenerImpl implements Listener {

    private final List<String> log;
    private int procesados = 0;
    private int saltados = 0;

    ListenerImpl(List<String> log) {
        this.log = log;
    }

    @Override
    public void imported(Model bean) {
        if (bean == null) {
            saltados++;
        } else {
            procesados++;
        }
    }

    @Override
    public void imported(Integer total, Integer success) {
        int fallidos = total - success;
        log.add(String.format("Procesados: %d | Ya existían: %d | Errores: %d | Total filas: %d",
            procesados, saltados, fallidos, total));
    }

    @Override
    public void handle(Model bean, Exception e) {
        log.add("Error en fila [" + bean + "]: " + e.getMessage());
    }
}