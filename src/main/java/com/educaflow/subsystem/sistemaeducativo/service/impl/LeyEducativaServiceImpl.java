package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.LeyEducativa;
import com.educaflow.subsystem.sistemaeducativo.service.LeyEducativaService;

import java.util.Optional;

public class LeyEducativaServiceImpl extends DefaultModelService<LeyEducativa> implements LeyEducativaService {

    public LeyEducativaServiceImpl(Class<LeyEducativa> model, Repository<LeyEducativa> repository) {
        super(model, repository);
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
