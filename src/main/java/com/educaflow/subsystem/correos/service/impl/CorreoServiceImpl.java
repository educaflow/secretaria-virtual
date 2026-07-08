package com.educaflow.subsystem.correos.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.mail.Attach;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.EMailUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.correos.db.Correo;
import com.educaflow.subsystem.correos.db.EstadoCorreo;
import com.educaflow.subsystem.correos.db.repo.CorreoRepository;
import com.educaflow.subsystem.correos.infrastructure.CorreoAsyncExecutor;
import com.educaflow.subsystem.correos.infrastructure.PostCommitRunner;
import com.educaflow.subsystem.correos.service.CorreoService;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import jakarta.inject.Inject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService {

    @Inject
    MailSender mailSender;

    @Inject
    CorreoAsyncExecutor correoAsyncExecutor;

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
        // El correo es inmutable tras su creación (V-Correo-015 / patrón gemelo de validateUpdate,
        // k-secure-coding §9.2): no hay flujo normal, solo el rechazo incondicional.
        throw new UnsupportedOperationException(I18n.get("El correo es inmutable tras su creación."));
    }

    @Override
    public void remove(Correo correo) {
        // RES-Correo-003: un correo nunca se puede borrar (patrón gemelo de validateRemove).
        throw new UnsupportedOperationException(I18n.get("Los correos no se pueden borrar."));
    }

    @Override
    public Correo reenviar(Correo entidad, Correo entidadOriginal) {
        validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_ProgramarReenvioAsincrono(entidadOriginal);
        // MUST NOT repository.save aquí: reenviar no cambia ningún campo de forma síncrona (ver
        // design/rules/R-Correo-002.md) — todo el cambio de estado ocurre dentro de enviarCorreo.
        return entidadOriginal;
    }

    @Override
    public void enviarCorreo(Long correoId) {
        JPA.runInTransaction(() -> {
            Correo correo = repository.find(correoId);
            if (correo == null || correo.getEstado() == EstadoCorreo.SUCCESS) {
                // Defensivo (correo borrado/inexistente) e idempotencia (SUCCESS es terminal).
                return;
            }

            fireActionRule_RegistrarIntentoEnvio(correo);
            Mail mail = construirMail(correo);

            try {
                mailSender.send(mail);
                fireActionRule_MarcarEnvioCorrecto(correo);
            } catch (RuntimeException ex) {
                fireActionRule_MarcarEnvioFallido(correo, ex);
            }

            repository.save(correo);
        });
    }

    @Override
    public List<Correo> listarCorreosEnFail() {
        // El finder-method se declaró con all="true" en Correo.xml, así que el repositorio
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
        validarHistorialEstado(correo, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarDniDestinatario(Correo correo, BusinessMessages messages) {
        // V-Correo-001 (VAL-Correo-001) dniDestinatario obligatorio
        if (correo.getDniDestinatario() == null || correo.getDniDestinatario().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario es obligatorio")));
        } else if (!DniUtil.isValid(correo.getDniDestinatario())) {
            // V-Correo-002 (VAL-Correo-015) dniDestinatario válido — solo si el anterior pasó
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario no es válido; compruebe la letra")));
        }
    }

    private void validarNombreYApellidos(Correo correo, BusinessMessages messages) {
        // V-Correo-003 (VAL-Correo-006) nombre obligatorio
        if (correo.getNombre() == null || correo.getNombre().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El nombre es obligatorio")));
        }

        // V-Correo-004 (VAL-Correo-007) apellidos obligatorios
        if (correo.getApellidos() == null || correo.getApellidos().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("Los apellidos son obligatorios")));
        }
    }

    private void validarDirecciones(Correo correo, BusinessMessages messages) {
        // V-Correo-005 (VAL-Correo-002) para: al menos una dirección tras separar por comas
        List<String> destinatariosPara = separarDirecciones(correo.getPara());
        if (destinatariosPara.isEmpty()) {
            messages.add(new BusinessMessage(I18n.get("Debe indicar al menos un destinatario en el «para»")));
        } else if (destinatariosPara.stream().anyMatch(direccion -> !EMailUtil.isValid(direccion))) {
            // V-Correo-006 (VAL-Correo-011) cada dirección de "para" tiene formato válido
            messages.add(new BusinessMessage(I18n.get(
                    "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)")));
        }

        // V-Correo-007 (VAL-Correo-012) si "enCopia" tiene valor, cada dirección válida
        if (correo.getEnCopia() != null && !correo.getEnCopia().isBlank()
                && separarDirecciones(correo.getEnCopia()).stream().anyMatch(direccion -> !EMailUtil.isValid(direccion))) {
            messages.add(new BusinessMessage(I18n.get("El «en copia» debe contener direcciones de correo válidas")));
        }

        // V-Correo-008 (VAL-Correo-013) si "enCopiaOculta" tiene valor, cada dirección válida
        if (correo.getEnCopiaOculta() != null && !correo.getEnCopiaOculta().isBlank()
                && separarDirecciones(correo.getEnCopiaOculta()).stream().anyMatch(direccion -> !EMailUtil.isValid(direccion))) {
            messages.add(new BusinessMessage(I18n.get("El «en copia oculta» debe contener direcciones de correo válidas")));
        }
    }

    private void validarAsuntoYCuerpo(Correo correo, BusinessMessages messages) {
        // V-Correo-009 (VAL-Correo-003) asunto obligatorio
        if (correo.getAsunto() == null || correo.getAsunto().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El asunto es obligatorio")));
        } else if (correo.getAsunto().length() > 255) {
            // V-Correo-010 (VAL-Correo-016) asunto <= 255 caracteres
            messages.add(new BusinessMessage(I18n.get("El asunto no puede superar 255 caracteres")));
        }

        // V-Correo-011 (VAL-Correo-004) cuerpo obligatorio
        if (correo.getCuerpo() == null || correo.getCuerpo().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El cuerpo es obligatorio")));
        }
    }

    private void validarCentro(Correo correo, BusinessMessages messages) {
        // V-Correo-012 (VAL-Correo-005) centro obligatorio
        if (correo.getCentro() == null) {
            messages.add(new BusinessMessage(I18n.get("El centro es obligatorio")));
        } else if (!SecurityUtil.isAdmin(SecurityUtil.getUser())) {
            // V-Correo-013 (VAL-Correo-008) si no es Administrador, el centro indicado MUST ser
            // uno de los centros del usuario
            if (!perteneceAlCentroDelUsuario(correo.getCentro())) {
                messages.add(new BusinessMessage(I18n.get("No puede crear correos para un centro que no es suyo")));
            }
        }
    }

    private void validarHistorialEstado(Correo correo, BusinessMessages messages) {
        // V-Correo-014 (VAL-Correo-014) si "historialEstado" tiene valor, comprobar que existe de verdad
        if (correo.getHistorialEstado() != null
                && JpaRepository.of(HistorialEstado.class).find(correo.getHistorialEstado().getId()) == null) {
            messages.add(new BusinessMessage(I18n.get("El historial de estado indicado no existe")));
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Correo nuevo, Correo original) {
        // V-Correo-015: no hay condición — el correo es inmutable tras su creación, siempre se rechaza.
        return Optional.of(BusinessMessages.single(I18n.get("El correo es inmutable tras su creación.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Correo correo) {
        // V-Correo-016 (RES-Correo-003): siempre se rechaza.
        return Optional.of(BusinessMessages.single(I18n.get("Los correos no se pueden borrar.")));
    }

    @Override
    public Optional<BusinessMessages> validateReenviar(Correo entidad, Correo entidadOriginal) {
        // Aplica sobre entidadOriginal: el estado real en BD (entidad solo trae el id, ver
        // allowPropertiesReenviar).
        BusinessMessages messages = new BusinessMessages();

        // V-Correo-017 (VAL-Correo-009) solo se pueden reenviar correos en FAIL
        if (entidadOriginal.getEstado() != EstadoCorreo.FAIL) {
            messages.add(new BusinessMessage(I18n.get("Solo se pueden reenviar correos que han fallado")));
        }

        // V-Correo-018 (VAL-Correo-010) si no es Administrador, el centro del correo MUST estar
        // entre los centros del usuario (mismo mecanismo que V-Correo-013)
        if (entidadOriginal.getCentro() != null && !SecurityUtil.isAdmin(SecurityUtil.getUser())
                && !perteneceAlCentroDelUsuario(entidadOriginal.getCentro())) {
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
        // Whitelist vacía: reenviar no acepta ningún dato del cliente más allá del id (que
        // ActionRequestHelper resuelve siempre, con independencia de la whitelist).
        return AllowProperties.createAllowProperties(Map.of());
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AsignarValoresIniciales(Correo correo) {
        // R-Correo-003 — asignación INCONDICIONAL de campos servidor (k-secure-coding §3.3): el
        // cliente NO puede dictar estos campos aunque vengan rellenos en el JSON de entrada.
        correo.setEstado(EstadoCorreo.PENDIENTE);
        correo.setFechaCreacion(LocalDateTime.now());
        correo.setNumeroReintentos(0);
        correo.setFechaPrimerIntentoEnvio(null);
        correo.setFechaUltimoIntentoEnvio(null);
        correo.setFechaEnvio(null);
        correo.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_ProgramarEnvioAsincrono(Correo correo) {
        // R-Correo-001 — ver design/rules/R-Correo-001.md. El envío se programa para ejecutarse
        // tras el commit de la transacción actual (la fila puede no ser visible todavía para el
        // hilo del executor si se sometiera antes del commit).
        Long correoId = correo.getId();
        PostCommitRunner.runAfterCommit(
                () -> correoAsyncExecutor.submit(() -> this.enviarCorreo(correoId)));
    }

    private void fireActionRule_ProgramarReenvioAsincrono(Correo correo) {
        // R-Correo-002 — ver design/rules/R-Correo-002.md (mecanismo compartido con R-Correo-001).
        Long correoId = correo.getId();
        PostCommitRunner.runAfterCommit(
                () -> correoAsyncExecutor.submit(() -> this.enviarCorreo(correoId)));
    }

    private void fireActionRule_RegistrarIntentoEnvio(Correo correo) {
        // R-Correo-004 (CC-Correo-002/003/005) — asignación INCONDICIONAL.
        correo.setFechaUltimoIntentoEnvio(LocalDateTime.now());
        if (correo.getFechaPrimerIntentoEnvio() == null) {
            // No es el antipatrón de mass-assignment: no depende de lo que mande el cliente
            // (correoId es el único parámetro externo), sino de si YA existe un primer intento en BD.
            correo.setFechaPrimerIntentoEnvio(LocalDateTime.now());
        }
        correo.setNumeroReintentos(correo.getNumeroReintentos() + 1);
    }

    private void fireActionRule_MarcarEnvioCorrecto(Correo correo) {
        // R-Correo-004 (RES-Correo-002: fechaEnvio solo con SUCCESS) — asignación INCONDICIONAL.
        correo.setEstado(EstadoCorreo.SUCCESS);
        correo.setFechaEnvio(LocalDateTime.now());
        correo.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_MarcarEnvioFallido(Correo correo, RuntimeException excepcion) {
        // R-Correo-004 (CC-Correo-006) — asignación INCONDICIONAL.
        correo.setEstado(EstadoCorreo.FAIL);
        correo.setDescripcionUltimoFallo(trazaCompleta(excepcion));
        correo.setFechaEnvio(null); // RES-Correo-002: nunca hay fecha de envío fuera de SUCCESS
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private boolean perteneceAlCentroDelUsuario(Centro centro) {
        return SecurityUtil.getUser().getCentroUsuarios() != null
                && SecurityUtil.getUser().getCentroUsuarios().stream()
                        .anyMatch(centroUsuario -> centroUsuario.getCentro() != null
                                && centroUsuario.getCentro().getId().equals(centro.getId()));
    }

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

        List<Attach> attachs = correo.getAdjuntos().stream()
                .map(adjunto -> new Attach(
                        adjunto.getNombreFichero(),
                        MetaFileUtil.downloadContent(adjunto.getContenido()),
                        adjunto.getContenido().getFileType()))
                .toList();

        return new Mail(to, cc, bcc, from, correo.getAsunto(), correo.getCuerpo(), correo.getCuerpo(), attachs);
    }

    private String trazaCompleta(Throwable excepcion) {
        StringWriter sw = new StringWriter();
        excepcion.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
