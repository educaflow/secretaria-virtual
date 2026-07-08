package com.educaflow.subsystem.correos.module;

import com.axelor.app.AppSettings;
import com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor;
import jakarta.inject.Provider;

public class CorreoAsyncExecutorProvider implements Provider<CorreoAsyncExecutor> {

    @Override
    public CorreoAsyncExecutor get() {
        int tamanoPool = AppSettings.get().getInt("mail.send.pool-size", 2);
        return new CorreoAsyncExecutor(tamanoPool);
    }

}
