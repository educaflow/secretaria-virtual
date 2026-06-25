package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository;
import com.educaflow.system.gruposnotas.service.AlumnoGrupoService;
import com.educaflow.system.gruposnotas.service.NotaInsertDTO;
import com.educaflow.system.gruposnotas.service.NotaService;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AlumnoGrupoServiceImpl extends DefaultModelService<AlumnoGrupo>
        implements AlumnoGrupoService {

    @Inject
    private ModelServiceFactory modelServiceFactory;

    public AlumnoGrupoServiceImpl(Class<AlumnoGrupo> model, Repository<AlumnoGrupo> repository) {
        super(model, repository);
    }

    @Override
    public AlumnoGrupo insert(AlumnoGrupo alumnoGrupo) {
        validateInsert(alumnoGrupo).ifPresent(BusinessMessages::throwIfInvalid);

        alumnoGrupo = repository.save(alumnoGrupo);

        fireActionRule_CrearNotasNoEvaluado(alumnoGrupo);

        return alumnoGrupo;
    }

    @Override
    public void remove(AlumnoGrupo alumnoGrupo) {
        validateRemove(alumnoGrupo).ifPresent(BusinessMessages::throwIfInvalid);

        repository.remove(alumnoGrupo);
    }

    @Override
    public String calcularNotaMedia(AlumnoGrupo alumnoGrupo) {
        // CC-001 (momento lectura): delega en el getter transient del dominio (única fuente de verdad).
        return alumnoGrupo.getNotaMedia();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(AlumnoGrupo alumnoGrupo) {
        // V-AlumnoGrupo-007 (VAL-018): grupo indicado.
        Grupo grupo = alumnoGrupo.getGrupo();
        if (grupo == null) {
            return Optional.of(BusinessMessages.single(I18n.get("El grupo es obligatorio")));
        }

        // V-AlumnoGrupo-002 (VAL-010): alumno indicado.
        User alumno = alumnoGrupo.getAlumno();
        if (alumno == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Debe elegir un alumno")));
        }

        // V-AlumnoGrupo-003 (VAL-011): el grupo está ABIERTO.
        if (grupo.getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se pueden añadir alumnos a un grupo cerrado")));
        }

        // V-AlumnoGrupo-008 (VAL-019): si el usuario es SUPERVISOR (no administrador), el grupo
        // pertenece a su centro activo (defensa IDOR del padre, k-secure-coding §3.6).
        User usuario = SecurityUtil.getUser();
        if (!SecurityUtil.isAdmin(usuario)
                && !Objects.equals(grupo.getCentro(), usuario.getCentroActivo())) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El grupo no pertenece a su centro")));
        }

        // V-AlumnoGrupo-004 (VAL-012): el alumno es un usuario del centro del grupo de tipo Alumno.
        if (!esAlumnoDelCentro(alumno, grupo.getCentro())) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El alumno debe ser un usuario de tipo Alumno del centro del grupo")));
        }

        // V-AlumnoGrupo-005 (VAL-013, RES-004): el alumno no pertenece ya a otro grupo del mismo
        // curso académico (excluyendo el propio id si ya tiene).
        if (perteneceAOtroGrupoDelCursoAcademico(alumnoGrupo, alumno, grupo)) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El alumno ya pertenece a otro grupo de este curso académico")));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRemove(AlumnoGrupo alumnoGrupo) {
        // V-AlumnoGrupo-006 (VAL-014): el grupo está ABIERTO.
        if (alumnoGrupo.getGrupo().getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se pueden quitar alumnos de un grupo cerrado")));
        }
        return Optional.empty();
    }

    /**
     * V-AlumnoGrupo-004 (VAL-012): comprueba que existe un {@link CentroUsuario} del alumno en el
     * centro del grupo con un tipo de usuario de código {@code ALUMNO}. Consulta ad-hoc sobre las
     * entidades externas de {@code common} con parámetros nombrados (k-secure-coding §5).
     */
    private boolean esAlumnoDelCentro(User alumno, Centro centro) {
        return JPA.all(CentroUsuario.class)
                .filter("self.usuario = :alumno AND self.centro = :centro"
                        + " AND EXISTS (SELECT 1 FROM CentroUsuarioTipoUsuario cutu"
                        + " WHERE cutu.centroUsuario = self AND cutu.tipoUsuario.codigo = :codigo)")
                .bind("alumno", alumno)
                .bind("centro", centro)
                .bind("codigo", "ALUMNO")
                .count() > 0;
    }

    /**
     * V-AlumnoGrupo-005 (VAL-013, RES-004): el alumno ya pertenece a otro grupo del mismo curso
     * académico. Usa el finder del repositorio y excluye el propio registro si ya tiene id.
     */
    private boolean perteneceAOtroGrupoDelCursoAcademico(AlumnoGrupo alumnoGrupo, User alumno, Grupo grupo) {
        List<AlumnoGrupo> existentes = ((AlumnoGrupoRepository) repository)
                .findByAlumnoAndGrupoCursoAcademico(alumno, grupo.getCursoAcademico())
                .fetch();
        return existentes.stream()
                .anyMatch(otro -> !Objects.equals(otro.getId(), alumnoGrupo.getId()));
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "grupo", Map.of(),
                "alumno", Map.of()));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of());
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    /**
     * R-AlumnoGrupo-001 (RN-005): por cada módulo del grupo, crea una nota NO_EVALUADO para el
     * alumno mediante {@link NotaService} (alta programática). Efecto colateral, después de save.
     */
    private void fireActionRule_CrearNotasNoEvaluado(AlumnoGrupo alumnoGrupo) {
        NotaService notaService = (NotaService) modelServiceFactory.resolve(Nota.class);
        for (ModuloGrupo moduloGrupo : alumnoGrupo.getGrupo().getModulosGrupo()) {
            notaService.insert(new NotaInsertDTO(moduloGrupo, alumnoGrupo));
        }
    }
}
