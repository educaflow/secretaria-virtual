# Modelo: TareaFirma

**Modelo existente:** sí

La tarea de firma es el encargo de firmar uno o varios documentos que se le hace a una persona concreta, su firmante, que la resuelve firmándolos o rechazándolos.

Lo que cambia en esta iniciativa: hasta ahora la firma la producía siempre AutoFirma en el ordenador del firmante y la tarea se limitaba a recoger el resultado. Ahora la tarea gana una forma nueva de resolverse, **firmar en el servidor**, que la secretaría virtual usa cuando el firmante tiene un certificado digital dado de alta para su DNI. Para ello el modelo necesita dos datos nuevos que solo viven mientras se está firmando: en qué **situación de firma** está el firmante —que es lo que decide qué se le ofrece en pantalla— y la **clave de firma** que teclea cuando hay que pedírsela.

El ciclo de vida de la tarea (pendiente de firmar → firmada o rechazada) **no cambia**: firmar en el servidor es otra manera de llegar al mismo estado «Firmado».

## Campos

- **clave de firma** — el PIN del dispositivo criptográfico o la contraseña del fichero del certificado que el firmante teclea en el momento de firmar, cuando la secretaría virtual no lo tiene guardado. **No se guarda nunca**: se usa para esa firma y se descarta.

## Campos calculados

- CC-TareaFirma-001 — situación de firma del firmante
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: se deduce del DNI del firmante y del certificado digital habilitado que la secretaría virtual tenga dado de alta para ese DNI, y toma uno de estos seis valores: **sin DNI** (el firmante no tiene DNI en su ficha, o el que tiene no es válido); **sin certificado** (tiene DNI, pero no hay ningún certificado habilitado dado de alta para él); **dispositivo con PIN guardado**; **dispositivo sin PIN guardado**; **fichero con contraseña guardada**; **fichero sin contraseña guardada**.

## Acción: Crear

**Input AllowProperties:** (ninguna — las tareas de firma no se dan de alta desde la interfaz; las crea el proceso que necesita la firma)

## Acción: Modificar

**Input AllowProperties:** (ninguna — la tarea solo se cambia mediante sus acciones propias de firmar o rechazar, nunca guardando el formulario)

## Acción: Firmar en el servidor

Es la acción nueva. Firma en el servidor **todos** los documentos de la tarea con el certificado digital del firmante y deja la tarea resuelta.

**Input AllowProperties:** clave de firma

**Validaciones:**

- VAL-TareaFirma-001 — La tarea está en estado «Pendiente de firmar»
  - mensaje: "Solo se pueden firmar las tareas pendientes de firmar"
- VAL-TareaFirma-002 — El firmante de la tarea es el usuario que está firmando
  - mensaje: "Solo puede firmar los documentos la persona a la que se le han encargado"
- VAL-TareaFirma-003 — El firmante tiene un DNI válido en su ficha
  - mensaje: "No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador."
- VAL-TareaFirma-004 — El firmante tiene un certificado digital habilitado dado de alta para su DNI
  - mensaje: "No es posible firmar en el servidor porque no tiene un certificado digital dado de alta"
- VAL-TareaFirma-005 — La clave de firma está indicada
  - condición: la situación de firma del firmante es «dispositivo sin PIN guardado»
  - mensaje: "El PIN es obligatorio"
- VAL-TareaFirma-006 — La clave de firma está indicada
  - condición: la situación de firma del firmante es «fichero sin contraseña guardada»
  - mensaje: "La contraseña es obligatoria"
- VAL-TareaFirma-007 — La tarea tiene al menos un documento que firmar
  - mensaje: "La tarea de firma no tiene ningún documento que firmar"

La situación de firma con la que se decide **la comprueba siempre el servidor en el momento de firmar**, no la que tuviera pintada la pantalla: si entre que el firmante abrió la pantalla y pulsó el botón esa situación ha cambiado (le han dado de alta un certificado, se lo han deshabilitado, le han quitado el PIN guardado), estas validaciones se evalúan sobre la situación real y, si ya no se puede firmar en el servidor, la acción se cancela con su mensaje de error.

**Reglas de negocio:**

- RN-TareaFirma-001 — Firmar en el servidor cada uno de los documentos de la tarea con el certificado digital del firmante, colocando la firma en el recuadro y la página que indica la propia tarea, y guardar el resultado como versión firmada de cada documento
  - fase: antes_de_commit
- RN-TareaFirma-002 — Si la firma de cualquiera de los documentos falla, cancelar la operación entera: ningún documento de la tarea queda firmado, la tarea sigue pendiente de firmar y el firmante puede volver a intentarlo
  - fase: antes_de_commit
- RN-TareaFirma-003 — Una vez firmados todos los documentos, dejar la tarea en estado «Firmado» con la fecha de resolución del momento
  - fase: antes_de_commit
- RN-TareaFirma-004 — Descartar la clave de firma recibida en cuanto termina la firma, sin guardarla en ningún sitio ni dejar rastro de ella
  - fase: antes_de_commit
- RN-TareaFirma-005 — Avisar al proceso que encargó la firma de que la tarea ha quedado resuelta, igual que cuando se firma con AutoFirma
  - fase: después_de_commit
- RN-TareaFirma-006 — Cuando la secretaría virtual ya tiene guardada la clave del certificado del firmante, firmar con esa clave guardada e **ignorar cualquier clave de firma que llegue de la pantalla**; la clave que teclea el firmante solo se usa cuando no hay ninguna guardada
  - fase: antes_de_commit
  - condición: la situación de firma del firmante es «dispositivo con PIN guardado» o «fichero con contraseña guardada»
- RN-TareaFirma-007 — Cuando la firma de alguno de los documentos falla, devolver al firmante el motivo del fallo en un mensaje que empieza por «No se han podido firmar los documentos:», para que sepa qué corregir antes de volver a intentarlo
  - fase: antes_de_commit
  - estado: Pendiente de firmar
  - condición: la firma de alguno de los documentos de la tarea falla
- RN-TareaFirma-008 — Descartar la clave de firma recibida **también cuando la firma no llega a completarse** —porque una validación la rechaza o porque falla la firma de algún documento—, sin guardarla en ningún sitio
  - fase: antes_de_commit
  - condición: la acción termina sin firmar los documentos
