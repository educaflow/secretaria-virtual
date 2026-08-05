package com.educaflow.tiposexpedientes.support;

import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFile;
import com.educaflow.common.buildtools.files.tipoexpediente.TipoExpedienteInstanceFileFinder;
import com.educaflow.common.buildtools.files.tramite.TramitesLayout;

import java.nio.file.Files;
import java.nio.file.Path;
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

    private static List<TipoExpedienteInstanceFile> load() {
        Path origen = projectRoot().resolve("src/main/java");
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

    /** Ruta relativa legible del fichero, p.ej. "tramites/prueba/v1/EventManagerImpl.java". */
    public static String rel(Path path) {
        String s = path.toAbsolutePath().normalize().toString().replace('\\', '/');
        int i = s.indexOf("/com/educaflow/");
        return i >= 0 ? s.substring(i + "/com/educaflow/".length()) : s;
    }

    /** La carpeta del tipo de expediente, que es donde viven todos sus ficheros hermanos. */
    public static Path carpeta(TipoExpedienteInstanceFile tipo) {
        return tipo.getPath().getParent();
    }
}
