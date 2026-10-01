package com.educaflow.secretariavirtual.module;

import com.axelor.app.AppSettings;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import jakarta.inject.Provider;

public class EjecutorAsincronoProvider implements Provider<EjecutorAsincrono> {

    @Override
    public EjecutorAsincrono get() {
        int tamanoPool = AppSettings.get().getInt("async.pool-size", 2);
        return new EjecutorAsincrono(tamanoPool);
    }

}
