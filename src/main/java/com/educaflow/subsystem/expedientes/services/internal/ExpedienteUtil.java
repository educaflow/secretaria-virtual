package com.educaflow.subsystem.expedientes.services.internal;

import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfFactory;
import com.educaflow.base.infrastructure.pdf.DocumentoPdfUtil;
import com.educaflow.subsystem.expedientes.db.Expediente;

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

}
