package com.educaflow.subsystem.correos.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.correos.db.Adjunto;
import com.educaflow.subsystem.correos.service.AdjuntoService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdjuntoServiceImpl extends DefaultModelService<Adjunto> implements AdjuntoService {

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public AdjuntoServiceImpl(Class<Adjunto> model, Repository<Adjunto> repository) {
        super(model, repository);
    }

    @Override
    public Adjunto update(Adjunto nuevo, Adjunto original) {
        // El adjunto es inmutable tras su creación (patrón gemelo de validateUpdate,
        // k-secure-coding §9.2): no hay flujo normal, solo el rechazo incondicional.
        throw new UnsupportedOperationException(I18n.get("El adjunto es inmutable tras su creación."));
    }

    @Override
    public void remove(Adjunto adjunto) {
        // Como un correo nunca se borra, sus adjuntos tampoco (análogo a RES-Correo-003).
        throw new UnsupportedOperationException(I18n.get("Los adjuntos no se pueden borrar."));
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(Adjunto adjunto) {
        BusinessMessages messages = new BusinessMessages();

        validarCorreoObligatorio(adjunto, messages);
        validarCentroDelCorreo(adjunto, messages);
        validarCorreoNoExistente(adjunto, messages);
        validarNombreFichero(adjunto, messages);
        validarContenido(adjunto, messages);
        validarNombreUnicoEnCorreo(adjunto, messages);

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    private void validarCorreoObligatorio(Adjunto adjunto, BusinessMessages messages) {
        // V-Adjunto-001: el adjunto debe pertenecer a un correo
        if (adjunto.getCorreo() == null) {
            messages.add(new BusinessMessage(I18n.get("El adjunto debe pertenecer a un correo")));
        }
    }

    private void validarCentroDelCorreo(Adjunto adjunto, BusinessMessages messages) {
        // V-Adjunto-002: solo si el correo está indicado (evitar NPE). Si el usuario actual no es
        // Administrador, el centro del correo MUST pertenecer a los centros del usuario (mismo
        // mecanismo que CorreoServiceImpl.perteneceAlCentroDelUsuario).
        if (adjunto.getCorreo() == null) {
            return;
        }
        if (!SecurityUtil.isAdmin(SecurityUtil.getUser())
                && !perteneceAlCentroDelUsuario(adjunto.getCorreo().getCentro())) {
            messages.add(new BusinessMessage(I18n.get("No puede añadir adjuntos a correos de un centro que no es suyo")));
        }
    }

    private void validarCorreoNoExistente(Adjunto adjunto, BusinessMessages messages) {
        // V-Adjunto-003: solo si el correo está indicado. fechaCreacion es un campo servidor que
        // CorreoServiceImpl solo asigna DESPUÉS de validar todo el árbol, así que null aquí
        // significa "el correo se está creando ahora mismo, en esta petición".
        if (adjunto.getCorreo() == null) {
            return;
        }
        if (adjunto.getCorreo().getFechaCreacion() != null) {
            messages.add(new BusinessMessage(I18n.get("No se pueden añadir adjuntos a un correo ya existente")));
        }
    }

    private void validarNombreFichero(Adjunto adjunto, BusinessMessages messages) {
        // V-Adjunto-004: nombreFichero obligatorio
        if (adjunto.getNombreFichero() == null || adjunto.getNombreFichero().isBlank()) {
            messages.add(new BusinessMessage(I18n.get("El nombre del fichero es obligatorio")));
        }
    }

    private void validarContenido(Adjunto adjunto, BusinessMessages messages) {
        // V-Adjunto-005: contenido obligatorio
        if (adjunto.getContenido() == null) {
            messages.add(new BusinessMessage(I18n.get("El contenido del adjunto es obligatorio")));
        }
    }

    private void validarNombreUnicoEnCorreo(Adjunto adjunto, BusinessMessages messages) {
        // V-Adjunto-006 (RES-Adjunto-001, defensa en profundidad de la unique-constraint del XML):
        // solo si el correo está indicado. Ningún OTRO adjunto del mismo correo (comparación por
        // identidad, nunca el propio adjunto contra sí mismo) puede tener el mismo nombreFichero
        // (trim + comparación exacta).
        if (adjunto.getCorreo() == null || adjunto.getCorreo().getAdjuntos() == null
                || adjunto.getNombreFichero() == null) {
            return;
        }
        List<Adjunto> hermanos = adjunto.getCorreo().getAdjuntos();
        String nombre = adjunto.getNombreFichero().trim();

        boolean existeDuplicado = hermanos.stream()
                .anyMatch(otro -> otro != adjunto
                        && otro.getNombreFichero() != null
                        && nombre.equals(otro.getNombreFichero().trim()));

        if (existeDuplicado) {
            messages.add(new BusinessMessage(I18n.get("Ya existe un adjunto con ese nombre en el correo")));
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original) {
        // Sin condición: el adjunto es inmutable tras su creación (Acción "Modificar" con Input
        // AllowProperties vacío), siempre se rechaza.
        return Optional.of(BusinessMessages.single(I18n.get("El adjunto es inmutable tras su creación.")));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(Adjunto adjunto) {
        // Sin condición: como un correo nunca se borra, sus adjuntos tampoco (análogo a
        // RES-Correo-003), siempre se rechaza.
        return Optional.of(BusinessMessages.single(I18n.get("Los adjuntos no se pueden borrar.")));
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(Map.of(
                "nombreFichero", Map.of(),
                "contenido", Map.of(),
                "correo", Map.of()
        ));
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
}
