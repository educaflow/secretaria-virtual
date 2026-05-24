package com.educaflow.subsystem.correos.service.impl;

import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.AdjuntoCorreo;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.service.RemitenteProvider;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye el DTO {@link Mail} de infraestructura a partir de un {@link Correo} de dominio.
 *
 * <p>Lee {@code emailDestinatario}, {@code asunto} y {@code cuerpo} del correo; resuelve el
 * remitente ({@code from}) desde la configuración del subsistema mail (nunca del cliente); y por
 * cada {@link AdjuntoCorreo} descarga los bytes del {@link MetaFile} con
 * {@link MetaFileUtil#downloadContent(MetaFile)} para crear un {@link Attach}.
 *
 * <p>El cuerpo del correo es HTML, por lo que se asigna a {@code htmlBody}; {@code textBody} queda
 * vacío. El destinatario es único: {@code to = List.of(emailDestinatario)}.
 */
public class CorreoMailFactory {

    private static final String MIME_TYPE_POR_DEFECTO = "application/octet-stream";

    @Inject
    private RemitenteProvider remitenteProvider;

    /**
     * Devuelve el {@link Mail} listo para {@code MailSender}. Puede lanzar {@link RuntimeException}
     * si un adjunto es ilegible; ese fallo lo captura {@code CorreoServiceImpl} y marca el correo
     * como FALLIDO. El {@code from} NO procede del cliente: lo resuelve {@link RemitenteProvider}.
     */
    public Mail build(Correo correo) {
        String from = remitenteProvider.getFrom();
        List<String> to = List.of(correo.getEmailDestinatario());
        String htmlBody = correo.getCuerpo();
        String textBody = "";

        List<Attach> attachs = construirAdjuntos(correo);

        return new Mail(to, from, correo.getAsunto(), htmlBody, textBody, attachs);
    }

    private List<Attach> construirAdjuntos(Correo correo) {
        List<Attach> attachs = new ArrayList<>();

        if (correo.getAdjuntos() == null) {
            return attachs;
        }

        for (AdjuntoCorreo adjunto : correo.getAdjuntos()) {
            byte[] bytes = MetaFileUtil.downloadContent(adjunto.getContenido());
            String mimeType = resolverMimeType(adjunto.getContenido());
            attachs.add(new Attach(adjunto.getNombreFichero(), bytes, mimeType));
        }

        return attachs;
    }

    private String resolverMimeType(MetaFile metaFile) {
        if (metaFile == null || metaFile.getFileType() == null || metaFile.getFileType().isBlank()) {
            return MIME_TYPE_POR_DEFECTO;
        }
        return metaFile.getFileType();
    }
}
