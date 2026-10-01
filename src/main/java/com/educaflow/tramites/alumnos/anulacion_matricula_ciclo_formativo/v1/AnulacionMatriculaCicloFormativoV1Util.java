package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.tramitador.tramitacion.eventmanager.State;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;

import java.util.Optional;
import java.util.Set;

public final class AnulacionMatriculaCicloFormativoV1Util {

    private AnulacionMatriculaCicloFormativoV1Util() {
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

        return (isBlank(solicitante.getApellidos()) == false)
                && (isBlank(solicitante.getNombre()) == false)
                && DniUtil.isValid(solicitante.getDni());
    }

    private static boolean isBlank(String texto) {
        return (texto == null) || texto.isBlank();
    }

    public static boolean sinOtraSolicitudEnCursoParaElMismoCiclo(AnulacionMatriculaCicloFormativoV1 expediente) {
        String cursoAcademico = expediente.getCursoAcademico();
        if (cursoAcademico == null || cursoAcademico.isBlank()) {
            throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente()
                    + " no tiene curso académico: el centro " + expediente.getCentro().getCode()
                    + " no lo tiene configurado");
        }

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
    /* Control de acceso                                                  */
    /* ------------------------------------------------------------------ */

    public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException {
        User creador = expediente.getUsuarioRegistrador();
        User usuarioAutenticado = SecurityUtil.getUser();

        Long idCreador = (creador == null) ? null : creador.getId();
        Long idUsuarioAutenticado = (usuarioAutenticado == null) ? null : usuarioAutenticado.getId();

        if (idCreador == null || idUsuarioAutenticado == null || idCreador.equals(idUsuarioAutenticado) == false) {
            throw new BusinessException(I18n.get(mensaje));
        }
    }

    public static void exigePertenecerAlCentroDelExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException {
        Centro centroExpediente = expediente.getCentro();
        User usuarioAutenticado = SecurityUtil.getUser();

        if (centroExpediente == null || usuarioAutenticado == null
                || usuarioAutenticado.getCentroUsuario(centroExpediente) == null) {
            throw new BusinessException(I18n.get(mensaje));
        }
    }

    // ExpedienteSecurity.checkPerfilDelEstado hace return para el administrador en cualquier estado: esta
    // guarda es la misma exigencia del motor, sin esa exención.
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

    /* ------------------------------------------------------------------ */
    /* Devolución del director a secretaría                               */
    /* ------------------------------------------------------------------ */

    public static void borrarDevolucionDelDirector(AnulacionMatriculaCicloFormativoV1 expediente) {
        expediente.setMotivoDevolucion(null);
        expediente.setFechaDevolucion(null);
        expediente.setDevueltoPor(null);
    }

}
