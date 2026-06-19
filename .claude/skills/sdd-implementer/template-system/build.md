# Contrato de build — verificar y corregir la compilación

Lo leen el **verificador-build** (README §2.3) y el **corrector-build** (README §2.4). Define **cómo compilar el proyecto**, qué cuenta como éxito y cómo reportar/corregir los errores. El motor (`SKILL.md` §10) orquesta el bucle; aquí va lo específico del proyecto.

---

## 1. Comando de compilación

El verificador-build **MUST** compilar con:

```bash
./gradlew clean build --info
```

`./gradlew clean build` **compila** todo el proyecto y **ejecuta los tests unitarios y de arquitectura** (JUnit + ArchUnit) que las tareas de test materializaron en `src/test/...`. **NO** ejecuta los tests E2E (esos los corre `/sdd-debug-app`).

- **MUST NOT** usar `gradlew run` ni `--debug-jvm`: aquí solo se compila/testea, no se arranca la app.
- El verificador-build **ejecuta él mismo** este comando con `Bash`. El motor **NUNCA** lo ejecuta (`SKILL.md` §2.2).

---

## 2. Criterio de éxito

- **Éxito** = `./gradlew clean build` termina con **BUILD SUCCESSFUL** (compila y todos los tests unitarios/arquitectura pasan). → responde **exactamente** `OK-COMPILA`.
- **Fallo** = cualquier error de compilación **o** cualquier test (unitario/arquitectura) que falle. → responde con el JSONL de §3.

---

## 3. Formato de reporte de errores (verificador-build)

Si el build falla, responde **únicamente** con líneas **JSONL**: **un error por línea**, sin texto antes ni después, sin envoltorio de array. Cada línea **MUST** ser un objeto JSON con **exactamente** estos campos, en este orden:

- `id` — correlativo `E-NNN` (`E-001`, `E-002`, …).
- `tipo` — `COMPILE` (error del compilador) | `TEST` (test que falla).
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
- Ante un error de **test** (`tipo: TEST`): decide si el fallo es del **código de producción** (corrige la producción) o del **test mal generado** (corrige el test para que refleje la descripción de `design/test-unit-desc.md` / `test-arch-desc.md`). **MUST NOT** debilitar un test para que pase si el fallo real está en la producción.
- **MUST NOT** usar `AskUserQuestion`: ante un bloqueo, descríbelo en tu respuesta y termina (el motor lo lleva al usuario).

Tras corregir, el motor relanza el verificador-build (§1). El bucle tiene **LIMIT 3** iteraciones (lo controla `SKILL.md` §10); si los mismos errores se repiten entre iteraciones, el motor para y pregunta al usuario.
