# Estados y transiciones

## Estados

| Estado | Título | Fase | Perfil con el turno | Inicial | Cerrado | ¿Se puede borrar? |
|---|---|---|---|---|---|---|
| F_ENTRADA_DATOS | Entrada de datos | F_ENTRADA | CREADOR | sí | no | sí |
| F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA | Pendiente de presentación | F_ENTRADA | CREADOR | no | no | no |
| F_SALIDA_PENDIENTE_RESOLUCION | Pendiente de resolución | F_SALIDA | RESPONSABLE | no | no | no |
| F_SALIDA_PENDIENTE_FIRMA_DIRECTOR | Pendiente de firma del director | F_SALIDA | — | no | no | no |
| F_TERMINADO_RESPONDIDO | Respondido | F_TERMINADO | — | no | sí | no |

### F_SALIDA_PENDIENTE_RESOLUCION

La instancia está presentada y espera la decisión del secretario. Se entra aquí tanto en la primera presentación como al re-presentar tras una subsanación — por eso el aviso es un efecto **de entrada**, no de una transición concreta.

**Efectos al entrar:**

- RN-F_SALIDA_PENDIENTE_RESOLUCION-001 — Avisar por correo al secretario del centro de que tiene una instancia pendiente de resolver
  - fase: después_de_commit

### F_SALIDA_PENDIENTE_FIRMA_DIRECTOR

Estado de **espera**: la respuesta está redactada y aguarda la firma del director. Nadie tiene el turno; el expediente avanza solo cuando la firma se completa (TR-005).

**Efectos al entrar:**

- RN-F_SALIDA_PENDIENTE_FIRMA_DIRECTOR-001 — Poner el documento de la respuesta a firmar al director en el portafirmas (FIR-respuesta-001)
  - fase: después_de_commit

## Creación del expediente

El expediente nace en F_ENTRADA_DATOS cuando el alumno elige el trámite en el árbol. El sistema precarga la persona solicitante e interesada (el alumno que lo crea), su DNI como DNI de firma del expediente y el centro. La creación no recibe datos del usuario.

## Transición: TR-001 — GUARDAR_DATOS: F_ENTRADA_DATOS → F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA

**Disparador:** botón

**Campos editables:** exposición, solicitud, los adjuntos (alta, edición y borrado)

**Validaciones:**

- VAL-TR-001-001 — La exposición está indicada
  - mensaje: "La exposición es obligatoria"
- VAL-TR-001-002 — La solicitud está indicada
  - mensaje: "La solicitud es obligatoria"

**Efectos:**

- RN-TR-001-001 — Generar el documento de la instancia (documento-instancia) con los datos introducidos y guardarlo en el expediente (se regenera en cada paso por esta transición: tras volver atrás o tras una subsanación)
  - fase: antes_de_commit

## Transición: TR-002 — VOLVER_A_DATOS: F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA → F_ENTRADA_DATOS

**Disparador:** botón

**Campos editables:** (ninguna — solo se vuelve a la entrada de datos)

## Transición: TR-003 — PRESENTAR_AUTOFIRMA: F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA → F_SALIDA_PENDIENTE_RESOLUCION

**Disparador:** botón, con confirmación: "¿Está seguro que desea presentar la solicitud? No podrá deshacer esta acción"

**Campos editables:** (ninguna — la instancia ya está generada; solo se firma y se presenta)

**Validaciones:**

- VAL-TR-003-001 — La firma corresponde al DNI de firma del expediente
  - mensaje: "El certificado usado no corresponde al DNI del solicitante"

**Efectos:**

- RN-TR-003-001 — Firmar la instancia con AutoFirma (FIR-instancia-001) y guardar la versión firmada
  - fase: antes_de_commit
- RN-TR-003-002 — Presentar la instancia firmada por registro de entrada y guardar el resguardo sellado
  - fase: antes_de_commit

## Transición: TR-004 — RESOLVER: F_SALIDA_PENDIENTE_RESOLUCION → F_SALIDA_PENDIENTE_FIRMA_DIRECTOR | F_ENTRADA_DATOS

**Disparador:** botón, con confirmación: "¿Está seguro que desea resolver el expediente?"

**Ramas:** si el tipo de resolución es RESPONDER → F_SALIDA_PENDIENTE_FIRMA_DIRECTOR; si es SUBSANAR_DATOS → F_ENTRADA_DATOS

**Campos editables:** tipo de resolución, respuesta, datos a subsanar

**Validaciones:**

- VAL-TR-004-001 — El tipo de resolución está indicado
  - mensaje: "Debe elegir una resolución"
- VAL-TR-004-002 — La respuesta está indicada
  - condición: el tipo de resolución es RESPONDER
  - mensaje: "La respuesta es obligatoria"
- VAL-TR-004-003 — Los datos a subsanar están indicados
  - condición: el tipo de resolución es SUBSANAR_DATOS
  - mensaje: "Debe indicar los datos a subsanar"

**Efectos:**

- RN-TR-004-001 — Generar el documento de la respuesta (documento-respuesta) y guardarlo en el expediente
  - fase: antes_de_commit
  - rama: RESPONDER
- RN-TR-004-002 — Enviar al solicitante un correo con los datos a subsanar
  - fase: después_de_commit
  - rama: SUBSANAR_DATOS

## Transición: TR-005 — FIRMA_COMPLETADA: F_SALIDA_PENDIENTE_FIRMA_DIRECTOR → F_TERMINADO_RESPONDIDO

**Disparador:** automática — cuando el director firma el documento de la respuesta en el portafirmas (FIR-respuesta-001)

**Efectos:**

- RN-TR-005-001 — Emitir la respuesta firmada por registro de salida y guardar el documento sellado
  - fase: antes_de_commit
- RN-TR-005-002 — Enviar al solicitante un correo con la respuesta sellada adjunta
  - fase: después_de_commit
