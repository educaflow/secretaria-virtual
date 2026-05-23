# Pantalla: "Todos los correos"

## Identidad
- **Quién la usa:** Administrador.
- **Qué muestra:** todos los Correos del sistema sin filtro por centro, en modo solo lectura (el alta se hace con "Nuevo correo").

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Todos los correos" |
| Título visible | Todos los correos |
| Quién lo ve | Administrador |

---

## Estructura jerarquica de las pantallas
```
Correo
```

---

## Grid 1 — "Todos los correos"
### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | Correo |
| Columnas (en orden) | asunto, dniDestinatario, emailDestinatario, estado, numeroIntentos, fechaCreacion, fechaEnvio, centro |
| Ordenación por defecto | por fechaCreacion descendente (los correos más recientes primero) |
| ¿Permite buscar? | sí — filtros por estado, destinatario (dniDestinatario / emailDestinatario), centro y rangos de fechas (fechaCreacion, fechaEnvio); además búsqueda libre |
| Formulario que abre el onclick | Formulario de Correo (modo detalle, solo lectura) — ver screen-correo.md, "Formulario de Correo (alta y detalle)" |
| Botones del toolbar | "Nuevo correo" |
| Botones de las columnas | — |

### Botones
| Botón | Qué hace |
|-------|----------|
| "Nuevo correo" | Abre el "Formulario de Correo" en modo alta para crear un nuevo Correo |

### Reglas de UI (U-todos-NNN)
*(no aplica)* — esta pantalla no tiene reglas de UI propias. El listado muestra todos los Correos sin filtro de centro porque la pantalla solo es accesible para el Administrador (control de acceso por rol vía menú, no filtro de dominio). El botón "Reenviar" no está en la lista: las acciones por fila se realizan abriendo el detalle del Correo (ver screen-correo.md).
