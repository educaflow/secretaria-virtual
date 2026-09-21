package com.educaflow.subsystem.tramitador.tramitacion.eventmanager;

import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.expedientes.db.Expediente;

/**
 * Rellena los datos iniciales de un expediente recién creado y decide el estado en el que nace.
 *
 * <p>El evento inicial es del <b>tipo de expediente</b>, no de una fase: se dispara cuando todavía
 * no hay estado del que partir, así que no hay ninguna fase a la que pertenezca. Por eso está
 * separado de {@link PhaseEventManager} —que sí es uno por fase— y por eso su implementación es
 * exactamente una por tipo de expediente, en el paquete base de la versión, junto al
 * {@code TipoExpedienteInstance.xml}.
 *
 * <p>{@code ExpedienteLocator} resuelve la implementación por convención de nombre
 * ({@code <basePackageName>.InitialEventManagerImpl}), igual que hace con las clases de cada fase.
 *
 * @param <T> la entidad del tipo de expediente.
 */
public interface InitialEventManager<T extends Expediente> {

    /**
     * Rellena los datos iniciales del expediente recién creado y fija su estado inicial.
     *
     * <p>MUST llamar a {@code initialEventContext.updateState(...)}: el estado inicial no se declara en el
     * {@code TipoExpedienteInstance.xml}, porque puede depender de cómo se crea el expediente, que es
     * lo que trae el {@code ContextoTramitacion} de {@code initialEventContext} (por ejemplo, si se registra una solicitud entregada en
     * papel o si se presenta en representación). Si no lo fija, {@code Tramitador} aborta el alta.
     *
     * <p>Qué campos hay que rellenar depende del tipo de expediente: {@code Tramitador} no impone
     * ninguno.
     */
    void triggerInitialEvent(InitialEventContext<T> initialEventContext) throws BusinessException;

}
