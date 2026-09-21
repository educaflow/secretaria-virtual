---
type: implementation-task
template: system
---

# Tarea 16 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

**Alcance de esta tarea:** fila `Modificar` de `menus.xml`. El `<menuitem>` está en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/menus.xml`. **Decisión de esta descomposición (el diseño manda sobre la regla genérica de fusión):** el Paso 8 del diseño (abajo) declara que el `<menuitem name="expedientes-tiposExpedientes-menuitem">` **existente se SUSTITUYE** por el de `design/menus.xml` (cambian `action` y `groups`; se conservan posición, padre y `order`). Por tanto, aquí la existencia de un `menuitem` con ese `name` **NO es `CONFLICT`** (sería la regla de inserción de `implementation.md` §3, pensada para menús nuevos): reemplaza esa línea, sin duplicarla, y no toques el resto del fichero. Tras editar, **MUST** validar con `xmllint` como indica `implementation.md` §3 (`xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`); si falla → `BLOCKED`.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | El menú «Tipos Expedientes» apunta a la acción nueva y solo lo ve `admins` |

**Del diseño — Paso 8 (verbatim)**

### Paso 8 — Menús

**Modificar** `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`: la línea existente con `name="expedientes-tiposExpedientes-menuitem"` se **sustituye** por la de `design/menus.xml`:
- `action` pasa a `sysExpedientes.Main@TipoExpediente-action`;
- `groups` pasa de `admins,users` a `admins` (solo lo ve el Administrador, ESC-008).

Se conservan su posición, su padre `expedientes-menuitem` y su `order="5"`. El resto del fichero no cambia.

Verificación: `grep -n "expedientes-tiposExpedientes-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` muestra una sola línea con `groups="admins"`.

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| `groups="admins,users"` del menú «Tipos Expedientes» (pasa a `admins`) | `secretariavirtual/menus/menus.xml` | `screen-tipos-de-expediente.md` «lo ve solo el Administrador»; ESC-008 |

**Del diseño — Notas y supuestos aplicables (verbatim)**

17. **Visibilidad de «Tipos de expediente» solo por menú (aceptado a propósito, no es un defecto a reportar).** El spec dice que la pantalla es «visible únicamente para el Administrador»; el diseño lo cumple solo ocultando el menú (`groups="admins"`, Paso 8), sin defensa en el servidor. Motivos: (a) el grupo `users` tiene que conservar la lectura `TipoExpediente.conAce` porque otras pantallas del expediente (tramitación, listados de expedientes) leen el tipo de expediente; (b) ese permiso ya filtra las filas a los tipos sobre los que el usuario tiene un `Ace` en uno de sus centros, así que abrir la acción por URL no enseña ningún dato que el usuario no pudiera leer ya por REST o por esas pantallas; (c) la pantalla es de solo lectura y la escritura la bloquea el servidor para todos (`TipoExpedienteServiceImpl`, V-TipoExpediente-001, D5). Una defensa real (un `domain` o permiso sobre la acción solo para `admins`) añadiría piezas sin proteger ningún dato. Si en el futuro la pantalla enseñara datos que `conAce` no da, habría que añadirla.
