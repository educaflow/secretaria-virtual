package com.educaflow.system.gruposnotas.service.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.sistemaeducativo.db.CursoModulo;
import com.educaflow.system.gruposnotas.db.EstadoGrupo;
import com.educaflow.system.gruposnotas.db.Grupo;
import com.educaflow.system.gruposnotas.db.ModuloGrupo;
import com.educaflow.system.gruposnotas.db.repo.GrupoRepository;
import com.educaflow.system.gruposnotas.db.repo.ModuloGrupoRepository;
import com.educaflow.system.gruposnotas.service.GrupoService;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class GrupoServiceImpl extends DefaultModelService<Grupo> implements GrupoService {

    @Inject
    GrupoRepository grupoRepository;

    @Inject
    ModuloGrupoRepository moduloGrupoRepository;

    @Inject
    public GrupoServiceImpl(Class<Grupo> model, Repository<Grupo> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Operaciones genéricas ********************************/
    /****************************************************************************************/

    @Override
    public Grupo insert(Grupo grupo) {
        validateInsert(grupo).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_FijarCentroYCursoAcademicoSiSupervisor(grupo);
        fireActionRule_EstadoInicialAbierto(grupo);

        grupo = repository.save(grupo);

        fireActionRule_CrearModulosGrupo(grupo);

        return grupo;
    }

    @Override
    public Grupo update(Grupo grupo, Grupo original) {
        validateUpdate(grupo, original).ifPresent(BusinessMessages::throwIfInvalid);

        grupo.setCurso(original.getCurso());
        grupo.setCentro(original.getCentro());
        grupo.setCursoAcademico(original.getCursoAcademico());
        grupo.setEstado(original.getEstado());
        grupo.setFechaCierre(original.getFechaCierre());

        return repository.save(grupo);
    }

    /****************************************************************************************/
    /******************************** Acciones propias ************************************/
    /****************************************************************************************/

    @Override
    public Grupo cerrar(Grupo grupo, Grupo grupoOriginal) {
        validateCerrar(grupo, grupoOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_RegistrarCierre(grupo);

        return repository.save(grupo);
    }

    @Override
    public Grupo reabrir(Grupo grupo, Grupo grupoOriginal) {
        validateReabrir(grupo, grupoOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_RegistrarReapertura(grupo);

        return repository.save(grupo);
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Grupo grupo) {
        if (TextUtil.isNullOrBlank(grupo.getNombre())) {
            return Optional.of(BusinessMessages.single(I18n.get("El nombre del grupo es obligatorio.")));
        }
        if (grupo.getCurso() == null) {
            return Optional.of(BusinessMessages.single(I18n.get("El curso es obligatorio.")));
        }
        if (existeOtroGrupoConMismoNombre(grupo)) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("Ya existe un grupo con ese nombre en este centro y curso académico.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Grupo grupo, Grupo original) {
        if (original.getEstado() == EstadoGrupo.CERRADO) {
            return Optional.of(BusinessMessages.single(I18n.get("No se puede modificar un grupo cerrado.")));
        }
        if (existeOtroGrupoConMismoNombre(grupo)) {
            return Optional.of(BusinessMessages.single(
                    I18n.get("Ya existe un grupo con ese nombre en este centro y curso académico.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Grupo grupo) {
        if (grupo.getEstado() == EstadoGrupo.CERRADO) {
            return Optional.of(BusinessMessages.single(I18n.get("No se puede borrar un grupo cerrado.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateCerrar(Grupo grupo, Grupo grupoOriginal) {
        if (grupoOriginal.getEstado() != EstadoGrupo.ABIERTO) {
            return Optional.of(BusinessMessages.single(I18n.get("El grupo ya está cerrado.")));
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateReabrir(Grupo grupo, Grupo grupoOriginal) {
        if (grupoOriginal.getEstado() != EstadoGrupo.CERRADO) {
            return Optional.of(BusinessMessages.single(I18n.get("El grupo ya está abierto.")));
        }
        if (!SecurityUtil.isAdmin(AuthUtils.getUser())) {
            return Optional.of(BusinessMessages.single(I18n.get("No tiene permisos para reabrir el grupo.")));
        }
        return Optional.empty();
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombre", Map.of(),
                "curso", Map.of(),
                "centro", Map.of(),
                "cursoAcademico", Map.of(),
                "alumnosGrupo", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombre", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesRemove() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesCerrar() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesReabrir() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_FijarCentroYCursoAcademicoSiSupervisor(Grupo grupo) {
        if (!SecurityUtil.isAdmin(AuthUtils.getUser())) {
            Centro centroActivo = AuthUtils.getUser().getCentroActivo();
            grupo.setCentro(centroActivo);
            grupo.setCursoAcademico(centroActivo.getCurso());
        }
    }

    private void fireActionRule_EstadoInicialAbierto(Grupo grupo) {
        grupo.setEstado(EstadoGrupo.ABIERTO);
    }

    private void fireActionRule_CrearModulosGrupo(Grupo grupo) {
        for (CursoModulo cursoModulo : grupo.getCurso().getModulos()) {
            ModuloGrupo moduloGrupo = new ModuloGrupo();
            moduloGrupo.setGrupo(grupo);
            moduloGrupo.setModulo(cursoModulo.getModulo());
            moduloGrupoRepository.save(moduloGrupo);
        }
    }

    private void fireActionRule_RegistrarCierre(Grupo grupo) {
        grupo.setEstado(EstadoGrupo.CERRADO);
        grupo.setFechaCierre(LocalDateTime.now());
    }

    private void fireActionRule_RegistrarReapertura(Grupo grupo) {
        grupo.setEstado(EstadoGrupo.ABIERTO);
        grupo.setFechaCierre(null);
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private boolean existeOtroGrupoConMismoNombre(Grupo grupo) {
        Grupo existente = grupoRepository.findByNombreCentroCursoAcademico(
                grupo.getNombre(), grupo.getCentro(), grupo.getCursoAcademico());

        return existente != null && !existente.equals(grupo);
    }

}
