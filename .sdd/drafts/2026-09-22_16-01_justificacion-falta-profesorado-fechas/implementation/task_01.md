---
type: implementation-task
template: expediente
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding

Esta es una **iniciativa de MODIFICACIÓN** de la versión existente `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/`. No se añade ninguna fase nueva, así que **MUST NOT** ejecutarse `CreateFilesTask`: todos los esqueletos ya existen en el árbol.

## Identidad del trámite y del tipo

| Dato | Valor |
|---|---|
| `code` del trámite | `JustificacionFaltaProfesorado` |
| Nombre visible (`<name>`) | Justificación de falta del profesorado |
| `tipoTramite` | `PROFESOR` |
| Carpeta del trámite | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/` |
| Carpeta de la versión | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` |
| `<defaultTipoExpediente>` | `v1` |
| `code` / entidad del tipo | `JustificacionFaltaProfesoradoV1` |
| FQN de la entidad | `com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1` |
| `basePackageName` | `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1` |
| Clase `States` (generada) | `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.States` |
| Form plantilla | `exp-JustificacionFaltaProfesoradoV1-Templates` |
| Modificación de | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` |

(`actual` es un segmento de agrupación intermedio; la carpeta de versión es `v1`.)

## Fichero que cubre esta tarea (fila de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/domains.xml` |

## Paso del diseño (verbatim)

### Paso 1 — `domains.xml`

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Verificación: el fichero existe; `diff` con el del diseño vacío; el fichero ya no contiene `dias`, `mes`, `anyo` ni `MesJustificacionFaltaProfesoradoV1`; ningún campo lleva `required="true"`.

### Cómo materializarlo

- **Origen:** `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/domains.xml`.
- **Destino resuelto:** `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml`.
- **Cópialo literalmente**, **sobrescribiendo** el fichero actual del árbol (y el esqueleto que hubiera dejado `CreateFilesTask`, si lo hubiera), **sin regenerarlo** y sin reescribir ni una línea. El XML materializado del diseño es **contrato fijo**.
- **MUST NOT** crearse ni tocarse `i18n_es.csv` ni `i18n_ca.csv`: los genera un script.

## 4. Modelo

### 4.1 Campos de la entidad

Tabla completa de los campos propios de `JustificacionFaltaProfesoradoV1` tras el delta. La columna **delta** dice qué hace esta iniciativa con cada uno.

| nombre | tipo | ref | title | para qué sirve | quién lo rellena | delta |
|---|---|---|---|---|---|---|
| `tipoJornadaFalta` | `enum` | `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` | Tipo de jornada faltada | En qué forma se faltó; decide qué campos del periodo se piden | `usuario` | **cambia** (el enum pasa de 2 a 4 ítems; se le añade `title`) |
| `fechaInicio` | `date` | — | Fecha de Inicio | Primer día de la ausencia (se titula «Fecha» cuando la ausencia es de un solo día) | `usuario` | **nuevo** |
| `fechaFin` | `date` | — | Fecha de fin | Último día de la ausencia; solo en los tipos que abarcan varios días | `usuario` | **nuevo** |
| `horaInicio` | `time` | — | Hora de inicio | Hora en que empezó la ausencia; solo en los tipos con jornada parcial | `usuario` | **cambia** (se le añade `title`) |
| `horaFin` | `time` | — | Hora de fin | Hora en que terminó la ausencia; solo en «Unas horas de un único día» | `usuario` | **cambia** (se le añade `title`) |
| `dias` | `string` | — | Días | Días sueltos del mes en que se faltó | `usuario` | **se elimina** |
| `mes` | `enum` | `MesJustificacionFaltaProfesoradoV1` | Mes | Mes de la falta | `usuario` | **se elimina** |
| `anyo` | `integer` | — | Año | Año de la falta | `usuario` | **se elimina** |
| `motivoFalta` | `enum` | `MotivoFaltaJustificacionFaltaProfesoradoV1` | Motivo falta | Motivo tipificado de la ausencia | `usuario` | sin cambios |
| `otroMotivo` | `string` | — | Otro motivo | Explicación del motivo cuando es «Otros» | `usuario` | sin cambios |
| `disconformidad` | `string` | — | Explicar los datos a subsanar | Qué debe subsanar el profesor | `usuario` | sin cambios |
| `resolucion` | `string` | — | Motivo del rechazo | Motivo por el que se rechaza | `usuario` | sin cambios |
| `tipoResolucion` | `enum` | `TipoResolucionJustificacionFaltaProfesoradoV1` | Tipo resolución | Sentido de la resolución | `usuario` | sin cambios |
| `justificante` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Foto o PDF del justificante | Justificante que aporta el profesor | `usuario` | sin cambios |
| `pdfSolicitud` | `many-to-one` | `com.axelor.meta.db.MetaFile` | PDF de la solicitud | Solicitud generada por el servidor en `GUARDAR_DATOS` | `servidor` | sin cambios |
| `pdfSolicitudFirmado` | `many-to-one` | `com.axelor.meta.db.MetaFile` | PDF de la solicitud | Solicitud firmada (en cliente con AutoFirma__!! o en servidor) | `servidor` (excepción de firma en cliente: sí aparece en el validador de `PRESENTAR`) | sin cambios |
| `pdfJustificanteRegistroEntrada` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Justificante del registro de entrada | Resguardo del registro de entrada | `servidor` | sin cambios |
| `pdfResolucion` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Resolución | Resolución firmada y registrada de salida | `servidor` | sin cambios |

- Ningún campo lleva `required="true"`: el expediente vive guardado desde `ENTRADA_DATOS` con todo vacío y la obligatoriedad es **por pareja (estado, evento)**, en el DSL del validador (sección 10).
- Los cinco campos del periodo son `usuario`, así que **MUST** aparecer los cinco en un `field(...)` de `getForStateEntradaDatosInEventGuardarDatos`: sin eso el `Tramitador` no los copiaría desde la petición y el profesor escribiría en el vacío.
- `claveCertificado`, `situacionFirma` y `firmaEnServidor` no son campos de esta entidad (los dos últimos son campos *dummy* de vista) y no los toca el delta.

### 4.2 Enums

| enum | numeric | ítem | title | value |
|---|---|---|---|---|
| `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` | — | `UN_DIA_COMPLETO` | Un día completo | — |
| `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` | — | `VARIOS_DIAS_COMPLETOS` | Varios días (todos ellos completos) | — |
| `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` | — | `UNAS_HORAS_UN_DIA` | Unas horas de un único día | — |
| `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` | — | `VARIOS_DIAS_PRIMERO_PARCIAL` | Varios días pero del primer día solo faltó unas horas | — |
| `MotivoFaltaJustificacionFaltaProfesoradoV1` | — | *(sus 9 ítems, sin cambios)* | | |
| `TipoResolucionJustificacionFaltaProfesoradoV1` | — | *(sus 3 ítems, sin cambios)* | | |

- `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` **se redefine en su sitio**: conserva el nombre y sustituye sus dos ítems (`TODA_LA_JORNADA`, `JORNADA_PARCIAL`) por los cuatro de arriba. No se crea un enum nuevo.
- `MesJustificacionFaltaProfesoradoV1` **se elimina por completo**: se queda sin ningún campo que lo referencie.

### 4.3 Tipo de jornada faltada → campos del periodo (CONTRATO)

El dueño de esta clasificación es el par `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` (los modos posibles, en `domains.xml`) + los tres métodos `necesita*` de `JustificacionFaltaProfesoradoV1Util` (qué campos pide cada modo, sección 9). Esta tabla es una **proyección** de esas dos piezas, para lectura rápida, no su fuente. Añadir un valor al enum obliga a tocar, **y exactamente**, estos sitios:

1. `domains.xml` — el nuevo ítem del enum `TipoJornadaFaltaJustificacionFaltaProfesoradoV1`.
2. `JustificacionFaltaProfesoradoV1Util` — el compilador exige clasificar el nuevo ítem en los tres `necesita*` (cada uno es un `switch` exhaustivo sobre el enum, sin `default`).
3. `views.xml` (raíz de la versión) — dentro de `datos-falta`, o bien un `<panel>` anidado nuevo con sus campos, su título y su `required`, o bien (si comparte título de `fechaInicio` con un panel existente y sus campos de más van al final de la fila) añadir el valor al `showIf` de ese panel y a los `showIf` de los campos que solo él usa, como hace `datos-falta-un-dia`.
   Y **lo mismo en `datos-falta-view`**, el gemelo de solo lectura que incluye TRAMITACION: sus paneles anidados `datos-falta-view-*` repiten los de `datos-falta`, con los campos en `readonly="true"` y sin `required` (`decisiones.md` D4).
4. `recepcion/views.xml` — añadir el nuevo valor a las condiciones `if` del `<action-record>` `exp-JustificacionFaltaProfesoradoV1-onChange-tipoJornadaFalta-action` que correspondan (los campos que el nuevo modo **sí** necesita).
5. `documentospdf/solicitud.xml` — un `<check>` nuevo, en la sección «Declara que», con su propio `<castellano>` (y `<valenciano>` si no debe traducirse solo) interpolando los campos del periodo que ese modo usa.

Nada más: el validador (`StateEventValidatorImpl.kt`) no cambia porque sus `ifLambda(util::necesita…)` son genéricos y ya delegan en el punto 2.

| tipo de jornada faltada | `fechaInicio` | `fechaFin` | `horaInicio` | `horaFin` | título de `fechaInicio` |
|---|---|---|---|---|---|
| `UN_DIA_COMPLETO` | sí | — | — | — | «Fecha» |
| `VARIOS_DIAS_COMPLETOS` | sí | sí | — | — | «Fecha de Inicio» |
| `UNAS_HORAS_UN_DIA` | sí | — | sí | sí | «Fecha» |
| `VARIOS_DIAS_PRIMERO_PARCIAL` | sí | sí | sí | — | «Fecha de Inicio» |
| *(sin elegir)* | sí | — | — | — | «Fecha de Inicio» |

La fila *(sin elegir)* es solo de pantalla (RUI-004 y RUI-006: la fecha de inicio se ve y se marca obligatoria siempre, con su título por defecto): la cubre el panel `datos-falta-varios-dias-completos`, cuyo `showIf` incluye `!tipoJornadaFalta`, con `fechaFin` oculto por su propio `showIf`. No es un modo del enum, así que no entra en los `necesita*` ni en la lista de sitios de arriba.

### 4.4 `domains.xml`

Materializado completo en `design/domains.xml` (fichero real + delta). Mantiene el `<module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>`, la entidad como **primera** `<entity>` con `extends="Expediente"` y el `<extra-code-model>` con sus dos constantes (`SOLICITUD`, `RESOLUCION`), que no cambia.

## 5. Documentos PDF

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/solicitud.xml` | `SOLICITUD` | `RECEPCION.ENTRADA_DATOS` + `GUARDAR_DATOS` | `pdfSolicitud` (`servidor`) | `cliente (AutoFirma__!!)` o `servidor: DNI del usuario autenticado`, en `PRESENTAR` | `entrada` (en `PRESENTAR`) |
| `documentospdf/resolucion.xml` | `RESOLUCION` | `TRAMITACION.PENDIENTE_RESOLUCION` + `RESOLVER` | `pdfResolucion` (`servidor`) | `servidor: DIRECTOR` | `salida` |

Fragmentos — **dos**, uno por bloque común a los dos documentos (`k-tipo-expediente/documentos.md` §2.5: un fragmento por cada bloque compartido, nunca un cajón de sastre con todos dentro; secciones comunes que van siempre seguidas y en el mismo orden son **un solo** bloque; y no se factoriza un bloque que un solo documento incluye):

| fragmento | contenido | lo incluye |
|---|---|---|
| `documentospdf/_template.xml` (se **conserva** y se modifica) | Centro de presentación (nombre, código, municipio) seguido de Datos del interesado (DNI, apellidos, nombre): en los dos documentos van seguidas y en este orden | `solicitud.xml`, `resolucion.xml` |
| `documentospdf/_documentacionAportada.xml` (**nuevo**) | Nombre del fichero del justificante + su hash (aparte porque no va seguido del bloque anterior: entre los dos va la sección «Declara que» de cada documento) | `solicitud.xml`, `resolucion.xml` |

(La sección 5 completa, con su delta y el detalle de las cuatro casillas, va en la tarea de `documentospdf/`. Aquí se copia porque es de donde salen las constantes del `<extra-code-model>` del `domains.xml`: `SOLICITUD` y `RESOLUCION`.)

## Reglas del diseño que viven en el modelo (sección 11)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<date>`/`<time>`/`<enum>`/`<string>`/`<many-to-one>` |

Reglas duras que se respetan: ningún `required="true"` en el `domains.xml`; ningún dato de usuario validado en un `trigger*` (RN-001 no valida, **normaliza**); ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.

## Nota de despliegue del diseño (sección 14, verbatim)

- **PRECONDICIÓN de despliegue: la base de datos se vacía.** El delta no es compatible con los expedientes v1 ya guardados: borra las columnas `dias`, `mes` y `anyo` y el enum `MesJustificacionFaltaProfesoradoV1`, y quita de `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` los ítems `TODA_LA_JORNADA`/`JORNADA_PARCIAL`. Por `k-tipo-expediente/recetas/versionado.md` §4, un cambio así **MUST** ir en una v2; hacerlo sobre la propia v1 solo es legítimo porque la especificación da por hecho que la BD se vacía antes de desplegar (y declara fuera de alcance migrar los expedientes existentes). Si se desplegara con expedientes v1 vivos, los que tengan `TODA_LA_JORNADA`/`JORNADA_PARCIAL` **no se pueden cargar** (el valor ya no corresponde a ningún ítem del enum), y habría que crear una v2 con la receta de versionado en vez de modificar la v1.
