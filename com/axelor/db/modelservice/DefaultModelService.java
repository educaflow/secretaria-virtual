/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.modelservice;

import com.axelor.db.Model;
import com.axelor.db.Repository;

import java.util.Map;

/**
 * Default implementation of {@link ModelService} that delegates to the entity's {@link
 * Repository}.
 *
 * <p>This implementation is used when no custom service is found for an entity type.
 *
 * @param <T> the type of the entity
 */
public class DefaultModelService<T extends Model> implements ModelService<T> {

  protected Class<T> model;
  protected final Repository<T> repository;

  public DefaultModelService(Class<T> model,Repository<T> repository) {
    this.model=model;
    this.repository = repository;
  }

  @Override
  public T insert(T entity) {
    return repository.save(entity);
  }

  @Override
  public T update(T entity,T original) {
    return repository.save(entity);
  }

  @Override
  public void remove(T entity) {
    repository.remove(entity);
  }

  @Override
  public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
    return repository.validate(json, context);
  }

}
