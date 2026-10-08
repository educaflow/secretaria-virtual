---
type: implementation-task
template: system
---

# Tarea 44 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-secure-coding
- k-code-quality

## Visibilidad del menú Del centro (MenuSecurityServiceImpl)

### Fila de la tabla «Ficheros a crear o modificar»

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/service/impl/MenuSecurityServiceImpl.java` | Modificar | k-vistas (menus.md) | `case` de `notificaciones-delCentro-menuitem` en lugar de `correos-delCentro-menuitem` |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

### Paso 12 (extracto verbatim del design.md: `MenuSecurityServiceImpl`)

### Paso 12 — Menús y visibilidad

`MenuSecurityServiceImpl.isVisible` (Modificar; el resto se conserva): se sustituye `case "correos-delCentro-menuitem" -> supervisor || user.tieneTipoUsuario(TipoUsuarioCodigo.ADMINISTRATIVO);` por `case "notificaciones-delCentro-menuitem" -> GestorNotificacionesUtil.esGestorEnAlgunCentro(user);`.

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

### Eliminaciones declaradas (filas que aplican)

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `MenuSecurityServiceImpl` | `case "correos-delCentro-menuitem"` | § Objetivo; sustituido por `notificaciones-delCentro-menuitem` |
