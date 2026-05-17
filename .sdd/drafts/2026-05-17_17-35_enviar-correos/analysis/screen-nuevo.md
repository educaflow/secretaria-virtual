# Pantalla: "Nuevo correo"

## Identidad
- **Quién la usa:** Administrador.
- **Qué muestra:** formulario para crear una nueva TareaCorreo. El envío posterior lo procesa el sistema de forma asíncrona.

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Nuevo correo" |
| Título visible | "Nuevo correo" |
| Quién lo ve | Administrador |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Formulario 1 — "Nuevo correo"

### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo |
| Solo lectura | no |

### Paneles
| Panel (título) | Tipo | Campos |
|----------------|------|--------|
| "Datos del correo" | normal | asunto, DNI del destinatario, dirección de correo, centro, historial de estado del expediente |
| "Contenido" | normal | cuerpo |
| "Adjuntos" | anidado → Grid 2 ("Adjuntos") | — |
| "(sin título)" | botones | botón "Cancelar", botón "Guardar" |

### Botones
| Botón | Qué hace |
|-------|----------|
| "Cancelar" | Cierra el formulario sin guardar |
| "Guardar" | Valida V-TareaCorreo-001, V-TareaCorreo-002, V-TareaCorreo-003, V-TareaCorreo-004, V-TareaCorreo-005, V-TareaCorreo-006, V-TareaCorreo-007 → Ejecuta la operación "Crear (insert)" (R-TareaCorreo-001, R-TareaCorreo-002, R-TareaCorreo-003) → Cierra el formulario |

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
| Botones del toolbar | "Añadir adjunto" |
| Botones de las columnas | "Quitar" |

### Botones
| Botón | Qué hace |
|-------|----------|
| "Añadir adjunto" | Añade un fichero como nuevo AdjuntoCorreo dentro de la TareaCorreo en creación |
| "Quitar" | Quita el adjunto seleccionado de la TareaCorreo en creación (antes de guardar) |