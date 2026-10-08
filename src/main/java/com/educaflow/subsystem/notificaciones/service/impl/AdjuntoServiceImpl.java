package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.notificaciones.db.Adjunto;
import com.educaflow.subsystem.notificaciones.service.AdjuntoService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class AdjuntoServiceImpl extends DefaultModelService<Adjunto> implements AdjuntoService {

    // Coincide con data.upload.max-size, que AOP solo aplica en el front: el REST automático no lo comprueba.
    static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    private static final int LONGITUD_MAXIMA_NOMBRE_FICHERO = 255;
    private static final Pattern CARACTERES_PROHIBIDOS_NOMBRE_FICHERO = Pattern.compile("[/\\\\\\p{Cntrl}]");
    private static final Set<String> NOMBRES_FICHERO_RESERVADOS = Set.of(".", "..");

    public AdjuntoServiceImpl(Class<Adjunto> model, Repository<Adjunto> repository) {
        super(model, repository);
    }

    @Override
    public Adjunto update(Adjunto nuevo, Adjunto original) {
        throw new UnsupportedOperationException(I18n.get("El adjunto es inmutable tras su creación."));
    }

    @Override
    public void remove(Adjunto adjunto) {
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
        validarCaracteresNombreFichero(adjunto, messages);
        validarContenido(adjunto, messages);
        validarTamanoContenido(adjunto, messages);
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
        User user = SecurityUtil.getUser();
        if (SecurityUtil.isAdmin(user) == false && user.perteneceAlCentro(adjunto.getCorreo().getCentro()) == false) {
            messages.add(new BusinessMessage(I18n.get("No puede añadir adjuntos a correos de un centro que no es suyo")));
        }
    }

    private void validarCorreoNoExistente(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getCorreo() != null && adjunto.getCorreo().getId() != null) {
            messages.add(new BusinessMessage(I18n.get("No se pueden añadir adjuntos a un correo ya existente")));
        }
    }

    private void validarNombreFichero(Adjunto adjunto, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(adjunto.getNombreFichero())) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero es obligatorio")));
        }
    }

    private void validarCaracteresNombreFichero(Adjunto adjunto, BusinessMessages messages) {
        String nombreFichero = adjunto.getNombreFichero();
        if (TextUtil.isNullOrBlank(nombreFichero)) {
            return;
        }
        if (CARACTERES_PROHIBIDOS_NOMBRE_FICHERO.matcher(nombreFichero).find()) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero no puede contener los caracteres / \\ ni caracteres de control")));
        }
        if (nombreFichero.length() > LONGITUD_MAXIMA_NOMBRE_FICHERO) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero no puede superar 255 caracteres")));
        }
        if (NOMBRES_FICHERO_RESERVADOS.contains(nombreFichero.trim())) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero no puede ser «.» ni «..»")));
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
        // Se mide en disco: el fileSize del MetaFile lo dicta el cliente.
        long tamano = MetaFileUtil.getSize(adjunto.getContenido());
        if (tamano > TAMANO_MAXIMO_BYTES) {
            messages.add(new BusinessMessage(I18n.get("El adjunto no puede superar los 10 MB")));
        } else if (tamano == 0) {
            messages.add(new BusinessMessage(I18n.get("El fichero adjunto está vacío")));
        }
    }

    private void validarNombreUnicoEnCorreo(Adjunto adjunto, BusinessMessages messages) {
        if (adjunto.getCorreo() == null || adjunto.getNombreFichero() == null) {
            return;
        }
        List<Adjunto> hermanos = Objects.requireNonNullElse(adjunto.getCorreo().getAdjuntos(), List.of());
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
        return Optional.of(BusinessMessages.single(I18n.get("El adjunto es inmutable tras su creación.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Adjunto adjunto) {
        return Optional.of(BusinessMessages.single(I18n.get("Los adjuntos no se pueden borrar.")));
    }

    /**************************************************************************************/
    /********************************** AllowProperties ***********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombreFichero", Map.of(),
                "contenido", Map.of(),
                "correo", Map.of()
        ));
    }

    @Override
    public AllowProperties allowPropertiesUpdate() {
        return AllowProperties.createDenyAllProperties();
    }

    @Override
    public AllowProperties allowPropertiesRemove() {
        return AllowProperties.createDenyAllProperties();
    }

    /**************************************************************************************/
    /************************************ Action Rules ************************************/
    /**************************************************************************************/

    /**************************************************************************************/
    /********************************** Otras funciones ***********************************/
    /**************************************************************************************/

}
