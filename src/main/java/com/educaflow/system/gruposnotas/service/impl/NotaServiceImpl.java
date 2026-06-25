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
import com.educaflow.system.gruposnotas.service.NotaInsertDTO;
import com.educaflow.system.gruposnotas.service.NotaService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class NotaServiceImpl extends DefaultModelService<Nota> implements NotaService {

    public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository) {
        super(model, repository);
    }

    @Override
    public Nota insert(NotaInsertDTO dto) {
        validateInsert(dto).ifPresent(BusinessMessages::throwIfInvalid);

        Nota nota = new Nota();
        nota.setModuloGrupo(dto.moduloGrupo());
        nota.setAlumnoGrupo(dto.alumnoGrupo());
        nota.setValor(ValorNota.NO_EVALUADO);
        return repository.save(nota);
    }

    @Override
    public Nota update(Nota nota, Nota original) {
        validateUpdate(nota, original).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_RestaurarCamposInmutables(nota, original);
        fireActionRule_AsignarFechaCalificacion(nota, original);
        fireActionRule_AsignarFechaUltimaModificacion(nota, original);

        return repository.save(nota);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(NotaInsertDTO dto) {
        if (dto.moduloGrupo() == null) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El módulo del grupo de la nota es obligatorio.")));
        }
        if (dto.alumnoGrupo() == null) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El alumno del grupo de la nota es obligatorio.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Nota nota, Nota original) {
        // V-Nota-002 (VAL-015): no se pueden modificar las notas de un grupo cerrado.
        if (nota.getModuloGrupo().getGrupo().getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se pueden modificar las notas de un grupo cerrado")));
        }

        // V-Nota-003 (VAL-016): el valor debe pertenecer al dominio del enum (con enum tipado solo
        // puede fallar si es null).
        if (nota.getValor() == null) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor")));
        }

        // V-Nota-004 (VAL-017): no más de 3 matrículas de honor por módulo del grupo.
        if (nota.getValor() == ValorNota.MATRICULA_HONOR) {
            long notaId = nota.getId() != null ? nota.getId() : -1L;
            if (((NotaRepository) repository)
                    .countMatriculasHonorByModuloGrupo(nota.getModuloGrupo(), notaId)
                    .count() >= 3) {
                return Optional.of(BusinessMessages.single(
                        I18n.get("No se pueden poner más de 3 matrículas de honor en un módulo")));
            }
        }

        return Optional.empty();
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of("valor", Map.of()));
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    /** R-Nota-001 (CC-002): asigna la fecha de calificación al dejar de ser NO_EVALUADO por primera vez. */
    private void fireActionRule_AsignarFechaCalificacion(Nota nota, Nota original) {
        if (original.getValor() == ValorNota.NO_EVALUADO
                && nota.getValor() != ValorNota.NO_EVALUADO
                && nota.getFechaCalificacion() == null) {
            nota.setFechaCalificacion(LocalDateTime.now());
        }
    }

    /** R-Nota-002 (CC-003): asigna la fecha de última modificación al cambiar un valor ya calificado. */
    private void fireActionRule_AsignarFechaUltimaModificacion(Nota nota, Nota original) {
        if (nota.getValor() != original.getValor() && original.getFechaCalificacion() != null) {
            nota.setFechaUltimaModificacion(LocalDateTime.now());
        }
    }

    /** R-Nota-003: defensa anti mass-assignment, restaura los campos inmutables desde el original. */
    private void fireActionRule_RestaurarCamposInmutables(Nota nota, Nota original) {
        nota.setModuloGrupo(original.getModuloGrupo());
        nota.setAlumnoGrupo(original.getAlumnoGrupo());
    }
}
