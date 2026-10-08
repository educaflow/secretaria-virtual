---
type: implementation-task
template: system
---

# Tarea 42 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas
- k-sistemas
- k-secure-coding
- k-code-quality

## Visibilidad del menú «Del centro» (MenuSecurityServiceImpl)

Las decisiones difíciles, con sus alternativas, están en [`decisiones.md`](decisiones.md) (D1–D6); este documento las cita por su número.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/security/service/impl/MenuSecurityServiceImpl.java` | Modificar | k-vistas (menus.md) | `case` de `notificaciones-delCentro-menuitem` en lugar de `correos-delCentro-menuitem` |

Para una clase Java con `Acción: Modificar` se declaran solo las firmas nuevas o cambiadas; **el resto de la clase se conserva**.

## Paso del diseño

### Paso 12 — Menús y visibilidad

`MenuSecurityServiceImpl.isVisible` (Modificar; el resto se conserva): se sustituye `case "correos-delCentro-menuitem" -> supervisor || user.tieneTipoUsuario(TipoUsuarioCodigo.ADMINISTRATIVO);` por `case "notificaciones-delCentro-menuitem" -> GestorNotificacionesUtil.esGestorEnAlgunCentro(user);`.

Verificación: `./gradlew -q test --tests '*Categoria10*'` y `MenuSecurityServiceImplTest`.

## Eliminaciones declaradas

| Fichero | Elemento eliminado | Justificación (ID de spec) |
|---|---|---|
| `MenuSecurityServiceImpl` | `case "correos-delCentro-menuitem"` | § Objetivo; sustituido por `notificaciones-delCentro-menuitem` |
