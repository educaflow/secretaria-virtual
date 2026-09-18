package com.educaflow.subsystem.expedientes.services;

import com.axelor.db.Model;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.ContextoTramitacion;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.tramitacion.internal.ExpedienteUtil;
import com.educaflow.subsystem.expedientes.tramitacion.core.Tramitador;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.util.Map;

/**
 * La capa de negocio de los expedientes: <b>autoriza</b> lo que se pide y se lo encarga al {@code
 * Tramitador}, que es el motor y no comprueba permisos.
 *
 * <p>Es lo único que llama {@code ExpedienteController}, que queda solo con la petición y la
 * respuesta. Todo lo que el cliente envía (el contexto del alta, el id del expediente, el nombre del
 * evento) pasa por aquí antes de llegar al motor.
 *
 * <p><b>Ojo con el paquete:</b> vive en {@code …expedientes.services} (plural). {@code
 * ModelServiceFactory} de Axelor resuelve el {@code ModelService} de la entidad {@code
 * …expedientes.db.Expediente} buscando {@code …expedientes.service.ExpedienteService} (singular), así
 * que hoy no colisionan. Mover esta clase a un paquete {@code service} haría que Axelor la tomara por
 * el {@code ModelService} de los expedientes y reventara por no implementarlo.
 */
public class ExpedienteService {

    @Inject
    Tramitador tramitador;

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;


    /**
     * Da de alta el expediente que describe el contexto de tramitación.
     *
     * <p>El centro, el papel y la representación llegan del cliente, así que se autorizan con la misma
     * regla que ha pintado la pantalla: lo que ella no ofreció, aquí no se acepta.
     */
    @Transactional
    public Expediente crear(ContextoTramitacion contextoTramitacion) throws BusinessException {
        return tramitador.triggerInitialEvent(contextoTramitacion);
    }

    /**
     * Dispara un evento sobre el expediente.
     *
     * <p>Quién puede dispararlo es el actor del estado actual. Sin esto, cualquiera con acceso de
     * lectura al expediente podría disparar los eventos de cualquier perfil — por ejemplo, el creador
     * autoaprobándose el expediente con el evento del TRAMITADOR.
     */
    @Transactional
    public void triggerEvent(Expediente expediente, String eventName, Map<String, Object> requestData, EventContext eventContext) throws BusinessException {
        tramitador.triggerEvent(expediente, eventName, requestData, eventContext);
    }

    /**
     * El expediente con ese id, comprobando que el usuario pueda leerlo: el id lo envía el cliente y
     * {@code find} no filtra filas.
     */
    public Expediente getExpediente(long idExpediente) {
        return ExpedienteUtil.getExpedienteFromIdExpediente(idExpediente);
    }

    public BusinessMessages validateChild(Expediente expediente, Model bean, Class<? extends Model> beanClass, String validateProperty, Map<String, Object> requestData) {
        return tramitador.validateChild(expediente, bean, beanClass, validateProperty, requestData);
    }

}
