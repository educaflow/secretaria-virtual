# review-contract — developer-model-reviewer

Contrato de revisión que consume el motor `/skill-orquestador-reviewer`. Declara **qué** se revisa; el **cómo** (bucle, tokens, severidades, límites) lo pone el motor.

## Alcance

Entran: **todo** XML bajo `src/main/java/com/educaflow/**` cuyo elemento raíz sea `<domain-models>`. Ese elemento raíz —no la ruta— es el criterio: son la fuente de verdad de la que Axelor genera las entidades JPA.

Formas que adopta hoy en el proyecto, a título orientativo:

- un fichero por entidad en una carpeta `domains/`: `**/domains/<Entidad>.xml` — p.ej. `subsystem/<subsistema>/domains/`, `base/infrastructure/<módulo>/domains/`
- un único `domains.xml` en la raíz del tipo de expediente: `system/tiposexpedientes/<tipo>/domains.xml`
- modelos auxiliares de pantalla fuera de `domains/`: `system/gestioncentro/views-models/*.xml`, `subsystem/expedientes/view_models/*.xml` — pese al nombre de la carpeta **no son vistas**, su raíz es `<domain-models>`

**MUST NOT** restringir el alcance por ruta ni por nombre de carpeta: cualquier paquete puede declarar dominios, y una carpeta llamada `views-models` puede contener modelos. Ante la duda, mira el elemento raíz del fichero.

**MUST NOT** entrar ni corregirse:

- Las clases generadas (`build/src-gen/**`): son un artefacto derivado. Ni el revisor las revisa ni el corrector las toca.
- Código Java/Kotlin escrito a mano → **STOP** y remite a `/developer-code-reviewer`.
- Vistas (`**/views/*.xml`, `menus.xml`) → **STOP** y remite a `/developer-view-reviewer`.
- Cualquier otro artefacto (properties, `data-init`, recursos): no hay revisor para ese tipo de fichero → **STOP** y dilo, sin remitir a ningún skill.

## Skills obligatorios

1. **MUST** `k-sistemas` — su fichero `modelos.md` es el conocimiento normativo de los dominios.
2. **MUST** `k-validaciones` — define qué restricciones (`RES-`) viven en el modelo XML y cuáles pertenecen al servicio o a la vista.
3. **MUST** `k-secure-coding` — el modelo decide qué campos existen y cuáles son del servidor; sus reglas son **BLOCKING** si se violan.
4. Los adicionales que indique el usuario.

## Ejes de revisión

1. **Campos del sistema** — un campo que rellena el servidor **MUST NOT** llevar `required="true"` (REGLA CRÍTICA de `k-sistemas/modelos.md`); comprueba que sigue el patrón estándar de campos del sistema y el checklist de campo nuevo.
2. **Tipo y atributos de cada campo** — que el tipo del elemento corresponda a la naturaleza del dato y no se use `<string>` como sumidero (cantidades, importes, fechas, booleanos, listas cerradas y referencias tienen su tipo propio), y que `large="true"` se reserve a texto realmente largo. Ambas reglas, con su tabla de tipos, sus excepciones (DNI, teléfono, código postal) y sus ejemplos ✅/❌, están en `k-sistemas/modelos.md`.
3. **`<module>`** — su `name` **MUST** coincidir con el final del paquete que contiene el dominio (REGLA CRÍTICA de `k-sistemas/modelos.md`).
4. **Ubicación de las restricciones** — cada `RES-` que pueda expresarse en el modelo (`required`, `unique`, `min`/`max`, `nameColumn`, longitudes) está en el modelo y no duplicada en el servicio, y lo que no puede expresarse ahí **no** se ha intentado forzar en el XML (`k-validaciones`).
5. **Frontera de confianza** — todo campo que el cliente **MUST NOT** poder dictar está identificado como campo servidor, y las entidades multicentro tienen su relación con el centro (`k-secure-coding`).
6. **Relaciones e integridad** — cardinalidades, `mappedBy`, `orphanRemoval`, borrados en cascada y `nameColumn`: que expresen lo que el requisito dice y no dejen huérfanos.
7. **Repositorios y extra code** — si el dominio los declara, que sigan los patrones de `k-sistemas/modelos.md`.

## Pasos obligatorios del revisor

NINGUNO más allá de recorrer los ejes de revisión.

**MUST NOT** validar los dominios contra `domain-models.xsd` en la revisión: el XSD de `axelor-open-platform` es **más estricto que el generador real**, y varios dominios del proyecto lo incumplen mientras generan entidad y compilan sin problema (`primary-key`, `sequence`, `description` en `<item>`, nombres con tilde). Reportar esos incumplimientos llenaría cada revisión de `BLOCKING` falsos, y corregirlos exigiría renombrar campos de entidades existentes, que es un cambio destructivo de esquema. El XSD se usa solo como chequeo de **regresión** del corrector (ver su sección).

## Pasos obligatorios del corrector

**MUST** comprobar por **regresión** contra el XSD cada XML que toques: que tu corrección **no introduce errores nuevos**, no que el fichero valide del todo.

1. Ejecuta el comando **desde la raíz del proyecto** (`secretaria-virtual`), ya que la ruta del esquema es relativa a ella, **antes y después** de tu cambio, y compara los errores:

   ```bash
   xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd <fichero.xml>
   ```

2. Errores **nuevos** respecto al estado previo → los ha introducido tu corrección: **MUST NOT** dejarlos. Corrige hasta volver al conjunto de errores previo, o revierte y repórtalo como `PUSHBACK`.
3. Errores **preexistentes** → **MUST NOT** tocarlos ni reportarlos: el XSD es más estricto que el generador real y 6 dominios del proyecto los tienen mientras compilan. Arreglarlos exigiría renombrar campos de entidades ya existentes, que es un cambio destructivo de esquema (ver `## Clasificación específica`).
4. Si el comando falla porque **no encuentra el esquema**, es un problema de directorio de trabajo, **no** un XML inválido: corrige el directorio y repite. **MUST NOT** emitir `PUSHBACK` por ese caso.

## Puertas

NINGUNA. La regeneración de entidades y la compilación no se ejecutan dentro de este skill: las lanza quien lo invoca, con `./run.sh` (ver `agent_docs/deploy.md`).

## Clasificación específica

**CRITICAL — cambios destructivos de esquema.** Un dominio XML genera el esquema de BD. Un cambio destructivo sobre una entidad ya existente —renombrar o borrar un campo o entidad, cambiar su tipo, o endurecer `required`/`unique` sobre datos ya cargados— puede perder datos o impedir el arranque.

- El revisor **MUST** emitirlo como `UNCLEAR`, nunca con severidad, y la **primera línea del texto** del bloque **MUST** ser exactamente `CAMBIO DE ESQUEMA: SI`; en los demás `UNCLEAR`, exactamente `CAMBIO DE ESQUEMA: NO`. El literal se compara carácter a carácter:
  - ✅ CORRECTO: `CAMBIO DE ESQUEMA: SI`
  - ✅ CORRECTO: `CAMBIO DE ESQUEMA: NO`
  - ❌ INCORRECTO: `CAMBIO DE ESQUEMA: SÍ` (con tilde: no casa, y el aviso de pérdida de datos no llega al informe)
  - ❌ INCORRECTO: `Cambio de esquema: si` (minúsculas: no casa)
  - ❌ INCORRECTO: omitir la línea en un `UNCLEAR` que no es de esquema (el informe no puede distinguirlos)
- El revisor **MUST NOT** pasárselo al corrector.
- Red de seguridad: si aun así le llega uno, el corrector **MUST NOT** aplicarlo → lo devuelve como `PUSHBACK` indicando que es un fallo de clasificación del revisor.
- Añadir un campo nuevo **opcional** no es destructivo: eso se corrige con normalidad.

Además:

- Toda violación de `k-secure-coding` es **BLOCKING**.
- Un error **nuevo** contra `domain-models.xsd` introducido por una corrección es **BLOCKING**; uno **preexistente** no se reporta (ver `## Pasos obligatorios del revisor`).

## Informe

Destaca aparte los bloques `UNCLEAR` con `CAMBIO DE ESQUEMA: SI` y su impacto en los datos existentes. Recuerda que los dominios modificados requieren regenerar las entidades al compilar (ver `agent_docs/deploy.md`).
