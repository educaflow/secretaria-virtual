---
type: implementation-task
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml` | Modificar | k-vistas (forms.md, actions.md) | Sustituye el panel único de firma por los seis paneles excluyentes y añade los botones y acciones de la firma en servidor |

## Cómo se materializa

El XML **ya está materializado** por el diseñador en `design/views/Pendiente-TareaFirma.xml` (fichero completo
resultante: base real + delta) y es la **fuente de verdad**: **MUST** copiarse **literalmente** (`cp`) a
`src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml`. **MUST NOT** regenerarlo desde
el `design.md`, ni reescribirlo, ni reformatearlo.

La fila es `Acción: Modificar`: el fichero destino **ya existe**. Antes de sobrescribirlo **MUST** aplicarse la
**comprobación de conservación**: todo elemento con nombre del fichero real actual (panel, campo, botón,
acción) tiene que estar presente en el XML del diseño **salvo** los tres listados en la sección
«Eliminaciones declaradas» del `design.md`, que se copian más abajo. Si pasa, se sobrescribe; si no, se
reporta `CONFLICT` sin fusionar a mano.

## Texto del diseño (verbatim)

### Paso 7 — Vista: los seis paneles del paso de firmar

**Fichero:** `src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml` (Modificar)
**XML del diseño:** `design/views/Pendiente-TareaFirma.xml` (fichero completo resultante: base real + delta)

**Resumen estructural**

- **Preexistente (se conserva):** el `<action-view>` `subsysFirmas.Pendiente@TareaFirma-action` con su
  `<domain>` por estado y firmante; el `<grid>` maestro; el `<panel-related>` «Documentos a firmar»; el panel
  `paso2Rechazado`; los paneles de botones `buttonsPaso1Inicio` y `buttonsPaso2Rechazado`; todos los
  `action-group`, el `action-condition` de rechazo, los tres `action-record` de `pasoActual` y los cuatro
  `action-method` existentes; y el bloque completo del detalle `DocumentoFirma` (grid + form con los dos
  `<viewer>` de PDF).
- **Delta (nuevo/cambiado):**
  - En el panel `tareaFirmaInsertDTO`: campo `situacionFirma` con `showIf="false"` (junto al `pasoActual` que
    ya estaba). Es lo que mete el valor en el registro del formulario para que los `showIf` lo puedan leer.
  - **Seis paneles excluyentes** que **sustituyen** al panel único `paso2Firmar`, todos con el mismo título
    visible «Firmar el documento» y con `showIf` sobre `pasoActual=='paso2Firmar'` **y** el valor de
    `situacionFirma`: `paso2FirmarSinCertificado`, `paso2FirmarDispositivoConPin`,
    `paso2FirmarDispositivoSinPin`, `paso2FirmarFicheroConClave`, `paso2FirmarFicheroSinClave`,
    `paso2FirmarSinDni`.
  - **Tres paneles de botones excluyentes** que **sustituyen** a `buttonsPaso2Firmar`:
    `buttonsPaso2FirmarAutoFirma` (solo `SIN_CERTIFICADO`), `buttonsPaso2FirmarServidor` (las cuatro
    situaciones de firma en servidor) y `buttonsPaso2FirmarSinDni`, que es el **caso por defecto**: su `showIf`
    **no** compara con `SIN_DNI`, sino que **niega** los cinco códigos que sí llevan botón de firmar
    (`!(SIN_CERTIFICADO || DISPOSITIVO_CON_PIN || DISPOSITIVO_SIN_PIN || FICHERO_CON_CLAVE || FICHERO_SIN_CLAVE)`).
    Así los tres siguen siendo mutuamente excluyentes **y cubren todo el dominio**: si `situacionFirma` llegara
    vacía o con un valor desconocido, el firmante sigue viendo el «Atrás» y no se queda en un paso sin salida
    (RUI-…-015, condición «Siempre»). El panel **de contenido** `paso2FirmarSinDni` **sí** conserva la
    comparación con `SIN_DNI` (RUI-…-006): su mensaje solo es cierto para ese caso.
  - Un `action-record` nuevo `…-set-claveFirma-null-action`, encadenado en el `onLoad-action` y en el
    `btnPaso1InicioFirmar-action`.
  - Un `action-group` nuevo `…-btnPaso2FirmarServidorGuardar-action`, un `action-validate` nuevo
    (`…-Local-validateFirmarEnServidor-action`) y dos `action-method` nuevos
    (`…-Remote-validateFirmarEnServidor-action` y `…-Remote-firmarEnServidor-action`).

**Por qué paneles anidados y no `showIf` en los campos.** Un elemento oculto con `showIf` **reserva sus
columnas** (k-vistas/forms.md §"Campos condicionales y el problema de los huecos"). Con seis variantes del paso
de firmar, la única maquetación que no deja huecos ni desplaza botones es un panel por variante con el `showIf`
**en el panel**. Y los `showIf` de los tres paneles de botones son mutuamente excluyentes por construcción,
porque los seis valores de `SituacionFirma` particionan el dominio.

#### ASCII Layout

Panel `tareaFirmaInsertDTO` (readonly). Tiene un único elemento condicional visible, `fechaResolucion`
(`showIf="fechaResolucion!=null"`), así que se dibuja **un ASCII Layout por estado**. En esta vista la tarea
está siempre pendiente, de modo que el estado real es el primero; el segundo se dibuja por completitud.

Leyenda: `m` = `motivoFirma` (8), `e` = `estadoTareaFirma` (4), `s` = `fechaSolicitud` (4),
`r` = `fechaResolucion` (4), `p` = `pasoActual` (6, `colSpan` por defecto), `q` = `situacionFirma` (6,
`colSpan` por defecto), `·` = columna vacía.

```
── fechaResolucion oculta (el caso de esta vista: la tarea está pendiente) ──
mmmmmmmmeeee   ← motivoFirma(8) + estadoTareaFirma(4)
ssss········   ← fechaSolicitud(4) + 8 columnas vacías (4 las reserva fechaResolucion, oculta)
ppppppqqqqqq   ← pasoActual(6) + situacionFirma(6)

── fechaResolucion visible ──
mmmmmmmmeeee   ← motivoFirma(8) + estadoTareaFirma(4)
ssssrrrr····   ← fechaSolicitud(4) + fechaResolucion(4) + 4 columnas vacías
ppppppqqqqqq   ← pasoActual(6) + situacionFirma(6)
```

`pasoActual` y `situacionFirma` llevan `showIf="false"`: son campos técnicos que meten su valor en el registro
del formulario para que los `showIf` de los demás paneles los puedan leer, y **nunca se pintan**. Su fila
aparece en el dibujo porque **reserva** sus doce columnas (un elemento oculto sigue consumiendo celdas,
k-vistas/forms.md §"Campos condicionales y el problema de los huecos"), pero visualmente el panel termina en la
fila de las fechas. Al ocupar la fila entera entre los dos, no dejan ningún hueco que empuje a otro campo.

Paneles del paso de firmar — **un dibujo por estado**, ya que son excluyentes:

```
── situacionFirma == SIN_CERTIFICADO → paso2FirmarSinCertificado ──
hhhhhhhhhhhh   ← help(12) con el aviso y el enlace de AutoFirma

── situacionFirma == DISPOSITIVO_CON_PIN → paso2FirmarDispositivoConPin ──
hhhhhhhhhhhh   ← help(12) «Los documentos se firmarán en el servidor…»

── situacionFirma == DISPOSITIVO_SIN_PIN → paso2FirmarDispositivoSinPin ──
hhhhhhhhhhhh   ← help(12) «…Introduzca el PIN de su dispositivo criptográfico.»
cccc········   ← claveFirma título «PIN» (4): mismo ancho que en el otro panel que la pide

── situacionFirma == FICHERO_CON_CLAVE → paso2FirmarFicheroConClave ──
hhhhhhhhhhhh   ← help(12) «Los documentos se firmarán en el servidor…»

── situacionFirma == FICHERO_SIN_CLAVE → paso2FirmarFicheroSinClave ──
hhhhhhhhhhhh   ← help(12) «…Introduzca la contraseña de su certificado.»
cccc········   ← claveFirma título «Contraseña» (4): es el título más largo de los dos y el que fija el ancho

── situacionFirma == SIN_DNI → paso2FirmarSinDni ──
hhhhhhhhhhhh   ← help(12) variant="warning" con el aviso de que no se puede firmar
```

La clave queda sola en su fila con hueco a la derecha: es correcto, no hay ningún campo semánticamente
relacionado con el que agruparla (k-vistas/forms.md §"Un campo solo en una fila es una señal de alerta").
Los dos paneles que la piden son variantes del **mismo** paso, así que el campo lleva **el mismo `colSpan="4"`**
en los dos: empieza en la columna 1 y termina en la 4 tanto con el título «PIN» como con el título «Contraseña»,
de modo que **sus dos bordes** —izquierdo y derecho— quedan alineados entre estados y el campo **no cambia de
tamaño ni de posición** al cambiar la situación de firma (`k-vistas/forms.md` §checklist: bordes alineados entre
filas y entre paneles condicionales). Es el mismo criterio que se aplica más abajo a los paneles de botones. El
ancho que se elige es el que admite el título **más largo** de los dos («Contraseña»); dárselo también al del
«PIN» solo le deja algo de holgura, que es preferible a que el recuadro salte de sitio.

`buttons-panel` — un dibujo por estado (los cinco paneles anidados son mutuamente excluyentes):

```
── pasoActual == paso1Inicio ──
rrr......fff   ← btnPaso1InicioRechazar(3) + colOffset(6) + btnPaso1InicioFirmar(3)          [3+6+3 = 12]

── pasoActual == paso2Rechazado ──
aaa......ggg   ← btnPaso2RechazadoAtras(3) + colOffset(6) + btnPaso2RechazadoGuardar(3)      [3+6+3 = 12]

── pasoActual == paso2Firmar && situacionFirma == SIN_CERTIFICADO ──
aaa....ggggg   ← btnPaso2FirmarAtrasAutoFirma(3) + colOffset(4) + btnPaso2FirmarGuardar(5)   [3+4+5 = 12]

── pasoActual == paso2Firmar && situacionFirma ∈ {DISPOSITIVO_CON_PIN, DISPOSITIVO_SIN_PIN,
                                                  FICHERO_CON_CLAVE, FICHERO_SIN_CLAVE} ──
aaa....ggggg   ← btnPaso2FirmarAtrasServidor(3) + colOffset(4) + btnPaso2FirmarServidorGuardar(5)  [3+4+5 = 12]

── pasoActual == paso2Firmar && cualquier otra situacionFirma (SIN_DNI, null o desconocida) ──
aaa·········   ← btnPaso2FirmarAtrasSinDni(3); no hay botón de firmar (RUI-…-014)
```

El panel de botones de la firma en servidor usa **el mismo reparto 3 + colOffset 4 + 5** que el panel
preexistente de AutoFirma: los tres paneles de botones son variantes del **mismo** paso, así que al cambiar de
situación de firma el botón principal **no cambia de tamaño ni de posición**, y sus bordes de columna quedan
alineados entre estados (`k-vistas/forms.md` §checklist: bordes alineados entre filas y entre paneles
condicionales). El reparto que se respeta es el del elemento **preexistente**: el que cede es el panel nuevo.

En los cinco estados el botón secundario (Atrás/Rechazar) está pegado a la izquierda y el principal pegado al
borde derecho (`colOffset + colSpan = 12`). En el estado del panel `buttonsPaso2FirmarSinDni` no hay principal:
la fila la ocupa solo el secundario, y ese hueco es la traducción visual de la desviación que el propio spec
declara («en el panel de firmante sin DNI no hay ningún botón de firmar»).

#### Acciones nuevas y cambiadas

| Acción | Tipo | Propósito | Campos/condiciones |
|---|---|---|---|
| `…-set-claveFirma-null-action` | `action-record` | Vacía la clave tecleada | `claveFirma` ← `eval: null` |
| `…-onLoad-action` | `action-group` (cambiada) | Al abrir la tarea: vacía la clave y sitúa el paso 1 | encadena `set-claveFirma-null` + `set-pasoActual-paso1Inicio` |
| `…-btnPaso1InicioFirmar-action` | `action-group` (cambiada) | Al entrar en el paso de firmar: vacía la clave y sitúa el paso 2 | encadena `set-claveFirma-null` + `set-pasoActual-paso2Firmar` |
| `…-btnPaso2FirmarServidorGuardar-action` | `action-group` (nueva) | Firma en el servidor y cierra | `Local-validateFirmarEnServidor` → `Remote-validateFirmarEnServidor` → `Remote-firmarEnServidor` → `force-back` |
| `…-Local-validateFirmarEnServidor-action` | `action-validate` (nueva) | Refuerzo **de cliente** de V-TareaFirma-005 / V-TareaFirma-006: avisa de la clave obligatoria sin roundtrip | dos `<error>`: «El PIN es obligatorio» si `situacionFirma=='DISPOSITIVO_SIN_PIN' && (claveFirma==null \|\| claveFirma=='')`; «La contraseña es obligatoria» si `situacionFirma=='FICHERO_SIN_CLAVE' && (claveFirma==null \|\| claveFirma=='')` |
| `…-Remote-validateFirmarEnServidor-action` | `action-method` (nueva) | Validación de servidor de la operación custom | `TareaFirmaController.validateFirmarEnServidor` |
| `…-Remote-firmarEnServidor-action` | `action-method` (nueva) | Ejecuta la firma en servidor | `TareaFirmaController.firmarEnServidor` |

La validación de la operación custom va **inmediatamente antes** de la operación, sin nada intercalado
(k-vistas/actions.md). Si cualquiera de las dos devuelve error, la cadena se detiene y el `force-back` **no**
se ejecuta: el firmante se queda en el paso de firmar, con su panel y con lo que hubiera tecleado
(RUI-…-017 y RUI-…-018).

**La clave obligatoria lleva además capa cliente.** `…-Local-validateFirmarEnServidor-action` es un
`<action-validate>` con **dos** `<error>` que reproducen **literalmente** los mensajes del spec («El PIN es
obligatorio» y «La contraseña es obligatoria»), cada uno condicionado a la situación de firma que lo hace
aplicable. Va **la primera** del `action-group`, antes de la validación remota, de modo que el firmante recibe
el aviso sin roundtrip; la fuente de verdad sigue siendo el servidor (V-TareaFirma-005 / V-TareaFirma-006), que
es la única capa por la que pasan todas las vías de entrada. Es el refuerzo **opcional** que admite
`k-validaciones/validaciones.md` §3, nunca la única capa. Como los dos mensajes son idénticos a los del
servidor, el escenario ESC-004 ve el mismo literal lo bloquee el cliente o lo bloquee el servidor.

**MUST NOT** implementarlo con un `<action-condition>` de dos `<check field="claveFirma"/>`: `ActionCondition`
hace `errors.put(field, …)` por cada `check`, así que el segundo **borra** el error del primero y solo se vería
uno de los dos mensajes. Los `<error>` de `<action-validate>` no tienen ese problema.

**Verificación:**
```bash
xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
  src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml
grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml   # sin coincidencias
grep -nE 'Remote-validate(Save|Delete)-action' src/main/java/com/educaflow/subsystem/firmas/views/Pendiente-TareaFirma.xml     # sin coincidencias
```

### Reglas de UI (sección «Trazabilidad Origen spec → V/R/U → ubicación»)

### Reglas de UI `U-documentos-pendientes-de-firma-NNN`

Todas viven en `views/Pendiente-TareaFirma.xml`, form `subsysFirmas.Pendiente@TareaFirma-form`.

| U | Origen spec | Ubicación |
|---|---|---|
| U-documentos-pendientes-de-firma-001 | RUI-documentos-pendientes-de-firma-formulario-001 | `showIf` del panel `paso2FirmarSinCertificado` (`situacionFirma=='SIN_CERTIFICADO'`) |
| U-documentos-pendientes-de-firma-002 | RUI-documentos-pendientes-de-firma-formulario-002 | `showIf` del panel `paso2FirmarDispositivoConPin` |
| U-documentos-pendientes-de-firma-003 | RUI-documentos-pendientes-de-firma-formulario-003 | `showIf` del panel `paso2FirmarDispositivoSinPin` |
| U-documentos-pendientes-de-firma-004 | RUI-documentos-pendientes-de-firma-formulario-004 | `showIf` del panel `paso2FirmarFicheroConClave` |
| U-documentos-pendientes-de-firma-005 | RUI-documentos-pendientes-de-firma-formulario-005 | `showIf` del panel `paso2FirmarFicheroSinClave` |
| U-documentos-pendientes-de-firma-006 | RUI-documentos-pendientes-de-firma-formulario-006 | `showIf` del panel `paso2FirmarSinDni` |
| U-documentos-pendientes-de-firma-007 | RUI-documentos-pendientes-de-firma-formulario-007 | `title="PIN"` del `<field name="claveFirma">` de `paso2FirmarDispositivoSinPin` |
| U-documentos-pendientes-de-firma-008 | RUI-documentos-pendientes-de-firma-formulario-008 | `title="Contraseña"` del `<field name="claveFirma">` de `paso2FirmarFicheroSinClave` |
| U-documentos-pendientes-de-firma-009 | RUI-documentos-pendientes-de-firma-formulario-009 | `widget="password"` en los dos `<field name="claveFirma">` (reforzado por `password="true"` en el dominio) |
| U-documentos-pendientes-de-firma-010 | RUI-documentos-pendientes-de-firma-formulario-010 | `required="true"` en los dos `<field name="claveFirma">` |
| U-documentos-pendientes-de-firma-011 | RUI-documentos-pendientes-de-firma-formulario-011 | `action-record` `…-set-claveFirma-null-action`, encadenada en `…-onLoad-action` y en `…-btnPaso1InicioFirmar-action` |
| U-documentos-pendientes-de-firma-012 | RUI-documentos-pendientes-de-firma-formulario-012 | `showIf` del panel `buttonsPaso2FirmarAutoFirma`, que contiene `btnPaso2FirmarGuardar` |
| U-documentos-pendientes-de-firma-013 | RUI-documentos-pendientes-de-firma-formulario-013 | `showIf` del panel `buttonsPaso2FirmarServidor`, que contiene `btnPaso2FirmarServidorGuardar` |
| U-documentos-pendientes-de-firma-014 | RUI-documentos-pendientes-de-firma-formulario-014 | Panel `buttonsPaso2FirmarSinDni`: solo lleva `btnPaso2FirmarAtrasSinDni`. El caso `SIN_DNI` cae en este panel por ser el **caso por defecto** (ver U-…-015) |
| U-documentos-pendientes-de-firma-015 | RUI-documentos-pendientes-de-firma-formulario-015 | Un botón `btnPaso2FirmarAtras…` en cada uno de los tres paneles de botones del paso de firmar. Como la regla es de condición «Siempre», el tercer panel (`buttonsPaso2FirmarSinDni`) usa la **condición negada** de los cinco códigos con botón de firmar, no `situacionFirma=='SIN_DNI'`: así los tres paneles cubren **todo** el dominio y el «Atrás» también aparece si `situacionFirma` llegara vacía o con un valor desconocido |
| U-documentos-pendientes-de-firma-016 | RUI-documentos-pendientes-de-firma-formulario-016 | `<field name="claveFirma">` solo existe dentro de `paso2FirmarDispositivoSinPin` y `paso2FirmarFicheroSinClave` |
| U-documentos-pendientes-de-firma-017 | RUI-documentos-pendientes-de-firma-formulario-017 | `action-group` `…-btnPaso2FirmarServidorGuardar-action`: el `force-back` va **después** de la acción remota, así que un error detiene la cadena y `pasoActual` no cambia |
| U-documentos-pendientes-de-firma-018 | RUI-documentos-pendientes-de-firma-formulario-018 | Mismo `action-group`: no hay ninguna acción que vacíe `claveFirma` tras el error (solo se vacía al entrar en el paso, U-…-011) |

### Eliminaciones declaradas

## Eliminaciones declaradas

| Elemento eliminado | Fichero | ID de spec que lo justifica |
|---|---|---|
| `<panel name="paso2Firmar">` (el panel único de firma) | `views/Pendiente-TareaFirma.xml` | `screen-documentos-pendientes-de-firma.md` §Paneles: «Los seis paneles siguientes **sustituyen** al único panel de firma que hay hoy». Su contenido (el aviso de AutoFirma) se conserva íntegro dentro del nuevo `paso2FirmarSinCertificado`. |
| `<panel name="buttonsPaso2Firmar">` (el panel de botones del paso de firmar) | `views/Pendiente-TareaFirma.xml` | RUI-…-012, RUI-…-013, RUI-…-014: los botones del paso de firmar dependen ahora de la situación de firma, así que se reparten en tres paneles excluyentes. |
| `<button name="btnPaso2FirmarAtras">` (el botón «Atrás» único) | `views/Pendiente-TareaFirma.xml` | RUI-…-015: pasa a haber un «Atrás» por panel de estado (`btnPaso2FirmarAtrasAutoFirma`, `btnPaso2FirmarAtrasServidor`, `btnPaso2FirmarAtrasSinDni`), los tres apuntando al **mismo** `…-btnPaso2FirmarAtras-action`, que **no** se elimina. Es la regla de botones gemelos de `k-vistas/forms.md`. |

Nada más se elimina. En particular **se conservan** el botón `btnPaso2FirmarGuardar` de AutoFirma con su
`onClick` `serial:…` intacto, sus dos `action-group` y los cuatro `action-method` preexistentes.

### Notas y supuestos aplicables

7. **El formulario no lleva el `buttons-panel` canónico Borrar/Cancelar/Guardar.** Es una desviación
   **preexistente** y justificada por el negocio: la pantalla es un asistente de dos pasos en el que la tarea
   no se guarda ni se borra desde el formulario, sino que se resuelve con acciones propias
   (`k-vistas/forms.md`: «salvo que […] haya algo en el negocio que te haga pensar que no es necesario»). El
   `<form>` sí cumple lo importante: todos los `can*` a `false`, ningún `onSave`, y ningún `<action-method>` de
   validación por entidad para `save`/`delete`. Por mínima intrusión, el diseño **no** cambia esto.

10. **`required="true"` en el campo de la clave, y las dos capas de la validación.** El `required` lo pide
    RUI-…-010 y aporta solo la **marca visual** del cliente: el `required` de Axelor bloquea el flujo de `save`,
    y esta pantalla no guarda —resuelve con una acción propia—, así que por sí solo no impide pulsar el botón.
    Por eso la clave obligatoria tiene **dos capas de validación**, no una:
    - **Cliente (opcional, refuerzo):** `…-Local-validateFirmarEnServidor-action`, un `<action-validate>` con
      los dos `<error>` y los literales exactos del spec, encadenado el primero del `action-group` del botón.
      Evita el roundtrip.
    - **Servidor (fuente de verdad, obligatoria):** V-TareaFirma-005 / V-TareaFirma-006 en
      `validateFirmarEnServidor`, con los mismos literales. Es la única capa por la que pasan todas las vías
      de entrada.

    Como los literales coinciden, ESC-004 ve el mismo mensaje («La contraseña es obligatoria») lo produzca el
    cliente o el servidor: no queda ninguna duda que resolver al ejecutar el E2E. **MUST NOT** quitar la
    validación de servidor en ningún caso; si algún día molestara la marca de `required`, la corrección sería
    cambiarla por `requiredIf` sobre el mismo panel.
