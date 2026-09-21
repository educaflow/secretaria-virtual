---
type: implementation-task
template: system
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

**Alcance de esta tarea:** crear el `enum` `FormaPresentacion` (fila `Crear`). Es Java: se materializa delegando en `developer-code-implementer` (`implementation.md` §2). **Superficie cerrada:** solo el enum, sus dos constantes y los métodos que el diseño lista. El record `ContextoTramitacion` que lo usa es de la tarea 03.

**Del diseño — fila de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/FormaPresentacion.java` | Crear | k-code-quality | `enum` dueño de la equivalencia perfil de inicio ↔ forma de presentar (Paso 2) |

**Del diseño — Paso 2, `FormaPresentacion` (verbatim; el `record ContextoTramitacion` lo cubre la tarea 03)**

### Paso 2 — `ContextoTramitacion` como entrada del motor

**Crear** `com.educaflow.subsystem.expedientes.tramitacion.eventmanager.FormaPresentacion` y `…eventmanager.ContextoTramitacion`.
`FormaPresentacion` es el dueño único de qué perfiles pueden iniciar un expediente y de la equivalencia perfil ↔ forma de presentar: una forma de presentar = una constante del enum (D1, D2).
**Invariante:** el enum tiene **exactamente dos constantes, una por cada valor de `presentadoEnPapel`**, porque la ventana (`NuevoExpediente.presentadoEnPapel`, pregunta de sí/no) y el expediente representan la forma de presentar con un booleano. Añadir una tercera forma **no** es tocar una constante: exige cambiar ese booleano en el modelo y en la ventana, y rediseñar `fromPresentadoEnPapel`, `getFormaPresentarVigente` y `toContextoTramitacion`. Cambiar el perfil de una forma existente sí es tocar solo su constante.
**Test unitario obligatorio** (para `test-unit-desc.md`): comprobar la invariante — para `true` y para `false` hay exactamente una constante en `values()` con ese `isPresentadoEnPapel()`, y `fromPresentadoEnPapel` devuelve esa constante.
`ContextoTramitacion` es un DTO puro de cuatro componentes.

```java
// Clase: com.educaflow.subsystem.expedientes.tramitacion.eventmanager.FormaPresentacion
public enum FormaPresentacion {
    TELEMATICA(Profile.CREADOR, false),
    EN_PAPEL(Profile.TRAMITADOR, true);

    FormaPresentacion(Profile profile, boolean presentadoEnPapel);

    public Profile getProfile();
    //   Perfil con el que se inicia el expediente presentado de esta forma.

    public boolean isPresentadoEnPapel();

    public static FormaPresentacion fromPresentadoEnPapel(boolean presentadoEnPapel);
    //   Filtra values() por isPresentadoEnPapel() == presentadoEnPapel y exige exactamente una: si encuentra cero o
    //   más de una lanza IllegalStateException con un mensaje que diga que se ha roto la invariante del enum (una
    //   constante por cada valor de presentadoEnPapel). No supone la invariante: la hace cumplir.
    //   Lo usa R-NuevoExpediente-001 (el perfil lo decide el sistema a partir de la forma de presentar).

    public static Optional<FormaPresentacion> fromProfile(Profile profile);
    //   La constante cuyo getProfile() coincide; vacío para cualquier perfil que no inicia, incluido null.

    public static boolean esPerfilDeInicio(Profile profile);
    //   fromProfile(profile).isPresent(). Único sitio donde se decide qué perfiles pueden iniciar un expediente
    //   (lo consulta ExpedienteService.getPerfilesDeInicio y, a través de ella, la ventana).
    //   Las tres consultas salen de values(), y la ventana deduce las formas de presentar posibles filtrando
    //   values() (R-NuevoExpediente-004): cambiar el perfil de una forma existente es tocar una sola constante.
    //   Añadir una forma NO lo es (invariante de dos constantes, una por valor de presentadoEnPapel; ver arriba).
}
```

**Del diseño — Trazabilidad de reglas de negocio (R) que usan la equivalencia perfil ↔ forma de presentar (verbatim)**

### Reglas de negocio (R)

| R | Origen spec | Ubicación |
|---|---|---|
| R-ContextoTramitacion-001 | RN-ContextoTramitacion-001 | `ContextoTramitacion.isPresentadoEnPapel` (equivalencia en `FormaPresentacion.fromProfile`), aplicado en `Tramitador.triggerInitialEvent` (Antes de `JPA.save`) |
| R-NuevoExpediente-001 | RN-NuevoExpediente-001 | `NuevoExpedienteServiceImpl.fireActionRule_IniciarExpediente` (+ `toContextoTramitacion`, que usa `FormaPresentacion.fromPresentadoEnPapel(...).getProfile()`) |

Las decisiones D1/D2 que motivan este enum están en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/decisiones.md` (lectura de contexto; el contrato es el texto de arriba).
