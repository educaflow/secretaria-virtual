package com.educaflow.tiposexpedientes.higiene;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.tiposexpedientes.support.TiposExpediente;
import com.educaflow.tiposexpedientes.support.Violacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * No hay ningún {@code PhaseEventManagerImpl} ni {@code StateEventValidatorImpl} en una carpeta que
 * no sea la de una fase declarada.
 *
 * <p>Es la dirección contraria a las reglas E0 y V0, que van de la fase al fichero: aquí se va del
 * fichero a la fase. Lo que queda entre medias es el caso de renombrar una fase en el
 * {@code TipoExpedienteInstance.xml} —o quitarla, o moverle los estados a otra— y no tocar las
 * carpetas: la fase nueva se genera con {@code CreateFilesTask}, E0 y V0 quedan contentas con ella y
 * la carpeta vieja se queda ahí, con su código completo y compilando, sin que nadie la ejecute nunca
 * más. {@code ExpedienteLocator} resuelve las clases por el {@code codePhase} del expediente, así
 * que a una carpeta que ya no es ninguna fase no llega jamás.
 *
 * <p>Y no es solo código muerto: es código muerto <b>que engaña</b>. Al leerlo parece la
 * implementación vigente de esos estados, y arreglar un bug ahí no cambia nada en la aplicación.
 *
 * <p>La regla mira el <b>árbol de fuentes</b>, no el bytecode como el resto del paquete. Aquí es lo
 * correcto: una clase compilada que sobra puede ser un resto de una compilación anterior sin
 * {@code clean} —un falso positivo que no se puede arreglar editando nada—, y lo que hay que
 * denunciar es el fichero que está en git.
 *
 * <p><b>Estos tests se escriben A MANO.</b> Este fichero es la fuente de verdad y se edita
 * directamente.
 */
class ClasesDeFaseHuerfanasTest {

    private static final String EXTENSION_JAVA = ".java";
    private static final String EXTENSION_KOTLIN = ".kt";

    @Test
    @DisplayName("H1: todo PhaseEventManagerImpl y StateEventValidatorImpl está en la carpeta de una fase declarada")
    void h1_noHayClasesDeFaseHuerfanas() {
        Set<String> declaradas = clasesDeLasFasesDeclaradas();
        List<Violacion> violaciones = new ArrayList<>();

        for (Path fichero : ficherosDeClasesDeFase()) {
            String fqcn = fqcn(fichero);
            if (declaradas.contains(fqcn)) {
                continue;
            }

            TipoExpedienteInstanceFile tipo = tipoDelFichero(fqcn);

            violaciones.add(new Violacion(
                    (tipo == null) ? "(ningún tipo)" : tipo.getCode(),
                    TiposExpediente.rel(fichero),
                    (tipo == null)
                            ? ("está bajo " + TiposExpediente.rel(TiposExpediente.raizDeTramites())
                               + " pero no dentro de ningún tipo de expediente, así que no lo ejecuta nadie")
                            : ("está en la carpeta '" + carpeta(fichero) + "', que no es ninguna fase de "
                               + tipo.getCode() + ": sus fases son " + nombresDeFases(tipo) + " (carpetas "
                               + carpetasDeFases(tipo) + "). ExpedienteLocator resuelve las clases por el"
                               + " codePhase del expediente, así que este fichero no se ejecuta nunca."
                               + " Si la fase se renombró, mueve el fichero a su carpeta; si desapareció,"
                               + " bórralo")));
        }

        Violacion.assertNone("[H1] Todo PhaseEventManagerImpl y todo StateEventValidatorImpl bajo tramites/ debe"
                + " estar en la subcarpeta de una fase declarada en el TipoExpedienteInstance.xml de su tipo:"
                + " en cualquier otra carpeta es código muerto que aparenta estar vivo.", violaciones);
    }

    // -----------------------------------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------------------------------

    /** Los FQCN de las dos clases de cada fase declarada: las únicas que ExpedienteLocator resuelve. */
    private static Set<String> clasesDeLasFasesDeclaradas() {
        Set<String> clases = new LinkedHashSet<>();
        for (Fase fase : TiposExpediente.todasLasFases()) {
            clases.add(fase.getFqcnPhaseEventManager());
            clases.add(fase.getFqcnStateEventValidator());
        }

        return clases;
    }

    /**
     * Los ficheros fuente de esas dos clases que hay bajo {@code tramites/}, se corresponda su
     * carpeta con una fase o no.
     */
    private static List<Path> ficherosDeClasesDeFase() {
        Set<String> nombres = nombresDeClase();
        Path raiz = TiposExpediente.raizDeTramites();

        try (Stream<Path> arbol = Files.walk(raiz)) {
            return arbol.filter(Files::isRegularFile)
                    .filter(fichero -> nombres.contains(sinExtension(fichero.getFileName().toString())))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo recorrer " + raiz, ex);
        }
    }

    /**
     * Los nombres simples de las dos clases, tal cual los nombra el generador de esqueletos (son los
     * mismos para todos los tipos, pero se derivan de la API y no se escriben a mano).
     */
    private static Set<String> nombresDeClase() {
        Set<String> nombres = new LinkedHashSet<>();
        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            nombres.add(tipo.getPhaseEventManagerClassName());
            nombres.add(tipo.getStateEventValidatorClassName());
        }

        return nombres;
    }

    /** El tipo de expediente en cuya carpeta está el fichero, o null si no está en ninguna. */
    private static TipoExpedienteInstanceFile tipoDelFichero(String fqcn) {
        TipoExpedienteInstanceFile propio = null;

        for (TipoExpedienteInstanceFile tipo : TiposExpediente.all()) {
            String base = tipo.getBasePackageName() + ".";

            if (fqcn.startsWith(base)) {
                if ((propio == null) || (base.length() > propio.getBasePackageName().length())) {
                    propio = tipo;
                }
            }
        }

        return propio;
    }

    /** El FQCN que le tocaría al fichero por su ubicación (que es como lo resuelve el locator). */
    private static String fqcn(Path fichero) {
        String relativo = TiposExpediente.origen().relativize(fichero).toString().replace('\\', '/');

        return sinExtension(relativo).replace('/', '.');
    }

    private static String sinExtension(String nombre) {
        if (nombre.endsWith(EXTENSION_JAVA)) {
            return nombre.substring(0, nombre.length() - EXTENSION_JAVA.length());
        }
        if (nombre.endsWith(EXTENSION_KOTLIN)) {
            return nombre.substring(0, nombre.length() - EXTENSION_KOTLIN.length());
        }

        return nombre;
    }

    private static String carpeta(Path fichero) {
        return fichero.getParent().getFileName().toString();
    }

    private static List<String> nombresDeFases(TipoExpedienteInstanceFile tipo) {
        List<String> nombres = new ArrayList<>();
        for (Fase fase : tipo.getFases()) {
            nombres.add(fase.getName());
        }

        return nombres;
    }

    private static List<String> carpetasDeFases(TipoExpedienteInstanceFile tipo) {
        List<String> carpetas = new ArrayList<>();
        for (Fase fase : tipo.getFases()) {
            carpetas.add(fase.getPackageSimpleName());
        }

        return carpetas;
    }
}
