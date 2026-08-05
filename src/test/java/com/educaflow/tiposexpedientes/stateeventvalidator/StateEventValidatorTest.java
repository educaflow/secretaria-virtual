package com.educaflow.tiposexpedientes.stateeventvalidator;

import com.educaflow.common.buildtools.common.TextUtil;
import com.educaflow.common.buildtools.files.stateeventvalidator.StateEventValidatorFile;
import com.educaflow.common.buildtools.files.tipoexpediente.State;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.subsystem.expedientes.services.tramitacion.CommonEvent;
import com.educaflow.subsystem.expedientes.services.validation.BeanValidationRulesForStateAndEvent;
import com.educaflow.tiposexpedientes.support.Bytecode;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * El {@code StateEventValidator} de cada tipo de expediente tiene un método de reglas por cada
 * pareja (estado, evento) declarada en su {@code TipoExpedienteInstance.xml}, ni más ni menos.
 *
 * <p>Esto <b>nunca se llegó a comprobar</b>: el {@code check()} de {@code createfiles} estaba
 * íntegramente comentado porque Spoon solo parsea Java y este fichero es Kotlin. Al leer bytecode
 * sí se puede, y merece la pena: {@code Tramitador.getBeansValidationRules} construye el nombre del
 * método a mano y lo busca por reflexión, así que un método que falte no es un detalle de estilo,
 * es un {@code RuntimeException} en la cara del usuario en cuanto pulse ese botón.
 *
 * <p><b>El evento {@code DELETE} es la excepción</b>: {@code Tramitador} lo excluye de la validación
 * y borra sin copiar campos, así que su método de reglas nunca se invoca y solo podría estar vacío.
 * Ni el esqueleto lo genera ya ni ningún tipo lo tiene. V1 no lo exige; V2 tampoco lo denuncia si
 * apareciera, porque su pareja (estado, evento) sí está declarada en el XML.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class StateEventValidatorTest {

    private static final String FQCN_BEAN_VALIDATION_RULES =
            "com.educaflow.base.infrastructure.validation.engine.BeanValidationRules";
    private static final String FQCN_STATE_EVENT_VALIDATOR =
            "com.educaflow.subsystem.expedientes.services.validation.StateEventValidator";

    /**
     * «Delete»: el evento {@code DELETE} en la forma en que aparece dentro de los nombres de método.
     * Se deriva de {@link CommonEvent}, que es lo que mira {@code Tramitador} para saltarse la
     * validación, para que la excepción de la regla y el runtime no puedan separarse.
     */
    private static final String EVENTO_DELETE_UPPER_CAMEL =
            TextUtil.getUpperCamelCase(List.of(CommonEvent.DELETE.name())).get(0);

    // -----------------------------------------------------------------------------------------
    // V0
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("V0: cada tipo de expediente tiene compilada la clase de su StateEventValidator, que implementa StateEventValidator")
    void v0_existeLaClaseDelValidator() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            String fqcn = tipo.getFqcnStateEventValidator();
            Optional<JavaClass> clase = Bytecode.clase(fqcn);

            if (clase.isEmpty()) {
                violaciones.add(new Violacion(tipo.getCode(), ficheroValidator(tipo),
                        "no existe la clase " + fqcn + " (¿falta compilar, o el fqcnStateEventValidator del XML apunta a otro sitio?)"));
            } else if (!clase.get().isAssignableTo(FQCN_STATE_EVENT_VALIDATOR)) {
                violaciones.add(new Violacion(tipo.getCode(), ficheroValidator(tipo),
                        "la clase " + fqcn + " no implementa " + FQCN_STATE_EVENT_VALIDATOR));
            }
        }

        Violacion.assertNone("[V0] La clase del StateEventValidator de cada tipo de expediente debe existir e"
                + " implementar " + FQCN_STATE_EVENT_VALIDATOR + ".", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // V1 / V2
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("V1: por cada pareja (estado, evento) salvo las de DELETE hay un método getForState<Estado>InEvent<Evento> con @BeanValidationRulesForStateAndEvent")
    void v1_existeUnMetodoPorParejaEstadoEvento() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnStateEventValidator());
            if (clase.isEmpty()) {
                continue; // ya lo reporta V0
            }
            StateEventValidatorFile validatorFile = validatorFile(tipo);

            for (Map.Entry<String, Pareja> entrada : parejasObligatorias(tipo).entrySet()) {
                String nombreMetodo = entrada.getKey();
                Pareja pareja = entrada.getValue();
                String esperada = Bytecode.firmaEsperada(nombreMetodo, FQCN_BEAN_VALIDATION_RULES);

                List<JavaMethod> homonimos = Bytecode.metodosLlamados(clase.get(), nombreMetodo);
                List<JavaMethod> correctos = homonimos.stream()
                        .filter(m -> m.isAnnotatedWith(BeanValidationRulesForStateAndEvent.class))
                        .filter(m -> Bytecode.tieneFirma(m, FQCN_BEAN_VALIDATION_RULES))
                        .toList();

                if (correctos.size() == 1) {
                    continue;
                }

                String detalle;
                if (homonimos.isEmpty()) {
                    detalle = "falta el método " + nombreMetodo + " para " + pareja
                            + ". Sin él, disparar ese evento revienta en runtime con"
                            + " «No se ha encontrado el método: " + nombreMetodo + "»";
                } else if (correctos.isEmpty()) {
                    StringBuilder sb = new StringBuilder("el método " + nombreMetodo + " (" + pareja
                            + ") existe pero no vale:");
                    for (JavaMethod metodo : homonimos) {
                        sb.append("\n      declarado: ").append(Bytecode.firma(metodo));
                        if (!metodo.isAnnotatedWith(BeanValidationRulesForStateAndEvent.class)) {
                            sb.append("  (sin @").append(BeanValidationRulesForStateAndEvent.class.getSimpleName()).append(")");
                        }
                    }
                    sb.append("\n      se esperaba: ").append(esperada)
                      .append("  con @").append(BeanValidationRulesForStateAndEvent.class.getSimpleName());
                    detalle = sb.toString();
                } else {
                    detalle = "hay " + correctos.size() + " métodos " + nombreMetodo + " válidos para "
                            + pareja + "; debe haber exactamente uno";
                }

                violaciones.add(new Violacion(tipo.getCode(), ficheroValidator(tipo),
                        detalle + ".\n    Código esperado:\n"
                        + validatorFile.getSourceCodeBeanValidationRulesMethod(pareja.estado(), pareja.evento())));
            }
        }

        Violacion.assertNone("[V1] Por cada pareja (estado, evento) del TipoExpedienteInstance.xml, salvo las del"
                + " evento " + CommonEvent.DELETE.name() + " (que el runtime nunca valida), debe haber exactamente"
                + " un método getForState<Estado>InEvent<Evento> anotado @BeanValidationRulesForStateAndEvent,"
                + " sin parámetros y que devuelva BeanValidationRules.",
                violaciones);
    }

    @Test
    @DisplayName("V2: no sobra ningún método @BeanValidationRulesForStateAndEvent cuya pareja no esté declarada")
    void v2_noSobraNingunMetodoDeReglas() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnStateEventValidator());
            if (clase.isEmpty()) {
                continue;
            }

            Map<String, Pareja> esperadas = parejasDeclaradas(tipo);

            for (JavaMethod metodo : Bytecode.metodosAnotados(clase.get(), BeanValidationRulesForStateAndEvent.class)) {
                if (!esperadas.containsKey(metodo.getName())) {
                    violaciones.add(new Violacion(tipo.getCode(), ficheroValidator(tipo),
                            "sobra el método " + metodo.getName() + "(): su pareja (estado, evento) no está"
                            + " declarada en el TipoExpedienteInstance.xml. Parejas declaradas: "
                            + esperadas.values().stream().map(Pareja::toString).collect(Collectors.joining(", "))));
                }
            }
        }

        Violacion.assertNone("[V2] Todo método anotado @BeanValidationRulesForStateAndEvent debe corresponder a una"
                + " pareja (estado, evento) declarada en el TipoExpedienteInstance.xml (las de " + CommonEvent.DELETE.name()
                + " no son obligatorias, pero si están declaradas en el XML se toleran).", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /**
     * Nombre del método esperado -> descripción de su pareja, en formato {@code Estado/Evento} con
     * los nombres ya en UpperCamelCase (que es como se forman los nombres de método).
     *
     * <p>Se recorre estado a estado, no la lista global de eventos: un mismo evento en tres estados
     * son tres métodos distintos del validator, aunque en el EventManager sea un único trigger.
     */
    private static Map<String, Pareja> parejasDeclaradas(TipoExpedienteInstanceFile tipo) {
        Map<String, Pareja> parejas = new LinkedHashMap<>();
        for (State state : tipo.getStates()) {
            String estado = state.getNameUpperCamelCase();
            for (String evento : state.getEventsUpperCamelCase()) {
                parejas.put(StateEventValidatorFile.getMethodNameBeanValidationRules(estado, evento),
                        new Pareja(estado, evento));
            }
        }
        return parejas;
    }

    /**
     * Las parejas que <b>MUST</b> tener método: las declaradas menos las del evento {@code DELETE}.
     *
     * <p>{@code Tramitador} salta la validación cuando el evento es {@code DELETE} y borra sin copiar
     * campos, así que ese método nunca se invoca y no hay nada que pueda contener. Exigirlo solo
     * produciría un `rules { }` vacío en cada tipo de expediente.
     */
    private static Map<String, Pareja> parejasObligatorias(TipoExpedienteInstanceFile tipo) {
        Map<String, Pareja> parejas = new LinkedHashMap<>(parejasDeclaradas(tipo));
        parejas.values().removeIf(pareja -> pareja.evento().equals(EVENTO_DELETE_UPPER_CAMEL));
        return parejas;
    }

    /** Una pareja (estado, evento) del XML, con los nombres ya en UpperCamelCase. */
    private record Pareja(String estado, String evento) {

        @Override
        public String toString() {
            return estado + "/" + evento;
        }
    }

    private static StateEventValidatorFile validatorFile(TipoExpedienteInstanceFile tipo) {
        Path path = TiposExpediente.carpeta(tipo).resolve(tipo.getStateEventValidatorClassName() + ".kt");
        return new StateEventValidatorFile(path, tipo);
    }

    private static String ficheroValidator(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.rel(TiposExpediente.carpeta(tipo).resolve(tipo.getStateEventValidatorClassName() + ".kt"));
    }
}
