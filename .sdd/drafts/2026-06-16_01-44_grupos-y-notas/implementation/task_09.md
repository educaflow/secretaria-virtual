---
type: implementation-task
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-validaciones

Implementa el servicio de `Nota` (interfaz + DTO + implementación). Alta **programática** (NO_EVALUADO) vía DTO; modificación de `valor` por REST (`save-modal`).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/NotaService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de Nota |
| `system/gruposnotas/service/impl/NotaServiceImpl.java` | Crear | k-sistemas, k-secure-coding | Implementación de Nota |

Del diseño, Paso 2 — Servicios. **MUST NOT** crear módulo Guice. Persistir siempre con `repository`, nunca `super.*`. Cada acción empieza con `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`.

```java
// Clase: com.educaflow.system.gruposnotas.service.NotaService  (extends ModelService<Nota>)
public interface NotaService extends ModelService<Nota> {
    Nota insert(NotaInsertDTO dto);
    Optional<BusinessMessages> validateInsert(NotaInsertDTO dto);
}
// DTO: com.educaflow.system.gruposnotas.service.NotaInsertDTO (record: ModuloGrupo moduloGrupo, AlumnoGrupo alumnoGrupo)

// Clase: com.educaflow.system.gruposnotas.service.impl.NotaServiceImpl
public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository);

@Override public Nota insert(NotaInsertDTO dto);
//   Alta PROGRAMÁTICA (la invoca AlumnoGrupoServiceImpl al crear las notas NO_EVALUADO; no hay UI/REST de alta).
//   1) validateInsert(dto).ifPresent(throwIfInvalid).
//   2) construye Nota(moduloGrupo, alumnoGrupo, valor=NO_EVALUADO) y return repository.save(...).
//   fechaCalificacion y fechaUltimaModificacion quedan nulas (aún NO_EVALUADO).

@Override public Nota update(Nota nota, Nota original);
//   1) validateUpdate(nota, original).ifPresent(throwIfInvalid)  → V-Nota-002/003/004.
//   2) fireActionRule_RestaurarCamposInmutables(nota, original)  → restaura moduloGrupo, alumnoGrupo desde `original`.
//   3) fireActionRule_AsignarFechaCalificacion(nota, original)   → R-Nota-001 (CC-002).
//   4) fireActionRule_AsignarFechaUltimaModificacion(nota, original) → R-Nota-002 (CC-003).
//   5) return repository.save(nota).

@Override public Optional<BusinessMessages> validateInsert(NotaInsertDTO dto);
//   Comprueba que moduloGrupo y alumnoGrupo no son nulos (integridad del DTO).

@Override public Optional<BusinessMessages> validateUpdate(Nota nota, Nota original);
//   - V-Nota-002 (Origen spec: VAL-015) el grupo de la nota (moduloGrupo.grupo) está ABIERTO. Mensaje literal:
//       "No se pueden modificar las notas de un grupo cerrado".
//   - V-Nota-003 (Origen spec: VAL-016) el valor pertenece al dominio {NO_EVALUADO, NOTA_1..NOTA_10,
//       MATRICULA_HONOR}. (Defensa por si llega un valor crudo por /ws/rest fuera del enum.) Mensaje literal:
//       "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor".
//   - V-Nota-004 (Origen spec: VAL-017) si el nuevo valor es MATRICULA_HONOR, en el módulo no hay ya 3
//       notas con MATRICULA_HONOR (NotaRepository.countMatriculasHonorByModuloGrupo excluyendo la nota actual).
//       Mensaje literal: "No se pueden poner más de 3 matrículas de honor en un módulo".

/******** AllowProperties ********/

@Override public AllowProperties allowPropertiesUpdate();
//   Whitelist (createAllowProperties): valor  (único campo cliente en Modificar).
//   moduloGrupo, alumnoGrupo (inmutables) y fechaCalificacion, fechaUltimaModificacion (servidor) quedan FUERA.

/******** Action Rules ********/

private void fireActionRule_AsignarFechaCalificacion(Nota nota, Nota original);
//   Aplica R-Nota-001 (Origen spec: CC-002; campo `fechaCalificacion` servidor). Si la nota pasa de
//   NO_EVALUADO (original) a un valor distinto y fechaCalificacion aún está vacía, asigna
//   INCONDICIONALMENTE nota.setFechaCalificacion(LocalDateTime.now()). La condición de transición se evalúa
//   contra `original` (servidor), no contra un flag del cliente; el cliente NO puede dictar fechaCalificacion
//   (está fuera de la whitelist). No es el anti-patrón `if (campo==null) set`: la guarda es la transición de
//   estado de la nota leída del servidor, no la nulidad de un campo que el cliente podría rellenar.

private void fireActionRule_AsignarFechaUltimaModificacion(Nota nota, Nota original);
//   Aplica R-Nota-002 (Origen spec: CC-003; campo `fechaUltimaModificacion` servidor). Si el valor cambia
//   respecto a `original` y la nota YA tenía fechaCalificacion (ya estaba calificada antes), asigna
//   INCONDICIONALMENTE nota.setFechaUltimaModificacion(LocalDateTime.now()). El cliente NO puede dictar este
//   campo (fuera de la whitelist).

private void fireActionRule_RestaurarCamposInmutables(Nota nota, Nota original);
//   Aplica R-Nota-003 (Origen spec: — ; defensa técnica): restaura INCONDICIONALMENTE moduloGrupo y
//   alumnoGrupo desde `original` (no se reparenta una nota en update).
```

> La nota inicial es `NO_EVALUADO` (RN-005). `fechaCalificacion`/`fechaUltimaModificacion` quedan nulas en el alta programática.

### Frontera de confianza — AllowProperties por acción

#### `NotaServiceImpl.update` (vía REST genérico `save-modal`)

Entidad: `Nota`. **Forma elegida:** `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-Nota.md`.

| Campo                     | Origen   | En whitelist | Justificación / Ubicación de la asignación |
|---------------------------|----------|--------------|---------------------------------------------|
| `valor`                   | cliente  | sí           | Único campo editable. |
| `moduloGrupo`             | servidor | **NO**       | Inmutable; restaurado desde `original`. |
| `alumnoGrupo`             | servidor | **NO**       | Inmutable; restaurado desde `original`. |
| `fechaCalificacion`       | servidor | **NO**       | Asignada en `update` → `fireActionRule_AsignarFechaCalificacion` (CC-002). |
| `fechaUltimaModificacion` | servidor | **NO**       | Asignada en `update` → `fireActionRule_AsignarFechaUltimaModificacion` (CC-003). |

#### DTO de alta programática

- **`NotaInsertDTO(ModuloGrupo moduloGrupo, AlumnoGrupo alumnoGrupo)`** — el DTO es la whitelist. Los aporta `AlumnoGrupoServiceImpl`. `valor` lo fija el servicio a NO_EVALUADO (no viene en el DTO). `fechaCalificacion`/`fechaUltimaModificacion` quedan nulas.

**Verificar:** que el `*ServiceImpl` está en `service.impl` con el nombre exacto `NotaServiceImpl` y el constructor `(Class<T>, Repository<T>)`; `grep` confirma que no hay `super.insert/update/remove`.

### Trazabilidad relevante
| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-Nota-001 | RES-006 | `domains/Nota.xml` `unique-constraint(moduloGrupo,alumnoGrupo)` |
| V-Nota-002 | VAL-015 | `NotaServiceImpl.validateUpdate` |
| V-Nota-003 | VAL-016 | `NotaServiceImpl.validateUpdate` (dominio del valor) + cliente: enum `ValorNota` |
| V-Nota-004 | VAL-017 | `NotaServiceImpl.validateUpdate` (`NotaRepository.countMatriculasHonorByModuloGrupo`) |

| R | Origen spec | Ubicación | Momento |
|---|-------------|-----------|---------|
| R-Nota-001 | CC-002 | `NotaServiceImpl.fireActionRule_AsignarFechaCalificacion` (incondicional al pasar de NO_EVALUADO) | Antes de save (update) |
| R-Nota-002 | CC-003 | `NotaServiceImpl.fireActionRule_AsignarFechaUltimaModificacion` (incondicional al cambiar un valor ya calificado) | Antes de save (update) |
| R-Nota-003 | — | `NotaServiceImpl.fireActionRule_RestaurarCamposInmutables` (defensa anti mass-assignment en update) | Antes de save (update) |
