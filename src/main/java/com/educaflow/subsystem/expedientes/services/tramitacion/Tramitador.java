package com.educaflow.subsystem.expedientes.services.tramitacion;


import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.modelservice.AllowProperties;
import com.educaflow.base.util.*;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteUtil;
import com.educaflow.subsystem.expedientes.services.internal.ExpedienteLocator;
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import org.apache.shiro.authz.UnauthorizedException;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.mapper.BeanMapperModel;
import com.educaflow.base.infrastructure.validation.engine.*;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.services.validation.StateEventValidator;
import com.google.common.base.CaseFormat;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Tramitador {

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    ExpedienteLocator expedienteLocator;


    public Expediente triggerInitialEvent(TipoExpediente tipoExpediente,  EventContext eventContext) throws BusinessException {
        try {
            //El evento inicial es del tipo de expediente, no de una fase: cuando se dispara todavía
            //no hay estado del que partir. Lo atiende el InitialEventManager, que es uno solo por
            //tipo.
            InitialEventManager initialEventManager = expedienteLocator.getInitialEventManager(tipoExpediente);
            Class<? extends Expediente> modelClass = expedienteLocator.getModelClass(tipoExpediente);
            State initialState = tipoExpediente.getTipoExpedienteStates().getInitialState();

            //Todavía no hay expediente contra el que preguntar, así que el perfil del estado inicial
            //se contrasta con los Ace que el usuario tiene sobre el trámite.
            checkPerfilDelEstado(initialState, perfilesUsuarioService.getPerfilesSobreTramite(
                    tipoExpediente.getTramite(), SecurityUtil.getUser()));

            Expediente expediente = modelClass.getDeclaredConstructor().newInstance();
            expediente.setTipoExpediente(tipoExpediente);
            expediente.setCentro(eventContext.getCentro());
            expediente.setUsuarioRegistrador(SecurityUtil.getUser());
            updateName(expediente);
            updateNumeroExpediente(expediente);

            initialEventManager.triggerInitialEvent(expediente, eventContext);

            ExpedienteUtil.updateState(expediente, initialState);
            addHistorialEstado(expediente, null, eventContext);

            //El onEnter sí es de una fase: la del estado en el que acaba de entrar el expediente.
            expedienteLocator.getPhaseEventManager(tipoExpediente, expediente.getCodePhase())
                    .onEnterState(expediente, eventContext);

            //JpaRepository.of(...).save(entidad) es literalmente esto (JpaRepository.save delega en
            //JPA.save), y así el alta no necesita un repositorio tipado con la clase concreta.
            JPA.save(expediente);

            return expediente;
        } catch (UnauthorizedException e) {
            //Sin envolver, para que llegue arriba como error de acceso y no como error genérico.
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void triggerEvent(Expediente expediente, String eventName,  Map<String, Object> requestData, EventContext eventContext ) throws BusinessException {
        BeanMapperModel beanMapperModel=new BeanMapperModel();
        TipoExpediente tipoExpediente=expediente.getTipoExpediente();
        //El evento lo atiende la fase en la que está el expediente ahora mismo. Si la transición
        //acaba llevándolo a otra fase, el onEnter de destino ya no es de esta clase: por eso se
        //vuelve a resolver más abajo.
        String codePhaseOrigen=expediente.getCodePhase();
        PhaseEventManager phaseEventManager=expedienteLocator.getPhaseEventManager(tipoExpediente, codePhaseOrigen);
        Expediente expedienteOriginal=(Expediente) beanMapperModel.getEntityCloned(expediente.getClass(), expediente);
        StateEventValidator stateEventValidator =expedienteLocator.getStateEventValidator(tipoExpediente, codePhaseOrigen);
        JpaRepository<Expediente> expedienteRepository = JpaRepository.of(phaseEventManager.getModelClass());
        State state = tipoExpediente.getTipoExpedienteStates()
                .getState(codePhaseOrigen, expediente.getCodeState())
                .orElseThrow(() -> new RuntimeException("El estado '" + codePhaseOrigen + "/"
                        + expediente.getCodeState() + "' no existe en el tipo de expediente "
                        + tipoExpediente.getCode() + "."));

        //Quién puede disparar el evento: el actor del estado actual. Sin esto, cualquiera con acceso
        //de lectura al expediente podría disparar los eventos de cualquier perfil — por ejemplo, el
        //creador autoaprobándose el expediente con el evento del RESPONSABLE.
        checkPerfilDelEstado(state, perfilesUsuarioService.getPerfilesSobreExpediente(
                expediente, SecurityUtil.getUser()));

        if (state.getEvents().contains(eventName) == false) {
            throw new RuntimeException("El evento '" + eventName + "' no es válido para el estado '"
                    + codePhaseOrigen + "/" + expediente.getCodeState() + "'");
        }

        if (((eventName.equals(CommonEvent.DELETE.name())) == false)) {
            BeanValidationRules beanValidationRules = getBeansValidationRules(stateEventValidator, expediente.getCodeState(), eventName);
            AllowProperties allowProperties = AllowProperties.createAllowProperties(AllowPropertiesFactory.getAllowProperties(beanValidationRules.getFieldValidationRules()));
            beanMapperModel.copyMapToEntity(expediente.getClass(), requestData, expediente, allowProperties);


            ValidatorEngine validatorEngine = new ValidatorEngine();
            BusinessMessages businessMessages = validatorEngine.validate(expediente, beanValidationRules);
            if (businessMessages.isValid() == false) {
                JPA.em().detach(expediente);
                throw new BusinessException(businessMessages);
            }
        }

        try {
            phaseEventManager.triggerEvent(eventName, expediente, expedienteOriginal, eventContext);
        } catch (BusinessException ex) {
            JPA.em().detach(expediente);
            throw ex;
        }

        if (eventName.equals(CommonEvent.DELETE.name())) {
            expedienteRepository.remove(expediente);
        } else {
            addHistorialEstado(expediente, eventName, eventContext);

            //El onEnter es del estado AL QUE se ha llegado, y la transición ha podido cruzar de
            //fase: hay que volver a resolver el PhaseEventManager con el estado nuevo, porque el método
            //onEnter<Estado> del destino solo existe en la clase de su propia fase.
            PhaseEventManager phaseEventManagerDestino=expedienteLocator.getPhaseEventManager(tipoExpediente, expediente.getCodePhase());
            phaseEventManagerDestino.onEnterState(expediente, eventContext);

            expedienteRepository.save(expediente);
        }


    }

    public BusinessMessages validateChild(Expediente expediente, Model bean, Class<? extends Model> beanClass, String validateProperty, Map<String,Object> requestData) {
        BeanMapperModel beanMapperModel=new BeanMapperModel();
        String methodName="get"+TextUtil.toFirstsLetterToUpperCase(validateProperty);

        TipoExpediente tipoExpediente=expediente.getTipoExpediente();

        StateEventValidator stateEventValidator = expedienteLocator.getStateEventValidator(tipoExpediente, expediente.getCodePhase());
        List<BeanValidationRules> beansValidationRules = getBeansValidationRules(stateEventValidator, expediente.getCodeState());
        List<FieldValidationRules> fieldsValidationRules=getFieldsValidationRules(beansValidationRules,methodName);

        AllowProperties allowProperties = AllowProperties.createAllowProperties(AllowPropertiesFactory.getAllowProperties(fieldsValidationRules));
        beanMapperModel.copyMapToEntity(beanClass, requestData, bean, allowProperties);

        ValidatorEngine validatorEngine = new ValidatorEngine();
        BusinessMessages businessMessages = validatorEngine.validate(bean, fieldsValidationRules);
        JPA.em().detach(bean);

        return businessMessages;
    }



    /*******************************************************************/
    /********************** Funciones de Negocio  **********************/
    /*******************************************************************/

    private static void addHistorialEstado(Expediente expediente, String eventName, EventContext eventContext) {
        HistorialEstado historialEstado = new HistorialEstado();
        //Corre siempre después de ExpedienteUtil.updateState, así que la pareja (fase, estado) y su
        //texto se copian del propio expediente sin volver a resolver el State.
        historialEstado.setCodePhase(expediente.getCodePhase());
        historialEstado.setNamePhase(expediente.getNamePhase());
        historialEstado.setCodeState(expediente.getCodeState());
        historialEstado.setNameState(expediente.getNameState());
        historialEstado.setCodeEvent((eventName != null) ? eventName : "");
        historialEstado.setNameEvent((eventName != null) ? TextUtil.humanize(eventName) : "");
        historialEstado.setFecha(LocalDateTime.now());


        if (eventContext.getRegistroEntrada()!=null) {
            historialEstado.setRegistroEntrada(eventContext.getRegistroEntrada());
        }

        if (eventContext.getRegistroSalida()!=null) {
            historialEstado.setRegistroSalida(eventContext.getRegistroSalida());
        }



        expediente.addHistorialEstado(historialEstado);
    }


    /**
     * Comprueba que el usuario tenga el perfil que el estado declara para su actor.
     *
     * <p>Es <b>pertenencia a un conjunto</b>, no derivación: un usuario puede tener varios perfiles a
     * la vez sobre el mismo expediente. El {@code _profile} que envía el cliente no participa — solo
     * elige qué vista se pinta.
     *
     * <p>Un estado <b>sin perfil</b> no exige ninguno: hay estados que no declaran actor (típicamente
     * los finales) y ahí la única barrera es el acceso al propio expediente.
     */
    private static void checkPerfilDelEstado(State state, Set<String> perfilesDelUsuario) {
        Profile profileDelEstado = state.getProfile();
        if (profileDelEstado == null) {
            return;
        }

        //El administrador ve y tramita expedientes de cualquier centro y no tiene filas Ace.
        if (SecurityUtil.isAdmin(SecurityUtil.getUser())) {
            return;
        }

        if (perfilesDelUsuario.contains(profileDelEstado.name()) == false) {
            throw new UnauthorizedException("El usuario no tiene el perfil '" + profileDelEstado.name()
                    + "', que es el que atiende el estado '" + state.getPhase().getCode() + "/"
                    + state.getCode() + "'.");
        }
    }


    private void updateName(Expediente expediente) {
        expediente.setName(expediente.getTipoExpediente().getName());
    }

    private void updateNumeroExpediente(Expediente expediente) {
        int anyoActual = LocalDate.now().getYear();
        String codigoCentro = expediente.getCentro().getCode();
        long numeroExpedienteSinAnyo = numeradorRepository.getSiguienteNumeroExpediente(codigoCentro, String.valueOf(anyoActual));
        String numeroExpediente = String.format("%05d", numeroExpedienteSinAnyo) + "/" + anyoActual;
        expediente.setNumeroExpediente(numeroExpediente);
    }





    /*********************************************************************/
    /********************** Funciones de Validación **********************/
    /*********************************************************************/

    /**
     * El trozo de nombre de método que aporta un estado: sale de su código dentro de la fase,
     * porque el validator ya está en el paquete de su fase y solo atiende los estados de esa fase.
     * Debe casar con {@code StateEventValidatorFile.getMethodNameBeanValidationRules} de los
     * build-tools, que es quien genera esos métodos.
     */
    private static String getEstadoUpperCamelCase(String codeState) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, codeState);
    }

    private BeanValidationRules getBeansValidationRules(StateEventValidator stateEventValidator, String state, String eventName) {
        try {
            String methodName = "getForState" + getEstadoUpperCamelCase(state) + "InEvent" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, eventName);
            Method method = ReflectionUtil.getMethod(stateEventValidator.getClass(), methodName, BeanValidationRules.class, BeanValidationRulesForStateAndEvent.class, new Class<?>[]{});
            if (method == null) {
                throw new RuntimeException("No se ha encontrado el método: " + methodName + " en la clase: " + stateEventValidator.getClass().getName());
            }
            Object result = method.invoke(stateEventValidator);
            if (result == null) {
                throw new RuntimeException("No se han encontrado las reglas de validación para el estado: " + state + " y el evento: " + eventName);
            }


            BeanValidationRules beanValidationRules = (BeanValidationRules) result;

            return beanValidationRules;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener las reglas de validación para el estado: " + state + " y el evento: " + eventName + " en " + stateEventValidator.getClass().getName(), ex);
        }
    }




    private List<BeanValidationRules> getBeansValidationRules(StateEventValidator stateEventValidator, String state) {
        try {
            List<BeanValidationRules> beansValidationRules=new ArrayList<>();
            String methodName = "getForState" + getEstadoUpperCamelCase(state) + "InEvent";


            for (Method method : stateEventValidator.getClass().getDeclaredMethods()) {
                if (method.getName().startsWith(methodName)) {
                    if (method.isAnnotationPresent(BeanValidationRulesForStateAndEvent.class)) {
                        BeanValidationRules beanValidationRules=(BeanValidationRules)method.invoke(stateEventValidator);
                        if (beanValidationRules == null) {
                            throw new RuntimeException("El método retorno null:" + method.getName());
                        }
                        beansValidationRules.add(beanValidationRules);
                    }
                }
            }

            if (beansValidationRules.isEmpty()) {
                throw new RuntimeException("No se han encontrado las reglas de validación para el estado: " + state);
            }

            return beansValidationRules;

        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener las reglas de validación para el estado: " + state + " en " + stateEventValidator.getClass().getName(), ex);
        }
    }

    private List<FieldValidationRules> getFieldsValidationRules(List<BeanValidationRules> beansValidationRules,String methodName) {
        List<FieldValidationRules> fieldsValidationRules=new ArrayList<>();
        for(BeanValidationRules rules:beansValidationRules) {
            for(FieldValidationRules fieldValidationRules:rules.getFieldValidationRules()) {
                if (fieldValidationRules.getMethodField().getName().equals(methodName)) {
                    for(ValidationRule validationRule:fieldValidationRules.getValidationRules()) {
                        if ((validationRule instanceof FieldValidationRules)) {
                            fieldsValidationRules.add((FieldValidationRules)validationRule);
                        }
                    }
                }
            }
        }

        return fieldsValidationRules;
    }

    /*******************************************************************/
    /********************** Funciones de Utilidad **********************/
    /*******************************************************************/



}
