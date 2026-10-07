# Contrato de materialización — de una tarea al árbol del proyecto (feature libre)

Lo lee el **implementador** (README §2.2) y lo consulta el **corrector-build** (README §2.4). Define **cómo materializar UNA tarea** según su naturaleza.

> Si la tarea es de **tests unitarios**, su materialización la define `tests-code.md`.

---

## 1. Principio: los XML del diseño se copian literalmente, no se regeneran

Si el diseño materializó XML (`design/domains/`, `design/views/`, `design/menus.xml`, `design/data-init/`), son la **fuente de verdad** (el verificador del diseño ya los validó). **MUST** copiarlos tal cual; **MUST NOT** reescribirlos desde el `design.md` ni reformatearlos. Si al copiar detectas un XML **mal**: errata pequeña → `DESIGN-ERRATA` (§5); si no → `DESIGN-ERROR`.

---

## 2. Principio: el Java/Kotlin se delega en `developer-code-implementer`

El implementador **MUST NOT** escribir código Java/Kotlin a mano. **MUST**, en este orden:

1. Lee `## Skills a usar` de la tarea y **carga cada skill con `Skill`** antes de nada.
2. **Invoca `developer-code-implementer`** con el `<texto del prompt>` de la tarea **verbatim**, añadiéndole:
   - **Ubicación**: la carpeta exacta que la tarea trae de `**Ubicación del código:**` / «Dónde vive el código y por qué». **MUST NOT** moverla «porque encaja mejor en otro sitio»: si cree que la ubicación está mal → `DESIGN-ERROR`.
   - **Superficie cerrada** (§2.1): solo los ficheros y miembros públicos que la tarea lista.
   - Si la tarea tiene ficheros `Acción: Modificar`: **editar** lo existente añadiendo/cambiando **solo** el delta declarado y **conservando** todo lo demás; lo preexistente no es «superficie de más».
   - Los XML ya colocados son **contrato fijo**: firmas Java ↔ acciones de las vistas, entidades ↔ dominios. XML mal → `DESIGN-ERRATA` si errata pequeña, si no `DESIGN-ERROR`; nunca editarlo.
   - Recuérdale que aplican `CLAUDE.md` y los skills cargados; la ubicación del código la fijó el diseño.
   - **Parar y reportar** ante cualquier bloqueo. **MUST NOT** adivinar.

### 2.1 Superficie cerrada

**CRITICAL**: la tarea define la **superficie exacta**. **MUST NOT** crear clases, métodos públicos, acciones ni endpoints que no liste; **MUST NOT** renombrar ni cambiar firmas (sobre todo las que el **test** de la pareja TDD ya usa); **MUST NOT** clonar patrones de otras piezas «por coherencia». Si «haría falta» algo más → `BLOCKED: {tarea} — superficie insuficiente: {qué y por qué}`.

---

## 3. Regla TDD — la tarea de pieza se cierra con su test en verde

Si la tarea es de **pieza** y su `<texto del prompt>` nombra la **tarea de test** que la precede (fase red ya materializada), el implementador **MUST**, tras `developer-code-implementer`:

1. Ejecutar **solo** ese test:
   ```bash
   ./gradlew test --tests '<FQN de la clase de test>'
   ```
   (patrón por FQN; `agent_docs/deploy.md`). **MUST NOT** usar `clean build` aquí (eso es del verificador-build) ni arrancar la app.
2. Si el test **pasa** → `DONE`.
3. Si **falla** por la **producción** (la pieza no hace lo que la descripción manda) → vuelve a 2 de §2 con el fallo concreto, **LIMIT 3 intentos**; si sigue en rojo → `BLOCKED: {tarea} — el test <Clase>Test sigue en rojo: {fallo}`.
4. Si falla porque el **test no cuadra con el diseño** (usa una firma o un mensaje que el diseño no declara) → **MUST NOT** adaptar el test ni la pieza: `DESIGN-ERRATA` si es una errata pequeña, si no `BLOCKED` explicando la divergencia. Un test que no refleja `test-unit-desc.md` es un fallo de la tarea de test, no algo que tapar aquí.
5. Si el test no compila porque falta **otra** pieza que viene en una tarea posterior (dependencia cruzada): anótalo en el resumen y devuelve `DONE`; el verificador-build lo cerrará al final.

- ✅ CORRECTO: `DONE: task_04.md` — «`AvisoGlobalServiceImplTest` 6/6 en verde».
- ❌ INCORRECTO: `DONE` sin haber ejecutado el test; cambiar el mensaje del test para que coincida con el que escribió el implementador (el mensaje lo fija la spec).

---

## 4. Materializar según la naturaleza de la tarea

- **XML materializado** (dominio, vista, data-init): `mkdir -p` + `cp` literal al destino (§1). Según `Acción`:
  - `Crear` + no existe → copiar. `Crear` + existe → `CONFLICT: {tarea} — ya existe {ruta}`.
  - `Modificar` + existe → **comprobación de conservación**: todo elemento con nombre del fichero real (campo, enum, finder, panel, botón, acción) está en el XML del diseño salvo los de `## 8. Eliminaciones declaradas` del `design.md`. Pasa → sobrescribir. Falla → `CONFLICT: {tarea} — el XML del diseño perdería elementos preexistentes no declarados: {lista}`. **MUST NOT** fusionar a mano.
  - `Modificar` + no existe → `BLOCKED: {tarea} — la base que el diseño asume no existe: {ruta}`.
- **`menus.xml`**: insertar los `<menuitem>` de `design/menus.xml` en `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` antes de `</object-views>`; `name` repetido → `CONFLICT`. Validar:
  ```bash
  xmllint --noout --schema ../axelor-open-platform/axelor-core/src/main/resources/object-views.xsd \
    src/main/java/com/educaflow/secretariavirtual/menus/menus.xml
  ```
- **Java/Kotlin** (cualquier carpeta: `base`, `system`, `subsystem`, `tramites/util`…): §2, y §3 si tiene test.
- **Recursos y configuración** (`axelor-config.properties`, plantillas, ficheros estáticos): editar/crear exactamente lo que la tarea dice; una propiedad nueva va en `axelor-config.properties` con su comentario; un **secreto** nunca va en un fichero versionado (→ `BLOCKED` indicando que debe ir al fichero privado).
- **Tests unitarios**: `tests-code.md`.

**MUST NOT** pasar a `developer-code-implementer` otros `design.md` como referencia.

---

## 5. Detenerse y reportar ante un bloqueo

**MUST NOT** adivinar. Tokens (primera línea + 1-2 líneas de resumen):

- `CONFLICT: {tarea} — {qué destino ya existe o qué se perdería}` — sobrescritura (lo decide el usuario).
- `BLOCKED: {tarea} — {motivo}` — bloqueo del **entorno** o test en rojo irreducible (§3).
- `DESIGN-ERROR: {tarea} — {motivo detallado}` — el problema está **en el diseño** (ubicación equivocada, pieza inconsistente, XML mal, dos reglas que se contradicen) y no se resuelve con código. **MUST NOT** editar el diseño.
- `DESIGN-ERRATA: {tarea}` + líneas `ERRATA: <fichero-del-diseño> § <ubicación>: dice «…»; debe decir «…». Motivo: <…>` — errata pequeña; prioridad sobre `DESIGN-ERROR`.
- `DONE: {tarea}` — solo cuando quedó materializada (y, si tiene test, en verde, salvo la dependencia cruzada de §3 paso 5).

**MUST NOT** pegar el código en la respuesta.

---

## 6. Marcar la tarea en el índice

**Solo al devolver `DONE`**: cambiar en `implementation/tasks.md` la línea de **esta** tarea de `- [ ]` a `- [x]` (con `Edit`). **MUST NOT** tocar las demás ni marcar ante otro token.
