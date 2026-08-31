package com.educaflow.tramites.prueba.v1;

import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.db.PruebaV1;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;


/**
 * Rellena los datos iniciales de un expediente de PruebaV1 recién creado.
 *
 * <p>El evento inicial es del <b>tipo de expediente</b>, no de una fase: se dispara cuando todavía
 * no hay estado del que partir. Por eso hay exactamente uno por tipo, aquí en la raíz de la
 * versión, y no uno por fase.
 *
 * <p>Qué campos hay que rellenar depende del tipo de expediente: {@code Tramitador} no impone
 * ninguno. Si los documentos de entrada de este tipo se firman, MUST dejar
 * {@code dniFirmaDocumentoEntrada} con un DNI válido.
 */
public class InitialEventManagerImpl implements InitialEventManager<PruebaV1> {

    @Override
    public void triggerInitialEvent(PruebaV1 pruebaV1, EventContext eventContext) throws BusinessException {


    }

}
