package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.ValorNota;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import com.educaflow.system.gruposnotas.service.NotaService;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class NotaServiceImpl extends DefaultModelService<Nota> implements NotaService {

    private static final long MAX_MATRICULAS_HONOR_POR_MODULO = 3;

    @Inject
    NotaRepository notaRepository;

    @Inject
    public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Operaciones genéricas ********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Nota nota) {
        if (nota.getModuloGrupo() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Debe indicarse el módulo del grupo.")));
        }
        if (nota.getAlumnoGrupo() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Debe indicarse el alumno del grupo.")));
        }
        // V-Nota-005 (RES-006): una nota por alumno+módulo. Defense-in-depth de la
        // unique-constraint(moduloGrupo,alumnoGrupo) de la BD con un mensaje de negocio.
        if (existeNotaParaModuloYAlumno(nota)) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("Ya existe una nota para ese alumno y módulo.")));
        }
        return Optional.empty();
    }

    /****************************************************************************************/
    /******************************** Acciones propias ************************************/
    /****************************************************************************************/

    @Override
    public Nota guardarNota(Nota nota, Nota notaOriginal) {
        validateGuardarNota(nota, notaOriginal).ifPresent(BusinessMessages::throwIfInvalid);
        fireActionRule_FijarFechasCalificacion(nota, notaOriginal);
        return repository.save(nota);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateGuardarNota(Nota nota, Nota notaOriginal) {
        // V-Nota-001 (VAL-016): valor en dominio. Al ser enum, basta comprobar que no es null.
        if (nota.getValor() == null) {
            return Optional.of(BusinessMessages.single(I18n.get(
                    "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor.")));
        }
        // V-Nota-002 (VAL-015): el grupo debe estar abierto.
        if (nota.getModuloGrupo().getGrupo().getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(I18n.get(
                    "No se pueden modificar las notas de un grupo cerrado.")));
        }
        // V-Nota-003 (VAL-017): máximo 3 matrículas de honor por módulo.
        if (nota.getValor() == ValorNota.MATRICULA_HONOR
                && notaOriginal.getValor() != ValorNota.MATRICULA_HONOR
                && notaRepository.countMatriculasHonorByModuloGrupo(nota.getModuloGrupo())
                        >= MAX_MATRICULAS_HONOR_POR_MODULO) {
            return Optional.of(BusinessMessages.single(I18n.get(
                    "No se pueden poner más de 3 matrículas de honor en un módulo.")));
        }
        return Optional.empty();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_FijarFechasCalificacion(Nota nota, Nota notaOriginal) {
        // R-Nota-001 (CC-002, fechaCalificacion servidor) y R-Nota-002 (CC-003, fechaUltimaModificacion
        // servidor). Las fechas son campos servidor: se restauran desde el original para que el cliente
        // no las pueda dictar, y solo la rama que aplique sobrescribe la suya con now().
        nota.setFechaCalificacion(notaOriginal.getFechaCalificacion());
        nota.setFechaUltimaModificacion(notaOriginal.getFechaUltimaModificacion());

        if (notaOriginal.getValor() == ValorNota.NO_EVALUADO && nota.getValor() != ValorNota.NO_EVALUADO) {
            nota.setFechaCalificacion(LocalDateTime.now());
        } else if (notaOriginal.getValor() != ValorNota.NO_EVALUADO
                && nota.getValor() != notaOriginal.getValor()) {
            nota.setFechaUltimaModificacion(LocalDateTime.now());
        }
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesRemove() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesGuardarNota() {
        return AllowProperties.createAllowProperties(Map.of(
                "valor", Map.of()
        ));
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private boolean existeNotaParaModuloYAlumno(Nota nota) {
        return notaRepository.countByModuloGrupoYAlumnoGrupo(
                nota.getModuloGrupo(), nota.getAlumnoGrupo()) > 0;
    }

}
