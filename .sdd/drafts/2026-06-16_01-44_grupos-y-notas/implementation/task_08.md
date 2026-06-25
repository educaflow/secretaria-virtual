---
type: implementation-task
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

Implementa el servicio de `ModuloGrupo` (interfaz + DTO + implementación). Alta **programática** vía DTO (la invoca `GrupoServiceImpl` al generar los módulos del grupo; no hay UI/REST de alta).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/ModuloGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz ModelService de ModuloGrupo |
| `system/gruposnotas/service/impl/ModuloGrupoServiceImpl.java` | Crear | k-sistemas | Implementación de ModuloGrupo |

Del diseño, Paso 2 — Servicios. **MUST NOT** crear módulo Guice (los descubre `ModelServiceFactory`). Persistir siempre con `repository`, nunca `super.*`. Cada acción empieza con `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid)`.

```java
// Clase: com.educaflow.system.gruposnotas.service.ModuloGrupoService  (extends ModelService<ModuloGrupo>)
public interface ModuloGrupoService extends ModelService<ModuloGrupo> {
    ModuloGrupo insert(ModuloGrupoInsertDTO dto);
    Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto);
}
// DTO: com.educaflow.system.gruposnotas.service.ModuloGrupoInsertDTO (record: Grupo grupo, Modulo modulo)

// Clase: com.educaflow.system.gruposnotas.service.impl.ModuloGrupoServiceImpl
public ModuloGrupoServiceImpl(Class<ModuloGrupo> model, Repository<ModuloGrupo> repository);
// @Inject ModelServiceFactory modelServiceFactory;   // para crear las notas de cada alumno ya presente (no aplica en alta normal)

@Override public ModuloGrupo insert(ModuloGrupoInsertDTO dto);
//   Alta PROGRAMÁTICA (la invoca GrupoServiceImpl al generar los módulos del grupo; no hay UI/REST de alta).
//   1) validateInsert(dto).ifPresent(throwIfInvalid).
//   2) construye ModuloGrupo(grupo, modulo) y return repository.save(...).
//   El DTO es la whitelist (no hay AllowProperties): ningún campo servidor sin justificar.

@Override public Optional<BusinessMessages> validateInsert(ModuloGrupoInsertDTO dto);
//   Comprueba que grupo y modulo no son nulos (integridad del DTO). Sin mensajes de negocio del spec.
//   NOTA: la unicidad RES-003 la garantiza el unique-constraint del dominio.
```

> No se declara `AllowProperties` para `ModuloGrupo`: no hay `@CallMethod` ni alta del cliente (los módulos del grupo no se editan; `Input AllowProperties` de ModuloGrupo está vacío en el spec). El alta es programática vía DTO.

### DTO de alta programática (Frontera de confianza)

- **`ModuloGrupoInsertDTO(Grupo grupo, Modulo modulo)`** — el DTO es la whitelist. `grupo` y `modulo` los aporta el servidor (`GrupoServiceImpl`), no el cliente. No hay campos `servidor` injustificados.

**Verificar:** que el `*ServiceImpl` está en `service.impl` con el nombre exacto `ModuloGrupoServiceImpl` y el constructor `(Class<T>, Repository<T>)`; `grep` confirma que no hay `super.insert/update/remove`.

### Trazabilidad relevante
| V | Origen spec | Ubicación |
|---|-------------|-----------|
| V-ModuloGrupo-001 | RES-003 | `domains/ModuloGrupo.xml` `unique-constraint(grupo,modulo)` |
