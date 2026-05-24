package com.educaflow.subsystem.correos.service;

import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.correos.db.Correo;

import java.util.Optional;

/**
 * Servicio del subsistema Correos.
 *
 * <p>Las acciones {@code insert(Correo)} / {@code update} / {@code remove} y sus
 * {@code validateInsert(Correo)} / {@code validateUpdate} / {@code allowPropertiesInsert} se
 * heredan de {@link ModelService} y se <b>sobrescriben</b> en {@code CorreoServiceImpl}; NO se
 * re-declaran aquí.
 */
public interface CorreoService extends ModelService<Correo> {

    Correo insert(CorreoInsertDTO dto);

    Optional<BusinessMessages> validateInsert(CorreoInsertDTO dto);

    Correo reenviar(Correo correo, Correo correoOriginal);

    Optional<BusinessMessages> validateReenviar(Correo correo, Correo correoOriginal);

    AllowProperties allowPropertiesReenviar();

    void enviarCorreosPendientes();

    String proponerEmailPorDni(String dni);
}
