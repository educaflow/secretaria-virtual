# Plantilla — fichero `entity-<Nombre>.md` (análisis de una entidad)

Esta plantilla describe la estructura obligatoria del fichero de análisis de una entidad. Cada análisis funcional contiene **un fichero por cada entidad** detectada (ver `examples/ciclo/` o `examples/firmas/` para ejemplos completos). El nombre del fichero es `entity-<NombreEntidad>.md` (CamelCase).

Las cuatro secciones (`Modelo de datos`, `Validaciones`, `Acciones`, `Reglas de negocio`) son **obligatorias y aparecen en este orden**. Si una no aplica, se conserva el encabezado y se pone `*(no aplica)*` o `*(no hay ...)*` debajo.

> **Importante**: la plantilla describe **QUÉ** hace la entidad, no **CÓMO** se implementa. No deben aparecer aquí nombres de clases Java (`XxxService`, `XxxImpl`), métodos del framework (`validateInsert`, `fireActionRule_*`), atributos XML (`required`, `showIf`), ni FQN. Si dudas si algo es análisis o diseño, mira `SKILL.md` § "Frontera entre análisis y diseño".

---

# Entidad: <NombreEntidad>

*(Una o dos frases describiendo qué representa la entidad en el negocio. Ej.: "Tarea de firma asignada a un usuario firmante. Agrupa uno o varios documentos que deben firmarse juntos.")*

## Modelo de datos

Lista de campos de la entidad en lenguaje funcional. **No** se usan tipos Java (`String`, `LocalDateTime`…) ni anotaciones JPA — se describe el tipo desde el punto de vista del negocio.

| Campo                | Tipo de dato                 | Relación                          | Origen del valor    | Notas                                                                    |
|----------------------|------------------------------|-----------------------------------|---------------------|--------------------------------------------------------------------------|
| *(nombre del campo)* | *(ver tabla de tipos abajo)* | *(ver tabla de relaciones abajo)* | `cliente` / `servidor` | *(opcional — aclaraciones funcionales, restricciones, quién lo rellena)* |

**Valores admitidos en `Tipo de dato`** (siempre funcionales, nunca Java):

`texto`, `texto largo`, `HTML enriquecido`, `entero`, `decimal`, `booleano`, `fecha`, `fecha-hora`, `enum` (en este caso indicar los valores en la columna `Relación` p.ej. `valores: BORRADOR, APROBADO, RECHAZADO`), `relación`, `lista`, `fichero`.

**Valores admitidos en `Relación`**:

- `—` si el campo no es relacional.
- `→ <Entidad>` si es relación uno-a-uno o muchos-a-uno (FK).
- `→ <Entidad> (padre)` si la relación apunta al padre en una jerarquía.
- `→ <Entidad> (uno a varios, hijos)` si es una colección.
- `valores: X, Y, Z` si el tipo es `enum`.

**Valores admitidos en `Origen del valor`** — clasificación de seguridad **obligatoria** (ver `[[k-secure-coding]]` §3):

- **`cliente`** — el valor lo aporta el usuario en el alta/edición. Validable con V-rules. Permitido en `AllowProperties` del controller. Es el caso típico de campos como `asunto`, `cuerpo`, `dniDestinatario`, etc.
- **`servidor`** — el valor lo dicta el servidor en una regla de negocio. Cubre tanto valores que vienen de fuera del bean (fechas de creación, estados iniciales, contadores, IDs generados, snapshots del usuario autenticado) como valores calculados a partir de otros campos (totales, hashes, denormalizaciones). El cliente **NUNCA** lo puede dictar; el servidor lo asigna/recalcula incondicionalmente. La diferencia entre "venía de fuera" y "calculado" se explica en el comentario de la R-… que lo asigna, no como categoría aparte.

**MUST** que cada campo tenga su `Origen del valor` relleno con `cliente` o `servidor`. Si la clasificación es ambigua (p.ej. un campo que en la UI rellena el usuario pero en un import masivo lo dicta el servidor), **pregunta al usuario con `AskUserQuestion`** — no inventes.

**Coherencia con las tablas de reglas:**

- Cada campo `servidor` **DEBE** estar respaldado por al menos una `R-<Entidad>-NNN` con momento `Antes` que documente cómo lo asigna el servidor (o aparecer justificado en "Asunciones a confirmar").
- Un campo clasificado como `cliente` **MUST NOT** aparecer como asignado por una `R-<Entidad>-NNN` con momento `Antes` de `Crear` (eso lo convertiría implícitamente en `servidor`).

**Columna `Notas`** — opcional. Útil para indicar:
- Quién rellena el campo (`asignado por el sistema`, `seleccionado por el usuario`, `viene de otra pantalla`).
- Restricciones de negocio que no caben en una validación con mensaje (`dominio limitado a "D" o "E"`).
- Significado funcional cuando el nombre del campo no es autoexplicativo.

---

## Validaciones (V-{NombreEntidad}-NNN)

Tabla canónica de `k-validaciones`. Una validación es una condición que **bloquea** una operación si no se cumple. Si no hay ninguna, se pone `*(no hay validaciones específicas)*`.

| ID                  | Campo(s)                                   | Descripción                                      | Condición                          | Mensaje al usuario                                                           | Origen EARS                                          |
|---------------------|--------------------------------------------|--------------------------------------------------|------------------------------------|------------------------------------------------------------------------------|------------------------------------------------------|
| V-{Entidad}-001     | *(uno o varios campos separados por coma)* | *(qué condición funcional debe cumplir el dato)* | *(cuándo se aplica la validación)* | *("Mensaje literal entre comillas con `{valor}` interpolado donde proceda")* | *(`E-XX-NNN` del spec separados por comas, o `—`)*   |

**Reglas:**

- **ID con prefijo de entidad**: `V-<NombreEntidad>-001`, `V-<NombreEntidad>-002`, … La numeración es **local por entidad** y empieza siempre en 001, sin huecos. El `<NombreEntidad>` coincide con el nombre del fichero `entity-<NombreEntidad>.md` (CamelCase). Ejemplo: para `entity-TareaCorreo.md` → `V-TareaCorreo-001`, `V-TareaCorreo-002`, …
- Si la validación se **asume** por análisis (no está explícitamente en la historia de usuario), marcar con `*` el ID (`V-TareaCorreo-007*`) y listarla en "Asunciones a confirmar" al final del análisis.
- **Condición**: lenguaje natural (`Siempre`, `Solo al rechazar la firma`, `Solo si grado = "D"`). Nunca expresiones de código.
- **Mensaje al usuario**: literal entre comillas. Sigue las guías de redacción de `k-validaciones`:
  - Empezar por el campo o el valor recibido.
  - Incluir el valor recibido (`'{email}'`) y, en dominios finitos, los valores válidos.
  - No usar tecnicismos del framework.
- Las validaciones dependientes de estado **comparten la misma secuencia `V-<Entidad>-NNN`**, no se abren tablas paralelas.
- Las reglas de unicidad declaran su **ámbito** (global / por centro / por año / combinación) en `Descripción` o `Condición`.
- **Origen EARS**: lista de IDs `E-XX-NNN` del `specification.md` que dieron lugar a esta validación, separados por comas (típicamente uno o varios `E-UN-NNN`, ocasionalmente otros patrones). Si la validación es **inventada por el analista** (no estaba en ningún `E-XX-NNN` del spec), se pone `—`. Los IDs deben existir realmente en el spec.

---

## Acciones

Una entrada por cada operación que el sistema permite sobre esta entidad. Las tres primeras filas (`Crear`, `Modificar`, `Borrar`) son **fijas y siempre aparecen**, aunque sea con "Cuándo se permite = Nunca — \<motivo\>". Después se añaden las operaciones de negocio puntuales (Aprobar, Rechazar, Reintentar, Archivar, etc.), una fila por cada una.

| Operación              | Cuándo se permite  | Validaciones que aplican          | Reglas que dispara              |
|------------------------|--------------------|-----------------------------------|---------------------------------|
| Crear (insert)         | *(condición)*      | *(V-{Entidad}-NNN referenciadas)* | *(R-{Entidad}-NNN referenciadas)* |
| Modificar (update)     | *(condición)*      | *(V-{Entidad}-NNN)*               | *(R-{Entidad}-NNN)*             |
| Borrar (remove)        | *(condición)*      | *(V-{Entidad}-NNN)*               | *(R-{Entidad}-NNN)*             |
| *(operación custom 1)* | *(condición)*      | *(V-{Entidad}-NNN)*               | *(R-{Entidad}-NNN)*             |
| *(operación custom 2)* | …                  | …                                 | …                               |

**Valores admitidos en `Cuándo se permite`**:

- `Siempre` — operación abierta.
- `Solo si <condición funcional>` — incluye estado, cargo, propiedad del registro, antigüedad, etc.
- `Nunca — <motivo>` — operación deliberadamente desactivada. **Obligatorio justificar** para que el diseñador no se pregunte si se olvidó.

Si una operación es `Nunca`, las columnas de validaciones y reglas quedan en `—`.

**Nombre de la operación**: usar el nombre funcional con el que la conoce el negocio. Para las custom, el nombre debe coincidir literalmente con el título del botón que la dispara en la pantalla correspondiente (facilita la trazabilidad).

---

## Reglas de negocio (R-{NombreEntidad}-NNN)

Tabla canónica de `k-validaciones`. Una regla de negocio es una acción que el sistema **ejecuta** automáticamente ante un evento (insertar, actualizar, borrar, operación custom). Modifica el estado del sistema o produce efectos colaterales. **Nunca bloquea** — si bloquea, es una `V-<Entidad>-NNN`.

Si no hay reglas, se pone `*(no hay reglas de negocio asociadas a <Entidad>)*`.

| ID                 | Descripción                                     | Entidad                        | Método                      | Momento             | Más información                                  | Origen EARS                                          |
|--------------------|-------------------------------------------------|--------------------------------|-----------------------------|---------------------|--------------------------------------------------|------------------------------------------------------|
| R-{Entidad}-001    | *(qué hace el sistema, no qué hace el usuario)* | *(entidad sobre la que actúa)* | *(operación de la entidad)* | *(Antes / Después)* | *(condiciones, dependencias, datos modificados)* | *(`E-XX-NNN` del spec separados por comas, o `—`)*   |

**Reglas:**

- **ID con prefijo de entidad**: `R-<NombreEntidad>-001`, `R-<NombreEntidad>-002`, … Numeración **local por entidad** desde 001, sin huecos. El `<NombreEntidad>` coincide con el del fichero. Ejemplo: para `entity-TareaCorreo.md` → `R-TareaCorreo-001`, `R-TareaCorreo-002`, …
- **Reglas asumidas** se marcan con `*` en el ID (`R-TareaCorreo-005*`) y se listan en "Asunciones a confirmar".
- **Descripción**: describir qué hace el sistema (`Asigna el número de expediente secuencial`, `Envía un correo al solicitante`), nunca qué hace el usuario.
- **Entidad**: la entidad sobre la que la regla actúa (normalmente la del fichero, pero puede ser otra si la regla tiene efectos colaterales en hijos).
- **Método**: la operación de la entidad donde se dispara la regla. Valores típicos: `insert`, `update`, `remove`, `cambiarEstado`, o el nombre de una operación custom (`Marcar como rechazada`, `Reintentar envío`…). El nombre debe coincidir con el de la columna `Operación` de la tabla de Acciones.
- **Momento**:
  - `Antes` si la regla **escribe sobre el mismo registro** que se está guardando (los cambios persistirán junto con el `save`).
  - `Después` si tiene **efectos colaterales** (correos, PDFs, propagación a otras entidades, llamadas a sistemas externos).
- Si una regla mezcla escritura propia + efectos colaterales, **partirla en dos `R-<Entidad>-NNN` separadas** (una Antes, otra Después) — mejora la trazabilidad al diseño.
- **Más información**: condiciones de aplicación (`Solo si el estado pasa a APROBADO`), datos que se modifican, dependencias, etc.
- **Origen EARS**: lista de IDs `E-XX-NNN` del `specification.md` que dieron lugar a esta regla, separados por comas (típicamente `E-EV-NNN` o `E-UB-NNN`; ocasionalmente otros patrones según el efecto). `—` si la regla fue inventada por el analista durante la interpretación.

---

## Asunciones a confirmar (opcional, al final del análisis completo)

Las reglas `V-<Entidad>-NNN` o `R-<Entidad>-NNN` marcadas con `*` se listan en el `analysis.md` consolidado, no en cada fichero de entidad.

Cada asunción se redacta como una pregunta corta y verificable, p.ej.:

- **V-TareaFirma-007**: ¿es correcto que el motivo de rechazo sea obligatorio solo al pasar a RECHAZADO, no en otros momentos?
- **R-TareaFirma-002**: ¿realmente se debe notificar al sistema solicitante al firmar, o basta con guardar?
