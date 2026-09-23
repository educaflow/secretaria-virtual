---
type: design
template: expediente
---

# Diseño: Justificación de falta del profesorado

## 1. Objetivo

Permitir que un profesor justifique ante su centro la falta a su puesto de trabajo indicando el periodo exacto de la ausencia (un día, varios días, unas horas o varios días con el primero parcial).

## 2. Identidad del trámite y del tipo

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

## 3. Máquina de estados

*(sin cambios)*

El ciclo de vida completo —las dos fases `RECEPCION` y `TRAMITACION`, sus cinco estados, sus eventos y todas sus transiciones— se conserva tal cual está hoy en `actual/v1/TipoExpedienteInstance.xml` y en `actual/v1/estados.puml`. Esta iniciativa no toca ninguno de los dos ficheros, así que **no** se materializan en el diseño (`design-contract.md` §8, modo modificación). Lo único que cambia dentro del estado `RECEPCION` / `ENTRADA_DATOS` es **qué datos pide** la acción `GUARDAR_DATOS` y **qué comprueba** — ver secciones 9, 10 y 11.

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

La sección «Declara que» **no** es un fragmento, porque ya no es literalmente idéntica en los dos documentos (`k-tipo-expediente/documentos.md` §2.5): va **inline** en cada uno, en el mismo punto en el que estaba dentro del antiguo `_template.xml` (entre los datos del interesado y la documentación aportada).
- En `solicitud.xml` lleva las cuatro casillas del periodo + la fila de motivo/otro motivo.
- En `resolucion.xml` lleva **solo** la fila de motivo/otro motivo, literalmente igual que la del as-is.

**Delta:** `documentospdf/_template.xml` deja de ser un cajón de sastre con los cuatro bloques y se queda **solo** con el primero (centro + interesado). Conserva su nombre porque el contrato no contempla borrar ni renombrar ficheros (la columna `Acción` de §6 solo admite `Crear`/`Modificar`), aunque `documentos.md` §2.5 pediría un nombre por contenido (ver §14 y `decisiones.md` D5). La documentación aportada pasa al fragmento nuevo `_documentacionAportada.xml`, y la sección «Declara que» pasa a ir inline en cada documento. Dentro de la de `solicitud.xml` desaparece la fila de `dias`/`mes`/`anyo` y las dos casillas «Toda la jornada» / «Jornada parcial…», y en su lugar quedan **cuatro casillas**, una por tipo de jornada faltada, con los textos que fija la especificación. **Cambio visible en la Resolución:** deja de mostrar el periodo (la fila de días/mes/año y las casillas de jornada), porque esos campos desaparecen del modelo; conserva el motivo de la falta y todo lo demás, en el mismo orden (ver §14). `solicitud.xml` y `resolucion.xml` **sí se tocan**: pasan de un solo `<include>` (`_template.xml`) a dos (`_template.xml`, `_documentacionAportada.xml`) con su sección «Declara que» inline entre ellos; el resto de su contenido propio no cambia.

Cada una de las cuatro casillas de la sección «Declara que» de `solicitud.xml` lleva su texto **inline**, en sus propios `<castellano>`/`<valenciano>`, con los campos del periodo que ese modo necesita interpolados directamente — el mismo idioma que ya usaba la casilla `JORNADA_PARCIAL` del as-is (`${self.horaInicio;0.6} horas a ${self.horaFin;0.6} horas`), sin pasar por Java. Como el nombre del campo PDF es la propia expresión y xml2pdf agrupa los widgets por nombre (`documentos.md` §2.3), una misma expresión en dos casillas sería **un solo campo** pintado en las dos. Por eso cada expresión es **propia de su casilla** y vale `null` fuera de su modo: `${self.tipoJornadaFalta==com.educaflow.subsystem.expedientes.db.TipoJornadaFaltaJustificacionFaltaProfesoradoV1.<MODO> ? self.<campo> : null;n}`. **`JustificacionFaltaProfesoradoV1Util` no tiene ningún método de presentación**: una frase construida en Java es indivisible para el generador i18n (protege los `${...}` de apertium), así que perdería la traducción al valenciano — `k-i18n` regla 5. Las expresiones de las cuatro casillas se evalúan siempre: la de la casilla del modo elegido lleva el valor, y las de las otras tres casillas valen `null` y se estampan vacías (`documentos.md` §2.8). Cada `<check>` solo referencia los campos que su propio modo necesita (la tabla 4.3).

## 6. Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/domains.xml` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/JustificacionFaltaProfesoradoV1Util.java` | Crear | `k-tipo-expediente` | Especificado en `## 9. Especificación de los PhaseEventManagerImpl`, subsección «Clase auxiliar del tipo» |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/views.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/views.xml` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/InitialEventManagerImpl.java` | Modificar | `k-tipo-expediente` | Especificado en `## 8. Especificación del InitialEventManagerImpl` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/PhaseEventManagerImpl.java` | Modificar | `k-tipo-expediente` | Especificado en `## 9. Especificación de los PhaseEventManagerImpl`, fase `RECEPCION` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/StateEventValidatorImpl.kt` | Modificar | `k-tipo-expediente`, `k-secure-coding` | Especificado en `## 10. Especificación de los StateEventValidatorImpl`, fase `RECEPCION` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/recepcion/views.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/fases/recepcion/views.xml` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/tramitacion/views.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/fases/tramitacion/views.xml` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/_template.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/documentospdf/_template.xml`: se queda solo con centro + interesado |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/_documentacionAportada.xml` | Crear | `k-tipo-expediente` | Copia literal de `design/documentospdf/_documentacionAportada.xml` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/solicitud.xml` | Modificar | `k-tipo-expediente`, `k-i18n` | Copia literal de `design/documentospdf/solicitud.xml`: cambian sus `<include>` y se añade inline la sección «Declara que» con las cuatro casillas |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/resolucion.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/documentospdf/resolucion.xml`: cambian sus `<include>` y se añade inline la sección «Declara que» con solo el motivo |

No se toca ningún otro fichero del árbol. En particular **no** cambian `TramiteInstance.xml`, `TipoExpedienteInstance.xml`, `estados.puml`, `tramitacion/PhaseEventManagerImpl.java` ni `tramitacion/StateEventValidatorImpl.kt`. Los `i18n_*.csv` los genera un script y **MUST NOT** escribirse a mano.

## 7. Pasos

### Paso 1 — `domains.xml`

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/domains.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Verificación: el fichero existe; `diff` con el del diseño vacío; el fichero ya no contiene `dias`, `mes`, `anyo` ni `MesJustificacionFaltaProfesoradoV1`; ningún campo lleva `required="true"`.

### Paso 2 — `JustificacionFaltaProfesoradoV1Util.java`

Fichero destino: `…/actual/v1/JustificacionFaltaProfesoradoV1Util.java`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util`.
Especificación: `## 9. Especificación de los PhaseEventManagerImpl`, subsección **«Clase auxiliar del tipo — `JustificacionFaltaProfesoradoV1Util`»**.
Clase `final`, sin supertipo, con constructor privado y solo métodos `static`.

Verificación: compila; existe un método por cada fila de la tabla de esa subsección, con su firma exacta.

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

### Paso 4 — `InitialEventManagerImpl.java`

Fichero destino: `…/actual/v1/InitialEventManagerImpl.java`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.InitialEventManagerImpl`.
Especificación: `## 8. Especificación del InitialEventManagerImpl`.
Supertipo: `implements InitialEventManager<JustificacionFaltaProfesoradoV1>`.

Verificación: compila; el método `triggerInitialEvent` ya no referencia `setAnyo` ni `LocalDate`; existe exactamente la asignación de la tabla de la sección 8; el fichero ya no importa `java.time.LocalDate` ni `com.educaflow.base.util.Convert`.

### Paso 5 — `recepcion/PhaseEventManagerImpl.java`

Fichero destino: `…/actual/v1/recepcion/PhaseEventManagerImpl.java`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.PhaseEventManagerImpl`.
Especificación: `## 9. Especificación de los PhaseEventManagerImpl`, subsección `### Fase RECEPCION`.
Supertipo: `extends PhaseEventManager<JustificacionFaltaProfesoradoV1> implements TareaFirmaNotifier` (sin cambios).

Verificación: compila; hay un `trigger*` por cada evento de la lista de cobertura de la fase y un `onEnter*` por cada estado, ninguno de más; la limpieza del periodo (`fechaFin`/`horaInicio`/`horaFin` según `JustificacionFaltaProfesoradoV1Util.necesitaFechaFin`/`necesitaHoraInicio`/`necesitaHoraFin`) va **inline** al principio de `triggerGuardarDatos`, **antes** de generar el PDF; el fichero no declara ningún método privado nuevo.

### Paso 6 — `recepcion/StateEventValidatorImpl.kt`

Fichero destino: `…/actual/v1/recepcion/StateEventValidatorImpl.kt`.
FQCN: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.StateEventValidatorImpl`.
Especificación: `## 10. Especificación de los StateEventValidatorImpl`, subsección `### Fase RECEPCION`.
Supertipo: `: StateEventValidator`.

Verificación: compila; existe un método por cada fila de la tabla de cobertura de la fase y ninguno de más; el fichero ya no importa `TipoJornadaFaltaJustificacionFaltaProfesoradoV1`, `MesJustificacionFaltaProfesoradoV1` ni `com.educaflow.base.infrastructure.validation.rules.Pattern`, y no menciona `getDias`, `getMes` ni `getAnyo`.

### Paso 7 — `recepcion/views.xml`

El fichero está materializado en `design/fases/recepcion/views.xml`. **Cópialo literalmente** a `…/actual/v1/recepcion/views.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Resumen estructural de la fase. Respecto al actual cambian tres cosas: la `<action-record>` nueva; el panel `avisoEstadoExpediente`, que faltaba en los dos forms genéricos de solo `EXIT`; y que el form genérico de `ENTRADA_DATOS` pasa de `-datos-falta` a `-datos-falta-view` (`decisiones.md` D4):

| estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|
| `ENTRADA_DATOS` | `CREADOR` | `-datos-profesor`, `-disconformidad-view`, `datos-falta`, `justificante-upload` | `DELETE` / `GUARDAR_DATOS` |
| `ENTRADA_DATOS` | — | `-datos-profesor`, `-disconformidad-view`, `-datos-falta-view`, `-justificante-view` + `avisoEstadoExpediente` | — / `EXIT` |
| `PENDIENTE_PRESENTACION` | `CREADOR` | `-pdfSolicitud` + paneles propios de firma | `BACK` / `PRESENTAR` (×2, excluyentes por `showIf`) |
| `PENDIENTE_PRESENTACION` | — | `-pdfSolicitud` + `avisoEstadoExpediente` | — / `EXIT` |

Verificación: el fichero existe; `diff` con el del diseño vacío; ningún `<button name="">`; existe la `<action-record>` `exp-JustificacionFaltaProfesoradoV1-onChange-tipoJornadaFalta-action`; el form genérico de `ENTRADA_DATOS` incluye `-datos-falta-view` y no `-datos-falta`; los dos forms genéricos de solo `EXIT` llevan su `<panel name="avisoEstadoExpediente">` (`vistas.md` §6.1).

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

### Paso 9 — `documentospdf/_template.xml`

El fichero está materializado en `design/documentospdf/_template.xml`. **Cópialo literalmente** a `…/actual/v1/documentospdf/_template.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo. Se queda solo con las secciones de centro de presentación y datos del interesado.

Verificación: el fichero existe; `diff` con el del diseño vacío; tiene exactamente dos `<seccion>` y ninguna expresión que use `self.dias`, `self.mes`, `self.anyo`, `self.tipoJornadaFalta`, `self.motivoFalta` ni `self.justificante`.

### Paso 10 — `documentospdf/_documentacionAportada.xml`

Fichero **nuevo**. Está materializado en `design/documentospdf/_documentacionAportada.xml`. **Cópialo literalmente** a `…/actual/v1/documentospdf/_documentacionAportada.xml`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

Verificación: el fichero existe; `diff` con el del diseño vacío.

### Paso 11 — `documentospdf/solicitud.xml`

El fichero está materializado en `design/documentospdf/solicitud.xml`. **Cópialo literalmente** a `…/actual/v1/documentospdf/solicitud.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo. Cambian sus `<include>` y se añade inline, entre los datos del interesado y la documentación aportada, la sección «Declara que»: el resto del fichero (Declaración responsable, Protección de datos, Signat) es idéntico al as-is.

Verificación: el fichero existe; `diff` con el del diseño vacío; tiene **exactamente dos** `<include>`, en este orden: `_template.xml`, `_documentacionAportada.xml`; entre los dos hay una `<seccion>` «Declara que» inline; no queda ninguna expresión que use `self.dias`, `self.mes` ni `self.anyo`; los `colspan` de cada `<fila>` de esa sección suman 12 o un múltiplo de 12; cada uno de los cuatro `<check>` lleva su texto **inline**, en su propio `<castellano>`, con los campos del periodo interpolados directamente, cada uno con una expresión propia de su casilla (`${self.tipoJornadaFalta==…<MODO> ? self.<campo> : null;n}`), así que no hay dos `${...}` con la misma expresión en la sección; el fichero no referencia `JustificacionFaltaProfesoradoV1Util`.

### Paso 12 — `documentospdf/resolucion.xml`

El fichero está materializado en `design/documentospdf/resolucion.xml`. **Cópialo literalmente** a `…/actual/v1/documentospdf/resolucion.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo. Cambian sus `<include>` y se añade inline, entre ellos, la sección «Declara que» con solo la fila de motivo: el resto del fichero (Resolución, Signat) es idéntico al as-is.

Verificación: el fichero existe; `diff` con el del diseño vacío; tiene **exactamente dos** `<include>`, en este orden: `_template.xml`, `_documentacionAportada.xml`; entre los dos hay una `<seccion>` «Declara que» con **solo** `self.motivoFalta` y `self.otroMotivo` (ninguna expresión del periodo ni de `tipoJornadaFalta`).

### Paso 13 — Verificación final

```
./run.sh
```

Se comprueba:

- `BUILD SUCCESSFUL`, con los tests de `com/educaflow/tiposexpedientes` y `com/educaflow/views` en verde. En particular el test **P1** (`ExpresionesDeDocumentoTest`) es el que caza que las expresiones nuevas del documento (las expresiones `${self.tipoJornadaFalta==…<MODO> ? self.<campo> : null;n}` de las cuatro casillas de la sección «Declara que» de `solicitud.xml`, el FQCN del enum con sus ítems nuevos, y los `<include>` nuevos de `solicitud.xml`/`resolucion.xml`) compilen y resuelvan contra la entidad: si `dias`, `mes` o `anyo` quedaran en algún documento o fragmento, saldría aquí.
- Que se regeneró `estados.png` (`GenerateDocs` va enganchada a `build` con `finalizedBy`). El `.puml` no cambia, así que el PNG debe quedar igual.
- **REQUIRED — comprobación en runtime**, porque el build solo ve la forma:
  - Recorrer los **cuatro** tipos de jornada faltada en `RECEPCION` / `ENTRADA_DATOS` con el perfil `CREADOR` y comprobar, uno a uno, qué campos se ven, con qué título sale la fecha de inicio y que al cambiar de tipo se vacían los valores que el nuevo tipo ya no necesita.
  - Recorrer las transiciones que el delta atraviesa —arranque → `ENTRADA_DATOS` → (`GUARDAR_DATOS`) `PENDIENTE_PRESENTACION` → `BACK` a `ENTRADA_DATOS` / `PRESENTAR` a `PENDIENTE_RESOLUCION` → (`RESOLVER`) `ACEPTADO` / `RECHAZADO`— y comprobar que cada estado alcanzado coincide con las transiciones del `estados.puml` del as-is (C-K12), que este delta no modifica.
  - Abrir el PDF de la solicitud generado en `GUARDAR_DATOS` y comprobar **con los ojos** que la casilla marcada es la del tipo elegido y que las fechas y horas salen con su formato: un fallo de evaluación de una expresión Groovy no rompe el evento, se escribe en el log y **el campo sale vacío**. Abrir también el PDF de la resolución y comprobar que muestra el motivo de la falta y ya no el periodo.
  - Abrir el expediente con la **vista genérica** de cada estado (entrando por una bandeja cuyo perfil no es el del estado) y comprobar que el bloque de la falta se ve entero y en solo lectura.
  - Como siempre, que `personaSolicitante` y `personaInteresada` siguen no nulos al llegar a `PRESENTAR` (el `createRegistroEntrada` lanza NPE si son `null` y **nada lo comprueba en build**). El delta no los toca, pero sí toca el `triggerInitialEvent`.

## 8. Especificación del InitialEventManagerImpl

Fichero: `actual/v1/InitialEventManagerImpl.java`, en la **raíz** de la carpeta de versión.

```java
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

public class InitialEventManagerImpl implements InitialEventManager<JustificacionFaltaProfesoradoV1> {
    @Override
    public void triggerInitialEvent(InitialEventContext<JustificacionFaltaProfesoradoV1> initialEventContext) throws BusinessException { … }
}
```

(Esta es la firma **real** de la interfaz `InitialEventManager` del árbol: un único parámetro `InitialEventContext<T>`, del que se obtiene el expediente con `getExpediente()`. El delta no la cambia.)

Asignaciones, **en orden**:

| # | Setter | Fuente del valor | Por qué |
|---|---|---|---|
| 1 | `initialEventContext.updateState(States.Recepcion.ENTRADA_DATOS)` | constante de la clase `States` de **esta** versión | El XML maestro no declara estado inicial: sin esta llamada el `Tramitador` aborta el alta |

**Delta:** desaparece la primera asignación que había hoy, `setAnyo(LocalDate.now(Convert.defaultZoneId).getYear())`, y con ella los `import` de `java.time.LocalDate` y `com.educaflow.base.util.Convert`. Ya no se precarga ningún dato sobre cuándo se produjo la falta: `fechaInicio` nace vacía y la rellena el profesor.

La sección **MUST** dejar explícito además:

- Lo que el `Tramitador` ya rellena **antes** de llamar a este método y **MUST NOT** reasignarse: `tipoExpediente`, `centro`, `usuarioRegistrador`, `name`, `numeroExpediente`.
- Lo que hace **después** y tampoco es cosa de este método: comprobar el perfil del estado inicial, crear el `HistorialEstado` y llamar al `onEnterState`.
- `triggerPresentar` **sí** llama a `createRegistroEntrada`, así que `personaSolicitante` y `personaInteresada` **MUST** llegar no nulos a ese evento. Hoy no los rellena este método —los deja el `Tramitador` a partir del contexto de tramitación— y el delta **no cambia eso**: quitar el `setAnyo` no puede dejarlos nulos.

Dependencias a inyectar: **ninguna**. La clase no tiene constructor propio ni `@Inject`.

## 9. Especificación de los PhaseEventManagerImpl

### Clase auxiliar del tipo — `JustificacionFaltaProfesoradoV1Util`

Fichero **nuevo**: `actual/v1/JustificacionFaltaProfesoradoV1Util.java` (raíz de la versión, junto al `TipoExpedienteInstance.xml`).

```java
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1;

public final class JustificacionFaltaProfesoradoV1Util {
    private JustificacionFaltaProfesoradoV1Util() {
    }
}
```

Es la **única** clase auxiliar del tipo (`k-tipo-expediente` §1.8: una sola `<Code>Util`, `final`, con constructor privado y solo métodos estáticos). Aquí vive el **dueño único en servidor** de la clasificación de la sección 4.3, y de él la leen el `trigger*` (RN-001) y el validador (VAL-005..010).

| # | Método | Devuelve | Quién lo usa | Qué hace |
|---|---|---|---|---|
| 1 | `necesitaFechaFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | `triggerGuardarDatos` de `recepcion/PhaseEventManagerImpl` (acción 1, inline), validador (`ifLambda`) | `switch` expression exhaustivo sobre `tipoJornadaFalta`, **sin `default`**: `case null -> false`; `case VARIOS_DIAS_COMPLETOS, VARIOS_DIAS_PRIMERO_PARCIAL -> true`; `case UN_DIA_COMPLETO, UNAS_HORAS_UN_DIA -> false` |
| 2 | `necesitaHoraInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | `triggerGuardarDatos` de `recepcion/PhaseEventManagerImpl` (acción 1, inline), validador (`ifLambda`) | `switch` expression exhaustivo sobre `tipoJornadaFalta`, **sin `default`**: `case null -> false`; `case UNAS_HORAS_UN_DIA, VARIOS_DIAS_PRIMERO_PARCIAL -> true`; `case UN_DIA_COMPLETO, VARIOS_DIAS_COMPLETOS -> false` |
| 3 | `necesitaHoraFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | `triggerGuardarDatos` de `recepcion/PhaseEventManagerImpl` (acción 1, inline), validador (`ifLambda`) | `switch` expression exhaustivo sobre `tipoJornadaFalta`, **sin `default`**: `case null -> false`; `case UNAS_HORAS_UN_DIA -> true`; `case UN_DIA_COMPLETO, VARIOS_DIAS_COMPLETOS, VARIOS_DIAS_PRIMERO_PARCIAL -> false` |
| 4 | `tieneTipoJornadaFalta(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getTipoJornadaFalta() != null` |
| 5 | `tieneFechaInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getFechaInicio() != null` |
| 6 | `fechaInicioNoAnteriorAHaceUnAnyo(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | Cierto si `fechaInicio` es `null` o **no** anterior a `LocalDate.now(Convert.defaultZoneId).minusYears(1)` |
| 7 | `fechaInicioNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | Cierto si `fechaInicio` es `null` o **no** posterior a `LocalDate.now(Convert.defaultZoneId)` |
| 8 | `tieneFechaFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getFechaFin() != null` |
| 9 | `fechaFinPosteriorAFechaInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `fechaFin.isAfter(fechaInicio)`. Da por hecho que los dos no son `null`: si alguno lo es, lanza `IllegalStateException` (la rama del validador lo excluye antes) |
| 10 | `fechaFinNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | Cierto si `fechaFin` es `null` o **no** posterior a `LocalDate.now(Convert.defaultZoneId)` |
| 11 | `tieneHoraInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getHoraInicio() != null` |
| 12 | `tieneHoraFin(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `getHoraFin() != null` |
| 13 | `horaFinPosteriorAHoraInicio(JustificacionFaltaProfesoradoV1 expediente)` | `boolean` | validador | `horaFin.isAfter(horaInicio)`. Da por hecho que las dos no son `null`: si alguna lo es, lanza `IllegalStateException` (la rama del validador lo excluye antes) |

Reglas de la clase:

- Los métodos 1-3 son la **única** definición en servidor de la tabla 4.3. **MUST NOT** repetirse esa clasificación en ningún `trigger*` ni en ningún `ifValueIn` del validador. Su `switch` **MUST NOT** llevar `default`: así un ítem nuevo del enum que no se clasifique no compila, en vez de devolver `false` en silencio.
- Los métodos 9 y 13 son **comparaciones tontas**: dan por hecho que los dos operandos no son `null` y, si alguno lo es, lanzan `IllegalStateException`. Cuándo se pueden evaluar lo decide **la rama del validador**, no ellos: van dentro de `ifLambda(util::tieneFechaFin) { ifLambda(util::tieneFechaInicio) { … } }` / `ifLambda(util::tieneHoraFin) { ifLambda(util::tieneHoraInicio) { … } }` (§10). Las dos ramas hacen falta porque el DSL evalúa **todas** las reglas de un `field(...)` sin cortar en el primer fallo: el `+Lambda(util::tieneFechaFin, …)` que va delante da el mensaje, pero no impide que se evalúe la regla siguiente. Los métodos 6, 7 y 10 comparan un solo campo con «hoy» y devuelven `true` si ese campo es `null`, igual que `PastOrToday` del catálogo base: la ausencia de ese mismo campo la señala el `tiene…` que va delante en su mismo `field(...)`.
- «Hoy» se obtiene siempre con `LocalDate.now(Convert.defaultZoneId)` (`com.educaflow.base.util.Convert`), que es la zona horaria que ya usaba este mismo tipo.
- **`JustificacionFaltaProfesoradoV1Util` no tiene ningún método de presentación** (ni `fecha`/`hora` de formato, ni un texto por modo): el texto de cada casilla del PDF vive **en la propia sección «Declara que» de `documentospdf/solicitud.xml`**, con los campos del periodo interpolados inline (§5). Una frase de presentación construida en Java perdería la traducción al valenciano (el generador i18n protege los `${...}` de apertium): `k-i18n` regla 5.
- **MUST NOT** crearse una `ValidationRule` propia del tipo: las comprobaciones del tipo son funciones `boolean` de esta clase, declaradas en el validador con `Lambda`/`ifLambda`.

### Fase RECEPCION

FQCN: `…actual.v1.recepcion.PhaseEventManagerImpl`.
Supertipo: `extends PhaseEventManager<JustificacionFaltaProfesoradoV1> implements TareaFirmaNotifier` — *(sin cambios)*.
Constructor `@Inject` con `JustificacionFaltaProfesoradoV1Repository` y `super(JustificacionFaltaProfesoradoV1.class)` — *(sin cambios)*.
Dependencias inyectadas (`RegistroEntradaRepository`, `ModelServiceFactory`, `FirmaServidorHelper`), constantes (`POSICION_FIRMA_SOLICITUD`, `PAGINA_FIRMA_SOLICITUD`) e `import` de `States` de la propia versión — *(sin cambios)*.

#### `triggerGuardarDatos`

Lista **ordenada** de acciones (la numeración es normativa):

1. `LIMPIAR(fechaFin)` si no `necesitaFechaFin`; `LIMPIAR(horaInicio)` si no `necesitaHoraInicio`; `LIMPIAR(horaFin)` si no `necesitaHoraFin` (predicados de `JustificacionFaltaProfesoradoV1Util`) — **inline, al principio de `triggerGuardarDatos`**: solo se limpian los que el `tipoJornadaFalta` vigente no necesita. Es **RN-001**, y va **la primera**: el PDF de la solicitud se genera en el paso siguiente y no puede llevar un valor de una elección de tipo anterior. No va a `Util` porque su único llamante es este `trigger*` (`k-tipo-expediente` §1.8 — una utilidad de un solo llamante no se saca a `<Code>Util`); los tres `necesita*`, que sí tienen un segundo llamante (el validador), se quedan en `Util`. **MUST NOT** extraerse a un método privado: lo que se hace en un solo evento se queda inline (`k-tipo-expediente/phaseeventmanager.md` §2.2).
2. `GENERAR_PDF(SOLICITUD)`
3. `CREAR_METAFILE → pdfSolicitud`
4. *(sin cambios)* — la creación de la `TareaFirma` de prueba a través del `ModelServiceFactory`, con su comentario de que es temporal.
5. `UPDATE_STATE(States.Recepcion.PENDIENTE_PRESENTACION)`

**Delta:** solo se añade la acción 1. Las 2-5 son exactamente las que ya hay.

#### Resto de la fase

- `triggerDelete` — *(sin cambios)*: cuerpo vacío, **MUST NOT** llamar a `UPDATE_STATE`.
- `triggerBack` — *(sin cambios)*: pone `claveCertificado` a `null` y ramifica sobre el estado actual con `States.INSTANCE.getState(codePhase, codeState)`, con `default → error`. **MUST NOT** limpiar los campos del periodo: ESC-014 exige que al volver atrás el profesor encuentre lo que había escrito.
- `triggerPresentar` — *(sin cambios)*.
- `onEnterEntradaDatos`, `onEnterPendientePresentacion` — *(sin cambios)*: vacíos.
- `notify(TareaFirma, Object)` de `TareaFirmaNotifier` — *(sin cambios)*.

#### Lista de cobertura de la fase

- Eventos de la fase (unión de los `events` de `ENTRADA_DATOS` y `PENDIENTE_PRESENTACION`): `DELETE` → `triggerDelete`; `GUARDAR_DATOS` → `triggerGuardarDatos`; `BACK` → `triggerBack`; `PRESENTAR` → `triggerPresentar`.
- Estados de la fase: `ENTRADA_DATOS` → `onEnterEntradaDatos`; `PENDIENTE_PRESENTACION` → `onEnterPendientePresentacion`.

### Fase TRAMITACION

*(sin cambios)* — no se toca `tramitacion/PhaseEventManagerImpl.java`. Su cobertura sigue siendo `RESOLVER` → `triggerResolver` (con su ramificación por `tipoResolucion` y su `default → error`), y `onEnterPendienteResolucion`, `onEnterAceptado`, `onEnterRechazado`.

## 10. Especificación de los StateEventValidatorImpl

### Fase RECEPCION

```kotlin
package com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion

import com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util as util
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1 as model
```

`import` que **se añaden**: `com.educaflow.base.infrastructure.validation.dsl.ifLambda`, `com.educaflow.base.infrastructure.validation.rules.Lambda` y el alias `util` de arriba.
`import` que **se quitan** (se quedan sin uso): `TipoJornadaFaltaJustificacionFaltaProfesoradoV1`, `GreaterThan`, `MaxValue`, `MinValue`, `java.time.LocalDate`, `Pattern` (`com.educaflow.base.infrastructure.validation.rules.Pattern`) — su único uso en el as-is era `field(model::getDias) { +Pattern(...) }`, y `dias` desaparece por completo del modelo; ningún `field(...)` de esta sección 10, en ninguno de los dos estados de la fase, vuelve a llamar a `Pattern`.
Los demás (`MotivoFaltaJustificacionFaltaProfesoradoV1`, `ifValueIn`, `Required`, `MinLength`, `MaxLength`, `NoAllUpperCase`, `FileType`, `FileMaxSize`, `SizeUnit`, `FirmaPdf`, `ClaveCertificadoValida`, `ifSituacionFirma`) siguen en uso.

#### Tabla de cobertura

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `ENTRADA_DATOS` | `GUARDAR_DATOS` | `getForStateEntradaDatosInEventGuardarDatos` | sí |
| `ENTRADA_DATOS` | `DELETE` | **ninguno** (exento) | — |
| `PENDIENTE_PRESENTACION` | `BACK` | `getForStatePendientePresentacionInEventBack` | `rules { }` vacío |
| `PENDIENTE_PRESENTACION` | `PRESENTAR` | `getForStatePendientePresentacionInEventPresentar` | sí |

#### `getForStateEntradaDatosInEventGuardarDatos` — **cambia entero el bloque del periodo**

```kotlin
return rules {
    field(model::getTipoJornadaFalta) {
        +Lambda(util::tieneTipoJornadaFalta, "Debe indicar el tipo de jornada faltada")
    }
    field(model::getFechaInicio) {
        +Lambda(util::tieneFechaInicio, "Debe indicar la fecha")
        +Lambda(util::fechaInicioNoAnteriorAHaceUnAnyo, "La fecha debe ser de los últimos 12 meses")
        +Lambda(util::fechaInicioNoPosteriorAHoy, "La fecha no puede ser posterior a hoy")
    }
    field(model::getFechaFin) {
        +ifLambda(util::necesitaFechaFin) {
            +Lambda(util::tieneFechaFin, "Debe indicar la fecha de fin")
            +ifLambda(util::tieneFechaFin) {
                +ifLambda(util::tieneFechaInicio) {
                    +Lambda(util::fechaFinPosteriorAFechaInicio, "La fecha de fin debe ser posterior a la fecha de inicio")
                }
            }
            +Lambda(util::fechaFinNoPosteriorAHoy, "La fecha de fin no puede ser posterior a hoy")
        }
    }
    field(model::getHoraInicio) {
        +ifLambda(util::necesitaHoraInicio) {
            +Lambda(util::tieneHoraInicio, "Debe indicar la hora de inicio")
        }
    }
    field(model::getHoraFin) {
        +ifLambda(util::necesitaHoraFin) {
            +Lambda(util::tieneHoraFin, "Debe indicar la hora de fin")
            +ifLambda(util::tieneHoraFin) {
                +ifLambda(util::tieneHoraInicio) {
                    +Lambda(util::horaFinPosteriorAHoraInicio, "La hora de fin debe ser posterior a la hora de inicio")
                }
            }
        }
    }
    field(model::getMotivoFalta) {
        +Required()
    }
    field(model::getOtroMotivo) {
        +ifValueIn(model::getMotivoFalta, listOf(MotivoFaltaJustificacionFaltaProfesoradoV1.OTROS)) {
            +Required()
            +NoAllUpperCase()
            +MinLength(5)
            +MaxLength(100)
        }
    }
    field(model::getJustificante) {
        +Required()
        +FileType(listOf("image/png","image/jpeg","image/gif","application/pdf"))
        +FileMaxSize(5, SizeUnit.MB)
    }
}
```

**Delta:** desaparecen los `field(model::getDias)`, `field(model::getMes)` y `field(model::getAnyo)` enteros; el bloque de `tipoJornadaFalta`, `horaInicio` y `horaFin` se reescribe; se añaden `fechaInicio` y `fechaFin`. Los tres últimos `field(...)` (`motivoFalta`, `otroMotivo`, `justificante`) se conservan **literalmente** como están hoy.

Por qué así y no de otra forma:

- Los mensajes son los que fija la especificación, y las reglas del catálogo base llevan el suyo dentro (`"Es requerido"`, `"El valor debe ser mayor que …"`). `Lambda(util::…, "<mensaje>")` es el mecanismo documentado para una comprobación propia de un tipo con su texto (ver `decisiones.md` D1).
- La **condición** de cuándo aplica cada grupo se lee **en la rama** (`ifLambda(util::necesitaFechaFin)`), no dentro de las reglas: las reglas de dentro dan por hecho que están en su caso. Por eso las comparaciones `fechaFinPosteriorAFechaInicio`/`horaFinPosteriorAHoraInicio` van envueltas en `ifLambda(util::tieneFechaFin) { ifLambda(util::tieneFechaInicio) { … } }` y su equivalente de horas: la ausencia de cualquiera de los dos operandos la excluye la rama, no el predicado (el DSL no corta en el primer fallo de un `field(...)`), y no depende de que `necesitaHoraFin` implique `necesitaHoraInicio`.
- Las ramas son complementarias por construcción: `necesitaX` / `!necesitaX`.

#### Los otros dos métodos

`getForStatePendientePresentacionInEventBack` (`rules { }` vacío) y `getForStatePendientePresentacionInEventPresentar` — *(sin cambios)*.

#### Frontera de confianza (CRITICAL)

- Los cinco campos del periodo (`tipoJornadaFalta`, `fechaInicio`, `fechaFin`, `horaInicio`, `horaFin`) son `usuario` y **MUST** estar los cinco en un `field(...)` de `GUARDAR_DATOS`: el conjunto de `field(...)` **es** la lista de campos que el cliente puede dictar en ese evento, y un campo que no esté ahí no se copia desde la petición (el profesor escribiría y el valor se perdería en silencio). `fechaFin`, `horaInicio` y `horaFin` están aunque en algunos tipos no se pidan: quien decide si el valor sobrevive es RN-001 en el servidor, no la ausencia del `field(...)`.
- **MUST NOT** aparecer en ningún `field(...)` de este evento ningún campo `servidor` (`pdfSolicitud`, `pdfSolicitudFirmado`, `pdfJustificanteRegistroEntrada`, `pdfResolucion`), ni `codePhase`, `codeState`, `abierto`, `centro` ni `usuarioRegistrador`.
- `required="true"`, `showIf` y el prefijo `-` del `views.xml` son **UX, nunca defensa**. Que un panel esté oculto no impide enviar su campo por la petición: lo que lo impide es no estar en el `field(...)`, y lo que lo corrige es RN-001.
- **CRITICAL** — nada de esto protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1`, que **no** pasa por el `Tramitador`. Es el agujero conocido documentado en `CLAUDE.md`; este diseño **MUST NOT** intentar taparlo por su cuenta ni reintroducir un `ModelService` deny-all de expedientes.

### Fase TRAMITACION

*(sin cambios)* — no se toca `tramitacion/StateEventValidatorImpl.kt`. Su única pareja sigue siendo `PENDIENTE_RESOLUCION` + `RESOLVER`.

## 11. Reparto de reglas

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<date>`/`<time>`/`<enum>`/`<string>`/`<many-to-one>` |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required()`, o `+Lambda(util::tiene…, "<mensaje>")` cuando la spec fija el texto |
| Formato, rango, longitud, comparación entre campos, tipo/tamaño de fichero, firma | **DSL del validador** | `Pattern`, `MinLength`/`MaxLength`, `FileType`/`FileMaxSize`, `FirmaPdf`, `Lambda` |
| Obligatoriedad **condicional** (depende del valor de otro campo) | **DSL del validador** | `ifValueIn(...) { … }` / `ifLambda(util::necesita…) { … }` |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar, calcular | **`trigger*`** del `PhaseEventManagerImpl` | lista de acciones de la sección 9 |
| Inicialización del expediente | **`triggerInitialEvent`** | sección 8 |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf` en el panel del modo, `required`, prefijo `-`, `<action-record>` del `onChange` — **solo UX, NUNCA defensa** |

### Dónde va cada regla de la especificación

| Regla | Capa | Cómo |
|---|---|---|
| VAL-…-001 tipo de jornada obligatorio | validador | `field(getTipoJornadaFalta) { +Lambda(util::tieneTipoJornadaFalta, "Debe indicar el tipo de jornada faltada") }` |
| VAL-…-002 fecha de inicio obligatoria | validador | `+Lambda(util::tieneFechaInicio, "Debe indicar la fecha")` |
| VAL-…-003 fecha de inicio no anterior a hace un año | validador | `+Lambda(util::fechaInicioNoAnteriorAHaceUnAnyo, "La fecha debe ser de los últimos 12 meses")` |
| VAL-…-004 fecha de inicio no posterior a hoy | validador | `+Lambda(util::fechaInicioNoPosteriorAHoy, "La fecha no puede ser posterior a hoy")` |
| VAL-…-005 fecha de fin obligatoria (condicional) | validador | dentro de `ifLambda(util::necesitaFechaFin)` |
| VAL-…-006 fecha de fin posterior a la de inicio | validador | dentro de `ifLambda(util::necesitaFechaFin)` |
| VAL-…-007 fecha de fin no posterior a hoy | validador | dentro de `ifLambda(util::necesitaFechaFin)` |
| VAL-…-008 hora de inicio obligatoria (condicional) | validador | dentro de `ifLambda(util::necesitaHoraInicio)` |
| VAL-…-009 hora de fin obligatoria (condicional) | validador | dentro de `ifLambda(util::necesitaHoraFin)` |
| VAL-…-010 hora de fin posterior a la de inicio | validador | dentro de `ifLambda(util::necesitaHoraFin)` |
| Comprobaciones de motivo, «Otros» y justificante | validador | se conservan **literalmente** como están hoy |
| RN-001 limpiar lo que el tipo no necesita | `trigger*` | la limpieza del periodo va inline al principio de `triggerGuardarDatos` (acción 1), antes de generar el PDF, usando los `necesita*` de `JustificacionFaltaProfesoradoV1Util` |
| RUI-ENTRADA_DATOS-CREADOR-001/002/003 mostrar fecha de fin / hora de inicio / hora de fin | vista | tres `<panel>` anidados con `showIf` dentro de `datos-falta`: `datos-falta-un-dia` (`UN_DIA_COMPLETO` o `UNAS_HORAS_UN_DIA`; `horaInicio`/`horaFin` con `showIf` propio a `UNAS_HORAS_UN_DIA`), `datos-falta-varios-dias-completos` (`VARIOS_DIAS_COMPLETOS` o sin tipo elegido; `fechaFin` con `showIf` propio a `VARIOS_DIAS_COMPLETOS`) y `datos-falta-varios-dias-primero-parcial` |
| RUI-ENTRADA_DATOS-CREADOR-004 título dinámico de la fecha de inicio | vista | `title="Fecha"` en los paneles de `UN_DIA_COMPLETO` y `UNAS_HORAS_UN_DIA`; en los otros dos (y sin tipo elegido, que lo cubre `datos-falta-varios-dias-completos`) hereda «Fecha de Inicio» del `domains.xml` |
| RUI-ENTRADA_DATOS-CREADOR-005..009 marcar como obligatorio | vista | `required="true"` estático en cada campo, dentro del panel de su modo; `fechaInicio` (RUI-006, «siempre») está en los tres paneles y el `showIf` de `datos-falta-varios-dias-completos` incluye `!tipoJornadaFalta`, así que se ve y sale obligatoria desde que se abre la pantalla, también sin tipo elegido |
| RUI-ENTRADA_DATOS-CREADOR-010/011/012 limpiar al cambiar de tipo | vista | `<action-record>` `exp-JustificacionFaltaProfesoradoV1-onChange-tipoJornadaFalta-action`, en el `onChange` del selector. Coherente con la visibilidad: nunca limpia `fechaInicio`, que se ve en todos los estados (también sin tipo), y cada `if` vacía un campo exactamente cuando queda oculto, incluido el caso sin tipo (`fechaFin`, `horaInicio` y `horaFin` están ocultos y sus tres condiciones son ciertas) |
| RUI-ENTRADA_DATOS-GENERICA-001..004 | vista | los paneles anidados del gemelo de solo lectura `datos-falta-view`, con los mismos `showIf` y el mismo `title="Fecha"`; el form genérico de `ENTRADA_DATOS` incluye `-datos-falta-view` |
| RUI-PENDIENTE_RESOLUCION-TRAMITADOR/GENERICA-001..004, RUI-ACEPTADO-GENERICA-001..004, RUI-RECHAZADO-GENERICA-001..004 | vista | los paneles anidados del gemelo de solo lectura `datos-falta-view` (`datos-falta-view-un-dia`, `datos-falta-view-varios-dias-completos`, `datos-falta-view-varios-dias-primero-parcial`), con los mismos `showIf` y el mismo `title="Fecha"`; los cuatro forms de `TRAMITACION` siguen incluyendo `-datos-falta-view` |

No se descarta ninguna regla de la especificación.

Reglas duras que se respetan: ningún `required="true"` en el `domains.xml`; ningún dato de usuario validado en un `trigger*` (RN-001 no valida, **normaliza**); ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.

## 12. Asignación de perfiles

*(sin cambios)*

Esta iniciativa no añade, quita ni cambia ningún perfil, ni toca los `<aces>` del `TramiteInstance.xml` ni los del `TipoExpedienteInstance.xml`, así que ninguno de los dos ficheros aparece en la tabla de la sección 6. Los perfiles que usan los estados del tipo (`CREADOR` en `RECEPCION`, `TRAMITADOR` en `TRAMITACION`) los sigue dando el data-init de security (`AceProfileTipoTramite.xml`: `CREADOR`/`PROFESOR` y `TRAMITADOR`/`JEFE_ESTUDIOS` para el `tipoTramite` `PROFESOR`), y el `AUDITOR` de ejemplo lo sigue dando el `<ace perfil="AUDITOR" cargo="DIRECTOR"/>` que ya hay en los dos ficheros maestros. `AceProfileGlobal.xml` y `AceProfileTipoTramite.xml` **MUST NOT** modificarse.

## 13. Tests

- **E2E:** `design/test-e2e-desc.md`, escrito según el contrato de `tests-e2e.md`. La cobertura se mide **solo sobre el delta** (los escenarios ESC-001..014, las pantallas modificadas y los caminos existentes que el delta atraviesa), tomando como referencia la tabla de transiciones del as-is (`actual/v1/TipoExpedienteInstance.xml`).
- **Unitarios:** `design/test-unit-desc.md`, que escribe el rol `test-unitarios` del motor.
- La numeración empieza en **`T-001`**: la carpeta espejo `src/test/e2e/tramites/profesores/justificacion_falta_profesorado/actual/v1/` **no existe**, así que esta versión no tiene ningún test E2E persistido.
- Por lo mismo, **no** hay subsección «Tests E2E supersedidos»: el delta no puede invalidar ningún test que no existe.

## 14. Notas y supuestos

- **La Resolución deja de mostrar el periodo; el resto no cambia.** La especificación dice que la Resolución «no muestra ese dato y no cambia en nada», pero el as-is sí lo mostraba: `resolucion.xml` incluía el fragmento cajón de sastre `_template.xml`, cuya sección «Declara que» llevaba el periodo (días/mes/año y las casillas de jornada) y el motivo. Se resuelve la contradicción así (`decisiones.md` D5):
  - La Resolución **deja de mostrar el periodo**: es la lectura literal de «no muestra ese dato», y además sus campos (`dias`/`mes`/`anyo`, `TODA_LA_JORNADA`/`JORNADA_PARCIAL`) desaparecen del modelo.
  - **Conserva el motivo de la falta** («Por el motivo siguiente» + otro motivo) en su sección «Declara que», inline, y todas las demás secciones, en el mismo orden: es la lectura de «no cambia en nada» para todo lo que no es el periodo.
  - Por tanto su contenido visible **sí cambia** respecto al as-is, pero solo en la parte del periodo.
- **`_template.xml` conserva su nombre aunque ya solo contiene centro + interesado.** `documentos.md` §2.5 pide que un fragmento se llame por lo que contiene, pero renombrarlo exigiría borrar el fichero del as-is, y la columna `Acción` de §6 solo admite `Crear`/`Modificar`. Renombrarlo queda como deuda para cuando el contrato admita borrar ficheros.
- **Se añade el panel `avisoEstadoExpediente` a los cinco forms genéricos de solo `EXIT`** (`ENTRADA_DATOS` y `PENDIENTE_PRESENTACION` en `recepcion/views.xml`; `PENDIENTE_RESOLUCION`, `ACEPTADO` y `RECHAZADO` en `tramitacion/views.xml`). Va **más allá del delta de la spec**, que declara esas pantallas sin cambios (y la de `PENDIENTE_PRESENTACION` ni siquiera la toca el delta). Motivo: es una regla MUST de `k-tipo-expediente/vistas.md` §6.1 que el as-is incumplía, y como los dos `views.xml` de fase se sobrescriben enteros con la copia del diseño, dejarlos sin ese panel sería materializar a sabiendas un incumplimiento. El efecto visible es solo el texto de aviso del estado del expediente; ningún dato ni botón cambia.
- **El `title` de los mensajes de error de la fecha de inicio es siempre «Fecha de Inicio».** El motor de validación compone el mensaje con el `@Widget(title=…)` del `domains.xml`, no con el `title` de la vista, así que en el modo «Un día completo» el usuario lee «Fecha de Inicio: Debe indicar la fecha» aunque el campo se titule «Fecha» en pantalla. El texto que la especificación exige es el del mensaje, y ese sí es literal.
- **Se mantiene el gemelo `datos-falta-view`** y se rehace en paralelo a `datos-falta`, así que los tres paneles anidados del periodo están escritos dos veces. No se puede reutilizar `-datos-falta` en las pantallas de solo lectura (las cuatro de TRAMITACION y la genérica de `ENTRADA_DATOS`). En solo lectura un `SwitchSelect` vertical sigue pintando todas sus opciones, y esas pantallas apilarían 13 (4 de `tipoJornadaFalta` + 9 de `motivoFalta`) para enseñar dos valores (`decisiones.md` D4). Añadir un modo exige tocar los dos paneles (sección 4.3, punto 3).
- **La firma `triggerInitialEvent(InitialEventContext<T>)` del árbol real tiene un solo parámetro**, mientras que la plantilla de `design-contract.md` §10 muestra dos (`(<Entidad>, InitialEventContext)`). Manda el árbol: la interfaz `InitialEventManager` del `subsystem/tramitador` declara un único parámetro. El delta no la cambia.
- **PRECONDICIÓN de despliegue: la base de datos se vacía.** El delta no es compatible con los expedientes v1 ya guardados: borra las columnas `dias`, `mes` y `anyo` y el enum `MesJustificacionFaltaProfesoradoV1`, y quita de `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` los ítems `TODA_LA_JORNADA`/`JORNADA_PARCIAL`. Por `k-tipo-expediente/recetas/versionado.md` §4, un cambio así **MUST** ir en una v2; hacerlo sobre la propia v1 solo es legítimo porque la especificación da por hecho que la BD se vacía antes de desplegar (y declara fuera de alcance migrar los expedientes existentes). Si se desplegara con expedientes v1 vivos, los que tengan `TODA_LA_JORNADA`/`JORNADA_PARCIAL` **no se pueden cargar** (el valor ya no corresponde a ningún ítem del enum), y habría que crear una v2 con la receta de versionado en vez de modificar la v1.
- **La copia en cliente de la clasificación de la sección 4.3 es de cortesía.** Vive en los `showIf` de los tres paneles anidados de `datos-falta` y de los tres de su gemelo `datos-falta-view` (y de `horaInicio`/`horaFin` dentro de `datos-falta-un-dia`/`datos-falta-view-un-dia` y de `fechaFin` dentro de `datos-falta-varios-dias-completos`) y en las condiciones `if` del `<action-record>`; la que cuenta es el enum `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` más los tres `necesita*` de `JustificacionFaltaProfesoradoV1Util` (`decisiones.md` D3). La tabla 4.3 es solo una proyección de ese par para leer el diseño; su lista numerada enumera **exactamente** los sitios del árbol que toca añadir un tipo de jornada nuevo, y el `switch` exhaustivo de los `necesita*` hace que el compilador obligue a clasificarlo.
- **La demo no trae certificados digitales** (`src/main/resources/data-demo/input/` solo tiene usuarios y centros), así que en `PENDIENTE_PRESENTACION` la situación de firma es `SIN_CERTIFICADO` y presentar exige AutoFirma__!! en el equipo del profesor, y resolver exige el certificado del director del centro en el servidor. Los tests E2E que atraviesan esos dos pasos van marcados `Manual: sí`. Esto **no** es del delta: es el estado del entorno de demo.
- **No se añade ningún usuario de demo:** `director@mislata.es` (profesor con cargo de director, perfil `CREADOR`) y `jefeestudios1@mislata.es` (perfil `TRAMITADOR`) ya existen en `usuarios-demo.xml` con la contraseña `demo1234`.

## 15. Checklist del diseñador

**Estructura y materialización**

- [x] ¿Existen todos los ficheros de §1 y **ninguno más**? ¿Ni un `.java`, ni un `.kt`, ni un `i18n_*.csv`, ni un `estados.png`, ni un `.pdf` dentro de `design/`? — en modo modificación existen **solo** los del delta: `design.md`, `domains.xml`, `views.xml`, `fases/recepcion/views.xml`, `fases/tramitacion/views.xml`, `documentospdf/_template.xml`, `documentospdf/_documentacionAportada.xml`, `documentospdf/solicitud.xml`, `documentospdf/resolucion.xml`, `test-e2e-desc.md` y `decisiones.md`.
- [x] ¿Hay un `design/fases/<fase>/views.xml` por **cada** fase declarada, con la fase en minúsculas? — las dos fases tocan su `views.xml`, así que están las dos.
- [x] ¿Hay un `design/documentospdf/<doc>.xml` por cada documento y un `_<fragmento>.xml` por cada fragmento **compartido**, y ninguno de más? — dos documentos (`solicitud.xml`, `resolucion.xml`) y dos fragmentos, uno por bloque común a ambos (`_template.xml` con centro + interesado, `_documentacionAportada.xml`); ningún cajón de sastre; la sección «Declara que», que ya no es idéntica en los dos documentos, va inline en cada uno y no es un fragmento.
- [x] ¿Todos los XML materializados están **completos**, sin `TODO`, sin `...`, sin `<button name="">`?
- [x] ¿El `design.md` tiene el frontmatter `type: design` con la clave `template:` copiada de la spec, y las 15 secciones de §2, con esos títulos y en ese orden?
- [x] **Iniciativa de MODIFICACIÓN:** ¿«Identidad del trámite y del tipo», «Ficheros a crear o modificar» y «Pasos» van **completas**, sin `*(sin cambios)*`?
- [x] ¿La fila `| Modificación de | … |` está y nombra una carpeta de versión que **existe** en el árbol?

**Máquina de estados**

- [x] ¿Ningún `<state>` lleva `initial`, y el `triggerInitialEvent` fija el estado inicial con `updateState` en todas sus ramas? — sin cambios; el `triggerInitialEvent` tiene una sola rama y la conserva.
- [x] ¿Todo `<state>` lleva escrito su `events`, aunque sea `events=""`? — sin cambios.
- [x] ¿`EXIT` **no** aparece en ningún `events`? — sin cambios.
- [x] ¿Todo `profile` de un estado es un valor del enum `Profile`? — sin cambios.
- [x] ¿Ningún nombre de estado ni de evento produce un método que pise la API base? — sin cambios.
- [x] ¿La tabla de transiciones tiene una fila por cada pareja (estado, evento) declarada, más el arranque, más las de `DELETE` hacia `[*]`? — sin cambios; la referencia es el as-is.
- [x] ¿Todo evento ramificado declara sus guardas y su `default`? — sin cambios (`RESOLVER` y `BACK`).

**Diagrama**

- [x] ¿El `estados.puml` dibuja **todos** los estados…? — el `.puml` no entra en el delta y no se regenera.
- [x] ¿No hay ningún alias que no corresponda a un estado declarado? — sin cambios.
- [x] ¿Cada transición del `.puml` coincide con una fila de la tabla de transiciones? — sin cambios.

**Modelo**

- [x] ¿La entidad es la **primera** `<entity>` del `domains.xml`, con `extends="Expediente"` y `name="JustificacionFaltaProfesoradoV1"`?
- [x] ¿El `<module>` es `expedientes` / `com.educaflow.subsystem.expedientes.db`?
- [x] ¿Ningún campo heredado de `Expediente` está redeclarado?
- [x] ¿Todo enum propio lleva `<Entidad>` como sufijo?
- [x] ¿Ningún campo lleva `required="true"`?
- [x] ¿Todo campo de la tabla de §6.1 tiene su columna «quién lo rellena» resuelta?
- [x] ¿Todo campo `servidor` está asignado por alguna acción de las secciones 8 o 9? — los cuatro `MetaFile` los asignan `triggerGuardarDatos`, `triggerPresentar` y `triggerResolver`, sin cambios.
- [x] ¿El `<extra-code-model>` existe si y solo si hay documentos PDF, con una constante por documento y su ruta correcta? — dos constantes, sin cambios.

**Clases**

- [x] ¿El `InitialEventManagerImpl` está en la **raíz** de la versión, parametrizado con la entidad, con exactamente un `triggerInitialEvent`, y sin reasignar lo que ya rellena el `Tramitador`?
- [x] Si hay `createRegistroEntrada`, ¿deja `personaSolicitante` y `personaInteresada` no nulos? — los deja el `Tramitador`; el delta no los toca y la sección 8 lo hace explícito.
- [x] Por cada fase: ¿un `trigger<Evento>` por cada evento de la **unión** de los `events` de sus estados, y ninguno de más?
- [x] Por cada fase: ¿un `onEnter<Estado>` por **cada** estado, y ninguno de más?
- [x] ¿Ningún `PhaseEventManagerImpl` declara `triggerInitialEvent` ni `triggerExit`?
- [x] ¿Cada `trigger*` tiene su lista **numerada** de acciones, y el `UPDATE_STATE` de cada una coincide con la tabla de transiciones?
- [x] ¿`triggerDelete` **no** llama a `UPDATE_STATE`?
- [x] ¿Cada fase declara sus `@Inject`, sus constantes y el `import` de `States` de **su propia** versión? — sin cambios.
- [x] ¿Ningún `trigger*`/`onEnter*` se factoriza en una superclase compartida? — no; lo común va a `JustificacionFaltaProfesoradoV1Util`, que es una clase de funciones estáticas, no una superclase.

**Validador**

- [x] Por cada fase: ¿un `getForState<Estado>InEvent<Evento>` por **cada pareja** salvo `DELETE`, y ninguno de más?
- [x] ¿Cada método tiene sus `field(...)` con las reglas y **argumentos literales**, o un `rules { }` vacío declarado explícitamente?
- [x] ¿Ningún `field(...)` menciona un campo `servidor`? — la única excepción, `pdfSolicitudFirmado` en `PRESENTAR`, es la de firma en cliente y está documentada.
- [x] ¿Todo campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista? — los cinco del periodo, más motivo, otroMotivo y justificante.
- [x] ¿Los enums usados en `ifValueIn` tienen su `import` declarado? — `MotivoFaltaJustificacionFaltaProfesoradoV1`, que se conserva.

**Vistas**

- [x] ¿El diseño pasa el **checklist de vistas** de `vistas.md` §8? — sí; los puntos con contenido nuevo son el ASCII Layout de `datos-falta` (paso 3), en el que ningún panel anidado envuelve un solo campo (los modos `UN_DIA_COMPLETO` y `UNAS_HORAS_UN_DIA` comparten `datos-falta-un-dia`), el ASCII Layout del gemelo de solo lectura `datos-falta-view` (paso 3), que ningún panel del almacén queda sin incluir, y que los cinco forms de solo `EXIT` (`ENTRADA_DATOS`/`PENDIENTE_PRESENTACION` genéricos, `PENDIENTE_RESOLUCION` genérico, `ACEPTADO`, `RECHAZADO`) llevan su panel `avisoEstadoExpediente` (§6.1).

**Permisos y pasos**

- [x] ¿El perfil del estado inicial lo da el `<aces>` del trámite, el de la versión activa o security? — security (`AceProfileTipoTramite`), sin cambios.
- [x] ¿Todo perfil usado por algún estado tiene actor desde un origen válido? — sin cambios.
- [x] ¿Cada `<ace>` lleva `perfil` y exactamente uno de `tipoUsuario`/`cargo`, sin repetir lo que ya da security y sin tocar esos ficheros? — el delta no escribe ningún `<ace>`.
- [x] ¿La tabla «Ficheros a crear o modificar» lista **todos** los ficheros reales y ninguno generado? ¿Solo los tocados por el delta?
- [x] ¿Los pasos siguen el orden obligatorio de §9, con `CreateFilesTask` en su sitio? — el delta **no añade ninguna fase**, así que el paso de `CreateFilesTask` **no existe** (§9), y los demás respetan el orden relativo de la tabla de la sección 6.
- [x] **Iniciativa de MODIFICACIÓN:** ¿hay **exactamente un paso de fichero por fila** de la tabla, renumerados sin huecos, y con `./run.sh` como último paso? — doce filas, doce pasos (1-12: los ocho de siempre más los dos fragmentos —`_template.xml` modificado y `_documentacionAportada.xml` nuevo— y los dos documentos con sus `<include>` actualizados), más el paso 13 de verificación.
- [x] ¿Cada paso de un XML dice «cópialo literalmente» con origen y destino, y cada paso de un `.java`/`.kt` apunta a su sección sin duplicarla?
- [x] ¿El paso final lleva `./run.sh` y la comprobación en runtime de los agujeros que el build no ve?

**Tests**

- [x] ¿`design/test-e2e-desc.md` existe, menciona **cada estado** y **cada transición** del delta, y no lleva código ni selectores?
- [x] **Iniciativa de MODIFICACIÓN:** ¿los `T-NNN` empiezan en el **primer número libre** de la carpeta espejo? — la carpeta no existe, así que empiezan en `T-001`.
- [x] **Iniciativa de MODIFICACIÓN:** ¿está la subsección «Tests E2E supersedidos» con cada test invalidado, o **ausente** porque no invalida ninguno? — ausente: no hay ningún test E2E persistido de esta versión.
