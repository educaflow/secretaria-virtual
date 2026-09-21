---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-code-quality
- k-secure-coding

**Alcance de esta tarea:** dos filas que son la misma pieza lógica (D1: el contexto deja de ser un modelo Axelor y pasa a ser un `record`):

1. **Crear** el `record` `ContextoTramitacion` en `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/ContextoTramitacion.java`: Java, se materializa delegando en `developer-code-implementer` (`implementation.md` §2). **Superficie cerrada:** el record, su constructor compacto y `isPresentadoEnPapel()`. Usa `FormaPresentacion` (tarea 02, ya en el árbol).
2. **Borrar** `src/main/java/com/educaflow/subsystem/expedientes/views-models/ContextoTramitacion.xml`, los ficheros `i18n_*.csv` de esa carpeta y la carpeta `views-models/` (queda vacía). Es una **eliminación declarada** (`## Eliminaciones declaradas` del diseño, abajo): la hace el implementador directamente con `rm`/`git rm` (no hay código que escribir; no aplica la comprobación de conservación de `implementation.md` §3, que es para sobrescribir). **MUST NOT** borrar nada más. Los usos que aún queden del modelo anterior (`ExpedienteController.triggerInitialEvent`, `ContextoTramitacionController`, etc.) los retiran las tareas 10-12: **no** los toques aquí y **no** verifiques compilación en esta tarea (el diseño: «el primer `./gradlew compileJava` que debe pasar es el del Paso 6»).

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/views-models/ContextoTramitacion.xml` | Borrar | k-sistemas (modelos.md) | El contexto deja de ser un modelo Axelor (D1). Se borran también los `i18n_*.csv` generados de esa carpeta y la carpeta `views-models/`, que queda vacía |
| `src/main/java/com/educaflow/subsystem/expedientes/tramitacion/eventmanager/ContextoTramitacion.java` | Crear | k-code-quality | `record` de entrada del motor (Paso 2) |

**Del diseño — Paso 1 (extracto verbatim: la retirada del modelo; la creación del dominio `NuevoExpediente` es de la tarea 01)**

### Paso 1 — Dominio `NuevoExpediente` y retirada del modelo `ContextoTramitacion`

**Borrar** `subsystem/expedientes/views-models/ContextoTramitacion.xml` (y la carpeta `views-models/`, que solo conserva `i18n_*.csv` generados).

Verificación: `validate.sh` da `VALIDACION-XML: OK` para `domains/NuevoExpediente.xml`; `grep -rn "views-models/ContextoTramitacion" src` sin resultados.
La compilación **no** se comprueba en los Pasos 1 a 5: hasta el Paso 6 quedan usos del modelo anterior (`ExpedienteController.triggerInitialEvent`, `ContextoTramitacionController`) que se retiran allí; el primer `./gradlew compileJava` que debe pasar es el del Paso 6.

**Del diseño — Paso 2 (extracto verbatim: introducción y `ContextoTramitacion`; el enum `FormaPresentacion` es de la tarea 02)**

### Paso 2 — `ContextoTramitacion` como entrada del motor

**Crear** `com.educaflow.subsystem.expedientes.tramitacion.eventmanager.FormaPresentacion` y `…eventmanager.ContextoTramitacion`.
`FormaPresentacion` es el dueño único de qué perfiles pueden iniciar un expediente y de la equivalencia perfil ↔ forma de presentar: una forma de presentar = una constante del enum (D1, D2).
**Invariante:** el enum tiene **exactamente dos constantes, una por cada valor de `presentadoEnPapel`**, porque la ventana (`NuevoExpediente.presentadoEnPapel`, pregunta de sí/no) y el expediente representan la forma de presentar con un booleano. Añadir una tercera forma **no** es tocar una constante: exige cambiar ese booleano en el modelo y en la ventana, y rediseñar `fromPresentadoEnPapel`, `getFormaPresentarVigente` y `toContextoTramitacion`. Cambiar el perfil de una forma existente sí es tocar solo su constante.
**Test unitario obligatorio** (para `test-unit-desc.md`): comprobar la invariante — para `true` y para `false` hay exactamente una constante en `values()` con ese `isPresentadoEnPapel()`, y `fromPresentadoEnPapel` devuelve esa constante.
`ContextoTramitacion` es un DTO puro de cuatro componentes.

```java
// Clase: com.educaflow.subsystem.expedientes.tramitacion.eventmanager.ContextoTramitacion
public record ContextoTramitacion(Tramite tramite, Centro centro, Profile profile, Boolean presentadoEnRepresentacion) {

    public ContextoTramitacion(Tramite tramite, Centro centro, Profile profile, Boolean presentadoEnRepresentacion);
    //   Constructor canónico, escrito en forma compacta.
    //   Objects.requireNonNull(tramite) con un mensaje que diga que un contexto sin trámite
    //   es un error de programación (la ventana lo fija siempre; un alta programática lo pasa por constructor).
    //   centro, profile y presentadoEnRepresentacion SÍ pueden ser null: es justo lo que comprueban
    //   V-ContextoTramitacion-001/003/004 en la puerta del motor.

    public boolean isPresentadoEnPapel();
    //   Aplica R-ContextoTramitacion-001 (Origen spec: RN-ContextoTramitacion-001):
    //   FormaPresentacion.fromProfile(profile).orElseThrow(() -> new IllegalStateException(<mensaje: solo se
    //   llama con un contexto ya validado por ExpedienteService, cuyo perfil es de inicio>)).isPresentadoEnPapel().
    //   Nunca devuelve un valor por defecto que tape un perfil inesperado.
    //   presentadoEnPapel ya no viaja: se deduce del perfil, así que no puede llegar un dato incoherente con él.
}
```

**Del diseño — Frontera de confianza, DTO de alta programática (verbatim)**

### DTO de alta programática — `ContextoTramitacion`

Record del motor con exactamente cuatro componentes; el propio record **es** la whitelist (`k-secure-coding` §3.5): no existe ningún otro campo que un llamador pueda colar.

| Componente | Origen | Justificación |
|---|---|---|
| `tramite` | cliente | Obligatorio por constructor (error de programación si falta). |
| `centro` | cliente | Validado en la puerta: V-ContextoTramitacion-001/002. |
| `profile` | cliente (alta programática) / servidor (ventana, R-NuevoExpediente-001) | Validado en la puerta: V-ContextoTramitacion-003 (solo creador o tramitador, y uno de los que el usuario tiene ahí). |
| `presentadoEnRepresentacion` | cliente | Validado en la puerta: V-ContextoTramitacion-004/005. |

`presentadoEnPapel` ya no es un componente: se deduce del perfil (R-ContextoTramitacion-001). El usuario que inicia no es un componente: es siempre `SecurityUtil.getUser()` en el servidor.

**Del diseño — Trazabilidad de reglas de negocio (R) (verbatim)**

### Reglas de negocio (R)

| R | Origen spec | Ubicación |
|---|---|---|
| R-ContextoTramitacion-001 | RN-ContextoTramitacion-001 | `ContextoTramitacion.isPresentadoEnPapel` (equivalencia en `FormaPresentacion.fromProfile`), aplicado en `Tramitador.triggerInitialEvent` (Antes de `JPA.save`) |

**Del diseño — Eliminaciones declaradas de esta tarea (verbatim)**

| Elemento eliminado | Fichero | Justificación (spec) |
|---|---|---|
| Fichero completo (entidad `ContextoTramitacion` como modelo Axelor, con `nombreTramite`, `ayudaTramite`, `presentadoEnPapel`) | `subsystem/expedientes/views-models/ContextoTramitacion.xml` | `entity-ContextoTramitacion.md` «Qué cambia»: se retiran nombre, ayuda y presentado en papel; pasa a ser solo la entrada del tramitador (D1) |

**Del diseño — Notas y supuestos aplicables (verbatim)**

13. **`presentadoEnRepresentacion` en `ContextoTramitacion` es `Boolean`** (no `boolean`) porque «no contestado» es un estado que `V-ContextoTramitacion-004` tiene que poder ver.

Las decisiones D1/D2 están en `/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual/.sdd/drafts/2026-09-19_14-49_sistema-expedientes-y-desacople-tramitador/design/decisiones.md` (lectura de contexto; el contrato es el texto de arriba).
