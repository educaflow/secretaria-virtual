package com.educaflow.tramites.util.entrada;

import com.axelor.meta.db.MetaFile;
import com.educaflow.subsystem.expedientes.db.Expediente;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Los campos de un tipo de expediente con los que trabaja la fase {@code ENTRADA}. Cada tipo los declara en su
 * {@code domains.xml} con el mismo nombre, pero son campos de <b>su</b> entidad y no de {@code Expediente}: por
 * eso el código común no los nombra y los recibe aquí, como referencias a sus getters y setters.
 *
 * <p>Cada tipo construye el suyo una vez, como constante de su {@code <Code>Util}.
 *
 * @param getPdfSolicitud                    la solicitud generada, sin firmar
 * @param getPdfSolicitudFirmada             la solicitud que se registra: la firmada o, en papel, la escaneada
 * @param setPdfSolicitudFirmada             donde se deja la solicitud cuando se firma en el servidor
 * @param setPdfJustificanteRegistroEntrada  donde se deja el resguardo de la presentación
 * @param borrarSubsanacion                  vacía lo que el verificador pidió subsanar: ya se ha vuelto a presentar
 */
public record CamposEntrada<T extends Expediente>(
        Function<T, MetaFile> getPdfSolicitud,
        Function<T, MetaFile> getPdfSolicitudFirmada,
        BiConsumer<T, MetaFile> setPdfSolicitudFirmada,
        BiConsumer<T, MetaFile> setPdfJustificanteRegistroEntrada,
        Consumer<T> borrarSubsanacion) {

    public CamposEntrada {
        Objects.requireNonNull(getPdfSolicitud, "getPdfSolicitud no puede ser null");
        Objects.requireNonNull(getPdfSolicitudFirmada, "getPdfSolicitudFirmada no puede ser null");
        Objects.requireNonNull(setPdfSolicitudFirmada, "setPdfSolicitudFirmada no puede ser null");
        Objects.requireNonNull(setPdfJustificanteRegistroEntrada, "setPdfJustificanteRegistroEntrada no puede ser null");
        Objects.requireNonNull(borrarSubsanacion, "borrarSubsanacion no puede ser null");
    }
}
