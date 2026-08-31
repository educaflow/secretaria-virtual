package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;

import java.time.LocalDate;


/**
 * Rellena los datos iniciales de un expediente de JustificacionFaltaProfesoradoV1 recién creado.
 *
 * <p>El evento inicial es del <b>tipo de expediente</b>, no de una fase: se dispara cuando todavía
 * no hay estado del que partir. Por eso hay exactamente uno por tipo, aquí en la raíz de la
 * versión, y no uno por fase.
 *
 * <p>Qué campos hay que rellenar depende del tipo de expediente: {@code Tramitador} no impone
 * ninguno. Si los documentos de entrada de este tipo se firman, MUST dejar
 * {@code dniFirmaDocumentoEntrada} con un DNI válido.
 */
public class InitialEventManagerImpl implements InitialEventManager<JustificacionFaltaProfesoradoV1> {

    @Override
    public void triggerInitialEvent(JustificacionFaltaProfesoradoV1 justificacionFaltaProfesorado, EventContext eventContext) throws BusinessException {
        justificacionFaltaProfesorado.setAnyo(LocalDate.now().getYear());
        Persona persona=new Persona();
        persona.setNombre(justificacionFaltaProfesorado.getUsuarioRegistrador().getNombre());
        persona.setApellidos(justificacionFaltaProfesorado.getUsuarioRegistrador().getApellidos());
        persona.setDni(justificacionFaltaProfesorado.getUsuarioRegistrador().getDni());
        justificacionFaltaProfesorado.setPersonaInteresada(persona);
        justificacionFaltaProfesorado.setPersonaSolicitante(persona);
        justificacionFaltaProfesorado.setDniFirmaDocumentoEntrada(persona.getDni());
    }

}
