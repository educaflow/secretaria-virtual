---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-secure-coding

Implementa el delta de infraestructura compartida sobre la clase Java existente `src/main/java/com/educaflow/base/infrastructure/mapper/BeanMapperModel.java`.

**Acción `Modificar`: el fichero YA EXISTE.** Edita la clase existente aplicando **solo** el delta que describe el Paso 1 de abajo y **conservando** todo lo demás (métodos, campos e imports preexistentes). El resto de la clase se conserva tal cual.

Va **la primera** de todas las tareas porque los servicios de `Nivel` y de `Ciclo` (tareas posteriores) dependen de este cambio para que sus validaciones vean lo que el usuario va a guardar.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/base/infrastructure/mapper/BeanMapperModel.java` | Modificar | k-code-quality, k-secure-coding | Sustituir la referencia many-to-one cuando el cliente elige **otra** entidad (hoy solo se hace para `MetaFile`). Sin esto, `validateUpdate` valida el grado antiguo (ver Paso 1 y `decisiones.md` D3). |

## Paso 1 del diseño (verbatim)

### Paso 1 — Infraestructura compartida: el mapeador debe sustituir la referencia cuando el cliente elige otra entidad

Categoría 1 de `design-contract.md` §8 (recursos e infraestructura). Va **primero** porque los pasos 3 y 4 dependen de él para que sus validaciones vean lo que el usuario va a guardar.

**Problema que resuelve.** El botón Guardar ejecuta `remote-validationSave-action` → `DefaultModelController.validateSave` → `ActionRequestHelper.getModel(...)` → `BeanMapperModel.copyMapToEntity`. Cuando el registro ya existe y la propiedad es un many-to-one que **ya tenía valor**, el mapeador no sustituye la referencia: copia los campos del mapa sobre la entidad referenciada actual y le restaura su id (`copyValueToEntityAndNoChangeId`), con una excepción escrita a mano para `MetaFile`. Consecuencia: al cambiar el grado de un ciclo ya guardado, el bean validado conserva el **grado antiguo** y `V-Ciclo-001` rechazaría un cambio legítimo (`ESC-004`). En el camino REST no ocurre porque `JPA.edit` sí resuelve la referencia nueva.

```java
// Clase: com.educaflow.base.infrastructure.mapper.BeanMapperModel
// Método (existente, se modifica SOLO la rama marcada):
private void copyMapToEntity(Class<? extends Model> clazz, Map<String, Object> entityMap, Model entityDest,
                             AllowProperties allowProperties, String mappedBy, Model mappedByModel,
                             InstanceModelList instanceModelList);
//   Delta: en el segundo bucle, rama de propiedad de tipo Model, caso «rawValue != null &&
//   valueDest != null». Esa rama cubre one-to-one y many-to-one, y SOLO esos dos: el `if` de
//   BeanMapperModel.java:237 revienta con RuntimeException cualquier otro tipo de relación.
//     - ANTES (BeanMapperModel.java:254-262): la referencia solo se sustituía si el tipo era
//       MetaFile Y el id entrante difería del actual; en cualquier otro caso se llamaba a
//       copyValueToEntityAndNoChangeId (:364-377), que copia los campos del mapa sobre la entidad
//       referenciada ACTUAL y le restaura su id, de modo que la referencia no cambia nunca.
//     - AHORA: se elimina SOLO el filtro `MetaFile.class.isAssignableFrom(...)`. La condición pasa a
//       ser `rawValueId != null && !rawValueId.equals(valueDest.getId())` → getInitialModelFromMap
//       (resuelve la otra entidad por id contra la BD) + PropertyUtils.setProperty sobre entityDest.
//       Mismo id, o mapa entrante sin id → comportamiento actual (copyValueToEntityAndNoChangeId).
//     - MetaFile deja de ser un caso especial: pasa a ser una instancia de la regla general.
//   Efecto: el bean que se valida es el que el usuario va a guardar, igual que hace JPA.edit en el
//   endpoint REST. La whitelist AllowProperties sigue siendo la frontera: una propiedad que no esté
//   en ella ni se sustituye ni se copia.
//   CRITICAL: la sustitución depende de la entrada EXTERNA de la whitelist (la que autoriza la
//   propiedad `grado`), NO del mapa interno de esa entrada. El mapa interno vacío (deny-all,
//   AllowProperties.java:66-83) gobierna solo qué campos se copian DENTRO de la entidad apuntada, y
//   es justo el caso de Ciclo.grado: con él la referencia igualmente se sustituye.
//   Efecto colateral buscado (k-secure-coding §3): al sustituir la referencia en vez de copiar
//   campos dentro de la entidad referenciada gestionada, un cliente ya no puede modificar de rebote
//   los datos de la entidad apuntada. Es una MEJORA de superficie solo donde la whitelist es
//   allow-all; donde el mapa interno ya está vacío, el comportamiento actual tampoco mutaba nada.
//   Alcance: el delta afecta a TODOS los many-to-one del proyecto que lleguen por esta rama, no
//   solo a Ciclo.grado. Las puertas de entrada (conjunto cerrado), el criterio que hace inocuos a
//   los demás caminos y los cuatro con cambio observable están en «Alcance revisado» justo debajo
//   de este bloque, con el barrido reproducible que lo sostiene.
//   El resto de la clase se conserva.
```

**Alcance revisado — quién pasa por la rama que se modifica.**

**Barrido con el que se construye esta sección — reproducible.** Esta sección **no** es una lista recordada ni un repaso a ojo: sale de los cuatro comandos de abajo, que **MUST** relanzarse antes de dar por vigente lo que aquí se afirma (y por eso quedan escritos en el propio diseño).

```bash
# (1) puertas de entrada al mapeador: quién llama a copyMapToEntity y quién a getModel
grep -rn "copyMapToEntity(\|\.getModel(" src/main/java --include=*.java --include=*.kt
# (2) quién declara whitelist propia, y si es allow-all, deny-all o lista escrita a mano
grep -rn "AllowProperties" src/main/java --include=*.java --include=*.kt
# (3) qué entidades se editan por la Vía A
grep -rln "remote-validationSave-action" src/main/java
# (4) qué many-to-one declara cada dominio (para saber si hay algo que sustituir)
grep -rn "many-to-one" src/main/java --include=*.xml
```

Resultado a fecha de este diseño: **3** llamadores de `copyMapToEntity` (`ActionRequestHelper.java:158`, `Tramitador.java:127`, `Tramitador.java:173`), **10** llamadas a `ActionRequestHelper.getModel`, **6** servicios con `allowProperties*` propios (`AdjuntoServiceImpl`, `CorreoServiceImpl`, `CertificadoDigitalServiceImpl`, `TareaFirmaServiceImpl`, `TareaImportacionServiceImpl`, `SmokeTestServiceImpl`) más 2 whitelists creadas *inline* en controladores y 2 construidas por `Tramitador`, y **16** ficheros de vistas con `remote-validationSave-action` (el grep devuelve 18: los otros dos son `base/infrastructure/controller/DefaultModelController.xml`, que **define** la acción, y `subsystem/importacion/CLAUDE.md`, que la cita).

**Qué se enumera aquí y qué NO — decisión explícita.** Hay dos formas de documentar el alcance: (A) una tabla con **todas** las entidades que entran en la rama, o (B) la lista cerrada de **puertas de entrada** más el **criterio** que hace inocuas a las demás. **Se elige B.** Motivo: el conjunto «entidades que entran en la rama» es **abierto por construcción** — toda entidad **sin `ModelService` propio** cae en `DefaultModelService`, cuyos `allowProperties*` son `createAllowAllProperties()`, así que entran **todos** sus many-to-one; añadir mañana una vista con `remote-validationSave-action`, una entidad o un many-to-one mete miembros nuevos en el conjunto **sin que nadie toque este diseño**. Una tabla de ese conjunto nace caducada, y es exactamente la promesa que este documento ya ha incumplido tres veces. En cambio los dos conjuntos de B **sí** son cerrados y pequeños, y viven en infraestructura que casi nunca cambia. Con B el siguiente desarrollador no tiene que recordar una lista: tiene una regla de decisión y cuatro comandos para reconstruirla.

**Precisión 1 — solo al modificar.** La rama modificada exige `valueDest != null`, es decir que la entidad destino **ya tenga valor** en ese many-to-one. En un **alta** `ActionRequestHelper` construye una entidad nueva con todos sus many-to-one a `null`, así que cae en la rama `rawValue != null && valueDest == null`, que **no se toca**. El delta solo se nota, por tanto, **al modificar** un registro existente (o un hijo ya existente de una colección).

**Precisión 2 — la whitelist es la frontera.** En la Vía A, `DefaultModelController.validateSave` resuelve el `ModelService` de la entidad y usa su `allowPropertiesUpdate()`. Una entidad **sin `ModelService` propio** —o con uno que no sobrescribe `allowProperties*`— hereda el `createAllowAllProperties()` de `DefaultModelService`, así que **todos** sus many-to-one entran en la rama. Por eso mirar solo las whitelists escritas a mano deja fuera la mayor parte del tráfico.

**Puertas de entrada (conjunto CERRADO, del barrido (1) y (2)).** Son todas las llamadas del proyecto que acaban en la rama modificada:

| Puerta de entrada | Whitelist que gobierna | ¿Entra algún many-to-one **no**-`MetaFile`? |
|---|---|---|
| `DefaultModelController.java:42` (alta) → `allowPropertiesInsert()` de cada `ModelService` | la de cada entidad | **No**: en alta `valueDest == null` (Precisión 1) |
| `DefaultModelController.java:45` (modificación) → `allowPropertiesUpdate()` de cada `ModelService` | la de cada entidad; **allow-all** si no la sobrescribe | **Sí** — es la puerta de volumen: las 16 entidades de la Vía A y sus hijos. Se resuelve con el criterio de abajo |
| `DefaultModelController.java:63` (borrado) → `allowPropertiesRemove()` | la de cada entidad | Sí, pero el bean solo alimenta `validateRemove`, que decide sobre el registro a borrar, no sobre la referencia entrante |
| `CorreoController.java:46` → `allowPropertiesReenviar()` = `createAllowProperties(Map.of())` | mapa externo **vacío** = nada autorizado | **No**: no se copia ninguna propiedad |
| `TareaFirmaController.java:69` → `allowPropertiesMarcarComoFirmada()` = `{documentosFirma:{documentoFirmado:{}}}` | lista escrita a mano | **No**: `documentoFirmado` es `MetaFile`, que ya se sustituía |
| `TareaFirmaController.java:83` → `allowPropertiesMarcarComoRechazada()` = `{motivoRechazo:{}}` | lista escrita a mano | **No**: escalar |
| `TareaFirmaController.java:97` → `allowPropertiesValidarDocumentosFirmados()` = **`createAllowAllProperties()`** (`TareaFirmaServiceImpl.java:250-252`) | **allow-all** sobre `TareaFirma` | **Sí**: `firmante` (m2o a `User`, `firmas/domains/TareaFirma.xml:7`), que en una tarea ya existente siempre tiene valor y que viaja en el registro que la vista manda (`Pendiente-TareaFirma.xml`: `<field name="firmante"/>` en el grid `:25`, el propio `<domain>` de la vista filtra por él `:15`, y la acción se dispara en `:139` → `:190-191`) |
| `TareaFirmaController.java:114` y `:129` → `allowPropertiesFirmarEnServidor()` = `createDenyAllProperties()` | **deny-all** | **No** |
| `PdfUtilitiesController.java:44` → `createAllowAllProperties()` sobre `PdfUtilities` | allow-all | **No**: los dos many-to-one de `PdfUtilities` son `MetaFile` transitorios (`pdfutilities/domains/PdfUtilities.xml:11-12`) |
| `GestionCentroController.java:153` → `createAllowAllProperties()` sobre `ViewGestionCentroImport` | allow-all | **No hoy**: el método `validarMismoCentro` entero está **comentado** (`/*@CallMethod` en `:143` … `}*/` en `:166`). Si se reactiva, le aplica el criterio de abajo |
| `Tramitador.java:127` (whitelist por pareja estado↔evento del `StateEventValidator`) — **y este camino además PERSISTE** (`Tramitador.java:156`, `expedienteRepository.save`) | lista construida desde `@BeanValidationRulesForStateAndEvent` | **Sí**: `tramites/prueba/v1` → `Expediente.ciclo` (`recepcion/StateEventValidatorImpl.kt:30`). Es el **único** many-to-one no-`MetaFile` declarado en un `StateEventValidator` del proyecto (barrido `grep -rn "field(model::" src/main/java/com/educaflow/tramites/`: los demás son escalares, enums, `MetaFile` o colecciones) |
| `Tramitador.java:173` (`validateChild`, que **detacha** y no persiste) | ídem, sobre el bean hijo | Los de los beans hijos; sin efecto por el criterio de abajo |

**Criterio que hace inocuos a todos los demás caminos.** Sustituir la referencia solo produce un **cambio observable** si se cumple **al menos una** de estas dos condiciones:

1. **El camino persiste el bean mapeado.** De los 3 llamadores de `copyMapToEntity`, solo `Tramitador.java:127` persiste (`:156`). La Vía A **no** persiste: `validateSave` valida y descarta, y el guardado real va por el endpoint REST, donde `JPA.edit` **ya** resolvía la referencia nueva. `Tramitador.java:173` detacha, y `PdfUtilitiesController` tampoco guarda.
2. **Alguien lee ese many-to-one del bean mapeado**, es decir el `validate*` (o el método del controlador) lo **desreferencia**. Si nadie lo mira, la referencia sustituida no cambia ninguna decisión.

Si no se cumple ninguna, el efecto del delta en ese camino es **solo de superficie**, y **a favor**: hoy, con mapa interno allow-all, los campos del JSON se copian **dentro de la entidad referenciada gestionada** (mass-assignment de rebote, `k-secure-coding` §3); con el delta la referencia se sustituye y esa copia deja de ocurrir. Lo fija el caso (g) del test unitario. Donde el mapa interno ya está vacío (deny-all), hoy tampoco se mutaba nada y no hay ni siquiera esa mejora.

Ese criterio descarta de golpe la puerta de volumen: las entidades **sin `ModelService` propio** de la Vía A (`Centro` —con sus cuatro many-to-one reales `comunidadAutonoma`/`provincia`/`municipio`/`conselleria`; `director`/`vicedirector`/`secretario`/`vicesecretario` están **comentados** en `Centro.xml:21-54`—, `Curso`, `CursoModulo`, `Modulo`, `FamiliaProfesional`, `Grado`, `Nivel`, `CentroUsuario*`, `User`…) no tienen `validate*` **ninguno**, así que nadie lee nada. Y descarta también los servicios que sí existen pero cuya modificación no llega a decidir sobre la referencia:

- **`CorreoServiceImpl`** (`:271`) y **`TareaImportacionServiceImpl`** (`:77-82`) declaran `allowPropertiesInsert()` pero **no** `allowPropertiesUpdate()`, así que en modificación heredan el allow-all y **sí** entran sus many-to-one (`Correo.centro`/`historialEstado`; `TareaImportacion.usuario`/`centro`/`fichero`, `importacion/domains/TareaImportacion.xml:10,15,34`) por sus vistas de la Vía A (`Main-Correo.xml`, `Main-TareaImportacion.xml`). Pero el `validateUpdate` de ambos **rechaza siempre** la modificación (`CorreoServiceImpl.java:220-223`, «El correo es inmutable tras su creación»; `TareaImportacionServiceImpl.java:58-63`, «Las importaciones ya registradas no se pueden modificar»), así que el bean mapeado nunca decide nada: solo queda la mejora de superficie.
- **`AdjuntoServiceImpl`** es el caso límite que conviene mirar dos veces, y también cae: su `validarNombreUnicoEnCorreo` **sí desreferencia** un many-to-one (`adjunto.getCorreo().getAdjuntos()`), pero solo se invoca desde `validateInsert` — y en alta no se entra en la rama (Precisión 1) —, mientras que su `validateUpdate` (`:125-129`) rechaza siempre.
- **`LeyEducativaServiceImpl`** (`:14`) y **`DispositivoCriptograficoServiceImpl`** (`:22`) tienen `ModelService` propio pero no sobrescriben `allowProperties*` (allow-all efectivo); da igual, porque **ninguna de las dos entidades declara un solo many-to-one**. Que `LeyEducativaServiceImpl` defina `validate*` es indiferente: solo miran `code` y `name`.
- **`SmokeTestServiceImpl`** tiene whitelist escrita a mano de un único campo escalar (`{texto:{}}`).
- Los demás `ModelService` del proyecto (`RegistroEntradaServiceImpl`, `RegistroSalidaServiceImpl`, `RegistroServiceImpl`, `RegistroPendienteServiceImpl`) **no tienen vista con `remote-validationSave-action`**, así que no se alcanzan por esta puerta.

**Caminos CON cambio de comportamiento observable (lista completa bajo el criterio anterior).** Son cuatro, y los cuatro son **correcciones**, no regresiones:

| Camino | Many-to-one | Quién lo lee / persiste | Cambio |
|---|---|---|---|
| `CicloServiceImpl.validateUpdate` — el caso que motiva este paso (Vía A, `Main-Ciclo.xml`) | `Ciclo.grado`, `Ciclo.nivel` (whitelist escrita a mano, mapa interno vacío) | `V-Ciclo-001`/`002`/`003` | La validación pasa a ver el grado que el usuario acaba de elegir. Sin esto `ESC-004` no puede pasar. **También por la colección de composición `FamiliaProfesional.ciclos`**, por la que el walker desciende y ejecuta `CicloServiceImpl.validate*` sobre cada ciclo ya existente del modal (ver Paso 9 y Nota 9) |
| `CertificadoDigitalServiceImpl.allowPropertiesUpdate()` (`:355-369`) | `dispositivoCriptografico` y `alias` — many-to-one a **entidades del proyecto**, con mapa interno vacío, que el usuario cambia en el formulario (`Main-CertificadoDigital.xml`, con `onChange` que vacía el alias al cambiar de dispositivo) | `validateCertificado` (`:252-255`) comprueba que «el alias pertenece al dispositivo criptográfico» — la pareja gemela de `V-Ciclo-003` | Al editar un certificado `DISPOSITIVO_PKCS11` y elegir **otro** dispositivo u **otro** alias, la comprobación deja de hacerse sobre **referencias rancias** |
| `TareaFirmaController.validarDocumentosFirmados` (`:97`) → `allowPropertiesValidarDocumentosFirmados()` = **allow-all** sobre `TareaFirma` | `firmante` (m2o a `User`) | `TareaFirmaServiceImpl.validarDocumentosFirmados` (`:142-154`) lee `tareaFirma.getFirmante().getDni()` para contrastar el DNI de la firma del PDF | Con el **mismo** id no cambia nada (el caso normal: la vista manda el firmante que ya tiene la tarea). Con un id **distinto**, la referencia se sustituye y el DNI contrastado pasa a ser el del firmante **recibido** en vez de copiarse campos dentro del `User` gestionado; es decir, el delta **cierra** aquí el mass-assignment de rebote sobre `User` (`k-secure-coding` §3) a costa de que el DNI comprobado sea el del request. Que esta acción use `createAllowAllProperties()` sobre una entidad con un m2o a `User` es **deuda preexistente**, anotada aquí y **FUERA DE ALCANCE** de esta iniciativa: **MUST NOT** tocarse esa whitelist desde este diseño |
| `Tramitador.java:127` (`tramites/prueba/v1`, evento `presentar`) — **persiste** (`:156`) | `Expediente.ciclo` | El `StateEventValidator` de `recepcion` (`:30`) y el propio `save` | Elegir **otro** ciclo pasa a **guardar el ciclo elegido**; hoy conserva el anterior |

**No-regresión disponible.** El subsistema `criptografia` tiene **17 tests E2E ya persistidos** en `src/test/e2e/subsystem/criptografia/` (`t-001`…`t-017`) que ejercen el alta, la **edición** y el borrado de certificados digitales por la Vía A; son la red de no-regresión del caso `CertificadoDigital` de la tabla de caminos observables y **MUST** seguir pasando tras el Paso 1. **Cuidado con lo que NO cubren**: todos usan certificados de tipo `CLASSPATH`, así que no ejercen la pareja `dispositivoCriptografico`/`alias` (haría falta un dispositivo PKCS#11 real); ese caso lo fija el test unitario del mapeador (caso (g) de la sección «Tests»). **MUST NOT** escribirse ni modificarse ningún fichero de esa carpeta desde esta iniciativa: solo se ejecutan.

**Verificación del paso:** compila; `grep -n "MetaFile.class.isAssignableFrom" src/main/java/com/educaflow/base/infrastructure/mapper/BeanMapperModel.java` ya no debe aparecer en esa rama. Criterio adicional, porque es exactamente el caso de `Ciclo.grado`: la sustitución **MUST** ocurrir **aunque el mapa interno de la whitelist esté vacío** (deny-all) — la entrada externa de `AllowProperties` es la que autoriza la propiedad; el mapa interno solo gobierna los campos de la entidad apuntada. Lo fija el test unitario (c) de la sección «Tests».


## Cobertura de no-regresión (sección «Tests» del diseño, verbatim)


**Cómo se cubre la no-regresión de `BeanMapperModel`, que es infraestructura compartida.** Con **dos** redes, ninguna de ellas un E2E nuevo:

1. **Tests unitarios del propio mapeador** (tabla de casos mínimos de aquí abajo, obligatoria para `test-unit-desc.md`), incluido el caso (g): un many-to-one a una **entidad de negocio** dentro de una whitelist **allow-all**, que es la forma en la que la mayor parte del proyecto atraviesa la rama modificada (ver «Alcance revisado» del Paso 1).
2. **La suite E2E ya persistida del subsistema `criptografia`** (`src/test/e2e/subsystem/criptografia/`, 17 tests `t-001`…`t-017`), que ejercita el alta, la **edición** y el borrado de certificados digitales por la misma Vía A y es el único sitio del proyecto con una whitelist escrita a mano que incluye many-to-one a entidades de negocio (`dispositivoCriptografico`, `alias`). **MUST** seguir pasando tras el Paso 1; **MUST NOT** escribirse ni modificarse ningún fichero de esa carpeta desde esta iniciativa.

**MUST NOT** añadirse un test E2E nuevo sobre el trámite `prueba/v1` para esto: se ejecutaría en pantallas de Expedientes que el `specification.md` de esta iniciativa no describe (no hay `screen-*.md` ni `entity-*.md` de expedientes), no materializaría ningún `ESC-NNN` y presupondría estado y credenciales fuera de la sección «Estado inicial de la base de datos» — los cuatro incumplimientos de `tests-e2e.md` §1. El cambio de comportamiento de ese trámite queda documentado en `decisiones.md` D3 y cubierto por los casos (a) y (g) del test unitario.


## Notas y supuestos aplicables (verbatim)

6. **Por qué el diseño toca `BeanMapperModel`.** Es la única forma de que la validación previa al guardado vea el grado que el usuario acaba de elegir; el razonamiento completo y el alcance revisado están en `decisiones.md` D3. Es la única pieza del delta fuera del subsistema `sistemaeducativo`. La decisión de que la corrección entre **dentro** de esta iniciativa es firme: sin ella `ESC-004` no puede pasar, porque el `remote-validationSave-action` corre **antes** del `save` y `validateUpdate` recibiría el grado viejo.
8. **Deuda declarada, FUERA DE ALCANCE — `ActionRequestHelper.getModel` no detacha.** `ActionRequestHelper.java:153-158` hace `jpaRepository.find(id)` y **no** detacha la entidad gestionada antes de pasársela a `BeanMapperModel.copyMapToEntity`, así que el pre-check de validación trabaja sobre el bean *managed* de la sesión. Con el delta del Paso 1 la referencia se sustituye en ese bean, que es justo lo que la validación necesita ver, pero conviene saber que no es una copia: cualquier trabajo futuro sobre el pre-check debe partir de ese hecho. Esta iniciativa **MUST NOT** cambiarlo.
