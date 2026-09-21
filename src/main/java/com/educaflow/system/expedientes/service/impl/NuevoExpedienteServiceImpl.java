package com.educaflow.system.expedientes.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.services.ExpedienteService;
import com.educaflow.subsystem.expedientes.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.system.expedientes.db.NuevoExpediente;
import com.educaflow.system.expedientes.service.NuevoExpedienteService;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NuevoExpedienteServiceImpl extends DefaultModelService<NuevoExpediente> implements NuevoExpedienteService {

    @Inject
    ExpedienteService expedienteService;

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    public NuevoExpedienteServiceImpl(Class<NuevoExpediente> model, Repository<NuevoExpediente> repository) {
        super(model, repository);
    }

    @Override
    public NuevoExpediente preparar(NuevoExpediente nuevoExpediente) {
        validatePreparar(nuevoExpediente).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarDatosTramite(nuevoExpediente);
        fireActionRule_AsignarCentrosDisponibles(nuevoExpediente);
        return nuevoExpediente;
    }

    @Override
    public NuevoExpediente recalcular(NuevoExpediente nuevoExpediente) {
        validateRecalcular(nuevoExpediente).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarFormaDePresentar(nuevoExpediente);
        fireActionRule_AsignarOpcionesParaQuien(nuevoExpediente);
        return nuevoExpediente;
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validatePreparar(NuevoExpediente nuevoExpediente) {
        Tramite tramite = getTramiteObligatorio(nuevoExpediente);

        if (getCentrosDisponibles(tramite).isEmpty()) {
            return Optional.of(BusinessMessages.single(I18n.get("No puede crear expedientes de este trámite en ninguno de sus centros")));
        }

        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateRecalcular(NuevoExpediente nuevoExpediente) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateCrear(NuevoExpediente nuevoExpediente) {
        return expedienteService.validateTriggerInitialEvent(toContextoTramitacion(nuevoExpediente));
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesPreparar() {
        return AllowProperties.createAllowProperties(Map.of(
                "tramite", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesRecalcular() {
        return AllowProperties.createAllowProperties(Map.of(
                "tramite", Map.of(),
                "centro", Map.of(),
                "presentadoEnPapel", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesCrear() {
        return AllowProperties.createAllowProperties(Map.of(
                "tramite", Map.of(),
                "centro", Map.of(),
                "presentadoEnPapel", Map.of(),
                "presentadoEnRepresentacion", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AsignarDatosTramite(NuevoExpediente nuevoExpediente) {
        Tramite tramite = getTramiteObligatorio(nuevoExpediente);

        nuevoExpediente.setNombreTramite(I18n.get(tramite.getName()));
        nuevoExpediente.setAyudaTramite(tramite.getHelp());
    }

    private void fireActionRule_AsignarCentrosDisponibles(NuevoExpediente nuevoExpediente) {
        Set<Centro> centrosDisponibles = new LinkedHashSet<>(getCentrosDisponibles(getTramiteObligatorio(nuevoExpediente)));

        nuevoExpediente.setCentrosDisponibles(centrosDisponibles);
        nuevoExpediente.setHayUnSoloCentroDisponible(centrosDisponibles.size() == 1);
    }

    private void fireActionRule_AsignarFormaDePresentar(NuevoExpediente nuevoExpediente) {
        Tramite tramite = getTramiteObligatorio(nuevoExpediente);
        Centro centro = nuevoExpediente.getCentro();

        nuevoExpediente.setHayQuePreguntarPresentacion(perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, SecurityUtil.getUser(), centro).size() > 1);
        nuevoExpediente.setPresentadoEnPapelDeducido(getPerfilDeducido(tramite, centro).map(Profile::permitePresentacionEnPapel).orElse(null));
        nuevoExpediente.setPresentadoEnPapelVigente(isPresentadoEnPapelVigente(nuevoExpediente));
    }

    private void fireActionRule_AsignarOpcionesParaQuien(NuevoExpediente nuevoExpediente) {
        Tramite tramite = getTramiteObligatorio(nuevoExpediente);
        Centro centro = nuevoExpediente.getCentro();

        if (centro == null) {
            nuevoExpediente.setSePuedeCrearParaMi(false);
            nuevoExpediente.setSePuedeCrearEnRepresentacion(false);
            nuevoExpediente.setHayQuePreguntarParaQuien(false);
            return;
        }

        boolean presentadoEnPapel = isPresentadoEnPapelVigente(nuevoExpediente);
        Profile perfil = perfilesUsuarioService.getPerfil(tramite, SecurityUtil.getUser(), centro, presentadoEnPapel);

        boolean sePuedeCrearParaMi = admiteElAlta(new ContextoTramitacion(tramite, centro, perfil, presentadoEnPapel, false));
        boolean sePuedeCrearEnRepresentacion = admiteElAlta(new ContextoTramitacion(tramite, centro, perfil, presentadoEnPapel, true));

        nuevoExpediente.setSePuedeCrearParaMi(sePuedeCrearParaMi);
        nuevoExpediente.setSePuedeCrearEnRepresentacion(sePuedeCrearEnRepresentacion);
        nuevoExpediente.setHayQuePreguntarParaQuien(sePuedeCrearParaMi && sePuedeCrearEnRepresentacion);
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private ContextoTramitacion toContextoTramitacion(NuevoExpediente nuevoExpediente) {
        Tramite tramite = getTramiteObligatorio(nuevoExpediente);
        Centro centro = getCentroObligatorio(nuevoExpediente);
        boolean presentadoEnPapel = nuevoExpediente.getPresentadoEnPapel();
        Profile perfil = perfilesUsuarioService.getPerfil(tramite, SecurityUtil.getUser(), centro, presentadoEnPapel);

        return new ContextoTramitacion(tramite, centro, perfil, presentadoEnPapel, nuevoExpediente.getPresentadoEnRepresentacion());
    }

    private List<Centro> getCentrosDisponibles(Tramite tramite) {
        List<CentroUsuario> centrosUsuario = SecurityUtil.getUser().getCentroUsuarios();
        if (centrosUsuario == null) {
            return List.of();
        }

        return centrosUsuario.stream()
                .map(CentroUsuario::getCentro)
                .filter(Objects::nonNull)
                .filter(centro -> !perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, SecurityUtil.getUser(), centro).isEmpty())
                .sorted(Comparator.comparing(Centro::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean admiteElAlta(ContextoTramitacion contexto) {
        return expedienteService.validateTriggerInitialEvent(contexto).isEmpty();
    }

    private boolean isPresentadoEnPapelVigente(NuevoExpediente nuevoExpediente) {
        return getPerfilDeducido(getTramiteObligatorio(nuevoExpediente), nuevoExpediente.getCentro())
                .map(Profile::permitePresentacionEnPapel)
                .orElse(nuevoExpediente.getPresentadoEnPapel());
    }

    private Optional<Profile> getPerfilDeducido(Tramite tramite, Centro centro) {
        Set<Profile> perfilesDeInicio = perfilesUsuarioService.getPerfilesDeInicioSobreTramite(tramite, SecurityUtil.getUser(), centro);

        if (perfilesDeInicio.size() == 1) {
            return Optional.of(perfilesDeInicio.iterator().next());
        }

        return Optional.empty();
    }

    private Centro getCentroObligatorio(NuevoExpediente nuevoExpediente) {
        Centro centro = nuevoExpediente.getCentro();

        if (centro == null) {
            throw new IllegalStateException("Un NuevoExpediente sin centro al crearlo es un error de programación o una petición manipulada: la vista lo exige antes de crear");
        }

        return centro;
    }

    private Tramite getTramiteObligatorio(NuevoExpediente nuevoExpediente) {
        Tramite tramite = nuevoExpediente.getTramite();

        if (tramite == null) {
            throw new IllegalStateException("Un NuevoExpediente sin trámite es un error de programación o una petición manipulada: la vista lo fija siempre al abrirse");
        }

        return tramite;
    }



}
