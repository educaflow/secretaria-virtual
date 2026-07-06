# Pantalla: Solicitudes del centro (secretaría)

## Identidad

- **Quién la usa:** cargos Secretario y Vicesecretario (resuelven en el detalle); Supervisor y demás cargos consultores del centro en solo lectura; Administrador en solo lectura sobre todos los centros.
- **Qué muestra:** el listado de las solicitudes del centro del usuario conectado, filtrables por estado, y, al entrar en una, el formulario con su detalle y las acciones de resolución.

## Menú

- Secretaría → Solicitudes de certificado — lo ven los cargos Secretario y Vicesecretario y el Supervisor; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de solicitudes del centro
└── Formulario de resolución   (se abre al entrar en una solicitud)
```

---

## Vista: Listado de solicitudes del centro

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** las solicitudes del centro del usuario conectado, filtrables por estado, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** alumno solicitante, tipo de certificado, fecha de solicitud, estado
- **Ordenación por defecto:** por fecha de solicitud, descendente (las más recientes primero)
- **Búsqueda / filtros:** sí, por estado
- **Al pulsar una fila abre:** el formulario de resolución

### Botones

*(sin botones)*

(Sin reglas de UI propias: el filtrado por centro es alcance de rol y vive en Seguridad.)

---

## Vista: Formulario de resolución

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** el detalle de una solicitud con sus datos y las acciones Emitir y Rechazar.
- **Se abre desde:** el listado de solicitudes del centro, al entrar en una solicitud.

### Propiedades

- **Modo:** solo lectura sobre los datos de la solicitud; la resolución se hace con los botones Emitir y Rechazar.

### Paneles

- **Solicitud** (normal) — alumno solicitante, tipo de certificado, fecha de solicitud, estado
- **Resolución** (normal) — motivo de rechazo, fecha de resolución, documento emitido

(«alumno solicitante» y «tipo de certificado» son selectores de otras entidades: **campos** del panel, no vistas de esta pantalla.)

### Botones

- **Emitir** — Emite el certificado; pasa la solicitud a EMITIDA. Visible solo si está PENDIENTE.
- **Rechazar** — Rechaza la solicitud indicando un motivo; pasa a RECHAZADA. Visible solo si está PENDIENTE.

### Reglas de UI

- RUI-solicitudes-centro-formulario-001 — El campo motivo de rechazo solo se muestra cuando el estado es RECHAZADA
  - disparador: continuo
  - condición: estado == RECHAZADA
