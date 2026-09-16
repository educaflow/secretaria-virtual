---
type: implementation-task
template: expediente
---

# Tarea 17 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-tipo-expediente
- k-secure-coding

Escribe el test unitario

```
src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java
```

que cubre `ControlDeAcceso.exigeSerElCreador` y `ControlDeAcceso.exigeMismoCentroQueElExpediente` (el tercer método, `exigeOstentarElPerfilDelEstado`, queda **excluido** por el motivo que declara la tabla de exclusiones).

**Por qué esta tarea existe.** Un tipo de expediente **no genera tests propios**: su conformidad la dan los tests ya existentes y escritos a mano de `src/test/java/com/educaflow/tiposexpedientes/`. La **única excepción** es la de una pieza de **lógica de negocio pura y aislable** que **no** vive en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`, y `design/test-unit-desc.md` describe exactamente eso para esta clase. El test va en el **mismo paquete** de la clase bajo `src/test/java/...`.

**CRITICAL — la descripción de `test-unit-desc.md` que va abajo es contrato fijo y la superficie es cerrada: MUST NOT escribirse ningún test que no liste, ni omitirse ninguno de los que lista, ni cambiarse su Arrange/Act/Assert.**

- **MUST NOT** editarse, ampliarse, debilitarse ni exonerarse ningún test existente de `src/test/java/com/educaflow/tiposexpedientes/` ni de `src/test/java/com/educaflow/views/`.
- **MUST NOT** añadirse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com/educaflow/views`.
- **MUST NOT** editarse ningún fichero de `src/test/e2e/**`.
- **MUST NOT** modificarse el código de producción para que el test pase: si el test no pasa, el fallo se reporta.

## `test-unit-desc.md` — «Tests nuevos a crear» (verbatim)

## Tests nuevos a crear

Siete ficheros, todos bajo `src/test/java/`, uno por clase o por fichero de reglas:

| Fichero de test | Cubre |
|---|---|
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAccesoTest.java` | `exigeSerElCreador` y `exigeMismoCentroQueElExpediente` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirectorTest.java` | `borrar` |
| `src/test/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatriculaTest.java` | `ReglasAnulacionMatricula.esRechazo` (desde Java, que es además la forma de llamada que exige el `@JvmStatic`) |
| `src/test/java/com/educaflow/subsystem/sistemaeducativo/db/CicloTest.java` | el campo derivado `Ciclo.gradoNivel` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/RequiredTest.kt` | `Required` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/StringRulesTest.kt` | `MinLength`, `MaxLength` y `Pattern` |
| `src/test/java/com/educaflow/base/infrastructure/validation/rules/NoAdmitidoTest.java` | `NoAdmitido` |

`StringRulesTest` cubre las tres reglas que viven en `StringRules.kt`. Los tests de las reglas de `base` van en Kotlin cuando necesitan nombrar los parámetros opcionales y la nulabilidad de la propia regla (`Required`, `MinLength`, `MaxLength`, `Pattern`) y en Java cuando solo construyen la regla y leen su mensaje (`NoAdmitido`). **MUST NOT** añadirse ningún fichero bajo `src/test/java/com/educaflow/tiposexpedientes/` ni bajo `com.educaflow.views`.

## Descripción del test (verbatim, íntegra)

### Clase: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ControlDeAcceso`

**Responsabilidad:** concentrar las guardas de identidad del usuario autenticado que los ocho `trigger*` de las tres fases llaman en su primera línea (§9.0.1). El mensaje llega **por parámetro** porque la especificación fija uno distinto en cada evento; la decisión vive en un solo sitio.
**Por qué sí se testea:** la define el diseño (tabla §6 y §9.0.1), no es ninguno de los tres managers, decide con ramas (compara identidades y lanza o deja pasar) y los dos métodos descritos aquí son aislables: no tocan base de datos, ni PDF, ni `EventContext`, ni la clase `States`.
**Colaboradores a mockear:** `SecurityUtil` (estático, `Mockito.mockStatic`, para el usuario autenticado) e `I18n` (estático, programado para devolver su propio argumento, porque sin el contexto de Axelor arrancado no hay traducción y lo que se comprueba es **qué texto elige** la guarda). Las entidades (`AnulacionMatriculaCicloFormativoV1`, `User`, `Centro`) se construyen a mano con sus setters, sin mocks.
**Método excluido:** `exigeOstentarElPerfilDelEstado`, por el motivo de la tabla de exclusiones.

#### Método: `public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeSerElCreador_usuarioAutenticadoEsElRegistrador_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `usuarioRegistrador` = un `User` de `id` 7; `SecurityUtil.getUser()` devuelve un `User` de `id` 7.
  - **Act:** invocar la guarda con un mensaje cualquiera.
  - **Assert:** no lanza (el test pasa por no lanzar nada).
- **`exigeSerElCreador_usuarioAutenticadoDistintoDelRegistrador_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `usuarioRegistrador` de `id` 7; `SecurityUtil.getUser()` devuelve un `User` de `id` 8; mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` cuyo `getMessage()` es exactamente `"Solo puede modificar sus propias solicitudes"`.
- **`exigeSerElCreador_cadaLlamanteRecibeSuPropioMensaje`** — Tipo: borde.
  - **Arrange:** el mismo escenario de fallo, invocado dos veces con dos mensajes distintos: `"Solo puede modificar sus propias solicitudes"` (el de `CONTINUAR`) y `"Solo puede borrar sus propias solicitudes"` (el de `DELETE`).
  - **Act:** invocar la guarda dos veces.
  - **Assert:** cada excepción lleva **su** mensaje, literal; la guarda no compone ni sustituye ningún texto propio.
- **`exigeSerElCreador_expedienteSinUsuarioRegistrador_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `usuarioRegistrador` a `null`; `SecurityUtil.getUser()` devuelve un `User` de `id` 7; mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con ese mismo mensaje — la guarda es **total** y falla cerrado cuando le falta el dato con el que decidir; **MUST NOT** dejar pasar.
- **`exigeSerElCreador_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `usuarioRegistrador` de `id` 7; `SecurityUtil.getUser()` devuelve `null`; mensaje `"Solo puede modificar sus propias solicitudes"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con ese mensaje, sin `NullPointerException`.

#### Método: `public static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException`

- **`exigeMismoCentro_centroActivoIgualAlDelExpediente_noLanza`** — Tipo: happy.
  - **Arrange:** expediente con `centro` de `id` 3; `SecurityUtil.getUser()` devuelve un `User` cuyo `centroActivo` tiene `id` 3.
  - **Act:** invocar la guarda.
  - **Assert:** no lanza.
- **`exigeMismoCentro_centroActivoDistinto_lanzaConElMensajeRecibido`** — Tipo: error.
  - **Arrange:** expediente con `centro` de `id` 3; usuario con `centroActivo` de `id` 4; mensaje `"Solo puede revisar solicitudes de su propio centro"`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` cuyo `getMessage()` es exactamente `"Solo puede revisar solicitudes de su propio centro"`.
- **`exigeMismoCentro_expedienteSinCentro_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` a `null`; usuario con `centroActivo` de `id` 3.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con el mensaje recibido (falla cerrado).
- **`exigeMismoCentro_usuarioSinCentroActivo_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id` 3; usuario con `centroActivo` a `null`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con el mensaje recibido.
- **`exigeMismoCentro_sinUsuarioAutenticado_lanza`** — Tipo: borde.
  - **Arrange:** expediente con `centro` de `id` 3; `SecurityUtil.getUser()` devuelve `null`.
  - **Act:** invocar la guarda.
  - **Assert:** lanza `BusinessException` con el mensaje recibido, sin `NullPointerException`.

