---
type: implementation-task
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Implementa el servicio de `ModuloGrupo`: la interfaz `ModuloGrupoService` y su implementación `ModuloGrupoServiceImpl` (restricción de unicidad módulo–grupo; no editable).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/ModuloGrupoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de ModuloGrupo |
| `system/gruposnotas/service/impl/ModuloGrupoServiceImpl.java` | Crear | k-sistemas, k-validaciones | Restricciones de ModuloGrupo |

Extiende `ModelService`/`DefaultModelService`, descubierto por `ModelServiceFactory` por convención (sin módulo Guice). Persiste con `repository.save/remove`, **nunca** `super.*`.

### `com.educaflow.system.gruposnotas.service.ModuloGrupoService` / `…impl.ModuloGrupoServiceImpl`

```java
public interface ModuloGrupoService extends ModelService<ModuloGrupo> { }

public class ModuloGrupoServiceImpl extends DefaultModelService<ModuloGrupo> implements ModuloGrupoService {
    @Inject public ModuloGrupoServiceImpl(Class<ModuloGrupo> model, Repository<ModuloGrupo> repository) { super(model, repository); }

    @Override
    public Optional<BusinessMessages> validateInsert(ModuloGrupo moduloGrupo);
    //   Aplica:
    //     - V-ModuloGrupo-001 (Origen spec: RES-003) módulo no repetido en el grupo: respaldo de la
    //       unique-constraint(grupo,modulo); comprueba que no exista ya un ModuloGrupo con ese grupo y módulo.
    //       Mensaje transmite: el módulo ya está en el grupo.

    @Override
    public ModuloGrupo update(ModuloGrupo moduloGrupo, ModuloGrupo original);
    //   Defensa en profundidad (k-secure-coding §9.2): ModuloGrupo lo crea el sistema (R-Grupo-001) y NO es
    //   editable. Lanza UnsupportedOperationException incondicionalmente, además del allowPropertiesUpdate
    //   denyAll, para que la invariante de no-editabilidad sea un rechazo explícito de la operación.

    @Override public AllowProperties allowPropertiesInsert();   // denyAll: los crea el servidor (R-Grupo-001), el cliente no envía nada
    @Override public AllowProperties allowPropertiesUpdate();   // denyAll: ModuloGrupo no se edita
}
```

### Trazabilidad V → ubicación (filas de esta tarea)

| V | Origen spec | Ubicación |
|---|---|---|
| V-ModuloGrupo-001 | RES-003 | ModuloGrupoServiceImpl.validateInsert (+ unique-constraint en ModuloGrupo.xml) |
