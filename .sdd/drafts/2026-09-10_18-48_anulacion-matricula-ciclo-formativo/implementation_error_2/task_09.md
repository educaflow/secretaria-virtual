---
type: implementation-task
template: expediente
---

# Tarea 09 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-secure-coding
- k-code-quality
- k-validaciones
- k-i18n

## Qué hay que hacer

Escribir las **tres clases auxiliares de la raíz de la carpeta de versión** (fuera de las carpetas de fase), a las que delegan los `trigger*` de las tres fases y el validador de `SOLICITUD`. Van **antes** que las tareas de fase porque esas las usan.

- `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAcceso.java` (FQCN `…v1.ControlDeAcceso`)
- `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirector.java` (FQCN `…v1.DevolucionDelDirector`)
- `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatricula.kt` (paquete `…v1`)

`CreateFilesTask` **no** genera esqueleto para ninguna de las tres: son ficheros nuevos.

**La especificación del diseño es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, clase, campo ni acción que la especificación no liste.** En particular, `DevolucionDelDirector` declara **un solo** método y **MUST NOT** ganar un segundo; y `ControlDeAcceso` y `DevolucionDelDirector` **MUST NOT** convertirse en superclase de ningún `PhaseEventManagerImpl`.

**MUST** usarse `SecurityUtil.getUser()` (`com.educaflow.base.util.SecurityUtil`), **NUNCA** `AuthUtils.getUser()`.

**MUST NOT** subirse ninguna de las tres a `tramites/util/` ni a `subsystem/expedientes`: nacen en la raíz de la carpeta de versión.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAcceso.java` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (§9.0.1) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirector.java` | Crear | `k-tipo-expediente`, `k-code-quality` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (§9.0.2): dueño único de «la devolución del director» |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatricula.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (§10.0) |

## Paso del diseño (verbatim)

### Paso 9 — Clases auxiliares de la versión

Ficheros: `…/v1/ControlDeAcceso.java` (FQCN `…v1.ControlDeAcceso`), `…/v1/DevolucionDelDirector.java` (FQCN `…v1.DevolucionDelDirector`) y `…/v1/ReglasAnulacionMatricula.kt` (paquete `…v1`).

Sus especificaciones quirúrgicas están en **`## 9. Especificación de los PhaseEventManagerImpl`** (§9.0.1 `ControlDeAcceso` y §9.0.2 `DevolucionDelDirector`) y en **`## 10. Especificación de los StateEventValidatorImpl`** (§10.0 `SinOtraSolicitudEnCursoParaElMismoCiclo`). Van antes de los `PhaseEventManagerImpl` y de los validadores porque ambos las usan.

**Verificación:** compilan; `ControlDeAcceso` y `DevolucionDelDirector` no tienen estado ni constructor público; `DevolucionDelDirector` declara **un solo** método (`borrar`) y ningún `trigger*` vuelve a escribir la lista `motivoDevolucion, fechaDevolucion, devueltoPor`; `SinOtraSolicitudEnCursoParaElMismoCiclo` implementa `ValidationRule`.

## `### 9.0 Clases auxiliares compartidas de la versión` (de `## 9. Especificación de los PhaseEventManagerImpl`), ÍNTEGRA (verbatim)

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

## `### 10.0 Regla propia del tipo — SinOtraSolicitudEnCursoParaElMismoCiclo` (de `## 10. Especificación de los StateEventValidatorImpl`), ÍNTEGRA (verbatim)

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

## Filas y párrafos de `## 11. Reparto de reglas` que aplican (verbatim)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Quién es el usuario autenticado (autoría, centro) | **guarda en la primera línea del `trigger*`** | `ControlDeAcceso.exige…` → `BusinessException` |

**Las cuatro guardas de autoría, y por qué son cuatro y no dos.** `CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR` —los cuatro eventos de los dos estados cuyo `profile` es `CREADOR`— llevan **todos** la guarda `ControlDeAcceso.exigeSerElCreador` en su primera línea. Las dos primeras las pide la especificación (VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001); las dos últimas las **añade el diseño**, y la razón es la misma que hace falta para las dos primeras: `Tramitador.checkPerfilDelEstado` **exime al administrador** (`SecurityUtil.isAdmin`), así que el perfil del estado no basta para garantizar que quien dispara el evento sea el creador. Sin ellas, un administrador que abriera la solicitud de otro alumno por «Expedientes Pendientes» —que fija `_profile=CREADOR`— podría disparar `VOLVER` y `PRESENTAR` sobre ella, contra HU-010/ESC-025 (el administrador solo consulta, en solo lectura) y ESC-020/ESC-032/ESC-045 (los no creadores ven la pantalla en solo lectura). **Esto anula expresamente la indicación de `design-guidelines.md`** («no se han añadido comprobaciones de autoría a VOLVER ni a PRESENTAR: serían una copia de lo que el motor ya garantiza»): el motor **no** lo garantiza para el administrador, que es exactamente el caso abierto, y la propia guía reconoce esa excepción al justificar por qué sí se conservan las otras dos. La desviación queda declarada aquí y en §14 nota 25.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Ver `decisiones.md` D4.

| Regla | Capa |
|---|---|
| VAL-DATOS_SOLICITUD-CONTINUAR-016 | guarda de `triggerContinuar` (`ControlDeAcceso.exigeSerElCreador`) |
| VAL-DATOS_SOLICITUD-CONTINUAR-017 | DSL del validador, regla `SinOtraSolicitudEnCursoParaElMismoCiclo` sobre `ciclo` |
| VAL-DATOS_SOLICITUD-BORRADO-001 | guarda de `triggerDelete` (único sitio posible: `DELETE` no pasa por el validador) |
| *(sin identificador en la spec)* — autoría en `VOLVER` | guarda de `triggerVolver` (`ControlDeAcceso.exigeSerElCreador`). **La añade el diseño**, no la spec: cierra el único evento de `PENDIENTE_FIRMA` que el administrador podía disparar sobre la solicitud de otro, porque el motor le exime del perfil del estado (§11, «Las cuatro guardas de autoría», y §14 nota 25) |
| *(sin identificador en la spec)* — autoría en `PRESENTAR` | guarda de `triggerPresentar` (`ControlDeAcceso.exigeSerElCreador`). **La añade el diseño**, por el mismo motivo: sin ella, el administrador podría presentar la solicitud de otro alumno (§11, «Las cuatro guardas de autoría», y §14 nota 25) |
