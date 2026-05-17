# Pantalla: "Correos del centro"

## Identidad
- **Quién la usa:** Supervisor del centro, Administrativa.
- **Qué muestra:** las TareaCorreo cuyo centro coincide con el centro del usuario conectado, en modo solo lectura.

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Correos del centro" |
| Título visible | "Correos del centro" |
| Quién lo ve | Supervisor, Administrativa |

---

## Estructura jerarquica de las pantallas

```
TareaCorreo
└── AdjuntoCorreo
```

---

## Grid 1 — "Correos del centro"

### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | TareaCorreo |
| Columnas (en orden) | fecha de creación, asunto, DNI del destinatario, dirección de correo, estado |
| Ordenación por defecto | fecha de creación descendente |
| ¿Permite buscar? | SÍ — búsqueda libre por todos los campos visibles, filtros por estado |
| Formulario que abre el onclick | Formulario 1 — "Detalle de correo" (en modo solo lectura) |
| Botones del toolbar | — (los roles que ven esta pantalla no pueden crear correos, ver entidad TareaCorreo) |
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
| "Datos del correo" | normal (siempre solo lectura) | asunto, DNI del destinatario, dirección de correo, centro, historial de estado del expediente |
| "Contenido" | normal (siempre solo lectura) | cuerpo |
| "Adjuntos" | anidado → Grid 2 ("Adjuntos") | — |
| "Estado del envío" | normal (siempre solo lectura) | estado, fecha de creación, fecha del último intento, número de intentos, motivo del fallo |
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