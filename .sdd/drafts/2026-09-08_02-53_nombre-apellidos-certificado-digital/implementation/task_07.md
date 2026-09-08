---
type: implementation-task
template: system
---

# Tarea 07 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/test/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImplTest.java` | Modificar | k-sistemas (servicios.md) | Los tests existentes mockean `repository.findByDni(...)`, que desaparece; hay que reapuntarlos a `findByDniHabilitados(...)` y añadir los tests de las reglas nuevas (el detalle lo materializa `test-unit-desc.md`) |

## Tarea

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/criptografia/service/impl/CertificadoDigitalServiceImplTest.java`.
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

## Secciones concretas de `design/test-unit-desc.md` que describen esta clase

Ruta absoluta del fichero de descripción:
`/workspace/secretaria-virtual/.sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/design/test-unit-desc.md`

- `## Convenciones` (aplica a todo el fichero: JUnit 5 + Mockito, nomenclatura `metodo_condicion_resultadoEsperado`, aserciones de `org.junit.jupiter.api.Assertions`, datos de referencia, y las convenciones sobre `enabled`, «certificado por lo demás válido», el `dni` del bean entrante en `update`, «certificado de fichero legible» y `nombreTomadoDelUsuario`).
- `## Clase: com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl  —  servicio` **completa**, incluidos su «Responsabilidad», «Colaboradores a mockear», «Origen diseño» y **«Tests existentes a adaptar»**, y sus diez subsecciones `### Método:`:
  - `CertificadoDigital insert(CertificadoDigital certificado)`
  - `CertificadoDigital update(CertificadoDigital certificado, CertificadoDigital certificadoOriginal)`
  - `Optional<BusinessMessages> validateInsert(CertificadoDigital certificado)`
  - `Optional<BusinessMessages> validateUpdate(CertificadoDigital certificado, CertificadoDigital certificadoOriginal)`
  - `Optional<BusinessMessages> validateGetDatosTitularByDni(String dni)`
  - `DatosTitular getDatosTitularByDni(String dni)`
  - `AlmacenClave getAlmacenClaveByDni(String dni, String claveAcceso)`
  - `SituacionFirma getSituacionFirmaByDni(String dni)`
  - `AllowProperties allowPropertiesInsert()`
  - `AllowProperties allowPropertiesUpdate()`
- `## Cobertura` y `## Decisiones ante ambigüedades` (deciden qué se testea y cómo se resuelven los literales de mensaje, los helpers privados y la inyección por reflexión del `UserRepository`).

**Es una modificación de un test ya existente**, no un fichero nuevo: los tests preexistentes de `getAlmacenClaveByDni` y sus helpers de construcción se **conservan** y se adaptan según «Tests existentes a adaptar».

## Contexto del diseño relevante (verbatim)

11. **Los tests unitarios existentes se rompen y hay que adaptarlos.** `CertificadoDigitalServiceImplTest` mockea `repository.findByDni(...)` en una veintena de casos; al sustituirse el finder, esos mocks deben apuntar a `findByDniHabilitados(...)` (que devuelve un `Query<CertificadoDigital>`, así que el mock encadena `.fetchOne()`). El `UserRepository` inyectado como campo `@Inject` (Paso 4) se mockea igual que cualquier otro colaborador — es una de las razones para inyectarlo en vez de resolverlo con `JpaRepository.of(...)` dentro del helper. La descripción completa de los tests unitarios —los adaptados y los nuevos de V-001…V-005, R-001…R-003 y el helper de lectura `getCertificadoHabilitado`— la materializa `test-unit-desc.md` en la fase siguiente del pipeline.

Regla compleja referenciada por los tests de `insert` (**MUST** leerse en su fichero, no se copia aquí):
`/workspace/secretaria-virtual/.sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/design/rules/R-CertificadoDigital-001.md`
