---
type: implementation-task
template: expediente
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-i18n

## Fichero que cubre esta tarea (fila de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/views.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/views.xml` |

## Cómo materializarlo

- **Origen:** `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/views.xml`.
- **Destino resuelto:** `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/views.xml`.
- **Cópialo literalmente**, **sobrescribiendo** el fichero actual del árbol (y el esqueleto que hubiera dejado `CreateFilesTask`), **sin regenerarlo** ni reescribir ni una línea. El XML materializado del diseño es **contrato fijo**.

## Paso del diseño (verbatim)

### Paso 3 — `views.xml` de la raíz de la versión

El fichero está materializado en `design/views.xml`. **Cópialo literalmente** a `…/actual/v1/views.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Resumen estructural del form plantilla `exp-JustificacionFaltaProfesoradoV1-Templates` (paneles hijos directos, todos con `name`): `datos-profesor`, `datos-falta`, `datos-falta-view`, `justificante-upload`, `justificante-view`, `pdfSolicitud`, `pdfSolicitudFirmado`, `pdfJustificanteRegistroEntrada`, `pdfResolucion`, `resolucion`, `resolucion-view`, `disconformidad-view`. Respecto al fichero actual se rehacen `datos-falta` y su gemelo de solo lectura `datos-falta-view` (`decisiones.md` D4).

**ASCII Layout de `datos-falta`** (uno de los dos paneles que cambian; un dibujo por estado del `showIf`, que es exclusivo por tipo de jornada faltada):

```
jjjjjjj·····   ← tipoJornadaFalta(7) [SwitchSelect vertical, 4 opciones de texto largo] + libre al borde
```
panel anidado `datos-falta-un-dia` (`tipoJornadaFalta == 'UN_DIA_COMPLETO' || tipoJornadaFalta == 'UNAS_HORAS_UN_DIA'`), modo `UN_DIA_COMPLETO`:
```
iii·········   ← fechaInicio «Fecha»(3); horaInicio y horaFin ocultas por su showIf (hueco al borde derecho)
```
mismo panel `datos-falta-un-dia`, modo `UNAS_HORAS_UN_DIA`:
```
iiihhhggg···   ← fechaInicio «Fecha»(3) + horaInicio(3, showIf) + horaFin(3, showIf)
```
panel anidado `datos-falta-varios-dias-completos` (`!tipoJornadaFalta || tipoJornadaFalta == 'VARIOS_DIAS_COMPLETOS'`), modo `VARIOS_DIAS_COMPLETOS`:
```
iiifff······   ← fechaInicio «Fecha de Inicio»(3) + fechaFin «Fecha de fin»(3, showIf VARIOS_DIAS_COMPLETOS)
```
mismo panel `datos-falta-varios-dias-completos`, **sin tipo elegido** (expediente recién creado):
```
iii·········   ← fechaInicio «Fecha de Inicio»(3, obligatoria); fechaFin oculta por su showIf (hueco al borde derecho)
```
panel anidado `datos-falta-varios-dias-primero-parcial` (`tipoJornadaFalta == 'VARIOS_DIAS_PRIMERO_PARCIAL'`):
```
iiihhhfff···   ← fechaInicio «Fecha de Inicio»(3) + horaInicio(3) + fechaFin «Fecha de fin»(3)
```
última fila del panel:
```
mmmmmmmooooo   ← motivoFalta(7) + otroMotivo(5, showIf motivoFalta=='OTROS', al borde derecho)
```

Notas del layout: los campos del periodo van en su **orden temporal natural** (inicio antes que fin, fecha antes que hora del mismo extremo) y en la **misma fila**, porque son un solo grupo semántico; cada fecha y cada hora son 3 columnas según la tabla de proporcionalidad (nunca 6 ni 12); los bordes de columna de los cuatro modos coinciden (3 | 6 | 9), y el borde de `tipoJornadaFalta` coincide con el de `motivoFalta` (7 | 8). `otroMotivo` es el **último elemento de su fila**, así que su `showIf` va en el propio campo y no necesita panel. Por la misma razón `horaInicio` y `horaFin` llevan su `showIf` en el propio campo dentro de `datos-falta-un-dia`: son el final de su fila, así que al ocultarse dejan el hueco en el borde derecho sin desplazar a nadie; e igual `fechaFin` dentro de `datos-falta-varios-dias-completos`, que se oculta cuando todavía no hay tipo elegido, porque ese panel también cubre ese estado para que la fecha de inicio se vea y se marque obligatoria desde que se abre la pantalla con su título por defecto «Fecha de Inicio» (RUI-004, RUI-006). Los modos `UN_DIA_COMPLETO` y `UNAS_HORAS_UN_DIA` comparten ese panel porque los dos titulan la fecha «Fecha»; así ningún panel anidado envuelve un solo campo (`k-vistas/forms.md`, checklist de maquetación). Los tres paneles anidados sí son obligatorios: son grupos que se muestran de forma **exclusiva** según otro campo (y con títulos distintos de `fechaInicio`), y sin ellos los huecos de los campos ocultos en mitad de la fila desplazarían a los de detrás.

**ASCII Layout de `datos-falta-view`**. Es el gemelo de solo lectura: lo incluyen, con el prefijo `-`, los cuatro forms de TRAMITACION y el form genérico de `ENTRADA_DATOS`. Es el mismo dibujo que `datos-falta`, con una diferencia:
- `tipoJornadaFalta` y `motivoFalta` van **sin widget** (desplegable en solo lectura), así que cada uno ocupa una sola línea con su valor como texto, en vez de una lista vertical con todas las opciones.
Los `showIf` son idénticos a los de `datos-falta`, incluida la fila *(sin elegir)*, porque el form genérico de `ENTRADA_DATOS` puede abrir un expediente recién creado sin tipo. En ese caso `datos-falta-view-varios-dias-completos` muestra «Fecha de Inicio» vacía y oculta `fechaFin` por su `showIf` propio, igual que la pantalla editable. En TRAMITACION el tipo siempre está elegido, así que esa fila no se da.
```
jjjjjjj·····   ← tipoJornadaFalta(7) [texto, una línea]
iii·········   ← UN_DIA_COMPLETO: fechaInicio «Fecha»(3)                                    (datos-falta-view-un-dia)
iiihhhggg···   ← UNAS_HORAS_UN_DIA: fechaInicio «Fecha»(3) + horaInicio(3) + horaFin(3)     (datos-falta-view-un-dia)
iiifff······   ← VARIOS_DIAS_COMPLETOS: fechaInicio(3) + fechaFin(3)                         (datos-falta-view-varios-dias-completos)
iii·········   ← sin tipo elegido: fechaInicio «Fecha de Inicio»(3); fechaFin oculta           (datos-falta-view-varios-dias-completos)
iiihhhfff···   ← VARIOS_DIAS_PRIMERO_PARCIAL: fechaInicio(3) + horaInicio(3) + fechaFin(3)   (datos-falta-view-varios-dias-primero-parcial)
mmmmmmmooooo   ← motivoFalta(7) [texto, una línea] + otroMotivo(5, showIf motivoFalta=='OTROS')
```
De las filas del periodo solo se ve una, la del tipo elegido (o la de «sin tipo elegido»). Los paneles anidados del gemelo llevan el prefijo `datos-falta-view-` para que sus `name` no choquen con los de `datos-falta` en el mismo form plantilla.

Verificación: el fichero existe; `diff` con el del diseño vacío; hay **exactamente un** `<form name="exp-JustificacionFaltaProfesoradoV1-Templates">`; existen `datos-falta` y `datos-falta-view`; ningún `<form state=…>` en este fichero.

## Filas del reparto de reglas (sección 11) que viven en este fichero

| Regla | Capa | Cómo |
|---|---|---|
| RUI-ENTRADA_DATOS-CREADOR-001/002/003 mostrar fecha de fin / hora de inicio / hora de fin | vista | tres `<panel>` anidados con `showIf` dentro de `datos-falta`: `datos-falta-un-dia` (`UN_DIA_COMPLETO` o `UNAS_HORAS_UN_DIA`; `horaInicio`/`horaFin` con `showIf` propio a `UNAS_HORAS_UN_DIA`), `datos-falta-varios-dias-completos` (`VARIOS_DIAS_COMPLETOS` o sin tipo elegido; `fechaFin` con `showIf` propio a `VARIOS_DIAS_COMPLETOS`) y `datos-falta-varios-dias-primero-parcial` |
| RUI-ENTRADA_DATOS-CREADOR-004 título dinámico de la fecha de inicio | vista | `title="Fecha"` en los paneles de `UN_DIA_COMPLETO` y `UNAS_HORAS_UN_DIA`; en los otros dos (y sin tipo elegido, que lo cubre `datos-falta-varios-dias-completos`) hereda «Fecha de Inicio» del `domains.xml` |
| RUI-ENTRADA_DATOS-CREADOR-005..009 marcar como obligatorio | vista | `required="true"` estático en cada campo, dentro del panel de su modo; `fechaInicio` (RUI-006, «siempre») está en los tres paneles y el `showIf` de `datos-falta-varios-dias-completos` incluye `!tipoJornadaFalta`, así que se ve y sale obligatoria desde que se abre la pantalla, también sin tipo elegido |
| RUI-ENTRADA_DATOS-GENERICA-001..004 | vista | los paneles anidados del gemelo de solo lectura `datos-falta-view`, con los mismos `showIf` y el mismo `title="Fecha"`; el form genérico de `ENTRADA_DATOS` incluye `-datos-falta-view` |
| RUI-PENDIENTE_RESOLUCION-TRAMITADOR/GENERICA-001..004, RUI-ACEPTADO-GENERICA-001..004, RUI-RECHAZADO-GENERICA-001..004 | vista | los paneles anidados del gemelo de solo lectura `datos-falta-view` (`datos-falta-view-un-dia`, `datos-falta-view-varios-dias-completos`, `datos-falta-view-varios-dias-primero-parcial`), con los mismos `showIf` y el mismo `title="Fecha"`; los cuatro forms de `TRAMITACION` siguen incluyendo `-datos-falta-view` |

Además, del reparto de capas de la sección 11: «Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf` en el panel del modo, `required`, prefijo `-`, `<action-record>` del `onChange` — **solo UX, NUNCA defensa**».

## Notas del diseño aplicables a este fichero (sección 14, verbatim)

- **El `title` de los mensajes de error de la fecha de inicio es siempre «Fecha de Inicio».** El motor de validación compone el mensaje con el `@Widget(title=…)` del `domains.xml`, no con el `title` de la vista, así que en el modo «Un día completo» el usuario lee «Fecha de Inicio: Debe indicar la fecha» aunque el campo se titule «Fecha» en pantalla. El texto que la especificación exige es el del mensaje, y ese sí es literal.
- **Se mantiene el gemelo `datos-falta-view`** y se rehace en paralelo a `datos-falta`, así que los tres paneles anidados del periodo están escritos dos veces. No se puede reutilizar `-datos-falta` en las pantallas de solo lectura (las cuatro de TRAMITACION y la genérica de `ENTRADA_DATOS`). En solo lectura un `SwitchSelect` vertical sigue pintando todas sus opciones, y esas pantallas apilarían 13 (4 de `tipoJornadaFalta` + 9 de `motivoFalta`) para enseñar dos valores (`decisiones.md` D4). Añadir un modo exige tocar los dos paneles (sección 4.3, punto 3).
