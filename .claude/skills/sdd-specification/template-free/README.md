# Guía de los ficheros de la especificación de una feature libre («free»)

Explica qué debe contener la especificación de una **feature libre**: cualquier funcionalidad que **no encaja en ninguna otra plantilla** del skill porque toca varios sistemas a la vez, es transversal (seguridad, i18n, infraestructura, build, configuración…), vive en la carpeta `base` o simplemente no es ni un sistema/subsistema ni un tipo de expediente.
Esta plantilla existe para que esas features **también pasen por todas las salvaguardas del pipeline SDD** (spec conversada, diseño criticado, implementación verificada, tests E2E depurados y persistidos) en vez de hacerse a mano fuera de él.

**CRITICAL — es la plantilla de último recurso**: se elige solo tras descartar las demás (paso 1 de «Exploración del contexto»), porque aquí el modelo tiene **libertad** para decidir forma, estructura y arquitectura, a cambio de **explicitar y justificar** sus decisiones.

Es **el único fichero de esta carpeta de plantillas que el skill `sdd-specification` conoce por nombre**: el skill lee este `README.md` y, a través de él, descubre y usa el resto.
Esta guía dirige la redacción y la revisión de la spec; **MUST NOT** copiarse al output ningún bloque explicativo de esta guía, de los catálogos ni del ejemplo.

## Ficheros de esta carpeta de plantillas

| Fichero | Qué es | Cómo se usa |
|---|---|---|
| `README.md` | **Esta guía**: el conjunto de ficheros, los apartados, la clasificación de los elementos y las reglas de numeración. | Única referencia que el skill conoce por nombre; dirige las preguntas de la Fase 2 y las validaciones de la Fase 3. |
| `specification.md` | **La plantilla del índice.** | Se reproduce **literalmente**, sustituyendo los placeholders por contenido real. Produce **un** fichero índice (el único con frontmatter `type: specification`). |
| `anexo.md` | **La plantilla de los anexos** (ficheros secundarios **opcionales**). | Se instancia solo si un apartado del índice crece tanto que estorba leerlo; una por `anexo-<slug>.md`. Cero anexos es lo normal. |
| `catalogos/` | **Catálogos de referencia**, uno por barrido: `catalogo-historias-escenarios.md` (cobertura de `HU-`/`ESC-`), `catalogo-pasos-escenario.md` (granularidad y autosuficiencia de los pasos), `catalogo-requisitos.md` (requisitos `REQ-` que se olvidan en una feature transversal). | Se consultan al rellenar cada apartado y son la referencia de los **barridos de completitud** (ver esa sección al final). |
| `example/` | **Un ejemplo completo** de spec terminada e instanciada con esta plantilla. | Referencia del aspecto final. |

## Exploración del contexto

**Esta sección la ejecuta el propio skill `sdd-specification` en su Fase 1** (no los subagentes), antes de preguntar o revisar:

1. **Confirma que `free` es la plantilla correcta.** Lee el título y primer párrafo del `README.md` de **cada** otra carpeta `template-*/` del skill y pregúntate si la petición cabe en alguna.
   Si cabe, **MUST** decírselo al usuario y proponerle esa plantilla antes de seguir; solo se continúa con `free` si lo confirma.
   - ✅ CORRECTO: «Un banner de aviso visible en todas las pantallas» → toca el layout común y la gestión de centro → ninguna plantilla concreta → `free`.
   - ❌ INCORRECTO: «Un mantenimiento de salas con su listado y formulario» con `free` → es un subsistema de libro: propón la plantilla de sistema.
2. **Lee la arquitectura general** en `agent_docs/architecture.md` y lista las carpetas de primer nivel del código (no de memoria):
   ```bash
   ls src/main/java/com/educaflow/ src/main/java/com/educaflow/base/ src/main/java/com/educaflow/subsystem/ src/main/java/com/educaflow/system/
   ```
   Si la petición menciona algo concreto (un sistema, una pantalla, una utilidad), lee esa parte del código antes de preguntar.
3. **Carga solo los skills de conocimiento que la feature toque de verdad** (p. ej. `k-sistemas` si toca servicios, `k-vistas` si toca vistas, `k-secure-coding` si toca entidades o permisos, `k-i18n` si toca textos, `k-scheduler` si toca tareas programadas).
   **MUST NOT** cargarlos todos por si acaso: la exploración sirve para preguntar mejor, no para diseñar.
4. **Comprueba si la solicitud es divisible**: si agrupa varias cosas independientes (desplegables por separado), propón dividirla en specs separadas, cada una con la plantilla que le corresponda (una parte puede ser un subsistema y otra `free`).
5. **Anota las pistas técnicas** que surjan en la conversación para `design-guidelines.md`: en una feature libre son frecuentes («reutilizad el filtro X», «esto va en `base`», «no toquéis el tramitador») y es justo ahí, no en la spec, donde deben ir.

## Ficheros que produce la especificación

| Fichero | Plantilla | Qué contiene |
|---|---|---|
| `specification.md` | `specification.md` | El **índice y, normalmente, toda la spec**: objetivo, contexto, actores, historias de usuario con sus escenarios, requisitos, seguridad, recursos y fuera de alcance. Único con frontmatter `type: specification`. |
| `anexo-<slug>.md` | `anexo.md` | **Opcional, cero o más.** Un anexo por cada apartado del índice que se haya desbordado (p. ej. una tabla de 40 requisitos, o el detalle de una pantalla compleja). El índice conserva un resumen y enlaza al anexo. |

- La spec libre es, por defecto, **un único fichero**.
  Los anexos son una válvula para no hacer ilegible el índice, no una estructura obligatoria: **MUST NOT** crearse un anexo que quepa cómodamente en el índice.
- `<slug>` va en **kebab-case** y describe el apartado que desarrolla (p. ej. `anexo-requisitos-exportacion.md`).
- Solo `specification.md` lleva frontmatter. Los anexos empiezan directamente por su título `# Anexo: <Nombre>`.
- **Correspondencia índice ↔ anexos**: cada anexo **MUST** estar enlazado desde el apartado del índice que desarrolla, y cada enlace a un anexo **MUST** tener su fichero.

---

## Reglas transversales

### Identificadores numerados

Historias, escenarios y requisitos llevan IDs estables para que el diseño pueda comprobar que **ninguno se pierde**: cada pieza del diseño declara de qué IDs de la spec proviene, y cada test E2E de qué escenario.

| Elemento | Formato | Ámbito de numeración | Dónde vive |
|---|---|---|---|
| Historias de usuario | `HU-NNN` | Global a la spec | `specification.md` |
| Escenarios | `ESC-NNN` | Global a la spec (no por historia) | `specification.md` |
| Requisitos | `REQ-NNN` | Global a la spec | `specification.md` (o su anexo) |

- Numeración desde `001`, **tres dígitos**, sin huecos al crear.
- **Los IDs no se renumeran nunca.** Al borrar un elemento su número se conserva como hueco, para no romper la trazabilidad con un diseño ya generado.
- **No hay otra taxonomía.** Esta plantilla no clasifica los requisitos en familias técnicas (restricción, validación, regla de negocio, regla de UI, campo calculado): esa conversión es trabajo del diseño.
  Un requisito puede llevar, como atributo opcional, una **etiqueta** libre de negocio que ayude a agruparlos (`comportamiento`, `seguridad`, `rendimiento`, `textos`, `compatibilidad`…), pero el ID es siempre `REQ-NNN`.
- ✅ CORRECTO: `HU-002`, `ESC-007`, `REQ-013`
- ❌ INCORRECTO: `REQ-13` (sin tres dígitos), `VAL-001` / `RN-001` / `RUI-001` (taxonomía de otra plantilla), `REQ-Banner-001` (los requisitos no llevan ámbito), `ESC_001` (guión bajo), `HU-001-ESC-001` (el escenario no anida el ID de la historia).

### Lenguaje de negocio

¿Lo entendería un supervisor del centro sin formación técnica? Si **no**, no va en la spec.
Aplica a todos los ficheros: ni el índice ni los anexos llevan tipos de dato, FQN, JPQL, atributos XML, nombres de método ni rutas de código.

**CRITICAL — en una feature libre la tentación de ser técnico es mayor**, porque a menudo la petición nace de un problema técnico («el filtro de centro está duplicado en cuatro sitios»).
La spec **MUST** reformularlo como necesidad de negocio («cada usuario ve solo lo de sus centros en todas las pantallas, sin excepciones») y las pistas técnicas van a `design-guidelines.md`.
(Única excepción: ver Requisitos.)

### Lo obvio pesa tanto como lo complicado

**CRITICAL — al redactar y al revisar, presta a lo trivial la MISMA atención que a lo sutil.**
El mayor riesgo de una spec no son los requisitos difíciles sino los **obvios**: se dan por sobreentendidos y nadie los escribe.
En una feature transversal los más olvidados son: qué pasa en el **resto de pantallas** que no se tocan, qué ve un usuario **sin permiso**, qué pasa con los **datos que ya existen**, y qué ocurre si la feature **falla** (un servicio externo caído, un fichero ausente).

- **MUST** declarar un requisito aunque sea evidente.
- **MUST NOT** saltarte una candidata trivial por obvia mientras resuelves con detalle las complejas.

### Especificar una modificación de algo existente

Lo normal en una feature libre es que **modifique** partes ya implementadas.
Se declara en «Contexto y alcance» nombrando, **en lenguaje de negocio**, qué partes de la aplicación cambian (p. ej. «la pantalla de correos del centro», «el inicio de sesión», «todas las pantallas con listado»).

- **Delta + conservación por defecto.** La spec declara **solo** lo nuevo o cambiado; todo lo no mencionado de las partes afectadas **MUST** conservarse tal cual.
  **MUST NOT** copiar en la spec el comportamiento actual que no cambia: el código es la fuente de verdad del as-is (el diseñador lo lee de `src/main/...`).
- **IDs locales a la iniciativa.** La numeración empieza en `001` y es **local** a esta spec: **MUST NOT** referenciar IDs de iniciativas archivadas.
- **Escenarios de no-regresión.** Por cada parte existente que la feature roce, **MUST** haber al menos un `ESC-` que compruebe que lo que ya funcionaba sigue funcionando (ver el barrido `historias-escenarios`).

---

## El índice — `specification.md`

### Objetivo

**Qué va:** una frase con lo que tiene que hacer la feature y para quién.

**Qué NO va:** rutas de código, nombres de paquete, cómo se implementa.

### Contexto y alcance

**Qué va:**

- **Por qué es una feature libre**: una o dos frases que expliquen por qué no es un sistema/subsistema ni un tipo de expediente (toca varias partes, es transversal, es de la base común…).
  Es la justificación de haber elegido esta plantilla y el diseñador la usa para decidir dónde vive el código.
- **Partes de la aplicación afectadas**, en lenguaje de negocio, una por viñeta: qué cambia en cada una (el delta).
  Si una parte **no** cambia pero el usuario podría pensar que sí, dilo aquí explícitamente.
- **Dependencias funcionales**: de qué funcionalidades existentes depende la feature (en lenguaje de negocio).

**Qué NO va:** nombres de clase, de paquete ni de carpeta; qué fichero se toca.

### Actores

Los actores que intervienen: quiénes son y qué papel juegan.
**MUST** ser tipos de usuario o cargos de `CLAUDE.md` (o «el sistema» para lo automático).
Si la feature afecta a **todos** los usuarios por igual, dilo así en vez de enumerarlos.

### Historias de usuario

- Cada historia es un encabezado `## HU-NNN — Como [Actor] quiero [feature] para [motivo]` y, **debajo de cada historia, van sus escenarios** `ESC-NNN`.
- No hay un apartado de escenarios aparte.
- Cada historia tiene **al menos un escenario** y cada escenario pertenece a exactamente una historia.
- Los escenarios deben cubrir el camino feliz, los alternativos, los errores y la **no-regresión** de lo existente.

**CRITICAL — claridad, especificidad y explicitud.**
Cada escenario nombra los datos concretos que se usan, el valor exacto que se introduce en cada campo, la acción precisa que dispara cada paso y la respuesta literal del sistema.
Quien lo lea para construir el test E2E debe poder reproducir cada paso sin adivinar nada.

**CRITICAL — formato y autosuficiencia.**
Cada `ESC-NNN` se convierte en el diseño en un test E2E que se ejecuta contra una aplicación recién arrancada, **sin estado previo**.
Por eso **cada escenario se escribe SIEMPRE como una lista de pasos numerados** (un paso por línea):

1. Empieza con el actor **iniciando sesión** en la aplicación.
2. Sigue con la **preparación**: el actor (u otro que también inicia sesión dentro del escenario) crea todos los datos que la prueba necesita.
   El único estado previo admisible es el descrito en «Recursos y datos iniciales».
3. Realiza la **acción que se prueba**.
4. Termina con la **respuesta del sistema**.

Entre medias puede haber **ramas condicionales** (*«si <condición> el sistema hace X; si no, hace Y»*); un escenario con ramas puede dar lugar a más de un test.

**CRITICAL — usuarios y centros reales de los datos de demo.**

- Cuando un escenario nombre un usuario o un centro, **MUST** usar los de `src/main/resources/data-demo/input/` (`centros-demo.xml` y `usuarios-demo.xml`), leídos antes de redactar: de ahí salen los centros y las cuentas de cada tipo de usuario y cargo.
- **MUST NOT** inventar centros, cuentas, logins ni DNI.
- Única excepción: el administrador global `admin` / `admin`.

**Escenarios no automatizables.**
Si un escenario exige la intervención de una persona fuera del navegador (firmar con AutoFirma, leer un correo real, un dispositivo físico…), **MUST** marcarse añadiendo al final de su nombre corto ` *(manual: <motivo>)*`.
Es la única forma de que el pipeline no intente automatizarlo; **MUST NOT** marcarse así un escenario solo porque sea laborioso.

**Formato:**

```
## HU-001 — Como [Actor] quiero [feature] para [motivo]

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión.>
  2. <Prepara los datos que necesita la prueba.>
  3. <Realiza la acción que se prueba.>
  4. <El sistema responde.>
- ESC-002 — <Nombre corto> *(manual: <motivo>)*:
  1. …
```

- ✅ CORRECTO (simple, pero en pasos numerados):
  ```
  - ESC-004 — El aviso no se muestra si está desactivado:
    1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
    2. Abre la pantalla de avisos globales y desmarca «Activo» del aviso «Mantenimiento».
    3. Pulsa «Guardar» y cierra sesión.
    4. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
    5. El sistema no muestra ningún banner de aviso en la pantalla de inicio.
  ```
- ❌ INCORRECTO (varias frases en una línea): *«ESC-004 — el admin desactiva el aviso; el supervisor entra; no ve el banner.»*
- ❌ INCORRECTO (presupone estado): un `ESC` cuyo primer paso es *«El supervisor abre la pantalla y no ve el aviso»* sin que nadie lo haya creado ni desactivado dentro del escenario.

**Qué NO va:** nombres de clase, pantallas técnicas, capas; en los escenarios, nombres técnicos de botón/campo/método, comandos de testing y pasos Given/When/Then.

### Requisitos

**Qué son:** las condiciones concretas y comprobables que la feature debe cumplir y que las historias por sí solas no fijan: reglas, límites, mensajes, comportamiento ante error, alcance por centro, textos, compatibilidad con lo existente.
Es el apartado que sustituye a las familias de reglas de las otras plantillas, sin clasificarlas técnicamente.

**Formato:** una lista, un `REQ-NNN` por viñeta, con el texto del requisito en una frase afirmativa y comprobable y, como **sub-viñetas opcionales**, sus atributos `key: valor`:

- `etiqueta`: una palabra de negocio para agrupar (`comportamiento`, `seguridad`, `rendimiento`, `textos`, `compatibilidad`, `datos existentes`…).
- `condición`: cuándo aplica, si no es siempre.
- `mensaje`: el texto literal que ve el usuario, si el requisito lo produce.
- `actor`: a qué rol afecta, si no es a todos.

**REQUIRED — identificación:** recorre `catalogos/catalogo-requisitos.md` apartado a apartado.
Es una ayuda **no exhaustiva**: declara igualmente lo que el negocio necesite aunque no figure.

**Reglas de redacción:**

- Cada requisito es **una** condición; si combina varias, sepáralas.
- El texto es **lo que debe cumplirse**, no cómo se consigue.
- **MUST** incluir los requisitos de **datos existentes** (qué pasa con lo que ya hay en la aplicación cuando se despliega la feature) y de **fallo** (qué ve el usuario si algo falla), que son los que más se olvidan.
- Si la feature decide qué propiedades puede enviar la interfaz en alguna acción, el requisito nombra `AllowProperties` en lenguaje de negocio (la única excepción técnica).

**Ejemplo:**

```
- REQ-001 — Solo el administrador puede crear, modificar o desactivar un aviso global.
  - etiqueta: seguridad
- REQ-002 — Un aviso activo se muestra a todos los usuarios de todos los centros en cuanto inician sesión, en todas las pantallas.
  - etiqueta: comportamiento
- REQ-003 — Si el texto del aviso está vacío al guardar, el sistema no guarda y avisa.
  - mensaje: «El texto del aviso es obligatorio»
- REQ-004 — Los usuarios que ya tenían sesión iniciada ven el aviso nuevo al cambiar de pantalla, sin tener que volver a entrar.
  - etiqueta: datos existentes
```

- ❌ INCORRECTO: `REQ-005 — El aviso se guarda y se muestra a todos los usuarios` (dos condiciones en un requisito: sepáralas)
- ❌ INCORRECTO: `REQ-006 — El servicio filtra la consulta por el centro del usuario` (dice cómo, no qué; y es técnico)

### Seguridad

Quién puede ver/hacer cada cosa, en lenguaje natural:

- **Declarar solo los roles que tienen algún acceso** (tipos de usuario y cargos de `CLAUDE.md`). La seguridad es **deny by default**: un rol no declarado no tiene acceso y **no se lista** como «sin acceso».
- Por cada rol con acceso, **qué puede hacer** y su **alcance por centro** (solo su centro, todos los centros, solo sus propios registros).
- Si la feature afecta a todos los usuarios por igual (p. ej. algo que ven todos al iniciar sesión), dilo así y declara aparte quién la **administra**.

### Recursos y datos iniciales

Recursos estáticos (plantillas, certificados, ficheros de configuración…) y datos que deben precargarse al arrancar.
Si no hay, `*(no aplica)*`.
Es el **único estado previo** que los escenarios pueden presuponer.

### Fuera de alcance

Lo que el negocio decide **no** hacer.
En una feature transversal es especialmente importante: acota qué partes de la aplicación **no** se tocan aunque podrían parecer relacionadas.

---

## Los anexos — `anexo-<slug>.md`

Cuándo se crea, su nombre y su correspondencia con el índice: «Ficheros que produce la especificación».

- El primer párrafo del anexo dice qué apartado del índice desarrolla.
- El apartado del índice conserva un **resumen** y un enlace `Ver [anexo-<slug>.md](./anexo-<slug>.md)`.
- ✅ CORRECTO: 35 requisitos de una exportación → el índice resume en tres viñetas y enlaza `anexo-requisitos-exportacion.md`, donde están los 35 `REQ-`.
- ❌ INCORRECTO: un anexo con 4 requisitos (caben en el índice); un anexo que repite lo que ya dice el índice; un anexo que no está enlazado.

---

## Barridos de completitud (subagentes)

Tras (re)generar el borrador de la spec, el skill lanza **subagentes de barrido** que buscan **candidatas que falten**, cada uno con su catálogo.
Esta tabla **declara** los barridos de esta plantilla; el skill es agnóstico y lanza los que aquí figuren.

Los barridos se ejecutan por **etapas en orden**; dentro de una misma etapa, todos en paralelo:

- **Etapa A — cobertura**: primero, porque una HU o un ESC aceptados dan material nuevo al resto.
- **Etapa B — calidad y requisitos**: sobre la spec ya completada con lo aceptado en A.

| Etapa | Barrido | Una instancia por cada… | Sobre qué piensa (iteración interna del subagente) | Catálogo | Contrato de salida |
|---|---|---|---|---|---|
| A | **historias-escenarios** | toda la spec (**instancia única**) | - Cruza Actores, «Contexto y alcance» (cada parte afectada), Requisitos y Seguridad contra las HU/ESC existentes: qué declarado no lo ejercita ningún escenario.<br>- **MUST** proponer al menos un `ESC-` de **no-regresión** por cada parte existente que la feature roce.<br>- Propone la HU o el ESC que falta **con sus pasos redactados** (numerados, concretos, autosuficientes, con usuarios/centros de demo). | `catalogos/catalogo-historias-escenarios.md` | historias-escenarios |
| B | **pasos-escenarios** | historia de usuario (`HU-NNN`) del índice | ESC a ESC de su historia y, dentro de cada uno, paso a paso:<br>1. **granularidad**;<br>2. **pasos que faltan** (inicio de sesión, preparación, valores concretos, pulsación, respuesta literal);<br>3. **autosuficiencia**.<br>Propone los pasos concretos que sustituyen o se insertan. | `catalogos/catalogo-pasos-escenario.md` | pasos-escenarios |
| B | **requisitos** | toda la spec (**instancia única**) | - Recorre el catálogo apartado a apartado (seguridad y alcance por centro, datos existentes, fallo y degradación, textos e idiomas, resto de pantallas no tocadas, administración de la feature, límites).<br>- Lo cruza con «Contexto y alcance» y las HU/ESC: qué condición necesaria para que la feature funcione no está escrita. | `catalogos/catalogo-requisitos.md` | reglas |

Reglas de los barridos:

- Cada subagente **propone candidatas, no escribe la spec**: solo entran cuando el usuario las acepta en la conversación.
- Cada subagente **MUST** leer toda la spec y **MUST NOT** proponer una candidata que duplique algo existente.
- El catálogo es **solo una guía no exhaustiva**: el subagente **puede y debe** proponer también candidatas fuera de él (indicando `(fuera de catálogo)`).
- Aplica «Lo obvio pesa tanto como lo complicado» (Reglas transversales): **MUST** proponer también las candidatas obvias.
- Toda candidata va en **lenguaje de negocio**.
- Una candidata **debe deducirse de lo que la spec ya cuenta**: el subagente **MUST NOT** inventar funcionalidad nueva.
- **Barrido `requisitos` y el contrato `reglas`:** el campo `tipo` del JSONL (`RES`/`VAL`/`RN`/`CC`/`RUI`) es **solo orientativo** sobre el efecto de la candidata (bloquea / actúa / calcula / muestra); al incorporarla, el requisito toma un ID `REQ-NNN` y, si ayuda, el efecto se recoge en su `etiqueta`. **MUST NOT** aparecer en la spec ningún ID con esos prefijos.
- La columna **«Contrato de salida»** dice cuál de los contratos JSONL del skill usa cada barrido. El motor **MUST** leerla en vez de deducir el contrato del nombre del barrido.
