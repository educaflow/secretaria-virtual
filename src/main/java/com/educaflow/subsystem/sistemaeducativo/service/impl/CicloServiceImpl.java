package com.educaflow.subsystem.sistemaeducativo.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.subsystem.sistemaeducativo.db.Ciclo;
import com.educaflow.subsystem.sistemaeducativo.db.Grado;
import com.educaflow.subsystem.sistemaeducativo.db.Nivel;
import com.educaflow.subsystem.sistemaeducativo.service.CicloService;

import java.util.Map;
import java.util.Optional;

public class CicloServiceImpl extends DefaultModelService<Ciclo> implements CicloService {

    public CicloServiceImpl(Class<Ciclo> model, Repository<Ciclo> repository) {
        super(model, repository);
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Ciclo ciclo) {
        return validateCoherenciaGradoNivel(ciclo);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Ciclo ciclo, Ciclo cicloOriginal) {
        return validateCoherenciaGradoNivel(ciclo);
    }

    /**
     * Sin grado no se evalúa ninguna de las tres reglas: el invariante «el ciclo tiene grado» ya
     * tiene dueño propio, {@code Ciclo.grado required="true"}, así que sin grado la fila no puede
     * llegar a existir; y sin grado tampoco hay dominio contra el que comparar el nivel.
     */
    private Optional<BusinessMessages> validateCoherenciaGradoNivel(Ciclo ciclo) {
        Grado grado = ciclo.getGrado();

        if (grado == null) {
            return Optional.empty();
        }

        BusinessMessages messages = new BusinessMessages();
        Nivel nivel = ciclo.getNivel();
        String nivelLabel = I18n.get(Mapper.of(Ciclo.class).getProperty("nivel").getTitle());

        if (Boolean.TRUE.equals(grado.getAdmiteNivel())) {
            if (nivel == null) {
                messages.add(new BusinessMessage("nivel", I18n.get("El nivel es obligatorio para el grado indicado"), nivelLabel));
            } else if (grado.equals(nivel.getGrado()) == false) {
                messages.add(new BusinessMessage("nivel", I18n.get("El nivel indicado no pertenece al grado del ciclo"), nivelLabel));
            }
        } else {
            if (nivel != null) {
                messages.add(new BusinessMessage("nivel", I18n.get("El grado indicado no admite nivel: el nivel debe quedar vacío"), nivelLabel));
            }
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

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
     * Whitelist única de los campos que el cliente puede dictar al crear y al modificar un ciclo.
     *
     * <p>Los campos relacionales que solo se aceptan como referencia llevan mapa interno vacío: así
     * solo pasan {@code id}/{@code version} y no se copia ningún campo dentro de la entidad apuntada.
     *
     * <p>CRITICAL: {@code cursos} lleva el subárbol que el panel maestro-detalle edita. Si se dejara
     * fuera —o con mapa interno vacío— el panel de cursos del formulario de ciclo dejaría de guardar
     * sus cursos y sus módulos.
     */
    private AllowProperties allowPropertiesEditables() {
        return AllowProperties.createAllowProperties(Map.of(
                "code", Map.of(),
                "name", Map.of(),
                "familiaProfesional", Map.of(),
                "grado", Map.of(),
                "nivel", Map.of(),
                "cursos", Map.<String, Object>of(
                        "code", Map.of(),
                        "name", Map.of(),
                        "ciclo", Map.of(),
                        "leyEducativa", Map.of(),
                        "modulos", Map.<String, Object>of(
                                "curso", Map.of(),
                                "modulo", Map.of()
                        )
                )
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

}
