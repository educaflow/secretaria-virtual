---
type: implementation-task
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-i18n

Esta tarea implementa el **servicio AdjuntoCorreo** (interfaz + implementación), que garantiza la inmutabilidad del adjunto.

**Nota de contrato fijo:** el XML del dominio `AdjuntoCorreo.xml` ya está copiado en `src/main/...` y es contrato fijo. Las firmas Java deben coincidir con la entidad generada. **NO** regenerar ni editar ese XML; si detectas un error, DETENTE y notifica.

Filas de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/service/AdjuntoCorreoService.java` | Crear | k-sistemas | Interfaz `extends ModelService<AdjuntoCorreo>` (inmutabilidad). |
| `subsystem/correos/service/impl/AdjuntoCorreoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Inmutabilidad del adjunto. |

---

## Paso 2 — Servicios

### `AdjuntoCorreoService` / `AdjuntoCorreoServiceImpl`

```java
public interface AdjuntoCorreoService extends ModelService<AdjuntoCorreo> { /* solo sobrescribe heredados */ }

public class AdjuntoCorreoServiceImpl extends DefaultModelService<AdjuntoCorreo> implements AdjuntoCorreoService {
    public AdjuntoCorreoServiceImpl(Class<AdjuntoCorreo> model, Repository<AdjuntoCorreo> repository) { super(model, repository); }
    // validateInsert  -> V-AdjuntoCorreo-001 (nombreFichero), V-AdjuntoCorreo-002 (contenido)
    // validateUpdate  -> V-AdjuntoCorreo-003 (SIEMPRE rechaza: adjunto inmutable)
    // update          -> UnsupportedOperationException (k-secure-coding §9.2)
    // allowPropertiesInsert -> createAllowProperties(Map.of("nombreFichero", Map.of(), "contenido", Map.of()))  // 'correo' es servidor: fuera
}
```

---

## Frontera de confianza — AllowProperties por acción (aplicable)

- **AdjuntoCorreo cliente (2):** `nombreFichero`, `contenido`. **AdjuntoCorreo servidor (1):** `correo`.

### `AdjuntoCorreoServiceImpl.insert` (cascada del maestro)

**Forma:** `createAllowProperties(Map.of("nombreFichero", Map.of(), "contenido", Map.of()))`. `correo` (servidor) fuera de la whitelist; lo fija el `onNew __parent__` del modal hijo.

### `AdjuntoCorreoServiceImpl.update`

Inmutable: `UnsupportedOperationException` + `validateUpdate` rechaza. Ningún campo aceptado.

Reglas aplicadas (k-secure-coding §3): ninguna whitelist enumera un campo `servidor` ✔; no se usa `createAllowAllProperties()` ✔.

---

## Trazabilidad V/R/U → ubicación (aplicable a esta tarea)

| ID | Ubicación |
|----|-----------|
| V-AdjuntoCorreo-001 (nombre obligatorio) | `AdjuntoCorreoServiceImpl.validateInsert` + `required="true"` en `nombreFichero`. |
| V-AdjuntoCorreo-002 (contenido obligatorio) | `AdjuntoCorreoServiceImpl.validateInsert` + `required="true"` en `contenido`. |
| V-AdjuntoCorreo-003 (adjunto inmutable) | `AdjuntoCorreoServiceImpl.validateUpdate` (rechaza siempre) + `update` lanza `UnsupportedOperationException`. |
| R-AdjuntoCorreo-001 (vincular al padre) | `subsysCorreos.Correo.AdjuntoCorreo@Main-set-correo-parent-action` (`<action-record>` con `__parent__`) — está en la vista, no en este servicio. |
