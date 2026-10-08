---
type: implementation-task
template: system
---

# Tarea 36 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit
- k-secure-coding

## Datos iniciales del subsistema: permisos de notificaciones

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/notificaciones/data-init/input-config.xml` | Crear | k-datainit | Binding de los permisos del subsistema |
| `subsystem/notificaciones/data-init/input/auth-notificaciones.xml` | Crear | k-datainit, k-secure-coding | Permisos de `Notificacion`, `Correo`, `Sms` y `Adjunto` |

**Nota de materialización:** estos dos XML **no** están materializados en `design/` (no hay fichero en `design/domains/` ni `design/views/` para ellos): se escriben a partir del Paso 13 siguiendo `k-datainit` (el `bind` de `permission` se toma del `correos/data-init/input-config.xml` que existía antes de la tarea 01; si ya se borró, recupéralo con `git show HEAD:src/main/java/com/educaflow/subsystem/correos/data-init/input-config.xml`).

### Paso 13 — Seguridad (permisos)

`subsystem/notificaciones/data-init/input-config.xml`: `<xml-inputs priority="10">` con un único `<input file="auth-notificaciones.xml" root="auth">` y el `bind` de `permission` de `correos/data-init/input-config.xml` (name, object, condition, conditionParams, can/*). Sin binding de grupos.

`subsystem/notificaciones/data-init/input/auth-notificaciones.xml` — 8 permisos, todos `read="true"` y el resto `false`. Como `AuthSecurity` no recorre superclases, cada entidad lleva los suyos:

| Permiso | `object` | `condition` | `conditionParams` |
|---|---|---|---|
| `Notificacion.propio-destinatario` | `…notificaciones.db.Notificacion` | `self.dniDestinatario = ? and self.estado = 'ENVIADO'` | `__user__.dni` |
| `Correo.propio-destinatario` | `…db.Correo` | ídem | `__user__.dni` |
| `Sms.propio-destinatario` | `…db.Sms` | ídem | `__user__.dni` |
| `Adjunto.propio-destinatario` | `…db.Adjunto` | `self.correo.dniDestinatario = ? and self.correo.estado = 'ENVIADO'` | `__user__.dni` |
| `Notificacion.propio-centro-gestion` | `…db.Notificacion` | `self.centro.id IN (?)` | GESTOR |
| `Correo.propio-centro-gestion` | `…db.Correo` | `self.centro.id IN (?)` | GESTOR |
| `Sms.propio-centro-gestion` | `…db.Sms` | `self.centro.id IN (?)` | GESTOR |
| `Adjunto.propio-centro-gestion` | `…db.Adjunto` | `self.correo.centro.id IN (?)` | GESTOR |

GESTOR = `com.educaflow.subsystem.notificaciones.util.GestorNotificacionesUtil.idsCentrosGestionados(__user__)` — el único dueño de la clasificación (D4), sin ninguna copia JPQL.
`AuthSecurity.Condition` evalúa con `GroovyScriptHelper` cada `conditionParams` distinto de `__user__` (con el `__user__` ligado), la `ScriptPolicy` lo admite porque la clase lleva `@ScriptAllowed`, y `JPQLFilter` pasa la `List<Long>` tal cual al `IN (?)`; la lista nunca está vacía (centinela `-1L` en el dueño).
La expresión **MUST NOT** llevar comas: `AuthSecurity` parte `conditionParams` por «,».

Regla de acceso en lenguaje natural:
- **Administrador**: ve y opera todo (sin filtro de permisos); da de alta por «Todas»; reenvía (V-Notificacion-015 lo admite). No modifica ni borra (V-Notificacion-012/013, en el servicio).
- **Gestor del centro**: lee las notificaciones y adjuntos de los centros donde es gestor; reenvía las FALLIDO de esos centros (V-Notificacion-015). No da de alta: no ve «Todas» ni «Nueva notificación», y sus permisos son de solo lectura (`create="false"`), así que tampoco por REST.
- **Destinatario**: lee solo sus notificaciones ENVIADO (por DNI) y sus adjuntos, de cualquier centro.

Verificación: reset de BD y arranque; el log de importación de data-init sin errores.

## Notas y supuestos (las que aplican a esta tarea)

- **Puerta REST de alta para gestores**: los permisos de los gestores son de solo lectura (`create="false"`), así que un gestor no puede dar de alta por REST; el administrador sí (no tiene filtro de permisos) y pasa por las mismas validaciones del servicio.
