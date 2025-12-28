package com.educaflow.apps.expedientes.common.tramitador;


import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.educaflow.apps.expedientes.common.*;
import com.educaflow.apps.expedientes.common.annotations.BeanValidationRulesForStateAndEvent;
import com.educaflow.apps.expedientes.db.Expediente;
import com.educaflow.apps.expedientes.db.ExpedienteHistorialEstados;
import com.educaflow.apps.expedientes.db.TipoExpediente;
import com.educaflow.apps.expedientes.db.repo.NumeradorRepository;
import com.educaflow.common.mapper.BeanMapperModel;
import com.educaflow.common.util.ReflectionUtil;
import com.educaflow.common.util.TextUtil;
import com.educaflow.common.validation.engine.*;
import com.educaflow.common.validation.messages.BusinessException;
import com.educaflow.common.validation.messages.BusinessMessages;
import com.google.common.base.CaseFormat;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class Tramitador {

    @Inject
    NumeradorRepository numeradorRepository;



    public Expediente triggerInitialEvent(TipoExpediente tipoExpediente,  EventContext eventContext) throws BusinessException {
        try {
            EventManager eventManager = tipoExpediente.getEventManager();
            JpaRepository<Expediente> expedienteRepository = JpaRepository.of(eventManager.getModelClass());
            Enum initialEvent = getInitialState(eventManager.getStateClass());

            Expediente expediente = (Expediente) eventManager.getModelClass().getDeclaredConstructor().newInstance();
            expediente.setTipoExpediente(tipoExpediente);
            expediente.setCentroReceptor(eventContext.getCentro());
            updateName(expediente);
            updateNumeroExpediente(expediente);

            eventManager.triggerInitialEvent(expediente, eventContext);

            expediente.updateState(initialEvent);
            addHistorialEstado(expediente, null);

            eventManager.onEnterState(expediente, eventContext);

            expedienteRepository.save(expediente);

            return expediente;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void triggerEvent(Expediente expediente, String eventName,  Map<String, Object> requestData, EventContext eventContext ) throws BusinessException {
        EventManager eventManager= expediente.getTipoExpediente().getEventManager();
        Expediente expedienteOriginal=(Expediente) BeanMapperModel.getEntityCloned(expediente.getClass(), expediente);
        StateEventValidator stateEventValidator =expediente.getTipoExpediente().getStateEventValidator();
        JpaRepository<Expediente> expedienteRepository = JpaRepository.of(eventManager.getModelClass());
        StateEnum stateEnum = new StateEnum(ReflectionUtil.getEnumConstant(eventManager.getStateClass(), expediente.getCodeState()));


        if ((stateEnum.getEvents().contains(eventName) == false)) {
            throw new RuntimeException("El evento '" + eventName + "' no es válido para el estado '" + expediente.getCodeState() + "'");
        }

        if (((eventName.equals(CommonEvent.DELETE.name())) == false)) {
            BeanValidationRules beanValidationRules = getBeansValidationRules(stateEventValidator, expediente.getCodeState(), eventName);
            Map<String, Object> allowProperties = AllowPropertiesFactory.getAllowProperties(beanValidationRules.getFieldValidationRules());
            BeanMapperModel.copyMapToEntity(expediente.getClass(), requestData, expediente, allowProperties);


            ValidatorEngine validatorEngine = new ValidatorEngine();
            BusinessMessages businessMessages = validatorEngine.validate(expediente, beanValidationRules);
            if (businessMessages.isValid() == false) {
                JPA.em().detach(expediente);
                throw new BusinessException(businessMessages);
            }
        }

        try {
            eventManager.triggerEvent(eventName, expediente, expedienteOriginal, eventContext);
        } catch (BusinessException ex) {
            JPA.em().detach(expediente);
            throw ex;
        }

        if (eventName.equals(CommonEvent.DELETE.name())) {
            expedienteRepository.remove(expediente);
        } else {
            addHistorialEstado(expediente, eventName);
            eventManager.onEnterState(expediente, eventContext);

            expedienteRepository.save(expediente);
        }


    }

    public BusinessMessages validateChild(Expediente expediente, Model bean, Class<? extends Model> beanClass, String validateProperty, Map<String,Object> requestData) {

        String methodName="get"+TextUtil.toFirstsLetterToUpperCase(validateProperty);

        TipoExpediente tipoExpediente=expediente.getTipoExpediente();

        StateEventValidator stateEventValidator = tipoExpediente.getStateEventValidator();
        List<BeanValidationRules> beansValidationRules = getBeansValidationRules(stateEventValidator, expediente.getCodeState());
        List<FieldValidationRules> fieldsValidationRules=getFieldsValidationRules(beansValidationRules,methodName);

        Map<String,Object> allowProperties = AllowPropertiesFactory.getAllowProperties(fieldsValidationRules);
        BeanMapperModel.copyMapToEntity(beanClass, requestData, bean, allowProperties);

        ValidatorEngine validatorEngine = new ValidatorEngine();
        BusinessMessages businessMessages = validatorEngine.validate(bean, fieldsValidationRules);
        JPA.em().detach(bean);

        return businessMessages;
    }



    /*******************************************************************/
    /********************** Funciones de Negocio  **********************/
    /*******************************************************************/

    private static void addHistorialEstado(Expediente expediente, String eventName) {
        ExpedienteHistorialEstados historialEstado = new ExpedienteHistorialEstados();
        historialEstado.setCodeState(expediente.getCodeState());
        historialEstado.setNameState(TextUtil.humanize(expediente.getCodeState()));
        historialEstado.setCodeEvent((eventName != null) ? eventName : "");
        historialEstado.setNameEvent((eventName != null) ? TextUtil.humanize(eventName) : "");
        historialEstado.setFecha(LocalDateTime.now());
        expediente.addHistorialEstado(historialEstado);
    }

    private void updateName(Expediente expediente) {
        expediente.setName(expediente.getTipoExpediente().getName());
    }

    private void updateNumeroExpediente(Expediente expediente) {
        int anyoActual = LocalDate.now().getYear();
        String codigoCentro = expediente.getCentroReceptor().getCode();
        long numeroExpedienteSinAnyo = numeradorRepository.getSiguienteNumeroExpediente(codigoCentro, String.valueOf(anyoActual));
        String numeroExpediente = String.format("%05d", numeroExpedienteSinAnyo) + "/" + anyoActual;
        expediente.setNumeroExpediente(numeroExpediente);
    }

    private void assertValidState(Expediente expediente, Class<? extends Enum> enumClass) {
        String stateCode = expediente.getCodeState();
        boolean isValid = Arrays.stream(enumClass.getEnumConstants()).anyMatch(enumConstant -> stateCode.equals(enumConstant.name()));

        if (isValid == false) {
            throw new IllegalArgumentException("Invalid state code '" + stateCode + "'  " + enumClass.getSimpleName());
        }
    }


    private Enum<?> getInitialState(Class<? extends Enum> stateEnumClass) {
        Enum<?> initialState = null;
        Enum<?>[] states = stateEnumClass.getEnumConstants();

        for (Enum<?> state : states) {
            StateEnum stateEnum = new StateEnum(state);

            if (stateEnum.isInitial()) {
                if (initialState != null) {
                    throw new RuntimeException("Hay más de un estado inicial en la clase: " + stateEnumClass.getName());
                }
                initialState = state;
            }
        }

        if (initialState == null) {
            throw new RuntimeException("No se ha encontrado el estado inicial en la clase: " + stateEnumClass.getName());
        }

        return initialState;
    }




    /*********************************************************************/
    /********************** Funciones de Validación **********************/
    /*********************************************************************/

    private BeanValidationRules getBeansValidationRules(StateEventValidator stateEventValidator, String state, String eventName) {
        try {
            String methodName = "getForState" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, state) + "InEvent" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, eventName);
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
            String methodName = "getForState" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, state) + "InEvent";


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
