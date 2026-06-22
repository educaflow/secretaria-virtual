---
type: implementation-task
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-code-quality

Implementa el servicio de `Nota`: la interfaz `NotaService` y su implementación `NotaServiceImpl` (validaciones, fijado de fechas en servidor y acción `guardarNota`).

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `system/gruposnotas/service/NotaService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de Nota |
| `system/gruposnotas/service/impl/NotaServiceImpl.java` | Crear | k-sistemas, k-validaciones, k-secure-coding | Validaciones, fechas y acción guardarNota |

Extiende `ModelService`/`DefaultModelService`, descubierto por `ModelServiceFactory` por convención (sin módulo Guice). Persiste con `repository.save/remove`, **nunca** `super.*`.

### `com.educaflow.system.gruposnotas.service.NotaService` / `…impl.NotaServiceImpl`

```java
public interface NotaService extends ModelService<Nota> {
    Nota guardarNota(Nota nota, Nota notaOriginal);
    Optional<BusinessMessages> validateGuardarNota(Nota nota, Nota notaOriginal);
    AllowProperties allowPropertiesGuardarNota();
}

public class NotaServiceImpl extends DefaultModelService<Nota> implements NotaService {

    @Inject NotaRepository notaRepository;

    @Inject public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository) { super(model, repository); }

    @Override
    public Optional<BusinessMessages> validateInsert(Nota nota);
    //   Aplica:
    //     - V-Nota-005 (Origen spec: RES-006) una nota por alumno+módulo: respaldo de la
    //       unique-constraint(moduloGrupo,alumnoGrupo). Mensaje transmite: ya existe nota para ese alumno y módulo.
    //   (El alta normal de notas la hace el servidor desde R-AlumnoGrupo-001 con valor NO_EVALUADO.)

    @Override
    public Nota guardarNota(Nota nota, Nota notaOriginal);
    //   1. validateGuardarNota(nota, notaOriginal).ifPresent(throwIfInvalid)  → V-Nota-001..003.
    //   2. fireActionRule_FijarFechasCalificacion(nota, notaOriginal)  → R-Nota-001/002 (Antes).
    //   3. return repository.save(nota).

    @Override
    public Optional<BusinessMessages> validateGuardarNota(Nota nota, Nota notaOriginal);
    //   Aplica:
    //     - V-Nota-001 (Origen spec: VAL-016) valor en dominio: comprueba que `valor` es uno de
    //       {NO_EVALUADO, NOTA_01..NOTA_10, MATRICULA_HONOR} (el enum lo garantiza, pero se valida en
    //       servidor para la vía REST). Mensaje transmite: la nota debe ser No evaluado, un número entero
    //       del 1 al 10 o Matrícula de Honor.
    //     - V-Nota-002 (Origen spec: VAL-015) grupo ABIERTO: nota.getModuloGrupo().getGrupo().getEstado()==ABIERTO.
    //       Mensaje transmite: no se pueden modificar las notas de un grupo cerrado.
    //     - V-Nota-003 (Origen spec: VAL-017) máx 3 matrículas por módulo: si el valor que se pone es
    //       MATRICULA_HONOR y notaOriginal.valor != MATRICULA_HONOR, comprueba que
    //       notaRepository.countMatriculasHonorByModuloGrupo(moduloGrupo) < 3. Mensaje transmite: no se pueden
    //       poner más de 3 matrículas de honor en un módulo.

    private void fireActionRule_FijarFechasCalificacion(Nota nota, Nota notaOriginal);
    //   Aplica R-Nota-001 (Origen spec: CC-002; campo `fechaCalificacion` servidor) y R-Nota-002
    //   (Origen spec: CC-003; campo `fechaUltimaModificacion` servidor). Momento: Antes de save.
    //   Lógica:
    //     - Si notaOriginal.valor == NO_EVALUADO && nota.valor != NO_EVALUADO  → primera calificación:
    //       asignación INCONDICIONAL nota.setFechaCalificacion(LocalDateTime.now()).
    //     - Si notaOriginal.valor != NO_EVALUADO && el valor cambia  → modificación posterior:
    //       asignación INCONDICIONAL nota.setFechaUltimaModificacion(LocalDateTime.now()).
    //   En todo caso se restauran desde notaOriginal las fechas que esta rama no toca, para que el cliente
    //   no las pueda dictar. MUST NOT usar `if (campo==null) set(...)` (k-secure-coding §3.3). Estos dos
    //   campos quedan FUERA de la whitelist de guardarNota (solo `valor` es cliente).

    @Override public AllowProperties allowPropertiesInsert();        // denyAll: las notas las crea el servidor (R-AlumnoGrupo-001)
    @Override public AllowProperties allowPropertiesUpdate();        // denyAll: la nota se cambia por guardarNota, no por update genérico
    @Override public AllowProperties allowPropertiesRemove();        // denyAll
    @Override public AllowProperties allowPropertiesGuardarNota();   // whitelist: valor (solo)
}
```

### Frontera de confianza — AllowProperties por acción

#### `NotaServiceImpl.guardarNota` (invocado desde `NotaController.guardarNota`)

Entidad: `Nota`. **Forma elegida**: `createAllowProperties` (whitelist).
**Origen spec:** `Input AllowProperties` de la acción `Modificar` de `entity-Nota.md` (valor).

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `valor` | cliente | sí | Único campo que el usuario edita. Validado por V-Nota-001/003. |
| `moduloGrupo` | servidor | **NO** | Inmutable; se opera sobre la nota por id, se restaura desde `original`. |
| `alumnoGrupo` | servidor | **NO** | Inmutable; restaurado desde `original`. |
| `fechaCalificacion` | servidor | **NO** | `fireActionRule_FijarFechasCalificacion` (incondicional, R-Nota-001). |
| `fechaUltimaModificacion` | servidor | **NO** | `fireActionRule_FijarFechasCalificacion` (incondicional, R-Nota-002). |

### Trazabilidad V/R → ubicación (filas de esta tarea)

| V | Origen spec | Ubicación |
|---|---|---|
| V-Nota-001 | VAL-016 | NotaServiceImpl.validateGuardarNota (+ enum ValorNota en Nota.xml) |
| V-Nota-002 | VAL-015 | NotaServiceImpl.validateGuardarNota |
| V-Nota-003 | VAL-017 | NotaServiceImpl.validateGuardarNota (+ NotaRepository.countMatriculasHonorByModuloGrupo) |
| V-Nota-005 | RES-006 | NotaServiceImpl.validateInsert (+ unique-constraint en Nota.xml) |

| R | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Nota-001 | CC-002 | NotaServiceImpl.fireActionRule_FijarFechasCalificacion (campo `fechaCalificacion`) | Antes de save (en `guardarNota`) |
| R-Nota-002 | CC-003 | NotaServiceImpl.fireActionRule_FijarFechasCalificacion (campo `fechaUltimaModificacion`) | Antes de save (en `guardarNota`) |

> Nota de numeración: en `Nota` el cálculo de CC-002/CC-003 lo hace un único método; se han numerado R-Nota-001 (fechaCalificacion/CC-002) y R-Nota-002 (fechaUltimaModificacion/CC-003) por el campo servidor que cada una asegura, aunque comparten método host.
