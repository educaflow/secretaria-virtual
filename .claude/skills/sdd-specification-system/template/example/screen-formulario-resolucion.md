# Pantalla: Formulario de resolución (secretaría)

## Identidad

- **Quién la usa:** cargos Secretario y Vicesecretario (emiten/rechazan); Supervisor, Administrador y demás cargos consultores en solo lectura.
- **Qué muestra:** el detalle de una solicitud con sus datos y las acciones Emitir y Rechazar.

## Menú

- No se abre desde un menú: se abre desde el listado «Solicitudes de certificado» al entrar en una solicitud.

## Paneles

- **Solicitud** — alumno solicitante, tipo de certificado, fecha de solicitud, estado
- **Resolución** — motivo de rechazo, fecha de resolución, documento emitido

## Botones

- **Emitir** — Emite el certificado; pasa la solicitud a EMITIDA. Visible solo si está PENDIENTE.
- **Rechazar** — Rechaza la solicitud indicando un motivo; pasa a RECHAZADA. Visible solo si está PENDIENTE.

## Reglas de UI

- RUI-002 — El campo motivo de rechazo solo se muestra cuando el estado es RECHAZADA
  - disparador: continuo
  - condición: estado == RECHAZADA
