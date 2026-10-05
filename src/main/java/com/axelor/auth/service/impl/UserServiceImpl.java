package com.axelor.auth.service.impl;

import com.axelor.auth.db.User;
import com.axelor.auth.service.UserService;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.CargoCodigo;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuarioCargo;
import com.educaflow.subsystem.common.db.repo.CentroUsuarioCargoRepository;
import com.google.inject.Inject;

import java.util.List;
import java.util.Objects;

public class UserServiceImpl extends DefaultModelService<User> implements UserService {

    @Inject
    CentroUsuarioCargoRepository centroUsuarioCargoRepository;

    public UserServiceImpl(Class<User> model, Repository<User> repository) {
        super(model, repository);
    }

    @Override
    public User getByCentroAndCargo(Centro centro, CargoCodigo cargo)  {
        Objects.requireNonNull(centro, "centro no puede ser null");
        Objects.requireNonNull(cargo, "cargo no puede ser null");

        List<CentroUsuarioCargo> usuariosConCargo = centroUsuarioCargoRepository.findByCentroAndCargo(centro, cargo);

        if (usuariosConCargo.isEmpty()) {
            throw new RuntimeException("El centro id=" + centro.getId() + " no tiene ningún usuario con el cargo " + cargo);
        }
        if (usuariosConCargo.size() > 1) {
            throw new RuntimeException("El centro id=" + centro.getId() + " tiene " + usuariosConCargo.size() + " usuarios con el cargo " + cargo);
        }

        User user=usuariosConCargo.get(0).getCentroUsuario().getUsuario();

        Objects.requireNonNull(user, "user no puede ser null");

        return user;
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

}
