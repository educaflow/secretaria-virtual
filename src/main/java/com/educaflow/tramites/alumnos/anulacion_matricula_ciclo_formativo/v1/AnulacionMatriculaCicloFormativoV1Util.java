package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.subsystem.expedientes.db.Profile;
import com.educaflow.subsystem.expedientes.services.eventmanager.State;
import com.educaflow.subsystem.security.service.PerfilesUsuarioService;

import java.util.Optional;
import java.util.Set;

public final class AnulacionMatriculaCicloFormativoV1Util {

    private AnulacionMatriculaCicloFormativoV1Util() {
    }

    /* ------------------------------------------------------------------ */
    /* Validaciones                                                       */
    /* ------------------------------------------------------------------ */

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
                .filter("self.abierto = true AND self.usuarioRegistrador = :usuario AND self.ciclo = :ciclo "
                        + "AND self.cursoAcademico = :curso AND self.id <> :id")
                .bind("usuario", expediente.getUsuarioRegistrador())
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

    public static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException {
        Centro centroExpediente = expediente.getCentro();
        User usuarioAutenticado = SecurityUtil.getUser();
        Centro centroUsuarioAutenticado = (usuarioAutenticado == null) ? null : usuarioAutenticado.getCentroActivo();

        Long idCentroExpediente = (centroExpediente == null) ? null : centroExpediente.getId();
        Long idCentroUsuarioAutenticado = (centroUsuarioAutenticado == null) ? null : centroUsuarioAutenticado.getId();

        if (idCentroExpediente == null || idCentroUsuarioAutenticado == null
                || idCentroExpediente.equals(idCentroUsuarioAutenticado) == false) {
            throw new BusinessException(I18n.get(mensaje));
        }
    }

    // Tramitador.checkPerfilDelEstado hace return para el administrador en cualquier estado: esta
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

        Set<String> perfilesDelUsuario = Beans.get(PerfilesUsuarioService.class)
                .getPerfilesSobreExpediente(expediente, SecurityUtil.getUser());

        if (perfilesDelUsuario.contains(perfilDelEstado.name()) == false) {
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
