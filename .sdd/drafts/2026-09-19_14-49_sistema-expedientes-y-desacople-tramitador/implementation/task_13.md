---
type: implementation-task
template: system
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**Alcance de esta tarea:** dos filas que son la misma pieza lógica (la ventana nueva sustituye a la actual):

1. **Crear** `src/main/java/com/educaflow/system/expedientes/views/Main-NuevoExpediente.xml`. El fichero **ya está materializado y validado** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/views/Main-NuevoExpediente.xml`. **MUST** copiarlo **literalmente** (`cp`) a `src/main/java/com/educaflow/system/expedientes/views/Main-NuevoExpediente.xml` **sin regenerarlo, reformatearlo ni editarlo** (`implementation.md` §1). La acción es `Crear`: el destino **no debe existir** (si existe, `CONFLICT`).
2. **Borrar** `src/main/java/com/educaflow/subsystem/expedientes/views/Main-ContextoTramitacion.xml` (eliminación declarada; la hace el implementador directamente con `rm`/`git rm`). **MUST NOT** borrar nada más. El resto del «Paso 7» (`Main-TipoExpediente.xml`, `Tramites.xml`, borrado de `TipoExpediente.xml`) lo cubren las tareas 14 y 15.

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/expedientes/views/Main-NuevoExpediente.xml` | Crear | k-vistas (forms.md, actions.md) | Ventana «Nuevo expediente» (copia verbatim de `design/views/Main-NuevoExpediente.xml`) |
| `src/main/java/com/educaflow/subsystem/expedientes/views/Main-ContextoTramitacion.xml` | Borrar | — | Sustituida por `Main-NuevoExpediente.xml` |

**Del diseño — Paso 7 (verbatim: introducción y `Main-NuevoExpediente.xml`; las demás vistas son de las tareas 14 y 15)**

### Paso 7 — Vistas

**Crear** `system/expedientes/views/Main-NuevoExpediente.xml` y `system/expedientes/views/Main-TipoExpediente.xml` copiando los de `design/views/`.
**Modificar** `tramites/views/Tramites.xml` copiando `design/views/Tramites.xml`.
**Borrar** `subsystem/expedientes/views/Main-ContextoTramitacion.xml` y `subsystem/expedientes/views/TipoExpediente.xml`.

#### `views/Main-NuevoExpediente.xml` (nuevo)

Un bloque `sysExpedientes.Main@NuevoExpediente`.
- `action-view` `sysExpedientes.Main@NuevoExpediente-action` («Nuevo expediente»): solo form, ventana emergente (`popup`, sin toolbar, `popup-save=false`, `show-confirm=false`), contexto `_tramiteId = id` del trámite pulsado en «Trámites». Ya no lleva `_profile`: el perfil lo decide el servidor.
- `form` `sysExpedientes.Main@NuevoExpediente-form`, `can*="false"`, sin `canBackOnSave` (no tiene `btnSave`), `onNew` en tres etapas `serial:` (D4).
- Acciones principales:
  - `-btnCancel-action` = `close` (U-018).
  - `-btnCrear-action` = `validate` (obligatorios de cliente, U-005) → `Remote-validateCrear` → `Remote-crear`.
  - Etapa `-onNew-action` = `set-tramite-seleccionado` (U-014).
  - Etapa `-onNew-preparar-action` = `Remote-validatePreparar` → `Remote-preparar` → `set-centro-unicoDisponible` (U-003, `if="hayUnSoloCentroDisponible"`).
  - Etapa `-onChangeCentro-action` = `set-presentadoEnPapel-sinMarcar` (parte «arranca sin marcar» de U-007): deja la entrada lista antes de recalcular; no llama al servidor ni consume ningún valor calculado.
  - Etapa `-onChangePresentadoEnPapel-action` = `Remote-recalcular` → `set-presentadoEnPapel-vigente` (U-007, copia sin condición `presentadoEnPapelVigente`: la vista no decide la forma vigente) → `set-presentadoEnRepresentacion-segunOpciones` (U-008, U-009, U-012, U-016) → `set-presentadoEnRepresentacion.title-formaPresentar` (U-010). Es la **única** etapa que recalcula, y todo lo que recibe es definitivo para el estado de la ventana (R-NuevoExpediente-004 y R-NuevoExpediente-005 salen del mismo `getFormaPresentarVigente`), así que ninguna etapa deja valores para que otra los corrija.
  - `set-presentadoEnRepresentacion-segunOpciones` es un único `action-record` total, sin `if`: si `hayQuePreguntarParaQuien`, la deja sin valor; si no, la contesta con la única opción posible (`false` si se puede para mí, `true` si solo en representación) o la deja sin valor si no cabe ninguna.
- Encadenamiento de etapas (a lo sumo una remota por etapa, al principio; las reglas de vista que dependen de lo que devolvió van detrás, en la misma etapa). Nombre de cada etapa (D4): la primera etapa de un evento es `{evento}-action` y las siguientes exclusivas suyas `{evento}-{sufijo}-action`; una etapa compartida por varios eventos se llama por el **único** evento que la dispara él solo (`onChangePresentadoEnPapel`, el `onChange` de `presentadoEnPapel`) y los demás la reutilizan como última etapa de su `serial:`:
  - al abrir: `onNew → onNew-preparar → onChangePresentadoEnPapel`;
  - al cambiar el centro: `onChangeCentro → onChangePresentadoEnPapel`;
  - al cambiar «presentado en papel»: `onChangePresentadoEnPapel`.
- Reglas de vista continuas en los campos: `centro` `domain="self IN (:centrosDisponibles)"` (U-002, parámetro con nombre que AOP resuelve del contexto, sin interpolar ids), `required` (U-005), `readonlyIf="hayUnSoloCentroDisponible"` (U-004; antes de `preparar` vale `false`), `canNew/canEdit/canView="false"` (U-015); `presentadoEnPapel` `showIf="hayQuePreguntarPresentacion"` (U-006); `presentadoEnRepresentacion` `showIf`/`requiredIf` `hayQuePreguntarParaQuien` (U-011, U-013). Ninguna condición se escribe dos veces: cada una es un campo `servidor` que la vista solo consulta.
- La tarjeta del trámite es el `viewer` de `ayudaTramite` con `nombreTramite` como cabecera y la ayuda como HTML (U-001, U-017), igual que hoy.
- Botones: solo «Cancelar» y «Crear expediente», sin Guardar ni Borrar: es la desviación del estándar que declara `screen-nuevo-expediente.md` (el formulario no se guarda).
- Remotas: cinco `action-method` sobre `NuevoExpedienteController` (`validatePreparar`, `preparar`, `recalcular`, `validateCrear`, `crear`); cada `Remote-{X}` llama al método `X`.

ASCII Layout de `sysExpedientes.Main@NuevoExpediente-form` (se conserva el de la ventana actual, que el spec exige que se vea exactamente igual):

```
tramitePanel (sin marco) — los 9 campos hidden no se pintan
aaaaaaaaaaaa   ← ayudaTramite(12): tarjeta con nombre + ayuda del trámite

presentacionPanel «Presentación» — un dibujo por estado
Estado 1: sin preguntas (un solo perfil en el centro y una sola opción de «para quién», o sin centro)
cccccccccccc   ← centro(12)
Estado 2: hay que preguntar cómo se presenta
cccccccccccc   ← centro(12)
pppppppppppp   ← presentadoEnPapel(12) interruptor, showIf
Estado 3: hay que preguntar para quién
cccccccccccc   ← centro(12)
rrrrrrrrrrrr   ← presentadoEnRepresentacion(12) radio, showIf
Estado 4: las dos preguntas
cccccccccccc   ← centro(12)
pppppppppppp   ← presentadoEnPapel(12)
rrrrrrrrrrrr   ← presentadoEnRepresentacion(12)

buttons-panel
.......cckkk   ← colOffset(7) + Cancelar(2) + Crear expediente(3)   [7+2+3 = 12]
```

Cada pregunta condicional está sola en su fila (borde izquierdo sin `colOffset` y borde derecho a la vez), así que su `showIf` va en el propio campo y al ocultarse no desplaza nada.
Las tres filas son preguntas apiladas: el interruptor (título de 57 caracteres) y la pregunta de radio (opción de 80 caracteres) necesitan la fila entera, y el centro ocupa la misma columna para mantener los bordes alineados; es además el layout actual, que el spec pide conservar.
No hay botones secundarios: los dos son principales y quedan pegados al borde derecho (`7 + 2 + 3 = 12`).

**Del diseño — Verificación del Paso 7 (verbatim)**

Verificación: `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design` → `VALIDACION-XML: OK`; `grep -rn "Main@ContextoTramitacion\|subsysExpedientes.TipoExpediente@Main" src` sin resultados.

**Del diseño — Trazabilidad de reglas de UI (U) (verbatim)**

### Reglas de UI (U) — pantalla `nuevo-expediente`

Todas en `views/Main-NuevoExpediente.xml`; los nombres de acción omiten el prefijo `sysExpedientes.Main@NuevoExpediente-`.

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-001 | RUI-nuevo-expediente-formulario-001 | `viewer` del campo `ayudaTramite` (`depends="nombreTramite,ayudaTramite"`), rellenado por `Remote-preparar` en la etapa `onNew-preparar` |
| U-nuevo-expediente-002 | RUI-nuevo-expediente-formulario-002 | domain declarativo `self IN (:centrosDisponibles)` en el campo `centro` (continuo) |
| U-nuevo-expediente-003 | RUI-nuevo-expediente-formulario-003 | `set-centro-unicoDisponible-action` (`action-record` con `if="hayUnSoloCentroDisponible"`), etapa `onNew-preparar` |
| U-nuevo-expediente-004 | RUI-nuevo-expediente-formulario-004 | `readonlyIf="hayUnSoloCentroDisponible"` en `centro` |
| U-nuevo-expediente-005 | RUI-nuevo-expediente-formulario-005 | `required="true"` en `centro` + acción predefinida `validate` en `btnCrear-action` |
| U-nuevo-expediente-006 | RUI-nuevo-expediente-formulario-006 | `showIf="hayQuePreguntarPresentacion"` en `presentadoEnPapel` |
| U-nuevo-expediente-007 | RUI-nuevo-expediente-formulario-007 | `set-presentadoEnPapel-sinMarcar-action` (etapa `onChangeCentro`: sin marcar al cambiar el centro) + `set-presentadoEnPapel-vigente-action` (copia sin condición `presentadoEnPapelVigente`, que decide el servidor, etapa `onChangePresentadoEnPapel`: toma la forma deducida al abrir y al cambiar el centro); sin forma deducida el servidor devuelve la de la ventana, que `onChangeCentro` dejó sin marcar (o la que contestó el usuario) |
| U-nuevo-expediente-008 | RUI-nuevo-expediente-formulario-008 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin valor si `hayQuePreguntarParaQuien`) en la etapa `onChangePresentadoEnPapel`, que cierra el `serial:` del `onChange` de `centro` |
| U-nuevo-expediente-009 | RUI-nuevo-expediente-formulario-009 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin valor si `hayQuePreguntarParaQuien`) en la etapa `onChangePresentadoEnPapel` (`onChange` de `presentadoEnPapel`) |
| U-nuevo-expediente-010 | RUI-nuevo-expediente-formulario-010 | `set-presentadoEnRepresentacion.title-formaPresentar-action` (`action-attrs` `title` con `if` por `presentadoEnPapel`), etapa `onChangePresentadoEnPapel` (corre al abrir y en cada cambio del que depende) |
| U-nuevo-expediente-011 | RUI-nuevo-expediente-formulario-011 | `showIf="hayQuePreguntarParaQuien"` en `presentadoEnRepresentacion` (sin centro vale `false`: R-NuevoExpediente-005) |
| U-nuevo-expediente-012 | RUI-nuevo-expediente-formulario-012 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin `hayQuePreguntarParaQuien`, contestada con la única opción posible), etapa `onChangePresentadoEnPapel` |
| U-nuevo-expediente-013 | RUI-nuevo-expediente-formulario-013 | `requiredIf="hayQuePreguntarParaQuien"` en `presentadoEnRepresentacion` (el mismo campo que su `showIf`) |
| U-nuevo-expediente-014 | RUI-nuevo-expediente-formulario-014 | `set-tramite-seleccionado-action` (etapa `onNew`, desde `_tramiteId`) + campo `tramite` `hidden` |
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-015 | `canNew="false" canEdit="false" canView="false"` en `centro` |
| U-nuevo-expediente-016 | RUI-nuevo-expediente-formulario-016 | `set-presentadoEnRepresentacion-segunOpciones-action` (sin valor si `hayQuePreguntarParaQuien`) en la última etapa del `onNew` + `nullable="true"` en el dominio (no nace a `false`) |
| U-nuevo-expediente-017 | RUI-nuevo-expediente-formulario-017 | `viewer` de `ayudaTramite` con `dangerouslySetInnerHTML` |
| U-nuevo-expediente-018 | RUI-nuevo-expediente-formulario-018 | `btnCancel-action` = `close` + `view-param show-confirm=false` en el `action-view` |
| U-nuevo-expediente-019 | RUI-nuevo-expediente-formulario-019 | `NuevoExpedienteController.validateCrear` → `ActionResponseHelper.doResponseBusinessMessagesAsError(título, mensajes)` (lista `<ul>`, un mensaje por línea); `.crear` no captura `BusinessException` (Notas §11). Único U fuera del XML: el aviso de error lo compone el servidor al devolver los mensajes. |

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Fichero completo (ventana actual) | `subsystem/expedientes/views/Main-ContextoTramitacion.xml` | Objetivo del spec: la ventana pasa al sistema nuevo con el modelo `NuevoExpediente` |

**Del diseño — Notas y supuestos aplicables (verbatim)**

3. **Receta pendiente del patrón nuevo de D4** (`serial:` como frontera de etapa en eventos de formulario): `k-vistas/actions.md` y `agent_docs/view-rules.md` (`VAR-7.1`) solo contemplan `serial:` en botones con AutoFirma. No se modifican en esta iniciativa. La receta incluirá cómo se nombran las etapas de un `serial:` de evento: la primera etapa de un evento es `{evento}-action`, las siguientes exclusivas suyas `{evento}-{sufijo}-action` (análogo a `VAR-7.1`), y una etapa compartida por varios eventos lleva el nombre del evento que la dispara él solo (aquí `onChangePresentadoEnPapel`), reutilizada como última etapa por los demás.
6. **`btnCancel` de «Nuevo expediente» usa `close`, no `back`** (lo que `VAR-7.2` pide a un maestro): es una ventana emergente y el spec pide cerrarla sin preguntar (RUI-018).
9. **Orden del desplegable de centros (ESC-002):** lo da la búsqueda de `Centro` restringida por el domain declarativo con parámetro `self IN (:centrosDisponibles)` (AOP resuelve el parámetro con nombre del contexto; no se interpolan ids en el JPQL); `centrosDisponibles` va además ordenado por nombre.
