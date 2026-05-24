---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Esta tarea implementa el **controlador** `CorreoController`.

**Nota de contrato fijo:** los XML de vistas ya están copiados en `src/main/...` y son contrato fijo. Las firmas de los métodos `@CallMethod` deben coincidir con las acciones `<action-method method="action-..." class="...CorreoController"/>` declaradas en las vistas (`btnReenviar`, `onChangeDni`). **NO** regenerar ni editar esos XML; si detectas un error, DETENTE y notifica.

Fila de la tabla "Ficheros a crear o modificar":

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/correos/controller/CorreoController.java` | Crear | k-sistemas (controladores.md) | `@CallMethod` de `btnReenviar` y `onChangeDni`. |

---

## Paso 4 — Controladores

`controller/CorreoController.java` — `@Inject private ModelServiceFactory modelServiceFactory;`. Parámetros siempre `actionRequest` / `actionResponse`.

- `@CallMethod @Transactional btnReenviar(actionRequest, actionResponse)`: resuelve `CorreoService`; `arh.getOriginalModel()` + `arh.getModel(correoService.allowPropertiesReenviar())`; llama `validateReenviar` y, si OK, `reenviar`; `actionResponse.setSignal("refresh-tab", null)`. La autorización (solo Administrador) vive en el SERVICIO, no aquí (controladores.md, k-secure-coding §4).
- `@CallMethod onChangeDni(actionRequest, actionResponse)` (U-correo-001 / R-Correo-004): obtiene el DNI del contexto, llama `correoService.proponerEmailPorDni(dni)` y `actionResponse.setValue("emailDestinatario", email)` (queda editable por el Administrador).

> Para resolver el email por DNI, `proponerEmailPorDni` usa un finder `findByDni` sobre `User`. Se añade `<finder-method name="findByDni" using="String:dni" filter="self.dni = :dni"/>` a `subsystem/common/domains/User.xml` (espejo del patrón existente en `CertificadoDigital`), salvo que ya exista una vía equivalente en `registrousuario`.

> **NO** se crea `AdjuntoCorreoController`: la descarga del adjunto la da el widget `binary` de la columna `contenido` (endpoint nativo de `MetaFile`).
> **U-grafica-002** (fecha final < inicial) **NO** se implementa con un controlador: se resuelve por el filtro `BETWEEN` del dataset (rango invertido → gráfica vacía).

---

## Trazabilidad V/R/U → ubicación (aplicable a esta tarea)

| ID | Ubicación |
|----|-----------|
| R-Correo-004 (proponer email por DNI) | UI: `CorreoController.onChangeDni` + `CorreoService.proponerEmailPorDni` + `setValue` (campo cliente). Ver U-correo-001. |
| U-correo-001 (autocompletar email onChange dni) | `onChange="...@Main-onChange-dni-action"` → `<action-method>` → `CorreoController.onChangeDni`. |
| U-correo-005 (Reenviar solo admin+FALLIDO) | `showIf="id != null && estado == 'FALLIDO'"` en `btnReenviar` + `groups` del menú; defensa en `validateReenviar`. |

## Paso 8 — Seguridad (aplicable a esta tarea)
- **Autorización por rol:** "solo Administrador crea/reenvía/ve la gráfica" → para reenviar, la defensa vive dentro de `validateReenviar`/`reenviar` con `SecurityUtil`. **Nunca en el controlador.**
