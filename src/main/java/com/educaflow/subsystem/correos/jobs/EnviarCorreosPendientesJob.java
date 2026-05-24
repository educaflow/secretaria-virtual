package com.educaflow.subsystem.correos.jobs;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job de Quartz que dispara el envío de los correos PENDIENTE.
 * Registrado vía MetaSchedule con cron tomado de la propiedad 'correos.envio.cron' (E-UB-012).
 * No contiene lógica de negocio: resuelve el servicio y delega.
 */
public class EnviarCorreosPendientesJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(EnviarCorreosPendientesJob.class);

    /**
     * Resuelve CorreoService vía Beans.get(ModelServiceFactory.class).resolve(Correo.class)
     * y llama enviarCorreosPendientes(). Envuelve la llamada en try/catch para que un fallo
     * global se loguee (id/estado, sin datos sensibles) sin propagar y reventar el scheduler.
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = sanearParaLog(context.getJobDetail().getKey().getName());

        try {
            CorreoService correoService = (CorreoService) Beans.get(ModelServiceFactory.class).resolve(Correo.class);
            correoService.enviarCorreosPendientes();
            log.info("Job de envío de correos pendientes ejecutado correctamente: job={}", jobName);
        } catch (Exception e) {
            log.error("Fallo global en el job de envío de correos pendientes: job={}", jobName, e);
            throw new JobExecutionException(e);
        }
    }

    /**
     * Sanea un valor de origen externo antes de loguearlo eliminando saltos de línea y
     * retornos de carro para evitar inyección de líneas falsas en el log (log forging).
     */
    private static String sanearParaLog(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replace('\n', '_').replace('\r', '_');
    }
}
