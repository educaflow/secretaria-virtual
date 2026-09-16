package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.Municipio;
import com.educaflow.subsystem.common.db.Persona;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventContext;
import com.educaflow.subsystem.expedientes.services.eventmanager.InitialEventManager;
import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.axelor.auth.db.User;


public class InitialEventManagerImpl implements InitialEventManager<AnulacionMatriculaCicloFormativoV1> {

    @Override
    public void triggerInitialEvent(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) throws BusinessException {
        User usuarioRegistrador = expediente.getUsuarioRegistrador();
        Persona persona = new Persona();
        persona.setNombre(usuarioRegistrador.getNombre());
        persona.setApellidos(usuarioRegistrador.getApellidos());
        persona.setDni(usuarioRegistrador.getDni());

        // createRegistroEntrada (al presentar) lee las dos personas y revienta con NPE si faltan.
        expediente.setPersonaInteresada(persona);
        expediente.setPersonaSolicitante(persona);

        Centro centro = expediente.getCentro();
        Integer curso = centro.getCurso();
        expediente.setCursoAcademico(curso == null ? null : curso + "/" + (curso + 1));

        expediente.setNombreCentro(centro.getName());

        Municipio municipio = centro.getMunicipio();
        expediente.setLocalidadCentro(municipio == null ? null : municipio.getName());
    }

}
