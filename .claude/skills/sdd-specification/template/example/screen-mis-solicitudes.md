# Pantalla: Mis certificados (alumno)

## Identidad

- **Quién la usa:** Alumno (sus propias solicitudes, puede crear); Exalumno y Familiar en solo lectura.
- **Qué muestra:** el listado de las solicitudes del alumno conectado con su estado; al entrar en una o al crear una nueva, el formulario de la solicitud con sus documentos adjuntos; y, al abrir un adjunto, el formulario del documento.

## Menú

- Secretaría → Mis certificados — lo ve el Alumno; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de mis solicitudes
└── Formulario de solicitud   (se abre al pulsar «Nueva solicitud», o al pulsar una fila en solo lectura)
    └── Listado de documentos adjuntos   (panel maestro-detalle «Documentos adjuntos» del formulario de solicitud)
        └── Formulario de documento adjunto   (se abre al pulsar una fila del listado o con «Añadir»)
```

---

## Vista: Listado de mis solicitudes

- **Tipo:** listado
- **Qué muestra:** las solicitudes del alumno conectado con su estado, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** tipo de certificado, fecha de solicitud, estado
- **Ordenación por defecto:** por fecha de solicitud, descendente (las más recientes primero)
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de solicitud (en solo lectura)
- **Mensaje de ayuda (opcional):** «Aquí ves tus solicitudes de certificado y en qué estado está cada una»

### Botones

- **Nueva solicitud** (barra superior) — Abre el formulario de alta de una solicitud. Solo visible para el Alumno.

(Esta vista no tiene reglas de UI propias: el filtrado «solo veo las mías» es alcance de rol y vive en Seguridad, no como `RUI`.)

---

## Vista: Formulario de solicitud

- **Tipo:** formulario
- **Qué muestra:** el alta de una solicitud eligiendo el tipo de certificado y aportando documentos adjuntos. Tras enviarla, queda en solo lectura.
- **Se abre desde:** el listado de mis solicitudes, al pulsar «Nueva solicitud» (alta) o al pulsar una fila (solo lectura).

### Propiedades

- **Modo:** editable durante el alta; tras enviar la solicitud, todo el formulario es de solo lectura.

### Paneles

- **Solicitud** (normal) — tipo de certificado
- **Documentos adjuntos** (maestro-detalle → «Listado de documentos adjuntos») — los ficheros aportados a la solicitud

(«tipo de certificado» se elige del catálogo con un selector: es un **campo** del panel, no una vista de esta pantalla —el catálogo de tipos tiene su propia pantalla en otro sitio—. Solo «Documentos adjuntos», por ser **maestro-detalle**, es una vista del árbol.)

### Botones

- **Enviar** — Registra la solicitud, que queda en estado PENDIENTE.

### Reglas de UI

- RUI-001 — Al crear una solicitud, el alumno se rellena con el usuario actual
  - disparador: al crear
  - condición: Siempre
- RUI-003 — Con la solicitud ya enviada, el panel «Documentos adjuntos» se muestra en solo lectura (los documentos solo se aportan durante el alta)
  - disparador: al cargar
  - condición: la solicitud ya está creada (no es un alta en curso)

---

## Vista: Listado de documentos adjuntos

- **Tipo:** listado
- **Qué muestra:** los ficheros aportados a la solicitud, en lectura.
- **Se abre desde:** embebido como panel «Documentos adjuntos» en el formulario de solicitud.

### Propiedades

- **Columnas (en orden):** nombre de fichero
- **Ordenación por defecto:** por nombre de fichero, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el formulario de documento adjunto

### Botones

- **Añadir** (barra superior) — Abre el formulario de alta de un documento adjunto. Solo disponible durante el alta de la solicitud.

---

## Vista: Formulario de documento adjunto

- **Tipo:** formulario
- **Qué muestra:** un documento aportado a la solicitud (nombre de fichero y contenido). En el alta de la solicitud es editable; con la solicitud ya enviada, en solo lectura para descargarlo.
- **Se abre desde:** el listado de documentos adjuntos, al pulsar una fila o «Añadir».

### Propiedades

- **Modo:** editable mientras se da de alta la solicitud; con la solicitud ya enviada, solo lectura (para descargar el contenido).

### Paneles

- **Adjunto** (normal) — nombre de fichero, contenido

### Botones

*(sin botones)*
