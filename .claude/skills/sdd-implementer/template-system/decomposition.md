# Contrato de descomposición — del `design.md` a las tareas

Lo lee el **descomponedor** (README §2.1). Define **cómo convertir el diseño en una lista de tareas atómicas** escritas en `{iniciativa}/implementation/`, sin implementar nada todavía: solo escribir los ficheros de tarea, su índice y los ficheros de contrato propagados.

> Para crear las tareas que materializan **tests** unitarios, lee también `tests-code.md`.

---

## 1. Leer el `design.md` íntegro y su tabla de ficheros

1. Lee **todo** el `design.md`, no solo la tabla. La tabla dice **qué** ficheros hay; las secciones "Paso N", "Frontera de confianza — AllowProperties por acción" y "Trazabilidad V/R/U" dicen **cómo** se implementa cada uno.
2. Localiza la tabla **"Ficheros a crear o modificar"**. Cada fila tiene la forma:

   | Fichero | Acción | Skill | Descripción |
   |---------|--------|-------|-------------|
   | `subsystem/foo/domains/Bar.xml` | Crear | k-sistemas (modelos.md) | Entidad Bar |
   | `subsystem/foo/service/BarService.java` | Crear | k-sistemas, k-secure-coding | Interfaz del servicio |
   | `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir menú |

   Si no existe esa tabla → responde con el token de bloqueo del descomponedor (`ESCRITO: implementation/` no aplica: en su lugar, indica el problema; el motor lo trata como STOP).
3. Las rutas relativas tipo `subsystem/foo/domains/Bar.xml` se resuelven contra el prefijo estándar `src/main/java/com/educaflow/`. Las rutas que ya empiezan por `src/main/...` se usan tal cual.
4. Comprueba si el diseño trae descripción de tests (`design/test-unit-desc.md`): generará **tareas de test** (ver `tests-code.md`).

---

## 2. Agrupar los ficheros en tareas

Cada fila de la tabla genera **una tarea**, salvo los ficheros **fuertemente acoplados**, que van juntos en **una sola tarea**. Están fuertemente acoplados los ficheros que no tienen sentido implementar por separado:

- ✅ AGRUPAR: una interfaz `XService`, su `XServiceImpl` y su `XInsertDTO` → una tarea de "servicio X".
- ✅ AGRUPAR: clases auxiliares privadas de un servicio (factories, records de resultado) con ese servicio.
- ❌ NO AGRUPAR: `Bar.xml` (dominio) con `BarController.java` (capas distintas, contratos distintos).
- ❌ NO AGRUPAR: dos vistas distintas (`Bar-Todos.xml` y `Bar-MiCentro.xml`) que el diseño describe por separado.

**LIMIT**: una tarea agrupa como mucho los ficheros de **un único componente lógico** (un servicio con su impl/DTO/auxiliares, o un dominio con su enum embebido). Si dudas, **NO agrupes**.

Numera las tareas `01`, `02`, … en el **orden lógico de implementación** del diseño:

1. dominios →
2. servicios →
3. repositorios →
4. controladores →
5. vistas →
6. menús →
7. jobs / seguridad / datos iniciales →
8. **tests unitarios** (dependen de las clases de producción ya descritas).

Razón del orden: las tareas posteriores dependen del código de las anteriores, y los tests se generan al final (ver `tests-code.md`).

**Ordenar por dependencias.** El orden por capas de arriba es el caso habitual, pero la regla general es: **coloca cada tarea después de aquellas de las que depende** y **empieza por las que no dependen de nadie**. Usa la dirección de dependencias real del diseño (qué clase usa a cuál); el orden por capas es solo el fallback cuando no hay más información.

**Dependencias cruzadas — no bloquean.** Si dos o más tareas se dependen mutuamente (dependencia cruzada o cíclica) y no existe un orden perfecto:

- **MUST NOT** detenerte, bloquear ni reportar el ciclo como error; **MUST NOT** fusionar tareas solo para romperlo.
- Elige un orden razonable (el de capas de arriba sirve) y **continúa**. Cada tarea se implementa con lo que dice **su propio** `<texto del prompt>` (el texto del diseño copiado verbatim), no con el código de las demás, así que el orden exacto entre tareas mutuamente dependientes no afecta a lo que se escribe.
- La compilación del proyecto al final (`./gradlew clean build`, `SKILL.md` §10) verificará que todas las piezas encajan. La descomposición **no** tiene que garantizar el orden perfecto.

---

## 3. Determinar los skills de cada tarea

Los skills de una tarea salen de la columna `Skill` de la tabla para sus ficheros, normalizados al nombre real del skill (`k-sistemas`, `k-vistas`, `k-secure-coding`, `k-scheduler`, `k-code-quality`, `k-i18n`, `k-guice`, …; ignora las anotaciones entre paréntesis tipo `(modelos.md)`).

**CRITICAL**: añade `k-secure-coding` y `k-code-quality` a **toda** tarea cuyo código Java toque entidades, servicios o controladores, aunque la tabla no lo liste — `k-secure-coding` define defensas (mass-assignment, `AllowProperties` por acción, asignación incondicional de campos `servidor`, multi-centro/IDOR, JPQL, adjuntos) que protegen al resto del sistema. Para tareas de solo XML (dominios, vistas, menús) no lo añadas si no aporta.

Para las tareas de test, los skills los fija `tests-code.md` (p.ej. `k-code-quality` para los tests unitarios).

---

## 4. Escribir cada `task_NN.md`

Por cada tarea, escribe `{iniciativa}/implementation/task_NN.md` con **exactamente** esta plantilla:

```
---
type: implementation-task
---

# Tarea NN a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- <skill A>
- <skill B>

<texto del prompt>
```

Reglas de relleno:

- `NN` es el número de dos dígitos de la tarea (`01`, `02`, …).
- La lista de skills es la determinada en §3.
- **`<texto del prompt>`**: todo lo relevante del `design.md` para los ficheros de esta tarea, copiado **verbatim**. **MUST** incluir, cuando apliquen:
  - La(s) fila(s) de la tabla "Ficheros a crear o modificar" de esos ficheros (con su ruta destino). **MUST** conservar la columna `Acción` (`Crear`/`Modificar`): el implementador la usa para decidir cómo materializar (ver `implementation.md` §3).
  - La(s) sección(es) "Paso N" que describen esos ficheros (firmas, comentarios, estructura).
  - Las secciones transversales que apliquen: "Frontera de confianza — AllowProperties por acción" y las filas de "Trazabilidad V/R/U → ubicación" que les correspondan.
  - Las referencias a `rules/R-*.md` citadas para esos ficheros (cita la ruta; **MUST NOT** copiar su contenido entero si es extenso).
- Para una tarea de **XML ya materializado** (dominio, vista, `menus.xml`), el `<texto del prompt>` **MUST** indicar explícitamente que el fichero está en `design/...` y que se debe **copiar literalmente** (o fusionar, para `menus.xml`) a su ruta destino, **sin regenerarlo** (ver `implementation.md` §1). Si la fila es `Acción: Modificar`, el texto **MUST** indicar además que el destino **ya existe** y que antes de sobrescribir aplica la **comprobación de conservación** de `implementation.md` §3.
- Para una tarea de **test**, el `<texto del prompt>` lo fija `tests-code.md` (referencia a `design/test-unit-desc.md` y la ubicación destino en `src/test/...`).

**MUST NOT**:

- **MUST NOT** resumir, reescribir o parafrasear el texto del diseño. Se copia verbatim — el diseño es el contrato.
- **MUST NOT** inventar pasos, validaciones o ficheros que no estén en el `design.md` (o en las descripciones de test).

Ejemplos ✅/❌ de cabecera de tarea:

- ✅ CORRECTO: `# Tarea 03 a implementar` con `type: implementation-task` a ras de margen.
- ❌ INCORRECTO: `# Tarea 3` (sin dos dígitos) o frontmatter `type: design` (tipo equivocado).

---

## 5. Escribir el índice `tasks.md` y propagar los ficheros de contrato

1. Escribe `{iniciativa}/implementation/tasks.md` con **exactamente** esta plantilla, una línea por tarea generada, en orden. Cada tarea lleva delante un **checkbox sin marcar** (`- [ ]`) que sirve de seguimiento: el implementador lo marca (`- [x]`) a medida que completa cada tarea en la Fase 4 (ver `implementation.md` §5):

```
---
type: implementation-tasks
---

# Lista de tareas a implementar
- [ ] [Tarea 01](task_01.md)
- [ ] [Tarea 02](task_02.md)
```

Reglas:

- Un enlace por cada `task_NN.md` creado, en orden, **precedido de un checkbox sin marcar** `- [ ]`.
- El texto del enlace es `Tarea NN`; el destino es `task_NN.md`.
- Todos los checkboxes se escriben **sin marcar** (`- [ ]`): marcarlos es responsabilidad del implementador al completar cada tarea, **MUST NOT** marcarlos al crear el índice.
- ✅ CORRECTO: `- [ ] [Tarea 01](task_01.md)`.
- ❌ INCORRECTO: `- [Tarea 01](task_01.md)` (sin checkbox), `- [x] [Tarea 01](task_01.md)` (no se marca al crear el índice), `- [ ] [Tarea 1](tarea_01.md)` (número sin dos dígitos y nombre de fichero que no coincide con el real).

2. Si existe `{iniciativa}/design/test-e2e-desc.md`, **cópialo literalmente** a `{iniciativa}/implementation/test-e2e-desc.md`. Es **contrato fijo hacia abajo**: **MUST NOT** modificarlo, resumirlo ni renumerarlo — es la entrada que `/sdd-debug-with-test-e2e-desc` ejecutará contra la aplicación real. Si no existe, no pasa nada.

---

## 6. Token de salida

Tras escribir todo, responde con el formato que define el skill (`SKILL.md` §7):

- Primera línea **exactamente** `ESCRITO: implementation/`.
- Una línea **exactamente** `=== TAREAS ===` y, debajo, **una línea por tarea** en orden, con el formato `task_NN.md | {título} | {ficheros que cubre}`.
- **MUST NOT** pegar el contenido de las tareas en la respuesta (ya está en disco).

---

## 7. Checklist del descomponedor

Antes de devolver el token, **MUST** recorrer este checklist. Si algo falla, corrige y repite. **LIMIT**: máximo 3 iteraciones de corrección.

- [ ] ¿Se leyó el `design.md` íntegro y se localizó la tabla "Ficheros a crear o modificar"?
- [ ] ¿Cada fichero de la tabla está cubierto por **exactamente una** tarea (agrupando solo los acoplados)?
- [ ] ¿Cada `task_NN.md` tiene `type: implementation-task`, su lista de skills y el texto del diseño **verbatim**?
- [ ] ¿Las tareas Java de entidades/servicios/controladores incluyen `k-secure-coding` y `k-code-quality`?
- [ ] ¿Se crearon las tareas de tests unitarios si el diseño trae `test-unit-desc.md` (ver `tests-code.md`)?
- [ ] ¿El orden de numeración sigue las dependencias (empezando por las que no dependen de nadie; el orden por capas dominios → servicios → … → tests como fallback) **sin** detenerse por dependencias cruzadas?
- [ ] ¿Existe `implementation/tasks.md` con `type: implementation-tasks` y, por tarea, un checkbox **sin marcar** `- [ ]` + enlace correcto, en orden?
- [ ] Si existía `design/test-e2e-desc.md`: ¿se copió literalmente a `implementation/test-e2e-desc.md` sin modificarlo?
- [ ] ¿La respuesta lleva `ESCRITO: implementation/` + el bloque `=== TAREAS ===` con una línea por tarea?
