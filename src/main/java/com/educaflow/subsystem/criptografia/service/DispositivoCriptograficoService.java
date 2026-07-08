package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;

import java.util.Optional;

public interface DispositivoCriptograficoService extends ModelService<DispositivoCriptografico> {

    /**
     * Vuelve a crear desde cero los dispositivos criptográficos del {@link com.educaflow.base.infrastructure.criptografia.EntornoCriptografico}
     * a partir de lo que hay en la tabla DispositivoCriptografico: se dan de baja los providers PKCS#11 actuales
     * y se abren sesiones nuevas contra cada dispositivo (login con el PIN incluido).
     */
    void recargarDispositivosEnEntornoCriptografico();

    Optional<BusinessMessages> validateRecargarDispositivosEnEntornoCriptografico();

}