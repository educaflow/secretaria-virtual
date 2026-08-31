package com.educaflow.tiposexpedientes.initialeventmanager;

import com.educaflow.common.buildtools.files.initialeventmanagerfile.InitialEventManagerFile;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.tiposexpedientes.support.Bytecode;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cada tipo de expediente tiene <b>exactamente un</b> {@code InitialEventManagerImpl}, en la raíz de
 * su carpeta de versión, que rellena los datos iniciales del expediente recién creado.
 *
 * <p>El evento inicial es del <b>tipo de expediente</b>, no de una fase: se dispara cuando todavía
 * no hay estado del que partir, así que no hay ninguna fase a la que pertenezca. Por eso su clase
 * cuelga del paquete base —donde está el {@code TipoExpedienteInstance.xml}— y no de una subcarpeta
 * de fase, y por eso es una sola por tipo mientras que del {@code PhaseEventManager} hay una por fase
 * ({@code PhaseEventManagerTest}).
 *
 * <p>Es una regla que ninguna otra cubre y que nada más detecta a tiempo: {@code Tramitador}
 * resuelve la clase por reflexión, así que olvidarla no es un error de compilación sino una
 * excepción al crear el primer expediente. La mitad de "firma incorrecta" es la más traicionera: una
 * clase que implementa la interfaz siempre tiene el método —lo exige el compilador—, pero si el
 * parámetro no es la entidad del tipo, la clase no compila contra {@code InitialEventManager<T>} y
 * eso sí lo caza el compilador; lo que queda por vigilar aquí es que la clase <b>exista</b> y que
 * implemente la interfaz del tipo correcto.
 *
 * <p>El fichero lo genera {@code ./gradlew -q CreateFilesTask -Ptipo=<carpeta del tipo>}, entre los
 * ficheros de la raíz de la versión.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class InitialEventManagerTest {

    private static final String FQCN_EVENT_CONTEXT =
            "com.educaflow.subsystem.expedientes.services.eventmanager.EventContext";
    private static final String FQCN_INITIAL_EVENT_MANAGER = InitialEventManager.class.getName();
    private static final String VOID = "void";

    @Test
    @DisplayName("I1: cada tipo de expediente tiene su InitialEventManagerImpl, que implementa InitialEventManager")
    void i1_existeLaClaseDelInitialEventManager() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            String fqcn = tipo.getFqcnInitialEventManager();
            Optional<JavaClass> clase = Bytecode.clase(fqcn);

            if (clase.isEmpty()) {
                violaciones.add(new Violacion(tipo.getCode(), fichero(tipo),
                        "no existe la clase " + fqcn + ", que es la que atiende el evento inicial del tipo."
                        + " Genérala con: ./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/"
                        + TiposExpediente.rel(TiposExpediente.carpeta(tipo))));
            } else if (!clase.get().isAssignableTo(FQCN_INITIAL_EVENT_MANAGER)) {
                violaciones.add(new Violacion(tipo.getCode(), fichero(tipo),
                        "la clase " + fqcn + " no implementa " + FQCN_INITIAL_EVENT_MANAGER));
            }
        }

        Violacion.assertNone("[I1] Cada tipo de expediente debe tener exactamente un InitialEventManagerImpl en la"
                + " raíz de su carpeta de versión, que implemente " + FQCN_INITIAL_EVENT_MANAGER + ".", violaciones);
    }

    @Test
    @DisplayName("I2: el InitialEventManagerImpl declara exactamente un triggerInitialEvent(<Entidad>, EventContext)")
    void i2_declaraElTriggerInitialEventConLaFirmaCorrecta() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<JavaClass> clase = Bytecode.clase(tipo.getFqcnInitialEventManager());
            if (clase.isEmpty()) {
                continue; // ya lo reporta I1; no repetimos el mismo fallo en cada regla
            }
            String modelo = initialEventManagerFile(tipo).getModelFQCN();
            String nombreMetodo = InitialEventManagerFile.getMethodNameTriggerInitialEvent();
            String esperada = Bytecode.firmaEsperada(nombreMetodo, VOID, modelo, FQCN_EVENT_CONTEXT);

            List<JavaMethod> homonimos = Bytecode.metodosLlamados(clase.get(), nombreMetodo);
            List<JavaMethod> correctos = homonimos.stream()
                    .filter(m -> Bytecode.tieneFirma(m, VOID, modelo, FQCN_EVENT_CONTEXT))
                    .toList();

            if (correctos.size() == 1) {
                continue;
            }

            StringBuilder detalle = new StringBuilder();
            if (homonimos.isEmpty()) {
                detalle.append("falta el método ").append(nombreMetodo);
            } else if (correctos.isEmpty()) {
                detalle.append("el método ").append(nombreMetodo).append(" existe pero no vale:");
                for (JavaMethod metodo : homonimos) {
                    detalle.append("\n      declarado: ").append(Bytecode.firma(metodo));
                }
                detalle.append("\n      se esperaba: ").append(esperada)
                       .append(" (el parámetro es la entidad del tipo de expediente, la misma que"
                               + " parametriza InitialEventManager<T>)");
            } else {
                detalle.append("hay ").append(correctos.size()).append(" métodos ").append(nombreMetodo)
                       .append(" válidos; debe haber exactamente uno");
            }

            violaciones.add(new Violacion(tipo.getCode(), fichero(tipo), detalle.toString()));
        }

        Violacion.assertNone("[I2] El InitialEventManagerImpl de cada tipo de expediente debe declarar exactamente un"
                + " " + InitialEventManagerFile.getMethodNameTriggerInitialEvent()
                + "(<Entidad>, EventContext) void.", violaciones);
    }

    private static InitialEventManagerFile initialEventManagerFile(TipoExpedienteInstanceFile tipo) {
        return new InitialEventManagerFile(path(tipo), tipo);
    }

    private static Path path(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.carpeta(tipo).resolve(tipo.getInitialEventManagerClassName() + ".java");
    }

    private static String fichero(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.rel(path(tipo));
    }
}
