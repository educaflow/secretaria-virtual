# Contrato de descomposición — del `design.md` a las tareas (feature libre)

Lo lee el **descomponedor** (README §2.1). Define **cómo convertir el diseño en una lista de tareas atómicas** en `{iniciativa}/implementation/`, sin implementar nada: solo los ficheros de tarea, su índice y la propagación de `test-e2e-desc.md`.

> Para las tareas que materializan **tests unitarios**, lee también `tests-code.md`.

---

## 1. Leer el `design.md` íntegro

1. Lee **todo** el `design.md`. La tabla dice **qué** ficheros hay; «Piezas», «Estrategia de verificación» y «Pasos» dicen **cómo** y **en qué orden**.
2. Lee la cabecera: `**Ubicación del código:**` es el conjunto de carpetas contra las que se resuelven las rutas relativas (prefijo `src/main/java/com/educaflow/`); las rutas que ya empiezan por `src/` se usan tal cual.
3. Localiza la tabla **«Ficheros a crear o modificar»** (`| Fichero | Acción | Skill | Descripción |`). Si no existe → indica el problema en la respuesta (el motor lo trata como STOP).
4. Lee `design/test-unit-desc.md`: cada `## Clase:` genera una **tarea de test** (ver `tests-code.md`). Si no existe el fichero, no hay tareas de test (el diseño debió justificarlo en su «Estrategia de verificación»; no es cosa del descomponedor).
5. Lee la sección `## 7. Pasos` del `design.md`: es el **orden** de las tareas. El descomponedor **MUST** respetarlo; solo lo ajusta si una dependencia real lo obliga, y entonces lo anota en la respuesta.

---

## 2. Agrupar los ficheros en tareas y ordenarlas (TDD)

Cada fila de la tabla genera **una tarea**, salvo los ficheros **fuertemente acoplados**, que van juntos:

- ✅ AGRUPAR: una interfaz, su `Impl` y sus DTOs → una tarea.
- ✅ AGRUPAR: clases auxiliares privadas de una pieza con esa pieza.
- ❌ NO AGRUPAR: un XML con la clase Java que lo usa; dos piezas de carpetas distintas; **un test con su pieza de producción** (van en **dos** tareas consecutivas, ver abajo).

Una tarea agrupa como mucho los ficheros de **un único componente lógico**. Si dudas, no agrupes.

La fila `src/test/java/...` de la tabla de ficheros y la sección `## Clase:` correspondiente de `test-unit-desc.md` son la **MISMA** tarea de test: no se duplica.

**CRITICAL — orden TDD.** Para cada pieza de producción que `test-unit-desc.md` describe:

1. **Tarea de test** (`tests-code.md`): escribe `<Clase>Test.java` **contra las firmas que el diseño declara**, aunque la clase aún no exista. Es la fase **red**.
2. **Tarea de pieza**, **inmediatamente después**: implementa la clase hasta que `<Clase>Test` pase. Es la fase **green**.

El `<texto del prompt>` de la tarea de pieza **MUST** terminar con el bloque de cierre de §4.

- ✅ CORRECTO: `task_03.md` = `AvisoGlobalServiceTest` (test) → `task_04.md` = `AvisoGlobalService` + `AvisoGlobalServiceImpl` (pieza, hasta que pase `task_03`).
- ❌ INCORRECTO: todos los tests al final (eso no es TDD: la pieza se escribe sin red), o test y pieza en la misma tarea (el implementador «adaptaría» el test al código).

Para una pieza **sin** test (excluida con justificación en la tabla 6.1 del diseño) hay solo la tarea de pieza.

**Orden general** (los «Pasos» del `design.md` mandan; esto es el fallback):

1. XML materializados (dominios, vistas, menús, data-init).
2. Parejas (test → pieza) en orden de dependencias, empezando por las que no dependen de nadie.
3. Configuración y recursos.
4. Lo demás.

**Dependencias cruzadas no bloquean**: elige un orden razonable y sigue; el build final verifica.

---

## 3. Determinar los skills de cada tarea

De la columna `Skill` de la tabla, normalizados al nombre real (`k-sistemas`, `k-vistas`, `k-secure-coding`, `k-code-quality`, `k-validaciones`, `k-guice`, `k-scheduler`, `k-datainit`, `k-i18n`, …; ignora las anotaciones entre paréntesis).

**CRITICAL**: añade `k-secure-coding` y `k-code-quality` a **toda** tarea con Java/Kotlin, aunque la tabla no lo liste. Para tareas de solo XML o recursos, no hace falta si no aporta. Las tareas de test llevan los skills de `tests-code.md` §1.

---

## 4. Escribir cada `task_NN.md`

```
---
type: implementation-task
template: <valor copiado del design.md>
---

# Tarea NN a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- <skill A>
- <skill B>

<texto del prompt>
```

Reglas:

- `NN` de dos dígitos. `template:` **verbatim** del `design.md` (incluido `external`).
- **`<texto del prompt>`**: todo lo relevante del `design.md` para los ficheros de la tarea, copiado **verbatim**: su(s) fila(s) de la tabla (con la columna `Acción`), su subsección de «Piezas» (firmas, colaboradores, comportamiento), las filas de «Trazabilidad» que le tocan, y la línea de «Dónde vive el código y por qué» de su carpeta (para que el implementador no la mueva).
- Para una tarea de **XML materializado**: indicar que el fichero está en `design/...` y se **copia literalmente** (o fusiona, `menus.xml`), sin regenerarlo; si es `Modificar`, que el destino existe y aplica la comprobación de conservación (`implementation.md` §4).
- Para una tarea de **pieza con test** (fase green): además de lo anterior, el `<texto del prompt>` **MUST** terminar con este bloque de cierre, con los placeholders rellenados:
  ```
  Tarea de test previa: task_MM.md (fase red). Al terminar la pieza ejecuta
  ./gradlew test --tests '<FQN de la clase de test>'
  y devuelve DONE solo si está en verde o aplica implementation.md §3 paso 5 (dependencia cruzada).
  ```
- Para una tarea de **test**: el prompt lo fija `tests-code.md` §2.
- **MUST NOT** resumir ni parafrasear el diseño. **MUST NOT** inventar ficheros, pasos ni validaciones.

---

## 5. Escribir el índice `tasks.md` y propagar `test-e2e-desc.md`

```
---
type: implementation-tasks
template: <valor copiado del design.md>
---

# Lista de tareas a implementar
- [ ] [Tarea 01](task_01.md)
- [ ] [Tarea 02](task_02.md)
```

- Un enlace por tarea, en orden, con checkbox **sin marcar** (lo marca el implementador al completar).
- Si existe `design/test-e2e-desc.md`, **cópialo literalmente** a `implementation/test-e2e-desc.md`. Es contrato fijo hacia abajo: **MUST NOT** modificarlo.

---

## 6. Token de salida

- Primera línea **exactamente** `ESCRITO: implementation/`.
- Una línea `=== TAREAS ===` y debajo **una línea por tarea**: `task_NN.md | {título} | {ficheros que cubre}`. En el título de una tarea de test añade `(TDD red)` y en el de su pieza `(TDD green de task_MM)`.
- **MUST NOT** pegar el contenido de las tareas.

---

## 7. Checklist del descomponedor

**LIMIT**: 3 iteraciones de corrección.

- [ ] ¿Se leyó el `design.md` íntegro (cabecera, tabla, Piezas, Estrategia de verificación, Pasos) y `test-unit-desc.md`?
- [ ] ¿Cada fichero de la tabla está en **exactamente una** tarea?
- [ ] ¿Cada pieza con test tiene su **pareja consecutiva** test → pieza, y la tarea de pieza termina con la instrucción de ejecutar su test?
- [ ] ¿Ninguna pieza excluida con justificación tiene tarea de test inventada?
- [ ] ¿Cada `task_NN.md` tiene `type: implementation-task`, `template:` verbatim, sus skills (con `k-secure-coding` y `k-code-quality` en todo Java/Kotlin) y el texto del diseño **verbatim**?
- [ ] ¿El orden sigue los «Pasos» del diseño (o la desviación está anotada en la respuesta)?
- [ ] ¿Existe `tasks.md` correcto y, si existía, `implementation/test-e2e-desc.md` copiado literalmente?
- [ ] ¿La respuesta lleva `ESCRITO: implementation/` + `=== TAREAS ===`?
