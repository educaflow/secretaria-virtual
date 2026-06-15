# Restricciones (`RES-`)

Una restricción (`RES-`) es un invariante de la entidad: una condición que debe cumplirse en **toda** operación, no en una acción concreta. Por eso su hogar natural es el **modelo XML del dominio**, donde JPA/Hibernate la aplica automáticamente en cada `save`.

---

## 1. Capa principal: el modelo XML del dominio

La mayoría de `RES-` se implementan de forma **declarativa** con un atributo del dominio Axelor. El formulario hereda automáticamente esos atributos (p.ej. `required="true"` marca el campo como obligatorio en la vista sin declararlo allí).

| Restricción (`RES-`) | Atributo del modelo XML | Aplica a |
|---|---|---|
| El campo es obligatorio siempre (no nulo) | `required="true"` (o `nullable="false"`) | cualquier campo |
| El valor del campo es único en toda la tabla | `unique="true"` | cualquier campo |
| La combinación de columnas es única | `<unique-constraint columns="A,B"/>` | entidad |
| Valor numérico mínimo / máximo (inclusive) | `min="N"` / `max="N"` | `<integer>`, `<decimal>`, `<long>` |
| Longitud mínima / máxima de la cadena | `min="N"` / `max="N"` | `<string>` |
| Total de dígitos / nº de decimales | `precision="P"` / `scale="S"` | `<decimal>` |

**REQUIRED** — el **catálogo completo de atributos del dominio y su sintaxis exacta** (tipos de campo, relaciones, índices, `<unique-constraint>`, etc.) está en [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md). Consúltalo siempre que materialices una `RES-` en el modelo; **MUST NOT** duplicar aquí la sintaxis del dominio.

> Cuando un atributo del modelo cubre la restricción, **no hace falta replicarla** en `validate*`. Replícala en servidor solo si quieres un mensaje personalizado en lugar del genérico de JPA.

---

## 2. Capa de respaldo: `validate*` para restricciones no declarables

Algunas restricciones no se pueden expresar con un atributo del modelo: comparaciones cruzadas entre campos (p.ej. *"la fecha de cierre ≥ la de apertura"*), unicidad de ámbito (única dentro del centro), cardinalidad de hijos, etc.

Esos `RES-` se implementan en el servidor, en `validate*` del `*ServiceImpl`, pero — por ser restricciones — se comprueban en **todas** las operaciones aplicables, no en una sola:

- Una restricción de estado del registro → `validateInsert` **y** `validateUpdate`.
- Una restricción que también deba protegerse al borrar → además `validateRemove`.

La mecánica de `validate*` (firma, `BusinessMessages`, salvaguarda de `DefaultModelService`) es la misma que la de las validaciones: ver [`validaciones.md`](validaciones.md) §3.

> Diferencia con una `VAL-`: la `VAL-` se ancla a **una** acción; el `RES-` se replica en **todas** porque debe cumplirse siempre.

---

## 3. Referencias

- [`../k-sistemas/references/models.md`](../k-sistemas/references/models.md) — sintaxis completa del modelo XML del dominio (atributos, tipos, índices, `<unique-constraint>`).
- [`validaciones.md`](validaciones.md) — mecánica de `validate*` para las restricciones no declarables.
