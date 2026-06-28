# Contrato de plantilla — creación de tests E2E de regresión

Esta carpeta es el **contrato** que leen los subagentes de `/sdd-create-tests-e2e`. El `SKILL.md` es un motor agnóstico: **todo** lo específico de cómo se genera y se sana un `.spec.ts` está aquí. Si cambias esta carpeta (o apuntas `--template-dir` a otra), cambias qué y cómo se genera **sin tocar el skill**.

---

## 1. Ficheros de la plantilla

| Fichero | Lo lee | Para qué |
|---------|--------|----------|
| `README.md` (este) | los tres roles + el motor | índice del contrato, contexto del proyecto y **«Gestión de la app»** (§4, la ejecuta el motor) |
| `generation.md` | **generador** | cómo convertir un `t-NNN-<slug>.desc.md` en su `.spec.ts`: ciclo de login/logout, plantilla del test, plantilla de `_support/auth.ts`, trazabilidad, checklist |
| `verification.md` | **verificador** | qué hace **fiel** a un test ya verde: cubrir todo el `Resultado esperado` con aserciones reales, auth correcta, sin debilitar ni saltar; cómo auditarlo sin tocarlo |
| `healing.md` | **sanador** | cómo diagnosticar y arreglar un `.spec.ts` rojo o declarado `INFIEL`, sin tocar el código Java |

---

## 2. Roles

**CRITICAL — separación de poderes anti-trampa**: los tres roles corren en **contextos aislados**. El que **crea** el test (generador) **no** decide si vale; quien lo decide es el **runner mecánico** (lo ejecuta el motor) más un **verificador** que no escribió el test. Un test verde pero infiel (aserciones que faltan o debilitadas) lo caza el verificador.

| Rol | Qué hace | Entrada propia | Lee de esta plantilla | Resultado |
|---|---|---|---|---|
| **generador** (§2.1) | **Genera** un `.spec.ts` desde su descripción, pilotando la app real. **No** declara si pasa | la ruta de **un** `t-NNN-<slug>.desc.md` (la copia en `src/test/e2e/<iniciativa>/`) + la ruta destino del `.spec.ts` | `generation.md` (+ contexto §3) | el fichero `t-NNN-<slug>.spec.ts` hermano + token `ESCRITO:` |
| **verificador** (§2.2) | **Audita** de forma adversarial que un `.spec.ts` ya verde es **fiel** a su descripción. NO modifica el test | la ruta del `.spec.ts` (verde) y su `.desc.md` | `verification.md` (+ contexto §3) | token `OK:` (fiel) o `INFIEL: — {motivo}` |
| **sanador** (§2.3) | **Arregla** un `.spec.ts` rojo o declarado `INFIEL` | la ruta del `.spec.ts`, su `.desc.md` y el fallo (salida del runner o motivo `INFIEL`) | `healing.md` (+ contexto §3) | el `.spec.ts` corregido **en sitio** + token `CORREGIDO:`/`BLOQUEADO:` |

Los tres roles:

- **MUST** cargar el skill `/k-playwright` (convenciones del proyecto: estructura, locators, baseURL, `_support/auth.ts`, login común).
- **MUST NOT** modificar código Java (`src/main/...`) ni la fuente en `.sdd/`.
- **MUST NOT** usar `AskUserQuestion`: ante un bloqueo, devuelven su token.
- **MUST NOT** pegar el contenido de los ficheros en la respuesta (ya está en disco): solo el token de estado.
- El **generador MUST NOT** declarar si el test pasa; el **verificador MUST NOT** editar el test; el **sanador MUST NOT** debilitar ni borrar aserciones.

---

## 3. Contexto del proyecto

- La app es una secretaría virtual sobre **Axelor 8.1**, servida en `http://localhost:8080/`; login en `http://localhost:8080/#/login`. El `baseURL` de `playwright.config.ts` ya es `http://localhost:8080`: **MUST** usar rutas relativas (`page.goto('/#/login')`).
- Convenciones de tests, locators y estructura de carpetas: las define `/k-playwright` (cárgalo). En particular: pares `t-NNN-<slug>.desc.md` ↔ `t-NNN-<slug>.spec.ts`, **mismo nombre base y misma carpeta**; helper compartido `src/test/e2e/_support/auth.ts`.
- La app es **multicentro y bilingüe (es/ca)**: los locators por texto asumen español salvo que el test diga lo contrario.

---

## 4. Gestión de la app (la ejecuta el MOTOR, no los subagentes)

El motor deja la app respondiendo `200` antes de generar el primer test y la para al terminar. **MUST** seguir estos comandos al pie de la letra.

### 4.1 Comprobar si está levantada

La app se considera levantada si responde `200`:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
```

### 4.2 Limpiar el puerto 8080 (antes de arrancar)

**CRITICAL**: una instancia previa colgada hace que el connector falle el bind en silencio. Limpia de verdad, **excluyendo** IntelliJ y similares:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
pkill -f 'TomcatRunner|GradleWrapperMain.*run' 2>/dev/null
ss -ltn | grep ':8080' || echo "8080 libre"
```

### 4.3 Arrancar (tarea tracked en segundo plano)

Usa **siempre** `./run.sh` (hace `./gradlew clean build` y arranca en el 8080 con la config correcta). Lánzalo con `Bash`, `run_in_background: true` y `dangerouslyDisableSandbox: true` (`run.sh` escribe en `~/.gradle`, fuera del sandbox), redirigiendo el log:

```bash
exec ./run.sh > src/test/e2e/.app.log 2>&1
```

**Sondea** hasta `200` con margen amplio (el `clean build` + el bind pueden tardar varios minutos): repite el `curl` de §4.1 con `Monitor`/reintentos, **LIMIT** de sondeo ~420 s por ventana, varias ventanas si hace falta.

### 4.4 Ejecutar un `.spec.ts` (lo hace el motor tras el generador / tras cada `CORREGIDO`)

```bash
npx playwright test {ruta del .spec.ts} --project=chromium
```

Exit code `0` = **PASS**; distinto de `0` = **FAIL** (pasa la salida al sanador). La app **MUST** estar en `200` antes de ejecutar.

### 4.5 Parar (al terminar, §10 del skill)

Siempre **por puerto**, nunca por handle de proceso:

```bash
fuser -k 8080/tcp 2>/dev/null || lsof -ti tcp:8080 | xargs -r kill
```

> El log `src/test/e2e/.app.log` es del motor; no se commitea (añadir a `.gitignore` si hiciera falta) y los subagentes lo ignoran.

---

## 5. Cabecera-banner del snapshot (la escribe el MOTOR en la Fase 2)

Al copiar un `t-NNN-<slug>.desc.md` de `test-e2e-desc/` a `src/test/e2e/<iniciativa>/`, el motor **antepone** este bloque **justo después del frontmatter** (para no romper `type:`/`id:`), dejando el resto del contenido **verbatim**:

```markdown
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/{carpeta-iniciativa}/test-e2e-desc/{fichero}.desc.md
     Test: {T-NNN}  |  Origen ESC: {ESC-NNN, leído de la línea "Origen ESC:" del propio fichero}
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->
```

- ✅ CORRECTO: el banner va entre el `---` de cierre del frontmatter y el `# T-NNN — …`.
- ❌ INCORRECTO: ponerlo **antes** del frontmatter (rompería el parseo de `type:`/`id:`), o reescribir el cuerpo del test.
