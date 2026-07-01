package com.educaflow.subsystem.smoketest.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.smoketest.db.SmokeTest;

public interface SmokeTestService extends ModelService<SmokeTest> {
    // Sin acciones propias adicionales.
    // El CRUD estándar (insert/update/remove) lo expone ModelService<T>.
    // Las sobrescrituras de validate*/allowProperties*/insert/update viven en la impl.
}
