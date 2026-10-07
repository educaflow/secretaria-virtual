package com.axelor.auth.service;

import java.util.Optional;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;

/**
 * El {@code ModelService} de {@link User}. Vive en el paquete de Axelor ({@code com.axelor.auth.service}) y no en
 * {@code subsystem/common}, donde se extiende el modelo, porque {@code ModelServiceFactory} busca el servicio de una
 * entidad junto al paquete de la propia entidad ({@code com.axelor.auth.db}).
 */
public interface UserService extends ModelService<User> {

    /**
     * El único usuario con ese cargo en el centro (el director, el secretario…), que es la persona a la que se le
     * pone a firmar lo que el centro resuelve.
     *
     * @throws RuntimeException si en el centro nadie tiene ese cargo o lo tiene más de uno: el centro está mal
     *                          configurado, y eso no debería pasar nunca.
     */
    User getByCentroAndCargo(Centro centro, CargoCodigo cargo);
    /**
     * Usuario cuyo DNI coincide con el recibido, o vacío si no hay ninguno o el DNI es nulo o está en blanco.
     */
    Optional<User> findByDni(String dni);

}
