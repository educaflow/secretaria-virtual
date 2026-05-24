package com.educaflow.subsystem.correos.service.impl;

import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.correos.db.AdjuntoCorreo;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.db.repo.CorreoRepository;
import com.educaflow.subsystem.correos.service.CorreoInsertDTO;
import com.educaflow.subsystem.correos.service.CorreoService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    private static final Logger logger = LoggerFactory.getLogger(CorreoServiceImpl.class);

    private static final int MAX_LONGITUD_MOTIVO = 4000;

    @Inject
    private MailSender mailSender;

    @Inject
    private CorreoMailFactory correoMailFactory;

    @Inject
    private UserRepository userRepository;

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    @Override
    public Correo insert(Correo correo) {
        validateInsert(correo).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_InicializarCorreo(correo);
        fireActionRule_AltaManualSinCentro(correo);
        return repository.save(correo);
    }

    @Override
    public Correo insert(CorreoInsertDTO dto) {
        validateInsert(dto).ifPresent(BusinessMessages::throwIfInvalid);

        Correo correo = new Correo();
        correo.setAsunto(dto.asunto());
        correo.setCuerpo(dto.cuerpo());
        correo.setDniDestinatario(dto.dniDestinatario());
        correo.setEmailDestinatario(dto.emailDestinatario());
        correo.setAdjuntos(construirAdjuntos(dto, correo));

        fireActionRule_InicializarCorreo(correo);
        fireActionRule_AltaProgramaticaCentroYReferencia(correo, dto);
        return repository.save(correo);
    }

    @Override
    public Correo update(Correo correo, Correo correoOriginal) {
        // El correo es inmutable tras su creación (k-secure-coding §9.2): no hay flujo normal.
        throw new UnsupportedOperationException(
                I18n.get("El correo es inmutable tras su creación y no admite modificación."));
    }

    @Override
    public void remove(Correo correo) {
        validateRemove(correo).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_BorrarAdjuntos(correo);
        repository.remove(correo);
    }

    @Override
    public Correo reenviar(Correo correo, Correo correoOriginal) {
        validateReenviar(correo, correoOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_ReactivarCorreo(correo);
        return repository.save(correo);
    }

    @Override
    public void enviarCorreosPendientes() {
        // Un fallo al obtener la lista se propaga fuera (lo captura el Job).
        List<Correo> pendientes = ((CorreoRepository) repository).findByEstado(EstadoCorreo.PENDIENTE).fetch();

        for (Correo correo : pendientes) {
            procesarCorreo(correo);
        }
    }

    @Override
    public String proponerEmailPorDni(String dni) {
        if (dni == null) {
            return null;
        }
        User user = userRepository.findByDni(dni);
        return user == null ? null : user.getEmail();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        BusinessMessages messages = new BusinessMessages();

        if (esVacio(correo.getAsunto())) {
            messages.add(new BusinessMessage("asunto",
                    I18n.get("El asunto es obligatorio."),
                    tituloCampo("asunto")));
        }
        if (esVacio(correo.getCuerpo())) {
            messages.add(new BusinessMessage("cuerpo",
                    I18n.get("El cuerpo es obligatorio."),
                    tituloCampo("cuerpo")));
        }
        if (esVacio(correo.getDniDestinatario())) {
            messages.add(new BusinessMessage("dniDestinatario",
                    I18n.get("El DNI del destinatario es obligatorio."),
                    tituloCampo("dniDestinatario")));
        }
        if (esVacio(correo.getEmailDestinatario())) {
            messages.add(new BusinessMessage("emailDestinatario",
                    I18n.get("El email del destinatario es obligatorio."),
                    tituloCampo("emailDestinatario")));
        }
        if (correo.getReferenciaHistorialEstadoExpediente() != null) {
            messages.add(new BusinessMessage("referenciaHistorialEstadoExpediente",
                    I18n.get("La referencia al historial del expediente no puede asignarse en el alta manual."),
                    tituloCampo("referenciaHistorialEstadoExpediente")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateInsert(CorreoInsertDTO dto) {
        BusinessMessages messages = new BusinessMessages();

        if (esVacio(dto.asunto())) {
            messages.add(new BusinessMessage("asunto", I18n.get("El asunto es obligatorio.")));
        }
        if (esVacio(dto.cuerpo())) {
            messages.add(new BusinessMessage("cuerpo", I18n.get("El cuerpo es obligatorio.")));
        }
        if (esVacio(dto.dniDestinatario())) {
            messages.add(new BusinessMessage("dniDestinatario",
                    I18n.get("El DNI del destinatario es obligatorio.")));
        }
        if (esVacio(dto.emailDestinatario())) {
            messages.add(new BusinessMessage("emailDestinatario",
                    I18n.get("El email del destinatario es obligatorio.")));
        }
        if (dto.centro() == null) {
            messages.add(new BusinessMessage("centro",
                    I18n.get("El centro es obligatorio en el alta programática.")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo correo, Correo correoOriginal) {
        // La entidad nunca admite update: siempre se rechaza (k-secure-coding §9.2).
        return Optional.of(BusinessMessages.single(
                I18n.get("El correo ya creado no admite modificación de sus datos de envío.")));
    }

    @Override
    public Optional<BusinessMessages> validateReenviar(Correo correo, Correo correoOriginal) {
        BusinessMessages messages = new BusinessMessages();

        if (SecurityUtil.isAdmin(SecurityUtil.getUser()) == false) {
            messages.add(new BusinessMessage(
                    I18n.get("Solo un administrador puede reenviar un correo.")));
            return Optional.of(messages);
        }

        if (correo.getEstado() != EstadoCorreo.FALLIDO) {
            messages.add(new BusinessMessage("estado", I18n.get(
                    "El correo solo se puede reenviar cuando su estado es FALLIDO; su estado actual es ")
                    + estadoComoTexto(correo.getEstado()) + "."));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "asunto", Map.of(),
                "cuerpo", Map.of(),
                "dniDestinatario", Map.of(),
                "emailDestinatario", Map.of(),
                "adjuntos", Map.of(
                        "nombreFichero", Map.of(),
                        "contenido", Map.of())
        ));
    }

    @Override
    public AllowProperties allowPropertiesReenviar() {
        return AllowProperties.createAllowProperties(Map.of());
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    // R-Correo-001: inicializa los campos de control del correo en el alta (incondicional).
    private void fireActionRule_InicializarCorreo(Correo correo) {
        correo.setEstado(EstadoCorreo.PENDIENTE);
        correo.setFechaCreacion(LocalDateTime.now());
        correo.setNumeroIntentos(0);
        correo.setFechaEnvio(null);
        correo.setFechaUltimoIntento(null);
        correo.setMotivoUltimoFallo(null);
    }

    // R-Correo-002: el alta manual nunca lleva centro (incondicional).
    private void fireActionRule_AltaManualSinCentro(Correo correo) {
        correo.setCentro(null);
    }

    // R-Correo-003: el alta programática toma centro y referencia del DTO (incondicional).
    private void fireActionRule_AltaProgramaticaCentroYReferencia(Correo correo, CorreoInsertDTO dto) {
        correo.setCentro(dto.centro());
        correo.setReferenciaHistorialEstadoExpediente(dto.referenciaHistorial());
    }

    // R-Correo-005: reactiva un correo FALLIDO dejándolo PENDIENTE (incondicional).
    private void fireActionRule_ReactivarCorreo(Correo correo) {
        correo.setEstado(EstadoCorreo.PENDIENTE);
    }

    // R-Correo-006: registra el intento ANTES del envío y persiste para dejar rastro (incondicional).
    private void fireActionRule_RegistrarIntento(Correo correo) {
        correo.setNumeroIntentos(correo.getNumeroIntentos() + 1);
        correo.setFechaUltimoIntento(LocalDateTime.now());
        repository.save(correo);
    }

    // R-Correo-007: registra el resultado del envío y persiste; no reintenta (incondicional).
    private void fireActionRule_RegistrarResultadoEnvio(Correo correo, boolean exito, String motivo) {
        if (exito) {
            correo.setEstado(EstadoCorreo.ENVIADO);
            correo.setFechaEnvio(LocalDateTime.now());
            correo.setMotivoUltimoFallo(null);
        } else {
            correo.setEstado(EstadoCorreo.FALLIDO);
            correo.setMotivoUltimoFallo(motivo);
        }
        repository.save(correo);
    }

    // R-Correo-008: borra en cascada los adjuntos del correo antes de eliminarlo (incondicional).
    private void fireActionRule_BorrarAdjuntos(Correo correo) {
        if (correo.getAdjuntos() == null) {
            return;
        }
        for (AdjuntoCorreo adjunto : List.copyOf(correo.getAdjuntos())) {
            JPA.remove(adjunto);
        }
        correo.getAdjuntos().clear();
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    /**
     * Procesa un único correo de forma aislada: su propia transacción y su propio try/catch, de
     * modo que el fallo de un correo no aborta el procesamiento de los demás (R-Correo-006).
     */
    private void procesarCorreo(Correo correo) {
        Long id = correo.getId();

        // Transacción propia: el incremento del intento se persiste ANTES del envío para que el
        // rastro sobreviva aunque el envío se interrumpa (R-Correo-006, idempotencia).
        JPA.runInTransaction(() -> fireActionRule_RegistrarIntento(correo));

        try {
            JPA.runInTransaction(() -> {
                Mail mail = correoMailFactory.build(correo);
                mailSender.send(mail);
                fireActionRule_RegistrarResultadoEnvio(correo, true, null);
            });
            logger.info("Correo enviado id={} estado={}", id, EstadoCorreo.ENVIADO);
        } catch (Exception ex) {
            String motivo = sanearMotivo(ex.getMessage());
            try {
                JPA.runInTransaction(() -> fireActionRule_RegistrarResultadoEnvio(correo, false, motivo));
            } catch (Exception persistEx) {
                logger.error("No se pudo persistir el fallo del correo id={}", id);
            }
            logger.warn("Correo fallido id={} estado={}", id, EstadoCorreo.FALLIDO);
        }
    }

    private List<AdjuntoCorreo> construirAdjuntos(CorreoInsertDTO dto, Correo correo) {
        if (dto.adjuntos() == null) {
            return new ArrayList<>();
        }
        List<AdjuntoCorreo> adjuntos = new ArrayList<>();
        for (CorreoInsertDTO.AdjuntoCorreoInsertDTO adjuntoDto : dto.adjuntos()) {
            AdjuntoCorreo adjunto = new AdjuntoCorreo();
            adjunto.setNombreFichero(adjuntoDto.nombreFichero());
            adjunto.setContenido(adjuntoDto.contenido());
            adjunto.setCorreo(correo);
            adjuntos.add(adjunto);
        }
        return adjuntos;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String tituloCampo(String campo) {
        return I18n.get(Mapper.of(Correo.class).getProperty(campo).getTitle());
    }

    private String estadoComoTexto(EstadoCorreo estado) {
        return estado == null ? "" : estado.name();
    }

    private String sanearMotivo(String motivo) {
        if (motivo == null) {
            return I18n.get("Error desconocido durante el envío del correo.");
        }
        String saneado = motivo.replace("\r", " ").replace("\n", " ");
        if (saneado.length() > MAX_LONGITUD_MOTIVO) {
            saneado = saneado.substring(0, MAX_LONGITUD_MOTIVO);
        }
        return saneado;
    }
}
