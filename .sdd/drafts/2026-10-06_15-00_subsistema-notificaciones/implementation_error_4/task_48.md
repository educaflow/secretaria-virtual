---
type: implementation-task
template: system
---

# Tarea 48 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Ficheros

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Grupo `users`: los 4 permisos `Correo.*`/`Adjunto.*` → los 8 de notificaciones; quita el comentario huérfano `<!-- Correos -->` |

## Diseño — Paso 13 (extracto)

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

`src/main/resources/data-init/input/auth.xml`, grupo `users`: las líneas `Correo.propio-destinatario`, `Correo.propio-centro-supervisor`, `Adjunto.propio-destinatario`, `Adjunto.propio-centro-supervisor` se sustituyen por los 8 permisos de la tabla; se quita el comentario `<!-- Correos -->` (sección vacía). El grupo `admins` no necesita ninguno (`AuthSecurity` no filtra al administrador).

Verificación: reset de BD y arranque; el log de importación de data-init sin errores.

## Eliminaciones declaradas (filas que aplican)

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `src/main/resources/data-init/input/auth.xml` | `Correo.propio-destinatario`, `Correo.propio-centro-supervisor`, `Adjunto.propio-destinatario`, `Adjunto.propio-centro-supervisor` del grupo `users`; comentario `<!-- Correos -->` | § Recursos y datos iniciales («sustituyen a los de los antiguos subsistemas») |

**Nota del descomponedor:** `Acción: Modificar` sobre un XML que el diseño no materializa: se edita in situ solo el grupo `users` como indica el Paso 13, conservando todo lo demás.
