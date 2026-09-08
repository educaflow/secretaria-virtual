# Decisiones tomadas durante la implementación (ejecución desatendida)

Este fichero recoge las decisiones que el skill `/sdd-implementer` habría consultado con el
usuario, resueltas según las instrucciones de la ejecución desatendida, más las dudas técnicas
que los subagentes resolvieron por su cuenta.

## Política aplicada

- **CONFLICT** (el fichero destino ya existe y NO estaba previsto por el diseño): se **MANTIENE
  siempre el fichero existente**, nunca se sobrescribe. Cada caso se anota abajo.
- Los ficheros que el propio diseño declara con acción **Modificar** (o cuya versión materializada
  el contrato manda colocar) **no** son un CONFLICT: aplicarlos es el comportamiento esperado.
- **Duda técnica** no prevista: se elige la opción más conservadora y reversible y se anota abajo.
- **Duda funcional** (qué debería hacer la funcionalidad): no se resuelve, es un STOP.

## Conflictos de fichero (CONFLICT)

Ninguno hasta el momento. Ningún subagente implementador ha devuelto el token `CONFLICT`.

## Dudas técnicas resueltas por los subagentes

### D-01 — Javadoc del controlador: matiz sobre qué campos recalcula el servidor (task_04)

- **Duda**: el Javadoc de `CertificadoDigitalController` (texto heredado **verbatim** del diseño)
  afirma que el servidor recalcula al guardar los tres valores devueltos por
  `getDatosTitularByDni`. Verificado contra `CertificadoDigitalServiceImpl`, eso es exacto solo
  para `nombreTomadoDelUsuario`: `nombre` y `apellidos` están en `allowPropertiesInsert` /
  `allowPropertiesUpdate` y `fireActionRule_AsignarTitular` solo los sobrescribe en la rama
  `datos.tomadoDelUsuario()`, así que sin usuario coincidente se conservan los valores del cliente
  (RN-CertificadoDigital-002).
- **Opción elegida (conservadora y reversible)**: **dejar el texto tal cual**, sin reescribirlo.
- **Motivo**: la redacción reproduce la prosa **verbatim** del diseño, que es contrato fijo.
  Corregir el matiz es una enmienda al **texto del diseño**, no a la implementación, y hacerla aquí
  desincronizaría el código respecto del diseño. Es un MINOR de documentación: no afecta al
  comportamiento, y queda anotado para que `/sdd-designer` lo ajuste si procede.

### D-02 — Tarea de test del controlador no listada en `design.md` (task_08, decidida en la descomposición)

- **Duda**: `design.md` solo lista el test del `ServiceImpl`, pero `test-unit-desc.md` describe dos
  clases con lógica.
- **Opción elegida**: crear también la tarea de test del controlador, en el paquete espejo.
- **Motivo**: `tests-code.md` §2 del contrato de la plantilla obliga a una tarea de test por clase
  con lógica. Queda documentado dentro de la propia `task_08.md`.
