package com.educaflow.subsystem.tramitador.tramitacion.core;


import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.auth.db.User;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.educaflow.base.util.*;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.tramitador.tramitacion.internal.ExpedienteLocator;
import com.educaflow.subsystem.tramitador.tramitacion.util.ExpedienteUtil;
import com.educaflow.subsystem.tramitador.tramitacion.validation.BeanValidationRulesForStateAndEvent;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.expedientes.db.TipoExpediente;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.*;
import org.apache.shiro.authz.UnauthorizedException;
import com.educaflow.base.infrastructure.numeradores.db.repo.NumeradorRepository;
import com.educaflow.base.infrastructure.mapper.BeanMapperModel;
import com.educaflow.base.infrastructure.validation.engine.*;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.tramitador.tramitacion.validation.StateEventValidator;
import com.google.common.base.CaseFormat;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.educaflow.base.util.Convert;


public class Tramitador {

    @Inject
    NumeradorRepository numeradorRepository;

    @Inject
    ExpedienteLocator expedienteLocator;

    @Inject
    ModelServiceFactory modelServiceFactory;


    public Expediente triggerInitialEvent(ContextoTramitacion contextoTramitacion) throws BusinessException {
        try {
            TipoExpediente tipoExpediente = contextoTramitacion.tramite().getDefaultTipoExpediente();
            Centro centro = contextoTramitacion.centro();
            boolean presentadoEnPapel = contextoTramitacion.presentadoEnPapel();
            boolean presentadoEnRepresentacion = contextoTramitacion.presentadoEnRepresentacion();


            InitialEventManager initialEventManager = expedienteLocator.getInitialEventManager(tipoExpediente);
            Class<? extends Expediente> modelClass = expedienteLocator.getModelClass(tipoExpediente);

            Expediente expediente = modelClass.getDeclaredConstructor().newInstance();
            expediente.setTipoExpediente(tipoExpediente);
            expediente.setCentro(centro);
            expediente.setUsuarioRegistrador(SecurityUtil.getUser());

            updatePersonas(expediente, presentadoEnPapel, presentadoEnRepresentacion);
            updateName(expediente);

            InitialEventContext initialEventContext = new InitialEventContext(expediente, contextoTramitacion);
            initialEventManager.triggerInitialEvent(initialEventContext);
            //Tras el evento inicial: su BusinessException es checked, la transacción hace commit y dejaría un hueco en la numeración.
            updateNumeroExpediente(expediente);


            EventContext eventContext = new EventContext(expediente, contextoTramitacion.profile(), modelServiceFactory);

            addHistorialEstado(expediente, null, eventContext);

            expedienteLocator.getPhaseEventManager(tipoExpediente, expediente.getCodePhase())
                    .onEnterState(expediente, eventContext);

            JPA.save(expediente);

            return expediente;
        } catch (UnauthorizedException | BusinessException e) {
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
        State state = ExpedienteUtil.getState(expediente);

        if (state.getEvents().contains(eventName) == false) {
            throw new RuntimeException("El evento '" + eventName + "' no es válido para el estado '"
                    + codePhaseOrigen + "/" + expediente.getCodeState() + "'");
        }

        if (!eventName.equals(CommonEvent.DELETE.name())) {
            BeanValidationRules beanValidationRules = getBeansValidationRules(stateEventValidator, expediente.getCodeState(), eventName);
            AllowProperties allowProperties = AllowProperties.createAllowProperties(AllowPropertiesFactory.getAllowProperties(beanValidationRules.getFieldValidationRules()));
            beanMapperModel.copyMapToEntity(expediente.getClass(), requestData, expediente, allowProperties);
            restaurarPersonas(expediente, expedienteOriginal);

            ValidatorEngine validatorEngine = new ValidatorEngine();
            BusinessMessages businessMessages = validatorEngine.validate(expediente, beanValidationRules);
            if (businessMessages.isValid() == false) {
                descartarCambios(expediente);
                throw new BusinessException(businessMessages);
            }
        }

        try {
            phaseEventManager.triggerEvent(eventName, expediente, expedienteOriginal, eventContext);
        } catch (BusinessException ex) {
            descartarCambios(expediente);
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

    /**
     * BusinessException es checked y el controller @Transactional la captura, así que sin el
     * rollback haría commit de la Persona y los hijos modificados (el detach no se propaga a
     * ellos). Quien capture la excepción ya no podrá guardar nada más en esta transacción.
     */
    private static void descartarCambios(Expediente expediente) {
        if (JPA.em().getTransaction().isActive()) {
            JPA.em().getTransaction().setRollbackOnly();
        }
        JPA.em().detach(expediente);
    }

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
        historialEstado.setFecha(LocalDateTime.now(Convert.defaultZoneId));


        if (eventContext.getRegistroEntrada()!=null) {
            historialEstado.setRegistroEntrada(eventContext.getRegistroEntrada());
        }

        if (eventContext.getRegistroSalida()!=null) {
            historialEstado.setRegistroSalida(eventContext.getRegistroSalida());
        }



        expediente.addHistorialEstado(historialEstado);
    }


    /**
     * Las personas nacen con los datos del usuario solo cuando es él quien presenta. En papel el usuario solo
     * registra lo que ha entregado otra persona, de la que el sistema no sabe nada: las dos nacen vacías y se
     * teclean (en papel y «para mí», solo la interesada, ver {@link #restaurarPersonas}).
     */
    private static void updatePersonas(Expediente expediente, boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {
        expediente.setPresentadoEnPapel(presentadoEnPapel);
        expediente.setPresentadoEnRepresentacion(presentadoEnRepresentacion);

        if (presentadoEnPapel) {
            expediente.setPersonaSolicitante(new Persona());
            expediente.setPersonaInteresada(new Persona());
        } else {
            User usuarioRegistrador = expediente.getUsuarioRegistrador();
            expediente.setPersonaSolicitante(crearPersona(usuarioRegistrador));
            expediente.setPersonaInteresada(presentadoEnRepresentacion ? new Persona() : crearPersona(usuarioRegistrador));
        }
    }

    private static Persona crearPersona(User user) {
        Persona persona = new Persona();
        persona.setNombre(user.getNombre());
        persona.setApellidos(user.getApellidos());
        persona.setDni(user.getDni());
        persona.setEmail(user.getEmail());

        return persona;
    }

    /**
     * Quién presenta y en nombre de quién lo fija el alta, no el cliente. Un tipo de expediente que
     * whitelistea la identificación de una persona para el modo en que se teclea la deja abierta también
     * en los demás modos, y el mapper admite además cambiar la referencia por otro id de Persona.
     *
     * <p>La identificación que se tecleó no se restaura:
     * <ul>
     *   <li>la del interesado, en representación o en papel;</li>
     *   <li>la del solicitante, en papel. En papel y «para mí» solicitante e interesado son la misma
     *       persona, así que solo se teclea el interesado y se copia en el solicitante.</li>
     * </ul>
     */
    private static void restaurarPersonas(Expediente expediente, Expediente expedienteOriginal) {
        exigeMismaPersona(expediente.getPersonaSolicitante(), expedienteOriginal.getPersonaSolicitante(), "personaSolicitante");
        exigeMismaPersona(expediente.getPersonaInteresada(), expedienteOriginal.getPersonaInteresada(), "personaInteresada");

        boolean presentadoEnPapel = Boolean.TRUE.equals(expedienteOriginal.getPresentadoEnPapel());
        boolean presentadoEnRepresentacion = Boolean.TRUE.equals(expedienteOriginal.getPresentadoEnRepresentacion());

        expediente.setPresentadoEnPapel(expedienteOriginal.getPresentadoEnPapel());
        expediente.setPresentadoEnRepresentacion(expedienteOriginal.getPresentadoEnRepresentacion());

        if (presentadoEnPapel == false) {
            copiarIdentificacion(expediente.getPersonaSolicitante(), expedienteOriginal.getPersonaSolicitante());
            if (presentadoEnRepresentacion == false) {
                copiarIdentificacion(expediente.getPersonaInteresada(), expedienteOriginal.getPersonaInteresada());
            }
        } else if (presentadoEnRepresentacion == false) {
            copiarIdentificacion(expediente.getPersonaSolicitante(), expediente.getPersonaInteresada());
        }
    }

    private static void exigeMismaPersona(Persona persona, Persona personaOriginal, String nombreCampo) {
        Long id = (persona == null) ? null : persona.getId();
        Long idOriginal = (personaOriginal == null) ? null : personaOriginal.getId();

        if (((persona == null) != (personaOriginal == null)) || (Objects.equals(id, idOriginal) == false)) {
            throw new IllegalStateException("La petición intenta cambiar la persona del campo '" + nombreCampo
                    + "' del expediente: de " + idOriginal + " a " + id + ".");
        }
    }

    private static void copiarIdentificacion(Persona destino, Persona origen) {
        if (destino == null) {
            return;
        }

        destino.setNombre(origen.getNombre());
        destino.setApellidos(origen.getApellidos());
        destino.setDni(origen.getDni());
    }

    private void updateName(Expediente expediente) {
        expediente.setName(expediente.getTipoExpediente().getName());
    }

    private void updateNumeroExpediente(Expediente expediente) {
        int anyoActual = LocalDate.now(Convert.defaultZoneId).getYear();
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
            Method method = ReflectionUtil.getMethod(stateEventValidator.getClass(), methodName, BeanValidationRules.class, BeanValidationRulesForStateAndEvent.class, new Class<?>[]{})
                    .orElseThrow(() -> new RuntimeException("No se ha encontrado el método: " + methodName + " en la clase: " + stateEventValidator.getClass().getName()));
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
        return beansValidationRules.stream()
                .flatMap(rules -> rules.getFieldValidationRules().stream())
                .filter(fieldValidationRules -> fieldValidationRules.getMethodField().getName().equals(methodName))
                .flatMap(fieldValidationRules -> fieldValidationRules.getValidationRules().stream())
                .filter(FieldValidationRules.class::isInstance)
                .map(FieldValidationRules.class::cast)
                .toList();
    }

    /*******************************************************************/
    /********************** Funciones de Utilidad **********************/
    /*******************************************************************/




}
