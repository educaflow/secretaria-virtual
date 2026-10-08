---
type: implementation-task
template: system
---

# Tarea 35 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-sistemas
- k-secure-coding
- k-code-quality

## MenuSecurityServiceImpl: visibilidad de «Del centro» de notificaciones

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/service/impl/MenuSecurityServiceImpl.java` | Modificar | k-vistas (menus.md) | `case` de `notificaciones-delCentro-menuitem` en lugar de `correos-delCentro-menuitem` |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 12 — Menús y visibilidad (extracto)

`MenuSecurityServiceImpl.isVisible` (Modificar; el resto se conserva): se sustituye `case "correos-delCentro-menuitem" -> supervisor || user.tieneTipoUsuario(TipoUsuarioCodigo.ADMINISTRATIVO);` por `case "notificaciones-delCentro-menuitem" -> GestorNotificacionesUtil.esGestorEnAlgunCentro(user);`.

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `MenuSecurityServiceImpl` | `case "correos-delCentro-menuitem"` | § Objetivo; sustituido por `notificaciones-delCentro-menuitem` |
