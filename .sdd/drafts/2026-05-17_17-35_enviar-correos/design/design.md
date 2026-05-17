---
type: design
---

# Diseño: Subsistema Correos

**Objetivo:** Implementar el subsistema de correos con TareaCorreo inmutable, envío SMTP asíncrono mediante job Quartz y pantallas según rol.
**Capa:** subsystem/correos
**Análisis de origen:** .sdd/drafts/2026-05-17_17-35_enviar-correos/analysis/analysis.md
**Skills necesarios:** k-sistemas, k-vistas, k-validaciones, k-scheduler

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---|---|---|---|
| `src/main/java/com/educaflow/subsystem/correos/domains/TareaCorreo.xml` | Crear | k-sistemas | Modelo TareaCorreo + enum EstadoTareaCorreo + finder findPendientes |
| `src/main/java/com/educaflow/subsystem/correos/domains/AdjuntoCorreo.xml` | Crear | k-sistemas | Modelo AdjuntoCorreo |
| `src/main/java/com/educaflow/subsystem/correos/service/TareaCorreoService.java` | Crear | k-sistemas | Interfaz del servicio |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/TareaCorreoServiceImpl.java` | Crear | k-sistemas | Implementación (validaciones + fireActionRule_*) |
| `src/main/java/com/educaflow/subsystem/correos/service/AdjuntoCorreoService.java` | Crear | k-sistemas | Interfaz |
| `src/main/java/com/educaflow/subsystem/correos/service/impl/AdjuntoCorreoServiceImpl.java` | Crear | k-sistemas | Implementación (clonado MetaFile) |
| `src/main/java/com/educaflow/subsystem/correos/controller/TareaCorreoController.java` | Crear | k-sistemas | Controlador (btnReenviar) |
| `src/main/java/com/educaflow/subsystem/correos/job/ProcesadorCorreosJob.java` | Crear | k-scheduler | Job Quartz envío asíncrono |
| `src/main/java/com/educaflow/subsystem/correos/config/MailSenderProvider.java` | Crear | k-sistemas | Provider Guice de MailSender desde AppSettings |
| `src/main/java/com/educaflow/subsystem/correos/config/CorreosModule.java` | Crear | k-sistemas | Módulo Guice (binding del provider) |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-Todos.xml` | Crear | k-vistas | Vista admin todos |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-Centro.xml` | Crear | k-vistas | Vista centro |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-Mis.xml` | Crear | k-vistas | Vista usuario destinatario |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-Nuevo.xml` | Crear | k-vistas | Formulario creación |
| `src/main/java/com/educaflow/subsystem/correos/views/TareaCorreo-Grafica.xml` | Crear | k-vistas | Gráfica |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas | Añadir menús del subsistema |
| `src/main/resources/data-init/input/correos-MetaSchedule.xml` | Crear | k-scheduler | Registro del job Quartz |

## Pasos

### Paso 1 — Dominios
Crear `domains/TareaCorreo.xml` y `domains/AdjuntoCorreo.xml`. El XML completo, ya validado contra `domain-models.xsd`, está en `design/domains/*.xml`.

`TareaCorreo` declara el finder `findPendientes(using="estado=PENDIENTE", all="true")` que será usado por el job de envío.

### Paso 2 — Servicios

#### `TareaCorreoService` (interfaz)
```java
// com.educaflow.subsystem.correos.service.TareaCorreoService
public interface TareaCorreoService extends ModelService<TareaCorreo> {
    TareaCorreo procesarEnvio(TareaCorreo tareaCorreo);
    TareaCorreo reenviar(TareaCorreo tareaCorreo, TareaCorreo tareaCorreoOriginal);
    List<TareaCorreo> obtenerPendientes();
}
```

#### `TareaCorreoServiceImpl extends DefaultModelService<TareaCorreo>`
```java
public TareaCorreoServiceImpl(Class<TareaCorreo> model, Repository<TareaCorreo> repository);

@Override
public Optional<BusinessMessages> validateInsert(TareaCorreo entidad);
//   V-TareaCorreo-005: comprueba que emailDestinatario respeta el patrón email (regex).
//   Mensaje debe transmitir: dirección recibida + indicación de que el formato esperado es 'usuario@dominio'.

@Override
public TareaCorreo insert(TareaCorreo entidad);
//   Orquesta: fireActionRule_inicializarEstado, fireActionRule_asignarCentroDelUsuario,
//   fireActionRule_copiarAdjuntos (todos Antes) → super.insert(entidad).

private void fireActionRule_inicializarEstado(TareaCorreo entidad);
//   R-TareaCorreo-001: estado=PENDIENTE, numeroIntentos=0, fechaCreacion=now,
//   fechaUltimoIntento=null, motivoFallo=null. Antes de super.insert. Solo si entidad es nueva.

private void fireActionRule_asignarCentroDelUsuario(TareaCorreo entidad);
//   R-TareaCorreo-002: si entidad.centro está vacío y el usuario actual pertenece a un centro,
//   asignar ese centro. Antes de super.insert. Lee usuario actual del subsistema security.

private void fireActionRule_copiarAdjuntos(TareaCorreo entidad);
//   R-TareaCorreo-003 / R-AdjuntoCorreo-001: clona cada MetaFile de los adjuntos para garantizar
//   inmutabilidad. Usa MetaFileUtil.cloneMetaFile(...). Antes de super.insert.

public TareaCorreo procesarEnvio(TareaCorreo tareaCorreo);
//   Operación custom. Invocada desde ProcesadorCorreosJob. Diseño detallado en
//   design/rules/R-TareaCorreo-005.md. Secuencia:
//     fireActionRule_marcarEnviando (Antes)
//     mailSender.send(...)
//     éxito  → fireActionRule_marcarEnviado
//     fallo  → fireActionRule_marcarFallado (capturando la excepción).

private void fireActionRule_marcarEnviando(TareaCorreo entidad);
//   R-TareaCorreo-004: estado=ENVIANDO, numeroIntentos+=1, fechaUltimoIntento=now.
//   Persiste con super.update + JPA.flush. Antes de mailSender.send.

private void fireActionRule_marcarEnviado(TareaCorreo entidad);
//   R-TareaCorreo-006: estado=ENVIADO. Persiste con super.update. Después de éxito SMTP.

private void fireActionRule_marcarFallado(TareaCorreo entidad, String motivo);
//   R-TareaCorreo-007: estado=FALLADO, motivoFallo=motivo. Persiste con super.update.
//   Después de fallo SMTP.

public TareaCorreo reenviar(TareaCorreo tareaCorreo, TareaCorreo tareaCorreoOriginal);
//   Operación custom (botón Reenviar). Solo válida si tareaCorreoOriginal.estado==FALLADO
//   (validado en validateUpdate cuando se transita FALLADO → PENDIENTE).
//   Llama fireActionRule_volverAPendiente (Antes) → super.update.

private void fireActionRule_volverAPendiente(TareaCorreo entidad);
//   R-TareaCorreo-008: estado=PENDIENTE, motivoFallo=null. No toca numeroIntentos.
//   El nuevo intento será contabilizado por R-TareaCorreo-004 cuando el job la procese.

@Override
public Optional<BusinessMessages> validateUpdate(TareaCorreo entidad, TareaCorreo original);
//   Permite update solo cuando el cambio es exclusivamente de estado/datos de intento
//   (gestionado por el propio servicio). Para los usuarios la operación "Modificar" es Nunca.

@Override
public Optional<BusinessMessages> validateRemove(TareaCorreo entidad);
//   Bloquea siempre. Mensaje: "Las tareas de correo son un registro histórico inmutable y no pueden borrarse."

public List<TareaCorreo> obtenerPendientes();
//   Delegado en TareaCorreoRepository.findPendientes() (finder declarado en el XML de dominio).
```

#### `AdjuntoCorreoService` / `AdjuntoCorreoServiceImpl`
- `AdjuntoCorreoService extends ModelService<AdjuntoCorreo>`.
- `validateInsert`: V-AdjuntoCorreo-001..003 cubiertas por `required="true"` en el modelo (declarativo). No requiere lógica adicional.
- `insert`: `fireActionRule_clonarMetaFile(entidad)` (R-AdjuntoCorreo-001) → `super.insert`. Usa `MetaFileUtil.cloneMetaFile(...)` para reemplazar `contenidoFichero` por una copia propia.
- `validateUpdate`: bloquea siempre (inmutable).
- `validateRemove`: bloquea siempre (traza permanente).

### Paso 3 — Repositorio
El finder `findPendientes` se declara directamente en `domains/TareaCorreo.xml` (Axelor genera `TareaCorreoRepository.findPendientes()`). No hace falta clase de repositorio personalizada.

### Paso 4 — Controlador
`com.educaflow.subsystem.correos.controller.TareaCorreoController`:

```java
public void btnReenviar(ActionRequest actionRequest, ActionResponse actionResponse);
//   Recibe TareaCorreo desde la vista, recupera el original con su id, delega en
//   TareaCorreoService.reenviar(tareaCorreo, tareaCorreoOriginal). Señaliza recarga
//   con actionResponse.setReload(true) o actionResponse.setView(...) según convenga.
```

### Paso 5 — Job Quartz (envío asíncrono)
`com.educaflow.subsystem.correos.job.ProcesadorCorreosJob implements org.quartz.Job`. Diseño detallado en `design/rules/R-TareaCorreo-005.md`.

Provider Guice de `MailSender` en `com.educaflow.subsystem.correos.config.MailSenderProvider` que lee `mail.smtp.host`, `mail.smtp.user`, `mail.smtp.password` de `AppSettings` (según guías de diseño del subsistema). Binding en `CorreosModule`.

### Paso 6 — Vistas
Cinco ficheros, uno por `<action-view>`. XML completo en `design/views/*.xml`:
- `TareaCorreo-Todos.xml` (admin, grid + form completo con btnReenviar/btnSave condicionales).
- `TareaCorreo-Centro.xml` (supervisor/administrativa, filtrada por centro del usuario, solo lectura).
- `TareaCorreo-Mis.xml` (destinatario, filtrada por DNI, solo lectura, campos reducidos).
- `TareaCorreo-Nuevo.xml` (admin, abre directamente el formulario en modo nuevo).
- `TareaCorreo-Grafica.xml` (chart de barras apiladas por día y estado con search-fields y validación del rango).

### Paso 7 — Menús
Fusionar la porción de `design/menus.xml` en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`:
- Padre "Correos" + 4 entradas (Todos, Nuevo, Centro, Gráfica) restringidas por `groups="admins"` donde proceda.
- Una entrada extra "Mis correos" bajo el menú existente `carpetaCiudadana-menuitem`.

### Paso 8 — Datos iniciales (MetaSchedule)
`src/main/resources/data-init/input/correos-MetaSchedule.xml` con el job:
- `name = "enviar-correos-job"`
- `cron = "0/30 * * * * ?"` (cada 30 s para testing; ajustar en producción).
- `jobClass = "com.educaflow.subsystem.correos.job.ProcesadorCorreosJob"`
- `active = true`

### Paso 9 — Verificación
`./gradlew clean build --info` → arrancar la aplicación → comprobar que:
- El menú "Correos" aparece para el Administrador.
- "Mis correos" aparece en "Carpeta ciudadana" para el resto de roles.
- El job aparece en Admin → Jobs → Schedules y se ejecuta.
- Una TareaCorreo creada en estado PENDIENTE pasa a ENVIANDO y luego a ENVIADO o FALLADO según el resultado del SMTP.

## Puntos a confirmar en implementación

- **Centro del usuario**: el `domain` de `TareaCorreo@Centro-action` usa `__user__?.centroActivo?.id`. El nombre exacto del campo (`centroActivo`, `centro`, etc.) lo expone el subsistema `security`; ajustar según la API real.
- **DNI del usuario**: el `domain` de `TareaCorreo@Mis-action` usa `__user__?.dni`. Mismo ajuste según la API de security.
- **`historialEstado`**: el dominio referencia `com.educaflow.subsystem.expedientes.db.HistorialEstado`. El subsistema `expedientes` es una excepción arquitectónica; el FQN está documentado en el contexto técnico de este diseño.

## Trazabilidad V/R/U → ubicación

### Validaciones V

| ID | Ubicación | Mecanismo |
|---|---|---|
| V-TareaCorreo-001 | domains/TareaCorreo.xml (asunto required) | Modelo declarativo |
| V-TareaCorreo-002 | domains/TareaCorreo.xml (cuerpo required) | Modelo declarativo |
| V-TareaCorreo-003 | domains/TareaCorreo.xml (dniDestinatario required) | Modelo declarativo |
| V-TareaCorreo-004 | domains/TareaCorreo.xml (emailDestinatario required) | Modelo declarativo |
| V-TareaCorreo-005 | TareaCorreoServiceImpl.validateInsert | Servidor (regex email) |
| V-TareaCorreo-006 | (cubierto por integridad referencial JPA del FK historialEstado) | Framework |
| V-TareaCorreo-007 | (cubierto por integridad referencial JPA del FK centro) | Framework |
| V-TareaCorreo-008 | domains/TareaCorreo.xml (enum EstadoTareaCorreo) | Modelo declarativo (enum) |
| V-AdjuntoCorreo-001 | domains/AdjuntoCorreo.xml (tareaCorreo required) | Modelo declarativo |
| V-AdjuntoCorreo-002 | domains/AdjuntoCorreo.xml (nombreFichero required) | Modelo declarativo |
| V-AdjuntoCorreo-003 | domains/AdjuntoCorreo.xml (contenidoFichero required) | Modelo declarativo |

### Reglas de negocio R

| ID | Ubicación | Momento | Detalle |
|---|---|---|---|
| R-TareaCorreo-001 | TareaCorreoServiceImpl.fireActionRule_inicializarEstado | Antes insert | — |
| R-TareaCorreo-002 | TareaCorreoServiceImpl.fireActionRule_asignarCentroDelUsuario | Antes insert | — |
| R-TareaCorreo-003 | TareaCorreoServiceImpl.fireActionRule_copiarAdjuntos | Antes insert | — |
| R-TareaCorreo-004 | TareaCorreoServiceImpl.fireActionRule_marcarEnviando | Antes procesarEnvio | — |
| R-TareaCorreo-005 | ProcesadorCorreosJob.execute | Job programado | design/rules/R-TareaCorreo-005.md |
| R-TareaCorreo-006 | TareaCorreoServiceImpl.fireActionRule_marcarEnviado | Después procesarEnvio | — |
| R-TareaCorreo-007 | TareaCorreoServiceImpl.fireActionRule_marcarFallado | Después procesarEnvio | — |
| R-TareaCorreo-008 | TareaCorreoServiceImpl.fireActionRule_volverAPendiente | Antes reenviar | — |
| R-AdjuntoCorreo-001 | AdjuntoCorreoServiceImpl.fireActionRule_clonarMetaFile | Antes insert | — |

### Reglas de UI U

| ID | Ubicación | Mecanismo |
|---|---|---|
| U-todos-001 | views/TareaCorreo-Todos.xml (btnReenviar showIf) | showIf="estado == 'FALLADO'" |
| U-todos-002 | views/TareaCorreo-Todos.xml (btnSave showIf) | showIf="id == null" |
| U-todos-003 | views/TareaCorreo-Todos.xml (paneles readonlyIf) | readonlyIf="id != null" en panel Datos del correo, Contenido y Adjuntos |
| U-todos-004 | views/TareaCorreo-Todos.xml (canNew del panel-related Adjuntos) | El grid de adjuntos solo permite alta cuando id==null (readonlyIf propagado al panel-related) |
| U-grafica-001 | views/TareaCorreo-Grafica.xml (action-record onInit set fechaInicial) | onInit + action-record |
| U-grafica-002 | views/TareaCorreo-Grafica.xml (action-record onInit set fechaFinal) | onInit + action-record |
| U-grafica-003 | views/TareaCorreo-Grafica.xml (search-field fechaInicial required) | required="true" |
| U-grafica-004 | views/TareaCorreo-Grafica.xml (search-field fechaFinal required) | required="true" |
| U-grafica-005 | views/TareaCorreo-Grafica.xml (action-validate btnRefrescar) | action-validate con error si fechaInicial > fechaFinal |
| U-grafica-006 | (cubierto por U-grafica-005 — misma validación cubre ambos cambios) | — |
