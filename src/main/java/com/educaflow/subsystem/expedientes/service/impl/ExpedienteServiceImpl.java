package com.educaflow.subsystem.expedientes.service.impl;

import com.axelor.db.EntityHelper;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.service.ExpedienteService;
import com.educaflow.subsystem.expedientes.util.ExpedienteNotasUtil;
import com.google.inject.Inject;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.Optional;

/**
 * No sobrescribe {@code insert}/{@code update}/{@code remove} ni sus {@code allowProperties*}: se quedan
 * en los valores por defecto de {@link DefaultModelService}, los mismos que tenía {@code Expediente}
 * antes de tener servicio propio.
 */
public class ExpedienteServiceImpl extends DefaultModelService<Expediente> implements ExpedienteService {

    @Inject
    JpaSecurity jpaSecurity;

    public ExpedienteServiceImpl(Class<Expediente> model, Repository<Expediente> repository) {
        super(model, repository);
    }

    @Override
    public Expediente addNote(Long idExpediente, String mensaje) {
        validateAddNote(idExpediente, mensaje).ifPresent(BusinessMessages::throwIfInvalid);

        Expediente expediente = getById(idExpediente);
        exigePoderAnotar(expediente);
        ExpedienteNotasUtil.addNote(expediente, mensaje);

        return expediente;
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateAddNote(Long idExpediente, String mensaje) {
        BusinessMessages messages = new BusinessMessages();

        if (mensaje == null || mensaje.isBlank()) {
            messages.add(new BusinessMessage(I18n.get("Escriba el texto de la nota")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

    /**
     * El id lo envía el cliente y {@code getById} no filtra por fila, así que sin la comprobación de
     * lectura cualquier usuario podría anotar en cualquier expediente. Se comprueba contra la clase real
     * del expediente, que sale de la base de datos y no del {@code _model} de la petición.
     *
     * <p>Que el panel de notas esté oculto para el creador es solo la pantalla: la defensa es esta. No
     * son validaciones: quien usa la pantalla nunca llega aquí sin poder leer el expediente o siendo su
     * creador.
     */
    private void exigePoderAnotar(Expediente expediente) {
        jpaSecurity.check(JpaSecurity.CAN_READ, EntityHelper.getEntityClass(expediente), expediente.getId());

        if (ExpedienteNotasUtil.esCreador(expediente, SecurityUtil.getUser())) {
            throw new UnauthorizedException("El creador de un expediente no puede añadirle notas");
        }
    }

}
