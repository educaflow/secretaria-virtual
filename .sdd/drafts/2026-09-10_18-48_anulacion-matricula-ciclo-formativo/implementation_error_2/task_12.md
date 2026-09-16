---
type: implementation-task
template: expediente
---

# Tarea 12 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding
- k-code-quality
- k-i18n

## Qué hay que hacer

Materializar **los tres ficheros de la fase `RESOLUCION`** de la versión `v1`, en `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/`:

1. `PhaseEventManagerImpl.java` (FQCN `…v1.resolucion.PhaseEventManagerImpl`) — se **escribe sobre el esqueleto** que dejó `CreateFilesTask`, siguiendo la subsección de `## 9` copiada abajo.
2. `StateEventValidatorImpl.kt` (paquete `…v1.resolucion`) — se **escribe sobre el esqueleto** que dejó `CreateFilesTask`, siguiendo la subsección de `## 10` copiada abajo.
3. `views.xml` — **NO se escribe**: se **copia literalmente** de `design/fases/resolucion/views.xml` a `…/v1/resolucion/views.xml`, **sobrescribiendo** el esqueleto. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**La especificación del diseño es contrato fijo y la superficie es cerrada: MUST NOT crearse ningún método, clase, campo, acción, panel ni botón que la especificación no liste**, ni alterarse el orden de las acciones numeradas de un `trigger*`, ni los argumentos literales del DSL del validador.

Reglas duras de esta tarea:

- **MUST** usarse `SecurityUtil.getUser()` (`com.educaflow.base.util.SecurityUtil`), **NUNCA** `AuthUtils.getUser()`.
- El `import` de `States` **MUST** ser el de **esta misma versión** (`com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States`), nunca el de otro tipo ni el de otra versión.
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase: el dispatcher usa `getDeclaredMethods()`. Lo común vive en `ControlDeAcceso` y `DevolucionDelDirector` (tarea 09), a las que se **delega**.
- **MUST NOT** declararse `triggerInitialEvent` ni `triggerExit` en un `PhaseEventManagerImpl`.
- El conjunto de `field(...)` de cada método del validador es la **whitelist de campos que el cliente puede dictar** en esa pareja (estado, evento): **MUST NOT** añadirse ninguno de más ni quitarse ninguno de los listados.

## Filas de la tabla `## 6. Ficheros a crear o modificar` del diseño para esta fase (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `RESOLUCION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `RESOLUCION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/resolucion/views.xml` |

## Pasos del diseño de esos tres ficheros (verbatim)

### Paso 10 — `PhaseEventManagerImpl.java` de cada fase

Ficheros: `…/v1/solicitud/PhaseEventManagerImpl.java`, `…/v1/revision/PhaseEventManagerImpl.java` y `…/v1/resolucion/PhaseEventManagerImpl.java` (FQCN = paquete de la fase + `.PhaseEventManagerImpl`).

Su especificación quirúrgica está en **`## 9. Especificación de los PhaseEventManagerImpl`**, con una subsección por fase.

Supertipo: `extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1>`, con constructor `@Inject` que recibe `AnulacionMatriculaCicloFormativoV1Repository` y llama a `super(AnulacionMatriculaCicloFormativoV1.class)`.

**Verificación:** compilan; por cada fase hay **un** `@WhenEvent trigger<Evento>` por cada evento de su lista de cobertura (§9.4) y **un** `@OnEnterState onEnter<Estado>` por cada estado de esa lista, y ninguno de más; ningún `triggerInitialEvent` ni `triggerExit`; el `import` de `States` es el de esta misma versión.

### Paso 11 — `StateEventValidatorImpl.kt` de cada fase

Ficheros: `…/v1/solicitud/StateEventValidatorImpl.kt`, `…/v1/revision/StateEventValidatorImpl.kt` y `…/v1/resolucion/StateEventValidatorImpl.kt` (paquete de la fase).

Su especificación quirúrgica está en **`## 10. Especificación de los StateEventValidatorImpl`**, con una subsección por fase. Interfaz: `StateEventValidator`.

**Verificación:** compilan; hay exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>()` por cada fila «sí» o «`rules { }` vacío» de la tabla de cobertura de §10, y **ninguno** para `DELETE`; ningún `field(...)` menciona un campo clasificado `servidor` en §4 (salvo `pdfSolicitudFirmada`, la excepción documentada).

### Paso 12 — `views.xml` de cada fase

Los ficheros están materializados en `design/fases/solicitud/views.xml`, `design/fases/revision/views.xml` y `design/fases/resolucion/views.xml`. **Cópialos literalmente** a `…/v1/solicitud/views.xml`, `…/v1/revision/views.xml` y `…/v1/resolucion/views.xml`, **sobrescribiendo** los esqueletos. **MUST NOT** modificarlos, reescribirlos ni regenerarlos.

**Resumen estructural por fase** (`(estado, perfil) → paneles → botones`):

| fase | estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|---|
| SOLICITUD | `DATOS_SOLICITUD` | `CREADOR` | `-subsanacion`, `datos-alumno`, `matricula` | `DELETE` / `CONTINUAR` |
| SOLICITUD | `DATOS_SOLICITUD` | — | `-subsanacion`, `-datos-alumno`, `-matricula` | — / `EXIT` |
| SOLICITUD | `PENDIENTE_FIRMA` | `CREADOR` | `-subsanacion`, `-solicitud-visor` + paneles de situación de firma | `VOLVER` / `PRESENTAR` ×2 |
| SOLICITUD | `PENDIENTE_FIRMA` | — | `-datos-alumno`, `-matricula`, `-solicitud-descarga` | — / `EXIT` |
| REVISION | `PENDIENTE_REVISION` | `SECRETARIO` | `-devolucion-view`, `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-solicitud-firmada-visor`, `revision` | `SUBSANAR` / `ENVIAR_A_FIRMA` |
| REVISION | `PENDIENTE_REVISION` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | `DIRECTOR` | `-datos-alumno-resumen`, `-matricula-director`, `-decision-secretaria`, `-resolucion-a-firmar`, `-solicitud-firmada-descarga`, `-justificante-descarga`, `devolucion` | `DEVOLVER` / `FIRMAR` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `ACEPTADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga-aceptada`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `RECHAZADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |

Cada form lleva además, **fuera** del `<include-panels>`, un `<panel showFrame="false">` con el `<help>` del aviso permanente que la especificación fija para esa pantalla. **Excepción:** la indicación de RUI-ACEPTADA-GENERICA-002 **no** va en ese panel de avisos, sino dentro del panel `presentacion-descarga-aceptada`, junto a la fecha y la hora de presentación a las que acompaña (mismo patrón que `matricula-director` para RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007).

Las acciones propias van en el `views.xml` de su fase: en `solicitud/`, las cuatro de la firma más el `<action-attrs>` `…-DATOS_SOLICITUD-onLoad-action` que marca como obligatorios en pantalla los siete campos del alumno (RUI-DATOS_SOLICITUD-CREADOR-002); en `revision/`, la que limpia el motivo/el texto al cambiar el sentido.

**Verificación:** `diff` vacío en los tres; ningún `<button name="">` sin rellenar; cada estado tiene su form genérico con botón `EXIT`; los estados con `profile` y eventos tienen además su form de perfil; la suma de `colSpan` de los botones de cada footer no pasa de 12.

## `### 9.3 Fase RESOLUCION` de `## 9. Especificación de los PhaseEventManagerImpl`, ÍNTEGRA (verbatim)

### 9.3 Fase RESOLUCION

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> { … }
```

- FQCN: `…v1.resolucion.PhaseEventManagerImpl`.
- Dependencias a inyectar:
  - `AnulacionMatriculaCicloFormativoV1Repository repository` — por constructor.
  - `@Inject AlmacenClaveResolver almacenClaveResolver` — el certificado del Director del centro.
- Constantes de clase:
  - `private static final Rectangulo POSICION_FIRMA_RESOLUCION = new Rectangulo(150, 220, 300, 60);`
- `import …v1.States;`

**`triggerFirmar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede firmar resoluciones de su propio centro"))` — VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001
2. `SERVICIO(almacenClaveResolver.getDirector(expediente.getCentro())) → almacenDirector`; `ERROR_NEGOCIO(la resolución del almacén falla, "El centro no tiene configurada la firma del Director; avise al administrador")`; `ERROR_NEGOCIO(almacenDirector == null, "El centro no tiene configurada la firma del Director; avise al administrador")` — VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-002

   - «La resolución del almacén falla» es el caso en que `getDirector` termina con `RuntimeException` porque **el recurso del certificado del cargo falta o no se puede leer** (`AlmacenClaveFichero` rechaza un `InputStream` nulo con «El fileCertificate no puede ser null»). Ese fallo se transmite al usuario con ese mensaje, conservando la excepción original como causa.
   - **El ámbito del fallo capturado es SOLO esta resolución**: no cubre el `FIRMAR_SERVIDOR` de la acción 5 ni ninguna otra acción del trigger, que fallan con su propio mensaje. **MUST NOT** ampliarse para envolver la firma.
   - **Las dos comprobaciones son complementarias y las dos MUST escribirse.** `almacenClaveResolver.getDirector(centro)` **nunca devuelve `null`** hoy —construye siempre un `AlmacenClaveFichero`—, así que escribir **solo** la comparación con `null` dejaría una condición inalcanzable y la regla de la especificación de hecho sin implementar; y escribir solo la primera dejaría sin cubrir el día en que el resolver pase a depender del centro y devuelva `null` para un centro sin certificado configurado.
   - El almacén se resuelve **una sola vez** y se reutiliza en la acción 5.
3. `ASIGNAR(fechaResolucion = LocalDate.now(Convert.defaultZoneId))` y `ASIGNAR(firmadoPor = SecurityUtil.getUser())` — RN-017 / CC-012; van **antes** de generar el PDF porque la fecha se estampa en él
4. `GENERAR_PDF(RESOLUCION)` — RN-022: se **regenera** (no se recupera la guardada) para que lleve la fecha de la resolución recién anotada
5. `FIRMAR_SERVIDOR(cargo=DIRECTOR, rect=POSICION_FIRMA_RESOLUCION)` — RN-018: sobre el `DocumentoPdf` del paso 4, con el `almacenDirector` del paso 2 (**no** se vuelve a resolver)
6. `CREAR_METAFILE → pdfTemporal` — RN-018 (la resolución firmada queda guardada en el expediente; el campo que la recibe es `pdfResolucionFirmada`, acción 7)
7. `REGISTRO_SALIDA(documento=pdfTemporal, anexos=[]) → pdfResolucionFirmada` — RN-019; el campo recibe `registroSalida.getDocumento()`, la resolución ya registrada
8. `LIMPIAR(pdfResolucion)` — RN-023: en el expediente cerrado queda una única versión de la resolución
9. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-020, sin condición (idempotente)
10. `UPDATE_STATE segun sentidoRevision: ACEPTAR → States.Resolucion.ACEPTADA; RECHAZAR → States.Resolucion.RECHAZADA; SUBSANAR → error; default → error` — el `default` lanza `IllegalArgumentException`: llegar aquí con `SUBSANAR` o con el sentido vacío es un error de programación, porque el validador de `ENVIAR_A_FIRMA` solo deja pasar `ACEPTAR` y `RECHAZAR`

- **`pdfTemporal` (acciones 6 y 7) es una variable local del trigger**, el `MetaFile` intermedio que se le pasa al registro de salida: **MUST NOT** declararse en `domains.xml` ni aparecer en la tabla de campos de §4. El único campo de la entidad que se asigna aquí es `pdfResolucionFirmada`, que recibe `registroSalida.getDocumento()`.

**`triggerDevolver`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede devolver resoluciones de su propio centro"))` — VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003
2. `ASIGNAR(fechaDevolucion = LocalDate.now(Convert.defaultZoneId))` y `ASIGNAR(devueltoPor = SecurityUtil.getUser())` — RN-021 / CC-011
3. `UPDATE_STATE(States.Revision.PENDIENTE_REVISION)`

**`onEnter*`**

- `onEnterPendienteFirmaDirector` — vacío.
- `onEnterAceptada` — vacío.
- `onEnterRechazada` — vacío.

#### 9.3.3 Lista de cobertura de la fase RESOLUCION

- Eventos: `FIRMAR` → `triggerFirmar`; `DEVOLVER` → `triggerDevolver`.
- Estados: `PENDIENTE_FIRMA_DIRECTOR` → `onEnterPendienteFirmaDirector`; `ACEPTADA` → `onEnterAceptada`; `RECHAZADA` → `onEnterRechazada` (los estados cerrados también necesitan su `onEnter`).

## `### 10.3 Fase RESOLUCION` de `## 10. Especificación de los StateEventValidatorImpl`, ÍNTEGRA (verbatim)

### 10.3 Fase RESOLUCION

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `getForStatePendienteFirmaDirectorInEventFirmar` | `rules { }` vacío |
| `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | `getForStatePendienteFirmaDirectorInEventDevolver` | sí |

**`getForStatePendienteFirmaDirectorInEventFirmar`** → `rules { }` (vacío, declarado explícitamente). El director **no envía ningún dato** al firmar: la firma la pone el centro y él solo la autoriza con el botón. Dejarlo vacío es también lo que impide que el cliente dicte ningún campo en este evento.

**`getForStatePendienteFirmaDirectorInEventDevolver`**

```kotlin
field(model::getMotivoDevolucion) {
    +Required("Debe indicar a secretaría por qué devuelve la resolución")
    +MinLength(10, "El motivo de la devolución debe tener entre 10 y 1000 caracteres")
    +MaxLength(1000, "El motivo de la devolución debe tener entre 10 y 1000 caracteres")
}
```

## `### 10.4 Frontera de confianza` (verbatim)

### 10.4 Frontera de confianza

- Ningún `field(...)` menciona un campo clasificado `servidor` en §4 — ni `codePhase`, `codeState`, `abierto`, `centro` o `usuarioRegistrador`, ni ninguno de los cinco `MetaFile` que rellena el servidor, ni las fechas y autores de revisión, devolución y resolución. La **única excepción** es `pdfSolicitudFirmada`, que **también está clasificado `servidor`** y aparece en el validador únicamente porque es el destino de la firma en cliente y ese `field(...)` es el único sitio donde se puede comprobar la firma (§4).
- Cada campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista: `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo` en `CONTINUAR`; `claveCertificado` y `pdfSolicitudFirmada` en `PRESENTAR`; `sentidoRevision`, `motivoRechazo` y `textoSubsanacion` en `ENVIAR_A_FIRMA` y en `SUBSANAR`; `motivoDevolucion` en `DEVOLVER`.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1`, que Axelor publica para toda entidad y que **no pasa por el `Tramitador`**. Son dos puertas distintas: **MUST NOT** darse por protegida esta entidad porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all como parche (se retiró a propósito, ver `CLAUDE.md`).
- `readonly`, `showIf` y `hidden` de las vistas son **UX, nunca defensa**.

## Tabla de transiciones de `## 3` filtrada a los estados de esta fase (verbatim)

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=ACEPTAR` | `RESOLUCION` | `ACEPTADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=RECHAZAR` | `RESOLUCION` | `RECHAZADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | — | `REVISION` | `PENDIENTE_REVISION` |

## Filas y párrafos de `## 11. Reparto de reglas` que aplican (verbatim)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<date>`/`<enum>`/`<many-to-one>` |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required(mensaje)` en la pareja (estado, evento) |
| Formato, rango, longitud, firma | **DSL del validador** | `Pattern`, `MinLength`/`MaxLength`, `FirmaPdf` |
| Obligatoriedad **condicional** | **DSL del validador** | `ifValueIn(...) { +Required(...) }` |
| Valor no admitido en un evento | **DSL del validador** | `ifValueIn`/`ifValueNotIn` + `NoAdmitido(mensaje)`, regla **nueva** del catálogo común que **MUST** ir siempre dentro de una rama: la rama pone la condición, la regla el mensaje (`decisiones.md` D7, §14 nota 21) |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar, calcular | **`trigger*`** del `PhaseEventManagerImpl` | listas de acciones de §9 |
| Inicialización del expediente | **`triggerInitialEvent`** | §8 |
| Quién es el usuario autenticado (autoría, centro) | **guarda en la primera línea del `trigger*`** | `ControlDeAcceso.exige…` → `BusinessException` |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`/`readonly`, `<help>`, `prompt`, `<action-record>` — **solo UX, NUNCA defensa** |
| Campo calculado de **lectura** | **modelo XML del catálogo** | `Ciclo.gradoNivel`, campo derivado `transient` |

**Dueño único de «cuándo aplica el motivo del rechazo» — decisión declarada, no convención tácita.** La decisión es una sola —*el motivo del rechazo aplica si y solo si `sentidoRevision == RECHAZAR`*— y la escriben **cinco** sitios: (1) el `showIf` de los paneles que lo enseñan (`revision`, `decision-secretaria` y `resolucion-firmada`), (2) el `<action-record>` `…-limpiar-revision-action` de la vista, (3) la acción 4 de `triggerEnviarAFirma` (§9.2), (4) la rama `ifValueIn(listOf(RECHAZAR))` del validador (§10.2) y (5) la expresión Groovy de `resolucion.xml`. La alternativa de darle **un dueño al que los cinco pregunten** —un campo derivado `transient` `esRechazo` en la entidad, consultado por la vista, el validador, el trigger y el PDF— se evaluó y se **descartó con motivo**, y la decisión está escrita entera en `decisiones.md` **D10**: dos de los cinco sitios (el `showIf` y el `<action-record>`) se evalúan **en el navegador sobre un registro sin guardar**, donde un campo calculado en el servidor no está recalculado, así que el «dueño único» solo alcanzaría a tres de los cinco y dejaría en la entidad un campo en el que el cliente no puede confiar: dos dueños en vez de uno. **Mientras siga así, el dueño de la condición es el valor `RECHAZAR` del enum**: los cinco **MUST** formularla alrededor de él (`== RECHAZAR` o `!= RECHAZAR`), nunca alrededor de `ACEPTAR`, para que añadir un cuarto sentido no cambie el significado de ninguno sin que nadie lo note; y añadir un cuarto sentido **MUST** revisar los cinco, que están enumerados aquí para que la lista exista en un solo sitio.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Ver `decisiones.md` D4.

| Regla | Capa |
|---|---|
| VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 y 002 | guarda de `triggerFirmar` (identidad y disponibilidad del certificado del centro; ninguna habla de un campo del formulario). La **002** se implementa capturando el fallo de la resolución del almacén, no comparando con `null`, porque `getDirector` nunca devuelve `null` (§9.3 acción 2 y §14 nota 14) |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-001 y 002 | DSL del validador, `getForStatePendienteFirmaDirectorInEventDevolver` |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 | guarda de `triggerDevolver` |
| RN-017 … RN-023 | acciones de `triggerFirmar` y `triggerDevolver` (§9.3) |
| CC-005 … CC-013 | acciones de los `trigger*` (§9) |
| RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-001 … 007 | vista `PENDIENTE_FIRMA_DIRECTOR` del `DIRECTOR` |
| RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001 | vista genérica de `PENDIENTE_FIRMA_DIRECTOR`. Que **todo** el que no sea el director caiga en ella lo garantiza la condición de perfil de la bandeja de firma (Paso 16.0) |
| RUI-ACEPTADA-GENERICA-001 … 003 y RUI-RECHAZADA-GENERICA-001 y 002 | vistas de `ACEPTADA` y `RECHAZADA`. La **002** de `ACEPTADA` es el `<help>` «La anulación de la matrícula surte efecto desde esta fecha» **dentro** del panel `presentacion-descarga-aceptada`, junto a `fechaHoraPresentacion`; ese panel solo lo incluye `ACEPTADA` |

Reglas duras respetadas: ningún `required="true"` en el `domains.xml`; ninguna validación de datos de usuario en un `trigger*`; ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.
