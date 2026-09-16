---
type: implementation-task
template: expediente
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente

Materializa el fichero maestro del tipo de expediente y su proyección en PlantUML.

Los dos ficheros **ya están materializados** en el diseño. **Cópialos literalmente** a su ruta destino, **sobrescribiendo** lo que haya (el `TipoExpedienteInstance.xml` existe hoy con `<fases>` vacío). **MUST NOT** modificarlos, reescribirlos ni regenerarlos.

| Origen | Destino |
|---|---|
| `design/TipoExpedienteInstance.xml` | `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/TipoExpedienteInstance.xml` |
| `design/estados.puml` | `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/estados.puml` |

**MUST NOT** crearse a mano `estados.png` (lo genera la tarea `GenerateDocs`, enganchada al build) ni `States.java` (lo genera `GenerateStatesTask`).

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/TipoExpedienteInstance.xml` | Modificar | `k-tipo-expediente` | Copia de `design/TipoExpedienteInstance.xml` (hoy tiene `<fases>` vacío) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/estados.puml` | Crear | `k-tipo-expediente` | Copia de `design/estados.puml` |

## Pasos del diseño (verbatim)

### Paso 2 — `TipoExpedienteInstance.xml` completo

El fichero está materializado en `design/TipoExpedienteInstance.xml`. **Cópialo literalmente** a `…/v1/TipoExpedienteInstance.xml`, **sobrescribiendo** el actual (que tiene `<fases>` vacío). **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; están las tres fases con sus seis estados y todos los `events` escritos, incluido `events=""` en los dos cerrados.


### Paso 13 — `estados.puml`

El fichero está materializado en `design/estados.puml`. **Cópialo literalmente** a `…/v1/estados.puml`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; aparecen los seis estados con alias `<FASE>_<ESTADO>` y las diez transiciones de §3. El `.png` lo genera `GenerateDocs` (enganchada a `build`): **MUST NOT** crearse a mano.


## `## 3. Máquina de estados` (verbatim, completa)

## 3. Máquina de estados

### Fase SOLICITUD — Solicitud de anulación

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `DATOS_SOLICITUD` | Datos de la solicitud | `CREADOR` | `DELETE,CONTINUAR` | sí | — |
| `PENDIENTE_FIRMA` | Pendiente de firma y presentación | `CREADOR` | `VOLVER,PRESENTAR` | — | — |

### Fase REVISION — Revisión de la secretaría

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `PENDIENTE_REVISION` | Pendiente de revisión | `SECRETARIO` | `ENVIAR_A_FIRMA,SUBSANAR` | — | — |

### Fase RESOLUCION — Resolución del centro

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `PENDIENTE_FIRMA_DIRECTOR` | Pendiente de la firma del director | `DIRECTOR` | `FIRMAR,DEVOLVER` | — | — |
| `ACEPTADA` | Anulación aceptada | `RESPONSABLE` | *(vacío)* | — | sí |
| `RECHAZADA` | Anulación rechazada | `RESPONSABLE` | *(vacío)* | — | sí |

**Por qué los dos estados cerrados declaran `profile="RESPONSABLE"` aunque en ellos no actúe nadie.** Las bandejas genéricas de la plataforma («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, en `tramites/views/`) fijan literalmente `_profile=RESPONSABLE` en su `action-view`, y `ExpedienteController.checkProfileDelTipoExpediente` **lanza una excepción** si el perfil que llega no lo usa ningún estado del tipo: sin esta declaración, abrir un expediente de este tipo desde esas bandejas reventaría. El `profile` de un estado **sin eventos es inerte para la autorización** —`Tramitador.checkPerfilDelEstado` solo se ejecuta al disparar un evento, y estos estados no tienen ninguno—, así que no contradice que en ellos «no actúa nadie»: solo declara `RESPONSABLE` como perfil del tipo. `RESPONSABLE` es, en este trámite, el perfil de **consulta** (§12), el que la especificación describe como «cualquier otro usuario con acceso al expediente». Es además lo que ya hace el trámite de referencia del árbol.

### Tabla de transiciones

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `[*]` | `[*]` | — | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `SOLICITUD` | `DATOS_SOLICITUD` | `CONTINUAR` | — | `SOLICITUD` | `PENDIENTE_FIRMA` |
| `SOLICITUD` | `DATOS_SOLICITUD` | `DELETE` | — | `[*]` | `[*]` |
| `SOLICITUD` | `PENDIENTE_FIRMA` | `VOLVER` | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `SOLICITUD` | `PENDIENTE_FIRMA` | `PRESENTAR` | — | `REVISION` | `PENDIENTE_REVISION` |
| `REVISION` | `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | — | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` |
| `REVISION` | `PENDIENTE_REVISION` | `SUBSANAR` | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=ACEPTAR` | `RESOLUCION` | `ACEPTADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=RECHAZAR` | `RESOLUCION` | `RECHAZADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | — | `REVISION` | `PENDIENTE_REVISION` |

`ENVIAR_A_FIRMA` y `SUBSANAR` tienen **un solo destino cada uno**, así que no llevan guarda: lo que la especificación describe como «condición» de cada uno (que el sentido sea aceptar/rechazar o que sea subsanar) es una **validación** que impide disparar el evento, no una ramificación de la transición, y vive en el DSL del validador (§10). El único evento ramificado es `FIRMAR`, cuyo discriminador es `sentidoRevision`; sus dos ramas cubren los dos únicos valores posibles en ese estado (`SUBSANAR` no puede llegar aquí, porque el validador de `ENVIAR_A_FIRMA` lo rechaza) y el `default` del `switch` es un error de programación.

Los ficheros materializados: `TipoExpedienteInstance.xml` y `estados.puml`.

