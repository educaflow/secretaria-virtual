package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.Model;

public interface ModelLoader {
    Model getModel(Class<? extends Model> classModel, Long id);
}
