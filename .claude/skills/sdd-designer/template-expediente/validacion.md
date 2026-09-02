# Reglas de verificación del diseño de un tipo de expediente

Define **qué cuenta como fallo en el diseño**: la **validación mecánica** de cada artefacto materializado (§1) y las **comprobaciones semánticas** de coherencia, cobertura y seguridad (§2).

Lo aplica el **verificador** sobre la carpeta `design/`; el **corrector** lo usa para saber por qué cada cosa es un fallo y **cuál es la corrección esperada** (cada comprobación la declara).

**Contrato de salida del verificador** (lo fija el motor): si no encuentra nada que corregir responde **exactamente** `OK-CORRECTO`; si encuentra problemas responde **solo** líneas JSONL, una por problema, con los campos `id` / `severidad` / `fichero` / `ubicacion` / `origen` / `problema` / `correccion`. En `origen` va el identificador de la regla incumplida: el `ID` de la comprobación de §2 (p.ej. `C-E02`), la regla de vistas de `vistas.md` §4 (`X1`…`Y3`), o el identificador del spec (`VAL-…`, `RN-…`, `ESC-…`).

> **REGLA DE GENERALIDAD.** Este fichero describe **el patrón**. **MUST NOT** aparecer en él el nombre de ningún trámite, fase, estado, evento, campo, enum, perfil ni documento reales; se usan los placeholders de `design-contract.md` §0.1. Los ejemplos van en bloques `> **Ejemplo** (ilustrativo, NO normativo):` con nombres inventados.

---

## 1. Validación mecánica

**CRITICAL — aquí NO todos los XML tienen el mismo esquema.** Aplicar el mismo comando a todos produce falsos positivos masivos. La tabla siguiente es normativa: cada familia de ficheros se valida **como dice su fila y de ninguna otra forma**.

| Fichero de `design/` | Raíz | Cómo se valida |
|---|---|---|
| `domains.xml` | `<domain-models>` | **Bien formado + XSD**. Valida contra el XSD de domain-models |
| `views.xml` (raíz de la versión) | `<object-views>` | **Solo bien formado.** **MUST NOT** validarse contra `object-views.xsd` |
| `fases/<fase>/views.xml` | `<object-views>` | **Solo bien formado.** **MUST NOT** validarse contra `object-views.xsd` |
| `TramiteInstance.xml` | `<Tramite>` | **Solo bien formado** (no hay XSD publicado) |
| `TipoExpedienteInstance.xml` | `<TipoExpediente>` | **Solo bien formado** (no hay XSD publicado) |
| `permisos.xml` | `<datos>` | **Solo bien formado** (no hay XSD publicado) |
| `documentospdf/*.xml` | `<documento>` / `<fragmento>` | **Bien formado**, y **XSD remoto solo si hay red** |
| `estados.puml` | — (no es XML) | Comprobación **sintáctica** de apertura y cierre |

**REQUIRED — los comandos los ejecuta el VERIFICADOR con `Bash`.** El motor **NUNCA** ejecuta ninguno (es agnóstico al artefacto). **MUST NOT** sustituirse esta validación por una inspección «a ojo».

### 1.1 Bien formado — todos los XML

```bash
D=.sdd/drafts/{iniciativa}/design
find "$D" -name '*.xml' -print0 | xargs -0 -n1 xmllint --noout
```

Cada error de `xmllint` es un fallo **BLOCKING**. **Corrección esperada:** arreglar el XML mal formado (etiqueta sin cerrar, entidad sin escapar, `&` suelto, CDATA sin cerrar).

### 1.2 `domains.xml` — bien formado **y** contra el XSD

El XSD vive en el repositorio hermano de AOP (fuera de este repositorio); usarlo **local** evita depender de la red. La ruta se resuelve desde la raíz del proyecto:

```bash
XSD=../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd
xmllint --noout --schema "$XSD" .sdd/drafts/{iniciativa}/design/domains.xml
```

- La salida esperada es `… validates`. `xmllint` imprime además varios **warnings del propio XSD** (`Skipping attribute use prohibition…`): son del esquema de AOP, no del diseño — **MUST NOT** reportarse como fallo.
- Si el fichero del XSD **no existe** (el repositorio hermano no está disponible), se admite validar contra la URL que el propio `domains.xml` declara en su `xsi:schemaLocation`; y si tampoco hay red, degradar a **solo bien formado** y **MUST NOT** reportar fallo por ello.
- Un error de validación real es **BLOCKING**. **Corrección esperada:** ajustar el elemento o el atributo rechazado al esquema (tipo de campo inexistente, atributo mal escrito, orden de elementos inválido).

### 1.3 Los `views.xml` — solo bien formado

**CRITICAL — MUST NOT** validarse ningún `views.xml` de un tipo de expediente contra `object-views.xsd`. Estos ficheros están en un **formato propio preprocesado**: llevan tags custom (`<include-panels>`, `<footer>`, `<buttons-left>`, `<buttons-right>`, `<form state=…>`, líneas sueltas con prefijo `-`) que **no existen** en el XSD de Axelor. La validación fallaría siempre y el corrector "arreglaría" ficheros que están bien.

```bash
D=.sdd/drafts/{iniciativa}/design
xmllint --noout "$D/views.xml"
find "$D/fases" -name 'views.xml' -print0 | xargs -0 -n1 xmllint --noout
```

Lo que sustituye a la validación XSD son las reglas estructurales de §2 (bloque **H**) y el checklist de `vistas.md` §8.

- ❌ INCORRECTO: `xmllint --noout --schema …/object-views.xsd design/fases/<fase>/views.xml` → produce decenas de errores espurios sobre `include-panels` y `footer`.
- ✅ CORRECTO: `xmllint --noout design/fases/<fase>/views.xml` + aplicar §2 bloque H.

### 1.4 `TramiteInstance.xml`, `TipoExpedienteInstance.xml` y `permisos.xml` — solo bien formado

No existe XSD publicado para ninguno de los tres: los parsea JAXB (los dos primeros) y el data-init (el tercero). **MUST NOT** inventarse un esquema ni validarlos contra uno ajeno. Su corrección la dan las reglas estructurales de §2 (bloques **B**, **C** y **J**).

**CRITICAL** — para el `TipoExpedienteInstance.xml`, recuerda que **JAXB ignora en silencio cualquier tag o atributo desconocido**: un typo no da error, aplica el default. Por eso las comprobaciones del bloque **C** son la única red.

### 1.5 `documentospdf/*.xml` — XSD remoto, tolerante a la falta de red

Cada documento y cada fragmento declara `xsi:noNamespaceSchemaLocation` apuntando a una **URL remota** del repositorio de build-tools.

```bash
D=.sdd/drafts/{iniciativa}/design
[ -d "$D/documentospdf" ] || echo "(el tipo no genera PDFs: nada que validar aquí)"
for f in "$D"/documentospdf/*.xml; do
  xmllint --noout "$f" || echo "FAIL-WELLFORMED: $f"
done
# Validación contra el XSD remoto: SOLO si hay red.
XSD_DOC=$(sed -n 's/.*noNamespaceSchemaLocation="\([^"]*\)".*/\1/p' "$D"/documentospdf/*.xml | head -1)
if [ -n "$XSD_DOC" ] && curl -sfI --max-time 10 "$XSD_DOC" >/dev/null 2>&1; then
  for f in "$D"/documentospdf/*.xml; do xmllint --noout --schema "$XSD_DOC" "$f"; done
else
  echo "SIN-RED: validacion XSD de documentospdf omitida (NO es un fallo)"
fi
```

- **CRITICAL — MUST NOT** fallar la verificación por no tener red. Si el XSD remoto no se puede descargar, la comprobación se **omite** y se deja constancia; **MUST NOT** emitirse ninguna línea JSONL por ello.
- Un error de validación **real** (con red) es **BLOCKING**. **Corrección esperada:** ajustar el elemento rechazado al esquema del documento.
- La estructura semántica de los documentos (los `colspan`, los `nombreCampo`, los `<include href>`) la comprueba §2 bloque **I**, no el XSD.

### 1.6 `estados.puml` — comprobación sintáctica

No es XML. Se comprueba que abre y cierra:

```bash
D=.sdd/drafts/{iniciativa}/design
head -1 "$D/estados.puml" | grep -qx '@startuml' || echo "FAIL: falta @startuml en la primera linea"
grep -qx '@enduml' "$D/estados.puml"              || echo "FAIL: falta @enduml"
```

Cualquier `FAIL` es **BLOCKING**. **Corrección esperada:** añadir la directiva que falte. El contenido (estados, alias, transiciones) lo comprueba §2 bloque **D**.

> **MUST NOT** intentar renderizar el `.puml` ni generar el `.png`: `estados.png` lo produce `GenerateDocs` durante el build, y en el diseño **no debe existir**.

### 1.7 CRITICAL — nota de entorno: el *sandbox* y Gradle

- **MUST NOT** ejecutar `./gradlew`, `./run.sh`, `CreateFilesTask`, `GenerateDocs` ni ninguna tarea de build en esta fase. El diseño es un **plan**: todavía no hay código, no hay nada que compilar y arrancar el servidor bloquearía al verificador. El build es un paso posterior, de `/sdd-implementer`.
- La herramienta `Bash` puede correr en **sandbox**, y el sandbox **impide escribir en `.gradle/` y en `build/`**. Si por cualquier motivo un comando falla con un error de **creación de directorio** —`Cannot create directory '…/.gradle/…'`, `Failed to create parent directory '…/build/…'`— **MUST** reintentarse el **mismo comando** con `dangerouslyDisableSandbox: true` **antes de reportar nada**.
- **MUST NOT** reportarse un fallo de creación de directorio como un fallo del diseño: no lo es, y mandaría al corrector a "arreglar" un diseño que está bien. Si el reintento vuelve a fallar por la misma causa, se reporta como **bloqueo de entorno** en prosa, no como línea JSONL.
- **MUST NOT** cambiarse un comando ni tocarse ningún fichero para sortear el sandbox.

---

## 2. Comprobaciones semánticas

Lista numerada y verificable. Cada comprobación declara **qué se mira**, **qué es fallo** y **cuál es la corrección esperada** (esta última la aplica el corrector). Los identificadores entre paréntesis son las reglas de la superficie normativa del artefacto que la comprobación materializa.

Severidad por defecto: **BLOCKING** para todo lo que impida arrancar, compilar o tramitar, y para toda fuga de la frontera de confianza; **IMPORTANT** para incoherencias entre representaciones; **MINOR** para forma y redacción.

**CRITICAL — iniciativa de MODIFICACIÓN de una versión existente.** Si el `design.md` declara la fila «Modificación de» en su sección «Identidad del trámite y del tipo» (README §4.2 y §5), el diseño es un **delta**: en `design/` solo existen `design.md`, `test-e2e-desc.md`, `test-unit-desc.md` y **exactamente** los ficheros que el delta toca, y las secciones del `design.md` que el delta no cambia dicen `*(sin cambios)*`. Entonces **todas** las comprobaciones de los bloques B-M se reinterpretan así, **antes** de aplicarlas:

- **Ámbito reducido.** Una comprobación solo se aplica a los ficheros y a las secciones **presentes** en `design/`. Que falte un fichero de §5.1 del README, o que una sección diga `*(sin cambios)*`, **NO es fallo**: es lo que el contrato pide. El verificador **MUST NOT** reportar `no existe` / `falta` sobre nada que el delta no toque, y el corrector **MUST NOT** crearlo ni regenerarlo — hacerlo contradice el README §5 y reintroduce el as-is en el diseño.
- **Coherencia contra el as-is.** Donde una comprobación cruza dos representaciones y solo una está en `design/` (p.ej. un `views.xml` tocado frente al `TipoExpedienteInstance.xml` que no lo está), el lado ausente se toma del **fichero real** de la carpeta de versión modificada, que el verificador **puede leer** para esto. Sigue siendo fallo la incoherencia real entre el delta y el as-is.
- **Sí se aplican íntegras**, porque son sobre lo que el delta sí trae: C-A02 (artefactos prohibidos), C-A03 (frontmatter y las 15 secciones), C-A05 (XML completos, sin placeholders), el Bloque K sobre las filas de la tabla §6 y los pasos —**con las tres excepciones de abajo**—, C-L04 (lo inventado), C-L05 (guías) y el Bloque M (prohibiciones transversales).
- **Excepciones del Bloque K en este modo** (`design-contract.md` §8 y §9). Aplicarlas tal cual **antes** de reportar nada; exigir la forma greenfield aquí encadena un bucle verificador↔corrector que nunca converge, porque cada arreglo viola la regla contraria:
  - **C-K03** (`permisos-demo.xml` con fila `Modificar`) solo se exige **si el delta añade perfiles o asignaciones**. Su ausencia cuando no los añade **NO es fallo**; lo que sí es fallo es que la fila esté sin que el delta la necesite.
  - **C-K06** y **C-K08** (paso `CreateFilesTask` en la posición 3) solo se exigen **si el delta añade fases nuevas**. Sin fases nuevas, el paso **MUST NOT** existir y su presencia **es fallo** (`design-contract.md` §9.1): `/sdd-implementer` no genera esa tarea.
  - **C-K09** (orden de los pasos) se comprueba como **orden relativo** sobre las filas de la tabla §6, no contra la tabla de 13 pasos de §9: hay exactamente un paso por fila, renumerados sin huecos, con `./run.sh` el último. Un paso sin fila en §6 es fallo.
- **Fallos específicos de este modo**, ambos **BLOCKING**:
  - Que `design/` contenga un fichero que la tabla §6 del `design.md` **no** lista, o que esa tabla liste un fichero que el delta no necesita. Corrección: borrar el fichero o la fila sobrante.
  - Que «Identidad del trámite y del tipo», «Ficheros a crear o modificar» o «Pasos» digan `*(sin cambios)*` en vez de ir completas (`design-contract.md` §8). Corrección: rellenarlas con los valores reales de la versión modificada (la identidad se lee de su `TipoExpedienteInstance.xml` y de su ruta). Sin ellas, `/sdd-implementer` se queda sin `<Entidad>` y `/sdd-create-tests-e2e` sin carpeta destino.

> El verificador **puede leer** (nunca escribir) estos ficheros reales del árbol, y solo estos: `src/main/resources/data-demo/input/permisos-demo.xml`, `src/main/resources/data-demo/input/usuarios-demo.xml`, `src/main/resources/data-demo/input/centros-demo.xml`, el `TipoTramites.xml` del data-init de expedientes, los `template-views.xml` globales de `tramites/shared/` y el árbol `src/main/java/com/educaflow/tramites/` (para comprobar `Crear` vs `Modificar` y, en una **iniciativa de modificación**, para leer el as-is de la carpeta de versión modificada).
En una **iniciativa de modificación** puede leer además la carpeta de tests **espejo** de esa versión (`src/test/e2e/tramites/…/<vN>/`), para comprobar C-A10 y la numeración de los `T-NNN`.

### Bloque A — Estructura de `design/`

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-A01** | El inventario de `design/` frente a `design-contract.md` §1 | Falta alguno de los ficheros obligatorios (§5.1 del README; en una **iniciativa de modificación** aplica el conjunto reducido de §5 del README, no §5.1), o sobra alguno que la plantilla no declara | Crear el que falta; borrar el que sobra |
| **C-A02** | Que no haya artefactos prohibidos | Existe en `design/` un `.java`, un `.kt`, un `i18n_es.csv`, un `i18n_ca.csv`, un `estados.png`, un `States.java`, un data-init generado, una carpeta `documentospdf/originales/` o un `.pdf` binario | Borrar el fichero. Si era un `.java`/`.kt`, trasladar su contenido a la sección de especificación correspondiente del `design.md` |
| **C-A03** | El frontmatter y las secciones del `design.md` | No lleva `type: design`, o su clave `template:` falta o no coincide con la del `specification.md`, o falta alguna de las 15 secciones de `design-contract.md` §2, o están en otro orden o con otro título | Añadir/corregir el frontmatter; añadir/renombrar/reordenar las secciones exactamente como §2 |
| **C-A04** | Un `fases/<fase>/views.xml` por **cada** fase declarada en el `TipoExpedienteInstance.xml` | Falta el de alguna fase, sobra el de una fase inexistente, o el nombre de la carpeta no es la fase **en minúsculas** | Crear/borrar/renombrar la carpeta y su `views.xml` |
| **C-A05** | Que los XML materializados estén **completos** | Contienen `TODO`, `...`, `FIXME`, un placeholder sin resolver (`<FASE>`, `<Entidad>`…) o un atributo de esqueleto vacío (`<button name="">`) | Resolver el valor real. Un `<button name="">` es además violación de `Y1` |
| **C-A06** | Que `permisos.xml` exista **siempre** (en una **iniciativa de modificación**, solo si el delta añade perfiles o asignaciones) | No existe | Crearlo con el fragmento de `design-contract.md` §14.3 |
| **C-A07** | Que `test-e2e-desc.md` exista y lo haya escrito el diseñador | No existe | Crearlo según `tests-e2e.md` |
| **C-A08** | Que el diseñador **no** haya escrito `test-unit-desc.md` | Existe `test-unit-desc.md` en un diseño que aún no ha pasado por el rol `test-unitarios` | Borrarlo: lo produce el rol `test-unitarios` en una fase posterior |
| **C-A09** | Que los logs del motor se ignoren | Se reporta un problema sobre `log_best.txt`, `log_revision.txt` o `log_revision_unit-test.txt` | No reportarlos: son artefactos del motor, no contenido de diseño |
| **C-A10** | La subsección `### Tests E2E supersedidos` de `## 13. Tests` (**solo** en una iniciativa de modificación; en cualquier otra, que **no** exista) | Existe fuera de una iniciativa de modificación; existe vacía; lista un `.spec.ts` que no está en la carpeta espejo de **esta** versión o que no existe; lista uno sin ID de spec o sin motivo; o el delta invalida un test persistido de esa carpeta que **no** está listado | Borrar la subsección o la línea sobrante; añadir la línea que falta con su ruta, su ID de spec y su motivo (`design-contract.md` §15.3). Ante la duda de si un test cae, **no** listarlo |

### Bloque B — Identidad del trámite y del tipo (T1)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-B01** | `TramiteInstance.xml`: raíz `<Tramite>` y datos como **elementos**, no atributos | Raíz distinta, o `code`/`name`/`tipoTramite` escritos como atributos | Reescribir con los datos como elementos hijos |
| **C-B02** | El `<code>` del trámite | Lleva guiones, underscores o espacios | Pasarlo a `UpperCamelCase` sin separadores; el `snake_case` es solo para la **carpeta** |
| **C-B03** | El `<tipoTramite>` contra el `TipoTramites.xml` real del data-init de expedientes | El valor no existe allí | Sustituirlo por un `code` que exista, o declarar en «Notas y supuestos» que hay que darlo de alta y añadir la fila correspondiente a la tabla de ficheros |
| **C-B04 (T1)** | `<defaultTipoExpediente>` | No es el **nombre de la carpeta** de una versión que el diseño crea (p.ej. lleva la ruta completa, o el `code` del tipo) | Poner el nombre de la carpeta de versión, a secas |
| **C-B05** | El `<help>` | Contiene la secuencia `]]>` dentro del CDATA | Reescribir el texto evitando `]]>`: el generador lanza `RuntimeException` |
| **C-B06** | La tabla «Identidad del trámite y del tipo» del `design.md` | `<Entidad>` no es `<Code><VN>`; el FQN no es `com.educaflow.subsystem.expedientes.db.<Entidad>`; el `basePackageName` no corresponde a la carpeta de versión; el form plantilla no es `exp-<Entidad>-Templates`; o alguna fila de las exigidas falta | Recalcular el valor derivado y corregir la fila |
| **C-B07** | Que el trámite no anide a otro | La carpeta del trámite cuelga de otra carpeta de trámite | Reubicar la carpeta como hermana, bajo un segmento de agrupación si hace falta |
| **C-B08 (CRITICAL)** | La fila `Modificación de` de la tabla «Identidad del trámite y del tipo» (`design-contract.md` §4), contra la línea **Versión** del `specification.md` | La spec declara una **modificación de una versión que ya existe** y la fila **falta**; o la fila está y la spec **no** declara una modificación (primera versión, o versión nueva); o la carpeta de versión que nombra **no existe** en el árbol real | Añadir la fila con la carpeta de versión real, o borrarla. **Es el interruptor de todo el modo delta**: sin ella el diseño se comporta como greenfield (el verificador exige el inventario completo de §5.1 del README y el diseño acaba regenerando encima una versión con expedientes vivos), y con ella de más se dan por conservados ficheros que nadie ha escrito |

### Bloque C — Máquina de estados (S1–S5, A1)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-C01** | `TipoExpedienteInstance.xml`: raíz `<TipoExpediente>` con un **único** `<fases>` | Hay un `<states>` suelto en la raíz (formato anterior a las fases), o más de un `<fases>` | Reescribir con el formato de `design-contract.md` §5.4 |
| **C-C02** | Que no se usen atributos inertes | Aparece `ambitoCreador`, `ambitoResponsable` o `ambitoAuditor` | Borrarlos: hoy no hacen nada y el data-init no los persiste |
| **C-C03 (S4)** | El estado inicial | Hay **cero** o **más de uno** con `initial="true"` en **todo el tipo** (no uno por fase) | Dejar exactamente uno, en la fase que la spec indique como arranque |
| **C-C04** | El atributo `events` de cada `<state>` | Algún `<state>` **omite** `events` | Escribirlo siempre, aunque sea `events=""`: omitirlo equivale en silencio a vacío |
| **C-C05** | `EXIT` | `EXIT` aparece en el `events` de algún estado | Quitarlo del `events`. `EXIT` es un botón puro de UI, interceptado antes del `Tramitador`; declararlo es código muerto |
| **C-C06 (S3)** | El atributo `profile` de cada `<state>` | El valor no es uno de los del enum `Profile`: `CREADOR`, `RESPONSABLE`, `SECRETARIO`, `DIRECTOR`, `AUDITOR` | Sustituirlo por el valor correcto del enum |
| **C-C07 (A1)** | Los nombres derivados de estados y eventos | Algún `onEnter<Estado>`, `trigger<Evento>` o `getForState<Estado>InEvent<Evento>` coincide en **nombre** con un método público de `PhaseEventManager`, `StateEventValidator` o `InitialEventManager` (en particular un estado `STATE` o un evento `INITIAL_EVENT`) | Renombrar el estado o el evento |
| **C-C08 (S2, S3)** | Nomenclatura y unicidad | Un nombre de fase, estado o evento no está en `UPPER_SNAKE_CASE`, o dos estados de la **misma** fase se llaman igual | Renombrar. (Dos estados homónimos en **fases distintas** sí son legales: la identidad es la pareja) |
| **C-C09 (S1, S2, S3)** | El orden de las tablas del `design.md` frente al XML maestro | Las subsecciones de fase, las filas de estado o la lista de `events` no siguen el **orden literal** del XML | Reordenar la tabla para que refleje el XML |
| **C-C10** | La **tabla de transiciones** frente a los `events` declarados | Falta la fila del **arranque** (`[*] → estado inicial`); falta una fila por alguna pareja (estado, evento) declarada; falta la fila `DELETE → [*]` de algún estado que declare `DELETE`; o hay una fila cuya pareja no está declarada | Añadir o quitar la fila. Es la **fuente de verdad** de los `UPDATE_STATE` y del `.puml` |
| **C-C11** | Los eventos que **ramifican** | Un evento con varias filas (mismo origen, distinto destino) no declara la **guarda** de cada rama, o no cubre todos los valores posibles del discriminador, o no declara el `default` como error | Completar las guardas y el `default` |
| **C-C12** | Los eventos que **no** cambian de estado | Una fila con destino = origen sin declararlo explícitamente, o un `trigger*` que calla si llama o no a `UPDATE_STATE` | Declararlo explícitamente (`UPDATE_STATE: ninguno (permanece en <ESTADO>)`) |

### Bloque D — Diagrama `estados.puml` (D1–D3)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-D01 (D1)** | Que exista y esté estructurado por fases | No existe, o los estados no están agrupados en `state <FASE> { … }` (un estado compuesto por fase) | Escribirlo según el esqueleto de `design-contract.md` §5.3 |
| **C-D02 (D3)** | Que **todo** estado del XML maestro aparezca | Falta algún estado, incluidos los `closed` y los que no tienen eventos | Añadir el `state "<ESTADO>" as <FASE>_<ESTADO>` que falte |
| **C-D03** | El **alias** de cada estado | Un alias que no es `<FASE>_<ESTADO>` | Renombrarlo. En PlantUML el identificador es **global**: dos estados homónimos de fases distintas se fundirían en un único nodo |
| **C-D04 (D2)** | Que no haya alias fantasma | Se usa en una transición o en una anotación un alias que no corresponde a ningún estado declarado | Corregir el alias. PlantUML **no da error**: crea un nodo nuevo en silencio |
| **C-D05** | Las **transiciones** frente a la tabla de transiciones | Falta una transición de la tabla, sobra una que no está en la tabla, o la etiqueta no coincide (`<EVENTO>` sin guarda, `<EVENTO>[<campo>=<VALOR>]` con guarda) | Sincronizar el `.puml` con la tabla. **Nada verifica esto en build**: es la única red |
| **C-D06** | El arranque y el borrado | Falta `[*] --> <FASE>_<ESTADO INICIAL>`, o falta `<FASE>_<ESTADO> -> [*] : DELETE` para algún estado que declare `DELETE` | Añadir la transición |
| **C-D07** | Los estados cerrados | Un estado con `closed="true"` no está anotado `<FASE>_<ESTADO> : closed` | Añadir la anotación |

### Bloque E — Modelo (`domains.xml`, M1)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-E01** | El `<module>` | No es exactamente `<module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>` | Restaurarlo: el paquete está **hardcodeado** en los build-tools y en los tests |
| **C-E02 (M1)** | Que la entidad del tipo sea la **primera** `<entity>` del fichero | Hay una entidad auxiliar por delante | Moverla detrás. Es convenio puro, no hay marca que lo señale |
| **C-E03 (M1)** | La declaración de la entidad | No es `<entity name="<Entidad>" extends="Expediente">` | Corregir `name` y/o añadir el `extends`. Sin el `extends`, la inyección del `<extra-code-model>` avisa pero **no detiene el build** |
| **C-E04** | Los campos heredados de `Expediente` | Alguno está **redeclarado** en el `domains.xml` del tipo | Borrar la redeclaración. (Referenciarlos desde las secciones de especificación, el validador o las vistas **sí** es legítimo) |
| **C-E05** | La obligatoriedad en el modelo | Algún campo lleva `required="true"` | Quitarlo y llevar la obligatoriedad al DSL del validador (`+Required()`) en la pareja (estado, evento) donde se pide. El expediente existe en BD desde el estado inicial con esos campos vacíos: un `NOT NULL` lo haría inguardable |
| **C-E06** | El sufijo de los enums propios | Un `<enum>` cuyo `name` no acaba en `<Entidad>` | Renombrarlo. Todos los tipos comparten el paquete `…expedientes.db`: sin sufijo colisionan entre tipos y entre versiones |
| **C-E07** | El bloque de código inyectado | Se usa `<extra-code>` en vez de `<extra-code-model>` | Cambiarlo: el primero inyecta en el `Repository`, el segundo en la entidad |
| **C-E08** | La columna «quién lo rellena» de la tabla de campos | Algún campo la tiene sin resolver, o con un valor distinto de `usuario` / `servidor` | Resolverla. No es documentación: es la **lista de permisos** de escritura del cliente |
| **C-E09** | Los campos `servidor` frente a las acciones descritas | Un campo `servidor` que **ninguna** acción del `triggerInitialEvent` ni de ningún `trigger*` asigna | O sobra el campo, o falta la acción: eliminar uno o añadir la otra |
| **C-E10** | Los campos citados en el validador | Un `field(model::get<Campo>)` referencia un campo que **no existe** en el `domains.xml` ni entre los heredados de `Expediente` | Corregir el nombre del campo, o declararlo en el modelo |
| **C-E11 (CRITICAL, frontera de confianza)** | Los campos `servidor` en el validador | Un campo clasificado como `servidor` —o `codePhase`, `codeState`, `abierto`, `centro`, `usuarioRegistrador`, un `MetaFile` generado por un `trigger*`, un resguardo de registro, un campo calculado— aparece en **algún** `field(...)` | Quitarlo del validador. Darle entrada lo hace **escribible por el cliente**. **Única excepción**: el **campo destino** de una firma en cliente, que lleva `+Required()` + `+FirmaPdf(...)` y **MUST** estar documentado como tal en el `design.md` |
| **C-E12** | Los campos editables de cada vista frente al validador | Un campo editable en el form de un estado que **no** aparece en el `field(...)` del evento que se dispara desde esa vista | Añadirlo al validador. Si no, el usuario escribirá y el valor **se perderá en silencio** |
| **C-E13** | El `<extra-code-model>` frente a `documentospdf/` | Existe el bloque y el tipo **no** genera ningún PDF, o el tipo genera PDFs y el bloque **no** existe | Añadirlo o quitarlo. Es un si y solo si |

### Bloque F — Clases descritas (I1, I2, E0–E5, H1, R1)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-F01 (I1)** | El `InitialEventManagerImpl` | No se describe, se describe más de uno, o se ubica en una **subcarpeta de fase** en vez de en la raíz de la versión | Describir exactamente uno, en `<basePackageName>.InitialEventManagerImpl` |
| **C-F02 (I1, M1)** | Su parametrización | Se declara `InitialEventManager` **en crudo**, o parametrizado con otra entidad | Parametrizarlo con `<Entidad>`: es el **único** sitio donde el tipo declara cuál es su entidad, y lo lee `ExpedienteLocator.getModelClass` en runtime |
| **C-F03 (I2)** | Su método | No hay exactamente un `triggerInitialEvent(<Entidad>, EventContext): void`, o se le pone anotación | Dejar exactamente ese método, sin anotación |
| **C-F04** | Lo que el `Tramitador` ya rellena | El `triggerInitialEvent` reasigna `tipoExpediente`, `centro`, `usuarioRegistrador`, `name` o `numeroExpediente`, o llama a `updateState` | Quitar esas asignaciones: el `Tramitador` las hace antes, y el estado inicial lo fija él después |
| **C-F05 (CRITICAL)** | La precondición de la firma en cliente | El tipo firma algún documento con AutoFirma y el `triggerInitialEvent` **no** deja `dniFirmaDocumentoEntrada` con un DNI válido | Añadir la asignación. `FirmaController` lanza `RuntimeException` si es nulo, vacío o inválido, y **nada lo verifica en build** |
| **C-F06 (CRITICAL)** | La precondición del registro de entrada | Algún `trigger*` llama a `REGISTRO_ENTRADA` y el `triggerInitialEvent` **no** deja `personaSolicitante` y `personaInteresada` no nulos | Añadir las asignaciones. `createRegistroEntrada` lanza **NPE**, y **nada lo verifica en build** |
| **C-F07** | La declaración explícita cuando no aplica | El tipo no firma en cliente ni crea registros de entrada, y la sección **no lo dice** | Añadir la frase que declara que C-F05 y C-F06 no aplican |
| **C-F08 (E0, H1)** | La ubicación de cada `PhaseEventManagerImpl` | El paquete no es `<basePackageName>.<fase>` con la fase **en minúsculas** | Corregirlo. En cualquier otra carpeta es código muerto que aparenta estar vivo |
| **C-F09 (E1)** | Los `trigger<Evento>` de cada fase | Falta el de algún evento de la **unión sin repetir** de los `events` de los estados de esa fase (incluido `DELETE`), o su firma no es `@WhenEvent public void trigger<Evento>(<Entidad>, <Entidad>, EventContext)` con **DOS** parámetros de entidad | Añadir el método o corregir la firma |
| **C-F10 (E2)** | Los `trigger*` sobrantes | Se describe un `trigger<Evento>` cuyo evento **no** es de esa fase | Quitarlo. El trigger de un evento de otra fase va en el manager de **esa otra** fase |
| **C-F11 (E5)** | Métodos prohibidos en un `PhaseEventManagerImpl` | Se describe un `triggerInitialEvent` o un `triggerExit` | Quitarlos: el evento inicial es del tipo, no de una fase, y `EXIT` nunca llega al manager |
| **C-F12 (E3)** | Los `onEnter<Estado>` | Falta el de algún estado de la fase —**incluidos los sin eventos y los `closed`**—, o su firma no es `@OnEnterState public void onEnter<Estado>(<Entidad>, EventContext)` con **UN** parámetro de entidad, o el nombre lleva la fase dentro | Añadir el método o corregir el nombre/firma: el estado va **sin** la fase |
| **C-F13 (E4)** | Los `onEnter*` sobrantes | Se describe un `onEnter<Estado>` de un estado que no es de esa fase | Quitarlo |
| **C-F14** | La lista de acciones de cada `trigger*` | No es una lista **numerada y ordenada** con el vocabulario de `design-contract.md` §11.2, o contiene código real (`if`, `for`, `switch` implementados) | Reescribirla como lista de acciones. El **orden es normativo** |
| **C-F15** | Los `UPDATE_STATE` frente a la tabla de transiciones | El destino de un `UPDATE_STATE` no coincide con la fila correspondiente de la tabla, guarda incluida | Sincronizar con la tabla (que es la fuente de verdad) |
| **C-F16** | `triggerDelete` | Llama a `UPDATE_STATE` | Quitarlo: el expediente se elimina justo después y el `onEnterState` no llega a ejecutarse. Su cuerpo puede quedar vacío |
| **C-F17 (LIMIT)** | Los registros por evento | Un mismo evento declara **dos** `REGISTRO_ENTRADA` o **dos** `REGISTRO_SALIDA` | Dejar uno de cada: el segundo lanza `RuntimeException("Ya existe un registro de entrada definido")` |
| **C-F18** | Los argumentos de los registros | El documento principal puede ser `null`, o un `MetaFile` de la lista de anexos puede no tener `fileName` | Garantizar en la lista de acciones que el documento se ha generado antes y que los anexos son válidos |
| **C-F19 (R1)** | El `import` de `States` | Se referencia el `States` de otro tipo o de otra versión | Usar `<basePackageName>.States` de la **propia** versión. Compila y revienta en runtime con `IllegalArgumentException` |
| **C-F20** | La factorización | Algún `trigger*`, `onEnter*` o método del validador se declara en una **superclase compartida** entre fases o versiones | Declararlo en cada fase, delegando en un helper o servicio. El dispatcher usa `getDeclaredMethods()`: un método heredado **no cuenta** |
| **C-F21** | Las dependencias | Una acción `SERVICIO(...)` o una firma en servidor usa un colaborador que **no** está en la lista de `@Inject` de esa fase, o falta la constante `Rectangulo` de una posición de firma | Declarar la dependencia o la constante en la cabecera de la fase |
| **C-F22** | Separación de responsabilidades | Un `trigger*` **valida datos del usuario**, o comprueba si el evento es disparable desde el estado actual, o usa `System.out` | Mover la validación al validador; quitar la comprobación (ya la hizo el `Tramitador`); usar logger slf4j |
| **C-F23** | Los errores de negocio | Se describe lanzar una excepción que no es `BusinessException` para un error de negocio | Usar `BusinessException`: el `Tramitador` hace `detach` y la propaga como mensaje de usuario |
| **C-F24** | El `switch` sobre el estado actual | Se ramifica sobre el estado actual sin obtenerlo con `States.INSTANCE.getState(codePhase, codeState)`, se incluyen estados de **otra** fase, o falta el `default` | Corregirlo. El `default` es **obligatorio**: `State` no es `sealed` |

### Bloque G — Validador (V1, V2)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-G01 (V1)** | La cobertura por **pareja (estado, evento)** | Falta un `getForState<Estado>InEvent<Evento>` para alguna pareja declarada en la fase, salvo las de `DELETE` | Añadir el método. Sin él, disparar el evento revienta en runtime con `"No se ha encontrado el método: …"` |
| **C-G02 (V1)** | La asimetría estado a estado | Un mismo evento declarado en varios estados se resuelve con **un solo** método de validación | Escribir **un método por estado** (mientras que en el `PhaseEventManagerImpl` es **un único** trigger). Es la asimetría clave |
| **C-G03 (V2)** | Los métodos sobrantes | Hay un método anotado cuya pareja **no** está declarada en la propia fase | Quitarlo |
| **C-G04** | `DELETE` | Se describe un `getForState<Estado>InEventDelete` | Quitarlo: el `Tramitador` se salta la validación para `DELETE` y nunca lo busca |
| **C-G05** | La firma de los métodos | No son `@BeanValidationRulesForStateAndEvent`, con **cero** parámetros, devolviendo `BeanValidationRules` | Corregir la firma |
| **C-G06** | Los métodos sin reglas | Un método que devolvería `null`, o cuya ausencia de reglas no se declara | Declarar explícitamente `rules { }` vacío: el `Tramitador` lanza si recibe `null` |
| **C-G07** | Los argumentos del DSL | Una regla con el argumento en prosa (`«el rango del año»`) en vez del valor literal, o una regla que no existe en el catálogo de `design-contract.md` §12.2 | Escribir el argumento exacto; sustituir la regla inventada por una del catálogo |
| **C-G08** | Los `import` de enums | Un `ifValueIn(...)` usa un enum propio sin declarar su `import` | Declararlo |
| **C-G09** | La tabla de cobertura por fase | Falta, o no cuadra con los métodos descritos (incluida la fila `DELETE → ninguno (exento)`) | Regenerar la tabla a partir de los `events` declarados |
| **C-G10** | Lógica de negocio en el validador | El validador transiciona, genera PDF, registra o asigna campos | Mover esa lógica al `trigger*` correspondiente |

### Bloque H — Vistas (X1–X3, Y1–Y3)

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-H01** | El checklist de `vistas.md` §8 y las reglas duras de `vistas.md` §4 | Cualquier punto incumplido | Aplicar la regla concreta que `vistas.md` declara. En `origen` va su ID (`X1`…`Y3`) |
| **C-H02 (X1)** | El form **genérico** de cada estado | Falta el `<form state="<ESTADO>">` **sin `profile`** de algún estado de la fase, incluidos los `closed` y los sin eventos | Añadirlo, en solo lectura y con botón `EXIT`. Sin él, navegar al estado lanza `"No existe la vista en el expediente"` |
| **C-H03 (X2)** | El form **con perfil** | Falta el `<form state="<ESTADO>" profile="<PERFIL>">` en un estado que tiene `profile` **y** al menos un evento | Añadirlo. Sin él, ese usuario cae en la vista de solo lectura y **el expediente se atasca sin ningún error** |
| **C-H04 (X3)** | Duplicados | Dos forms de la misma fase con el mismo `(state, profile)` | Fusionarlos: producen el mismo nombre de vista y Axelor se queda con la última |
| **C-H05 (Y1)** | El `name` de cada botón | No es un evento declarado en **ese** estado, ni `DELETE`, ni `EXIT`; o es `<button name="">` | Poner el evento correcto |
| **C-H06 (Y2)** | La cobertura de eventos por botón | Un evento declarado en un `<state>` sin botón en **ninguno** de los forms de ese estado (unión genérico + perfil) | Añadir el botón. Si no, el usuario no puede llegar al evento aunque su `trigger*` exista |
| **C-H07 (Y3)** | El `onClick` | No incluye `subsysExpedientes-event-action`, o una cadena `serial:` no **termina** en ella | Corregir el `onClick` |
| **C-H08** | El `state` de cada form | Lleva la fase dentro del atributo, o nombra un estado que **no** es de esa fase | Poner solo el nombre del estado; la fase la deduce el preprocesador del nombre de la carpeta |
| **C-H09** | El `profile` de cada form | No pertenece a la **unión de perfiles del tipo** (los que usa algún estado del XML maestro) | Corregirlo. El build falla con `"El perfil '…' no lo usa ningún estado de …"` |
| **C-H10** | Los paneles referenciados en un `<include-panels>` | Un panel citado que **no existe** ni en el form plantilla del diseño ni entre los globales reales de `tramites/shared/` (que el verificador **MUST** leer) ni es la cabecera `subsysExpedientes-template-header-panel` | Declarar el panel en el form plantilla, o corregir el nombre. Un panel inexistente **hace fallar el build** |
| **C-H11** | Paneles muertos y repetidos | Un panel del form plantilla que **ninguna** fase incluye; o un panel repetido dentro del mismo `<include-panels>` | Borrar el panel muerto; dejar una sola línea por panel (si se repite, gana el flag readonly de la última aparición, **sin aviso**) |
| **C-H12 (CRITICAL)** | Ficheros de vistas vacíos | Algún `views.xml` sin ningún elemento hijo de `<object-views>`, o con todos sus forms comentados | Rellenarlo, o eliminar el fichero entero. **Un `<object-views>` vacío tumba el arranque y el build NO lo detecta**: la aplicación queda sin vistas, sin menús y sin data-init |
| **C-H13** | El form plantilla | Hay cero o más de un `<form name="exp-<Entidad>-Templates">` en el `views.xml` de la raíz; su `<Entidad>` no es la del **propio** tipo; o aparece un `<form state=…>` en la raíz, o el form plantilla dentro de una fase | Corregirlo. El `<Entidad>` equivocado es el error típico al duplicar una versión |
| **C-H14** | La cabecera | Se usa `includeHeader` en vez de `header` | Cambiarlo: el preprocesador **ignora `includeHeader` en silencio** |
| **C-H15** | El footer | La suma de los `colSpan` de los botones de un footer pasa de 12; o un bloque sin botones se omite en vez de escribirse `<buttons-left/>` | Ajustar los `colSpan`; escribir el bloque vacío. Al primer botón de `<buttons-right>` se le asigna siempre `colOffset = 12 − suma`, y si la suma pasa de 12 sale **negativo sin aviso** |
| **C-H16** | El resumen estructural del `design.md` | Falta la tabla `(estado, perfil) → paneles → botones` de alguna fase, o no coincide con el XML materializado | Regenerar el resumen desde el XML |
| **C-H17** | Convenciones que **no** aplican | Se ha aplicado a estas vistas una regla `VAR-` de `agent_docs/view-rules.md`, una convención de `k-vistas`, el patrón `buttons-panel`/`btnSave`/`btnDelete`, una PI `sv-*` o `remote-validation*`; o se ha materializado un `menus.xml` | Quitarlo. Estas vistas están **excluidas** de ese cuerpo de reglas, y un tipo de expediente **no** declara menús propios |
| **C-H18** | La firma en cliente en la vista | Falta alguna de sus **tres** piezas (par de campos en el modelo, `<action-method>` + botón `serial:` en el `views.xml` de **su** fase, `FirmaPdf` en el validador), o la llamada no lleva los **8 argumentos** en orden, o el `method` no va entre comillas simples | Completar la pieza que falte según `vistas.md` §6 |
| **C-H19** | El visor de PDF embebido | El `depends` del `<viewer>` no es el nombre de un campo `MetaFile` **real** de la entidad | Corregirlo. (El `name` del campo dummy sí es libre) |

### Bloque I — Documentos PDF (condicional)

Este bloque se aplica **solo si** el tipo genera al menos un documento. Si no genera ninguno, el fallo es el inverso: que exista la carpeta `documentospdf/` o el `<extra-code-model>` (ver **C-E13**).

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-I01 (CRITICAL)** | El nombre de la carpeta | Es `documentos/` en vez de `documentospdf/` | Renombrarla. `documentos/` renderiza pero **no** se escanea para el enum: el documento queda muerto sin aviso |
| **C-I02** | Los nombres de fichero | No están en `camelCase`, o llevan espacios o guiones | Renombrarlos: se convierten en la constante del enum (`UPPER_SNAKE_CASE`) |
| **C-I03** | Ambigüedad | Conviven un `<doc>.xml` y un `<doc>.pdf` con el mismo nombre base | Dejar uno: el build aborta por ambigüedad |
| **C-I04** | La biyección enum ↔ ficheros | Una constante de `TipoDocumentoPdf` sin fichero en `documentospdf/`, o un `<doc>.xml` sin constante | Añadir o quitar la constante / el fichero, hasta que la correspondencia sea **uno a uno** |
| **C-I05** | Los fragmentos | Un fichero con prefijo `_` tiene constante en el enum, o su raíz no es `<fragmento>` | Quitarle la constante y poner la raíz correcta: un fragmento **no** genera PDF propio |
| **C-I06** | Las rutas de recurso del enum | La ruta de una constante no es la ruta absoluta de recurso del `<doc>.pdf` bajo el paquete de la versión | Corregirla |
| **C-I07** | Las expresiones de los campos | Un `<campo nombreCampo="self.<x>">` (o un `<check nombreCampo="…">`) que referencia un campo **inexistente** en la entidad y en `Expediente` | Corregir la ruta. **Los fallos de evaluación son SILENCIOSOS**: la expresión Groovy revienta, se escribe en el log y el campo queda vacío |
| **C-I08** | La retícula | Los `colspan` de alguna `<fila>` no suman 12 ni un múltiplo de 12, o un elemento cruza el límite de 12 | Reajustar los `colspan` |
| **C-I09** | Los idiomas | `valenciano` o `castellano` aparecen como **atributos** en vez de elementos hijos | Convertirlos en elementos hijos. Omitir `<valenciano>` lo hace traducir al build; ponerlo **vacío** deja solo castellano — no es lo mismo |
| **C-I10** | Los `<include>` | Un `<include href="_<fragmento>.xml"/>` dentro de una `<seccion>`, o apuntando a un fichero que no existe en el diseño | Moverlo a hijo **directo** de `<documento>`/`<fragmento>`; crear el fragmento o corregir el `href` |

### Bloque J — Permisos

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-J01 (REQUIRED)** | El perfil del **estado inicial** | Se asigna por `tipoExpedienteCode` en vez de por `tramiteCode` | Asignarlo por `tramiteCode`. En la creación todavía no hay expediente y el `Tramitador` contrasta el perfil contra los `Ace` **sobre el trámite**: por `tipoExpedienteCode` no se podría crear nada |
| **C-J02** | La cobertura de perfiles | Un perfil que usa algún estado del tipo **no** tiene ningún actor asignado | Asignarlo: un perfil sin actor deja ese estado inalcanzable |
| **C-J03** | El formato del fragmento | `permisos.xml` no tiene raíz `<datos>`, o es una copia del `permisos-demo.xml` completo en vez de solo lo nuevo | Reescribirlo como fragmento a **fusionar** |
| **C-J04** | Los `<perfil>` duplicados | Declara un `<perfil name="…">` que **ya existe** en el `permisos-demo.xml` real (que el verificador **MUST** leer) | Quitarlo del fragmento |
| **C-J05** | El alcance | Contiene asignaciones a otros trámites o a otras versiones | Quitarlas |
| **C-J06** | El `tipoExpedienteCode` | Se usa el `code` del **trámite** donde va el del **tipo** (`<Entidad>` = `<Code><VN>`) | Corregirlo |
| **C-J07** | Los perfiles posteriores | Se usa `tipoExpedienteCode` sin motivo donde valdría `tramiteCode` | **SHOULD** preferirse `tramiteCode`: las asignaciones por `tipoExpedienteCode` hay que duplicarlas en cada versión nueva. Severidad **MINOR** |
| **C-J08 (CRITICAL)** | `auth-expedientes.xml` | El diseño añade una `<permission name="<Entidad>.all">` sin que la especificación la exija, o la añade copiando `create/read/write/remove` **sin `condition`** | Quitarla, o añadir la `condition`. Es un agujero conocido documentado en `CLAUDE.md`; **MUST NOT** introducirse tampoco un `ModelService` deny-all de expedientes como parche |

### Bloque K — Ficheros y pasos

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-K01** | La tabla «Ficheros a crear o modificar» | Falta la fila de algún fichero real que la implementación crea o modifica, o sobra una | Añadir/quitar la fila. Una fila por fichero real, ni una de más ni una de menos |
| **C-K02** | Ficheros generados en la tabla | Aparece un `i18n_*.csv`, un `estados.png`, un `States.java`, un data-init generado o algo bajo `build/` | Quitar la fila: los produce el build |
| **C-K03** | `permisos-demo.xml` (en una **iniciativa de modificación**, solo si el delta añade perfiles o asignaciones) | Falta su fila, o su acción no es **`Modificar`** | Añadirla como `Modificar`: es una **fusión**, no una copia |
| **C-K04** | `Crear` vs `Modificar` | Una fila `Crear` cuyo destino **ya existe** en el árbol real, o una `Modificar` cuyo destino **no existe** (el verificador **MUST** comprobarlo) | Cambiar la acción. Si pasa a `Modificar`, el XML del diseño **MUST** ser el fichero real como base **más** el delta |
| **C-K05** | Las columnas `Skill` y `Descripción` | Vacías, o la `Descripción` de un XML no dice de qué fichero de `design/` se copia, o la de un `.java`/`.kt` no apunta a su sección de especificación | Rellenarlas |
| **C-K06 (CRITICAL)** | El paso de `CreateFilesTask` (en una **iniciativa de modificación**, solo si el delta añade fases nuevas; si no, que **no** exista) | No existe, no está **exactamente** en la posición 3 (después del `TipoExpedienteInstance.xml` completo, antes de rellenar nada), o su comando no es el exacto con `-Ptipo=<ruta de la carpeta de versión>`. En una modificación sin fases nuevas: que el paso exista | Colocarlo en su sitio con el comando exacto; o borrarlo si el delta no añade fases |
| **C-K07** | `-Pfase` | El paso usa `-Pfase` | Quitarlo: acota a una fase y entonces **no** genera los ficheros de la raíz de la versión |
| **C-K08** | Lo que declara el paso 3 | No dice qué crea exactamente (raíz: `domains.xml`, `views.xml`, `InitialEventManagerImpl.java`; por fase: `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt`, `views.xml`), que es idempotente, o que la verificación es una línea `CREADO` por fichero | Completar el paso |
| **C-K09** | El orden de los pasos (en una **iniciativa de modificación**, el orden **relativo** de las filas de la tabla §6: un paso por fila, renumerados sin huecos, `./run.sh` el último) | No sigue el orden obligatorio de `design-contract.md` §9, o hay un paso sin fila en la tabla §6 | Reordenar; borrar el paso sobrante |
| **C-K10** | Los pasos de XML | No dicen «cópialo literalmente» con origen (`design/<x>`) y destino, o no declaran su verificación | Reescribir el paso con esa forma |
| **C-K11** | Los pasos de Java/Kotlin | Duplican la especificación en vez de apuntar a la sección (`## 8. …`, `## 9. …`, `## 10. …`) por su título exacto | Sustituir la copia por la referencia: dos copias divergen |
| **C-K12** | El paso 12 | No dice que `permisos-demo.xml` es una **fusión** que conserva todo lo preexistente | Corregirlo |
| **C-K13** | El paso final | No es el último, no lleva el comando exacto `./run.sh`, o no declara la **comprobación en runtime** de lo que el build no ve (`dniFirmaDocumentoEntrada`, `personaSolicitante`/`personaInteresada`, las transiciones del `.puml`, las expresiones Groovy de los PDFs) | Completarlo |

### Bloque L — Cobertura de la especificación

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-L01** | El «Reparto de reglas» | Una regla funcional de la spec (`VAL-`, `RN-`, `RUI-`, `CC-`) que no aparece con la capa a la que se lleva | Añadir su fila con la capa, o listarla como descartada **con justificación** |
| **C-L02** | La capa asignada a cada regla | Una regla que debe **impedir** algo se lleva solo a la vista (`showIf`/`readonly`), o una obligatoriedad se lleva al `domains.xml` | Reubicarla: si debe impedir, su sitio es el validador. Las reglas de vista son **solo UX, NUNCA defensa** |
| **C-L03** | Los escenarios de la spec | Un escenario sin test en `test-e2e-desc.md` | Añadir el test (ver `tests-e2e.md`) |
| **C-L04** | Lo inventado | El diseño declara una fase, un estado, un evento, un campo, un perfil o un documento que la especificación **no** pide y que no está justificado en «Notas y supuestos» | Quitarlo, o justificarlo como supuesto |
| **C-L05** | Las guías de diseño | Existe `design-guidelines.md` y el diseño incumple alguna de sus indicaciones | Aplicar la guía, con la ubicación de la fuga |
| **C-L06** | «Notas y supuestos» | Una ambigüedad de la spec resuelta por el diseñador sin dejar constancia | Añadir el supuesto, en una línea, con qué se asumió y por qué |

### Bloque M — Prohibiciones transversales

| ID | Qué se mira | Qué es fallo | Corrección esperada |
|---|---|---|---|
| **C-M01** | Código implementado en el `design.md` | Hay cuerpos de método Java/Kotlin reales (un `if`, un `for`, un `switch` implementado dentro de un método) | Sustituirlos por la lista declarativa de acciones o de reglas. **Única excepción**: el DSL del validador, que **sí** se escribe con su sintaxis literal |
| **C-M02** | Escritura fuera de la carpeta | El diseño ha modificado algún fichero del árbol real | Revertirlo y **describir** el cambio dentro del diseño para que lo aplique `/sdd-implementer` |
| **C-M03** | Cambios fuera de la carpeta no documentados | El diseño necesita un cambio en el árbol real (una propiedad de configuración, un dato maestro nuevo, una clase existente a modificar) y no lo describe | Añadir su fila a la tabla de ficheros y su paso |
| **C-M04** | El resumen de vistas | El `design.md` vuelca XML de vistas inline en vez de un resumen estructural | Dejar el XML en su fichero y el resumen en el `design.md` |
| **C-M05** | El checklist | La sección «Checklist del diseñador» del `design.md` no reproduce la lista de `design-contract.md` §17 con sus checkbox, o marca como cumplido algo que no lo está | Regenerar el checklist con el estado real |
