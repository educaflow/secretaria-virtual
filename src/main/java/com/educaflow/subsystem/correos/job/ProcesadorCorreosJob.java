package com.educaflow.subsystem.correos.job;

import com.axelor.db.JPA;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.correos.db.TareaCorreo;
import com.educaflow.subsystem.correos.service.TareaCorreoService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProcesadorCorreosJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorCorreosJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            TareaCorreoService servicio =
                    (TareaCorreoService) Beans.get(ModelServiceFactory.class).resolve(TareaCorreo.class);
            List<TareaCorreo> pendientes = servicio.obtenerPendientes();
            for (TareaCorreo tarea : pendientes) {
                try {
                    JPA.runInTransaction(() -> servicio.procesarEnvio(tarea));
                } catch (Exception e) {
                    log.error("Error procesando TareaCorreo id={}", tarea.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error en el ciclo del ProcesadorCorreosJob", e);
            throw new JobExecutionException(e);
        }
    }
}
