---
type: implementation-task
template: expediente
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-i18n

## Ficheros que cubre esta tarea (filas de la sección 6 del diseño)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/_template.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/documentospdf/_template.xml`: se queda solo con centro + interesado |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/_documentacionAportada.xml` | Crear | `k-tipo-expediente` | Copia literal de `design/documentospdf/_documentacionAportada.xml` |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/solicitud.xml` | Modificar | `k-tipo-expediente`, `k-i18n` | Copia literal de `design/documentospdf/solicitud.xml`: cambian sus `<include>` y se añade inline la sección «Declara que» con las cuatro casillas |
| `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/resolucion.xml` | Modificar | `k-tipo-expediente` | Copia literal de `design/documentospdf/resolucion.xml`: cambian sus `<include>` y se añade inline la sección «Declara que» con solo el motivo |

## Cómo materializarlos

Los cuatro XML están **materializados en el diseño** y son **contrato fijo**: se **copian literalmente**, **sobrescribiendo** el fichero actual del árbol cuando ya existe (y el esqueleto que hubiera dejado `CreateFilesTask`), **sin regenerarlos** ni reescribir ni una línea.

| Origen (en `design/`) | Destino resuelto |
|---|---|
| `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/documentospdf/_template.xml` | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/_template.xml` |
| `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/documentospdf/_documentacionAportada.xml` | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/_documentacionAportada.xml` |
| `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/documentospdf/solicitud.xml` | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/solicitud.xml` |
| `.sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/design/documentospdf/resolucion.xml` | `src/main/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/documentospdf/resolucion.xml` |

**MUST NOT** crearse ni tocarse `i18n_es.csv` ni `i18n_ca.csv`: los genera un script.

## Pasos del diseño (verbatim)

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

## 5. Documentos PDF (verbatim, completa)

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

## Contrato de la tabla 4.3 que usan las cuatro casillas (verbatim)

| tipo de jornada faltada | `fechaInicio` | `fechaFin` | `horaInicio` | `horaFin` | título de `fechaInicio` |
|---|---|---|---|---|---|
| `UN_DIA_COMPLETO` | sí | — | — | — | «Fecha» |
| `VARIOS_DIAS_COMPLETOS` | sí | sí | — | — | «Fecha de Inicio» |
| `UNAS_HORAS_UN_DIA` | sí | — | sí | sí | «Fecha» |
| `VARIOS_DIAS_PRIMERO_PARCIAL` | sí | sí | sí | — | «Fecha de Inicio» |
| *(sin elegir)* | sí | — | — | — | «Fecha de Inicio» |

## Nota del diseño aplicable a estos ficheros (sección 14, verbatim)

- **La Resolución deja de mostrar el periodo; el resto no cambia.** La especificación dice que la Resolución «no muestra ese dato y no cambia en nada», pero el as-is sí lo mostraba: `resolucion.xml` incluía el fragmento cajón de sastre `_template.xml`, cuya sección «Declara que» llevaba el periodo (días/mes/año y las casillas de jornada) y el motivo. Se resuelve la contradicción así (`decisiones.md` D5):
  - La Resolución **deja de mostrar el periodo**: es la lectura literal de «no muestra ese dato», y además sus campos (`dias`/`mes`/`anyo`, `TODA_LA_JORNADA`/`JORNADA_PARCIAL`) desaparecen del modelo.
  - **Conserva el motivo de la falta** («Por el motivo siguiente» + otro motivo) en su sección «Declara que», inline, y todas las demás secciones, en el mismo orden: es la lectura de «no cambia en nada» para todo lo que no es el periodo.
  - Por tanto su contenido visible **sí cambia** respecto al as-is, pero solo en la parte del periodo.
- **`_template.xml` conserva su nombre aunque ya solo contiene centro + interesado.** `documentos.md` §2.5 pide que un fragmento se llame por lo que contiene, pero renombrarlo exigiría borrar el fichero del as-is, y la columna `Acción` de §6 solo admite `Crear`/`Modificar`. Renombrarlo queda como deuda para cuando el contrato admita borrar ficheros.
