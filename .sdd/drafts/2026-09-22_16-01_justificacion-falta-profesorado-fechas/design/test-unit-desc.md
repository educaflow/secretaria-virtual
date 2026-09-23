# Tests unitarios — Justificación de falta del profesorado (`JustificacionFaltaProfesoradoV1`)

## No aplican tests unitarios de clases

Para este artefacto **no se describe ningún test unitario** de las clases del tipo de expediente, y los únicos tests nuevos son los de las clases auxiliares descritas en «Clases auxiliares con lógica propia». No es una omisión: es una decisión de contrato, por dos motivos.

**1. La conformidad ya la cubren tests existentes, escritos a mano.** Los tests genéricos de `src/test/java/com/educaflow/tiposexpedientes/` recorren automáticamente todos los tipos de expediente del árbol, así que cubren este tipo por construcción, sin tocar nada. Para este diseño comprueban:

- que cada fase declarada tiene su `PhaseEventManagerImpl` y su `StateEventValidatorImpl` en la carpeta de la fase en minúsculas;
- que hay exactamente un `InitialEventManagerImpl` en la raíz de la versión, con un único `triggerInitialEvent`, parametrizado con la entidad del `domains.xml`;
- que en cada fase hay un `trigger<Evento>` por cada evento de la fase y un `onEnter<Estado>` por cada estado, ninguno de más;
- que en cada fase hay un `getForState<Estado>InEvent<Evento>` por cada pareja (estado, evento) salvo las de `DELETE`, ninguno de más;
- que el `TipoExpedienteInstance.xml` tiene el `events` escrito en todos los estados y perfiles válidos;
- que el `estados.puml` dibuja todos los estados con el alias `<FASE>_<ESTADO>` y sin alias fantasma;
- que los `views.xml` de cada fase tienen el form genérico de cada estado, el form con perfil donde procede, sin duplicados, y que todo evento tiene su botón;
- que no se referencia la clase `States` de otro tipo ni de otra versión, y que el `<defaultTipoExpediente>` apunta a una carpeta de versión que existe;
- que toda expresión Groovy de los `documentospdf/` compila contra la entidad del tipo (propiedades existentes y FQCN que resuelven).

Esos tests **se escriben a mano** y los `.java` son su fuente de verdad: este diseño **no propone crearlos, modificarlos, ampliarlos ni regenerarlos**.

**2. Las clases del tipo no son unitariamente testeables con sentido.** `InitialEventManagerImpl`, `PhaseEventManagerImpl` y `StateEventValidatorImpl` no tienen lógica propia aislable: dependen del `Tramitador`, del `EventContext`, de la persistencia, de la generación y firma de PDF y de la clase `States` generada por el build. Un test unitario tendría que mockear todo eso y acabaría verificando el mock, no el trámite.

**Dónde se verifica el comportamiento real.** End-to-end, en `test-e2e-desc.md`: cada transición de la máquina de estados, cada perfil que la dispara, cada validación que debe impedir avanzar y cada estado de solo lectura. Y en el paso final del diseño, con `./run.sh` y el recorrido en runtime de todos los estados.

## Clases del tipo — excluidas y por qué

| Clase | Motivo de la exclusión |
|---|---|
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.InitialEventManagerImpl` | Sin lógica aislable: inicializa la entidad (solo `updateState` al estado inicial) y su efecto lo ejerce el `Tramitador`. Cubierta por los tests existentes (forma) y por `test-e2e-desc.md` (comportamiento). |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.PhaseEventManagerImpl` | Sin lógica aislable: depende del `Tramitador`, del `EventContext`, de la persistencia y del PDF. La limpieza RN-001 va inline en `triggerGuardarDatos` y solo combina los `necesita*`, que sí se testean en `JustificacionFaltaProfesoradoV1Util`. |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.recepcion.StateEventValidatorImpl` | Declarativa: no ejecuta nada; sus reglas las interpreta el `Tramitador`. Las funciones que referencia con `Lambda`/`ifLambda` se testean en `JustificacionFaltaProfesoradoV1Util`. |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion.PhaseEventManagerImpl` | Sin lógica aislable: depende del `Tramitador`, del `EventContext`, de la persistencia, del PDF y de la firma en servidor. Además el delta no la toca. |
| `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.tramitacion.StateEventValidatorImpl` | Declarativa: no ejecuta nada; sus reglas las interpreta el `Tramitador`. Además el delta no la toca. |

## Tests nuevos a crear

Un único fichero: `src/test/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/JustificacionFaltaProfesoradoV1UtilTest.java` (mismo paquete que la clase bajo test; la carpeta `src/test/java/com/educaflow/tramites/` ya aloja tests de las `<Code>Util` de otros trámites). Nada bajo `src/test/java/com/educaflow/tiposexpedientes/` ni `com.educaflow.views`.

## Clases auxiliares con lógica propia

### Clase: `com.educaflow.tramites.profesores.justificacion_falta_profesorado.actual.v1.JustificacionFaltaProfesoradoV1Util`

**Responsabilidad:** dueño único en servidor de la clasificación «tipo de jornada faltada → campos del periodo» (`necesitaFechaFin`, `necesitaHoraInicio`, `necesitaHoraFin`, que usan RN-001 y el validador) y de las comprobaciones `boolean` del periodo que el validador declara con `Lambda`/`ifLambda` (VAL-001..010): presencia de cada campo, rango de fechas respecto a «hoy» y orden fin > inicio.
**Por qué sí se testea:** cumple las cuatro condiciones: (1) la define el diseño (fila `Crear` de la sección 6 y tabla de 13 métodos de la sección 9); (2) no es ninguno de los tres managers ni subclase suya (clase `final` sin supertipo); (3) tiene lógica propia: tres `switch` exhaustivos sobre el enum, comparaciones de fechas contra «hoy» y comparaciones entre campos con excepción; (4) es aislable: solo lee getters de la entidad, que se instancia con `new JustificacionFaltaProfesoradoV1()` y se rellena con setters, sin `Tramitador`, `EventContext`, BD, PDF ni `States`.
**Colaboradores a mockear:** ninguno. «Hoy» se calcula en el propio test con `LocalDate.now(Convert.defaultZoneId)`, la misma expresión que usa la clase.

Convenciones comunes a todos los tests:

- `hoy` = `LocalDate.now(Convert.defaultZoneId)` calculado en el `Arrange`; `haceUnAnyo` = `hoy.minusYears(1)`.
- Para los tres `necesita*`, cada test cubre los **cinco** casos del `switch` (los cuatro ítems de `TipoJornadaFaltaJustificacionFaltaProfesoradoV1` y `null`); puede escribirse como test parametrizado, con un caso por fila.
- **Decisión documentada (mensaje de las excepciones):** el diseño fija que los métodos 9 y 13 lanzan `IllegalStateException` si falta un operando, pero **no fija el texto** del mensaje. Para no inventar un contrato que el diseño no declara, los tests de error comprueban **solo el tipo** de la excepción; si `/sdd-implementer` fija un mensaje, se añade al aserto.
- **Riesgo conocido:** los tests que dependen de «hoy» pueden fallar si se ejecutan justo al cruzar la medianoche de `Europe/Madrid` entre el `Arrange` y el `Act`; se asume despreciable.

#### Método: `static boolean necesitaFechaFin(JustificacionFaltaProfesoradoV1 expediente)`

- **`necesitaFechaFin_clasificaCadaTipoDeJornada`** — Tipo: happy.
  - **Arrange:** una entidad por caso, con `tipoJornadaFalta` = `UN_DIA_COMPLETO`, `VARIOS_DIAS_COMPLETOS`, `UNAS_HORAS_UN_DIA`, `VARIOS_DIAS_PRIMERO_PARCIAL`.
  - **Act:** `necesitaFechaFin(expediente)` en cada caso.
  - **Assert:** `false`, `true`, `false`, `true` respectivamente.
- **`necesitaFechaFin_sinTipoDeJornada_devuelveFalse`** — Tipo: borde.
  - **Arrange:** entidad con `tipoJornadaFalta` = `null`.
  - **Act:** `necesitaFechaFin(expediente)`.
  - **Assert:** `false` (sin excepción).

#### Método: `static boolean necesitaHoraInicio(JustificacionFaltaProfesoradoV1 expediente)`

- **`necesitaHoraInicio_clasificaCadaTipoDeJornada`** — Tipo: happy.
  - **Arrange:** una entidad por caso, con `tipoJornadaFalta` = `UN_DIA_COMPLETO`, `VARIOS_DIAS_COMPLETOS`, `UNAS_HORAS_UN_DIA`, `VARIOS_DIAS_PRIMERO_PARCIAL`.
  - **Act:** `necesitaHoraInicio(expediente)` en cada caso.
  - **Assert:** `false`, `false`, `true`, `true` respectivamente.
- **`necesitaHoraInicio_sinTipoDeJornada_devuelveFalse`** — Tipo: borde.
  - **Arrange:** entidad con `tipoJornadaFalta` = `null`.
  - **Act:** `necesitaHoraInicio(expediente)`.
  - **Assert:** `false` (sin excepción).

#### Método: `static boolean necesitaHoraFin(JustificacionFaltaProfesoradoV1 expediente)`

- **`necesitaHoraFin_clasificaCadaTipoDeJornada`** — Tipo: happy.
  - **Arrange:** una entidad por caso, con `tipoJornadaFalta` = `UN_DIA_COMPLETO`, `VARIOS_DIAS_COMPLETOS`, `UNAS_HORAS_UN_DIA`, `VARIOS_DIAS_PRIMERO_PARCIAL`.
  - **Act:** `necesitaHoraFin(expediente)` en cada caso.
  - **Assert:** `false`, `false`, `true`, `false` respectivamente.
- **`necesitaHoraFin_sinTipoDeJornada_devuelveFalse`** — Tipo: borde.
  - **Arrange:** entidad con `tipoJornadaFalta` = `null`.
  - **Act:** `necesitaHoraFin(expediente)`.
  - **Assert:** `false` (sin excepción).

#### Métodos de presencia: `tieneTipoJornadaFalta`, `tieneFechaInicio`, `tieneFechaFin`, `tieneHoraInicio`, `tieneHoraFin` (todos `static boolean …(JustificacionFaltaProfesoradoV1 expediente)`)

- **`tiene<Campo>_conValor_devuelveTrue`** (uno por cada uno de los cinco métodos) — Tipo: happy.
  - **Arrange:** entidad con solo ese campo relleno (`tipoJornadaFalta` = `UN_DIA_COMPLETO`; `fechaInicio`/`fechaFin` = `hoy`; `horaInicio`/`horaFin` = `09:00`).
  - **Act:** el `tiene<Campo>` correspondiente.
  - **Assert:** `true`.
- **`tiene<Campo>_sinValor_devuelveFalse`** (uno por cada uno de los cinco métodos) — Tipo: borde.
  - **Arrange:** entidad recién creada, con el campo a `null`.
  - **Act:** el `tiene<Campo>` correspondiente.
  - **Assert:** `false`.

#### Método: `static boolean fechaInicioNoAnteriorAHaceUnAnyo(JustificacionFaltaProfesoradoV1 expediente)`

- **`fechaInicioNoAnteriorAHaceUnAnyo_justoHaceUnAnyo_devuelveTrue`** — Tipo: borde.
  - **Arrange:** `fechaInicio` = `haceUnAnyo`.
  - **Act:** `fechaInicioNoAnteriorAHaceUnAnyo(expediente)`.
  - **Assert:** `true`.
- **`fechaInicioNoAnteriorAHaceUnAnyo_unDiaAntesDeHaceUnAnyo_devuelveFalse`** — Tipo: error.
  - **Arrange:** `fechaInicio` = `haceUnAnyo.minusDays(1)`.
  - **Act:** `fechaInicioNoAnteriorAHaceUnAnyo(expediente)`.
  - **Assert:** `false`.
- **`fechaInicioNoAnteriorAHaceUnAnyo_hoy_devuelveTrue`** — Tipo: happy.
  - **Arrange:** `fechaInicio` = `hoy`.
  - **Act:** `fechaInicioNoAnteriorAHaceUnAnyo(expediente)`.
  - **Assert:** `true`.
- **`fechaInicioNoAnteriorAHaceUnAnyo_sinFecha_devuelveTrue`** — Tipo: borde.
  - **Arrange:** `fechaInicio` = `null`.
  - **Act:** `fechaInicioNoAnteriorAHaceUnAnyo(expediente)`.
  - **Assert:** `true` (la ausencia la señala `tieneFechaInicio`).

#### Método: `static boolean fechaInicioNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente)`

- **`fechaInicioNoPosteriorAHoy_hoy_devuelveTrue`** — Tipo: borde.
  - **Arrange:** `fechaInicio` = `hoy`.
  - **Act:** `fechaInicioNoPosteriorAHoy(expediente)`.
  - **Assert:** `true`.
- **`fechaInicioNoPosteriorAHoy_ayer_devuelveTrue`** — Tipo: happy.
  - **Arrange:** `fechaInicio` = `hoy.minusDays(1)`.
  - **Act:** `fechaInicioNoPosteriorAHoy(expediente)`.
  - **Assert:** `true`.
- **`fechaInicioNoPosteriorAHoy_manyana_devuelveFalse`** — Tipo: error.
  - **Arrange:** `fechaInicio` = `hoy.plusDays(1)`.
  - **Act:** `fechaInicioNoPosteriorAHoy(expediente)`.
  - **Assert:** `false`.
- **`fechaInicioNoPosteriorAHoy_sinFecha_devuelveTrue`** — Tipo: borde.
  - **Arrange:** `fechaInicio` = `null`.
  - **Act:** `fechaInicioNoPosteriorAHoy(expediente)`.
  - **Assert:** `true`.

#### Método: `static boolean fechaFinPosteriorAFechaInicio(JustificacionFaltaProfesoradoV1 expediente)`

- **`fechaFinPosteriorAFechaInicio_finDespues_devuelveTrue`** — Tipo: happy.
  - **Arrange:** `fechaInicio` = `hoy.minusDays(3)`, `fechaFin` = `hoy.minusDays(1)`.
  - **Act:** `fechaFinPosteriorAFechaInicio(expediente)`.
  - **Assert:** `true`.
- **`fechaFinPosteriorAFechaInicio_mismoDia_devuelveFalse`** — Tipo: borde.
  - **Arrange:** `fechaInicio` = `fechaFin` = `hoy.minusDays(1)`.
  - **Act:** `fechaFinPosteriorAFechaInicio(expediente)`.
  - **Assert:** `false` (la regla es «posterior», estricta).
- **`fechaFinPosteriorAFechaInicio_finAntes_devuelveFalse`** — Tipo: error.
  - **Arrange:** `fechaInicio` = `hoy.minusDays(1)`, `fechaFin` = `hoy.minusDays(3)`.
  - **Act:** `fechaFinPosteriorAFechaInicio(expediente)`.
  - **Assert:** `false`.
- **`fechaFinPosteriorAFechaInicio_sinFechaFin_lanzaIllegalState`** — Tipo: error.
  - **Arrange:** `fechaInicio` = `hoy`, `fechaFin` = `null`.
  - **Act:** `fechaFinPosteriorAFechaInicio(expediente)`.
  - **Assert:** lanza `IllegalStateException` (mensaje no fijado por el diseño: ver «Decisión documentada»).
- **`fechaFinPosteriorAFechaInicio_sinFechaInicio_lanzaIllegalState`** — Tipo: error.
  - **Arrange:** `fechaInicio` = `null`, `fechaFin` = `hoy`.
  - **Act:** `fechaFinPosteriorAFechaInicio(expediente)`.
  - **Assert:** lanza `IllegalStateException` (mensaje no fijado por el diseño).

#### Método: `static boolean fechaFinNoPosteriorAHoy(JustificacionFaltaProfesoradoV1 expediente)`

- **`fechaFinNoPosteriorAHoy_hoy_devuelveTrue`** — Tipo: borde.
  - **Arrange:** `fechaFin` = `hoy`.
  - **Act:** `fechaFinNoPosteriorAHoy(expediente)`.
  - **Assert:** `true`.
- **`fechaFinNoPosteriorAHoy_manyana_devuelveFalse`** — Tipo: error.
  - **Arrange:** `fechaFin` = `hoy.plusDays(1)`.
  - **Act:** `fechaFinNoPosteriorAHoy(expediente)`.
  - **Assert:** `false`.
- **`fechaFinNoPosteriorAHoy_sinFecha_devuelveTrue`** — Tipo: borde.
  - **Arrange:** `fechaFin` = `null`.
  - **Act:** `fechaFinNoPosteriorAHoy(expediente)`.
  - **Assert:** `true`.

#### Método: `static boolean horaFinPosteriorAHoraInicio(JustificacionFaltaProfesoradoV1 expediente)`

- **`horaFinPosteriorAHoraInicio_finDespues_devuelveTrue`** — Tipo: happy.
  - **Arrange:** `horaInicio` = `09:00`, `horaFin` = `11:30`.
  - **Act:** `horaFinPosteriorAHoraInicio(expediente)`.
  - **Assert:** `true`.
- **`horaFinPosteriorAHoraInicio_mismaHora_devuelveFalse`** — Tipo: borde.
  - **Arrange:** `horaInicio` = `horaFin` = `09:00`.
  - **Act:** `horaFinPosteriorAHoraInicio(expediente)`.
  - **Assert:** `false`.
- **`horaFinPosteriorAHoraInicio_finAntes_devuelveFalse`** — Tipo: error.
  - **Arrange:** `horaInicio` = `11:30`, `horaFin` = `09:00`.
  - **Act:** `horaFinPosteriorAHoraInicio(expediente)`.
  - **Assert:** `false`.
- **`horaFinPosteriorAHoraInicio_sinHoraFin_lanzaIllegalState`** — Tipo: error.
  - **Arrange:** `horaInicio` = `09:00`, `horaFin` = `null`.
  - **Act:** `horaFinPosteriorAHoraInicio(expediente)`.
  - **Assert:** lanza `IllegalStateException` (mensaje no fijado por el diseño).
- **`horaFinPosteriorAHoraInicio_sinHoraInicio_lanzaIllegalState`** — Tipo: error.
  - **Arrange:** `horaInicio` = `null`, `horaFin` = `11:30`.
  - **Act:** `horaFinPosteriorAHoraInicio(expediente)`.
  - **Assert:** lanza `IllegalStateException` (mensaje no fijado por el diseño).

## Cobertura

- Clases auxiliares descritas: 1 (`JustificacionFaltaProfesoradoV1Util`, sus 13 métodos).
- Clases del tipo excluidas: `InitialEventManagerImpl`, `recepcion.PhaseEventManagerImpl`, `recepcion.StateEventValidatorImpl`, `tramitacion.PhaseEventManagerImpl`, `tramitacion.StateEventValidatorImpl`.
- Tests nuevos a crear: 1 fichero bajo `src/test/java/com/educaflow/tramites/profesores/justificacion_falta_profesorado/actual/v1/` (`JustificacionFaltaProfesoradoV1UtilTest.java`).
