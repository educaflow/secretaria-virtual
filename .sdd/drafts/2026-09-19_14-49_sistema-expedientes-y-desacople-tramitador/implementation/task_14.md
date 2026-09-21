---
type: implementation-task
template: system
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**Alcance de esta tarea:** dos filas que son la misma pieza lógica (la consulta nueva sustituye a la pantalla actual de tipos):

1. **Crear** `src/main/java/com/educaflow/system/expedientes/views/Main-TipoExpediente.xml`. El fichero **ya está materializado y validado** en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/views/Main-TipoExpediente.xml`. **MUST** copiarlo **literalmente** (`cp`) a `src/main/java/com/educaflow/system/expedientes/views/Main-TipoExpediente.xml` **sin regenerarlo, reformatearlo ni editarlo** (`implementation.md` §1). La acción es `Crear`: el destino **no debe existir** (si existe, `CONFLICT`).
2. **Borrar** `src/main/java/com/educaflow/subsystem/expedientes/views/TipoExpediente.xml` (eliminación declarada; la hace el implementador directamente con `rm`/`git rm`). **MUST NOT** borrar nada más.

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/expedientes/views/Main-TipoExpediente.xml` | Crear | k-vistas (grids.md, forms.md) | Consulta de tipos de expediente (copia verbatim de `design/views/Main-TipoExpediente.xml`) |
| `src/main/java/com/educaflow/subsystem/expedientes/views/TipoExpediente.xml` | Borrar | — | Sustituida por `Main-TipoExpediente.xml` |

**Del diseño — Paso 7 (verbatim: introducción y `Main-TipoExpediente.xml`; las demás vistas son de las tareas 13 y 15)**

### Paso 7 — Vistas

**Crear** `system/expedientes/views/Main-NuevoExpediente.xml` y `system/expedientes/views/Main-TipoExpediente.xml` copiando los de `design/views/`.
**Modificar** `tramites/views/Tramites.xml` copiando `design/views/Tramites.xml`.
**Borrar** `subsystem/expedientes/views/Main-ContextoTramitacion.xml` y `subsystem/expedientes/views/TipoExpediente.xml`.

#### `views/Main-TipoExpediente.xml` (nuevo)

Un bloque `sysExpedientes.Main@TipoExpediente`.
- `action-view` `sysExpedientes.Main@TipoExpediente-action` («Tipos de expediente»): grid → form, sin toolbar de grid ni de form, sin `forceEdit` (se abre en lectura).
- `grid` `sysExpedientes.Main@TipoExpediente-grid`: columnas `code`, `name`, `tramite`; `canNew="false"` (sin «Nuevo»), `canViewOnClick="true"`, `allowSearchFields="false"` (sin búsqueda), `orderBy="code"`.
- `form` `sysExpedientes.Main@TipoExpediente-form`: panel `TipoExpediente` «Datos» con `code`, `name`, `tramite` en `readonly="true"`; `tramite` con `canNew/canEdit/canView="false"` para que no se pueda abrir ni editar el trámite desde aquí (el spec prohíbe mantener otras tablas); sin Guardar ni Borrar (desviación del estándar que declara `screen-tipos-de-expediente.md`); un único botón «Salir» (`btnCancel` → `back`).
- Quedan fuera `versionExpediente`, `openDate`, `closeDate` (no existen en la entidad) y `basePackageName` (dato interno), como piden las guías.

ASCII Layout de `sysExpedientes.Main@TipoExpediente-form`:

```
panel TipoExpediente «Datos»
ccccnnnntttt   ← code(4) + name(4) + tramite(4)   [código ~34 car., nombre y trámite ~40 car.: longitudes parecidas]
buttons-panel
..........ss   ← colOffset(10) + Salir(2)          [10+2 = 12]
```

**Del diseño — Verificación del Paso 7 (verbatim)**

Verificación: `bash .claude/skills/sdd-designer/template-system/validate.sh .sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design` → `VALIDACION-XML: OK`; `grep -rn "Main@ContextoTramitacion\|subsysExpedientes.TipoExpediente@Main" src` sin resultados.

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Fichero completo (pantalla actual de tipos) | `subsystem/expedientes/views/TipoExpediente.xml` | `screen-tipos-de-expediente.md`: «Sustituye a la pantalla actual del mismo nombre» |

**Del diseño — Notas y supuestos aplicables (verbatim)**

7. **«Salir» en el formulario de tipos de expediente** (Origen spec: —): el formulario de solo lectura necesita un botón para volver al listado porque la toolbar está oculta (`VAR-6.1`); no es Guardar ni Borrar, que es lo que el spec excluye.
8. **`orderBy="code"` en el listado de tipos:** el spec dice «sin orden definido»; `VAR-5.1` exige `orderBy` en todo grid y el código es la clave natural.
17. **Visibilidad de «Tipos de expediente» solo por menú (aceptado a propósito, no es un defecto a reportar).** El spec dice que la pantalla es «visible únicamente para el Administrador»; el diseño lo cumple solo ocultando el menú (`groups="admins"`, Paso 8), sin defensa en el servidor. Motivos: (a) el grupo `users` tiene que conservar la lectura `TipoExpediente.conAce` porque otras pantallas del expediente (tramitación, listados de expedientes) leen el tipo de expediente; (b) ese permiso ya filtra las filas a los tipos sobre los que el usuario tiene un `Ace` en uno de sus centros, así que abrir la acción por URL no enseña ningún dato que el usuario no pudiera leer ya por REST o por esas pantallas; (c) la pantalla es de solo lectura y la escritura la bloquea el servidor para todos (`TipoExpedienteServiceImpl`, V-TipoExpediente-001, D5). Una defensa real (un `domain` o permiso sobre la acción solo para `admins`) añadiría piezas sin proteger ningún dato. Si en el futuro la pantalla enseñara datos que `conAce` no da, habría que añadirla.
