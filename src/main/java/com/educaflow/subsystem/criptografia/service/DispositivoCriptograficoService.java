package com.educaflow.subsystem.criptografia.service;

import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.criptografia.db.DispositivoCriptografico;

import java.util.List;
import java.util.Optional;

public interface DispositivoCriptograficoService extends ModelService<DispositivoCriptografico> {

    void recargarDispositivosEnEntornoCriptografico();
    List<Integer> getSlotsDisponibles(String pkcs11LibraryPath);
    String getDescripcionSlotsDisponibles(String pkcs11LibraryPath);



    Optional<BusinessMessages> validateRecargarDispositivosEnEntornoCriptografico();
    Optional<BusinessMessages> validateGetSlotsDisponibles(String pkcs11LibraryPath);
    Optional<BusinessMessages> validateGetDescripcionSlotsDisponibles(String pkcs11LibraryPath);


}