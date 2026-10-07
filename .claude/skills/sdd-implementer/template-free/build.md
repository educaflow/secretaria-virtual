# Contrato de build — verificar y corregir la compilación (feature libre)

Lo leen el **verificador-build** (README §2.3) y el **corrector-build** (README §2.4). Define cómo compilar, qué cuenta como éxito y cómo reportar/corregir. El motor orquesta el bucle (LIMIT 20); aquí va lo específico del proyecto.

---

## 1. Comando de compilación

```bash
./gradlew clean build --info
```

Compila todo y ejecuta:

- los **tests unitarios** (incluidos los generados por TDD);
- los tests de **arquitectura** (ArchUnit, `architecture-rules.md`);
- los de **vistas** (`view-rules.md`);
- los de **orden de métodos**;
- los de **tipos de expediente**;
- el **CPD** (duplicados);
- el **CRAP** (`crapUmbral`).

No ejecuta los E2E.

- **MUST NOT** usar `gradlew run` ni `--debug-jvm`. El verificador-build **ejecuta él mismo** el comando con `Bash`.

---

## 2. Criterio de éxito

- **Éxito** = `BUILD SUCCESSFUL` **y** sin `warning: [` de Error Prone en `src/` (§2.1) **y** el chequeo de conformidad de superficie (§5) limpio → **exactamente** `OK-COMPILA`.
- **Fallo** = error de compilación, warning de Error Prone en `src/`, cualquier test que falle (unitario, ArchUnit, vistas, orden de métodos), `cpdCheck` con duplicados, `crapCheck` por encima del umbral, o superficie no declarada → JSONL de §3.
- **Bloqueo** = el build no se puede ni ejecutar por el entorno → `BLOCKED: {motivo}`.

### 2.1 Warnings de Error Prone

Salen como `warning: [NombreDelCheck] …` con fichero y línea. **MUST** guardar la salida completa y buscar los que estén bajo `src/`. Cada uno es una línea JSONL `tipo: WARNING`. **MUST NOT** responder `OK-COMPILA` mientras quede alguno.

---

## 3. Formato de reporte de errores (verificador-build)

Solo líneas **JSONL**, un error por línea, con **exactamente** estos campos en este orden:

- `id` — `E-NNN`.
- `tipo` — `COMPILE` | `WARNING` | `TEST` (test unitario) | `ARCH` (ArchUnit, vistas, orden de métodos, tipos de expediente) | `QUALITY` (CPD o CRAP) | `CONFORMANCE` (§5).
- `fichero` — ruta, o `null`.
- `ubicacion` — línea / método / nombre del test / regla (`C-12`, `VAR-forms.3`); `null` si no aplica.
- `tarea` — la `task_NN.md` de origen probable, o `null`.
- `mensaje` — el mensaje **literal**.
- `correccion` — qué cambiar.

- ✅ CORRECTO: `{"id":"E-001","tipo":"ARCH","fichero":"src/main/java/com/educaflow/base/web/AvisoGlobalBanner.java","ubicacion":"C-3","tarea":"task_04.md","mensaje":"Classes in base should not depend on com.educaflow.system..","correccion":"Invertir la dependencia: base expone una interfaz y gestioncentro la implementa (ver design.md §2)."}`
- ✅ CORRECTO: `{"id":"E-002","tipo":"TEST","fichero":"src/test/java/com/educaflow/system/gestioncentro/service/AvisoGlobalServiceImplTest.java","ubicacion":"insert_textoVacio_lanzaExcepcion","tarea":"task_04.md","mensaje":"expected ValidationException but none was thrown","correccion":"Implementar REQ-003 en AvisoGlobalServiceImpl.validateInsert."}`
- ✅ CORRECTO: `{"id":"E-003","tipo":"QUALITY","fichero":"src/main/java/com/educaflow/base/web/AvisoGlobalBanner.java","ubicacion":"render","tarea":"task_04.md","mensaje":"CRAP 42 > crapUmbral 30 (CC 6, cobertura 0.10)","correccion":"Añadir los tests de ramas descritos en test-unit-desc.md o trocear render()."}`
- ❌ INCORRECTO: prosa; `OK` con un test rojo; omitir un fallo de `cpdCheck` porque «no es código mío».

---

## 4. Qué puede y qué NO puede tocar el corrector-build

- **MUST** corregir solo código Java/Kotlin (producción o tests) y, si el error es de **recursos/configuración** que una tarea creó (una propiedad mal escrita, un fichero estático), ese recurso. Delega el Java en `developer-code-implementer` cargando antes los skills de la `tarea` de origen.
- **CRITICAL — los XML del diseño ya colocados son contrato fijo**: **MUST NOT** editarlos para que cuadre el Java. Si un error solo se resuelve cambiando el diseño → `DESIGN-ERRATA: corrector-build` + líneas `ERRATA:` (errata pequeña) o `DESIGN-ERROR: {motivo detallado}`.
- **`tipo: TEST`** (unitario): decide si el fallo es de la **producción** (corrige la producción: es la fase green que quedó a medias) o de un **test mal generado** respecto a `design/test-unit-desc.md` (corrige el test para que refleje la descripción). **MUST NOT** debilitar un test para que pase si el fallo real está en la producción. **MUST NOT** cambiar un mensaje esperado si coincide con la spec.
- **`tipo: ARCH`**: una regla de arquitectura o de vistas rota se corrige **respetando la regla**, nunca editando el test de arquitectura/vistas ni añadiendo excepciones a `architecture-rules.md`/`view-rules.md`. Si la única forma es cambiar la ubicación que el diseño fijó → `DESIGN-ERROR` (la ubicación la decide el diseñador).
- **`tipo: QUALITY`**: CPD → extraer el código común donde corresponda (`tramites/util` si lo comparten trámites, `base` si es transversal, nunca el motor); `// CPD-OFF` solo para duplicación **deliberada** y justificada. CRAP → añadir los tests que `test-unit-desc.md` ya describe o trocear el método; **MUST NOT** subir `crapUmbral`.
- **`tipo: WARNING`**: corregir como indica el check; `@SuppressWarnings` solo ante falso positivo con comentario. **MUST NOT** desactivar checks en `build.gradle`.
- **CRITICAL — no legitimar superficie no diseñada**: ante «method does not override…» o firma que no cuadra, comprueba el origen en la `task` y en `design.md`; si figura → alinéalo con el diseño; si **no** figura → **elimínalo** (y sus llamadores) en vez de ampliar la interfaz. **Guarda anti-borrado**: con `git diff`/`git log`, si el miembro **preexistía** a la iniciativa **MUST NOT** eliminarlo: **detente y repórtalo**.
- **MUST NOT** usar `AskUserQuestion`. Tokens en la primera línea: `CORREGIDO` (+ `E-NNN: <qué cambiaste>` por error), `BLOCKED: {motivo}`, `DESIGN-ERRATA: corrector-build`, `DESIGN-ERROR: {motivo}`.

---

## 5. Chequeo de conformidad de superficie (verificador-build)

Solo cuando el build termina en `BUILD SUCCESSFUL`, **antes** de `OK-COMPILA`:

1. **Superficie declarada**: unión de las tablas de ficheros de todas las `task_NN.md` + los miembros públicos de sus bloques de firmas.
2. **Superficie real** de la iniciativa (`git status` / `git diff --name-only`): fichero **nuevo** bajo `src/main/...` o `src/test/...` → toda su superficie pública; fichero **preexistente modificado** → solo lo **añadido o con firma cambiada** según su `git diff`. **MUST NOT** reportar miembros preexistentes.
3. Reportar como `CONFORMANCE` cualquier clase/fichero nuevo no listado o método público no descrito/renombrado. Los XML y el código generado (`build/`) no cuentan.
4. **Especial de feature libre**: un fichero tocado bajo `src/main/...` en una carpeta que **no** está en `**Ubicación del código:**` del `design.md` es `CONFORMANCE` aunque su contenido sea correcto (el implementador movió código a donde el diseño no lo puso). Los ficheros bajo `src/test/...` **no** entran en esta comprobación (ese campo solo lista producción; los tests ya se cubren en los puntos 2 y 3).

Si §5 está limpio **y** el build pasó → **exactamente** `OK-COMPILA`.
