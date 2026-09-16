---
type: implementation-task
template: expediente
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

## Qué hay que hacer

Escribe las tres clases auxiliares que viven en la **raíz de la versión** (fuera de las carpetas de fase) y de las que dependen los `PhaseEventManagerImpl` y los `StateEventValidatorImpl` de las tres fases; por eso esta tarea va **antes** que las de fase:

- `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAcceso.java` (FQCN `…v1.ControlDeAcceso`)
- `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirector.java` (FQCN `…v1.DevolucionDelDirector`)
- `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatricula.kt` (paquete `…v1`, con la regla del DSL `SinOtraSolicitudEnCursoParaElMismoCiclo`)

Ninguno de los tres lo genera `CreateFilesTask`: se crean de cero.

La especificación de §9.0 y §10.0 que va copiada abajo es **contrato fijo** y la **superficie es cerrada**: **MUST NOT** crearse ningún método, clase, campo ni acción que esas especificaciones no listen (en particular, `DevolucionDelDirector` declara **un solo** método). Para el usuario autenticado **MUST** usarse `SecurityUtil.getUser()`, nunca `AuthUtils.getUser()`; el filtro de la regla **MUST** usar parámetros con nombre, nunca concatenación de cadenas.

## Filas de la tabla «## 6. Ficheros a crear o modificar» del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAcceso.java` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (§9.0.1) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirector.java` | Crear | `k-tipo-expediente`, `k-code-quality` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (§9.0.2): dueño único de «la devolución del director» |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatricula.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (§10.0) |

## `### Paso 9 — Clases auxiliares de la versión` del diseño (verbatim)

### Paso 9 — Clases auxiliares de la versión

Ficheros: `…/v1/ControlDeAcceso.java` (FQCN `…v1.ControlDeAcceso`), `…/v1/DevolucionDelDirector.java` (FQCN `…v1.DevolucionDelDirector`) y `…/v1/ReglasAnulacionMatricula.kt` (paquete `…v1`).

Sus especificaciones quirúrgicas están en **`## 9. Especificación de los PhaseEventManagerImpl`** (§9.0.1 `ControlDeAcceso` y §9.0.2 `DevolucionDelDirector`) y en **`## 10. Especificación de los StateEventValidatorImpl`** (§10.0 `SinOtraSolicitudEnCursoParaElMismoCiclo`). Van antes de los `PhaseEventManagerImpl` y de los validadores porque ambos las usan.

**Verificación:** compilan; `ControlDeAcceso` y `DevolucionDelDirector` no tienen estado ni constructor público; `DevolucionDelDirector` declara **un solo** método (`borrar`) y ningún `trigger*` vuelve a escribir la lista `motivoDevolucion, fechaDevolucion, devueltoPor`; `SinOtraSolicitudEnCursoParaElMismoCiclo` implementa `ValidationRule`.


## `### 9.0 Clases auxiliares compartidas de la versión` del diseño, ÍNTEGRA (verbatim)

## 9. Especificación de los PhaseEventManagerImpl

### 9.0 Clases auxiliares compartidas de la versión

Dos clases `final` sin estado, con constructor privado, en la **raíz de la versión** (fuera de las carpetas de fase), a las que los `trigger*` de las tres fases **delegan**. **MUST NOT** convertirse en una superclase: el dispatcher usa `getDeclaredMethods()` (§9.1.4).

#### 9.0.1 `ControlDeAcceso`

Fichero `…/v1/ControlDeAcceso.java`. Concentra las comprobaciones de **quién es el usuario autenticado**. La usan los `trigger*` de las tres fases como **guarda en su primera línea**; ninguna vive en el validador (ver §11 y `decisiones.md` D4).

```java
public final class ControlDeAcceso {
    public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
    public static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
}
```

- `exigeSerElCreador` compara el `id` de `expediente.getUsuarioRegistrador()` con el de `SecurityUtil.getUser()`; si no coinciden (o alguno es nulo) lanza `new BusinessException(I18n.get(mensaje))`.
- `exigeMismoCentroQueElExpediente` compara el `id` de `expediente.getCentro()` con el de `SecurityUtil.getUser().getCentroActivo()`; si no coinciden (o alguno es nulo) lanza `new BusinessException(I18n.get(mensaje))`.
- **MUST** usarse `SecurityUtil.getUser()`, nunca `AuthUtils.getUser()`.
- Las dos son **totales**: para cualquier entrada, o dejan pasar o fallan con motivo; no devuelven «vale» ante un dato ausente.
- El mensaje llega por parámetro porque la especificación fija uno distinto en cada evento; la **decisión** (quién puede) está en un solo sitio.

#### 9.0.2 `DevolucionDelDirector`

Fichero `…/v1/DevolucionDelDirector.java`, con la misma forma que `ControlDeAcceso`. Es el **dueño único** de qué campos forman «la devolución del director»: la terna `motivoDevolucion` + `fechaDevolucion` + `devueltoPor`, que cuatro `trigger*` de tres fases distintas tienen que dejar limpia.

```java
public final class DevolucionDelDirector {
    public static void borrar(AnulacionMatriculaCicloFormativoV1 expediente);
}
```

- **Por qué se llama así.** La clase nombra **la cosa** de la que es dueña —la devolución que el director hace a secretaría— y el método dice **qué le hace**: `DevolucionDelDirector.borrar(expediente)` se lee entero en la línea de llamada, sin abrir la clase. **MUST NOT** nombrarse con una palabra del vocabulario del dominio de este trámite —`ciclo`, `Ciclo`, `grado`, `nivel`, `matrícula`— porque aquí «ciclo» es siempre **el ciclo formativo** que se anula (campo `ciclo`, entidad `Ciclo`, panel «Matrícula que se anula», regla `SinOtraSolicitudEnCursoParaElMismoCiclo`): un nombre como `CicloDeRevision` se leería espontáneamente como «algo del ciclo formativo» y obligaría a abrir el fichero para descubrir que no lo es.
- `borrar` pone a `null` `motivoDevolucion`, `fechaDevolucion` y `devueltoPor`. Nada más: no transiciona, no valida y no lanza.
- Es **idempotente** y **total**: si el expediente no venía de una devolución, esos campos ya están vacíos y volver a limpiarlos no cambia nada, así que ningún llamante necesita preguntarse si le toca.
- Lo llaman `triggerPresentar` (RN-007), `triggerEnviarAFirma` (RN-011), `triggerSubsanar` (RN-015) y `triggerFirmar` (RN-020). **El motivo de que exista es exactamente ese**: sin él la lista de los tres campos estaría escrita literalmente en cuatro sitios y quien añadiera un cuarto dato a la devolución tendría que acordarse de los cuatro, fallando en silencio si se dejara uno.
- **MUST NOT** añadírsele un segundo método para limpiar la decisión de secretaría (ni equivalente): esa limpieza tiene **un solo** punto de llamada y, además, no habla de la devolución del director, que es lo único de lo que esta clase es dueña. Una operación entra aquí cuando trata de la devolución del director **y** la comparten **dos o más** triggers.


## `### 10.0 Regla propia del tipo — SinOtraSolicitudEnCursoParaElMismoCiclo` del diseño, ÍNTEGRA (verbatim)

## 10. Especificación de los StateEventValidatorImpl

### 10.0 Regla propia del tipo — `SinOtraSolicitudEnCursoParaElMismoCiclo`

Fichero `…/v1/ReglasAnulacionMatricula.kt` (raíz de la versión). Es una regla del DSL, no lógica de negocio: comprueba el **valor de `ciclo`** contra el resto de expedientes del alumno.

```kotlin
data class SinOtraSolicitudEnCursoParaElMismoCiclo(
    val mensaje: String = "Ya tiene una solicitud de anulación en curso para este ciclo"
) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? { … }
}
```

Comportamiento exigido:

- Sin ciclo no existe ningún duplicado posible, así que la regla es **total**: devuelve válido por su propio significado, no porque otra regla cubra el caso. Que el ciclo sea obligatorio es una exigencia independiente, declarada aparte con `Required` en el mismo `field`.
- Si hay ciclo, busca con `JpaRepository.of(AnulacionMatriculaCicloFormativoV1::class.java).all().filter(...)` los expedientes que cumplan **todo**: `abierto = true`, mismo `usuarioRegistrador` que el del expediente, mismo `ciclo`, mismo `cursoAcademico` y `id` distinto del propio expediente. Si existe alguno, devuelve `BusinessMessages.single(I18n.get(mensaje))`; si no, `null`.
- **Qué hace si `cursoAcademico` es nulo o está en blanco.** VAL-DATOS_SOLICITUD-CONTINUAR-017 exige «mismo ciclo **y mismo curso académico**», y `cursoAcademico` lo rellena el servidor al crear el expediente a partir de `centro.getCurso()`, que puede no estar informado (§8 asignación 4). Sin ese dato la regla **no puede decidir**, y `self.cursoAcademico = :curso` con `:curso` a `null` **no casaría con ninguna fila**: devolvería «válido» en silencio justo cuando le falta lo que necesita, y el duplicado pasaría sin que nadie se entere. Por eso la regla **MUST** fallar explícitamente en ese caso, con `BusinessMessages.single(I18n.get("Su centro no tiene configurado el curso académico; avise a la secretaría del centro"))`. Es un centro mal configurado, no un dato del alumno, y la regla sigue siendo **total**: para cualquier entrada, o deja pasar, o falla con motivo; nunca devuelve «válido» por no saber. Ver §14 nota 15.
- El filtro **MUST** usar **parámetros con nombre** (`:usuario`, `:ciclo`, `:curso`, `:id`), nunca concatenación de cadenas (`k-secure-coding`).
- Un expediente cerrado (`abierto = false`) **no** cuenta: la especificación solo impide dos solicitudes **en curso**.


## Filas de `## 11. Reparto de reglas` que aplican a estas tres clases (verbatim)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Quién es el usuario autenticado (autoría, centro) | **guarda en la primera línea del `trigger*`** | `ControlDeAcceso.exige…` → `BusinessException` |
**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Ver `decisiones.md` D4.
| Regla | Capa |
|---|---|
| VAL-DATOS_SOLICITUD-CONTINUAR-016 | guarda de `triggerContinuar` (`ControlDeAcceso.exigeSerElCreador`) |
| VAL-DATOS_SOLICITUD-CONTINUAR-017 | DSL del validador, regla `SinOtraSolicitudEnCursoParaElMismoCiclo` sobre `ciclo` |
| VAL-DATOS_SOLICITUD-BORRADO-001 | guarda de `triggerDelete` (único sitio posible: `DELETE` no pasa por el validador) |
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 | guarda de `triggerEnviarAFirma` |
| VAL-PENDIENTE_REVISION-SUBSANAR-004 | guarda de `triggerSubsanar` |
| VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 y 002 | guarda de `triggerFirmar` (identidad y disponibilidad del certificado del centro; ninguna habla de un campo del formulario). La **002** se implementa capturando el fallo de la resolución del almacén, no comparando con `null`, porque `getDirector` nunca devuelve `null` (§9.3 acción 2 y §14 nota 14) |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 | guarda de `triggerDevolver` |
