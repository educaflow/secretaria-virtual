package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.infrastructure.fichero.Fichero;
import com.educaflow.base.infrastructure.mail.Mail;
import com.educaflow.base.infrastructure.mail.MailSender;
import com.educaflow.base.util.EMailUtil;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.db.Correo;
import com.educaflow.subsystem.notificaciones.service.CorreoService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CorreoServiceImpl extends NotificacionServiceImpl<Correo> implements CorreoService {

    private static final int LONGITUD_MAXIMA_ASUNTO = 255;
    private static final long TAMANO_MAXIMO_ADJUNTOS = 25L * 1024 * 1024;
    private static final Pattern CARACTER_DE_CONTROL = Pattern.compile("\\p{Cntrl}");

    // Provider: una instalación sin servidor de correo debe dejar el correo FALLIDO al enviar, no impedir crear el servicio.
    @Inject
    Provider<MailSender> mailSenderProvider;

    public CorreoServiceImpl(Class<Correo> model, Repository<Correo> repository) {
        super(model, repository);
    }

    @Override
    protected void antesDeGuardar(Correo correo) {
        fireActionRule_CopiarAdjuntos(correo);
    }

    @Override
    protected void enviar(Correo correo) {
        mailSenderProvider.get().send(construirMail(correo));
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Correo correo) {
        BusinessMessages messages = validateDatosComunes(correo);

        validarDirecciones(correo, messages);
        validarAsuntoYCuerpo(correo, messages);
        validarTamanoTotalAdjuntos(correo, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarDirecciones(Correo correo, BusinessMessages messages) {
        List<String> destinatariosPara = separarDirecciones(correo.getPara());
        if (destinatariosPara.isEmpty()) {
            messages.add(new BusinessMessage(I18n.get("Debe indicar al menos un destinatario en el «para»")));
        } else if (destinatariosPara.size() > 1) {
            messages.add(new BusinessMessage(I18n.get(
                    "El «para» debe contener una sola dirección de correo; use «en copia» para añadir más destinatarios")));
        } else if (EMailUtil.isValid(destinatariosPara.get(0)) == false) {
            messages.add(new BusinessMessage(I18n.get(
                    "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)")));
        }

        if (contieneDireccionNoValida(correo.getEnCopia())) {
            messages.add(new BusinessMessage(I18n.get("El «en copia» debe contener direcciones de correo válidas")));
        }

        if (contieneDireccionNoValida(correo.getEnCopiaOculta())) {
            messages.add(new BusinessMessage(I18n.get("El «en copia oculta» debe contener direcciones de correo válidas")));
        }

        if (tieneCopias(correo) && hayDireccionRepetida(correo)) {
            messages.add(new BusinessMessage(I18n.get(
                    "Una misma dirección de correo no puede aparecer más de una vez entre el «para», el «en copia» y el «en copia oculta»")));
        }
    }

    private void validarAsuntoYCuerpo(Correo correo, BusinessMessages messages) {
        String asunto = correo.getAsunto();
        if (TextUtil.isNullOrBlank(asunto)) {
            messages.add(new BusinessMessage(I18n.get("El asunto es obligatorio")));
        } else {
            if (asunto.length() > LONGITUD_MAXIMA_ASUNTO) {
                messages.add(new BusinessMessage(I18n.get("El asunto no puede superar 255 caracteres")));
            }
            if (CARACTER_DE_CONTROL.matcher(asunto).find()) {
                messages.add(new BusinessMessage(I18n.get("El asunto no puede contener saltos de línea ni caracteres de control")));
            }
        }

        if (TextUtil.isNullOrBlank(correo.getCuerpo())) {
            messages.add(new BusinessMessage(I18n.get("El cuerpo es obligatorio")));
        }
    }

    private void validarTamanoTotalAdjuntos(Correo correo, BusinessMessages messages) {
        // Se mide en disco: el fileSize del MetaFile lo dicta el cliente.
        long tamanoTotal = adjuntosDe(correo).stream()
                .map(Adjunto::getContenido)
                .filter(Objects::nonNull)
                .mapToLong(MetaFileUtil::getSize)
                .sum();
        if (tamanoTotal > TAMANO_MAXIMO_ADJUNTOS) {
            messages.add(new BusinessMessage(I18n.get("Los adjuntos del correo no pueden superar 25 MB en total")));
        }
    }

    /**************************************************************************************/
    /********************************** AllowProperties ***********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        Map<String, Object> propiedades = allowPropertiesInsertComunes();
        propiedades.putAll(Map.of(
                "para", Map.of(),
                "enCopia", Map.of(),
                "enCopiaOculta", Map.of(),
                "asunto", Map.of(),
                "cuerpo", Map.of(),
                "adjuntos", Map.of(
                        "nombreFichero", Map.of(),
                        "contenido", Map.of()
                )
        ));
        return AllowProperties.createAllowProperties(propiedades);
    }

    /**************************************************************************************/
    /************************************ Action Rules ************************************/
    /**************************************************************************************/

    private void fireActionRule_CopiarAdjuntos(Correo correo) {
        for (Adjunto adjunto : adjuntosDe(correo)) {
            MetaFile copia = MetaFileUtil.cloneMetaFile(adjunto.getContenido());
            copia.setFileName(adjunto.getNombreFichero().trim());
            adjunto.setContenido(copia);
        }
    }

    /**************************************************************************************/
    /********************************** Otras funciones ***********************************/
    /**************************************************************************************/

    private List<Adjunto> adjuntosDe(Correo correo) {
        return Objects.requireNonNullElse(correo.getAdjuntos(), List.of());
    }

    private boolean contieneDireccionNoValida(String direcciones) {
        return separarDirecciones(direcciones).stream().anyMatch(direccion -> EMailUtil.isValid(direccion) == false);
    }

    private boolean tieneCopias(Correo correo) {
        return TextUtil.isNullOrBlank(correo.getEnCopia()) == false
                || TextUtil.isNullOrBlank(correo.getEnCopiaOculta()) == false;
    }

    private boolean hayDireccionRepetida(Correo correo) {
        List<String> direcciones = Stream.of(correo.getPara(), correo.getEnCopia(), correo.getEnCopiaOculta())
                .flatMap(lista -> separarDirecciones(lista).stream())
                .map(direccion -> direccion.toLowerCase(Locale.ROOT))
                .toList();
        return new HashSet<>(direcciones).size() < direcciones.size();
    }

    private List<String> separarDirecciones(String direcciones) {
        if (TextUtil.isNullOrBlank(direcciones)) {
            return List.of();
        }
        return Arrays.stream(direcciones.split(","))
                .map(String::trim)
                .filter(direccion -> direccion.isEmpty() == false)
                .toList();
    }

    private Mail construirMail(Correo correo) {
        List<String> to = separarDirecciones(correo.getPara());
        List<String> cc = separarDirecciones(correo.getEnCopia());
        List<String> bcc = separarDirecciones(correo.getEnCopiaOculta());

        String from = AppSettings.get().get("mail.address.from");

        List<Fichero> attachs = adjuntosDe(correo).stream()
                .map(adjunto -> new Fichero(
                        adjunto.getNombreFichero(),
                        MetaFileUtil.downloadContent(adjunto.getContenido()),
                        adjunto.getContenido().getFileType()))
                .toList();

        // Solo texto plano: el cuerpo puede incrustar texto libre (p. ej. el motivo de subsanación) que, enviado
        // también como HTML, el cliente de correo interpretaría como marcado.
        return new Mail(to, cc, bcc, from, correo.getAsunto(), null, correo.getCuerpo(), attachs);
    }
}
