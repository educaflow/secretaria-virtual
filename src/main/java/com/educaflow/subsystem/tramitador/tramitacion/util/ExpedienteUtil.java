package com.educaflow.subsystem.tramitador.tramitacion.util;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaSecurity;
import com.axelor.inject.Beans;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.TipoExpedienteStates;

import java.time.LocalDateTime;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.tramitador.tramitacion.internal.ExpedienteLocator;

public class ExpedienteUtil {

    /**
     * La máquina de estados del tipo de expediente.
     *
     * <p>Es el punto de entrada <b>polimórfico</b>: el que se usa cuando el tipo de expediente no se
     * conoce en compilación. Cuando sí se conoce, se usa directamente el {@code States.INSTANCE} de
     * su carpeta de versión.
     *
     * <p>Está aquí y no en un {@code <extra-code-model>} de la entidad {@code TipoExpediente} a
     * propósito: la entidad se genera en {@code subsystem.expedientes.db} y llamar desde ella al
     * localizador acoplaría el dominio al motor, que es justo la dirección prohibida.
     *
     * <p>{@link ExpedienteLocator} es un bean inyectable, pero esto es un método estático de utilidad
     * al que se llega desde varios sitios, así que se pide con {@code Beans.get} igual que en
     * {@code getClaseConcreta}.
     */
    public static TipoExpedienteStates getTipoExpedienteStates(TipoExpediente tipoExpediente) {
        return Beans.get(ExpedienteLocator.class).getTipoExpedienteStates(tipoExpediente);
    }

    /**
     * Lleva el expediente a un estado. Es el <b>único</b> sitio que escribe la pareja
     * {@code (codePhase, codeState)} y el único que decide si el estado no ha cambiado.
     */
    // Los State son singletons (enums) y la barrera de abajo es por identidad A PROPÓSITO.
    @SuppressWarnings("ReferenceEquality")
    public static void updateState(Expediente expediente, State state) {
        if (state == null) {
            throw new IllegalArgumentException("El state no puede ser nulo.");
        }

        String phaseCode = state.getPhase().getCode();
        String stateCode = state.getCode();

        // Barrera cross-tipo: los State generados son singletons, así que el estado de ESTE tipo de
        // expediente con estos códigos debe ser el mismo objeto que el recibido. Sin genéricos en
        // EventContext, un State de otro tipo compila, y el caso es realista: crear una versión
        // nueva es duplicar la carpeta de la anterior, y las dos tienen una clase llamada States.
        State propio = getTipoExpedienteStates(expediente.getTipoExpediente())
                .getState(phaseCode, stateCode).orElse(null);
        if (propio != state) {
            throw new IllegalArgumentException("El estado " + phaseCode + "/" + stateCode
                    + " no es del tipo de expediente " + expediente.getTipoExpediente().getCode()
                    + ": o no existe en su máquina de estados o el State es de la clase States de"
                    + " otro tipo (típicamente un import sin actualizar al duplicar una versión).");
        }

        if (stateCode.equals(expediente.getCodeState())
                && phaseCode.equals(expediente.getCodePhase())) {
            return;
        }

        expediente.setCodePhase(phaseCode);
        expediente.setNamePhase(state.getPhase().getName());
        expediente.setCodeState(stateCode);
        expediente.setNameState(state.getName());
        expediente.setPerfilEstado(state.getProfile());
        expediente.setFechaUltimoEstado(LocalDateTime.now(Convert.defaultZoneId));
        expediente.setAbierto(state.isFinal() == false);
    }

    /**
     * El estado en el que está el expediente ahora mismo, resuelto contra la máquina de estados de su
     * tipo. Es el sitio único: lo piden tanto el servicio, para autorizar el evento, como el motor,
     * para saber qué eventos admite el estado.
     */
    public static State getState(Expediente expediente) {
        TipoExpediente tipoExpediente = expediente.getTipoExpediente();

        return getTipoExpedienteStates(tipoExpediente)
                .getState(expediente.getCodePhase(), expediente.getCodeState())
                .orElseThrow(() -> new RuntimeException("El estado '" + expediente.getCodePhase() + "/"
                        + expediente.getCodeState() + "' no existe en el tipo de expediente "
                        + tipoExpediente.getCode() + "."));
    }

    public static Expediente getExpedienteFromIdExpediente(long idExpediente) {
        Class<? extends Expediente> claseConcreta = getClaseConcreta(idExpediente);

        // El idExpediente lo envía el cliente en el JSON (TramitadorController.viewExpediente,
        // triggerEvent y validateChild; FirmaClienteController) y JpaRepository.find delega en em.find sin
        // ningún filtro de fila, así que sin esta comprobación cualquier usuario autenticado puede
        // leer cualquier expediente por id.
        // La clase contra la que se comprueba sale de la BD, NUNCA del _model que envía el cliente,
        // que es precisamente el vector de bypass. Y MUST NOT filtrarse por self.centro: la
        // autorización de Expediente es por perfiles AceProfile* (auth-expedientes.xml), no por centro.
        // El administrador no queda bloqueado: AuthSecurity.getUser() devuelve null para admin.
        Beans.get(JpaSecurity.class).check(JpaSecurity.CAN_READ, claseConcreta, idExpediente);

        Expediente expediente = JpaRepository.of(claseConcreta).find(idExpediente);
        if (expediente == null) {
            throw new RuntimeException("No existe el expediente con idExpediente: " + idExpediente);
        }

        return expediente;
    }

    /**
     * Obtiene la clase concreta de un expediente en función de su id.
     * Se usa este método porque de otra forma se trabajaría con {@link Expediente} y no con la
     * entidad concreta del tipo de expediente.
     *
     * @param idExpediente
     * @return
     */
    private static Class<? extends Expediente> getClaseConcreta(long idExpediente) {
        JpaRepository<Expediente> onlyExpedienteRepository = JpaRepository.of(Expediente.class);
        Expediente expediente = onlyExpedienteRepository.find(idExpediente);
        if (expediente == null) {
            throw new RuntimeException("No existe el expediente con idExpediente: " + idExpediente);
        }
        //La clase del modelo es la misma en todas las fases, así que aquí no hace falta resolver por estado.
        //ExpedienteLocator es un bean inyectable, pero esto es un método estático de utilidad al que
        //se llega desde varios sitios, así que se pide con Beans.get igual que el JpaSecurity de arriba.
        Class<? extends Expediente> claseConcreta = Beans.get(ExpedienteLocator.class).getModelClass(expediente.getTipoExpediente());
        JPA.em().detach(expediente);

        return claseConcreta;
    }

}
