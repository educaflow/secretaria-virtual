package com.educaflow.tramites.util.verificacion;

import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lo que hacen igual todos los tipos de expediente en su fase {@code VERIFICACION}. A qué estado se pasa
 * después lo decide cada tipo, que es el único que puede nombrar los estados de su {@code States}.
 */
public class VerificacionHelper {

    private static final Logger log = LoggerFactory.getLogger(VerificacionHelper.class);

    @Inject
    private ModelServiceFactory modelServiceFactory;

    /**
     * Avisa por correo a quien presentó la solicitud de que tiene que subsanarla.
     *
     * <p>El aviso es una cortesía, no parte del trámite: lo que hay que subsanar queda en el expediente, que es
     * donde se subsana. Por eso, si no se le puede escribir (en papel no hay correo del solicitante), no se
     * envía nada y la verificación sigue adelante.
     *
     * @return si se ha creado el correo
     */
    public boolean avisarDeSubsanacion(Expediente expediente, String textoSubsanacion) {
        CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
        Correo correo = crearCorreoSubsanacion(expediente, textoSubsanacion);

        if (correoService.validateInsert(correo).isPresent()) {
            log.info("No se avisa por correo de la subsanación del expediente id={}: no hay a quién escribir", expediente.getId());
            return false;
        }

        correoService.insert(correo);
        return true;
    }

    private static Correo crearCorreoSubsanacion(Expediente expediente, String textoSubsanacion) {
        Persona solicitante = expediente.getPersonaSolicitante();

        Correo correo = new Correo();
        correo.setDniDestinatario(solicitante.getDni());
        correo.setNombre(solicitante.getNombre());
        correo.setApellidos(solicitante.getApellidos());
        correo.setPara(solicitante.getEmail());
        correo.setCentro(expediente.getCentro());
        correo.setAsunto(I18n.get("Tiene que subsanar su solicitud del expediente %s").formatted(expediente.getNumeroExpediente()));
        correo.setCuerpo(I18n.get("Se ha revisado su solicitud «%s» (expediente %s) y hay que subsanarla antes de continuar con su tramitación:\n\n%s\n\nEntre en la secretaría virtual, corrija la solicitud en ese mismo expediente y vuelva a presentarla.")
                .formatted(expediente.getName(), expediente.getNumeroExpediente(), textoSubsanacion));

        return correo;
    }
}
