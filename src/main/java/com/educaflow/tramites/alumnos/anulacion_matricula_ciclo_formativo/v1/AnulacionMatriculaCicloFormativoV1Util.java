package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.firmas.db.TareaFirma;
import com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO;
import com.educaflow.subsystem.firmas.service.TareaFirmaService;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.firma.PhaseEventManagerImpl;
import com.educaflow.tramites.util.entrada.CamposEntrada;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AnulacionMatriculaCicloFormativoV1Util {

    private AnulacionMatriculaCicloFormativoV1Util() {
    }

    /* ------------------------------------------------------------------ */
    /* Fases comunes                                                      */
    /* ------------------------------------------------------------------ */

    /** Los campos de este tipo con los que trabaja el código común de la fase ENTRADA (tramites/util/entrada). */
    public static final CamposEntrada<AnulacionMatriculaCicloFormativoV1> CAMPOS_ENTRADA = new CamposEntrada<>(
            AnulacionMatriculaCicloFormativoV1::getPdfSolicitud,
            AnulacionMatriculaCicloFormativoV1::getPdfSolicitudFirmada,
            AnulacionMatriculaCicloFormativoV1::setPdfSolicitudFirmada,
            AnulacionMatriculaCicloFormativoV1::setPdfJustificanteRegistroEntrada,
            AnulacionMatriculaCicloFormativoV1Util::borrarSubsanacion);

    public static void borrarSubsanacion(AnulacionMatriculaCicloFormativoV1 expediente) {
        expediente.setResultadoVerificacion(null);
        expediente.setTextoSubsanacion(null);
    }

    /* ------------------------------------------------------------------ */
    /* Validaciones                                                       */
    /* ------------------------------------------------------------------ */

    public static boolean esPresentadoEnPapelEnRepresentacion(AnulacionMatriculaCicloFormativoV1 expediente) {
        return Boolean.TRUE.equals(expediente.getPresentadoEnPapel()) && Boolean.TRUE.equals(expediente.getPresentadoEnRepresentacion());
    }

    public static boolean tieneIdentificadoAlSolicitante(AnulacionMatriculaCicloFormativoV1 expediente) {
        Persona solicitante = expediente.getPersonaSolicitante();
        if (solicitante == null) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene persona solicitante: la crea el Tramitador al dar de alta el expediente");
        }

        return (TextUtil.isNullOrBlank(solicitante.getApellidos()) == false)
                && (TextUtil.isNullOrBlank(solicitante.getNombre()) == false)
                && DniUtil.isValid(solicitante.getDni());
    }

    public static boolean esPresentadoEnRepresentacion(AnulacionMatriculaCicloFormativoV1 expediente) {
        return Boolean.TRUE.equals(expediente.getPresentadoEnRepresentacion());
    }

    /**
     * En representación quien presenta y el interesado son dos personas: si tienen el mismo DNI es que
     * el solicitante lo presenta para sí mismo, y para eso está el modo «para la persona que lo presenta».
     * Un DNI en blanco en cualquiera de los dos se da por válido: su obligatoriedad ya la exigen
     * {@code Required} y {@link #tieneIdentificadoAlSolicitante}.
     */
    public static boolean interesadoDistintoDelSolicitante(AnulacionMatriculaCicloFormativoV1 expediente) {
        Persona solicitante = expediente.getPersonaSolicitante();
        Persona interesada = expediente.getPersonaInteresada();
        if ((solicitante == null) || (interesada == null)) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene persona solicitante o interesada: las crea el Tramitador al dar de alta el expediente");
        }
        if (TextUtil.isNullOrBlank(solicitante.getDni()) || TextUtil.isNullOrBlank(interesada.getDni())) {
            return true;
        }

        return DniUtil.clean(solicitante.getDni()).equals(DniUtil.clean(interesada.getDni())) == false;
    }

    public static boolean sinOtraSolicitudEnCursoParaElMismoCiclo(AnulacionMatriculaCicloFormativoV1 expediente) {
        String cursoAcademico = expediente.getCursoAcademico();
        // Sin auto-flush: cuando la regla corre, el expediente gestionado ya lleva copiados los datos
        // del cliente todavía sin validar, y un flush de esta consulta sobre su misma tabla los
        // persistiría aunque la validación acabe fallando.
        long otrasSolicitudesEnCurso = JpaRepository.of(AnulacionMatriculaCicloFormativoV1.class).all()
                .autoFlush(false)
                // Por el alumno y no por quien registra: quien registra solicitudes en papel lo hace para muchos alumnos.
                .filter("self.abierto = true AND self.personaInteresada.dni = :dni AND self.ciclo = :ciclo "
                        + "AND self.cursoAcademico = :curso AND self.id <> :id")
                .bind("dni", expediente.getPersonaInteresada().getDni())
                .bind("ciclo", expediente.getCiclo())
                .bind("curso", cursoAcademico)
                .bind("id", expediente.getId())
                .count();

        return otrasSolicitudesEnCurso == 0;
    }

    /* ------------------------------------------------------------------ */
    /* Firma de la resolución                                             */
    /* ------------------------------------------------------------------ */

    // El campoFirma del hueco que documentospdf/resolucion.xml deja bajo «El secretario / la secretaria».
    public static final String CAMPO_FIRMA_SECRETARIO = "firmaSecretario";

    // El campoFirma del hueco que documentospdf/resolucion.xml deja bajo «El director / la directora».
    public static final String CAMPO_FIRMA_DIRECTOR = "firmaDirector";

    /**
     * Pone {@code pdfResolucion} a firmar a {@code firmante} en su bandeja de firmas, en el hueco {@code campoFirma}.
     * Cuando firme o rechace, el subsistema de firmas llama al notify del PhaseEventManagerImpl de la fase FIRMA,
     * que recupera el expediente por su id ({@code callBackData}).
     */
    public static void ponerResolucionAFirmar(AnulacionMatriculaCicloFormativoV1 expediente, User firmante, String campoFirma) {
        TareaFirmaService tareaFirmaService = (TareaFirmaService) Beans.get(ModelServiceFactory.class).resolve(TareaFirma.class);
        tareaFirmaService.insert(new TareaFirmaInsertDTO(
                firmante,
                expediente.getCentro(),
                List.of(expediente.getPdfResolucion()),
                I18n.get("Resolución de la solicitud de anulación de matrícula del expediente %s").formatted(expediente.getNumeroExpediente()),
                campoFirma,
                PhaseEventManagerImpl.class,
                expediente.getId()));
    }

    /* ------------------------------------------------------------------ */
    /* Control de acceso                                                  */
    /* ------------------------------------------------------------------ */

    public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException {
        User creador = expediente.getUsuarioRegistrador();
        if (creador == null) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene usuario registrador: lo fija el Tramitador al dar de alta el expediente");
        }
        User usuarioAutenticado = Objects.requireNonNull(SecurityUtil.getUser(), "No hay usuario autenticado: el Tramitador solo dispara eventos de un usuario autenticado");

        if (creador.getId().equals(usuarioAutenticado.getId()) == false) {
            throw new BusinessException(I18n.get(mensaje));
        }
    }

    public static void exigePertenecerAlCentroDelExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException {
        Centro centroExpediente = expediente.getCentro();
        if (centroExpediente == null) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene centro: lo fija el Tramitador al dar de alta el expediente");
        }
        User usuarioAutenticado = Objects.requireNonNull(SecurityUtil.getUser(), "No hay usuario autenticado: el Tramitador solo dispara eventos de un usuario autenticado");

        if (usuarioAutenticado.getCentroUsuario(centroExpediente) == null) {
            throw new BusinessException(I18n.get(mensaje));
        }
    }

    // TramitadorService.validatePerfilDelEstado deja pasar al administrador en cualquier estado (le atribuye
    // todos los perfiles): esta guarda es la misma exigencia del motor, sin esa exención.
    public static void exigeOstentarElPerfilDelEstado(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException {
        Optional<State> estado = States.INSTANCE.getState(expediente.getCodePhase(), expediente.getCodeState());

        if (estado.isEmpty()) {
            throw new IllegalStateException("El expediente está en el estado '" + expediente.getCodePhase()
                    + "/" + expediente.getCodeState() + "', que no existe en este tipo de expediente.");
        }

        Profile perfilDelEstado = estado.get().getProfile();

        if (perfilDelEstado == null) {
            return;
        }

        Set<Profile> perfilesDelUsuario = Beans.get(PerfilesUsuarioService.class)
                .getPerfilesSobreExpediente(expediente, SecurityUtil.getUser());

        if (perfilesDelUsuario.contains(perfilDelEstado) == false) {
            throw new BusinessException(I18n.get(mensaje));
        }
    }

}
