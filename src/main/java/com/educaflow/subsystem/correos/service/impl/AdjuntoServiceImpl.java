package com.educaflow.subsystem.correos.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaFiles;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.correos.db.Adjunto;
import com.educaflow.subsystem.correos.service.AdjuntoService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class AdjuntoServiceImpl extends DefaultModelService<Adjunto> implements AdjuntoService {

    // Coincide con data.upload.max-size, que AOP solo aplica en el front: el REST automático no lo comprueba.
    static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    private static final Pattern CARACTERES_PROHIBIDOS_NOMBRE_FICHERO = Pattern.compile("[/\\\\\\p{Cntrl}]");

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public AdjuntoServiceImpl(Class<Adjunto> model, Repository<Adjunto> repository) {
        super(model, repository);
    }

    @Override
    public Adjunto update(Adjunto nuevo, Adjunto original) {
        throw new UnsupportedOperationException(I18n.get("El adjunto es inmutable tras su creación."));
    }

    @Override
    public void remove(Adjunto adjunto) {
        // Como un correo nunca se borra, sus adjuntos tampoco.
        throw new UnsupportedOperationException(I18n.get("Los adjuntos no se pueden borrar."));
    }

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Adjunto adjunto) {
        BusinessMessages messages = new BusinessMessages();

        validarCorreoObligatorio(adjunto, messages);
        validarCentroDelCorreo(adjunto, messages);
        validarCorreoNoExistente(adjunto, messages);
        validarNombreFichero(adjunto, messages);
        validarContenido(adjunto, messages);
        validarTamanoContenido(adjunto, messages);
        validarCaracteresNombreFichero(adjunto, messages);
        validarNombreUnicoEnCorreo(adjunto, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarCorreoObligatorio(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getCorreo() == null) {
            messages.add(new BusinessMessage(I18n.get("El adjunto debe pertenecer a un correo")));
        }
    }

    private void validarCentroDelCorreo(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getCorreo() == null) {
            return;
        }
        if (!SecurityUtil.isAdmin(SecurityUtil.getUser())
                && !SecurityUtil.getUser().perteneceAlCentro(adjunto.getCorreo().getCentro())) {
            messages.add(new BusinessMessage(I18n.get("No puede añadir adjuntos a correos de un centro que no es suyo")));
        }
    }

    private void validarCorreoNoExistente(Adjunto adjunto, BusinessMessages messages) {
        // fechaCreacion es un campo servidor que CorreoServiceImpl solo asigna DESPUÉS de validar todo el árbol, así que null aquí
        // significa "el correo se está creando ahora mismo, en esta petición".
        if (adjunto.getCorreo() == null) {
            return;
        }
        if (adjunto.getCorreo().getFechaCreacion() != null) {
            messages.add(new BusinessMessage(I18n.get("No se pueden añadir adjuntos a un correo ya existente")));
        }
    }

    private void validarNombreFichero(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getNombreFichero() == null || adjunto.getNombreFichero().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero es obligatorio")));
        }
    }

    private void validarContenido(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getContenido() == null) {
            messages.add(new BusinessMessage(I18n.get("El contenido del adjunto es obligatorio")));
        }
    }

    private void validarTamanoContenido(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getContenido() == null) {
            return;
        }
        // Se mide el fichero en disco: fileSize es un dato del MetaFile que el cliente puede dictar.
        long tamano;
        try {
            tamano = Files.size(MetaFiles.getPath(adjunto.getContenido()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        if (tamano > TAMANO_MAXIMO_BYTES) {
            messages.add(new BusinessMessage(I18n.get("El adjunto no puede superar los 10 MB")));
        }
    }

    private void validarCaracteresNombreFichero(Adjunto adjunto, BusinessMessages messages) {
        String nombreFichero = adjunto.getNombreFichero();
        if (nombreFichero == null || nombreFichero.isBlank()) {
            return;
        }
        if (CARACTERES_PROHIBIDOS_NOMBRE_FICHERO.matcher(nombreFichero).find()) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero no puede contener los caracteres / \\ ni caracteres de control")));
        }
    }

    private void validarNombreUnicoEnCorreo(Adjunto adjunto, BusinessMessages messages) {
        // Defensa en profundidad de la unique-constraint del XML.
        if (adjunto.getCorreo() == null || adjunto.getCorreo().getAdjuntos() == null
                || adjunto.getNombreFichero() == null) {
            return;
        }
        List<Adjunto> hermanos = adjunto.getCorreo().getAdjuntos();
        String nombre = adjunto.getNombreFichero().trim();

        boolean existeDuplicado = hermanos.stream()
                .anyMatch(otro -> !Objects.equals(otro, adjunto)
                        && otro.getNombreFichero() != null
                        && nombre.equals(otro.getNombreFichero().trim()));

        if (existeDuplicado) {
            messages.add(new BusinessMessage(I18n.get("Ya existe un adjunto con ese nombre en el correo")));
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original) {
        // Sin condición: el adjunto es inmutable tras su creación, siempre se rechaza.
        return Optional.of(BusinessMessages.single(I18n.get("El adjunto es inmutable tras su creación.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Adjunto adjunto) {
        // Sin condición: como un correo nunca se borra, sus adjuntos tampoco.
        return Optional.of(BusinessMessages.single(I18n.get("Los adjuntos no se pueden borrar.")));
    }

    /************************************************************************************/
    /********************************* AllowProperties **********************************/
    /************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombreFichero", Map.of(),
                "contenido", Map.of(),
                "correo", Map.of()
        ));
    }

    /***********************************************************************************/
    /********************************** Action Rules ***********************************/
    /***********************************************************************************/

    /***********************************************************************************/
    /********************************* Otras funciones *********************************/
    /***********************************************************************************/

}
