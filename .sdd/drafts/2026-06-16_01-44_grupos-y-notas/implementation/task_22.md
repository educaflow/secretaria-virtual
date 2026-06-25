---
type: implementation-task
---

# Tarea 22 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase com.educaflow.system.gruposnotas.db.AlumnoGrupo (getter `getNotaMedia()`, lógica de CC-001).

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno. La sección concreta es **"Clase: `com.educaflow.system.gruposnotas.db.AlumnoGrupo` — getter `getNotaMedia()`"** de `design/test-unit-desc.md`.
- Ubicación de salida: `src/test/java/com/educaflow/system/gruposnotas/db/AlumnoGrupoTest.java` (mismo paquete que la clase generada por Axelor).
- Stack: JUnit 5/Jupiter + Mockito.
- Excepción justificada a "no se testean POJOs de dominio": este getter contiene la lógica de CC-001 (cálculo
  INLINE de la nota media), así que se testea como clase con lógica. La entidad `AlumnoGrupo` se instancia con
  `new` (sin mocks) y las `Nota` se crean con `new` y se asocian con setters/colección.
- Las clases de producción ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita, o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente (ni reinterpretar a qué método apuntan): esa divergencia
  es un fallo previo del implementador que decide el motor/usuario, no algo que el generador de tests deba tapar.

> Casos a cubrir (todos verifican CC-001): sin módulos evaluados → "Sin nota" (ESC-008); una nota numérica → ese valor (NOTA_8 → "8", ESC-006); MATRICULA_HONOR + NO_EVALUADO → "10" (MH cuenta 10, NO_EVALUADO se excluye, ESC-007); MATRICULA_HONOR (10) + NOTA_7 → "9" (media 8,5 redondeada, ESC-007 paso 5); NOTA_8 + NOTA_6 → "7" (ESC-014); colección de notas nula/vacía → "Sin nota" sin NPE.
