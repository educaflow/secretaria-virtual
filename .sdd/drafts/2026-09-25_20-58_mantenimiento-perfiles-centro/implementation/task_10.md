---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: la tabla no asigna skill a este fichero, que es documentación en markdown)

# Diseño: Mantenimiento de perfiles de trámites por centro

**Objetivo:** que el supervisor (en los centros que supervisa) y el administrador (en cualquier centro) puedan ver, crear, modificar y borrar las filas de `AceProfileCentro`, con las reglas que garantizan que cada fila dice sin ambigüedad a quién se da el perfil.
**Capa:** subsystem/security
**Especificación de origen:** .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-validaciones, k-code-quality, k-secure-coding, k-vistas, k-datainit

Las decisiones difíciles y sus alternativas están en `decisiones.md` (D1–D6); este documento es coherente con ellas.

## Ficheros a crear o modificar

Rutas relativas a `src/main/java/com/educaflow/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/CLAUDE.md` | Modificar | — | Documentar quién rellena `AceProfileCentro`, el enlace de permisos al grupo desde el propio data-init y la excepción `Tramite.supervisor`. |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño. Los fragmentos de `auth-security.xml`, `input-config.xml` y `CLAUDE.md` del Paso 7 se aplican **añadiendo** a lo que ya hay (no se borra nada).

**Acción `Modificar`:** el fichero ya existe; solo se **añade** lo que dice el apartado 7.3, sin quitar nada. Es documentación (no Java ni XML): se edita directamente, sin `developer-code-implementer`.

## Pasos

### Paso 7 — Seguridad (data-init del subsistema y documentación)

Todo dentro de `subsystem/security` (guía de diseño). Reglas de acceso en lenguaje natural:

- **Administrador** (grupo `admins`): ve, crea, modifica y borra filas de cualquier centro. No necesita permiso: Axelor no aplica permisos a `admins`, y el servicio no le aplica V-008.
- **Supervisor** (grupo `users` con el tipo de usuario `SUPERVISOR` en algún centro): ve, crea, modifica y borra solo filas de los centros que supervisa, y lee todos los trámites para poder elegirlos.
- **Cualquier otro usuario** del grupo `users`: tiene el permiso por pertenecer a `users`, pero la condición no le deja ver, modificar ni borrar ninguna fila, y V-008 le rechaza el alta.

**7.3 — `subsystem/security/CLAUDE.md`** (añadidos, sin quitar nada):

- En la tabla «Las tablas», la fila de `AceProfileCentro` dice que la rellenan las pantallas «Mi centro → Perfiles de trámites» (supervisor, sus centros) y «Administración → Perfiles de trámites por centro» (administrador), a través de `AceProfileCentroService`, que valida el destinatario único.
- En «Permisos de Axelor»: los permisos propios de este subsistema se enlazan al grupo `users` desde su propio `auth-security.xml` (bind de `group` en su `input-config.xml`), no desde el `auth.xml` global. Y la excepción `Tramite.supervisor`: el supervisor **lee** todos los trámites aunque no tenga perfil sobre ellos, solo para poder asignarlos; por eso ahí lo que ve no coincide con lo que puede hacer.

## Notas y supuestos (aplicables)

- **`Tramite.supervisor`** amplía lo que el supervisor puede **leer** de `Tramite` (decisiones D3). No cambia qué trámites le ofrece «Nuevo trámite» (la ventanilla filtra por perfiles), ni qué expedientes ve, ni el cálculo de perfiles: lo que el «Fuera de alcance» protege sigue intacto.
