---
type: implementation-task
template: system
---

# Tarea 47 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Permisos del grupo users en auth.xml

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Grupo `users`: los 4 permisos `Correo.*`/`Adjunto.*` → los 8 de notificaciones; quita el comentario huérfano `<!-- Correos -->` |

### Paso 13 (extracto verbatim del design.md: tabla de permisos y `auth.xml`)

### Paso 13 — Seguridad (permisos)

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

`src/main/resources/data-init/input/auth.xml`, grupo `users`: las líneas `Correo.propio-destinatario`, `Correo.propio-centro-supervisor`, `Adjunto.propio-destinatario`, `Adjunto.propio-centro-supervisor` se sustituyen por los 8 permisos de la tabla; se quita el comentario `<!-- Correos -->` (sección vacía). El grupo `admins` no necesita ninguno (`AuthSecurity` no filtra al administrador).

Regla de acceso en lenguaje natural:
- **Administrador**: ve y opera todo (sin filtro de permisos); da de alta por «Todas»; reenvía (V-Notificacion-015 lo admite). No modifica ni borra (V-Notificacion-012/013, en el servicio).
- **Gestor del centro**: lee las notificaciones y adjuntos de los centros donde es gestor; reenvía las FALLIDO de esos centros (V-Notificacion-015). No da de alta: no ve «Todas» ni «Nueva notificación», y sus permisos son de solo lectura (`create="false"`), así que tampoco por REST.
- **Destinatario**: lee solo sus notificaciones ENVIADO (por DNI) y sus adjuntos, de cualquier centro.

Verificación: reset de BD y arranque; el log de importación de data-init sin errores.

### Eliminaciones declaradas (filas que aplican)

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `src/main/resources/data-init/input/auth.xml` | `Correo.propio-destinatario`, `Correo.propio-centro-supervisor`, `Adjunto.propio-destinatario`, `Adjunto.propio-centro-supervisor` del grupo `users`; comentario `<!-- Correos -->` | § Recursos y datos iniciales («sustituyen a los de los antiguos subsistemas») |
