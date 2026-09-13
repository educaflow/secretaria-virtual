---
type: implementation-task
template: system
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Crea el servicio de `Grado`: la interfaz y su implementación, que son un único componente lógico.

- `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/GradoService.java` (`Crear`)
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/GradoServiceImpl.java` (`Crear`)

Los dominios XML ya están colocados en `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/` por las tareas 02-04 y son **contrato fijo**. **MUST NOT** editarlos ni regenerarlos.

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/GradoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de `Grado`. |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/GradoServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Solo whitelists `{code, name}`: cierra el allow-all de `Grado` (ver `decisiones.md` D5). |

## Paso 5 del diseño (verbatim)

### Paso 5 — Servicio de `Grado`

```java
// Clase: com.educaflow.subsystem.sistemaeducativo.service.GradoService
public interface GradoService extends ModelService<Grado>;
//   Sin métodos propios: la spec no añade validaciones a Grado.

// Clase: com.educaflow.subsystem.sistemaeducativo.service.impl.GradoServiceImpl
//   extends DefaultModelService<Grado> implements GradoService

public GradoServiceImpl(Class<Grado> model, Repository<Grado> repository);
//   Constructor obligatorio: super(model, repository).

// Bloque 3 — AllowProperties (los bloques 1, 2, 4 y 5 quedan vacíos y sin header)
@Override
public AllowProperties allowPropertiesInsert();
//   Devuelve allowPropertiesEditables().

@Override
public AllowProperties allowPropertiesUpdate();
//   Devuelve allowPropertiesEditables().

private AllowProperties allowPropertiesEditables();
//   Único dueño de la whitelist de Grado: createAllowProperties con `code` y `name`, exactamente
//   la línea Input AllowProperties de entity-Grado.md. Deja FUERA `niveles` (lado inverso que este
//   diseño añade: sin whitelist, el endpoint REST genérico podría arrastrar hijos por la cascada
//   PERSIST/MERGE) y `admiteNivel` (campo derivado). Motivo de existir esta clase: sin ella Grado
//   cae en DefaultModelService, cuyos allowProperties son createAllowAllProperties() — el fallo
//   fail-open y silencioso de k-secure-coding §3.2.
```

**Verificación del paso:** compila; `POST /ws/rest/...db.Grado` con un `niveles` inventado no modifica ningún nivel.


## Frontera de confianza — AllowProperties por acción (verbatim)


El diseño no declara ninguna acción propia con `@CallMethod`. Las whitelists que siguen son las de `insert`/`update`, y se aplican en los dos caminos por los que puede llegar un guardado **de esa misma entidad**:

- **Vía A (botón de la UI):** `remote-validationSave-action` → `DefaultModelController.validateSave` (que es un `@CallMethod`) → `ActionRequestHelper.getModel(allowPropertiesInsert()|allowPropertiesUpdate())`.
- **Vía B (endpoint REST genérico `POST /ws/rest/<FQN>`):** `Resource.save` → `ModelService.validate(json, context)` → `AllowProperties.filter(json, allowPropertiesInsert()|allowPropertiesUpdate())`.

Convención de esta sección: un campo relacional que se acepta **solo como referencia** lleva mapa interno **vacío**; con él, `AllowProperties.filter` deja pasar únicamente `id`/`version` y `BeanMapperModel` no copia ningún campo dentro de la entidad apuntada.

### `GradoServiceImpl.insert` y `GradoServiceImpl.update`

Entidad: `Grado`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de las acciones `Crear` y `Modificar` de `entity-Grado.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `code` | cliente | sí | Input directo del usuario (en `Input AllowProperties`). |
| `name` | cliente | sí | Input directo del usuario (en `Input AllowProperties`). |
| `niveles` | servidor | **NO** | Lado inverso de `Nivel.grado`. Nadie lo asigna desde `Grado`; dejarlo entrar permitiría arrastrar hijos por la cascada `PERSIST`/`MERGE` del one-to-many. Los niveles se mantienen desde la pantalla de Niveles. |
| `admiteNivel` | servidor | **NO** | `CC-Grado-001`, derivado de solo lectura: el getter generado lo recalcula en cada lectura, así que nada de lo que el cliente enviara sobreviviría; queda fuera igualmente por higiene. |


## Campos calculados (trazabilidad, verbatim)

### Campos calculados

| Campo | Origen spec | Ubicación | Momento |
|---|---|---|---|
| `Grado.admiteNivel` | `CC-Grado-001` | `domains/Grado.xml`, `<boolean name="admiteNivel" transient="true">` con cuerpo de cálculo sobre `niveles` | lectura (derivado, no persistido) |

## Clasificación `cliente` / `servidor` de los campos (filas aplicables, verbatim)

| Entidad | Campo | Origen | Motivo |
|---|---|---|---|
| `Grado` | `code` | cliente | En `Input AllowProperties` de Crear y Modificar (`entity-Grado.md`). |
| `Grado` | `name` | cliente | En `Input AllowProperties` de Crear y Modificar. |
| `Grado` | `niveles` | servidor | Lado **inverso** de `Nivel.grado`: no se persiste desde `Grado`, no aparece en ninguna línea `Input AllowProperties` y nadie lo dicta desde el cliente. Derivado de solo lectura: sin `R-` que lo asigne y **fuera** de las dos whitelists. |
| `Grado` | `admiteNivel` | servidor | `CC-Grado-001`, `momento: lectura`, `sobreescribible: nunca`. Campo derivado no persistido: el getter generado **recalcula** el valor en cada lectura, así que no necesita `R-` que lo asigne y **no puede** conservar nada que llegue del cliente. Fuera de las dos whitelists. |

Ningún campo `servidor` del delta se asigna en una acción, luego **no hay ninguna `R-` en este diseño**: los dos son derivados de solo lectura, el caso que `design-contract.md` §3 exime expresamente de tener una `R-Antes`. Tampoco hay reglas `RN-` en la especificación.
