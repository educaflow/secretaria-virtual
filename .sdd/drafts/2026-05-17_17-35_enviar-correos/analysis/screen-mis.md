# Pantalla: "Mis correos"

## Identidad
- **Quién la usa:** Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo.
- **Qué muestra:** las TareaCorreo cuyo DNI de destinatario coincide con el DNI del usuario conectado, en modo solo lectura. Estos usuarios ven sus correos aunque el correo no tenga centro asignado.

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Carpeta ciudadana" → "Mis correos" |
| Título visible | "Mis correos" |
| Quién lo ve | Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Grid 1 — "Mis correos"

### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo |
| Columnas (en orden) | fecha de creación, asunto, estado |
| Ordenación por defecto | fecha de creación descendente |
| ¿Permite buscar? | SÍ — búsqueda libre por asunto |
| Formulario que abre el onclick | Formulario 1 — "Detalle de correo" (en modo solo lectura) |
| Botones del toolbar | — (los destinatarios no pueden crear correos, ver entidad TareaCorreo) |
| Botones de las columnas | — |

### Botones
*(sin botones)*

---

## Formulario 1 — "Detalle de correo"

### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo |
| Solo lectura | sí |

### Paneles
| Panel (título) | Tipo | Campos |
|----------------|------|--------|
| "Datos del correo" | normal (siempre solo lectura) | asunto, dirección de correo, historial de estado del expediente |
| "Contenido" | normal (siempre solo lectura) | cuerpo |
| "Adjuntos" | anidado → Grid 2 ("Adjuntos") | — |
| "Estado del envío" | normal (siempre solo lectura) | estado, fecha de creación |
| "(sin título)" | botones | botón "Volver" |

### Botones
| Botón | Qué hace |
|-------|----------|
| "Volver" | Vuelve a la pantalla anterior |

### Reglas de UI
*(no aplica)*

---

## Grid 2 — "Adjuntos"

### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | AdjuntoCorreo |
| Columnas (en orden) | nombre del fichero, contenido del fichero |
| Ordenación por defecto | nombre del fichero ascendente |
| ¿Permite buscar? | NO |
| Formulario que abre el onclick | — |
| Botones del toolbar | — |
| Botones de las columnas | — |

### Botones
*(sin botones)*