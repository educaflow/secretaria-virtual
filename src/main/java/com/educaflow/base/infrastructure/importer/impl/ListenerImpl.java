package com.educaflow.base.infrastructure.importer.impl;

import com.axelor.data.Listener;
import com.axelor.db.Model;

import java.util.List;

public class ListenerImpl implements Listener {

    private final List<String> log;

    ListenerImpl(List<String> log) {
        this.log = log;
    }

    @Override
    public void imported(Model bean) {
        //importLog.add("Registro importado: " + bean);
    }

    @Override
    public void imported(Integer total, Integer success) {
        log.add(String.format("Finalizado. Total: %d, Éxitos: %d", total, success));
    }

    @Override
    public void handle(Model bean, Exception e) {
        log.add("Error en registro [" + bean + "]: " + e.getMessage());
    }
}
