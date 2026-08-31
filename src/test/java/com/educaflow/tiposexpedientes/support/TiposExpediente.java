package com.educaflow.tiposexpedientes.support;

import com.educaflow.common.buildtools.files.tipoexpediente.Fase;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFileFinder;
import com.educaflow.common.buildtools.files.tramite.TramitesLayout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Descubrimiento de los tipos de expediente del proyecto para los tests de
 * {@code com.educaflow.tiposexpedientes}.
 *
 * <p>El parseo NO se reimplementa aquí: se delega en las mismas clases de
 * {@code com.educaflow:EducaFlowBuildTools} que usa el generador
 * ({@link TramitesLayout} + {@link TipoExpedienteInstanceFileFinder}). Así los tests validan el
 * código contra exactamente la misma lectura del {@code TipoExpedienteInstance.xml} que hizo el
 * generador — incluidas las derivaciones no triviales (el {@code code} a partir del trámite y del
 * nombre de la carpeta de versión, la unión ordenada de eventos, los FQCN por omisión).
 */
public final class TiposExpediente {

    private static List<TipoExpedienteInstanceFile> cache;

    private TiposExpediente() {}

    /** Todos los tipos de expediente del proyecto, parseados. Cacheado. */
    public static synchronized List<TipoExpedienteInstanceFile> all() {
        if (cache == null) {
            cache = load();
        }
        return cache;
    }

    /** La raíz de las fuentes Java, desde la que se relativizan los paquetes. */
    public static Path origen() {
        return projectRoot().resolve("src/main/java");
    }

    /** La carpeta que cuelga de {@link #origen()} y contiene todos los trámites. */
    public static Path raizDeTramites() {
        return origen().resolve(TramitesLayout.PAQUETE_RAIZ_POR_DEFECTO.replace('.', '/'));
    }

    private static List<TipoExpedienteInstanceFile> load() {
        Path origen = origen();
        TramitesLayout tramitesLayout =
                new TramitesLayout(origen, TramitesLayout.PAQUETE_RAIZ_POR_DEFECTO);

        List<TipoExpedienteInstanceFile> tipos =
                new TipoExpedienteInstanceFileFinder(tramitesLayout).findTiposExpedienteFile();

        if (tipos.isEmpty()) {
            throw new IllegalStateException("No se encontró ningún " + TipoExpedienteInstanceFileFinder.TIPO_EXPEDIENTE_XML_NAME
                    + " bajo " + origen + " (¿directorio de trabajo incorrecto?)");
        }
        return tipos;
    }

    /** Localiza la raíz del proyecto subiendo desde el directorio de trabajo hasta hallar src/main/java. */
    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("src/main/java/com/educaflow"))) {
                return p;
            }
        }
        return dir;
    }

    /** Ruta relativa legible del fichero, p.ej. "tramites/prueba/v1/recepcion/PhaseEventManagerImpl.java". */
    public static String rel(Path path) {
        String s = path.toAbsolutePath().normalize().toString().replace('\\', '/');
        int i = s.indexOf("/com/educaflow/");
        return i >= 0 ? s.substring(i + "/com/educaflow/".length()) : s;
    }

    /** La carpeta de versión del tipo de expediente, donde están el XML maestro y el modelo. */
    public static Path carpeta(TipoExpedienteInstanceFile tipo) {
        return tipo.getPath().getParent();
    }

    /**
     * Todas las fases de todos los tipos de expediente. Es la unidad sobre la que van los tests:
     * el {@code PhaseEventManagerImpl} y el {@code StateEventValidatorImpl} son <b>uno por fase</b>, cada
     * uno en el paquete de su fase, y cada uno atiende solo los estados de esa fase.
     */
    public static List<Fase> todasLasFases() {
        List<Fase> fases = new ArrayList<>();
        for (TipoExpedienteInstanceFile tipo : all()) {
            fases.addAll(tipo.getFases());
        }
        return fases;
    }

    /** La subcarpeta de la fase, que es donde viven sus tres ficheros. */
    public static Path carpeta(Fase fase) {
        return carpeta(fase.getTipoExpediente()).resolve(fase.getPackageSimpleName());
    }

    /** Cómo se identifica una fase en los mensajes de error: {@code PruebaV1/RECEPCION}. */
    public static String nombre(Fase fase) {
        return fase.getTipoExpediente().getCode() + "/" + fase.getName();
    }
}
