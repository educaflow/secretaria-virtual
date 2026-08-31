package com.educaflow.subsystem.expedientes.services.internal;

import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.services.eventmanager.TipoExpedienteStates;
import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Localiza las clases de un tipo de expediente: la máquina de estados tipada del tipo entero (la
 * clase {@code States} que genera el build), su {@code InitialEventManager} y, por cada fase, su
 * {@code PhaseEventManager} y su {@code StateEventValidator}.
 *
 * <p>Desde que los tipos de expediente están divididos en fases hay un {@code PhaseEventManagerImpl} y un
 * {@code StateEventValidatorImpl} por <b>fase</b>, cada uno en el paquete de su fase. La resolución
 * es <b>en función de la fase</b>, que viaja en su propia columna {@code codePhase} del expediente,
 * sin ningún tratamiento especial para las transiciones que cruzan fases. Quien atiende un evento es
 * la clase de la fase del estado <b>desde el que se dispara</b>; quien atiende el {@code onEnter} es
 * la de la fase del estado <b>al que se llega</b>, que puede ser otra.
 *
 * <pre>
 *   tipoExpediente.basePackageName + "." + fase.toLowerCase() + ".PhaseEventManagerImpl"
 *   tipoExpediente.basePackageName + ".States"
 *   tipoExpediente.basePackageName + ".InitialEventManagerImpl"
 * </pre>
 *
 * <p>El {@code InitialEventManager} es el único que no se resuelve por fase: el evento inicial se
 * dispara cuando todavía no hay estado del que partir, así que es del tipo de expediente entero y
 * hay exactamente uno, en el paquete base.
 *
 * <p>El paquete base se guarda en la base de datos en vez de derivarse del código porque es el
 * único dato que el runtime no puede deducir; el data-init lo reescribe en cada arranque, así que
 * mover la carpeta de un tipo de expediente se corrige solo. Los nombres de las clases son fijos e
 * iguales en todas las fases: lo que las distingue es el paquete.
 *
 * <p>Las vistas <b>no</b> pasan por aquí: aunque los {@code views.xml} estén repartidos por fase,
 * los nombres de vista son globales y llevan la fase y el estado como segmentos, así que
 * {@code PhaseEventManager.getViewName} los compone sin necesidad de localizador.
 *
 * <p>Es un <b>bean inyectable</b> ({@code @Singleton}), no una clase de estáticos: quien lo usa lo
 * declara como dependencia y en test se puede sustituir. Las clases que resuelve por reflexión se
 * instancian con el {@link Injector} inyectado, que es la dependencia que antes estaba escondida
 * detrás de {@code Beans.get}. La única excepción es la entidad {@code TipoExpediente}, que llega
 * aquí con {@code Beans.get(ExpedienteLocator.class)} desde su {@code <extra-code-model>} porque las
 * entidades JPA no las construye Guice.
 *
 * @author logongas
 */
@Singleton
public class ExpedienteLocator {

    private static final String CLASE_PHASE_EVENT_MANAGER = "PhaseEventManagerImpl";
    private static final String CLASE_INITIAL_EVENT_MANAGER = "InitialEventManagerImpl";
    private static final String CLASE_STATE_EVENT_VALIDATOR = "StateEventValidatorImpl";
    private static final String CLASE_STATES = "States";

    /**
     * El INSTANCE de la clase States de cada tipo, por basePackageName. Se resuelve en cada
     * tramitación. Es de instancia y no estático porque la clase es {@code @Singleton}: hay una
     * caché por aplicación, igual que antes, pero sin estado global.
     */
    private final ConcurrentHashMap<String, TipoExpedienteStates> statesPorPaquete = new ConcurrentHashMap<>();

    private final Injector injector;

    @Inject
    public ExpedienteLocator(Injector injector) {
        this.injector = injector;
    }

    /**
     * La máquina de estados tipada del tipo de expediente. Es a lo que delega el
     * {@code getTipoExpedienteStates()} de la entidad.
     *
     * <p>La reflexión acaba aquí: quien recibe el {@link TipoExpedienteStates} hace llamadas
     * normales, sin {@code Method}.
     */
    public TipoExpedienteStates getTipoExpedienteStates(TipoExpediente tipoExpediente) {
        String basePackageName = getBasePackageName(tipoExpediente);

        return statesPorPaquete.computeIfAbsent(basePackageName, ExpedienteLocator::cargarStates);
    }

    private static TipoExpedienteStates cargarStates(String basePackageName) {
        String fqcn = basePackageName + "." + CLASE_STATES;
        try {
            Object instance = Class.forName(fqcn).getField("INSTANCE").get(null);

            return (TipoExpedienteStates) instance;
        } catch (Exception ex) {
            throw new RuntimeException("No se ha podido obtener el INSTANCE de " + fqcn + ", que es la clase"
                    + " que el build genera desde el TipoExpedienteInstance.xml del tipo. Si la clase no"
                    + " existe, es que GenerateStatesTask no ha corrido o que el basePackageName del tipo"
                    + " no corresponde a ninguna carpeta de versión.", ex);
        }
    }

    /** El {@code PhaseEventManager} de la fase. */
    public PhaseEventManager getPhaseEventManager(TipoExpediente tipoExpediente, String phaseCode) {
        return injector.getInstance(getClasePhaseEventManager(tipoExpediente, phaseCode));
    }

    /** El {@code StateEventValidator} de la fase. */
    public StateEventValidator getStateEventValidator(TipoExpediente tipoExpediente, String phaseCode) {
        return injector.getInstance(getClaseStateEventValidator(tipoExpediente, phaseCode));
    }

    public Class<PhaseEventManager> getClasePhaseEventManager(TipoExpediente tipoExpediente, String phaseCode) {
        return cargar(tipoExpediente, phaseCode, CLASE_PHASE_EVENT_MANAGER, PhaseEventManager.class);
    }

    public Class<StateEventValidator> getClaseStateEventValidator(TipoExpediente tipoExpediente, String phaseCode) {
        return cargar(tipoExpediente, phaseCode, CLASE_STATE_EVENT_VALIDATOR, StateEventValidator.class);
    }

    /**
     * El {@code InitialEventManager} del tipo de expediente, que es el que rellena los datos
     * iniciales del expediente recién creado.
     */
    public InitialEventManager getInitialEventManager(TipoExpediente tipoExpediente) {
        return injector.getInstance(getClaseInitialEventManager(tipoExpediente));
    }

    public Class<InitialEventManager> getClaseInitialEventManager(TipoExpediente tipoExpediente) {
        String fqcn = getBasePackageName(tipoExpediente) + "." + CLASE_INITIAL_EVENT_MANAGER;

        return cargar(fqcn, InitialEventManager.class,
                "que es la que debería atender el evento inicial del tipo de expediente "
                + tipoExpediente.getCode() + ". Hay exactamente una por tipo de expediente, en la"
                + " raíz de la carpeta de versión (el evento inicial no es de ninguna fase).");
    }

    /**
     * La clase de la entidad del tipo de expediente.
     *
     * <p>Sale del parámetro de tipo con el que el {@code InitialEventManagerImpl} del tipo implementa
     * {@link InitialEventManager} ({@code implements InitialEventManager<PruebaV1>}). Se resuelve por
     * ahí y no por convención de nombre como el resto de clases de aquí porque la entidad no vive en
     * el paquete de la versión sino en el de expedientes, así que su nombre no se puede componer; y
     * se pregunta al {@code InitialEventManager} y no a un {@code PhaseEventManager} porque la
     * entidad es del tipo entero, no de una fase, igual que él.
     */
    public Class<? extends Expediente> getModelClass(TipoExpediente tipoExpediente) {
        Class<InitialEventManager> claseInitialEventManager = getClaseInitialEventManager(tipoExpediente);

        for (Type interfazImplementado : claseInitialEventManager.getGenericInterfaces()) {
            if (interfazImplementado instanceof ParameterizedType interfazParametrizado
                    && interfazParametrizado.getRawType() == InitialEventManager.class
                    && interfazParametrizado.getActualTypeArguments()[0] instanceof Class<?> entidad
                    && Expediente.class.isAssignableFrom(entidad)) {

                return (Class<? extends Expediente>) entidad;
            }
        }

        throw new RuntimeException("La clase " + claseInitialEventManager.getName() + " no declara con qué"
                + " entidad implementa InitialEventManager, así que no se puede saber cuál es la entidad"
                + " del tipo de expediente " + tipoExpediente.getCode() + ". Debe implementarlo con la"
                + " subclase de Expediente del tipo (implements InitialEventManager<MiExpedienteV1>),"
                + " nunca en crudo: ese parámetro de tipo es el único sitio donde el tipo de expediente"
                + " declara cuál es su entidad.");
    }

    private <T> Class<T> cargar(TipoExpediente tipoExpediente, String phaseCode, String simpleClassName, Class<T> tipoEsperado) {
        String fqcn = getFqcn(tipoExpediente, phaseCode, simpleClassName);

        return cargar(fqcn, tipoEsperado,
                "que es la que debería atender la fase '" + phaseCode + "' del tipo de expediente "
                + tipoExpediente.getCode() + ". Cada fase tiene la suya en el paquete de la fase.");
    }

    /**
     * Carga la clase y comprueba que es del tipo esperado. El {@code queEs} explica, en el mensaje
     * de error, qué papel juega la clase que falta y dónde debería estar: es diagnóstico de
     * despliegue, no mensaje de usuario, así que va sin {@code I18n.get} como el resto de esta clase.
     */
    private static <T> Class<T> cargar(String fqcn, Class<T> tipoEsperado, String queEs) {
        Class<?> clase;
        try {
            clase = Class.forName(fqcn);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("No existe la clase " + fqcn + ", " + queEs, ex);
        }

        if (tipoEsperado.isAssignableFrom(clase) == false) {
            throw new RuntimeException("La clase " + fqcn + " no es un " + tipoEsperado.getName() + ".");
        }

        return (Class<T>) clase;
    }

    private static String getFqcn(TipoExpediente tipoExpediente, String phaseCode, String simpleClassName) {
        // Sin este guard, un codePhase nulo da un NPE pelado y uno en blanco compone el FQCN
        // "paquete..PhaseEventManagerImpl", que revienta con un ClassNotFoundException igual de
        // desconcertante. Es diagnóstico de despliegue, no mensaje de usuario: sin I18n.get, como
        // los demás mensajes de esta clase.
        if ((phaseCode == null) || (phaseCode.isBlank())) {
            throw new RuntimeException("El expediente no tiene fase (codePhase) para el tipo de"
                    + " expediente " + tipoExpediente.getCode() + ". La pareja (codePhase, codeState)"
                    + " la escribe ExpedienteUtil.updateState; si está vacía, la fila se ha creado o"
                    + " modificado por fuera de la tramitación.");
        }

        // Locale.ROOT a propósito: con la JVM en locale turco una fase con I minusculizaría a 'ı' y
        // el Class.forName fallaría con un ClassNotFoundException desconcertante sobre un paquete
        // que sí existe.
        return getBasePackageName(tipoExpediente) + "." + phaseCode.toLowerCase(Locale.ROOT) + "." + simpleClassName;
    }

    private static String getBasePackageName(TipoExpediente tipoExpediente) {
        String basePackageName = tipoExpediente.getBasePackageName();

        if ((basePackageName == null) || (basePackageName.isBlank())) {
            throw new RuntimeException("No existe el basePackageName para el tipo de expediente: "
                    + tipoExpediente.getName() + ". Lo rellena el data-init de los tipos de expediente"
                    + " en cada arranque; si está vacío es que ese data-init no se ha cargado.");
        }

        return basePackageName;
    }

}
