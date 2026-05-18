package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.subsystem.correos.db.AdjuntoCorreo;
import com.educaflow.subsystem.correos.db.EstadoTareaCorreo;
import com.educaflow.subsystem.correos.db.TareaCorreo;
import com.educaflow.subsystem.correos.db.repo.TareaCorreoRepository;
import com.educaflow.subsystem.correos.service.TareaCorreoService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class TareaCorreoServiceImpl extends DefaultModelService<TareaCorreo> implements TareaCorreoService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public TareaCorreoServiceImpl(Class<TareaCorreo> model, Repository<TareaCorreo> repository) {
        super(model, repository);
    }

    /****************************************************************************************/
    /******************************** Métodos públicos *************************************/
    /****************************************************************************************/

    @Override
    public TareaCorreo insert(TareaCorreo entidad) {
        fireActionRule_inicializarEstado(entidad);
        fireActionRule_asignarCentroDelUsuario(entidad);
        fireActionRule_copiarAdjuntos(entidad);
        entidad = super.insert(entidad);
        return entidad;
    }

    @Override
    public TareaCorreo procesarEnvio(TareaCorreo tareaCorreo) {
        fireActionRule_marcarEnviando(tareaCorreo);
        try {
            MailSender mailSender = Beans.get(MailSender.class);
            Mail mail = construirMail(tareaCorreo);
            mailSender.send(mail);
            fireActionRule_marcarEnviado(tareaCorreo);
        } catch (Exception e) {
            fireActionRule_marcarFallado(tareaCorreo, e.getMessage());
        }
        return tareaCorreo;
    }

    @Override
    public TareaCorreo reenviar(TareaCorreo tareaCorreo, TareaCorreo tareaCorreoOriginal) {
        if (tareaCorreoOriginal == null || tareaCorreoOriginal.getEstado() != EstadoTareaCorreo.FALLADO) {
            throw new IllegalStateException(I18n.get("Solo se puede reenviar un correo en estado FALLADO"));
        }
        fireActionRule_volverAPendiente(tareaCorreo);
        return super.update(tareaCorreo, tareaCorreoOriginal);
    }

    @Override
    public List<TareaCorreo> obtenerPendientes() {
        return Beans.get(TareaCorreoRepository.class).findPendientes().fetch();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(TareaCorreo entidad) {
        BusinessMessages businessMessages = new BusinessMessages();

        // V-TareaCorreo-005: formato de email
        String email = entidad.getEmailDestinatario();
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            businessMessages.add(new BusinessMessage("emailDestinatario",
                    I18n.get("La dirección de correo '%s' no tiene un formato válido. Se espera 'usuario@dominio'.")
                            .formatted(email)));
        }

        if (businessMessages.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(businessMessages);
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(TareaCorreo entidad, TareaCorreo original) {
        BusinessMessages businessMessages = new BusinessMessages();

        if (original != null) {
            boolean cambioInmutable =
                    !Objects.equals(entidad.getAsunto(), original.getAsunto())
                    || !Objects.equals(entidad.getCuerpo(), original.getCuerpo())
                    || !Objects.equals(entidad.getDniDestinatario(), original.getDniDestinatario())
                    || !Objects.equals(entidad.getEmailDestinatario(), original.getEmailDestinatario())
                    || !Objects.equals(idOf(entidad.getCentro()), idOf(original.getCentro()))
                    || !Objects.equals(idOf(entidad.getHistorialEstado()), idOf(original.getHistorialEstado()));

            if (cambioInmutable) {
                businessMessages.add(new BusinessMessage("",
                        I18n.get("Las tareas de correo son un registro histórico inmutable y no pueden modificarse después de creadas.")));
            }
        }

        if (businessMessages.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(businessMessages);
        }
    }

    @Override
    public Optional<BusinessMessages> validateRemove(TareaCorreo entidad) {
        BusinessMessages businessMessages = new BusinessMessages();
        businessMessages.add(new BusinessMessage("",
                I18n.get("Las tareas de correo son un registro histórico inmutable y no pueden borrarse.")));
        return Optional.of(businessMessages);
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_inicializarEstado(TareaCorreo entidad) {
        // R-TareaCorreo-001
        if (entidad.getId() == null) {
            entidad.setEstado(EstadoTareaCorreo.PENDIENTE);
            entidad.setNumeroIntentos(0);
            entidad.setFechaCreacion(LocalDateTime.now());
            entidad.setFechaUltimoIntento(null);
            entidad.setMotivoFallo(null);
        }
    }

    private void fireActionRule_asignarCentroDelUsuario(TareaCorreo entidad) {
        // R-TareaCorreo-002
        if (entidad.getCentro() == null) {
            com.axelor.auth.db.User usuario = AuthUtils.getUser();
            if (usuario != null && usuario.getCentroActivo() != null) {
                entidad.setCentro(usuario.getCentroActivo());
            }
        }
    }

    private void fireActionRule_copiarAdjuntos(TareaCorreo entidad) {
        // R-TareaCorreo-003
        if (entidad.getAdjuntos() != null) {
            for (AdjuntoCorreo adjunto : entidad.getAdjuntos()) {
                if (adjunto.getId() == null && adjunto.getContenidoFichero() != null) {
                    adjunto.setContenidoFichero(MetaFileUtil.cloneMetaFile(adjunto.getContenidoFichero()));
                }
                adjunto.setTareaCorreo(entidad);
            }
        }
    }

    private void fireActionRule_marcarEnviando(TareaCorreo entidad) {
        // R-TareaCorreo-004
        entidad.setEstado(EstadoTareaCorreo.ENVIANDO);
        Integer intentos = entidad.getNumeroIntentos();
        entidad.setNumeroIntentos((intentos == null ? 0 : intentos) + 1);
        entidad.setFechaUltimoIntento(LocalDateTime.now());
        persistir(entidad);
    }

    private void fireActionRule_marcarEnviado(TareaCorreo entidad) {
        // R-TareaCorreo-006
        entidad.setEstado(EstadoTareaCorreo.ENVIADO);
        persistir(entidad);
    }

    private void fireActionRule_marcarFallado(TareaCorreo entidad, String motivo) {
        // R-TareaCorreo-007
        entidad.setEstado(EstadoTareaCorreo.FALLADO);
        entidad.setMotivoFallo(motivo);
        persistir(entidad);
    }

    private void fireActionRule_volverAPendiente(TareaCorreo entidad) {
        // R-TareaCorreo-008
        entidad.setEstado(EstadoTareaCorreo.PENDIENTE);
        entidad.setMotivoFallo(null);
    }

    /*************************************************************************************/
    /********************************    Helpers internos    *****************************/
    /*************************************************************************************/

    private void persistir(TareaCorreo entidad) {
        JPA.em().merge(entidad);
        JPA.flush();
    }

    private Long idOf(com.axelor.db.Model m) {
        return m == null ? null : m.getId();
    }

    private Mail construirMail(TareaCorreo tareaCorreo) {
        AppSettings settings = AppSettings.get();
        String from = settings.get("mail.smtp.user");

        List<Attach> attachs = new ArrayList<>();
        if (tareaCorreo.getAdjuntos() != null) {
            for (AdjuntoCorreo adjunto : tareaCorreo.getAdjuntos()) {
                MetaFile mf = adjunto.getContenidoFichero();
                if (mf == null) {
                    continue;
                }
                byte[] data = MetaFileUtil.downloadContent(mf);
                String mime = mf.getFileType() != null ? mf.getFileType() : "application/octet-stream";
                attachs.add(new Attach(adjunto.getNombreFichero(), data, mime));
            }
        }

        return new Mail(
                List.of(tareaCorreo.getEmailDestinatario()),
                from,
                tareaCorreo.getAsunto(),
                tareaCorreo.getCuerpo(),
                null,
                attachs
        );
    }
}
