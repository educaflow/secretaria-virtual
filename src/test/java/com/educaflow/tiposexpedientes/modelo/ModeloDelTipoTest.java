package com.educaflow.tiposexpedientes.modelo;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.tiposexpedientes.support.Bytecode;
import com.educaflow.tiposexpedientes.support.DomainsDelTipo;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Todas las clases de un tipo de expediente hablan de la <b>misma</b> entidad, y es la que declara
 * su {@code domains.xml}.
 *
 * <p>La entidad es del tipo entero, no de una fase: el {@code InitialEventManagerImpl} de la raíz de
 * la versión y el {@code PhaseEventManagerImpl} de cada fase la llevan cada uno en su parámetro de
 * tipo ({@code implements InitialEventManager<PruebaV1>},
 * {@code extends PhaseEventManager<PruebaV1>}), así que son varias declaraciones de un único hecho y
 * pueden divergir. Cuál es la buena lo dice el {@code domains.xml} del tipo, que es donde la entidad
 * se define: la <b>primera</b> {@code <entity>} del fichero.
 *
 * <p>No es una comprobación de estilo. El parámetro de tipo del {@code InitialEventManagerImpl} es
 * lo que lee {@code ExpedienteLocator.getModelClass} en runtime para saber qué entidad instanciar al
 * crear un expediente, así que si diverge del {@code domains.xml} o del que usan las fases, el fallo
 * no aparece al compilar sino al tramitar. La entidad no vive en el paquete de la versión sino en el
 * de expedientes, de modo que no hay convención de nombre que la ate: solo estas declaraciones.
 *
 * <p>Que cada clase exista y extienda o implemente lo que debe lo comprueban {@code E0} de
 * {@code PhaseEventManagerTest} e {@code I1} de {@code InitialEventManagerTest}; aquí se da por
 * hecho y solo se mira el parámetro de tipo, de manera que una clase que falte no se reporte dos
 * veces.
 *
 * <p><b>Estos tests se escriben A MANO.</b> No son una proyección de ningún catálogo markdown, al
 * contrario que {@code com.educaflow.architecture} y {@code com.educaflow.views}: este fichero es la
 * fuente de verdad y se edita directamente.
 */
class ModeloDelTipoTest {

    private static final String FQCN_INITIAL_EVENT_MANAGER =
            "com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager";
    private static final String FQCN_PHASE_EVENT_MANAGER =
            "com.educaflow.subsystem.expedientes.services.eventmanager.PhaseEventManager";

    @Test
    @DisplayName("M1: el InitialEventManagerImpl y todos los PhaseEventManagerImpl del tipo declaran como parámetro de tipo la entidad del domains.xml")
    void m1_todasLasClasesDelTipoDeclaranLaEntidadDelDomainsXml() {
        List<Violacion> violaciones = new ArrayList<>();

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            Optional<String> entidad = DomainsDelTipo.fqcnEntidad(tipo);

            if (entidad.isEmpty()) {
                violaciones.add(new Violacion(tipo.getCode(), DomainsDelTipo.fichero(tipo),
                        DomainsDelTipo.existe(tipo)
                                ? "no se puede saber cuál es la entidad del tipo: el " + DomainsDelTipo.NOMBRE_FICHERO
                                        + " no declara ninguna <entity> con name, o su <module> no tiene package"
                                : "no existe el " + DomainsDelTipo.NOMBRE_FICHERO + " de la versión, que es donde se"
                                        + " define la entidad del tipo. Genéralo con: ./gradlew -q CreateFilesTask"
                                        + " -Ptipo=src/main/java/com/educaflow/" + TiposExpediente.rel(TiposExpediente.carpeta(tipo))));
                continue;
            }

            comprobar(violaciones, tipo, entidad.get(),
                    tipo.getFqcnInitialEventManager(), FQCN_INITIAL_EVENT_MANAGER,
                    ficheroInitialEventManager(tipo));

            for (Fase fase : tipo.getFases()) {
                comprobar(violaciones, tipo, entidad.get(),
                        fase.getFqcnPhaseEventManager(), FQCN_PHASE_EVENT_MANAGER,
                        ficheroPhaseEventManager(fase));
            }
        }

        Violacion.assertNone("[M1] La entidad de un tipo de expediente es la primera <entity> de su "
                + DomainsDelTipo.NOMBRE_FICHERO + ", y es la que deben llevar como parámetro de tipo tanto su"
                + " InitialEventManagerImpl como el PhaseEventManagerImpl de cada una de sus fases.", violaciones);
    }

    /**
     * Comprueba el parámetro de tipo de una de las clases del tipo de expediente. Si la clase no está
     * compilada no se dice nada: es cosa de las reglas que vigilan que exista (E0 e I1).
     */
    private static void comprobar(List<Violacion> violaciones, TipoExpedienteInstanceFile tipo, String entidad,
                                  String fqcnClase, String fqcnGenerico, String fichero) {
        Optional<JavaClass> clase = Bytecode.clase(fqcnClase);
        if (clase.isEmpty()) {
            return;
        }

        String generico = TipoExpedienteInstanceFile.getSimpleClassName(fqcnGenerico);
        Optional<String> declarada = Bytecode.parametroDeTipo(clase.get(), fqcnGenerico);

        if (declarada.isEmpty()) {
            violaciones.add(new Violacion(tipo.getCode(), fichero,
                    "la clase " + fqcnClase + " no declara con qué entidad usa " + generico + "."
                    + "\n      se esperaba: " + generico + "<" + entidad + ">"
                    + "\n      Sin parámetro de tipo (en crudo) el tipo de expediente se queda sin decir cuál"
                    + " es su entidad, que es lo que ExpedienteLocator.getModelClass lee en runtime."));
        } else if (!declarada.get().equals(entidad)) {
            violaciones.add(new Violacion(tipo.getCode(), fichero,
                    "la clase " + fqcnClase + " usa " + generico + " con otra entidad."
                    + "\n      declarada: " + generico + "<" + declarada.get() + ">"
                    + "\n      se esperaba: " + generico + "<" + entidad + "> (la primera <entity> de "
                    + DomainsDelTipo.fichero(tipo) + ")"));
        }
    }

    /** El fuente del InitialEventManagerImpl, en la raíz de la versión: el evento inicial no es de ninguna fase. */
    private static String ficheroInitialEventManager(TipoExpedienteInstanceFile tipo) {
        return TiposExpediente.rel(TiposExpediente.carpeta(tipo)
                .resolve(tipo.getInitialEventManagerClassName() + ".java"));
    }

    /** El fuente del PhaseEventManagerImpl de la fase, en la subcarpeta de la fase. */
    private static String ficheroPhaseEventManager(Fase fase) {
        return TiposExpediente.rel(TiposExpediente.carpeta(fase)
                .resolve(fase.getTipoExpediente().getPhaseEventManagerClassName() + ".java"));
    }
}
