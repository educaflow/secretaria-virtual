---
type: implementation-task
template: system
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-validaciones

## Servicio de Notificacion (NotificacionService + NotificacionServiceImpl)

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/service/NotificacionService.java` | Crear | k-sistemas (servicios.md) | Servicio de `Notificacion` con las factorías `createCorreo`/`createSms` |
| `subsystem/notificaciones/service/impl/NotificacionServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Factorías + cierre de la puerta REST de `Notificacion` |

### Paso 6 — Servicio de `Notificacion` (factorías y puerta REST de la base)

**`NotificacionService extends ModelService<Notificacion>`**:

```java
Correo createCorreo();
Sms createSms();


Optional<BusinessMessages> validateCreateCorreo();
Optional<BusinessMessages> validateCreateSms();
```

**`NotificacionServiceImpl extends DefaultModelService<Notificacion> implements NotificacionService`**:

```java
@Inject ModelServiceFactory modelServiceFactory;   // la factoría, no un ModelService (C21)

public NotificacionServiceImpl(Class<Notificacion> model, Repository<Notificacion> repository);

@Override public Notificacion insert(Notificacion notificacion);
//   V-Notificacion-017: la operación nunca se admite (§9.2 de k-secure-coding): solo
//   throw new UnsupportedOperationException(mensaje de V-Notificacion-017), igual que update/remove de los canales.
@Override public Notificacion update(Notificacion nueva, Notificacion original);
//   Delegación al dueño: servicioDelCanal(original).update(nueva, original) (lanza con el texto de su canal).
@Override public void remove(Notificacion notificacion);   // servicioDelCanal(notificacion).remove(notificacion)
@Override public Correo createCorreo();
//   validateCreateCorreo().ifPresent(throwIfInvalid); new Correo() con
//   tipoNotificacion = TipoNotificacion.deClase(Correo.class). NO lo inserta (quien llama rellena y usa CorreoService).
@Override public Sms createSms();   // ídem con Sms

@Override public Optional<BusinessMessages> validateInsert(Notificacion notificacion);
//   V-Notificacion-017 (Origen spec: —; «Input AllowProperties: ninguna» de la acción Crear de Notificacion):
//   siempre rechaza; el mensaje transmite que una notificación se da de alta siempre como uno de sus canales.
//   (Por /ws/rest/…Notificacion JPA.edit crea siempre una Notificacion «genérica»: nunca es un canal.)
@Override public Optional<BusinessMessages> validateUpdate(Notificacion nueva, Notificacion original);
//   servicioDelCanal(original).validateUpdate(nueva, original) → V-Notificacion-012 con el texto de su canal.
@Override public Optional<BusinessMessages> validateRemove(Notificacion notificacion);
//   servicioDelCanal(notificacion).validateRemove(notificacion) → V-Notificacion-013.
@Override public Optional<BusinessMessages> validateCreateCorreo();   // sin condiciones: Optional.empty()
@Override public Optional<BusinessMessages> validateCreateSms();      // sin condiciones: Optional.empty()

@Override public AllowProperties allowPropertiesInsert();   // createDenyAllProperties()
@Override public AllowProperties allowPropertiesUpdate();   // createDenyAllProperties()
@Override public AllowProperties allowPropertiesRemove();   // createDenyAllProperties()

private NotificacionCanalService<Notificacion> servicioDelCanal(Notificacion notificacion);
//   (NotificacionCanalService) modelServiceFactory.resolve(EntityHelper.getEntityClass(notificacion)).
//   La puerta /ws/rest/…Notificacion/{id} carga la instancia de la subclase (JOINED); quien sabe qué hacer con
//   ella es el servicio de su canal. Si la clase real no es un canal (no puede pasar: JOINED solo persiste
//   subclases), IllegalStateException.
```

Verificación (al cerrar el bloque 1–9): compila; un `POST /ws/rest/com.educaflow.subsystem.notificaciones.db.Notificacion` de admin responde error sin crear fila (test unitario en `test-unit-desc.md`).

### `insert` de cada entidad (puerta REST `/ws/rest/<FQN>` y `save` de los forms)

No se invoca desde un `@CallMethod`, pero es la frontera real del alta y se documenta igual.

**`NotificacionServiceImpl.allowPropertiesInsert/Update/Remove`** y **`*.allowPropertiesUpdate/Remove`** de los canales y del adjunto: `createDenyAllProperties` (inmutables, sin borrado; alta genérica imposible).

### DTO de alta programática

No hay DTO: la alta programática (`VerificacionHelper`) parte de `NotificacionService.createCorreo()` y pasa por el mismo `CorreoService.validateInsert/insert`, donde R-Notificacion-001 vuelve a fijar todos los campos servidor.

## Trazabilidad Origen spec → V/R/U → ubicación (filas de esta tarea)

### V

| ID | Origen spec | Ubicación |
|---|---|---|
| V-Notificacion-012 | RES-Notificacion-004 | `NotificacionCanalServiceImpl.validateUpdate` + `update` (throw); `NotificacionServiceImpl.validateUpdate/update` (delegan); `allowPropertiesUpdate` deny-all |
| V-Notificacion-013 | RES-Notificacion-005 | `NotificacionCanalServiceImpl.validateRemove` + `remove` (throw); `NotificacionServiceImpl.validateRemove/remove` (delegan) |
| V-Notificacion-017 | — | `NotificacionServiceImpl.validateInsert` (puerta de validación) + `insert` (solo throw): no se da de alta una notificación genérica |
