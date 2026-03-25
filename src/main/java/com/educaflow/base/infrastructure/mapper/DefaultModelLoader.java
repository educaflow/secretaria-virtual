package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.JpaRepository;
import com.axelor.db.Model;

public class DefaultModelLoader implements ModelLoader {
    @Override
    public Model getModel(Class<? extends Model> classModel, Long id) {
        System.out.println("ERROR:------>   TODO:Comprobar la seguridad de acceso a la base de datos al llamar a este método!!!!!!.");

        JpaRepository jpaRepository = JpaRepository.of(classModel);
        if (jpaRepository == null) {
            throw new RuntimeException("No se encontró el repositorio para la clase: " + classModel);
        }
        return jpaRepository.find(id);
    }
}