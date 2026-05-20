---
type: analysis
---

## Análisis Funcional: Correos

**Tipo:** subsistema
**Capa:** subsystem/correos
**Descripción:** Registra y envía correos electrónicos de la secretaría virtual, dejando trazabilidad inmutable de qué se envió, a quién, cuándo y con qué resultado. Cada usuario consulta los correos que le corresponden según su rol y centro. El envío SMTP en sí lo proporciona la infraestructura ya existente.

### Dependencias de otros subsistemas
- `base/infrastructure/mail` — Realiza el envío SMTP efectivo; el servidor, usuario y contraseña vienen de la configuración de la aplicación. Este subsistema solo lo invoca, no lo reimplementa.
- `subsystem/expedientes` — Para permitir asociar opcionalmente una TareaCorreo a un cambio de estado concreto (historial de estado) de un expediente.
- `subsystem/security` (gestión de centro / usuarios) — Para conocer el centro del usuario conectado (filtrado de visibilidad) y el DNI del usuario.

### Seguridad
- **Administrador:** puede ver todas las TareaCorreo del sistema (con y sin centro), crear correos nuevos, reenviar correos fallidos y consultar la gráfica.
- **Supervisor del centro:** puede ver únicamente las TareaCorreo cuyo centro coincide con su propio centro. Las TareaCorreo sin centro asignado **no** las ve. No puede crear, modificar, borrar ni reenviar.
- **Administrativa:** igual que Supervisor del centro.
- **Profesor, Exprofesor, Alumno, Exalumno, Familiar, Externo:** pueden ver únicamente las TareaCorreo cuyo DNI de destinatario coincide con el suyo (sin filtro adicional por centro). No pueden crear, modificar, borrar ni reenviar.
- Nadie, en ningún rol, puede modificar ni borrar una TareaCorreo ni sus adjuntos (regla histórica inmutable; ver V-TareaCorreo-007 / V-TareaCorreo-008 / V-AdjuntoCorreo-003 / V-AdjuntoCorreo-004).
- **Multicentro:** sí. Un Supervisor o Administrativa solo ve los correos de su centro; los correos sin centro asignado los ve únicamente el Administrador (además del propio destinatario en "Mis correos").

### Entidades
| Fichero                                              | Entidad        | Para qué sirve                                                                                                |
|------------------------------------------------------|----------------|---------------------------------------------------------------------------------------------------------------|
| [entity-TareaCorreo.md](./entity-TareaCorreo.md)     | TareaCorreo    | Cada correo electrónico que el sistema debe enviar o ya ha enviado a un destinatario concreto.                |
| [entity-AdjuntoCorreo.md](./entity-AdjuntoCorreo.md) | AdjuntoCorreo  | Fichero adjunto vinculado a una TareaCorreo, con copia propia desacoplada del original.                       |

### Pantallas
| Fichero                                            | Pantalla                       | Para qué sirve                                                                                                          |
|----------------------------------------------------|--------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| [screen-todos.md](./screen-todos.md)               | "Todos los correos"            | Vista del Administrador con todas las TareaCorreo del sistema. Permite abrir el detalle, crear y reenviar.              |
| [screen-centro.md](./screen-centro.md)             | "Correos del centro"           | Vista de Supervisor y Administrativa con las TareaCorreo de su centro, en solo lectura.                                 |
| [screen-mis.md](./screen-mis.md)                   | "Mis correos"                  | Vista del destinatario con las TareaCorreo dirigidas a su DNI, en solo lectura.                                         |
| [screen-grafica.md](./screen-grafica.md)           | "Gráfica de correos enviados"  | Gráfica diaria del número de TareaCorreo creadas en un rango de fechas, desglosada por estado. Solo Administrador.      |

### Resumen de reglas
Cada entidad numera sus propias reglas como `V-<Entidad>-NNN` y `R-<Entidad>-NNN` (ver `entity-*.md`).
Cada pantalla numera sus propias reglas como `U-<slug-pantalla>-NNN` (ver `screen-*.md`).
Cada V/R/U lleva en su tabla una columna **"Origen EARS"** con los IDs `E-XX-NNN` del `specification.md` que la originaron, o `—` si fue inventada por el análisis.

- Total validaciones: 13 (de las cuales sin Origen EARS: 2 — V-AdjuntoCorreo-001 y V-AdjuntoCorreo-002, validaciones triviales de campo obligatorio inferidas por el analista).
- Total reglas de negocio: 6 (sin Origen EARS: 0).
- Total reglas de UI: 21 (sin Origen EARS: 7 — U-todos-004 / U-centro-003 para ocultar "motivo del fallo" cuando no aplica, y U-todos-005..U-todos-007 / U-todos-012 / U-todos-013 para controlar la visibilidad de paneles, botones del formulario y botones del grid de adjuntos según el modo creación vs. detalle; inferidas por el analista al fundir "Nuevo correo" dentro del Formulario 1 de "Todos los correos").

### Tests E2E
Los escenarios concretos de prueba viven en [tests.md](./tests.md), numerados `T-NNN` y trazables a los `F-NNN` del spec.
`/sdd-implementer-system` los ejecuta con `playwright-cli` tras escribir el código Java (bucle de auto-corrección).

- Total tests: 5
- Flujos del spec cubiertos: 4 / 4 (todos los `F-NNN` aparecen como `Origen F` en al menos un test).
- Mapeo flujo → tests: F-001 → T-001; F-002 → T-002; F-003 → T-003, T-004; F-004 → T-005.

### Flujos sin tests

*(Todos los flujos principales están cubiertos por tests.)*

### V/R/U sin tests
V/R/U declaradas en los `entity-*.md` / `screen-*.md` que **no** aparecen en la columna `Verifica` de ningún test `T-NNN`. Los tests del fichero `tests.md` cubren los flujos principales del spec (consulta y creación + filtrados por centro/DNI); el resto de reglas críticas queda como **pendiente** y las reglas de UI como **smoke manual**, salvo las explícitamente marcadas en la tabla. Esta lista es una **decisión deliberada** del analista para que la revisión humana o la siguiente iteración añadan los tests faltantes que considere oportunos.

| Regla                  | Cobertura       | Justificación                                                                                                                                 |
|------------------------|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| V-TareaCorreo-005      | pendiente       | Validación de formato de email; conviene test E2E negativo introduciendo una dirección sin "@".                                                |
| V-TareaCorreo-006      | pendiente       | Validación de historial de expediente inexistente; depende del subsistema expedientes para crear el escenario.                                 |
| V-TareaCorreo-007      | smoke manual    | Inmutabilidad post-creación: en la UI el formulario de detalle ya está en solo lectura, por lo que es difícil disparar la validación vía UI.   |
| V-TareaCorreo-008      | smoke manual    | Idem: no hay botón "Borrar" en ninguna pantalla. La validación blinda la API.                                                                  |
| V-TareaCorreo-009      | pendiente       | Reenvío fuera de estado FALLADO; depende de tener un correo en ENVIANDO o ENVIADO real, lo cual requiere un mock SMTP.                         |
| V-AdjuntoCorreo-001    | smoke manual    | Validación trivial (nombre obligatorio).                                                                                                       |
| V-AdjuntoCorreo-002    | smoke manual    | Validación trivial (contenido obligatorio).                                                                                                    |
| V-AdjuntoCorreo-003    | smoke manual    | Heredada de V-TareaCorreo-007.                                                                                                                 |
| V-AdjuntoCorreo-004    | smoke manual    | Heredada de V-TareaCorreo-008.                                                                                                                 |
| R-TareaCorreo-002      | cubierta indirectamente por T-002 | El test crea un correo (sin adjuntos en el happy path actual). Conviene un test adicional con adjunto para verificar la copia desacoplada. |
| R-TareaCorreo-003      | pendiente       | Reenvío: requiere un correo en FALLADO real (mock SMTP que falle).                                                                             |
| R-TareaCorreo-004      | pendiente       | Transición PENDIENTE → ENVIANDO con incremento de intentos; depende del scheduler.                                                             |
| R-TareaCorreo-005      | pendiente       | Transición ENVIANDO → ENVIADO/FALLADO; depende del scheduler y de mock SMTP.                                                                   |
| R-AdjuntoCorreo-001    | pendiente       | Verificar que la copia del adjunto sobrevive a la modificación del fichero original.                                                           |
| U-todos-001            | pendiente       | Visibilidad condicional del botón "Reenviar" según estado; conviene verificarlo en T-001 ampliándolo, o en un T nuevo.                          |
| U-todos-002            | smoke manual    | Visibilidad del enlace "Ver expediente"; trivial.                                                                                              |
| U-todos-003            | smoke manual    | Visibilidad del panel "Expediente relacionado"; trivial.                                                                                       |
| U-todos-004            | smoke manual    | Visibilidad del campo "motivo del fallo"; trivial.                                                                                             |
| U-centro-001           | smoke manual    | Visibilidad del enlace "Ver expediente"; trivial.                                                                                              |
| U-centro-002           | smoke manual    | Visibilidad del panel "Expediente relacionado"; trivial.                                                                                       |
| U-centro-003           | smoke manual    | Visibilidad del campo "motivo del fallo"; trivial.                                                                                             |
| U-mis-001              | smoke manual    | Visibilidad del enlace "Ver expediente"; trivial.                                                                                              |
| U-mis-002              | smoke manual    | Visibilidad del panel "Expediente relacionado"; trivial.                                                                                       |
| U-todos-005            | smoke manual    | Mostrar/ocultar panel "Estado del envío" según modo creación vs. existente; trivial.                                                           |
| U-todos-006            | cubierta indirectamente por T-002 | Visibilidad de "Guardar y enviar" / "Cancelar" en modo creación; el test feliz los pulsa.                                              |
| U-todos-007            | smoke manual    | Visibilidad del botón "Cerrar" solo en modo edición de existente; trivial.                                                                     |
| U-todos-008            | cubierta indirectamente por T-002 | Obligatoriedad de "asunto" en creación; conviene un T adicional dejándolo vacío para verificar V-TareaCorreo-001.                       |
| U-todos-009            | cubierta indirectamente por T-002 | Obligatoriedad de "cuerpo" en creación; conviene un T adicional para verificar V-TareaCorreo-002.                                       |
| U-todos-010            | cubierta indirectamente por T-002 | Obligatoriedad de "DNI del destinatario" en creación; conviene un T adicional para verificar V-TareaCorreo-003.                         |
| U-todos-011            | cubierta indirectamente por T-002 | Obligatoriedad de "dirección de correo del destinatario" en creación; conviene un T adicional para verificar V-TareaCorreo-004.         |
| U-todos-012            | smoke manual    | Visibilidad de "Añadir adjunto" solo en modo creación; trivial.                                                                                |
| U-todos-013            | smoke manual    | Visibilidad de "Quitar adjunto" solo en modo creación; trivial.                                                                                |
| U-grafica-001          | pendiente       | Obligatoriedad de la fecha inicial en la gráfica.                                                                                              |
| U-grafica-002          | pendiente       | Obligatoriedad de la fecha final en la gráfica.                                                                                                |
| U-grafica-003          | pendiente       | Aviso por rango de fechas inválido en la gráfica.                                                                                              |

### EARS descartados
IDs `E-XX-NNN` del `specification.md` que **no** se han mapeado a ninguna V/R/U, con su justificación.

| Origen EARS | Motivo                                                                                                                                                            |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| E-UB-001    | "Acepta cualquier DNI": es la ausencia explícita de validación. No requiere V ni R; se cubre por omisión (no hay V-TareaCorreo sobre el formato del DNI).         |
| E-UB-002    | "Permite crear sin centro asignado": ausencia de obligatoriedad de centro; cubierto por la **no** existencia de validación sobre `centro` + Seguridad.            |
| E-UB-003    | "Permite crear sin referencia a historial de estado": ausencia de obligatoriedad; no requiere V/R.                                                                |
| E-UB-004    | "Permite crear sin adjuntos": ausencia de obligatoriedad; no requiere V/R.                                                                                        |
| E-UB-007    | Visibilidad por centro: se materializa en la sección Seguridad de este `analysis.md`, no como V/R/U.                                                              |
| E-UB-008    | Visibilidad por DNI: se materializa en la sección Seguridad de este `analysis.md`, no como V/R/U.                                                                 |
| E-UB-009    | Admin ve todas las TareaCorreo: se materializa en la sección Seguridad de este `analysis.md`, no como V/R/U.                                                      |
| *E-UB-011   | Descarga de adjuntos en cualquier estado: ausencia de restricción condicional; el botón "Descargar" está siempre presente en el Grid 2 "Adjuntos" sin condición.  |
| E-UN-007    | No-Admin no crea ni reenvía: cubierto por Seguridad (operaciones Crear y Reenviar restringidas a Administrador en `entity-TareaCorreo.md`).                       |
| E-UN-008    | Supervisor/Administrativa no acceden a correos de otro centro: cubierto por Seguridad (filtro de visibilidad en `screen-centro.md`).                              |

### Asunciones a confirmar
Resumen de validaciones / reglas / pantallas que el analista ha introducido por interpretación y que pueden requerir confirmación del usuario:

- **V-AdjuntoCorreo-001 / V-AdjuntoCorreo-002:** se asume que un AdjuntoCorreo no puede tener nombre o contenido vacío. Razonable, pero no estaba enunciado como EARS.
- **U-todos-004 / U-centro-003:** se asume que el campo "motivo del fallo" se oculta cuando no hay valor (no aplica al correo). Es una preferencia de ergonomía visual; podría dejarse siempre visible.
- **"Nuevo correo" fundido en el Formulario 1 de "Todos los correos":** el spec mencionaba "Detalle de correo" y "Nuevo correo" como pantallas separadas. Se han modelado como **un único** Formulario 1 dentro de `screen-todos.md` con doble modo (solo lectura al abrir desde el grid; editable al pulsar "Nuevo correo"). Las reglas U-todos-005..011 controlan qué campos y botones se muestran según el modo.
- **Detalle único por grid:** el spec menciona "Detalle de correo" como pantalla independiente, pero al ser idéntico para los 3 listados (con la variante del botón "Reenviar" en "Todos los correos") se ha replicado como Form 1 dentro de cada `screen-*.md`. La alternativa hubiera sido un `screen-detalle.md` único enlazado por los tres grids.
- **Cobertura de tests:** el `tests.md` cubre **solo** los flujos principales (`F-001`..`F-004`) declarados en el spec. Reenvío de fallido, gráfica y casos de error (campos vacíos, email inválido, fechas inválidas) **no** tienen tests E2E aún; constan en la tabla "V/R/U sin tests" como `pendiente`.
