# Pantalla: Administración de SMS

## Identidad

- **Quién la usa:** el Administrador, que da de alta SMS, los consulta y reenvía los fallidos.
- **Qué muestra:** los SMS de todos los centros, sin filtro; el alta de un SMS nuevo en edición y el detalle de un SMS ya creado en solo lectura, con los datos de su envío.

## Menú

- SMS → Todos — lo ve solo el Administrador; lleva a esta pantalla. El menú «SMS» va justo después de «Correos».

## Estructura jerárquica de las vistas

```
Listado de SMS
└── Formulario de SMS   (se abre al pulsar una fila o con «Nuevo SMS»)
```

---

## Vista: Listado de SMS

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** todos los SMS de todos los centros, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** estado, DNI del destinatario, nombre, apellidos, teléfono, mensaje, centro, nombre del expediente, fecha de creación, fecha de envío
- **Ordenación por defecto:** por fecha de creación, del más reciente al más antiguo
- **Búsqueda / filtros:** sí, por cualquiera de las columnas
- **Al pulsar una fila abre:** el formulario de SMS

### Botones

- **Nuevo SMS** (barra superior) — Abre el formulario de alta de un SMS.

## Vista: Formulario de SMS

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos de un SMS: en el alta, los que rellena el Administrador; en el detalle, todos en solo lectura junto con los datos del envío.
- **Se abre desde:** el listado de SMS, al pulsar una fila o «Nuevo SMS».

### Propiedades

- **Modo:** editable en el alta; en el detalle de un SMS ya creado, de solo lectura.

### Paneles

- **Datos del SMS** (normal) — centro, DNI del destinatario, nombre, apellidos, teléfono (móvil de España), mensaje, estado del expediente
- **Datos del envío** (normal) — estado, número de reintentos, fecha de creación, fecha de envío, fecha del primer intento de envío, fecha del último intento de envío, descripción del último fallo

### Botones

- **Guardar** — Da de alta el SMS; solo visible en el alta.
- **Cancelar** — Descarta el alta y vuelve al listado; solo visible en el alta.
- **Reenviar** — Vuelve a intentar el envío del SMS y muestra «El reenvío del SMS se ha puesto en marcha.»; solo visible en el detalle cuando el SMS está en estado FALLIDO.
- **Salir** — Vuelve al listado; solo visible en el detalle.
- Desviación del estándar: **sin «Borrar»** (un SMS no se puede borrar) y **sin «Guardar»** en el detalle (un SMS no se puede modificar).

### Reglas de UI

- RUI-sms-todos-formulario-001 — En el detalle de un SMS ya creado, todos los campos del panel «Datos del SMS» son de solo lectura
  - disparador: al cargar
  - condición: el SMS ya está creado
- RUI-sms-todos-formulario-002 — El panel «Datos del envío» solo se muestra en el detalle de un SMS ya creado, no en el alta
  - disparador: continuo
  - condición: el SMS ya está creado
- RUI-sms-todos-formulario-003 — La fecha de envío solo se muestra cuando el SMS está en estado ENVIADO
  - disparador: continuo
  - condición: estado == ENVIADO
- RUI-sms-todos-formulario-004 — La descripción del último fallo solo se muestra cuando el SMS está en estado FALLIDO
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-sms-todos-formulario-005 — Los botones «Guardar» y «Cancelar» solo se ven en el alta, y el botón «Salir» solo en el detalle
  - disparador: continuo
  - condición: el SMS ya está creado (para «Salir»); no lo está (para «Guardar» y «Cancelar»)
- RUI-sms-todos-formulario-006 — El botón «Reenviar» solo se ve en el detalle de un SMS en estado FALLIDO
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-sms-todos-formulario-007 — El selector de país del teléfono solo ofrece España
  - disparador: continuo
  - condición: Siempre
- RUI-sms-todos-formulario-008 — Tras guardar el alta de un SMS, el usuario vuelve al listado de SMS en lugar de quedarse en el formulario
  - disparador: al guardar
  - condición: el alta se ha guardado sin errores
