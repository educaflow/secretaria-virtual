package com.educaflow.base.infrastructure.mapper;

import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultModelLoader implements ModelLoader {

    private static final Logger logger = LoggerFactory.getLogger(DefaultModelLoader.class);

    @Override
    public Model getModel(Class<? extends Model> classModel, Long id) {
        // PENDIENTE: no se comprueba ni el permiso de lectura ni que la entidad cargada pertenezca al padre/expediente
        // editado. Un JpaSecurity.CAN_READ no lo resolvería: las entidades que llegan (MetaFile, Ciclo, User,
        // FormacionCentroTrabajo) tienen lectura sin condition. Es la misma pendiente de pertenencia que
        // TramitadorController.findModel.
        logger.warn("ERROR:------>   TODO:Comprobar la seguridad de acceso a la base de datos al llamar a este método!!!!!!.");


        JpaRepository jpaRepository = JpaRepository.of(classModel);
        if (jpaRepository == null) {
            throw new RuntimeException("No se encontró el repositorio para la clase: " + classModel);
        }
        return jpaRepository.find(id);
    }
}