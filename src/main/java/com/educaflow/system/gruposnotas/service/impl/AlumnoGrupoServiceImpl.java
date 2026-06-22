package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.system.gruposnotas.db.AlumnoGrupo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.Nota;
import com.educaflow.system.gruposnotas.db.ValorNota;
import com.educaflow.system.gruposnotas.db.repo.AlumnoGrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.GrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.NotaRepository;
import com.educaflow.system.gruposnotas.service.AlumnoGrupoService;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AlumnoGrupoServiceImpl extends DefaultModelService<AlumnoGrupo> implements AlumnoGrupoService {

    private static final String CODIGO_TIPO_USUARIO_ALUMNO = "ALUMNO";

    @Inject
    AlumnoGrupoRepository alumnoGrupoRepository;

    @Inject
    NotaRepository notaRepository;

    @Inject
    GrupoRepository grupoRepository;

    @Inject
    ModelServiceFactory modelServiceFactory;

    @Inject
    public AlumnoGrupoServiceImpl(Class<AlumnoGrupo> model, Repository<AlumnoGrupo> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Operaciones genéricas ********************************/
    /****************************************************************************************/

    @Override
    @com.google.inject.persist.Transactional
    public AlumnoGrupo guardarAlumnoGrupo(AlumnoGrupo alumnoGrupo, Long grupoId) {
        fireActionRule_RestaurarGrupoDesdeContexto(alumnoGrupo, grupoId);

        validateInsert(alumnoGrupo).ifPresent(BusinessMessages::throwIfInvalid);

        alumnoGrupo = repository.save(alumnoGrupo);

        fireActionRule_CrearNotasNoEvaluado(alumnoGrupo);

        return alumnoGrupo;
    }

    @Override
    public AlumnoGrupo update(AlumnoGrupo alumnoGrupo, AlumnoGrupo original) {
        throw new UnsupportedOperationException(
                I18n.get("La pertenencia de un alumno a un grupo no se puede modificar."));
    }

    /****************************************************************************************/
    /******************************** Acciones propias ************************************/
    /****************************************************************************************/

    @Override
    public String calcularNotaMedia(AlumnoGrupo alumnoGrupo) {
        return alumnoGrupo.getNotaMedia();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(AlumnoGrupo alumnoGrupo) {
        if (alumnoGrupo.getAlumno() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Debe elegir un alumno.")));
        }
        if (alumnoGrupo.getGrupo() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Debe indicarse el grupo.")));
        }
        if (alumnoGrupo.getGrupo().getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se pueden añadir alumnos a un grupo cerrado.")));
        }
        if (!esAlumnoDelCentroDelGrupo(alumnoGrupo)) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El alumno debe ser un usuario de tipo Alumno del centro del grupo.")));
        }
        Grupo grupo = alumnoGrupo.getGrupo();
        if (alumnoGrupoRepository.existsOtroGrupoMismoCursoAcademico(
                alumnoGrupo.getAlumno(), grupo.getCentro(), grupo.getCursoAcademico(), null)) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("El alumno ya pertenece a otro grupo de este curso académico.")));
        }
        if (alumnoYaEstaEnGrupo(alumnoGrupo)) {
            return Optional.of(BusinessMessages.single(I18n.get("El alumno ya está en el grupo.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRemove(AlumnoGrupo alumnoGrupo) {
        Grupo grupo = alumnoGrupo.getGrupo();
        if (grupo == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Debe indicarse el grupo.")));
        }
        if (!SecurityUtil.isAdmin(AuthUtils.getUser())) {
            Centro centroActivo = AuthUtils.getUser().getCentroActivo();
            if (grupo.getCentro() == null || !grupo.getCentro().equals(centroActivo)) {
                return Optional.of(BusinessMessages.single(
                        I18n.get("No puede quitar alumnos de un grupo de otro centro.")));
            }
        }
        if (grupo.getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("No se pueden quitar alumnos de un grupo cerrado.")));
        }
        return Optional.empty();
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "alumno", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesRemove() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_RestaurarGrupoDesdeContexto(AlumnoGrupo alumnoGrupo, Long grupoId) {
        Grupo grupo = grupoRepository.find(grupoId);
        if (grupo == null) {
            BusinessMessages.single(I18n.get("No se ha encontrado el grupo.")).throwIfInvalid();
        }
        if (!SecurityUtil.isAdmin(AuthUtils.getUser())) {
            Centro centroActivo = AuthUtils.getUser().getCentroActivo();
            if (grupo.getCentro() == null || !grupo.getCentro().equals(centroActivo)) {
                BusinessMessages.single(
                        I18n.get("No puede añadir alumnos a un grupo de otro centro.")).throwIfInvalid();
            }
        }
        alumnoGrupo.setGrupo(grupo);
    }

    private void fireActionRule_CrearNotasNoEvaluado(AlumnoGrupo alumnoGrupo) {
        ModelService<Nota> notaService = modelServiceFactory.resolve(Nota.class);
        for (ModuloGrupo moduloGrupo : alumnoGrupo.getGrupo().getModulosGrupo()) {
            Nota nota = new Nota();
            nota.setModuloGrupo(moduloGrupo);
            nota.setAlumnoGrupo(alumnoGrupo);
            nota.setValor(ValorNota.NO_EVALUADO);
            notaService.insert(nota);
        }
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private boolean esAlumnoDelCentroDelGrupo(AlumnoGrupo alumnoGrupo) {
        User alumno = alumnoGrupo.getAlumno();
        Grupo grupo = alumnoGrupo.getGrupo();
        if (alumno.getCentroUsuarios() == null) {
            return false;
        }
        return alumno.getCentroUsuarios().stream()
                .filter(centroUsuario -> centroUsuario.getCentro() != null
                        && centroUsuario.getCentro().equals(grupo.getCentro()))
                .anyMatch(this::tieneTipoUsuarioAlumno);
    }

    private boolean tieneTipoUsuarioAlumno(CentroUsuario centroUsuario) {
        List<CentroUsuarioTipoUsuario> tipos = centroUsuario.getCentroUsuarioTipoUsuario();
        if (tipos == null) {
            return false;
        }
        return tipos.stream()
                .map(CentroUsuarioTipoUsuario::getTipoUsuario)
                .filter(tipoUsuario -> tipoUsuario != null)
                .map(TipoUsuario::getCodigo)
                .anyMatch(CODIGO_TIPO_USUARIO_ALUMNO::equals);
    }

    private boolean alumnoYaEstaEnGrupo(AlumnoGrupo alumnoGrupo) {
        List<AlumnoGrupo> alumnosGrupo = alumnoGrupoRepository.findByGrupo(alumnoGrupo.getGrupo());
        if (alumnosGrupo == null) {
            return false;
        }
        return alumnosGrupo.stream()
                .anyMatch(existente -> existente.getAlumno() != null
                        && existente.getAlumno().equals(alumnoGrupo.getAlumno())
                        && !existente.equals(alumnoGrupo));
    }

}
