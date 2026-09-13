---
type: implementation-task
template: system
---

# Tarea 19 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.sistemaeducativo.db.Grado`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Sección concreta de `design/test-unit-desc.md` que describe esta clase: `## Clase: `com.educaflow.subsystem.sistemaeducativo.db.Grado`  —  entidad con campo calculado` (con todos sus sub-apartados `### Método: …`). Aplica además la sección `## Convenciones` del mismo fichero.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/GradoTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita (p.ej. la descripción dice
      `insert(X)` y el código tiene `guardarX(X, Long)`), o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más). En una clase
      que el diseño **modifica** (fila `Acción: Modificar` — ya existía antes de la iniciativa), este criterio se
      acota a la superficie **nueva/cambiada**: los métodos públicos **preexistentes** de la clase NO son motivo
      de BLOCKED (puedes leer el fichero real, o su `git diff`, para distinguirlos).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

Cubre `CC-Grado-001`: el getter `getAdmiteNivel()` de la entidad **generada** por Axelor a partir de `domains/Grado.xml` (tarea 02). El cuerpo del cálculo vive en el dominio XML, que es **contrato fijo**: si un test no cuadra con él, **MUST NOT** editarse el XML — reporta `BLOCKED`.

## Campos calculados (trazabilidad del `design.md`, verbatim)

### Campos calculados

| Campo | Origen spec | Ubicación | Momento |
|---|---|---|---|
| `Grado.admiteNivel` | `CC-Grado-001` | `domains/Grado.xml`, `<boolean name="admiteNivel" transient="true">` con cuerpo de cálculo sobre `niveles` | lectura (derivado, no persistido) |

## Nota y supuesto aplicable (verbatim)

7. **Niveles archivados.** `CC-Grado-001` dice literalmente «cierto si el grado tiene al menos un nivel en el catálogo de niveles». El diseño lo **ajusta** a «al menos un nivel **no archivado**» (ver `decisiones.md` D7). Motivo: el selector de nivel filtra por `domain="self.grado = :grado"` y Axelor no ofrece registros archivados, así que un grado con **todos** sus niveles archivados daría `admiteNivel = true`, mostraría el panel con el nivel marcado obligatorio, `V-Ciclo-001` lo exigiría y el usuario no tendría ninguno que elegir: un estado sin salida en el que ningún ciclo de ese grado se podría guardar. El ajuste es coherente con la intención de la regla («que el grado sepa si sus ciclos pueden llevar nivel»), no con su letra.
