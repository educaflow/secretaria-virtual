package com.educaflow.subsystem.correos.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.infrastructure.mail.impl.SmtpCredentialSimplePassword;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.service.CorreoService;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject
    MailSender mailSender;

    @Inject
    SmtpCredentialSimplePassword smtpCredentialSimplePassword;

    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    @Override
    public Correo insert(Correo correo) {
        correo.setDe(smtpCredentialSimplePassword.userName());
        correo.setEstado(EstadoCorreo.PENDIENTE);
        fireActionRule_AutoResolverUsuario(correo);
        correo = super.insert(correo);

        final Long correoId = correo.getId();
        Thread thread = new Thread(() -> fireActionRule_EnviarCorreo(correoId));
        thread.setDaemon(true);
        thread.start();

        return correo;
    }

    @Override
    public void reenviar(Correo correo) {
        final Long correoId = correo.getId();
        Thread thread = new Thread(() -> fireActionRule_EnviarCorreo(correoId));
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal) {
        return Optional.empty();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AutoResolverUsuario(Correo correo) {
        if (correo.getDni() == null || correo.getDni().isBlank()) {
            return;
        }
        User usuario = JpaRepository.of(User.class).all()
                .filter("self.dni = :dni")
                .bind("dni", correo.getDni())
                .fetchOne();
        correo.setUsuario(usuario);
    }

    private void fireActionRule_EnviarCorreo(Long correoId) {
        // Este método se ejecuta en un hilo background con su propia transacción JPA.
        try {
            JPA.em().getTransaction().begin();
            Correo correo = JpaRepository.of(Correo.class).find(correoId);
            try {
                Mail mail = buildMail(correo);
                mailSender.send(mail);
                correo.setEstado(EstadoCorreo.ENVIADO);
                correo.setEnviadoEn(LocalDateTime.now());
            } catch (Exception e) {
                correo.setEstado(EstadoCorreo.ERROR);
                correo.setUltimoFalloEn(LocalDateTime.now());
            }
            JpaRepository.of(Correo.class).save(correo);
            JPA.em().getTransaction().commit();
        } catch (Exception e) {
            if (JPA.em().getTransaction().isActive()) {
                JPA.em().getTransaction().rollback();
            }
        } finally {
            JPA.em().close();
        }
    }

    private Mail buildMail(Correo correo) {
        List<Attach> attachs = new ArrayList<>();
        if (correo.getAdjuntos() != null) {
            for (MetaFile metaFile : correo.getAdjuntos()) {
                byte[] data = MetaFileUtil.downloadContent(metaFile);
                attachs.add(new Attach(metaFile.getFileName(), data, metaFile.getFileType()));
            }
        }
        return new Mail(
            List.of(correo.getPara()),
            correo.getDe(),
            correo.getAsunto(),
            correo.getCuerpoHtml(),
            correo.getCuerpoTexto(),
            attachs
        );
    }
}