# Modelo: SolicitudCertificado

Petición de un alumno para obtener un certificado académico de su centro. Tiene ciclo de vida: nace pendiente y la secretaría la resuelve emitiéndola o rechazándola.

## Campos

- **alumno solicitante** — el alumno que pide el certificado
- **tipo de certificado** — el tipo elegido del catálogo
- **fecha de solicitud** — cuándo se registró la solicitud
- **estado** — situación de la solicitud (sus valores, en «Estados y transiciones»)
- **motivo de rechazo** — explicación cuando se rechaza
- **fecha de resolución** — cuándo se emitió o rechazó
- **documento emitido** — el certificado generado al emitir la solicitud

## Estados y transiciones

- Estado inicial: PENDIENTE
- PENDIENTE → EMITIDA: el secretario emite el certificado.
- PENDIENTE → RECHAZADA: el secretario rechaza la solicitud indicando un motivo.
- EMITIDA y RECHAZADA son terminales.

## Restricciones

- RES-001 — Una solicitud pertenece siempre a un alumno del centro
- RES-002 — La fecha de resolución no puede ser anterior a la fecha de solicitud

## Campos calculados

- CC-001 — fecha de solicitud
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: fecha y hora actuales en el momento de crear la solicitud

## Acción: Crear

**Input AllowProperties:** tipo de certificado

**Validaciones:**

- VAL-001 — El tipo de certificado está indicado
  - mensaje: "El tipo de certificado es obligatorio"

**Reglas de negocio:**

- RN-001 — Registrar la solicitud en el registro de entrada
  - fase: antes_de_commit
- RN-002 — Avisar a la secretaría del centro
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — una vez enviada, la solicitud no se edita; solo cambia de estado por las acciones de resolución)

## Acción: Emitir

**Validaciones:**

- VAL-002 — La solicitud está en estado PENDIENTE
  - mensaje: "Solo se pueden emitir solicitudes pendientes"
- VAL-003 — El usuario pertenece a la secretaría del centro
  - actor: [SECRETARIO, VICESECRETARIO]
  - mensaje: "No tiene permisos para emitir certificados"

**Reglas de negocio:**

- RN-003 — Generar el documento del certificado a partir de la plantilla
  - fase: antes_de_commit
- RN-004 — Enviar al alumno un correo con el certificado emitido
  - fase: después_de_commit
  - condición: el alumno tiene email registrado

## Acción: Rechazar

**Input AllowProperties:** motivo de rechazo

**Validaciones:**

- VAL-004 — La solicitud está en estado PENDIENTE
  - mensaje: "Solo se pueden rechazar solicitudes pendientes"
- VAL-005 — El motivo de rechazo está indicado
  - mensaje: "El motivo es obligatorio"

**Reglas de negocio:**

- RN-005 — Enviar al alumno un correo con el motivo del rechazo
  - fase: después_de_commit
