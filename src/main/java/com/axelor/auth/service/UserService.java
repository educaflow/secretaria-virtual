package com.axelor.auth.service;

import com.axelor.auth.db.User;
import com.axelor.db.modelservice.ModelService;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
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
     * @throws BusinessException si en el centro nadie tiene ese cargo o lo tiene más de uno: no hay forma de
     *                           saber a quién le toca, y lo tiene que arreglar quien gestiona los cargos del centro.
     */
    User getByCentroAndCargo(Centro centro, CargoCodigo cargo) throws BusinessException;
}
