package com.educaflow.subsystem.notificaciones.service.impl;

import com.axelor.auth.db.User;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.Repository;
import com.axelor.db.modelservice.AllowProperties;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.i18n.I18n;
import com.educaflow.base.infrastructure.async.EjecutorAsincrono;
import com.educaflow.base.util.Convert;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.ExceptionUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.TextUtil;
import com.educaflow.subsystem.expedientes.db.HistorialEstado;
import com.educaflow.subsystem.notificaciones.db.EstadoNotificacion;
import com.educaflow.subsystem.notificaciones.db.Notificacion;
import com.educaflow.subsystem.notificaciones.db.TipoNotificacion;
import com.educaflow.subsystem.notificaciones.db.repo.NotificacionRepository;
import com.educaflow.subsystem.notificaciones.service.NotificacionService;
import com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lo común a todos los canales de notificación; cada canal solo aporta el envío físico y lo propio de su tabla.
 *
 * <p>Su nombre es también el que ModelServiceFactory busca para la entidad padre {@code Notificacion}: al ser
 * abstracta y sin constructor público, la factoría no puede instanciarla y el REST automático de guardar o borrar
 * una {@code Notificacion} falla. Es deliberado: una notificación solo se trata a través de su canal.
 */
public abstract class NotificacionServiceImpl<T extends Notificacion> extends DefaultModelService<T>
        implements NotificacionService<T> {

    private static final int LONGITUD_MAXIMA = 255;

    @Inject
    EjecutorAsincrono ejecutorAsincrono;

    @Inject
    NotificacionRepository notificacionRepository;

    protected NotificacionServiceImpl(Class<T> model, Repository<T> repository) {
        super(model, repository);
    }

    @Override
    public T insert(T notificacion) {
        validateInsert(notificacion).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarValoresIniciales(notificacion);
        antesDeGuardar(notificacion);
        notificacion = repository.save(notificacion);
        fireActionRule_ProgramarEnvioAsincrono(notificacion);
        return notificacion;
    }

    @Override
    public T update(T nueva, T original) {
        throw new UnsupportedOperationException(mensajeInmutable());
    }

    @Override
    public void remove(T notificacion) {
        throw new UnsupportedOperationException(mensajeNoBorrable());
    }

    @Override
    public T create() {
        validateCreate().ifPresent(BusinessMessages::throwIfInvalid);

        T notificacion = nuevaInstancia();
        notificacion.setTipoNotificacion(TipoNotificacion.deClase(model));
        return notificacion;
    }

    @Override
    public T reenviar(T entidad, T entidadOriginal) {
        validateReenviar(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        // Sin repository.save: el cambio de estado lo hace procesarEnvio() en la transacción del hilo del pool.
        fireActionRule_ProgramarEnvioAsincrono(entidadOriginal);
        return entidadOriginal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> listarEnFail() {
        validateListarEnFail().ifPresent(BusinessMessages::throwIfInvalid);

        // Filtrar por el tipo garantiza que todas las filas son de la clase de este canal.
        return (List<T>) notificacionRepository
                .findByTipoNotificacionAndEstado(TipoNotificacion.deClase(model), EstadoNotificacion.FALLIDO)
                .fetch();
    }

    /** Lo que el canal hace con la notificación ya validada, justo antes de guardarla. */
    protected void antesDeGuardar(T notificacion) {
    }

    /** El envío físico por el canal; si falla lanza y la notificación queda FALLIDA. */
    protected abstract void enviar(T notificacion);

    /**************************************************************************************/
    /******************************* Métodos de Validación ********************************/
    /**************************************************************************************/

    @Override
    public Optional<BusinessMessages> validateInsert(T notificacion) {
        BusinessMessages messages = validateDatosComunes(notificacion);
        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /** Las validaciones del alta comunes a todos los canales; el canal añade las suyas a los mensajes devueltos. */
    protected BusinessMessages validateDatosComunes(T notificacion) {
        BusinessMessages messages = new BusinessMessages();

        validarMotivo(notificacion, messages);
        validarDniDestinatario(notificacion, messages);
        validarNombreYApellidos(notificacion, messages);
        validarCentro(notificacion, messages);
        validarHistorialEstado(notificacion, messages);

        return messages;
    }

    private void validarMotivo(T notificacion, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(notificacion.getName())) {
            messages.add(new BusinessMessage(I18n.get("El motivo es obligatorio")));
        } else if (notificacion.getName().length() > LONGITUD_MAXIMA) {
            messages.add(new BusinessMessage(I18n.get("El motivo no puede superar 255 caracteres")));
        }
    }

    private void validarDniDestinatario(T notificacion, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(notificacion.getDniDestinatario())) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario es obligatorio")));
        } else if (DniUtil.isValid(notificacion.getDniDestinatario()) == false) {
            messages.add(new BusinessMessage(I18n.get("El DNI del destinatario no es válido; compruebe la letra")));
        }
    }

    private void validarNombreYApellidos(T notificacion, BusinessMessages messages) {
        if (TextUtil.isNullOrBlank(notificacion.getNombre())) {
            messages.add(new BusinessMessage(I18n.get("El nombre es obligatorio")));
        } else if (notificacion.getNombre().length() > LONGITUD_MAXIMA) {
            messages.add(new BusinessMessage(I18n.get("El nombre no puede superar 255 caracteres")));
        }

        if (TextUtil.isNullOrBlank(notificacion.getApellidos())) {
            messages.add(new BusinessMessage(I18n.get("Los apellidos son obligatorios")));
        } else if (notificacion.getApellidos().length() > LONGITUD_MAXIMA) {
            messages.add(new BusinessMessage(I18n.get("Los apellidos no pueden superar 255 caracteres")));
        }
    }

    private void validarCentro(T notificacion, BusinessMessages messages) {
        User user = SecurityUtil.getUser();
        if (notificacion.getCentro() == null) {
            messages.add(new BusinessMessage(I18n.get("El centro es obligatorio")));
        } else if (SecurityUtil.isAdmin(user) == false && user.perteneceAlCentro(notificacion.getCentro()) == false) {
            messages.add(new BusinessMessage(I18n.get("No puede crear notificaciones para un centro que no es suyo")));
        }
    }

    private void validarHistorialEstado(T notificacion, BusinessMessages messages) {
        HistorialEstado historialEstado = notificacion.getHistorialEstado();
        if (notificacion.getCentro() == null || historialEstado == null) {
            return;
        }
        // Sin releer por repositorio: JPA.edit ya resolvió la referencia; sin id es una cáscara que mandó el cliente.
        if (historialEstado.getId() == null
                || historialEstado.getExpediente() == null
                || Objects.equals(historialEstado.getExpediente().getCentro(), notificacion.getCentro()) == false) {
            messages.add(new BusinessMessage(I18n.get("El estado del expediente indicado no existe")));
        }
    }

    @Override
    public Optional<BusinessMessages> validateUpdate(T nueva, T original) {
        return Optional.of(BusinessMessages.single(mensajeInmutable()));
    }

    @Override
    public Optional<BusinessMessages> validateRemove(T notificacion) {
        return Optional.of(BusinessMessages.single(mensajeNoBorrable()));
    }

    @Override
    public Optional<BusinessMessages> validateCreate() {
        return Optional.empty();
    }

    @Override
    public Optional<BusinessMessages> validateReenviar(T entidad, T entidadOriginal) {
        Objects.requireNonNull(entidadOriginal, "entidadOriginal no puede ser null");
        BusinessMessages messages = new BusinessMessages();

        if (entidadOriginal.getEstado() != EstadoNotificacion.FALLIDO) {
            messages.add(new BusinessMessage(I18n.get("Solo se pueden reenviar notificaciones que han fallado")));
        }

        User user = SecurityUtil.getUser();
        if (SecurityUtil.isAdmin(user) == false
                && GestorNotificacionesUtil.idsCentrosGestionados(user).contains(entidadOriginal.getCentro().getId()) == false) {
            messages.add(new BusinessMessage(I18n.get("No puede reenviar notificaciones de un centro que no es suyo")));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    @Override
    public Optional<BusinessMessages> validateListarEnFail() {
        return Optional.empty();
    }

    /**************************************************************************************/
    /********************************** AllowProperties ***********************************/
    /**************************************************************************************/

    @Override
    public AllowProperties allowPropertiesInsert() {
        return AllowProperties.createAllowProperties(allowPropertiesInsertComunes());
    }

    /** Los campos del alta comunes a todos los canales; el canal añade los suyos al mapa devuelto. */
    protected Map<String, Object> allowPropertiesInsertComunes() {
        return new HashMap<>(Map.of(
                "name", Map.of(),
                "dniDestinatario", Map.of(),
                "nombre", Map.of(),
                "apellidos", Map.of(),
                "centro", Map.of(),
                "historialEstado", Map.of()
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

    @Override
    public AllowProperties allowPropertiesReenviar() {
        return AllowProperties.createDenyAllProperties();
    }

    /**************************************************************************************/
    /************************************ Action Rules ************************************/
    /**************************************************************************************/

    private void fireActionRule_AsignarValoresIniciales(T notificacion) {
        notificacion.setTipoNotificacion(TipoNotificacion.deClase(EntityHelper.getEntityClass(notificacion)));
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacion.setFechaCreacion(LocalDateTime.now(Convert.defaultZoneId));
        notificacion.setNumeroReintentos(0);
        notificacion.setFechaPrimerIntentoEnvio(null);
        notificacion.setFechaUltimoIntentoEnvio(null);
        notificacion.setFechaEnvio(null);
        notificacion.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_ProgramarEnvioAsincrono(T notificacion) {
        // Solo el id: la entidad pertenece al EntityManager de esta transacción y la tarea corre en otro hilo.
        Long notificacionId = notificacion.getId();
        ejecutorAsincrono.ejecutarTrasCommit(() -> this.procesarEnvio(notificacionId));
    }

    private void fireActionRule_RegistrarIntentoEnvio(T notificacion) {
        LocalDateTime ahora = LocalDateTime.now(Convert.defaultZoneId);
        notificacion.setFechaUltimoIntentoEnvio(ahora);
        if (notificacion.getNumeroReintentos() == 0) {
            notificacion.setFechaPrimerIntentoEnvio(ahora);
        }
        notificacion.setNumeroReintentos(notificacion.getNumeroReintentos() + 1);
    }

    private void fireActionRule_MarcarEnvioCorrecto(T notificacion) {
        notificacion.setEstado(EstadoNotificacion.ENVIADO);
        notificacion.setFechaEnvio(LocalDateTime.now(Convert.defaultZoneId));
        notificacion.setDescripcionUltimoFallo(null);
    }

    private void fireActionRule_MarcarEnvioFallido(T notificacion, RuntimeException excepcion) {
        notificacion.setEstado(EstadoNotificacion.FALLIDO);
        notificacion.setDescripcionUltimoFallo(ExceptionUtil.getTraceAsString(excepcion));
        notificacion.setFechaEnvio(null);
    }

    /**************************************************************************************/
    /********************************** Otras funciones ***********************************/
    /**************************************************************************************/

    void procesarEnvio(Long notificacionId) {
        JPA.runInTransaction(() -> {
            // Bloqueo pesimista: dos «Reenviar» seguidos pueden correr a la vez en el pool; el segundo espera al
            // commit del primero y, si este acabó en ENVIADO, no vuelve a enviar.
            T notificacion = JPA.em().find(model, notificacionId, LockModeType.PESSIMISTIC_WRITE);
            if (notificacion == null) {
                throw new IllegalStateException("No existe la notificación " + notificacionId);
            }
            if (notificacion.getEstado() == EstadoNotificacion.ENVIADO) {
                return;
            }

            fireActionRule_RegistrarIntentoEnvio(notificacion);
            try {
                enviar(notificacion);
                fireActionRule_MarcarEnvioCorrecto(notificacion);
            } catch (RuntimeException ex) {
                fireActionRule_MarcarEnvioFallido(notificacion, ex);
            }

            repository.save(notificacion);
        });
    }

    private T nuevaInstancia() {
        try {
            return model.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se puede instanciar " + model.getName(), ex);
        }
    }

    private String mensajeInmutable() {
        return I18n.get("La notificación es inmutable tras su creación.");
    }

    private String mensajeNoBorrable() {
        return I18n.get("Las notificaciones no se pueden borrar.");
    }
}
