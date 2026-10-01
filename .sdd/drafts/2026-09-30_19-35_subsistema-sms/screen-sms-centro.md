# Pantalla: SMS de mis centros

## Identidad

- **Quién la usa:** el Supervisor y el Administrativo, en solo lectura, con la posibilidad de reenviar los SMS fallidos.
- **Qué muestra:** los SMS de los centros en los que el usuario tiene el tipo de usuario Supervisor o Administrativo, con una columna de centro para distinguirlos; el detalle de cada SMS con los datos de su envío, en solo lectura.

## Menú

- SMS → Del centro — lo ven los usuarios de los centros (no el Administrador); lleva a esta pantalla. Solo muestra datos a quien es Supervisor o Administrativo de algún centro.

## Estructura jerárquica de las vistas

```
Listado de SMS del centro
└── Formulario de SMS del centro   (se abre al pulsar una fila)
```

---

## Vista: Listado de SMS del centro

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los SMS de los centros en los que el usuario es Supervisor o Administrativo, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** centro, estado, DNI del destinatario, nombre, apellidos, teléfono, mensaje, nombre del expediente, fecha de creación, fecha de envío
- **Ordenación por defecto:** por fecha de creación, del más reciente al más antiguo
- **Búsqueda / filtros:** sí, por cualquiera de las columnas (incluido el centro)
- **Al pulsar una fila abre:** el formulario de SMS del centro

### Botones

*(sin botones)*

## Vista: Formulario de SMS del centro

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** todos los datos de un SMS de uno de los centros del usuario y los datos de su envío, en solo lectura.
- **Se abre desde:** el listado de SMS del centro, al pulsar una fila.

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Datos del SMS** (normal) — centro, DNI del destinatario, nombre, apellidos, teléfono, mensaje, estado del expediente
- **Datos del envío** (normal) — estado, número de reintentos, fecha de creación, fecha de envío, fecha del primer intento de envío, fecha del último intento de envío, descripción del último fallo

### Botones

- **Reenviar** — Vuelve a intentar el envío del SMS y muestra «El reenvío del SMS se ha puesto en marcha.»; solo visible cuando el SMS está en estado FALLIDO.
- **Salir** — Vuelve al listado.
- Desviación del estándar: formulario de solo lectura, **sin «Guardar» ni «Borrar»**.

### Reglas de UI

- RUI-sms-centro-formulario-001 — La fecha de envío solo se muestra cuando el SMS está en estado ENVIADO
  - disparador: continuo
  - condición: estado == ENVIADO
- RUI-sms-centro-formulario-002 — La descripción del último fallo solo se muestra cuando el SMS está en estado FALLIDO
  - disparador: continuo
  - condición: estado == FALLIDO
- RUI-sms-centro-formulario-003 — El botón «Reenviar» solo se ve cuando el SMS está en estado FALLIDO
  - disparador: continuo
  - condición: estado == FALLIDO
