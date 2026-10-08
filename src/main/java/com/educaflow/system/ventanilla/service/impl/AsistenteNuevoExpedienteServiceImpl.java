package com.educaflow.system.ventanilla.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.CentroUsuario;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.db.Tramite;
import com.educaflow.subsystem.expedientes.service.TramiteService;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.subsystem.tramitador.service.TramitadorService;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.ContextoTramitacion;
import com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente;
import com.educaflow.system.ventanilla.service.AsistenteNuevoExpedienteService;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AsistenteNuevoExpedienteServiceImpl extends DefaultModelService<AsistenteNuevoExpediente>
        implements AsistenteNuevoExpedienteService {

    @Inject
    TramitadorService tramitadorService;

    @Inject
    PerfilesUsuarioService perfilesUsuarioService;

    @Inject
    ModelServiceFactory modelServiceFactory;

    public AsistenteNuevoExpedienteServiceImpl(Class<AsistenteNuevoExpediente> model,
                                               Repository<AsistenteNuevoExpediente> repository) {
        super(model, repository);
    }

    @Override
    public AsistenteNuevoExpediente prepararCentros(AsistenteNuevoExpediente asistente) {
        validatePrepararCentros(asistente).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarArranqueDelAsistente(asistente);
        return asistente;
    }

    @Override
    public AsistenteNuevoExpediente prepararTramites(AsistenteNuevoExpediente asistente) {
        validatePrepararTramites(asistente).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarTramitesDisponibles(asistente);
        return asistente;
    }

    @Override
    public AsistenteNuevoExpediente recalcular(AsistenteNuevoExpediente asistente) {
        validateRecalcular(asistente).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarPresentacion(asistente);
        return asistente;
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente) {
        return validateCentroIndicado(asistente);
    }

    @Override
    public Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente) {
        Optional<BusinessMessages> centroIndicado = validateCentroIndicado(asistente);
        if (centroIndicado.isPresent()) {
            return centroIndicado;
        }

        return validateTramiteEvaluable(asistente);
    }

    @Override
    public Optional<BusinessMessages> validateTriggerInitialEvent(AsistenteNuevoExpediente asistente) {
        Optional<BusinessMessages> centroIndicado = validateCentroIndicado(asistente);
        if (centroIndicado.isPresent()) {
            return centroIndicado;
        }

        Optional<BusinessMessages> tramiteEvaluable = validateTramiteEvaluable(asistente);
        if (tramiteEvaluable.isPresent()) {
            return tramiteEvaluable;
        }

        if (asistente.getPresentadoEnRepresentacion() == null) {
            BusinessMessages businessMessages = new BusinessMessages();
            businessMessages.add(new BusinessMessage("presentadoEnRepresentacion",
                    I18n.get("Debe indicar para quién es el expediente"),
                    I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("presentadoEnRepresentacion").getTitle())));
            return Optional.of(businessMessages);
        }

        if (Boolean.TRUE.equals(asistente.getPresentadoEnPapel()) && asistente.getIdioma() == null) {
            BusinessMessages businessMessages = new BusinessMessages();
            businessMessages.add(new BusinessMessage("idioma",
                    I18n.get("Debe indicar el idioma"),
                    I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("idioma").getTitle())));
            return Optional.of(businessMessages);
        }

        return validateAlta(asistente.getTramite(), asistente.getCentro(),
                asistente.getPresentadoEnPapel(), asistente.getPresentadoEnRepresentacion());
    }

    private Optional<BusinessMessages> validateCentroIndicado(AsistenteNuevoExpediente asistente) {
        if (asistente.getCentro() == null) {
            BusinessMessages businessMessages = new BusinessMessages();
            businessMessages.add(new BusinessMessage("centro",
                    I18n.get("Debe indicar el centro"),
                    I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("centro").getTitle())));
            return Optional.of(businessMessages);
        }

        return Optional.empty();
    }

    private Optional<BusinessMessages> validateTramiteEvaluable(AsistenteNuevoExpediente asistente) {
        if (!getTramitesConTipoExpedienteActivo().contains(asistente.getTramite())) {
            BusinessMessages businessMessages = new BusinessMessages();
            businessMessages.add(new BusinessMessage("tramite",
                    I18n.get("Debe indicar el trámite"),
                    I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("tramite").getTitle())));
            return Optional.of(businessMessages);
        }

        return Optional.empty();
    }

    private Optional<BusinessMessages> validateAlta(Tramite tramite, Centro centro,
                                                    boolean presentadoEnPapel,
                                                    boolean presentadoEnRepresentacion) {
        ContextoTramitacion contexto = new ContextoTramitacion(tramite, centro,
                perfilDeInicioPara(presentadoEnPapel), presentadoEnPapel, presentadoEnRepresentacion, null);

        Optional<BusinessMessages> mensajesDelMotor = tramitadorService.validateTriggerInitialEvent(contexto);
        if (mensajesDelMotor.isPresent()) {
            return mensajesDelMotor;
        }

        return validateAfinadoDestinatario(contexto);
    }

    private Optional<BusinessMessages> validateAfinadoDestinatario(ContextoTramitacion contexto) {
        Tramite tramite = contexto.tramite();
        Centro centro = contexto.centro();

        if (tramite.getTipoUsuario() == null) {
            return Optional.empty();
        }

        boolean loPresentaElPropioUsuario = !contexto.presentadoEnPapel();
        boolean esEnRepresentacionDeOtraPersona = contexto.presentadoEnRepresentacion();

        if (loPresentaElPropioUsuario && !esEnRepresentacionDeOtraPersona
                && tramite.getPermitidoPresentarEnRepresentacion()
                && esFamiliar(centro) && !esDestinatarioDelTramite(tramite, centro)) {
            BusinessMessages businessMessages = new BusinessMessages();
            businessMessages.add(new BusinessMessage("presentadoEnRepresentacion",
                    I18n.get("No puede crear este expediente para usted mismo en el centro indicado"),
                    I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("presentadoEnRepresentacion").getTitle())));
            return Optional.of(businessMessages);
        }

        if (loPresentaElPropioUsuario && esEnRepresentacionDeOtraPersona
                && esDestinatarioDelTramite(tramite, centro) && !esFamiliar(centro)) {
            BusinessMessages businessMessages = new BusinessMessages();
            businessMessages.add(new BusinessMessage("presentadoEnRepresentacion",
                    I18n.get("No puede crear este expediente en representación de otra persona en el centro indicado"),
                    I18n.get(Mapper.of(AsistenteNuevoExpediente.class).getProperty("presentadoEnRepresentacion").getTitle())));
            return Optional.of(businessMessages);
        }

        return Optional.empty();
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesPrepararCentros() {
        return AllowProperties.createAllowProperties(Map.of(
                "presentadoEnPapel", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesPrepararTramites() {
        return AllowProperties.createAllowProperties(Map.of(
                "centro", Map.of(),
                "presentadoEnPapel", Map.of()
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
    public AllowProperties allowPropertiesTriggerInitialEvent() {
        return AllowProperties.createAllowProperties(Map.of(
                "tramite", Map.of(),
                "centro", Map.of(),
                "presentadoEnPapel", Map.of(),
                "presentadoEnRepresentacion", Map.of(),
                "idioma", Map.of()
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    private void fireActionRule_AsignarArranqueDelAsistente(AsistenteNuevoExpediente asistente) {
        List<Centro> centrosCandidatos = getCentrosCandidatos(asistente.getPresentadoEnPapel());

        asistente.setCentrosDisponibles(new LinkedHashSet<>(centrosCandidatos));
        asistente.setHayQueElegirCentro(centrosCandidatos.size() > 1);
        asistente.setCentro(centrosCandidatos.size() == 1 ? centrosCandidatos.get(0) : null);
    }

    private void fireActionRule_AsignarTramitesDisponibles(AsistenteNuevoExpediente asistente) {
        List<Tramite> tramitesCandidatos =
                getTramitesCandidatos(asistente.getCentro(), getTramitesConTipoExpedienteActivo(),
                        asistente.getPresentadoEnPapel());

        asistente.setTramitesDisponibles(new LinkedHashSet<>(tramitesCandidatos));
    }

    private void fireActionRule_AsignarPresentacion(AsistenteNuevoExpediente asistente) {
        Tramite tramite = asistente.getTramite();
        Centro centro = asistente.getCentro();
        boolean presentadoEnPapel = asistente.getPresentadoEnPapel();

        boolean admiteParaMi = admiteAlta(tramite, centro, presentadoEnPapel, false);
        boolean admiteEnRepresentacion = admiteAlta(tramite, centro, presentadoEnPapel, true);
        boolean hayQuePreguntarElDestinatario = admiteParaMi && admiteEnRepresentacion;

        asistente.setHayQuePreguntarParaQuien(hayQuePreguntarElDestinatario);
        asistente.setPresentadoEnRepresentacion(
                hayQuePreguntarElDestinatario ? null : admiteEnRepresentacion);
        asistente.setIdioma(presentadoEnPapel ? SecurityUtil.getUser().getLanguage() : null);
    }

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

    private List<Tramite> getTramitesCandidatos(Centro centro, List<Tramite> tramitesEvaluables,
                                                boolean presentadoEnPapel) {
        User usuario = SecurityUtil.getUser();
        Profile perfilDeInicio = perfilDeInicioPara(presentadoEnPapel);

        return tramitesEvaluables.stream()
                .filter(tramite -> perfilesUsuarioService
                        .getPerfilesDeInicioSobreTramite(tramite, usuario, centro).contains(perfilDeInicio))
                .toList();
    }

    private List<Centro> getCentrosCandidatos(boolean presentadoEnPapel) {
        List<CentroUsuario> centroUsuarios = SecurityUtil.getUser().getCentroUsuarios();
        if (centroUsuarios == null) {
            return List.of();
        }

        List<Tramite> tramitesEvaluables = getTramitesConTipoExpedienteActivo();

        return centroUsuarios.stream()
                .map(CentroUsuario::getCentro)
                .filter(Objects::nonNull)
                .filter(centro -> !getTramitesCandidatos(centro, tramitesEvaluables, presentadoEnPapel).isEmpty())
                .sorted(Comparator.comparing(Centro::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Profile perfilDeInicioPara(boolean presentadoEnPapel) {
        List<Profile> perfilesDeInicio = Arrays.stream(Profile.values())
                .filter(profile -> profile.puedeCrearExpediente()
                        && profile.permitePresentacionEnPapel() == presentadoEnPapel)
                .toList();

        if (perfilesDeInicio.size() != 1) {
            throw new IllegalStateException(
                    "Debe haber exactamente un perfil de inicio por forma de presentar, y hay " + perfilesDeInicio);
        }

        return perfilesDeInicio.get(0);
    }

    private boolean admiteAlta(Tramite tramite, Centro centro,
                               boolean presentadoEnPapel, boolean presentadoEnRepresentacion) {
        return validateAlta(tramite, centro, presentadoEnPapel, presentadoEnRepresentacion).isEmpty();
    }

    private boolean esDestinatarioDelTramite(Tramite tramite, Centro centro) {
        return tieneTipoUsuario(centro, tramite.getTipoUsuario().getCodigo());
    }

    private boolean esFamiliar(Centro centro) {
        return tieneTipoUsuario(centro, TipoUsuarioCodigo.FAMILIAR);
    }

    private List<Tramite> getTramitesConTipoExpedienteActivo() {
        final TramiteService tramiteService = (TramiteService) modelServiceFactory.resolve(Tramite.class);
        return tramiteService.findConTipoExpedienteActivo();
    }

    private boolean tieneTipoUsuario(Centro centro, TipoUsuarioCodigo codigoTipoUsuario) {
        User usuario = SecurityUtil.getUser();
        if (!usuario.perteneceAlCentro(centro)) {
            throw new IllegalStateException("El usuario no pertenece al centro indicado");
        }

        return usuario.tieneTipoUsuario(centro, codigoTipoUsuario);
    }

}
