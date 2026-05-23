# Pantalla: "Formulario de Correo (alta y detalle)"

## Identidad
- **Quién la usa:** En modo alta, solo el Administrador (crea correos manualmente desde "Todos los correos"). En modo detalle, cualquier rol que pueda ver el Correo desde su listado (Administrador y roles consultores del centro), siempre en solo lectura; únicamente el Administrador ve el botón "Reenviar" cuando el correo está FALLIDO.
- **Qué muestra:** Una única pantalla en dos modos. En alta, un formulario editable para componer un correo a un destinatario identificado por su DNI: DNI, email (autocompletado desde el DNI si corresponde a un usuario existente, editable a mano si no), asunto, cuerpo enriquecido y una lista opcional de adjuntos. En detalle, el correo ya creado en solo lectura junto con sus campos de seguimiento (estado, número de intentos, fecha del último intento, motivo del último fallo, fecha de envío, fecha de creación y centro) y sus adjuntos descargables.

---

## Estructura jerarquica de las pantallas
```
Correo
└── AdjuntoCorreo
```

---

## Formulario 1 — "Correo"
### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | Correo |
| Solo lectura | no — editable solo en modo alta (correo nuevo, sin guardar); en modo detalle (correo ya creado) todo el formulario es solo lectura |

### Paneles
| Panel (título) | Tipo | Campos |
|----------------|------|--------|
| Destinatario | normal | dniDestinatario, emailDestinatario |
| Mensaje | normal | asunto, cuerpo |
| Adjuntos | anidado → Grid 2 ("Adjuntos") | adjuntos |
| Seguimiento | normal (siempre solo lectura) | estado, numeroIntentos, fechaUltimoIntento, motivoUltimoFallo, fechaEnvio, fechaCreacion, centro |
| (botonera) | botones | "Cancelar", "Guardar", "Cerrar", "Reenviar" |

### Botones
| Botón | Qué hace |
|-------|----------|
| Cancelar | Descarta el alta en curso y cierra el formulario sin crear el correo. Solo visible en modo alta. |
| Guardar | Crea el correo con los datos introducidos; queda en estado PENDIENTE y el formulario pasa a modo detalle. Solo visible en modo alta. |
| Cerrar | Cierra el formulario y vuelve al listado. Solo visible en modo detalle. |
| Reenviar | Solicita el reenvío del correo (operación "Reenviar", referencia R-Correo-005; valida V-Correo-007: solo FALLIDO). Solo visible en modo detalle cuando el usuario es Administrador y el estado es FALLIDO. |

(No se incluye botón "Borrar" en el formulario: la especificación no contempla el borrado de un correo desde su detalle, y el detalle lo abren también roles consultores en solo lectura; el borrado, donde proceda, se gestiona desde el listado.)

### Reglas de UI (U-correo-NNN)
| ID | Disparador | Efecto | Campo/Panel afectado | Condición | Origen EARS |
|----|------------|--------|----------------------|-----------|-------------|
| U-correo-001 | Al cambiar dniDestinatario (alta) | Valor por defecto / rellenar | emailDestinatario | Si existe un usuario con ese DNI, se propone su email; si no existe, se deja vacío. El Administrador puede editarlo. | E-EV-003, E-EV-009 |
| U-correo-002 | Al abrir un correo ya creado (modo detalle) | Solo lectura | Todo el formulario (paneles Destinatario, Mensaje, Adjuntos) | El correo ya está creado (no es un alta en curso). | E-ST-004 |
| U-correo-003 | Al cargar el formulario | Solo lectura | referenciaHistorialEstadoExpediente | Siempre: nunca es editable desde la interfaz, en alta ni en detalle. | E-UN-009 |
| U-correo-004 | Al cargar el formulario | Ocultar | Panel "Seguimiento" (estado, numeroIntentos, fechaUltimoIntento, motivoUltimoFallo, fechaEnvio, fechaCreacion, centro) | Solo visible en modo detalle; oculto en modo alta (el correo aún no tiene datos de seguimiento). | E-UB-009 |
| U-correo-005 | Al cargar el formulario | Ocultar | Botón "Reenviar" | Visible solo si el usuario es Administrador y el estado es FALLIDO; oculto en cualquier otro estado o rol. | E-EV-007, E-ST-002, E-ST-003 |
| U-correo-006 | Al cargar el formulario | Ocultar | Botones "Cancelar" y "Guardar" | Visibles solo en modo alta; ocultos en modo detalle. | — |
| U-correo-007 | Al cargar el formulario | Ocultar | Botón "Cerrar" | Visible solo en modo detalle; oculto en modo alta. | — |

---

## Grid 2 — "Adjuntos"
### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | AdjuntoCorreo |
| Columnas (en orden) | nombreFichero, contenido |
| Ordenación por defecto | por nombreFichero ascendente |
| ¿Permite buscar? | no — es un grid anidado de un único correo, lista corta |
| Formulario que abre el onclick | Formulario 2 ("Adjunto del correo") — solo lectura, para descargar el contenido |
| Botones del toolbar | en modo alta: "Añadir adjunto"; en modo detalle: — (ninguno) |
| Botones de las columnas | descargar (sobre la columna contenido) |

### Botones
| Botón | Qué hace |
|-------|----------|
| Añadir adjunto | Abre una nueva fila de adjunto para aportar nombre de fichero y contenido en el alta del correo. Solo disponible en modo alta. |
| Descargar | Descarga el contenido del adjunto seleccionado. Disponible en alta y detalle. |

(El grid no permite "Nuevo" en modo detalle porque los adjuntos son inmutables una vez creado el correo; solo se pueden añadir durante el alta, mediante "Añadir adjunto".)

---

## Formulario 2 — "Adjunto del correo"
### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | AdjuntoCorreo |
| Solo lectura | no — editable solo como hijo nuevo durante el alta del correo; en modo detalle (adjunto ya creado) es solo lectura |

### Paneles
| Panel (título) | Tipo | Campos |
|----------------|------|--------|
| Adjunto | normal | nombreFichero, contenido |

### Botones
| Botón | Qué hace |
|-------|----------|
| (sin botones propios) | El guardado/descarte del adjunto se hereda del formulario maestro del correo; en detalle, el campo contenido permite descargar. |
