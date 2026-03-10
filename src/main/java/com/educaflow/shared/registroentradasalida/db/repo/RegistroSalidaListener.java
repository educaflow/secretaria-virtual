package com.educaflow.shared.registroentradasalida.db.repo;


import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.shared.registroentradasalida.db.RegistroSalida;
import com.google.inject.Inject;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import java.util.ArrayList;
import java.util.List;

public class RegistroSalidaListener {


    private MailSender mailSender;

    public RegistroSalidaListener() {
        mailSender= Beans.get(MailSender.class);
    }


    @PostPersist
    private void onPostPersist(RegistroSalida registroSalida) {
        sendMail(registroSalida);
    }


    @PostUpdate
    private void onPostUpdate(RegistroSalida registroSalida) {
        sendMail(registroSalida);
    }

    @PostRemove
    private void onPostRemove(RegistroSalida registroSalida) {

    }


    /***************** Funciones de negocio *******************/
    /**** BEGIN ****/

    private void sendMail(RegistroSalida registroSalida) {
        List<Attach> attachs = createAttachFromMetaFiles(registroSalida.getAnexos());
        attachs.add(createAttachFromMetaFile(registroSalida.getDocumento()));
        String subject = "Nuevo Registro de Salida Nº " + registroSalida.getNumeroRegistro();
        String body = "Se ha creado un nuevo registro de salida con número " + registroSalida.getNumeroRegistro() + " en el centro " + registroSalida.getCentro().getName();
        Mail mail = new Mail(List.of("lorenzo.profesor@gmail.com"), "secretariavirtual@fpmislata.com", subject, body, body, attachs);

        mailSender.send(mail);
    }




    /***************** Funciones de utilidad *******************/
    /**** BEGIN ****/

    private Attach createAttachFromMetaFile(MetaFile metaFile) {
        return new Attach(metaFile.getFileName(), MetaFileUtil.downloadContent(metaFile), metaFile.getFileType());
    }

    private List<Attach> createAttachFromMetaFiles(List<MetaFile> metaFiles) {
        List<Attach> attachs=new ArrayList<>();

        for(MetaFile metaFile:metaFiles) {
            attachs.add(createAttachFromMetaFile(metaFile));
        }

        return attachs;
    }
}
