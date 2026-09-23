---
type: implementation-task
template: expediente
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality

Esta es la tarea de la fase **TRAMITACION** (segunda fase declarada en `actual/v1/TipoExpedienteInstance.xml`). Al ser una **iniciativa de MODIFICACIÓN**, esta fase solo aporta a la tabla de la sección 6 **un** fichero: su `views.xml`. **MUST NOT** tocarse `tramitacion/PhaseEventManagerImpl.java` ni `tramitacion/StateEventValidatorImpl.kt`: el diseño los declara sin cambios y no están en la tabla de la sección 6.

## Fichero que cubre esta tarea (fila de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/tramitacion/views.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/fases/tramitacion/views.xml` |

## Cómo materializarlo

- **Origen:** `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/fases/tramitacion/views.xml`.
- **Destino resuelto:** `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/tramitacion/views.xml`.
- **Cópialo literalmente**, **sobrescribiendo** el fichero actual del árbol (y el esqueleto que hubiera dejado `CreateFilesTask`), **sin regenerarlo** ni reescribir ni una línea.

## Paso del diseño (verbatim)

### Paso 8 — `tramitacion/views.xml`

El fichero está materializado en `design/fases/tramitacion/views.xml`. **Cópialo literalmente** a `…/actual/v1/tramitacion/views.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Resumen estructural de la fase. El delta es que los tres forms de solo `EXIT` llevan ahora su panel `avisoEstadoExpediente`, que faltaba. Los cuatro forms siguen incluyendo `-datos-falta-view`, cuyo contenido se rehace en el paso 3:

| estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|
| `PENDIENTE_RESOLUCION` | `TRAMITADOR` | `-datos-profesor`, `-datos-falta-view`, `-pdfSolicitudFirmado`, `resolucion` | — / `RESOLVER` |
| `PENDIENTE_RESOLUCION` | — | `-datos-profesor`, `-datos-falta-view`, `-pdfJustificanteRegistroEntrada` + `avisoEstadoExpediente` | — / `EXIT` |
| `ACEPTADO` | — | `-datos-profesor`, `-datos-falta-view`, `-pdfResolucion`, `-resolucion-view` + `avisoEstadoExpediente` | — / `EXIT` |
| `RECHAZADO` | — | `-datos-profesor`, `-datos-falta-view`, `-pdfResolucion`, `-resolucion-view` + `avisoEstadoExpediente` | — / `EXIT` |

Verificación: el fichero existe; `diff` con el del diseño vacío; los cuatro forms incluyen `-datos-falta-view` y ninguno `-datos-falta`; los tres forms de solo `EXIT` llevan su `<panel name="avisoEstadoExpediente">` (`vistas.md` §6.1).

## Máquina de estados (sección 3 del diseño, verbatim)

## 3. Máquina de estados

*(sin cambios)*

El ciclo de vida completo —las dos fases `RECEPCION` y `TRAMITACION`, sus cinco estados, sus eventos y todas sus transiciones— se conserva tal cual está hoy en `actual/v1/TipoExpedienteInstance.xml` y en `actual/v1/estados.puml`. Esta iniciativa no toca ninguno de los dos ficheros, así que **no** se materializan en el diseño (`design-contract.md` §8, modo modificación). Lo único que cambia dentro del estado `RECEPCION` / `ENTRADA_DATOS` es **qué datos pide** la acción `GUARDAR_DATOS` y **qué comprueba** — ver secciones 9, 10 y 11.

> Por tanto **no hay tabla de transiciones en el diseño** que filtrar para esta fase: la referencia normativa de las transiciones de `TRAMITACION` es el `TipoExpedienteInstance.xml` y el `estados.puml` del as-is, que esta tarea **MUST NOT** modificar.

## 9. Especificación de los PhaseEventManagerImpl — `### Fase TRAMITACION` (verbatim, íntegra)

### Fase TRAMITACION

*(sin cambios)* — no se toca `tramitacion/PhaseEventManagerImpl.java`. Su cobertura sigue siendo `RESOLVER` → `triggerResolver` (con su ramificación por `tipoResolucion` y su `default → error`), y `onEnterPendienteResolucion`, `onEnterAceptado`, `onEnterRechazado`.

## 10. Especificación de los StateEventValidatorImpl — `### Fase TRAMITACION` (verbatim, íntegra)

### Fase TRAMITACION

*(sin cambios)* — no se toca `tramitacion/StateEventValidatorImpl.kt`. Su única pareja sigue siendo `PENDIENTE_RESOLUCION` + `RESOLVER`.

## Filas del reparto de reglas (sección 11) que aplican a esta fase

| Regla | Capa | Cómo |
|---|---|---|
| RUI-PENDIENTE_RESOLUCION-TRAMITADOR/GENERICA-001..004, RUI-ACEPTADO-GENERICA-001..004, RUI-RECHAZADO-GENERICA-001..004 | vista | los paneles anidados del gemelo de solo lectura `datos-falta-view` (`datos-falta-view-un-dia`, `datos-falta-view-varios-dias-completos`, `datos-falta-view-varios-dias-primero-parcial`), con los mismos `showIf` y el mismo `title="Fecha"`; los cuatro forms de `TRAMITACION` siguen incluyendo `-datos-falta-view` |

## Nota del diseño aplicable a esta fase (sección 14, verbatim)

- **Se añade el panel `avisoEstadoExpediente` a los cinco forms genéricos de solo `EXIT`** (`ENTRADA_DATOS` y `PENDIENTE_PRESENTACION` en `recepcion/views.xml`; `PENDIENTE_RESOLUCION`, `ACEPTADO` y `RECHAZADO` en `tramitacion/views.xml`). Va **más allá del delta de la spec**, que declara esas pantallas sin cambios (y la de `PENDIENTE_PRESENTACION` ni siquiera la toca el delta). Motivo: es una regla MUST de `k-tipo-expediente/vistas.md` §6.1 que el as-is incumplía, y como los dos `views.xml` de fase se sobrescriben enteros con la copia del diseño, dejarlos sin ese panel sería materializar a sabiendas un incumplimiento. El efecto visible es solo el texto de aviso del estado del expediente; ningún dato ni botón cambia.
