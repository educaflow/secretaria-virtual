package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.sistemaeducativo.db.Grado;
import com.educaflow.subsystem.sistemaeducativo.service.GradoService;

import java.util.Map;

public class GradoServiceImpl extends DefaultModelService<Grado> implements GradoService {

    public GradoServiceImpl(Class<Grado> model, Repository<Grado> repository) {
        super(model, repository);
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return allowPropertiesEditables();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return allowPropertiesEditables();
    }

    /**
     * Whitelist única de los campos que el cliente puede dictar al crear y al modificar un grado.
     *
     * <p>Esta clase existe solo por esta whitelist: sin ella {@code Grado} caería en
     * {@code DefaultModelService}, cuyos {@code allowProperties} son
     * {@code createAllowAllProperties()} — un fallo fail-open y silencioso que dejaría al endpoint
     * REST genérico dictar cualquier campo de la entidad.
     *
     * <p>{@code niveles} queda FUERA: es el lado inverso de {@code Nivel.grado}, nadie lo asigna
     * desde {@code Grado} y dejarlo entrar permitiría arrastrar hijos por la cascada
     * {@code PERSIST}/{@code MERGE} del one-to-many. Los niveles se mantienen desde la pantalla de
     * Niveles.
     *
     * <p>{@code admiteNivel} también queda FUERA: es un campo derivado, de solo
     * lectura, cuyo getter generado recalcula el valor en cada lectura; nada de lo que enviara el
     * cliente sobreviviría.
     */
    private AllowProperties allowPropertiesEditables() {
        return AllowProperties.createAllowProperties(Map.of(
                "code", Map.of(),
                "name", Map.of()
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

}
