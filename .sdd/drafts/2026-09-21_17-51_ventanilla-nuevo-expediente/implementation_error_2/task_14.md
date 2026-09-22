---
type: implementation-task
template: system
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.system.ventanilla.controller.AsistenteNuevoExpedienteController`.

La sección concreta que los describe es **«Clase: com.educaflow.system.ventanilla.controller.AsistenteNuevoExpedienteController  —  controlador»** de
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/test-unit-desc.md`. Aplican además, de ese
mismo fichero, la sección **«Convenciones»**, la sección **«Fixtures comunes de
`AsistenteNuevoExpedienteServiceImpl` y del controlador»** (incluido el stub del oráculo del motor con
`thenAnswer`) y las **«Notas y supuestos»** del final.

- Son **17 tests** sobre sus 4 `@CallMethod`. Los tests del controlador declaran `Verifica: —`: fijan el tramo común (whitelist → validador → error o acción) y la forma exacta de la respuesta que leen las vistas.
- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/system/ventanilla/controller/AsistenteNuevoExpedienteControllerTest.java`.
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
- **MUST NOT** convertir en tests las reglas `U-` (UI/cliente): esas se verifican como E2E en
  `test-e2e-desc.md`, no aquí.

## Sección «Tests» del diseño (verbatim)

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).
- **Tests E2E**: descritos en `test-e2e-desc.md`, 26 tests (`T-001`…`T-026`) que cubren los 25 escenarios del spec. La carpeta destino es `src/test/e2e/system/ventanilla/`, que aún no existe, por eso la numeración arranca en `T-001`.

---
