---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Subsistema que registra en base de datos cada correo electrónico que la aplicación envía o intenta enviar a un destinatario identificado por DNI, gestiona su envío asíncrono apoyándose en la infraestructura de correo existente y ofrece distintas vistas de consulta según el rol del usuario.

### Dependencias de otros subsistemas

- `subsystem/expedientes` — referencia opcional al historial de estado de un expediente cuando el Correo se origina en su tramitación (externa; no se inspecciona su modelo).
- `subsystem/security` — identificación del usuario logado, su rol, su DNI y su centro activo.
- `subsystem/registrousuario` — resolución del email a partir de un DNI en el alta manual.
- `subsystem/common` — entidad `Centro` (referenciada, no creada).
- `base/infrastructure/mail` — envío SMTP real (se invoca, no se implementa).

### Seguridad

- **Administrador:** ve todos los Correos sin filtro por centro; crea Correos manualmente; reenvía Correos en FALLIDO; ve la gráfica.
- **Supervisor:** ve, en solo lectura, los Correos cuyo centro coincide con su centro activo. No crea ni reenvía.
- **Administrativa:** ve, en solo lectura, los Correos cuyo centro coincide con su centro activo. No crea ni reenvía.
- **Profesor, Alumno, Exprofesor, Exalumno, Familiar, Externo:** ven, en solo lectura, únicamente los Correos cuyo DNI de destinatario coincide con su propio DNI. No crean ni reenvían.
- La autorización por rol (quién puede crear, reenviar o ver la gráfica) se materializa como autorización de la operación (columna "Cuándo se permite" de Acciones) y control de acceso por menú; por convención del proyecto no se modela como V/R/U.
- **Multicentro:** sí. Cada Correo pertenece a un único centro o a ninguno; los listados de Supervisor y Administrativa están restringidos a su centro activo.

### Entidades

| Fichero                                                  | Entidad        | Para qué sirve                                                                                  |
|----------------------------------------------------------|----------------|-------------------------------------------------------------------------------------------------|
| [entity-Correo.md](./entity-Correo.md)                   | Correo         | Cada correo que la aplicación envía o ha intentado enviar a un destinatario identificado por DNI. Inmutable salvo estado, contador de intentos, fechas de intento/envío y motivo del último fallo. |
| [entity-AdjuntoCorreo.md](./entity-AdjuntoCorreo.md)     | AdjuntoCorreo  | Copia inmutable de un fichero adjunto asociado a un Correo, tomada en el momento de su creación. |

Relación: Correo (1) ─── (N) AdjuntoCorreo (padre/hijo; borrado en cascada desde el padre). Correo (N) ─── (1) Centro (opcional, externo). Correo (N) ─── (1) historial de estado de expediente (opcional, externo, solo asignable programáticamente).

### Pantallas

| Fichero                                              | Pantalla                                | Para qué sirve                                                                                       |
|------------------------------------------------------|-----------------------------------------|------------------------------------------------------------------------------------------------------|
| [screen-todos.md](./screen-todos.md)                 | "Todos los correos"                     | Listado del Administrador con todos los Correos del sistema sin filtro de centro; entrada al alta.   |
| [screen-correo.md](./screen-correo.md)               | "Formulario de Correo (alta y detalle)" | Pantalla única en dos modos: alta editable (Administrador) y detalle solo lectura con "Reenviar".    |
| [screen-mi-centro.md](./screen-mi-centro.md)         | "Correos de mi centro"                  | Listado solo lectura de los Correos del centro activo del usuario (Supervisor, Administrativa).      |
| [screen-mis.md](./screen-mis.md)                     | "Mis correos"                           | Listado solo lectura de los Correos dirigidos al DNI del usuario logado (roles consultores).         |
| [screen-grafica.md](./screen-grafica.md)             | "Gráfica de correos"                    | Gráfica de barras apiladas por estado entre dos fechas, con granularidad día/semana/mes (Administrador). |

El "Formulario de Correo" se modela como una pantalla compartida (`screen-correo.md`) porque los tres listados ("Todos los correos", "Correos de mi centro", "Mis correos") abren el mismo formulario en modo detalle; así se evita duplicarlo.

### Resumen de reglas

Cada entidad numera sus propias reglas como `V-<Entidad>-NNN` y `R-<Entidad>-NNN` (ver `entity-*.md`).
Cada pantalla numera sus propias reglas como `U-<slug-pantalla>-NNN` (ver `screen-*.md`).
Cada V/R/U lleva en su tabla una columna **"Origen EARS"** con los IDs `E-XX-NNN` del `specification.md` que la originaron, o `—` si fue inventada por el análisis.

- Total validaciones: 10 (de las cuales sin Origen EARS: 0)
- Total reglas de negocio: 9 (sin Origen EARS: 1 — R-Correo-008, borrado en cascada)
- Total reglas de UI: 11 (sin Origen EARS: 2 — U-correo-006, U-correo-007, visibilidad de botones según modo)

### Resumen de campos por origen

Cada `entity-*.md` clasifica cada campo en `cliente` (lo aporta el usuario) o `servidor` (lo dicta el servidor en una R-…). El diseñador usa esta clasificación para decidir `AllowProperties` y la asignación incondicional por acción (ver `k-secure-coding` §3).

| Entidad        | cliente | servidor |
|----------------|---------|----------|
| Correo         | 5       | 8        |
| AdjuntoCorreo  | 2       | 1        |

### Tests E2E

Los escenarios concretos de prueba viven en [tests.md](./tests.md), numerados `T-NNN` y trazables a los `F-NNN` del spec.
`/sdd-implementer-system` los ejecuta con `playwright-cli` tras escribir el código Java (bucle de auto-corrección).

- Total tests: 17 (T-001 … T-017)
- Flujos del spec cubiertos: 8 / 8 (todos los `F-NNN` aparecen como `Origen F` en al menos un test)

> **Nota sobre tests no navegables solo con navegador:** T-009 y T-010 (F-002 / F-003) dependen de disparar la tarea periódica de envío asíncrona; T-013 (F-005) depende de un alta programática solicitada por otro subsistema. En los tres casos el resultado se verifica abriendo el Correo en "Todos los correos", pero el disparo no tiene botón de UI: `/sdd-implementer-system` deberá provocar la ejecución de la tarea / el alta programática por otro medio.

### Flujos sin tests

*(todos los flujos principales están cubiertos por tests)*

### V/R/U sin tests

V/R/U declaradas que **no** aparecen en la columna `Verifica` de ningún test `T-NNN`. La decisión de cobertura es explícita (confirmada con el usuario), no bloquea la generación.

| Regla              | Cobertura                          | Justificación                                                                          |
|--------------------|------------------------------------|----------------------------------------------------------------------------------------|
| V-AdjuntoCorreo-003| pendiente                          | Falta test E2E de la inmutabilidad de un adjunto ya creado.                            |
| R-Correo-008       | pendiente                          | Falta test E2E del borrado en cascada de los AdjuntoCorreo al borrar su Correo padre.  |
| U-correo-003       | smoke manual                       | UI trivial: la referencia al expediente nunca es editable desde la interfaz.           |
| U-correo-004       | smoke manual                       | UI trivial: el panel "Seguimiento" se oculta en modo alta.                             |
| U-correo-006       | cubierta indirectamente por T-008  | T-008 comprueba que "Cancelar"/"Guardar" no se muestran en detalle, sin listarla en `Verifica`. |
| U-correo-007       | cubierta indirectamente por T-008  | T-008 comprueba que "Cerrar" se muestra en detalle, sin listarla en `Verifica`.        |

### EARS descartados

IDs `E-XX-NNN` del `specification.md` que **no** se han mapeado a ninguna V/R/U, con su justificación.

| Origen EARS | Motivo                                                                                                                                                  |
|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| E-UB-005    | Es una característica del tipo de dato del campo `cuerpo` (HTML enriquecido), reflejada en el Modelo de datos de `entity-Correo.md`; no genera una V/R/U. |
| E-UB-012    | Parametrización de la frecuencia de la tarea periódica vía propiedad de configuración; detalle de scheduler/implementación, no una V/R/U.                |
| E-UN-002    | Control de acceso por rol (solo el Administrador crea manualmente); documentado en Acciones ("Cuándo se permite") y en Seguridad. Por convención del proyecto (k-secure-coding §9) la autorización por rol vive en el controlador, no como V/R/U. |
| E-UN-003    | Control de acceso por rol (solo el Administrador reenvía); documentado en Acciones ("Cuándo se permite") y en Seguridad. Misma convención de autorización en el controlador. |
| E-UN-008    | Control de acceso por rol (solo el Administrador ve la gráfica); documentado en Seguridad y en el control de acceso por menú de `screen-grafica.md`.     |
