# Contrato de build — verificar y corregir la compilación

Lo leen el **verificador-build** (README §2.3) y el **corrector-build** (README §2.4). Define **cómo compilar el proyecto**, qué cuenta como éxito y cómo reportar/corregir los errores. El motor (`SKILL.md` §10) orquesta el bucle; aquí va lo específico del proyecto.

---

## 1. Comando de compilación

El verificador-build **MUST** compilar con:

```bash
./gradlew clean build --info
```

`./gradlew clean build` **compila** todo el proyecto y **ejecuta los tests unitarios** (JUnit + Mockito) que las tareas de test materializaron en `src/test/...`. **NO** ejecuta los tests E2E (esos los corre `/sdd-debug-with-test-e2e-desc`).

- **MUST NOT** usar `gradlew run` ni `--debug-jvm`: aquí solo se compila/testea, no se arranca la app.
- El verificador-build **ejecuta él mismo** este comando con `Bash`. El motor **NUNCA** lo ejecuta (`SKILL.md` §2.2).

---

## 2. Criterio de éxito

- **Éxito** = `./gradlew clean build` termina con **BUILD SUCCESSFUL** (compila y todos los tests unitarios pasan) **Y** el chequeo de conformidad de superficie (§5) no encuentra superficie no declarada. → responde **exactamente** `OK-COMPILA`.
- **Fallo** = cualquier error de compilación, cualquier test unitario que falle, **o** cualquier superficie no declarada que detecte §5. → responde con el JSONL de §3.

---

## 3. Formato de reporte de errores (verificador-build)

Si el build falla, responde **únicamente** con líneas **JSONL**: **un error por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:

- `id` — correlativo `E-NNN` (`E-001`, `E-002`, …).
- `tipo` — `COMPILE` (error del compilador) | `TEST` (test que falla) | `CONFORMANCE` (superficie no declarada, §5).
- `fichero` — ruta del fichero afectado, o `null`.
- `ubicacion` — línea / método / nombre del test; `null` si no aplica.
- `tarea` — la `task_NN.md` de `implementation/` de la que probablemente proviene (úsala para que el corrector sepa qué skills aplican), o `null`.
- `mensaje` — el mensaje **literal** del compilador o del test.
- `correccion` — qué cambiar para resolverlo.

Cada línea **MUST** ser JSON válido en una sola línea (escapa los saltos como `\n`). **MUST NOT** añadir comentarios ni texto fuera del JSONL.

- ✅ CORRECTO: `{"id":"E-001","tipo":"COMPILE","fichero":"src/main/java/com/educaflow/system/bar/service/BarServiceImpl.java","ubicacion":"línea 42","tarea":"task_03.md","mensaje":"cannot find symbol: method getNombre()","correccion":"Usar getName() según Bar.xml."}`
- ✅ CORRECTO (test que falla): `{"id":"E-002","tipo":"TEST","fichero":"src/test/java/com/educaflow/system/bar/service/BarServiceImplTest.java","ubicacion":"validateInsert_nombreVacio","tarea":"task_08.md","mensaje":"expected exception ValidationException but none was thrown","correccion":"Implementar la validación VAL-Bar-001 en BarServiceImpl.validateInsert."}`
- ❌ INCORRECTO: `BUILD FAILED, hay 3 errores` (prosa, no JSONL), o devolver `OK` cuando un test falla.

---

## 4. Qué puede y qué NO puede tocar el corrector-build

El corrector-build resuelve cada línea JSONL. Reglas duras:

- **MUST** corregir **solo código Java** (producción o tests). Si el contrato de dominio lo aconseja, delega en `code-implementer` cargando antes los skills de la tarea de origen (`tarea`).
- **MUST NOT** editar los XML del diseño ya colocados (dominios, vistas, `menus.xml`): son **contrato fijo** (`implementation.md` §1/§4). Si un error apunta a que un XML está mal, **detente y repórtalo** en tu respuesta (no lo edites) — hay que volver a `/sdd-designer`.
- Ante un error de **test** (`tipo: TEST`): decide si el fallo es del **código de producción** (corrige la producción) o del **test mal generado** (corrige el test para que refleje la descripción de `design/test-unit-desc.md`). **MUST NOT** debilitar un test para que pase si el fallo real está en la producción.
- **CRITICAL — no legitimar superficie no diseñada**: ante un error tipo *"method does not override or implement a method from a supertype"* (o un `@Override`/firma que no cuadra con su interfaz/supertipo), **MUST NOT** ampliar la interfaz/supertipo ni crear el método para que el `@Override` compile **sin antes comprobar el origen del método**. Comprueba si figura en la `task` de origen del error o en `design.md`:
  - Si **sí** figura → alinéalo con la firma del diseño.
  - Si **NO** figura (es superficie inventada por una tarea previa — método, controlador o clase de más) → **elimínalo del impl** y de cualquier llamador (controlador/acción) que lo use, en vez de añadirlo a la interfaz. La vía barata —ampliar la API para que el `@Override` compile— **consolida el invento**; **MUST NOT** tomarla.
  - Si no puedes determinar el origen → **detente y repórtalo** en tu respuesta (no adivines).
- **MUST NOT** usar `AskUserQuestion`: ante un bloqueo, descríbelo en tu respuesta y termina (el motor lo lleva al usuario).

Tras corregir, el motor relanza el verificador-build (§1). El bucle tiene **LIMIT 3** iteraciones (lo controla `SKILL.md` §10); si los mismos errores se repiten entre iteraciones, el motor para y pregunta al usuario.

---

## 5. Chequeo de conformidad de superficie (verificador-build)

Compilar y pasar los tests **no** detecta que el implementador haya creado **superficie de más** (clases, controladores o métodos públicos que ninguna tarea pidió): de hecho compila sin problema y el bucle de build tiende a **legitimar** el invento (por eso §4 lo prohíbe). Por eso, **solo cuando `./gradlew clean build` termina en BUILD SUCCESSFUL**, el verificador-build **MUST** ejecutar este chequeo **antes** de responder `OK-COMPILA`.

Pasos:

1. Reúne la **superficie declarada**: la unión de las **tablas de ficheros** de todas las `implementation/task_NN.md` (columna `Fichero`) más los métodos/clases públicos que sus bloques de firma describen.
2. Reúne la **superficie real producida**: los ficheros Java creados/modificados bajo `src/main/...` y `src/test/...` por esta iniciativa (p.ej. los que aparecen en `git status`/`git diff --name-only` respecto al punto de partida) y sus clases/métodos públicos.
3. Compara. **MUST** reportar como error de conformidad cualquier elemento de la superficie real que **no** esté en la declarada:
   - una **clase/fichero** nuevo no listado en ninguna tabla de tareas (p.ej. un `XxxController` no pedido), o
   - un **método público** no descrito por ninguna tarea, o un método de la tarea **renombrado / con firma distinta** de la declarada.
4. Los XML de dominios/vistas/menús y el código generado por Axelor (`build/`) **NO** cuentan: solo Java escrito a mano bajo `src/main/...` y `src/test/...`.

Cada hallazgo se reporta como una línea JSONL de §3 con `tipo: CONFORMANCE`, `tarea` = la tarea más cercana (o `null`), y `correccion` apuntando a eliminar la superficie no declarada (§4) o, si de verdad falta en el diseño, a volver a `/sdd-designer`.

- ✅ CORRECTO: `{"id":"E-001","tipo":"CONFORMANCE","fichero":"src/main/java/com/educaflow/system/gruposnotas/controller/AlumnoGrupoController.java","ubicacion":"guardarAlumnoGrupo","tarea":null,"mensaje":"Controlador y método no declarados en ninguna task_NN.md (el alta de AlumnoGrupo va por save-modal → insert).","correccion":"Eliminar AlumnoGrupoController.guardarAlumnoGrupo y la firma asociada del servicio; el alta es insert genérico (build.md §4)."}`
- ❌ INCORRECTO: responder `OK-COMPILA` con una clase/método de más en el árbol que ninguna tarea pidió.

Si §5 no encuentra superficie no declarada **y** el build pasó → responde **exactamente** `OK-COMPILA`.
