package com.educaflow.subsystem.common.service;

import com.axelor.auth.db.User;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.subsystem.common.db.Centro;

/**
 * Responde a «quién es el director de este centro», que es la persona a la que se le pone a firmar lo que el
 * centro resuelve.
 *
 * <p>No es un {@code ModelService}: no gestiona el ciclo de vida de ninguna entidad, solo consulta. Su binding
 * está en {@code CommonModule}.
 */
public interface DirectorCentroService {

    /**
     * El único usuario con el cargo de director en el centro.
     *
     * @throws BusinessException si el centro no tiene director o tiene más de uno: no hay forma de saber a
     *                           quién le toca, y lo tiene que arreglar quien gestiona los cargos del centro.
     */
    User getDirector(Centro centro) throws BusinessException;
}
