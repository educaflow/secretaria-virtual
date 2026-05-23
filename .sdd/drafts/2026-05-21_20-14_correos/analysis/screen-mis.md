# Pantalla: "Mis correos"

## Identidad
- **Quién la usa:** Profesor, Alumno, Exprofesor, Exalumno, Familiar, Externo.
- **Qué muestra:** los Correos cuyo DNI de destinatario coincide con el DNI del usuario logado, en modo solo lectura.

## Menú
| Propiedad | Valor |
|-----------|-------|
| Ruta jerárquica | "Correos" → "Mis correos" |
| Título visible | Mis correos |
| Quién lo ve | Profesor, Alumno, Exprofesor, Exalumno, Familiar, Externo |

---

## Estructura jerarquica de las pantallas
```
Correo
```

---

## Grid 1 — "Mis correos"
### Propiedades
| Propiedad | Valor |
|-----------|-------|
| Entidad | Correo |
| Columnas (en orden) | asunto, estado, fechaCreacion, fechaEnvio |
| Ordenación por defecto | por fechaCreacion descendente (los más recientes primero) |
| ¿Permite buscar? | sí — por asunto, estado y fechas (fechaCreacion, fechaEnvio), siempre dentro del subconjunto de correos del propio usuario (el filtro de dominio de U-mis-001 no se puede levantar con la búsqueda) |
| Formulario que abre el onclick | Formulario de Correo (modo detalle, solo lectura) — ver screen-correo.md |
| Botones del toolbar | — (estos roles no crean Correos) |
| Botones de las columnas | — |

(No se incluye columna con el DNI ni el email del destinatario: todos los correos del listado van dirigidos al propio usuario, por lo que el dato es siempre el mismo y no aporta información. El destinatario reconoce sus correos por el asunto y las fechas.)

(No existe botón "Nuevo": estos roles son destinatarios de los correos, no remitentes. La creación de Correos solo la realiza el Administrador en el alta manual o el sistema en el alta programática — ver entity-Correo.md, operación Crear. Tampoco hay reenvío: el reenvío es exclusivo del Administrador.)

### Botones
*(sin botones)*

### Reglas de UI (U-mis-NNN)
| ID | Disparador | Efecto | Campo/Panel afectado | Condición | Origen EARS |
|----|------------|--------|----------------------|-----------|-------------|
| U-mis-001 | continuo | Filtrar dominio | listado de Correos | Solo los Correos cuyo DNI de destinatario coincide con el DNI del usuario logado | E-ST-005, E-UN-007 |
