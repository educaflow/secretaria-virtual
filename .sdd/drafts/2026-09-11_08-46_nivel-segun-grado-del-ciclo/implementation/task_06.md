---
type: implementation-task
template: system
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Crea el servicio de `Nivel`: la interfaz y su implementación, que son un único componente lógico.

- `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/NivelService.java` (`Crear`)
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/NivelServiceImpl.java` (`Crear`)

Los dominios XML ya están colocados en `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/` por las tareas 02-04 y son **contrato fijo**: las entidades JPA generadas mandan sobre el Java. **MUST NOT** editarlos ni regenerarlos.

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/NivelService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Nivel`. |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/NivelServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | `V-Nivel-001` + whitelists de alta y modificación. |

## Paso 3 del diseño (verbatim)

### Paso 3 — Servicio de `Nivel`

```java
// Clase: com.educaflow.subsystem.sistemaeducativo.service.NivelService
public interface NivelService extends ModelService<Nivel>;
//   Sin métodos propios: el subsistema no añade acciones a Nivel. MUST NOT re-declarar
//   validateInsert/validateUpdate/validateRemove ni allowPropertiesInsert/Update/Remove:
//   sus firmas vienen de ModelService<Nivel> (k-sistemas/servicios.md).

// Clase: com.educaflow.subsystem.sistemaeducativo.service.impl.NivelServiceImpl
//   extends DefaultModelService<Nivel> implements NivelService
//   La descubre ModelServiceFactory por nombre y paquete: MUST NOT registrarse en ningún módulo Guice.

// Métodos (bloque 1 — acciones): ninguno. No se sobrescriben insert/update/remove porque no hay
//   nada que añadir al «validar + repository.save» que ya hace DefaultModelService.

// Bloque 2 — Métodos de Validación
public NivelServiceImpl(Class<Nivel> model, Repository<Nivel> repository);
//   Constructor obligatorio: super(model, repository).

@Override
public Optional<BusinessMessages> validateInsert(Nivel nivel);
//   Aplica:
//     - V-Nivel-001 (Origen spec: RES-Nivel-001) el nivel indica a qué grado pertenece: comprueba
//       que el grado no es nulo. Mensaje: el literal que declara RES-Nivel-001 en entity-Nivel.md
//       (MUST copiarse tal cual, porque es el que el escenario ESC-007 espera ver).
//   Delega en el helper compartido validateGradoIndicado(nivel): la restricción vale para toda
//   operación, no solo para el alta (k-validaciones/restricciones.md §2).

@Override
public Optional<BusinessMessages> validateUpdate(Nivel nivel, Nivel nivelOriginal);
//   Aplica la misma V-Nivel-001, con el mismo helper. Cambiar el grado de un nivel ya usado por
//   ciclos está declarado fuera de alcance en la spec: aquí no se comprueba nada más.

private Optional<BusinessMessages> validateGradoIndicado(Nivel nivel);
//   Helper de validación compartido por validateInsert y validateUpdate (por eso vive en el bloque
//   de Métodos de Validación, no en «Otras funciones»). Acumula los mensajes en un BusinessMessages
//   y devuelve el Optional vacío cuando la validación es válida y el BusinessMessages acumulado
//   cuando no lo es (patrón canónico de k-validaciones §2). Nunca lanza excepciones.
//   El mensaje se ancla al campo `grado` para que el error salga pegado al campo en el formulario.

// Bloque 3 — AllowProperties
@Override
public AllowProperties allowPropertiesInsert();
//   Devuelve la whitelist de propiedades editables (helper allowPropertiesEditables()).

@Override
public AllowProperties allowPropertiesUpdate();
//   Devuelve la misma whitelist: entity-Nivel.md declara la misma lista en Crear y en Modificar.

private AllowProperties allowPropertiesEditables();
//   Único dueño de la whitelist de Nivel: createAllowProperties con `code`, `name` y `grado`
//   (este último con mapa interno vacío: se acepta la referencia por id, pero MUST NOT copiarse
//   ningún campo dentro del Grado apuntado). Ver la sección «Frontera de confianza».
```

**Verificación del paso:** compila; `ModelServiceFactory` resuelve `NivelServiceImpl` (guardar un nivel sin grado devuelve el mensaje de negocio, no el error genérico de JPA).


## Frontera de confianza — AllowProperties por acción (verbatim)


El diseño no declara ninguna acción propia con `@CallMethod`. Las whitelists que siguen son las de `insert`/`update`, y se aplican en los dos caminos por los que puede llegar un guardado **de esa misma entidad**:

- **Vía A (botón de la UI):** `remote-validationSave-action` → `DefaultModelController.validateSave` (que es un `@CallMethod`) → `ActionRequestHelper.getModel(allowPropertiesInsert()|allowPropertiesUpdate())`.
- **Vía B (endpoint REST genérico `POST /ws/rest/<FQN>`):** `Resource.save` → `ModelService.validate(json, context)` → `AllowProperties.filter(json, allowPropertiesInsert()|allowPropertiesUpdate())`.

Convención de esta sección: un campo relacional que se acepta **solo como referencia** lleva mapa interno **vacío**; con él, `AllowProperties.filter` deja pasar únicamente `id`/`version` y `BeanMapperModel` no copia ningún campo dentro de la entidad apuntada.

### `NivelServiceImpl.insert` y `NivelServiceImpl.update`

Entidad: `Nivel`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de las acciones `Crear` y `Modificar` de `entity-Nivel.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `code` | cliente | sí | Input directo del usuario. |
| `name` | cliente | sí | Input directo del usuario. |
| `grado` | cliente | sí (mapa interno vacío) | El administrador elige el grado en el formulario. Solo se acepta la **referencia**; `V-Nivel-001` comprueba en el servidor que viene indicada. El mapa vacío impide que el JSON modifique de rebote el código o el nombre del grado apuntado. |


## Trazabilidad V/R/U → ubicación (filas aplicables, verbatim)

### Validaciones `V-<Entidad>-NNN`

| V | Origen spec | Ubicación | Capa |
|---|---|---|---|
| `V-Nivel-001` | `RES-Nivel-001` | `NivelServiceImpl.validateInsert` y `NivelServiceImpl.validateUpdate` → `validateGradoIndicado`; capa declarativa `required="true"` de `grado` en `domains/Nivel.xml` | servidor (modelo + `validate*`) |

## Clasificación `cliente` / `servidor` de los campos (filas aplicables, verbatim)

| Entidad | Campo | Origen | Motivo |
|---|---|---|---|
| `Nivel` | `code` | cliente | En `Input AllowProperties` de Crear y Modificar (`entity-Nivel.md`). |
| `Nivel` | `name` | cliente | En `Input AllowProperties` de Crear y Modificar. |
| `Nivel` | `grado` | cliente | En `Input AllowProperties` de Crear y Modificar; lo elige el administrador en el formulario. Validado por `V-Nivel-001`. |

Ningún campo `servidor` del delta se asigna en una acción, luego **no hay ninguna `R-` en este diseño**: los dos son derivados de solo lectura, el caso que `design-contract.md` §3 exime expresamente de tener una `R-Antes`. Tampoco hay reglas `RN-` en la especificación.
