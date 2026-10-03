package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.EMailUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.ExceptionUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuarioCodigo;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.db.repo.CorreoRepository;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.educaflow.base.util.Convert;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject
    MailSender mailSender;

    @Inject
    EjecutorAsincrono ejecutorAsincrono;

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    @Override
    public Correo insert(Correo correo) {
        validateInsert(correo).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarValoresIniciales(correo);
        correo = repository.save(correo);
        fireActionRule_ProgramarEnvioAsincrono(correo);
        return correo;
    }

    @Override
    public Correo update(Correo nuevo, Correo original) {
        throw new UnsupportedOperationException(I18n.get("El correo es inmutable tras su creación."));
    }

    @Override
    public void remove(Correo correo) {
        // Un correo nunca se puede borrar (patrón gemelo de validateRemove).
        throw new UnsupportedOperationException(I18n.get("Los correos no se pueden borrar."));
    }

    @Override
    public Correo reenviar(Correo entidad, Correo entidadOriginal) {
        validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_ProgramarEnvioAsincrono(entidadOriginal);
        // MUST NOT repository.save aquí: reenviar no cambia ningún campo de forma síncrona;
        // todo el cambio de estado ocurre dentro de enviarCorreo.
        return entidadOriginal;
    }

    @Override
    public List<Correo> listarCorreosEnFail() {
        validateListarCorreosEnFail().ifPresent(BusinessMessages::throwIfInvalid);

        // El finder-method se declaró con all="true" en Main-Correo.xml, así que el repositorio
        // autogenerado devuelve Query<Correo> (para permitir encadenar order/cacheable), no
        // List<Correo> directamente — de ahí el .fetch() final.
        return ((CorreoRepository) repository).findByEstado(EstadoCorreo.FAIL).fetch();
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        BusinessMessages messages = new BusinessMessages();

        validarDniDestinatario(correo, messages);
        validarNombreYApellidos(correo, messages);
        validarDirecciones(correo, messages);
        validarAsuntoYCuerpo(correo, messages);
        validarCentro(correo, messages);
        if (correo.getCentro() != null && correo.getHistorialEstado() != null) {
            validarHistorialEstado(correo, messages);
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarDniDestinatario(Correo correo, BusinessMessages messages) {
        if (correo.getDniDestinatario() == null || correo.getDniDestinatario().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario es obligatorio")));
        } else if (!DniUtil.isValid(correo.getDniDestinatario())) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario no es válido; compruebe la letra")));
        }
    }

    private void validarNombreYApellidos(Correo correo, BusinessMessages messages) {
        if (correo.getNombre() == null || correo.getNombre().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El nombre es obligatorio")));
        }

        if (correo.getApellidos() == null || correo.getApellidos().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("Los apellidos son obligatorios")));
        }
    }

    private void validarDirecciones(Correo correo, BusinessMessages messages) {
        List<String> destinatariosPara = separarDirecciones(correo.getPara());
        if (destinatariosPara.isEmpty()) {
            messages.add(new BusinessMessage(I18n.get("Debe indicar al menos un destinatario en el «para»")));
        } else if (destinatariosPara.stream().anyMatch(direccion -> !EMailUtil.isValid(direccion))) {
            messages.add(new BusinessMessage(I18n.get(
                    "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)")));
        }

        if (correo.getEnCopia() != null && !correo.getEnCopia().isBlank()
                && separarDirecciones(correo.getEnCopia()).stream().anyMatch(direccion -> !EMailUtil.isValid(direccion))) {
            messages.add(new BusinessMessage(I18n.get("El «en copia» debe contener direcciones de correo válidas")));
        }

        if (correo.getEnCopiaOculta() != null && !correo.getEnCopiaOculta().isBlank()
                && separarDirecciones(correo.getEnCopiaOculta()).stream().anyMatch(direccion -> !EMailUtil.isValid(direccion))) {
            messages.add(new BusinessMessage(I18n.get("El «en copia oculta» debe contener direcciones de correo válidas")));
        }
    }

    private void validarAsuntoYCuerpo(Correo correo, BusinessMessages messages) {
        if (correo.getAsunto() == null || correo.getAsunto().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El asunto es obligatorio")));
        } else if (correo.getAsunto().length() > 255) {
            messages.add(new BusinessMessage(I18n.get("El asunto no puede superar 255 caracteres")));
        }

        if (correo.getCuerpo() == null || correo.getCuerpo().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El cuerpo es obligatorio")));
        }
    }

    private void validarCentro(Correo correo, BusinessMessages messages) {
        if (correo.getCentro() == null) {
            messages.add(new BusinessMessage(I18n.get("El centro es obligatorio")));
        } else if (!SecurityUtil.isAdmin(SecurityUtil.getUser())) {
            if (!SecurityUtil.getUser().perteneceAlCentro(correo.getCentro())) {
                messages.add(new BusinessMessage(I18n.get("No puede crear correos para un centro que no es suyo")));
            }
        }
    }

    private void validarHistorialEstado(Correo correo, BusinessMessages messages) {
        // Sin releer por repositorio: JPA.edit ya resolvió la referencia; un padre sin id es una cáscara del cliente.
        HistorialEstado padre = correo.getHistorialEstado();
        if (padre.getId() == null
                || padre.getExpediente() == null
                || !Objects.equals(padre.getExpediente().getCentro(), correo.getCentro())) {
            messages.add(new BusinessMessage(I18n.get("El historial de estado indicado no existe")));
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo nuevo, Correo original) {
        // No hay condición: el correo es inmutable tras su creación, siempre se rechaza.
        return Optional.of(BusinessMessages.single(I18n.get("El correo es inmutable tras su creación.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Correo correo) {
        return Optional.of(BusinessMessages.single(I18n.get("Los correos no se pueden borrar.")));
    }

    private Optional<BusinessMessages> validateEnviarCorreo(Long correoId) {
        // No hay ninguna condición que validar: enviarCorreo no recibe datos del cliente (solo un
        // id) y las comprobaciones sobre el correo (inexistente / ya en SUCCESS) son idempotencia
        // del propio envío, no validaciones de negocio corregibles por el usuario.
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateListarCorreosEnFail() {
        // No hay ninguna condición que validar: la consulta no recibe parámetros.
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal) {
        // Aplica sobre entidadOriginal: el estado real en BD (entidad solo trae el id, ver
        // allowPropertiesReenviar).
        if (entidadOriginal == null) {
            return Optional.of(BusinessMessages.single(I18n.get("Solo se pueden reenviar correos que han fallado")));
        }

        BusinessMessages messages = new BusinessMessages();

        if (entidadOriginal.getEstado() != EstadoCorreo.FAIL) {
            messages.add(new BusinessMessage(I18n.get("Solo se pueden reenviar correos que han fallado")));
        }

        User user = SecurityUtil.getUser();
        Centro centro = entidadOriginal.getCentro();
        if (!SecurityUtil.isAdmin(user)
                && !(user.tieneTipoUsuario(centro, TipoUsuarioCodigo.SUPERVISOR)
                        || user.tieneTipoUsuario(centro, TipoUsuarioCodigo.ADMINISTRATIVO))) {
            messages.add(new BusinessMessage(I18n.get("No puede reenviar correos de un centro que no es suyo")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.ofEntries(
                Map.entry("dniDestinatario", Map.of()),
                Map.entry("nombre", Map.of()),
                Map.entry("apellidos", Map.of()),
                Map.entry("para", Map.of()),
                Map.entry("enCopia", Map.of()),
                Map.entry("enCopiaOculta", Map.of()),
                Map.entry("asunto", Map.of()),
                Map.entry("cuerpo", Map.of()),
                Map.entry("centro", Map.of()),
                Map.entry("historialEstado", Map.of()),
                Map.entry("adjuntos", Map.of(
                        "nombreFichero", Map.of(),
                        "contenido", Map.of()
                ))
        ));
    }

    @Override
    public AllowProperties allowPropertiesReenviar() {
        return AllowProperties.createDenyAllProperties();
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AsignarValoresIniciales(Correo correo) {
        correo.setEstado(EstadoCorreo.PENDIENTE);
        correo.setFechaCreacion(LocalDateTime.now(Convert.defaultZoneId));
        correo.setNumeroReintentos(0);
        correo.setFechaPrimerIntentoEnvio(null);
        correo.setFechaUltimoIntentoEnvio(null);
        correo.setFechaEnvio(null);
        correo.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_ProgramarEnvioAsincrono(Correo correo) {
        // Tras el commit: antes, el hilo del pool puede no ver todavía la fila.
        Long correoId = correo.getId();
        ejecutorAsincrono.ejecutarTrasCommit(() -> this.enviarCorreo(correoId));
    }

    private void fireActionRule_RegistrarIntentoEnvio(Correo correo) {
        correo.setFechaUltimoIntentoEnvio(LocalDateTime.now(Convert.defaultZoneId));
        if (correo.getFechaPrimerIntentoEnvio() == null) {
            // No es el antipatrón de mass-assignment: no depende de lo que mande el cliente
            // (correoId es el único parámetro externo), sino de si YA existe un primer intento en BD.
            correo.setFechaPrimerIntentoEnvio(LocalDateTime.now(Convert.defaultZoneId));
        }
        correo.setNumeroReintentos(correo.getNumeroReintentos() + 1);
    }

    private void fireActionRule_MarcarEnvioCorrecto(Correo correo) {
        correo.setEstado(EstadoCorreo.SUCCESS);
        correo.setFechaEnvio(LocalDateTime.now(Convert.defaultZoneId));
        correo.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_MarcarEnvioFallido(Correo correo, RuntimeException excepcion) {
        correo.setEstado(EstadoCorreo.FAIL);
        correo.setDescripcionUltimoFallo(ExceptionUtil.getTraceAsString(excepcion));
        correo.setFechaEnvio(null); // Nunca hay fecha de envío fuera de SUCCESS.
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/


    private List<String> separarDirecciones(String direcciones) {
        if (direcciones == null || direcciones.isBlank()) {
            return List.of();
        }
        return Arrays.stream(direcciones.split(","))
                .map(String::trim)
                .filter(direccion -> !direccion.isEmpty())
                .toList();
    }

    private Mail construirMail(Correo correo) {
        List<String> to = separarDirecciones(correo.getPara());
        List<String> cc = separarDirecciones(correo.getEnCopia());
        List<String> bcc = separarDirecciones(correo.getEnCopiaOculta());

        String from = AppSettings.get().get("mail.address.from");

        List<Fichero> attachs = correo.getAdjuntos().stream()
                .map(adjunto -> new Fichero(
                        adjunto.getNombreFichero(),
                        MetaFileUtil.downloadContent(adjunto.getContenido()),
                        adjunto.getContenido().getFileType()))
                .toList();

        // El cuerpo es texto plano y puede incrustar texto libre (p. ej. el motivo de subsanación): enviarlo
        // también como HTML dejaría que ese texto se interpretase como marcado en el cliente de correo.
        return new Mail(to, cc, bcc, from, correo.getAsunto(), null, correo.getCuerpo(), attachs);
    }

    void enviarCorreo(Long correoId) {
        validateEnviarCorreo(correoId).ifPresent(BusinessMessages::throwIfInvalid);

        JPA.runInTransaction(() -> {
            // Bloqueo pesimista: dos envíos del mismo correo (dos «Reenviar», o el envío tras el alta y un
            // «Reenviar») pueden correr a la vez en el pool; así el segundo espera al commit del primero y
            // ve SUCCESS en lugar de mandar el correo otra vez.
            Correo correo = JPA.em().find(Correo.class, correoId, LockModeType.PESSIMISTIC_WRITE);
            if (correo == null || correo.getEstado() == EstadoCorreo.SUCCESS) {
                return;
            }

            fireActionRule_RegistrarIntentoEnvio(correo);

            try {
                Mail mail = construirMail(correo);
                mailSender.send(mail);
                fireActionRule_MarcarEnvioCorrecto(correo);
            } catch (RuntimeException ex) {
                fireActionRule_MarcarEnvioFallido(correo, ex);
            }

            repository.save(correo);
        });
    }
}
