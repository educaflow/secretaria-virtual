package com.educaflow.subsystem.expedientes.services.eventmanager;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.expedientes.db.Expediente;

/**
 * Rellena los datos iniciales de un expediente recién creado.
 *
 * <p>El evento inicial es del <b>tipo de expediente</b>, no de una fase: se dispara cuando todavía
 * no hay estado del que partir, así que no hay ninguna fase a la que pertenezca. Por eso está
 * separado de {@link PhaseEventManager} —que sí es uno por fase— y por eso su implementación es
 * exactamente una por tipo de expediente, en el paquete base de la versión, junto al
 * {@code TipoExpedienteInstance.xml}.
 *
 * <p>Antes esto era un método concreto de {@code PhaseEventManager} que lanzaba
 * {@code UnsupportedOperationException} en las fases que no contenían el estado inicial: una
 * comprobación en runtime de algo que el tipo puede expresar. Ahora la fase que no participa en la
 * creación del expediente simplemente no tiene el método.
 *
 * <p>{@code ExpedienteLocator} resuelve la implementación por convención de nombre
 * ({@code <basePackageName>.InitialEventManagerImpl}), igual que hace con las clases de cada fase.
 *
 * @param <T> la entidad del tipo de expediente.
 */
public interface InitialEventManager<T extends Expediente> {

    /**
     * Rellena los datos iniciales del expediente recién creado.
     *
     * <p>Qué campos hay que rellenar depende del tipo de expediente: {@code Tramitador} no impone
     * ninguno. En particular, los tipos cuyos documentos de entrada se firman <b>MUST</b> dejar
     * {@code dniFirmaDocumentoEntrada} con un DNI válido, porque es el que usa
     * {@code FirmaController.firmarDocumentoEntrada}, que lo comprueba en el momento de firmar.
     */
    void triggerInitialEvent(T expediente, EventContext eventContext) throws BusinessException;

}
