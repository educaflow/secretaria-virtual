---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Registra y envía correos electrónicos de la secretaría virtual, dejando trazabilidad inmutable de qué se envió, a quién, cuándo y con qué resultado. Cada usuario consulta los correos que le corresponden según su rol y centro. El envío SMTP efectivo lo proporciona la infraestructura existente.

### Dependencias de otros subsistemas

- `base/infrastructure/mail` — Envío SMTP efectivo. Solo se invoca, no se reimplementa.
- `subsystem/expedientes` — Referencia opcional a un historial de estado de un expediente desde la TareaCorreo.
- `subsystem/security` / gestión multicentro — Para resolver el centro del usuario conectado (filtrado de visibilidad) y para identificar el DNI del destinatario.

### Seguridad

- **Administrador**: ve todos los correos del sistema sin filtro, crea correos nuevos, reenvía correos fallidos y consulta la gráfica.
- **Supervisor del centro**: ve los correos cuyo centro coincide con su propio centro. Solo lectura.
- **Administrativa**: igual que Supervisor del centro.
- **Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo**: ven únicamente los correos cuyo DNI de destinatario coincide con el suyo. Solo lectura.
- Nadie puede modificar ni borrar una TareaCorreo ni sus adjuntos.
- Multicentro: sí. Los correos sin centro asignado los ve únicamente el Administrador (y el propio destinatario en "Mis correos").

### Entidades

| Fichero                                              | Entidad        | Para qué sirve                                                                                              |
|------------------------------------------------------|----------------|-------------------------------------------------------------------------------------------------------------|
| [entity-TareaCorreo.md](./entity-TareaCorreo.md)     | TareaCorreo    | Correo electrónico que el sistema debe enviar o ya ha enviado. Inmutable salvo estado y datos de intento.   |
| [entity-AdjuntoCorreo.md](./entity-AdjuntoCorreo.md) | AdjuntoCorreo  | Fichero adjunto a una TareaCorreo. Copia propia inmutable.                                                  |

### Pantallas

| Fichero                                       | Pantalla                          | Para qué sirve                                                                                                |
|-----------------------------------------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------|
| [screen-todos.md](./screen-todos.md)          | "Todos los correos"               | Vista del Administrador con todos los correos del sistema. Permite crear, ver detalle y reenviar si está FALLADO. |
| [screen-centro.md](./screen-centro.md)        | "Correos del centro"              | Vista de Supervisor y Administrativa con los correos cuyo centro coincide con el suyo. Solo lectura.          |
| [screen-mis.md](./screen-mis.md)              | "Mis correos"                     | Vista personal del destinatario: lista los correos cuyo DNI coincide con el suyo. Solo lectura.               |
| [screen-nuevo.md](./screen-nuevo.md)          | "Nuevo correo"                    | Formulario del Administrador para crear una TareaCorreo (envío asíncrono posterior).                          |
| [screen-grafica.md](./screen-grafica.md)      | "Gráfica de correos enviados"     | Gráfica diaria por estado entre dos fechas elegidas. Solo Administrador.                                      |

### Resumen de reglas

Cada entidad numera sus propias reglas como `V-<Entidad>-NNN` y `R-<Entidad>-NNN` (ver `entity-*.md`).
Cada pantalla numera sus propias reglas como `U-<slug-pantalla>-NNN` (ver `screen-*.md`).

- Total validaciones: 11 (8 en TareaCorreo, 3 en AdjuntoCorreo)
- Total reglas de negocio: 9 (8 en TareaCorreo, 1 en AdjuntoCorreo)
- Total reglas de UI: 10 (4 en `todos`, 6 en `grafica`)

### Asunciones a confirmar

- **V-TareaCorreo-005**: el formato del email del destinatario se valida en el momento de crear la TareaCorreo (no se difiere al envío SMTP). Asunción heredada de la especificación.
- **R-TareaCorreo-002**: al crear una TareaCorreo, si el usuario que la crea pertenece a un centro, ese centro se asigna automáticamente; el Administrador puede dejarlo vacío para correos "del sistema". Confirmar que es la conducta deseada.
- **U-grafica-001 / U-grafica-002**: valores por defecto del rango de fechas (mes en curso). Confirmar.
- **`screen-grafica` U-grafica-005/006**: la validación "fecha inicial ≤ fecha final" se modela como regla de UI (bloquea el refresco de la gráfica) en vez de validación de entidad, porque la gráfica no toca ninguna TareaCorreo.