package com.educaflow.tiposexpedientes.states;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.tiposexpedientes.support.Bytecode;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El código de un tipo de expediente solo referencia <b>su propia</b> clase {@code States}.
 *
 * <p>Todos los tipos tienen una clase generada que se llama igual, {@code States}, y todos suelen
 * tener estados que se llaman igual ({@code ENTRADA_DATOS}, {@code ACEPTADO}…). Así que al duplicar
 * una versión —que es literalmente copiar la carpeta entera y cambiarle el nombre
 * ({@code versionado.md})— un {@code import} que se quede apuntando a la versión vieja <b>compila
 * sin rechistar</b>: los nombres existen todos en el otro tipo.
 *
 * <p>Lo que pasa en runtime es peor que un error: {@code ExpedienteUtil.updateState} escribe en el
 * expediente el {@code codePhase} y el {@code codeState} del estado que le pasan, sin comprobar de
 * qué tipo es. El expediente de la v2 queda entonces en un estado de la v1 y, como los nombres
 * coinciden, todo parece funcionar hasta que los dos tipos divergen: en ese momento el expediente
 * está en un estado que su propia máquina no tiene, y ni el PhaseEventManager ni las vistas de su
 * fase saben qué hacer con él.
 *
 * <p>La regla mira <b>todas</b> las dependencias, no solo las lecturas de constante: así entran
 * también los {@code States.INSTANCE}, los alias de fase y los tipos de parámetro, y el mensaje sale
 * con el fichero y la línea que da ArchUnit.
 *
 * <p>Numeración aparte de la de {@code StatesTest} (S1–S5) a propósito: aquello compara la clase
 * generada con su XML y esto mira quién la usa.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class ReferenciasAStatesTest {

    private static final String CLASE_STATES = "States";

    @Test
    @DisplayName("R1: el código de un tipo de expediente no referencia la clase States de otro tipo")
    void r1_ningunTipoReferenciaElStatesDeOtro() {
        Map<String, TipoExpedienteInstanceFile> tipoPorStates = tipoPorStates();
        List<Violacion> violaciones = new ArrayList<>();

        for (JavaClass clase : Bytecode.all()) {
            TipoExpedienteInstanceFile propio = tipoDeLaClase(clase);
            if (propio == null) {
                continue; // código de tramites que no es de ningún tipo de expediente (p.ej. shared)
            }

            for (Dependency dependencia : clase.getDirectDependenciesFromSelf()) {
                TipoExpedienteInstanceFile ajeno = tipoPorStates.get(claseExterna(dependencia.getTargetClass()));

                if ((ajeno == null) || (ajeno == propio)) {
                    continue;
                }

                violaciones.add(new Violacion(propio.getCode(), clase.getName(),
                        "usa el States de " + ajeno.getCode() + " (" + fqcnStates(ajeno) + ") en vez del suyo ("
                        + fqcnStates(propio) + "): un estado de otro tipo se guardaría tal cual en el"
                        + " codePhase/codeState de este expediente. Revisa el import — es lo que queda al"
                        + " duplicar una versión.\n      " + dependencia.getDescription()));
            }
        }

        Violacion.assertNone("[R1] Ninguna clase de un tipo de expediente puede referenciar la clase States de"
                + " otro tipo: los nombres de estado coinciden entre versiones, así que el import equivocado"
                + " compila y solo se nota cuando las dos máquinas de estados divergen.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /** Índice de la clase States de cada tipo por su FQCN. */
    private static Map<String, TipoExpedienteInstanceFile> tipoPorStates() {
        Map<String, TipoExpedienteInstanceFile> indice = new LinkedHashMap<>();
        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            indice.put(fqcnStates(tipo), tipo);
        }

        return indice;
    }

    /**
     * El tipo de expediente dueño de la clase: aquel cuyo paquete base es el de la clase o un
     * prefijo suyo (las clases de las fases cuelgan de él). Se queda con el más específico, que es
     * lo correcto si algún día un tipo cuelga de la carpeta de otro.
     */
    private static TipoExpedienteInstanceFile tipoDeLaClase(JavaClass clase) {
        TipoExpedienteInstanceFile propio = null;

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            String base = tipo.getBasePackageName();

            if (clase.getPackageName().equals(base) || clase.getPackageName().startsWith(base + ".")) {
                if ((propio == null) || (base.length() > propio.getBasePackageName().length())) {
                    propio = tipo;
                }
            }
        }

        return propio;
    }

    /**
     * La clase de nivel superior del destino de la dependencia: los estados son constantes de los
     * enums anidados de States ({@code …States$Recepcion}), y lo que se compara es el States.
     */
    private static String claseExterna(JavaClass clase) {
        String nombre = clase.getFullName();
        int anidada = nombre.indexOf('$');

        return (anidada < 0) ? nombre : nombre.substring(0, anidada);
    }

    private static String fqcnStates(TipoExpedienteInstanceFile tipo) {
        return tipo.getBasePackageName() + "." + CLASE_STATES;
    }
}
