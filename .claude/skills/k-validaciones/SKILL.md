---
name: k-validaciones
description: Cómo se IMPLEMENTAN en código Java y XML las restricciones (`RES-`), validaciones (`VAL-`), reglas de negocio (`RN-`), reglas de UI (`RUI-`) y campos calculados (`CC-`) que la especificación (`/sdd-specification`) ya definió y clasificó. Cubre las capas (modelo XML del dominio, `validate*` del servicio, `fireActionRule_*`, vista cliente) y dónde vive cada regla. NO documenta cómo se especifican ni clasifican esas reglas — eso es trabajo de la spec.
---

# k-validaciones

Las restricciones, validaciones, reglas de negocio, reglas de UI y campos calculados de una entidad se **definen y clasifican en la especificación** (`/sdd-specification`), cada una con su identificador estable (`RES-`/`VAL-`/`RN-`/`RUI-`/`CC-`). Este skill describe **únicamente cómo se materializan en código y XML**: en qué capa vive cada tipo y con qué mecanismo. Lo cargan los skills `sdd-*` y `code-*` al implementar.

**MUST NOT** reabrir aquí qué es cada tipo, cómo se distinguen ni cómo se documentan: para eso, la spec y su guía `sdd-specification/template/README.md`.

---

## 1. Qué implementa cada categoría de la spec

Cada categoría de la spec se materializa en una capa concreta:

| Categoría (spec) | Dónde se implementa | Detalle |
|---|---|---|
| **Restricción** `RES-` | Modelo XML del dominio (atributos declarativos); si la restricción no es declarable, `validate*` en **todas** las operaciones. | [`restricciones.md`](restricciones.md) |
| **Validación** `VAL-` | `validateInsert`/`validateUpdate`/`validateRemove` del `*ServiceImpl` (servidor = fuente de verdad) + cliente XML opcional (solo UX). | [`validaciones.md`](validaciones.md) |
| **Regla de negocio** `RN-` | Método `fireActionRule_<Nombre>` en el `*ServiceImpl`, `Antes` o `Después` de `repository.save/remove`. | [`reglas-negocio.md`](reglas-negocio.md) |
| **Regla de UI** `RUI-` | Atributos `showIf`/`hideIf`/`readonlyIf`/`requiredIf`, `<action-attrs>`, `<action-record>` en la vista. | [`reglas-ui.md`](reglas-ui.md) |
| **Campo calculado** `CC-` | `momento: escritura` → `fireActionRule_*` `Antes` que calcula y asigna el campo. `momento: lectura` → campo derivado (cuerpo Java o `formula="true"`) en el modelo XML. | [`reglas-negocio.md`](reglas-negocio.md) §2 |

Patrones XML cliente listos para copiar: [`examples/ejemplos-validaciones.md`](examples/ejemplos-validaciones.md) (P1–P17).

---

## 2. Origen del valor de cada campo: cliente vs servidor

Ortogonal al tipo de regla, cada campo tiene un **origen de valor** que la spec ya declara vía `AllowProperties`:

- **cliente**: lo aporta el usuario (aparece en la línea `AllowProperties` de la acción) — es validable con una `VAL-`.
- **servidor**: lo dicta el servidor (no aparece en `AllowProperties`) — típicamente una `RN-` con momento `Antes` de `Crear` lo asigna/recalcula **incondicionalmente**.

Las dos cosas se cruzan: un campo `servidor` puede tener `VAL-` que validen el valor que el servidor le pone, y siempre tendrá una `RN-` `Antes` que lo asigna. **Por qué importa** (mass-assignment) y **cómo** se escribe la asignación incondicional de campos `servidor`: ver `[[k-secure-coding]]` §3.

---

## 3. Coexistencia de validaciones y reglas en una misma operación

`VAL-` y `RN-` no compiten — coexisten en el mismo flujo. En `insert`/`update`/`remove` del `*ServiceImpl` el orden es:

```java
@Override
public T insert(T entidad) {
    validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid);   // VAL- (primera línea)
    fireActionRule_X_Antes(entidad);     // RN- que escriben sobre el mismo registro
    entidad = repository.save(entidad);  // persiste (NUNCA super.insert)
    fireActionRule_Y_Despues(entidad);   // RN- con efectos colaterales (correos, PDFs, otros registros)
    return entidad;
}
```

- Las `VAL-` viven en `validateInsert`/`validateUpdate`/`validateRemove`. Si **no** sobrescribes `insert/update/remove`, las dispara `DefaultModelService` (salvaguarda); si los sobrescribes, las disparas **tú** como primera línea con `validateXxx(...).ifPresent(throwIfInvalid)`. Si fallan, abortan la operación.
- Las `RN-` viven en métodos `fireActionRule_<Nombre>` y se ejecutan alrededor de `repository.save/remove` (**nunca** `super.*`): antes si modifican el mismo registro, después si tienen efectos colaterales.

---

## 4. Dónde está cada cosa

- [`restricciones.md`](restricciones.md) — `RES-`: restricciones declarativas en el modelo XML del dominio; restricciones no declarables en `validate*`.
- [`validaciones.md`](validaciones.md) — `VAL-`: capa servidor (`validate*`), salvaguarda de `DefaultModelService`, patrón `action-group` Local → Remote → save, controlador puente, capa cliente.
- [`reglas-negocio.md`](reglas-negocio.md) — `RN-` y `CC-`: métodos `fireActionRule_*`, momento `Antes`/`Después`.
- [`reglas-ui.md`](reglas-ui.md) — `RUI-`: mecanismos de vista (`*If`, `<action-attrs>`, `<action-record>`).
- [`examples/ejemplos-validaciones.md`](examples/ejemplos-validaciones.md) — 17 patrones XML cliente (P1–P17) para copiar y adaptar.
