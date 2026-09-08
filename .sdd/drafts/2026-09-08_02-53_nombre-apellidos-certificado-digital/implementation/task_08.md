---
type: implementation-task
template: system
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

## Fichero a crear

`src/test/java/com/educaflow/subsystem/criptografia/controller/CertificadoDigitalControllerTest.java` (**Crear**).

> **Decisión documentada (ejecución desatendida, sin usuario a quien preguntar).** La tabla «Ficheros a crear o modificar» del `design.md` solo lista el test de `CertificadoDigitalServiceImpl`, pero `design/test-unit-desc.md` describe **dos** clases de producción con lógica testable (`## Cobertura`: «Clases con lógica descritas: 2»), y la segunda es `CertificadoDigitalController` con su método `getDatosTitularByDni` y sus seis tests. El contrato de descomposición (`tests-code.md` §2) manda crear **una tarea por clase de producción que `test-unit-desc.md` describe**, así que esta tarea existe. El fichero de test no aparece en la tabla porque la clase de producción es nueva y su test no existía; su ubicación se deriva de `tests-code.md` §1 (mismo paquete que la clase bajo test, bajo `src/test/...`).

## Tarea

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/criptografia/controller/CertificadoDigitalControllerTest.java`.
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

`CertificadoDigitalController` es una clase **nueva** (fila `Acción: Crear` del diseño), así que el criterio de «superficie de más» se le aplica **completo**: su única superficie pública admisible es el `@CallMethod getDatosTitularByDni(ActionRequest, ActionResponse)` que describe el Paso 5.

## Secciones concretas de `design/test-unit-desc.md` que describen esta clase

Ruta absoluta del fichero de descripción:
`/workspace/secretaria-virtual/.sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/design/test-unit-desc.md`

- `## Convenciones` (aplica a todo el fichero: JUnit 5 + Mockito, nomenclatura `metodo_condicion_resultadoEsperado`, aserciones de `org.junit.jupiter.api.Assertions`, datos de referencia).
- `## Clase: com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController  —  controlador` **completa**, incluidos su «Responsabilidad», «Colaboradores a mockear» (con la exigencia de que el mapa del contexto lleve **siempre** la clave `_model`) y «Origen diseño», y su subsección `### Método: void getDatosTitularByDni(ActionRequest actionRequest, ActionResponse actionResponse)` con sus seis tests.
- `## Cobertura` y `## Decisiones ante ambigüedades` (en particular la decisión 4, sobre la inyección por reflexión de los campos `@Inject` con un helper `setField`).

**MUST NOT** convertir en tests las reglas `U-` (UI/cliente): esas se verifican como E2E en `test-e2e-desc.md`, no aquí. Por eso los seis tests de esta clase llevan `Verifica: —`.
