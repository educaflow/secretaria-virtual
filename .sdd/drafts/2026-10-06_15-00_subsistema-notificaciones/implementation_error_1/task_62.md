---
type: implementation-task
template: system
---

# Tarea 62 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

## Tests unitarios de VerificacionHelper

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.tramites.util.verificacion.VerificacionHelper`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/tramites/util/verificacion/VerificacionHelperTest.java`.
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

**Sección de la descripción:** `design/test-unit-desc.md` § «## Clase: `com.educaflow.tramites.util.verificacion.VerificacionHelper`» (todas sus subsecciones `### Método:`). Aplica además la sección «## Convenciones» y «## Notas y decisiones del test-unitarios» del mismo fichero.

## Ficheros a crear o modificar (extracto del diseño)

Rutas relativas a `src/main/java/com/educaflow/` salvo que empiecen por `src/`, `agent_docs/` o `.claude/`.

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/tramites/util/verificacion/VerificacionHelperTest.java` | Modificar | — | Adaptado a `NotificacionService` (detalle en `test-unit-desc.md`) |

**Acción: `Modificar`** — `src/test/java/com/educaflow/tramites/util/verificacion/VerificacionHelperTest.java` ya existe: se edita conservando los tests preexistentes salvo los que la descripción ordena quitar, y se añaden/adaptan los que describe.

> **Nota del descomponedor:** los tests sustituidos de `correos`/`sms` que `test-unit-desc.md` cita como referencia de estilo se borraron en la tarea 07; se leen con `git show HEAD:<ruta>` (solo lectura).
