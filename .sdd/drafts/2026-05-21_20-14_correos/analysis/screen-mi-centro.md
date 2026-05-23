# Pantalla: "Correos de mi centro"

## Identidad
- **Quién la usa:** Supervisor, Administrativa.
- **Qué muestra:** los Correos cuyo centro coincide con el centro activo del usuario logado, en modo solo lectura. Al hacer click en una fila se abre el detalle del correo (solo lectura). No hay alta ni reenvío desde esta pantalla.

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Correos de mi centro" |
| Título visible | Correos de mi centro |
| Quién lo ve | Supervisor, Administrativa |

---

## Estructura jerarquica de las pantallas
```
Correo
```

---

## Grid 1 — "Correos de mi centro"
### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | Correo |
| Columnas (en orden) | asunto, dniDestinatario, emailDestinatario, estado, fechaCreacion, fechaEnvio |
| Ordenación por defecto | por fechaCreacion descendente (los más recientes primero) |
| ¿Permite buscar? | sí — por estado, por destinatario (dniDestinatario / emailDestinatario), por rango de fechas (fechaCreacion, fechaEnvio) y búsqueda libre; pero la búsqueda siempre opera dentro del subconjunto de Correos del centro activo del usuario (ver U-mi-centro-001) |
| Formulario que abre el onclick | Formulario de Correo (modo detalle, solo lectura) — ver screen-correo.md |
| Botones del toolbar | — (los roles de centro no crean Correos: la operación Crear de Correo solo la permite el Administrador, ver entity-Correo.md "Acciones"; por eso no existe botón "Nuevo") |
| Botones de las columnas | — |

### Botones
*(sin botones)* — el listado es solo lectura; el detalle se abre con click sobre la fila.

### Reglas de UI (U-mi-centro-NNN)
| ID | Disparador | Efecto | Campo/Panel afectado | Condición | Origen EARS |
|----|------------|--------|----------------------|-----------|-------------|
| U-mi-centro-001 | continuo | Filtrar dominio | listado de Correos | Solo se muestran los Correos cuyo centro coincide con el centro activo del usuario logado. Es también el mecanismo que impide ver Correos de otros centros. | E-ST-006, E-UN-006 |
