package com.educaflow.base.infrastructure.importer.util;


import com.axelor.data.Listener;
import com.axelor.db.Model;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named
public class CustomListener implements Listener {

    private final Logger logger = LoggerFactory.getLogger(CustomListener.class);
    private final List<Model> records = new ArrayList<>();

    @Override
    public void imported(Model bean) {
        //logger.info("Bean importado: {}", bean);
        records.add(bean);
    }

    @Override
    public void imported(Integer total, Integer success) {
        logger.info("Importación finalizada. Total: {}, Éxitos: {}", total, success);
    }

    @Override
    public void handle(Model bean, Exception e) {
        logger.warn("Error al importar bean: {}. Error: {}", bean, e.getMessage());
    }
}
