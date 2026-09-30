---
name: developer-reduce-crap
description: Baja por debajo de la constante `crapUmbral` del `build.gradle` el CRAP de los 5 métodos con CRAP más alto del proyecto, según el informe `build/reports/crap/crap.csv` que genera `./gradlew -q crapCheck`. Por cada método escribe tests unitarios de caracterización y, cuando los tests no bastan (CRAP mínimo alcanzable = CC), reduce su complejidad ciclomática con las técnicas de `estrategias-cc.md` sin cambiar el comportamiento ni las firmas. La salida son tests nuevos en `src/test/java`, el código refactorizado y un informe antes→después por método, que empieza con una tabla markdown de todos los métodos que superan el umbral (con enlace a cada uno) y termina con la de los métodos tratados y sus valores nuevos.
allowed-tools: Read, Write, Edit, Bash, Skill, Agent, mcp__intellij-index__ide_find_references, mcp__intellij-index__ide_search_text
---

# developer-reduce-crap

Asumes el rol de **reductor de CRAP**: transformas los métodos peor puntuados del informe CRAP en métodos con CRAP ≤ `crapUmbral`, subiendo su cobertura con tests unitarios y, si hace falta, bajando su complejidad ciclomática (CC).

---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).
Argumentos esperables:

- Vacío → los 5 métodos con CRAP más alto que superen el umbral.
- `--top=<N>` → los N métodos con CRAP más alto que superen el umbral, en lugar de 5.

---

## Outline

1. **Medir** el estado inicial y elegir los métodos (Fase 0).
2. **Reducir** cada método en un subagente, uno detrás de otro (Fase 1).
3. **Verificar** el conjunto: CRAP, CPD y tests (Fase 2).
4. **Informar** antes→después (Fase 3).

**STOP conditions**:

- En la Fase 0, tras `crapCheck`, `build/reports/crap/crap.csv` no existe (§2.2) → **ERROR**: muestra el error y detente. **MUST NOT** arreglar tests ajenos para poder medir.
- No se encuentra `def crapUmbral` en el `build.gradle` → **ERROR** y detente.
- Ningún método supera el umbral → informa y termina sin tocar nada.
- Un subagente devuelve `BLOQUEADO` → **STOP**: lleva el motivo al usuario antes de seguir con el siguiente método.
- En la Fase 2 falla la compilación o algún test, un método tratado pierde cobertura o sube de CRAP, queda por encima del umbral algún identificador que no estaba en el conjunto guardado en la Fase 0 (salvo los `Métodos nuevos` de un PENDIENTE, incluidos los reclasificados en §4.2 o en la Fase 2 3.1) o `cpdCheck` falla, por código que ha introducido este skill → arréglalo (**LIMIT**: 2 intentos) o **STOP** y avisa al usuario.

---

## 1. Entrada y salida

### 1.1 Entrada

- `build.gradle`: la constante `crapUmbral` y las tareas `crapCheck` y `cpdCheck`.
- `build/reports/crap/crap.csv`, que genera `./gradlew -q crapCheck`, con estas columnas: `fichero,linea,clase,metodo,descriptor,cc,coberturaInstrucciones,crap,instruccionesCubiertas,instruccionesTotales,lineasCubiertas,lineasTotales,ramasCubiertas,ramasTotales`.

### 1.2 Salida

- Tests unitarios nuevos o ampliados en `src/test/java`, en el mismo paquete que la clase probada y con nombre `<Clase>Test` (ampliar el que exista).
- Código de `src/main/java` refactorizado cuando haya que bajar la CC.
- Tabla de todos los métodos por encima del umbral al empezar (Fase 0) y la de los métodos tratados con sus valores nuevos al terminar (Fase 3), según §2.5.
- Informe final en la conversación (plantilla en §6).

---

## 2. Principios

### 2.1 Cómo se calcula

- `CRAP = CC² × (1 − cobertura)³ + CC`, con la CC y la cobertura de instrucciones de JaCoCo medidas **solo** con los tests unitarios.
- Con cobertura 100 % el CRAP vale exactamente la CC, así que el **CRAP mínimo alcanzable es la CC**.
- **MUST** decidir la estrategia con esta cuenta, antes de escribir nada:
  - CC ≤ `crapUmbral` → basta con tests (calcula qué cobertura hace falta).
  - CC > `crapUmbral` → **MUST** bajar la CC; los tests solos nunca bastan.
- Un método troceado deja de ser uno: **cada** método resultante, incluidas las lambdas sintéticas (`lambda$<metodo>$N`, o `<metodo>$lambda$N` en Kotlin), **MUST** quedar ≤ `crapUmbral`.

### 2.2 Cómo se mide

- **MUST** leer el umbral del `build.gradle`, nunca asumir un valor:
  ```bash
  grep -oP 'def crapUmbral = \K[0-9.]+' build.gradle
  ```
- **MUST** borrar el CSV con `rm -f build/reports/crap/crap.csv` antes de cada `./gradlew -q crapCheck`. Si tras la tarea no existe, ha fallado la compilación o algún test, y **MUST NOT** leerse ninguna cifra.
- **MUST** lanzar `./gradlew -q crapCheck` y leer el CSV **aunque la tarea termine con error**: falla siempre que quede algún método del proyecto por encima del umbral, y eso es lo esperado.
- **MUST** identificar y comparar las filas del CSV por `clase` + `metodo` + `descriptor`, con el formato `<clase>#<metodo><descriptor>` (sin espacios), nunca por `linea` (cambia al editar).
  - ✅ CORRECTO: `com.educaflow.base.util.DniUtil#isValid(Ljava/lang/String;)Z`
  - ❌ INCORRECTO: `DniUtil.java:89` (la línea se mueve al añadir código encima)
- Las lambdas se comparan sin sufijos numéricos, porque el compilador las renumera al editar la clase:
  - Java: `<clase>#lambda$<metodo>$` + descriptor.
  - Kotlin: `<clase>#<metodo>$lambda$` + descriptor, quitando todos los `$N` finales.
- Comandos para leer el CSV:
  ```bash
  # un método y sus lambdas
  grep -F ',<clase>,' build/reports/crap/crap.csv | grep -E ',(<metodo>|lambda\$<metodo>\$[0-9]+|<metodo>\$lambda(\$[0-9]+)+),'
  ```
- **CRITICAL**: **MUST NOT** lanzar dos Gradle a la vez sobre el mismo árbol: se pisan `build/`. Todo el skill es secuencial.

### 2.3 Qué no se puede hacer

Trampas que bajan la métrica sin bajar el riesgo. **MUST NOT**:

- Subir `crapUmbral`, excluir clases de JaCoCo o de `crapCheck`, ni añadir `// CPD-OFF`.
- Escribir tests sin asserts, o que solo ejecutan el código para sumar cobertura.
- Mover lógica a lambdas o a métodos privados solo para esconder la CC, cuando el trozo no tiene sentido propio.
- Cambiar la firma de un método público o protegido, sus argumentos o sus constructores, ni el comportamiento observable.
- Convertir una validación de usuario (`BusinessMessages`, `BusinessException`) en `Objects.requireNonNull`/`checkArgument`, ni al revés: estos solo sirven para los asserts de la aplicación (lo que "nunca debería darse").
- Tocar código generado (`*.db.*`, `States`) ni ficheros de `build/`.
- Borrar campos, loggers o servicios "sin usar" de la clase: son idiomas del proyecto.

### 2.4 Skills que se cargan

- `/k-code-quality` → **siempre** que se refactorice (troceo, nombres, idiomas Java).
- `/k-secure-coding` → si el método está en una entidad, un servicio o un controlador.
- `/k-tipo-expediente` → si el fichero está bajo una carpeta de versión de un trámite (`tramites/<tramite>/<vN>/`).

### 2.5 Tablas de ranking

Las dos tablas las genera `tabla-crap.py` (en la carpeta de este skill): markdown alineado en columnas, métricas a la derecha, de más a menos CRAP y cada método como enlace `fichero:linea` a su definición.
**MUST** mostrar su salida tal cual, sin rehacerla a mano, sin recortar filas ni quitar el enlace.
`<N>` vale 5, o el valor de `--top`.

- Tabla inicial (`#`, Método, CRAP, CC, Cobertura): **todos** los métodos con `crap` > `crapUmbral` del CSV recién medido, para ver de un vistazo cuánto código está mal:
  ```bash
  python3 .claude/skills/developer-reduce-crap/tabla-crap.py build/reports/crap/crap.csv <umbral>
  ```
- Tabla final: solo los métodos tratados, que son las `<N>` primeras filas de la inicial, con 3 columnas más (CRAP nuevo, CC nuevo, Cobertura nueva), emparejados por identificador (§2.2) y con el enlace a la línea nueva:
  ```bash
  python3 .claude/skills/developer-reduce-crap/tabla-crap.py "${TMPDIR:-/tmp}/developer-reduce-crap-antes.csv" build/reports/crap/crap.csv <umbral> <N>
  ```

---

## 3. Fase 0 — Medir

1. Lee `crapUmbral` (§2.2). Si no aparece → **ERROR**.
2. Lanza `./gradlew -q crapCheck`. Si `build/reports/crap/crap.csv` no existe (§2.2) → **ERROR** (STOP conditions).
   2.1 Guarda una copia para la tabla final: `cp build/reports/crap/crap.csv "${TMPDIR:-/tmp}/developer-reduce-crap-antes.csv"`.
   2.2 Muestra la tabla inicial (§2.5) **nada más medir**, antes que cualquier otro texto.
3. Quédate con las primeras 5 filas de la tabla inicial (**LIMIT**: 5, o el valor de `--top`). **MUST** ser esas filas y en ese orden, también en los empates de CRAP, para que la tabla final muestre los mismos métodos.
   3.1 Si hay menos, trabaja solo con esos.
   3.2 Si la tabla sale vacía (ningún método supera el umbral) → informa y termina.
4. Guarda para cada uno la fila **antes** (CC, cobertura, CRAP) y su identificador `<clase>#<metodo><descriptor>` (§2.2). Guarda además el conjunto de identificadores de **todas** las filas con `crap` > `crapUmbral`.
5. Di debajo de la tabla inicial qué métodos vas a tratar (por su `#`) y continúa sin esperar respuesta.

---

## 4. Fase 1 — Reducir cada método

**CRITICAL**: lanza **un subagente con `Agent` por método, uno detrás de otro**, esperando el resultado de cada uno antes de lanzar el siguiente.
**MUST NOT** lanzarlos en paralelo ni con `run_in_background` (todos usan Gradle sobre el mismo árbol).
Los subagentes **MUST NOT** usar `AskUserQuestion`: una duda que bloquea se devuelve como `BLOQUEADO`.

### 4.1 Prompt del subagente

Rellena solo los placeholders de las líneas `Método`, `Fichero`, `Antes` y `Umbral`; el resto del bloque, incluidas las plantillas de respuesta, pásalo tal cual:

```text
Tienes que bajar el CRAP de un método Java/Kotlin del proyecto por debajo del umbral, subiendo su cobertura con tests unitarios y, si hace falta, bajando su complejidad ciclomática.

Método: <clase>#<metodo><descriptor>
Fichero: <fichero>
Antes: CC=<cc> cobertura=<cobertura> CRAP=<crap>
Umbral (crapUmbral): <umbral>

Lee y aplica las secciones §2.1, §2.2, §2.3 y §2.4 de .claude/skills/developer-reduce-crap/SKILL.md y las técnicas de .claude/skills/developer-reduce-crap/estrategias-cc.md.

Pasos:
1. Lee el método, quién lo llama y sus tests existentes (el test de la clase: su nombre simple seguido de `Test`, en el mismo paquete de src/test/java).
2. Decide la estrategia con la cuenta de §2.1: solo tests, o tests + bajar la CC.
3. Escribe PRIMERO tests de caracterización del comportamiento ACTUAL, que recorran cada rama y con asserts reales (JUnit 5 + Mockito; `Mockito.mockStatic` para `SecurityUtil`). Si un test destapa un bug, NO lo arregles: fija el comportamiento actual y anótalo en el resumen.
4. Si hay que bajar la CC, aplica las técnicas de estrategias-cc.md empezando por las más baratas y locales (1-8, 10-12); trocea el método (técnica 9) solo si tiene partes claramente independientes. Los tests ya escritos MUST seguir verdes sin modificarlos; sí puedes añadir tests nuevos (p.ej. para los métodos extraídos).
5. Lanza `./gradlew -q crapCheck` (nunca en paralelo con otro Gradle) y busca en build/reports/crap/crap.csv la fila del método y las de sus lambdas y métodos extraídos.
6. Comprueba: la cobertura es igual o mayor (mayor si antes no era 100 %), el CRAP ha BAJADO, y el método y todos los que han salido de él tienen CRAP <= umbral. Si no se cumple, vuelve al paso 3 (añade los tests que falten para las ramas sin cubrir) y, si la estrategia incluye bajar la CC, al 4. LIMIT: 3 iteraciones. Si tras 3 iteraciones el código compila, los tests pasan, la cobertura no ha bajado y el CRAP no ha subido respecto a «Antes», pero no se cumple alguna de las condiciones de REDUCIDO de este paso, devuelve PENDIENTE. MUST NOT devolver un método con menos cobertura o más CRAP que antes: deshaz primero el cambio que lo empeora. Devuelve BLOQUEADO solo si no consigues que compile o que los tests pasen, o si necesitas una decisión del usuario. MUST NOT devolver REDUCIDO ni PENDIENTE con el código sin compilar o con tests en rojo. Antes de devolver BLOQUEADO, deja el árbol compilando y con los tests en verde (deshaz tus cambios en src/main/java si hace falta y conserva solo los tests que pasen) e indícalo en Motivo.

Responde SOLO con uno de estos formatos (la primera línea es literal):

REDUCIDO <clase>#<metodo><descriptor>
Después: CC=<cc> cobertura=<cobertura> CRAP=<crap>
Métodos nuevos: <<clase>#<metodo><descriptor> CRAP=<n>, … | ninguno>
Técnicas: <números de estrategias-cc.md aplicados | solo tests>
Tests: <rutas de los ficheros de test creados o ampliados>
Notas: <bugs destapados, decisiones | ninguna>

PENDIENTE <clase>#<metodo><descriptor>
Después: CC=<cc> cobertura=<cobertura> CRAP=<crap>
Métodos nuevos: <<clase>#<metodo><descriptor> CRAP=<n>, … | ninguno>
Técnicas: <números de estrategias-cc.md aplicados | solo tests>
Motivo: <qué condición de REDUCIDO no se cumple tras 3 iteraciones (umbral, CRAP no bajado o cobertura no subida) y por qué>
Tests: <rutas>
Notas: <bugs destapados, decisiones | ninguna>

BLOQUEADO <clase>#<metodo><descriptor>
Motivo: <qué no compila o qué tests fallan, o qué decisión hace falta del usuario; qué has deshecho para dejar el árbol en verde>
Tests: <rutas de los ficheros de test conservados | ninguno>
```

### 4.2 Contrato de respuesta

- ✅ CORRECTO: `REDUCIDO com.educaflow.base.util.DniUtil#isValid(Ljava/lang/String;)Z`
- ✅ CORRECTO: `PENDIENTE com.educaflow.base.infrastructure.mapper.BeanMapperModel#copyMapToEntity(Ljava/lang/Class;Ljava/util/Map;Lcom/axelor/db/Model;Lcom/axelor/db/modelservice/AllowProperties;Ljava/lang/String;Lcom/axelor/db/Model;Lcom/educaflow/base/infrastructure/mapper/InstanceModelList;)V`
- ❌ INCORRECTO: `He terminado, el método ya está bien` (sin token literal: no se puede comparar)
- ❌ INCORRECTO: pegar el código de los tests en la respuesta (ya está en disco; gasta el contexto del orquestador)

Al recibir cada respuesta:

1. `REDUCIDO` → comprueba tú en el CSV (§2.2) la fila del método, la de cada identificador de `Métodos nuevos` y las de sus lambdas. Si alguna no cuadra con lo declarado o supera `crapUmbral`, trátalo como `PENDIENTE` con motivo "la medición no confirma el resultado".
2. `PENDIENTE` → anótalo y sigue con el siguiente método.
3. `BLOQUEADO` → **STOP** (STOP conditions). Si el usuario decide seguir, anótalo como BLOQUEADO con su motivo.

---

## 5. Fase 2 — Verificar el conjunto

1. Lanza `./gradlew -q crapCheck`.
2. Comprueba que `build/reports/crap/crap.csv` existe: si no existe (§2.2), ha fallado la compilación o algún test (el código de salida de `crapCheck` no lo distingue). **MUST NOT** comparar cifras hasta arreglarlo (paso 6).
3. Compara con la medición de la Fase 0:
   3.1 Un REDUCIDO cuyo método, o alguno de sus `Métodos nuevos` o lambdas, ya no está ≤ `crapUmbral` pasa a PENDIENTE en el informe.
   3.2 Un método tratado (REDUCIDO o PENDIENTE) con menos cobertura o más CRAP que antes es una regresión de este skill.
   3.3 **MUST NOT** haber por encima del umbral ningún identificador, existente o extraído, que no estuviera en el conjunto guardado en la Fase 0 (las lambdas, con la clave sin sufijo de §2.2), salvo los métodos declarados en `Métodos nuevos` de un PENDIENTE, incluidos los reclasificados en §4.2 o en 3.1, que se listan en «Pendientes y bloqueados» del informe.
4. Lanza `./gradlew -q cpdCheck`: el código extraído no puede crear duplicados.
5. Si todo cuadra, pasa a la Fase 3.
6. Si falla el paso 2, 3.2, 3.3 o 4 por algo introducido por este skill → corrígelo y vuelve al paso 1. **LIMIT**: 2 intentos; si persiste → **STOP** y avisa al usuario.

---

## 6. Fase 3 — Informe

Devuelve exactamente esta plantilla, con la tabla final de §2.5 (medida en la Fase 2) en `<tabla final>`:

```markdown
## Reducción de CRAP (umbral <umbral>)

<tabla final>

| Método | CC | Cobertura | CRAP | Estado | Técnicas |
|---|---|---|---|---|---|
| [`<Clase>.<metodo>`](<fichero>:<linea>) | <antes> → <después> | <antes> → <después> | <antes> → <después> | REDUCIDO / PENDIENTE / BLOQUEADO | <números o "solo tests"> |

**Métodos nuevos** (troceos): <[`Clase.metodo`](<fichero>:<linea>) CRAP n, … | ninguno>

**Tests añadidos**:
- `<ruta>`

**Pendientes y bloqueados**:
- [`<Clase>.<metodo>`](<fichero>:<linea>): <motivo>

**Notas** (bugs destapados, decisiones): <… | ninguna>

**Verificación final**: crapCheck <quedan N métodos del proyecto sobre el umbral> · cpdCheck <OK/FALLA> · tests <OK/FALLA>
```

En las filas BLOQUEADO, «después» es la fila medida en la Fase 2 y Técnicas lleva `—`.
Cada método es un enlace a su definición, con `fichero` y `linea` de la fila del CSV de la Fase 2.

---

## Quick Guidelines

- El CRAP mínimo alcanzable es la CC: con CC > umbral hay que refactorizar, los tests no bastan.
- Umbral siempre leído de `crapUmbral` en el `build.gradle`; métodos identificados por clase + nombre + descriptor.
- Primero tests de caracterización del comportamiento actual, luego refactor; los tests no se tocan al refactorizar.
- Técnicas baratas y locales antes que trocear; trocear solo lo que es independiente.
- Ninguna técnica cuenta hasta verla en el CSV.
- Un Gradle cada vez: subagentes secuenciales, nunca en paralelo.
- Tabla de ranking (§2.5) al empezar, con todos los métodos sobre el umbral, y al terminar, con los tratados y sus valores nuevos; métodos siempre como enlace `fichero:linea`.
- Nada de trampas (§2.3): ni tocar el umbral, ni tests sin asserts, ni cambiar firmas.

