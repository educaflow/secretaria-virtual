package com.educaflow.subsystem.expedientes.services.internal;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;
import com.educaflow.subsystem.expedientes.services.eventmanager.EventManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public class ExpedienteUtil {

    public static DocumentoPdf getDocumentoPdf(Expediente expediente,String documentoPdfFileName ) {
        try {
            Class<?> callerClass = expediente.getClass();

            Path pathFileName= Path.of(documentoPdfFileName);

            try (InputStream in = callerClass.getResourceAsStream(documentoPdfFileName)) {
                if (in == null) {
                    throw new IOException("No se encontró el recurso: " + documentoPdfFileName);
                }
                DocumentoPdf documentoPdfVacio= DocumentoPdfFactory.getDocumentoPdf(in.readAllBytes(), pathFileName.getFileName().toString());

                Map<String, Object> contexto = Map.of("self", expediente,"now", java.time.LocalDateTime.now());

                DocumentoPdf documentoPdfRelleno = DocumentoPdfUtil.generate(documentoPdfVacio, contexto);

                return documentoPdfRelleno;

            }
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el documento PDF: " + documentoPdfFileName, e);
        }
    }

    public static void updateState(Expediente expediente,Enum state) {
        if (state==null) {
            throw new IllegalArgumentException("El state no puede ser nulo.");
        }
        String currentCodeState = expediente.getCodeState();

        StateEnum stateEnum = new StateEnum(state);
        if ((currentCodeState!=null) && (currentCodeState.equals(stateEnum.getCodeState()))) {
            return;
        }

        expediente.setCodeState(stateEnum.getCodeState());
        expediente.setNameState(com.educaflow.base.util.TextUtil.humanize(stateEnum.getCodeState()));
        expediente.setFechaUltimoEstado(java.time.LocalDateTime.now());
        expediente.setAbierto(!stateEnum.isClosed());
    }

    public static Expediente getExpedienteFromIdExpediente(long idExpediente) {
        JpaRepository<Expediente> expedienteRepository = getJpaRepository(idExpediente);
        Expediente expediente =expedienteRepository.find(idExpediente);
        if (expediente == null) {
            throw new RuntimeException("No existe el expediente con idExpediente: " + idExpediente);
        }

        return expediente;
    }

    /**
     * Obtiene el Repository de un expediente en función del id del expediente.
     * Se usa este método porque de otra forma se retornaría el Repositorio de Expediente y no del expediente en concreto.
     *
     * @param idExpediente
     * @return
     */
    private static JpaRepository<Expediente> getJpaRepository(long idExpediente) {
        JpaRepository<Expediente> onlyExpedienteRepository = JpaRepository.of(Expediente.class);
        Expediente expediente = onlyExpedienteRepository.find(idExpediente);
        EventManager eventManager = TipoExpedienteUtil.getEventManager(expediente.getTipoExpediente());
        JpaRepository<Expediente> realExpedienteRepository = JpaRepository.of(eventManager.getModelClass());
        JPA.em().detach(expediente);

        return realExpedienteRepository;
    }

}
