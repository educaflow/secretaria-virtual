---
type: implementation-task
template: expediente
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente

Materializa los **documentos PDF** del tipo y saca de en medio el modelo escaneado.

1. **Mueve** con `git mv` el fichero `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/modelo.pdf` a `…/v1/documentospdf/originales/modelo.pdf`. Motivo: todo `.pdf` que cuelgue **directamente** de `documentospdf/` genera una constante en el enum `TipoDocumentoPdf`, y el modelo visual del impreso oficial no es un documento del trámite. La subcarpeta `originales/` no se escanea.
2. Los dos XML **ya están materializados** en el diseño. **Cópialos literalmente**, **sobrescribiendo** lo que haya:

| Origen | Destino |
|---|---|
| `design/documentospdf/solicitud.xml` | `…/v1/documentospdf/solicitud.xml` |
| `design/documentospdf/resolucion.xml` | `…/v1/documentospdf/resolucion.xml` |

**MUST NOT** modificarlos, reescribirlos ni regenerarlos: los XML del diseño son **contrato fijo**. **MUST NOT** crearse ningún fragmento `_*.xml`: los dos documentos no comparten ningún trozo.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/solicitud.xml` | Crear | `k-tipo-expediente` | Copia de `design/documentospdf/solicitud.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/resolucion.xml` | Crear | `k-tipo-expediente` | Copia de `design/documentospdf/resolucion.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/modelo.pdf` | Modificar | `k-tipo-expediente` | **Moverlo** (`git mv`) a `…/v1/documentospdf/originales/modelo.pdf`. Motivo: todo `.pdf` directo en `documentospdf/` genera una constante en `TipoDocumentoPdf`, y el modelo escaneado del ANEXO VII no es un documento del trámite |

## Paso del diseño (verbatim)

### Paso 14 — `documentospdf/`

1. **Mueve** `…/v1/documentospdf/modelo.pdf` a `…/v1/documentospdf/originales/modelo.pdf` (`git mv`), para que el escaneo del enum `TipoDocumentoPdf` no lo tome por un documento del trámite.
2. Los dos XML están materializados en `design/documentospdf/`. **Cópialos literalmente** a `…/v1/documentospdf/` (`solicitud.xml`, `resolucion.xml`). **MUST NOT** modificarlos, reescribirlos ni regenerarlos. **MUST NOT** crearse ningún fragmento `_*.xml`: los dos documentos no comparten ningún trozo (§5).

**Verificación:** en `documentospdf/` solo hay dos `.xml` (`solicitud.xml` y `resolucion.xml`, ningún `_*.xml`) y la subcarpeta `originales/`; el build genera `solicitud.pdf` y `resolucion.pdf`; el `<extra-code-model>` que reescribe el build sigue teniendo exactamente las dos constantes `SOLICITUD` y `RESOLUCION`.


## `## 5. Documentos PDF` (verbatim, completa)

## 5. Documentos PDF

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/solicitud.xml` | `SOLICITUD` | `SOLICITUD.DATOS_SOLICITUD` + `CONTINUAR` | `pdfSolicitud` (`servidor`) | `cliente (AutoFirma__!!)` o, si el alumno tiene certificado custodiado, `servidor: DNI SecurityUtil.getUser().getDni()` | `entrada` (al presentar, sobre `pdfSolicitudFirmada`) |
| `documentospdf/resolucion.xml` | `RESOLUCION` | `REVISION.PENDIENTE_REVISION` + `ENVIAR_A_FIRMA`, y de nuevo en `RESOLUCION.PENDIENTE_FIRMA_DIRECTOR` + `FIRMAR` | `pdfResolucion` (`servidor`); la firmada, en `pdfResolucionFirmada` (`servidor`) | `servidor: DIRECTOR` | `salida` (al firmar) |

Fragmentos: **ninguno**. Esto **anula expresamente** una indicación de `design-guidelines.md`, que enumera un `_template.xml` bilingüe como tercer XML del trámite; la desviación está declarada en §14 nota 27. Los tres trozos que `documentos.md` declara comunes se resuelven así:

- **Cabecera institucional** — no es contenido del documento: la pinta el generador (logo + `<titulo>`), no hay nada que compartir (§14 nota 17).
- **Identificación del alumno** — los dos documentos abren con una sección de identificación, pero **no imprimen lo mismo**: la solicitud imprime el **bloque A completo del impreso oficial** (apellidos, nombre, NIA, DNI/NIE, dirección, teléfono, población, provincia y CP) y la resolución solo los cuatro datos del destinatario (apellidos, nombre, DNI/NIE y NIA) que su ficha declara. Compartir obligaría a imprimir de más en la resolución o a partir el bloque A en dos (§14 nota 4).
- **Referencia de la matrícula** — aquí sí hay **una línea duplicada literalmente**, pero **no es factorizable con el mecanismo disponible**: un `<include>` solo va como hijo directo de `<documento>` y esa línea es un `<texto>` **dentro** de una sección distinta en cada documento («Expone» / «Vistos»). Se asume la duplicación y **MUST** tocarse en los dos ficheros a la vez (§14 nota 4).

Cada documento declara por tanto su propia `<seccion>` y **MUST NOT** existir ningún fichero con prefijo `_` en `documentospdf/`.

Notas:

- El PDF escaneado del impreso oficial que hoy está en `documentospdf/modelo.pdf` **MUST moverse** a `documentospdf/originales/modelo.pdf`: todo `.pdf` que cuelgue directamente de `documentospdf/` genera una constante en el enum `TipoDocumentoPdf`, y un modelo visual no es un documento del trámite. La subcarpeta `originales/` no se escanea (precedente: `justificacion_falta_profesorado`).
- Recuadros de firma (las constantes viven en el `PhaseEventManagerImpl` de su fase, §9). Los cuatro números de cada uno están **medidos sobre el PDF generado** por `Xml2Pdf` (§14 nota 12), no elegidos a ojo, y caen dentro de la celda del documento a la que pertenecen:
  - solicitud: `Rectangulo(320, 418, 240, 32)`, página `1` — dentro de la celda «Signatura:/Firma:» de la sección «Lugar, fecha y firma», que ocupa `x 28,35 → 566,93` e `y 415,52 → 472,22`; queda a la derecha y **por debajo** de los dos rótulos (líneas base `y 462,70` en valenciano y `y 454,58` en castellano). Son **los mismos ocho argumentos** que la `<action-method>` de la vista de la fase `SOLICITUD`.
  - resolución: `Rectangulo(150, 317, 300, 53)`, página `1` — dentro del recuadro vacío que hay bajo «El director / la directora del centro», que ocupa `x 28,35 → 566,93` e `y 315,28 → 372,00`.
  - En los dos, `x + ancho ≤ 566,93` (el borde derecho del marco del documento) e `y + alto` queda por debajo del borde superior de su celda. **MUST** recomprobarse si se toca la maquetación del documento: cualquier fila que se añada o se quite mueve esas celdas.
- **El ancho de un campo inline nunca es `;12`.** El widget se dibuja en `x + PAD`, así que un campo inline que pida la rejilla entera se sale del marco: medido sobre el PDF generado, con `;12` su borde derecho cae en `x = 569,79` frente al borde derecho de la tabla en `x = 566,93`. Las cuatro expresiones de ancho completo de `resolucion.xml` (la fórmula y el motivo, en sus dos idiomas) llevan por eso `;11.8` — el `colspan="12"` de la fila **sí** se queda en 12, porque es la celda y no el widget. El separador acepta decimales (`([0-9]+(?:\.[0-9]+)?)`). En `solicitud.xml` no hace falta: su campo más ancho llega a `x = 561,86`.
- **Las fechas en valenciano NO llevan el literal `'de'` en el patrón.** En catalán/valenciano el nombre de mes en **contexto de formato** (`MMMM` con `Locale.forLanguageTag("ca")`) ya trae la preposición con la elisión resuelta —«de gener», «d’abril», «d’agost», «d’octubre»—, así que el patrón correcto es `"d MMMM 'de' yyyy"`: con `"d 'de' MMMM 'de' yyyy"` saldría «1 de de gener de 2026» los meses consonánticos y «1 de d’abril de 2026» los vocálicos. El castellano **sí** conserva `"d 'de' MMMM 'de' yyyy"`, porque su `MMMM` es solo el mes. Afecta a las **tres** fechas en valenciano: la de la solicitud y las dos de la resolución (presentación y resolución).
- Las expresiones Groovy de los documentos **fallan en silencio** (log + campo vacío): el paso final de verificación exige abrir los dos PDF generados en runtime y comprobar que ningún hueco queda en blanco, que las tres fechas en valenciano salen bien escritas y que ningún campo inline sobresale del marco.


## `## 11. Reparto de reglas` — cómo consultan los documentos al dueño de «la revisión rechaza la anulación» (verbatim)

**Dueño único de «la revisión rechaza la anulación» — un predicado en el servidor, no una convención de redacción.** La decisión es una sola —*la revisión rechaza si y solo si `sentidoRevision == RECHAZAR`*, de donde se siguen sus dos consecuencias: la resolución **desestima** la solicitud y el **motivo del rechazo** aplica— y su **dueño** es la función estática `ReglasAnulacionMatricula.esRechazo(expediente)`, declarada en `…/v1/ReglasAnulacionMatricula.kt`, en la raíz de la versión (§9.0.3 y §4). **MUST NOT** declararse en el `<extra-code-model>` del `domains.xml`: el build reescribe ese bloque entero en cada compilación y el predicado desaparecería, dejando sin compilar al trigger y al validador y en silencio a las dos expresiones del PDF. Los **cuatro** puntos que se evalúan en el **servidor** (en tres ficheros: `resolucion.xml` lo consulta dos veces, una por cada consecuencia) no la escriben: se la **preguntan**.

| Sitio | Capa | Cómo consulta al dueño |
|---|---|---|
| acción 4 de `triggerEnviarAFirma` (§9.2) | Java, servidor | `if (ReglasAnulacionMatricula.esRechazo(expediente) == false) { … }` |
| rama de `motivoRechazo` en `getForStatePendienteRevisionInEventEnviarAFirma` (§10.2) | DSL del validador, servidor | `ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true)) { … }` |
| expresión de la **fórmula** de estimación/desestimación en `documentospdf/resolucion.xml` (§5, §14 nota 5) | Groovy, servidor | `${<FQCN>.esRechazo(self) ? "…desestima…" : "…estima…"}`, con `<FQCN>` = `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula` (en un `${…}` no hay `import`) |
| expresión del **motivo** en `documentospdf/resolucion.xml` (§5) | Groovy, servidor | `${<FQCN>.esRechazo(self) ? … : ""}`, con el mismo `<FQCN>` |

**MUST NOT** aparecer en ninguno de esos cuatro una comparación contra el enum (`sentidoRevision == RECHAZAR`, `!= RECHAZAR` y, mucho menos, `== ACEPTAR`): eso devolvería la decisión a varios dueños. Que el DSL lo admita no es una suposición — `IfValueIn` recibe un `KFunction<*>` y lo invoca con `dependentField.call(bean)`, así que un método sin argumentos vale igual que un getter; y `AllowPropertiesFactory` solo recoge los `field(...)`, nunca el dependiente de una rama, así que consultar el predicado **no** mete nada en la whitelist del evento.
