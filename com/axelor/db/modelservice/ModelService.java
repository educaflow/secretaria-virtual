/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.modelservice;

import com.axelor.db.Model;
import java.util.Map;

/**
 * Service interface for entity persistence operations.
 *
 * <p>Implement this interface to intercept save and remove operations for a specific entity type.
 * The implementation is discovered by convention: for an entity {@code com.pkg.db.MyEntity}, the
 * framework looks for {@code com.pkg.service.MyEntityService} and then {@code
 * com.pkg.service.MyEntityServiceImpl}.
 *
 * @param <T> the type of the entity
 */
public interface ModelService<T extends Model> {

  /**
   * Inserta la entidad dada por primera vez en la base de datos.
   *
   * @param entity la entidad a insertar
   * @return la entidad insertada
   */
  T insert(T entity);

  /**
   * Actualiza la entidad dada en la base de datos.
   *
   * @param entity la entidad a actualizar
   * @return la entidad actualizada
   */
  T update(T entity, T original);

  /**
   * Remove the given entity.
   *
   * @param entity the entity to remove
   */
  void remove(T entity);

  /**
   * Validate the given json map before persisting.
   *
   * @param json the json map to validate
   * @param context the context
   * @return validated json map
   */
  Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context);

}
