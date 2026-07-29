# Informe: cómo crear un nuevo tipo de expediente

Este informe describe, tras la migración de las carpetas de trámites, cómo funciona la maquinaria de tipos de expediente y los pasos exactos para crear uno nuevo.
El ejemplo de referencia es `justificacion_falta_profesorado`.

## 1. Estructura de carpetas (tras la migración)

Los trámites y sus tipos de expediente viven **fuera de `system/`** (no son sistemas: no siguen la estructura Controller→Service→Repository), en:

```
src/main/java/com/educaflow/tramites/
├── views/nuevo-tramite.xml                → pantalla "Crear un nuevo expediente" (árbol de trámites)
├── shared/                                → recursos compartidos por todos los tipos:
│   └── template-views.xml                 → form plantilla global + historial de estados
├── certificado_tutor/                     → TRÁMITE CertificadoTutor
│   ├── TramiteInstance.xml                → fichero maestro del trámite (ver §1.1)
│   ├── i18n_es.csv / i18n_ca.csv          → i18n del nombre del trámite (generados por el build, ver §2)
│   ├── abstractsimplesolicitudresolucion/ → tipo de expediente
│   └── certificado_tutor/                 → tipo de expediente
├── justificacion_falta_profesorado/       → TRÁMITE JustificacionFaltaProfesorado
│   ├── TramiteInstance.xml
│   └── justificacion_falta_profesorado/
├── prueba/                                → TRÁMITE Prueba
│   ├── TramiteInstance.xml
│   ├── comision_servicio/
│   └── prueba/
├── renuncia_convocatoria_alumno/          → trámites solo con TramiteInstance.xml (sin tipos de expediente aún):
│   └── TramiteInstance.xml                   renuncia_convocatoria_alumno, firmar_actas,
├── firmar_actas/…                            renuncia_convocatoria_tutor, amonestacion_leve,
├── renuncia_convocatoria_tutor/…             autorizacion_recogida_titulo_tercera_persona
├── amonestacion_leve/…
├── autorizacion_recogida_titulo_tercera_persona/…
└── licenciacursos/                        → trámite pendiente de definir (sin TramiteInstance.xml)
    └── licenciacursos/
```

**Cada trámite es independiente** y se define con un único fichero maestro `TramiteInstance.xml` en su raíz (hermano de las carpetas de sus tipos de expediente), del que la tarea gradle `generateDataInitTramites` genera en el build los dos data-init (ver §1.1 y §2).
Cada **tipo de expediente pertenece a un único trámite** y su carpeta cuelga directamente de la carpeta del trámite: `tramites/<tramite>/<tipo>/`.
Un trámite puede tener varios tipos (p. ej. `certificado_tutor` tiene dos) pero solo uno activo (`<defaultTipoExpediente>`).

### 1.1 `TramiteInstance.xml` — el fichero maestro del trámite

Mismo espíritu que `TipoExpedienteInstance.xml` (§3): un XML fuente del que el build genera los data-init.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Tramite>
    <code>JustificacionFaltaProfesorado</code>
    <name>Justificación de falta del profesorado</name>
    <tipoTramite>PROFESOR</tipoTramite>                 <!-- ALUMNO/PROFESOR/TUTOR/DIRECCION/ADMINISTRATIVO/CONSERJE… -->
    <!-- Opcionales: solo se emiten al data-init si se declaran -->
    <publico>false</publico>
    <privado>true</privado>
    <!-- Opcional: el tipo de expediente activo del trámite; sin él no se genera tipo_expediente_activo -->
    <defaultTipoExpediente>JustificacionFaltaProfesorado</defaultTipoExpediente>
    <!-- La ayuda que se muestra en el árbol "Crear un nuevo expediente" (admite HTML) -->
    <help><![CDATA[
        Este trámite permite a justificar la falta del profesorado.<br>
    ]]></help>
</Tramite>
```

De cada `TramiteInstance.xml`, `generateDataInitTramites` genera en `build/resources/main/tramites/<Code>/` (nunca en `src`):
- `definicion/data-init/` (`priority="1"`): crea/actualiza el `Tramite` en BD con `code`, `name`, `publico`/`privado` (solo si están declarados), `tipoTramite` y `help`.
- `tipo_expediente_activo/data-init/` (`priority="-1"`, `update` sin `create`): fija el `defaultTipoExpediente`. Solo se genera si el maestro declara `<defaultTipoExpediente>`.

El orden en tres fases lo gobierna la `priority`: primero el trámite (`1`), luego el `TipoExpediente` (el data-init generado por `generateDataInitTiposExpedientes` no lleva `priority`, default `0`) y por último la asignación del tipo activo (`-1`), que ya encuentra ambos en BD.

Contenido de la carpeta de un tipo de expediente:

| Fichero | Quién lo escribe | Qué es |
|---|---|---|
| `TipoExpedienteInstance.xml` | **tú** | La definición del tipo: código, trámite, estados/eventos/perfiles. Es el fichero maestro; el resto se genera/valida a partir de él |
| `domains.xml` | tú (el esqueleto lo genera el build) | Entidad JPA del expediente (`extends="Expediente"`), en el paquete `com.educaflow.subsystem.expedientes.db` |
| `views.xml` | tú (esqueleto generado) | Vistas en el **formato preprocesado** (ver §4), una por estado(+perfil) |
| `<EventManager>.java` | tú (esqueleto generado) | Máquina de estados: qué pasa en cada evento y a qué estado se transita. Por defecto se llama `EventManagerImpl.java`, pero si declaras `fqcnEventManager` el esqueleto se genera con ese nombre simple (p. ej. `EventManagerFaltaProfesor.java`) — ver §3 |
| `<StateEventValidator>.kt` | tú (esqueleto generado) | Validaciones (Kotlin DSL) por estado+evento. Por defecto `StateEventValidatorImpl.kt`; con `fqcnStateEventValidator` custom, ese nombre |
| `documentospdf/*.pdf` | tú | PDFs con formulario cuyos campos se rellenan con Groovy (ver §7). Conviven los `.odt` de maquetación (el build solo usa los `.pdf`) y una subcarpeta `originales/` con los PDF publicados por el organismo |
| `estados.puml` / `estados.png` | tú / build | Diagrama de estados (el build renderiza el `.puml` a `.png`; se salta el render si el `.png` es más nuevo que el `.puml`, y añade `@startuml`/`@enduml` si faltan) |
| `i18n_ca.csv`, `i18n_es.csv` | build (y tú, solo la columna `message`) | **NO crearlos a mano** — los genera el procesador i18n (ver §2.1, i18n: sí está previsto corregir a mano una traducción mala editando la columna `message`) |

## 2. El papel de `../EducaFlowBuildTools`

El build de gradle invoca varias tareas del jar `com.educaflow:EducaFlowBuildTools:1.0-SNAPSHOT` (si tocas ese proyecto: `mvn install` y recompila la app).
Todas parten de **buscar cada `TipoExpedienteInstance.xml` recorriendo `src/main/java`** (la búsqueda es por nombre de fichero; la única ruta "fija" es la de los PDFs compartidos, ver §7).

| Tarea gradle | Clase Main | Qué hace |
|---|---|---|
| `CreateFilesTask` (antes de `generateCode`) | `createfiles.Main` | **Genera si no existen** `domains.xml`, `views.xml`, el `<EventManager>.java` y el `<StateEventValidator>.kt` a partir de plantillas (los esqueletos ya incluyen todos los métodos requeridos; qué trae exactamente cada esqueleto: ver §2.2). Después **valida con Spoon el EventManager** (ver §2.1); si falla, el build se detiene con "Alguna de las clases no se pudo precompilar" (los errores de todos los tipos se acumulan y se lanzan juntos al final). **OJO: el `StateEventValidator` NO se comprueba en build** (su `check()` está vacío): un método de validación que falte solo se detecta en **runtime**, al disparar el evento |
| `RichDomainXmlTask` (antes de `generateCode`) | `richdomainclass.MainModelXml` | **Reescribe dentro del propio `domains.xml` fuente** el bloque `<extra-code-model>`: el enum `TipoDocumentoPdf` (una constante por cada `*.pdf` de `documentospdf/` propio + `shared/`) y el método `getDocumentoPdf(...)`. No editar ese bloque a mano: se regenera en cada build. Es **esta** vía por la que el enum acaba en la clase Java generada: el `generateCode` de Axelor vuelca el `<extra-code-model>` del `domains.xml` a la clase |
| `RichDomainClassTask` (tras `generateCode`) | `richdomainclass.Main` | **Hoy es un no-op efectivo**: evalúa la plantilla `extra-code-domain.template`, que está **vacía**, e inserta solo líneas en blanco antes de la última `}` de la clase generada en `build/src-gen`. Existe como mecanismo alternativo de inyección directa en la clase, pero con el fuente actual no aporta nada (el enum llega vía `RichDomainXmlTask`, ver fila anterior) |
| `viewProcessorTask` (tras `processResources`) | `viewprocessor.Main` | **Preprocesa TODAS las vistas** (XML con raíz `object-views`) expandiendo los tags custom (§4) y las copia a `build/resources/main/views`. Ojo: TODOS los XML de vistas se reescriben en la copia (re-indentado a 2 espacios y eliminación de text nodes vacíos), también los de sistemas normales sin tags custom |
| `generateDataInitTiposExpedientes` (tras `build`) | `createdatainittipoexpediente.Main` | Genera en `build/resources/main/tiposExpedientes/<Code>/data-init/` (ficheros `input-config.xml` + `input/<Code>-data.xml`) el data-init que inserta/actualiza la fila `TipoExpediente` en BD. Qué persiste exactamente: ver §3.1 |
| `managei18nfiles` (finalizer de `processResources`) | `i18nprocessor.Main` | Por cada carpeta, recolecta los textos traducibles (atributos `title`/`help` de las vistas, títulos de dominios, `name` y títulos de estado del `TipoExpedienteInstance.xml`, y el `<name>` de los `TramiteInstance.xml` — extractor `TitleExtractorImplTramiteInstance`, que los identifica por el nombre exacto del fichero), mantiene los `i18n_es.csv`/`i18n_ca.csv` junto al fuente (los del nombre del trámite quedan en la raíz de la carpeta del trámite) y **traduce automáticamente** castellano→valenciano con apertium (sufijo `__!!` = no traducir), copiando el resultado al build como `custom_es.csv`/`custom_ca.csv`. Detalles y matices en §2.1 (i18n): las claves obsoletas NO se borran y las correcciones manuales de la columna `message` SÍ se conservan |
| `generateDataInitTramites` (tras `build`) | (gradle) | Tarea gradle **nativa** (no usa el jar): busca cada `TramiteInstance.xml` bajo `src/main/java` y genera en `build/resources/main/tramites/<Code>/` los dos data-init del trámite — `definicion/data-init/` (`priority="1"`) y, solo si el maestro declara `<defaultTipoExpediente>`, `tipo_expediente_activo/data-init/` (`priority="-1"`). Es idempotente: borra su carpeta de salida antes de generar. Contenido del maestro: ver §1.1 |
| `copiarPdf`, `copyDataInit`, `GenerateDocs` | (gradle) | Copian los PDF y data-init a resources preservando la ruta, y renderizan los `.puml` |

Caveats del build que conviene conocer:

- El data-init de tipos de expediente se genera **siempre** y su escritura falla si el fichero destino ya existe ("El fichero no puede existir").
  Un `./gradlew build` sin `clean` con restos de la ejecución anterior en `build/resources/main` revienta con ese error poco descriptivo: es otra razón por la que `./run.sh` hace siempre `clean build`.
- `RichDomainXmlTask` y `RichDomainClassTask` declaran `inputs`/`outputs` en gradle y pueden quedar `UP-TO-DATE` (no re-ejecutarse) en builds incrementales; el resto de tareas del BuildTools se ejecutan siempre.
- Si el número de argumentos de `viewprocessor`/`i18nprocessor` no es el esperado, imprimen "Uso: ..." y **terminan con exit 0**: un error de configuración en `build.gradle` haría pasar el build sin preprocesar vistas ni i18n.
- La tarea i18n **requiere apertium instalado en el PATH** con el par `spa-cat_valencia` (el ejecutable es un argumento de la tarea en `build.gradle`).

### 2.1 TODAS las comprobaciones que hace EducaFlowBuildTools

Estas comprobaciones son la "checklist" real al crear un tipo de expediente: si alguna no se cumple, **el build falla** (salvo las marcadas como runtime o aviso).

**Sobre `TipoExpedienteInstance.xml`** (al parsearlo, en todas las tareas):
1. Debe ser parseable por JAXB; si no: "Fallo al obtener el tipo de expediente:<fichero>".
   OJO: JAXB **ignora silenciosamente los elementos desconocidos** (no hay validación por schema): un typo en un tag (p. ej. `<fqcnEventmanager>`) no da error, simplemente se aplica el valor por defecto.
2. **Exactamente un** estado con `initial="true"`: ninguno → "No existe ningun estado inicial"; dos o más → "Existe más de un estado inicial:...".
3. El atributo `events` es **obligatorio** en cada `<state>` aunque esté vacío (`events=""`).
   Si se omite el atributo, el parseo revienta con un NPE que aflora como el genérico "Fallo al obtener el tipo de expediente:<fichero>", sin pista de la causa.
4. El atributo `profile` en cambio es **opcional**: un estado sin profile (o con profile en blanco) se genera con perfil `null` en el enum `State` y no aporta valor al enum `Profile`.
5. Los nombres de estados, eventos y perfiles se usan **tal cual como identificadores Java** de los enums (sin validación previa): un nombre inválido rompe la compilación después, sin mensaje del BuildTools.
   La conversión a UpperCamel para los nombres de método es `UPPER_UNDERSCORE→UpperCamel` (Guava): está pensada para nombres `EN_MAYUSCULAS`; un nombre en otro formato produce métodos inesperados (p. ej. `GuardarDatos` → `triggerGuardardatos`).

**Sobre el `EventManager`** (`CreateFilesTask` → `EventManagerFile.check()`, con Spoon):
6. Por **cada evento** declarado en algún `<state events="...">` debe existir **exactamente un** método con la firma exacta `@WhenEvent void trigger<EventoEnUpperCamel>(<Entidad>, <Entidad>, EventContext)`.
   Si falta → "Faltan métodos para los eventos" (el error incluye el código a copiar).
   Un overload duplicado también cuenta como fallo (exige *exactamente uno*).
   Un método con el nombre correcto pero la **firma equivocada** (tipos o número de parámetros) también sale como "Faltan métodos" (no como "Sobran"), lo cual despista porque el método "existe".
7. **No puede sobrar** ningún método anotado `@WhenEvent` cuyo nombre no corresponda a un evento declarado → "Sobran métodos para los eventos" (si quitas un evento del XML, tienes que quitar también su método, y viceversa).
8. Por **cada estado** debe existir exactamente un método `@OnEnterState void onEnter<EstadoEnUpperCamel>(<Entidad>, EventContext)` → "Faltan métodos para los estados".
9. **No puede sobrar** ningún método `@OnEnterState` que no corresponda a un estado → "Sobran métodos para los estados".
10. Límites del check que hay que conocer:
    el FQCN de la entidad está **hardcodeado** como `com.educaflow.subsystem.expedientes.db.<Code>` (si cambias el `package` del `<module>` en `domains.xml`, el check falla aunque los métodos existan);
    Spoon solo parsea **el fichero** del EventManager (métodos heredados de una superclase cuentan como "Faltan");
    y `triggerInitialEvent` **no** se comprueba (solo lo fuerza el compilador por ser abstract en la clase base).

**Sobre los FQCN custom** (`fqcnEventManager`/`fqcnStateEventValidator`):
11. Si se omiten **o están en blanco**, el default es `<paquete>.EventManagerImpl` / `.StateEventValidatorImpl`, donde `<paquete>` se deriva de la ruta de la carpeta del tipo (lo que hay tras `/java/`).
12. Con un FQCN custom, `CreateFilesTask` usa **solo el nombre simple** de la clase y genera/valida siempre el fichero `<SimpleName>.java`/`.kt` **en la carpeta del tipo**, con el `package` calculado de la ruta.
    Consecuencia: el paquete del FQCN custom **debe ser el de la propia carpeta del tipo**; si apunta a otro paquete, el build genera silenciosamente un esqueleto nuevo en la carpeta y el FQCN del data-init apuntará a una clase distinta de la validada.

**Sobre el `StateEventValidator`**: su `check()` de build está **vacío** (todo el cuerpo de comprobación, incluido `checkStates()`, está comentado en `StateEventValidatorFile`).
El esqueleto generado ya trae un método por cada estado+evento, pero si borras/renombras uno, o añades un evento nuevo sin su método, **el build NO lo detecta**: falla en **runtime** al disparar el evento ("No se ha encontrado el método: getForState<Estado>InEvent<Evento>...").
Hay que mantenerlos a mano.

**Sobre `domains.xml`** (`RichDomainXmlTask`/`RichDomainClassTask`):
13. Debe existir junto al `TipoExpedienteInstance.xml`, con el tag `<module>` (único: duplicado → "Hay mas de un elemento con tag:module"; ausente → NPE) y con una `<entity name="<Code>">` cuyo nombre sea **exactamente el `<code>`** del tipo; si no, la tarea revienta ("No se encontró el tag '<entity>' con name=...").
14. La entity debe tener `extends="Expediente"` para que se le inyecte el `<extra-code-model>`.
    En realidad **toda** la inyección del extra-code (en el XML y en la clase generada) está en un try/catch que imprime el error y **continúa** (aviso, no detiene el build): eso incluye no encontrar `extends="Expediente"`, no encontrar `</entity>`, o que la clase generada no exista — pero te quedas sin enum `TipoDocumentoPdf`.

**Sobre las vistas** (`viewProcessorTask`):
15. Todo XML bajo `src/main/java` con raíz `object-views` debe ser **parseable**; si no, el build falla indicando el fichero.
16. Debe existir al menos un `template-views.xml` y debe contener el form `subsysExpedientes-templates-form`; si no → error.
    OJO: cuenta como plantilla global **cualquier** fichero llamado `template-views.xml` (comparación case-insensitive) en cualquier punto de `src/main/java`, no solo el de `tramites/shared/`.
17. En cada `views.xml` de tipo de expediente debe haber **exactamente un** form plantilla que case con `exp-<Code>-Templates`; dos o más → "Existen al menos 2 nodos de plantilla".
    Con **cero** plantillas no sale ese error: sale un NPE críptico envuelto en "Fallo al prerocesar el fichero:<fichero>".
    El patrón `exp-([a-zA-Z0-9]+)-Templates` se evalúa como **substring** (`find()`, no `matches()`): cualquier form cuyo `name` *contenga* ese patrón cuenta; y el `<code>` no puede llevar guiones ni underscores.
18. Cada nombre listado en `<include-panels>` debe existir como panel (por atributo `name`, en tags `panel*`) en el form plantilla local o en un `template-views.xml`; si no existe → "No existe el panel con nombre:<nombre>"; si está repetido dentro de una plantilla o entre varios `template-views.xml` → "Hay mas de un elemento..." / "Existe más de un panel llamado...".
    La búsqueda es **primero en el form plantilla local** y solo si no está ahí en los `template-views.xml`: un panel local con el mismo nombre que uno global (incluidos `subsysExpedientes-template-header-panel` y `...-footer-panel`) lo **sobreescribe silenciosamente**.

**Sobre i18n** (`managei18nfiles`):
19. Todo texto traducible nuevo se traduce automáticamente al valenciano; si el traductor **no consigue una traducción fiable**, el build falla con "Fallo al traducir estos campos (debe modificar o añadir el atributo title correspondiente)" (mensaje agregado: "No fue posible generar lo ficheros de i18n:").
    Heurística de "fiable": apertium marca con `*` las palabras que no conoce; una palabra marcada se acepta igualmente si va seguida de punto, si termina en `__!!` o si está toda en mayúsculas (siglas); después se eliminan los `*` y los sufijos `__!!` del resultado.
20. Qué genera claves además de los `title` explícitos:
    el `name` del tipo y los títulos de estado se registran con prefijo `value:` (que se quita antes de traducir);
    un estado **sin** `title` genera clave con su name humanizado (`ENTRADA_DATOS` → "Entrada datos");
    los campos de dominio y los `<item>` de los `<enum>` **sin** `title` también generan clave (humanizados con el mismo algoritmo que usa Axelor);
    y las claves `Code`/`Name` se traducen hardcoded a "Código"/"Codi" y "Nombre"/"Nom" sin pasar por apertium.
21. Ciclo de vida de los CSV, importante:
    la tarea **añade** claves nuevas y **rellena** los mensajes vacíos, pero **nunca borra una clave obsoleta** (persisten hasta que se editen a mano) y **nunca retraduce un mensaje ya relleno** — por eso corregir a mano la columna `message` de `i18n_ca.csv` es el mecanismo previsto para arreglar una traducción mala de apertium, y esa corrección se conserva.
    Lo que **no** hay que hacer es crear los ficheros a mano ni añadir/quitar filas: los CSV se reescriben siempre con las 4 columnas `key,message,comment,context` (comentarios manuales se pierden), se procesan **por directorio y sin recursión** (cada carpeta con XML traducibles tiene su propio par es/ca), y si las listas es/ca descuadran (p. ej. una fila añadida a mano en solo uno) revienta con "El tamaño de las listas no coincide".
    En la copia al build solo se aceptan los nombres exactos `i18n_es.csv`/`i18n_ca.csv` (renombrados a `custom_es.csv`/`custom_ca.csv`); otro nombre → "El nombre del fichero no es válido".

**Comprobaciones en runtime relacionadas** (no son del BuildTools pero completan la lista de lo que debe quedar bien hecho):
- El `<tramite>` del `TipoExpedienteInstance.xml` debe existir como trámite en BD, es decir debe ser el `<code>` del `TramiteInstance.xml` del trámite (el data-init lo busca por `code`; si no existe, el tipo queda sin trámite y no aparece).
- Los `profile` de los estados deben existir como perfiles en BD (mismo motivo).
- `triggerInitialEvent` debe dejar `dniFirmaDocumentoEntrada` relleno con un DNI válido (lo exige `Tramitador` con `Preconditions` + `DniUtil`), y también `personaSolicitante` si el flujo crea registros de entrada (ver §5).
- Para cada (estado, perfil) alcanzable debe existir la vista `exp-<Code>-<STATE>-<PROFILE>-form` o la genérica `exp-<Code>-<STATE>-form`; si no, excepción "No existe la vista en el expediente:...".
- Cada método del validator debe existir para su (estado, evento) — ver arriba.

### 2.2 Qué traen exactamente los esqueletos generados

`domains.xml`:
- Schema `domain-models_8.0.xsd`, `<module name="expedientes" package="com.educaflow.subsystem.expedientes.db"/>` (nombre de módulo fijo) y `<entity name="<Code>" extends="Expediente">` con cuerpo vacío.
- El esqueleto NO trae `<extra-code-model>`: ese bloque aparece en el fichero fuente tras el primer build (lo inserta `RichDomainXmlTask` antes de `</entity>`, o sustituye el existente).
  Si el tipo no tiene ningún PDF, el bloque se escribe igualmente pero **vacío** (sin enum ni `getDocumentoPdf`).

`<EventManager>.java`:
- Campo `private final <Code>Repository repository` inyectado por constructor `@Inject`, logger slf4j, y constructor que llama `super(<Code>.class, State.class, Event.class, Profile.class)`.
- El enum `State` generado es "**rico**": cada constante lleva su Profile dueño (o `null`), `initial`, `closed` y la lista de eventos permitidos, con getters.
  **Toda la máquina de estados del XML queda codificada en este enum, y es la única fuente de esos flags en runtime** (el data-init no los persiste, ver §3.1).
- Los enums `Event` y `Profile` se generan deduplicados en orden de primera aparición recorriendo los estados.
- `triggerInitialEvent` y todos los `trigger<Evento>` declaran `throws BusinessException`; los `onEnter<Estado>` no.
  Cada trigger trae el comentario-guía `//eventContext.updateState(State.);`.
- Las mismas plantillas de método se usan para el "código a copiar" de los errores "Faltan métodos..." del check.

`<StateEventValidator>.kt`:
- Importa la entidad con alias: `import com.educaflow.subsystem.expedientes.db.<Code> as model` — **de ahí sale el `model` de los ejemplos del §6**; sin ese alias los ejemplos no compilan.
- Trae ya los imports del DSL (`rules`, `ifValueIn`, `BeanValidationRules`, `...validation.rules.*`, `java.time.LocalDate`).
- Genera un método `public fun getForState<X>InEvent<Y>(): BeanValidationRules { return rules { } }` por **cada** evento de cada estado, **incluido `DELETE`** si aparece en `events="..."` (el runtime nunca invoca el de DELETE; el método sobra pero es inofensivo).

`views.xml`:
- El form plantilla se genera vacío con `name="exp-<Code>-Templates"`, `title="<name del tipo>"`, `width="large"`, `model="com.educaflow.subsystem.expedientes.db.<Code>"` y `groups="admins,users"`.
- Por cada estado genera ya **dos** forms: uno `<form state profile>` (solo si el estado tiene profile) y siempre otro genérico `<form state>` sin profile, cada uno con `<include-panels>` vacío y un `<footer>` con un botón placeholder (`name=""`, `title=""`, el `onClick="subsysExpedientes-event-action"` ya puesto).
- OJO: el esqueleto escribe `<include-panels includeHeader="true">`, pero el preprocesador lee el atributo **`header`**, no `includeHeader` — el atributo generado se ignora (inofensivo porque el default ya es `true`), y quien copie el patrón y escriba `includeHeader="false"` **no** quitará la cabecera: hay que escribir `header="false"`.

## 3. `TipoExpedienteInstance.xml` — el fichero maestro

```xml
<TipoExpediente>
    <name>Justificación de falta del profesorado</name>
    <code>JustificacionFaltaProfesorado</code>          <!-- = nombre de la entidad -->
    <tramite>JustificacionFaltaProfesorado</tramite>    <!-- code del trámite (TramiteInstance.xml, §1.1) -->
    <!-- Opcionales: si se omiten (o están en blanco), se usan <paquete>.EventManagerImpl / .StateEventValidatorImpl.
         El paquete de un FQCN custom DEBE ser el de la propia carpeta del tipo (ver §2.1 punto 12) -->
    <fqcnEventManager>com.educaflow.tramites.justificacion_falta_profesorado.justificacion_falta_profesorado.EventManagerFaltaProfesor</fqcnEventManager>
    <fqcnStateEventValidator>...StateEventValidatorImplFaltaProfesor</fqcnStateEventValidator>
    <!-- Opcionales: ambitoCreador / ambitoResponsable / ambitoAuditor (INDIVIDUAL|CENTRO|DEPARTAMENTO) -->
    <states>
        <state name="ENTRADA_DATOS"          events="DELETE,GUARDAR_DATOS" profile="CREADOR"     title="Entrada de datos" initial="true"/>
        <state name="PENDIENTE_PRESENTACION" events="BACK,PRESENTAR"       profile="CREADOR"/>
        <state name="PENDIENTE_RESOLUCION"   events="RESOLVER"             profile="RESPONSABLE"/>
        <state name="ACEPTADO"               events=""                     profile="RESPONSABLE" closed="true"/>
        <state name="RECHAZADO"              events=""                     profile="RESPONSABLE" closed="true"/>
    </states>
</TipoExpediente>
```

- Cada `<state>` declara los **eventos** que puede disparar el usuario en ese estado, el **perfil dueño** del estado (quién "tiene el turno") y los flags `initial`/`closed`.
  El atributo `events` es obligatorio aunque sea vacío; `profile` es opcional (ver §2.1 puntos 3-4).
  Los valores de `events` se separan por comas (tolera espacios alrededor).
- Los eventos y perfiles de todos los estados forman los enums `Event` y `Profile`; los nombres deben ser identificadores Java válidos y conviene que sean `UPPER_SNAKE` (ver §2.1 punto 5).
- Además de los eventos propios existen los **comunes** (`CommonEvent`): solo `EXIT` (cerrar la pestaña, no llega al EventManager) y `DELETE`.
  **`BACK` NO es un evento común**: si un estado necesita "volver atrás" hay que declarar `BACK` como evento normal en su `events="..."`, implementar su `@WhenEvent triggerBack` y darle método en el validator (así lo hace `justificacion_falta_profesorado`; ver el patrón multi-origen en §5).
- Estos FQCN acaban en la tabla `expedientes_tipo_expediente` (vía el data-init generado) y se instancian **por reflexión + Guice** en `TipoExpedienteUtil.getEventManager()/getStateEventValidator()`.

### 3.1 Qué persiste exactamente el data-init generado

El data-init generado (`input-config.xml` + `input/<Code>-data.xml`) hace un bind del `TipoExpediente` con `search="self.code = :code" create="true" update="true"` (por eso se refresca al arrancar) y persiste: `code`, `name`, los tres `ambito*` (siempre, aunque queden vacíos), el `tramite` (resuelto por `search="self.code = :tramite"`) y los dos FQCN.
Los FQCN se emiten **siempre ya resueltos**: aunque el XML los omita, en BD queda el default explícito `<paquete>.EventManagerImpl` / `.StateEventValidatorImpl`.

De cada `<state>` solo se persisten `codeState` y `profile` (resuelto por `search="self.code = :profile"`; el atributo solo se emite si no está en blanco).
`events`, `initial`, `closed` y `title` **NO van a BD**: la máquina de estados vive únicamente en el enum `State` del EventManager (§2.2).
OJO: el bind de los estados no lleva atributo `search`, así que un reimport puede crear filas de estado duplicadas.

## 4. El formato preprocesado de `views.xml` (tags custom) ⚠️

Las vistas de los tipos de expediente **NO siguen las normas de las vistas de sistemas y subsistemas** (`k-vistas`, `view-rules.md` las excluye).
Se escriben en un formato propio que el preprocesador de `EducaFlowBuildTools` (`viewprocessor`) convierte en vistas Axelor estándar durante el build.
En el fichero conviven:

1. **Un form plantilla** (Axelor normal) llamado `exp-<Code>-Templates`, que actúa de **almacén de paneles** con nombre.
   Nunca se muestra tal cual: sus atributos (`model`, `width`, `groups`…) se heredan y sus paneles se copian a las vistas finales.
2. **Un form por estado (y opcionalmente perfil)** con los tags custom:

```xml
<form state="ENTRADA_DATOS" profile="CREADOR">
    <include-panels>
        -datos-profesor          <!-- con guion: se incluye con TODOS sus fields readonly -->
        datos-falta              <!-- sin guion: editable -->
        justificante-upload
    </include-panels>
    <footer>
        <buttons-left>
            <button name="DELETE" colSpan="2" title="Borrar" onClick="subsysExpedientes-event-action" prompt="¿Seguro?"/>
        </buttons-left>
        <buttons-right>
            <button name="GUARDAR_DATOS" colSpan="2" title="Siguiente" onClick="subsysExpedientes-event-action"/>
        </buttons-right>
    </footer>
</form>
```

Qué hace el preprocesador con cada tag:

- **`<form state="X" profile="Y">`** → se convierte en un `<form>` estándar con `name="exp-<Code>-<STATE>-<PROFILE>-form"` (o `exp-<Code>-<STATE>-form` sin perfil), heredando los atributos del form plantilla.
  El título por defecto es el `code` humanizado con reglas concretas: separa camelCase, capitaliza solo la primera palabra y pasa el resto a minúsculas conservando siglas (`JustificacionFaltaProfesorado` → "Justificacion falta profesorado").
  **Des-herencia**: si el form de estado declara un atributo con valor **en blanco** que también existe en la plantilla, el atributo se **elimina** del resultado (p. ej. `width=""` quita el `width="large"` heredado).
- **`<include-panels>`** → se sustituye por copias de los paneles referenciados por nombre.
  Se buscan **primero en el form plantilla del propio fichero** y si no, en los `template-views.xml` (precedencia local, ver §2.1 punto 18).
  Prefijo `-` = copia con todos sus `<field>` a `readonly="true"` (solo los `<field>` descendientes, nada más).
  Un panel repetido en la lista se incluye una sola vez (gana el flag readonly de la última aparición), sin aviso.
  Con `header="false"` no se antepone el panel de cabecera `subsysExpedientes-template-header-panel` (por defecto sí se incluye; el atributo solo admite true/false, otro valor → "Valor fuera de rango"); listar explícitamente el panel de cabecera en la lista permite reconfigurarlo (p. ej. incluirlo readonly).
- **`<footer>` con `<buttons-left>`/`<buttons-right>`** → se sustituye por el panel `subsysExpedientes-template-footer-panel` (resuelto con la misma precedencia local→global; el de `tramites/shared/template-views.xml` además pinta los mensajes de error de validación) con los botones dentro.
  El colSpan por defecto de cada botón es el `itemSpan` del panel footer (default 1).
  Al primer botón de la derecha se le **asigna siempre** (sobrescribiendo cualquier valor manual) `colOffset = 12 − suma de todos los colSpan` (izquierda + derecha), para alinearlo al margen derecho; si la suma pasa de 12 el offset sale negativo sin comprobación.
  Dentro de `<buttons-left>`/`<buttons-right>` se importa cualquier elemento hijo, no solo `<button>`.
- El **`name` de cada botón es el nombre del evento** que dispara; todos usan `onClick="subsysExpedientes-event-action"` (se pueden encadenar acciones: `onClick="serial:otra-accion,subsysExpedientes-event-action"`, p. ej. para AutoFirma, ver §7.1).
  Los botones admiten los atributos Axelor normales: además de `colSpan`/`title`/`prompt`, el expediente real usa `css="btn-danger"`, `outline="true"` e `icon="trash"`.
- Los `<include-panels>` y `<footer>` se expanden **en cualquier punto del documento** (no solo dentro de forms con `state`), y un form de estado puede contener además **paneles Axelor normales** — el expediente real añade tras el footer un `<panel showFrame="false">` con un `<help>` de ayuda.

**Cómo elige la vista el runtime** (`EventManager.getViewName`): tras cada evento se busca primero `exp-<Code>-<STATE>-<PROFILE>-form` (la del perfil que está actuando) y si no existe, `exp-<Code>-<STATE>-form` (la genérica, normalmente de solo lectura).
Por eso en `views.xml` se escribe, por estado, una vista para el perfil dueño y otra genérica (el esqueleto ya trae ambas, §2.2).

### 4.1 Patrón: visor de PDF embebido

Para mostrar un PDF guardado en un campo `many-to-one` a `MetaFile`, el expediente de referencia usa un panel con un field *dummy* cuyo `<viewer>` pinta un iframe apuntando al download inline del MetaFile:

```xml
<panel name="pdfSolicitud" title="Solicitud">
    <field name="new" showTitle="false" readonly="true" colSpan="9">
        <viewer depends="pdfSolicitud"><![CDATA[
            <>
            <Box as="iframe" height="900" border="0" src={`ws/rest/com.axelor.meta.db.MetaFile/${pdfSolicitud.id}/content/download?inline=true&name=${pdfSolicitud.fileName}`} ></Box>
            </>
        ]]></viewer>
    </field>
</panel>
```

Es la técnica estándar para todos los PDF del expediente (solicitud, solicitud firmada, justificante de registro, resolución); cada visor va en su propio panel con nombre para poder incluirlo por estado.

## 5. El `EventManager` — quién decide el cambio de estado

Clase Java que extiende `EventManager<Entidad, State, Event, Profile>` (los tres enums se declaran dentro, generados a partir de los estados del XML; el `State` es "rico" y contiene toda la máquina de estados, §2.2).
Se construye por reflexión + Guice, así que admite inyección: el esqueleto ya inyecta el repositorio de la entidad por constructor (con `super(<Code>.class, State.class, Event.class, Profile.class)`), y el expediente real añade con `@Inject` de campo `AlmacenClaveResolver` (firma en servidor) y `ModelServiceFactory` (acceso a servicios de otros subsistemas).

El framework la invoca por convención de nombres:

- `triggerInitialEvent(entidad, eventContext)` — al **crear** el expediente.
  Antes de llamarlo, `Tramitador` ya deja rellenos el tipo de expediente, el centro, el usuario registrador, el nombre y el número de expediente; aquí se inicializa el resto (persona interesada y solicitante desde `expediente.getUsuarioRegistrador()`, año…).
  **Obligatorio** dejar relleno `dniFirmaDocumentoEntrada` (el DNI con el que se exigirá firmar; `Tramitador` lo comprueba con `Preconditions` + validación de formato `DniUtil`).
  También conviene dejar relleno **`personaSolicitante`**: si está a null, `eventContext.createRegistroEntrada(...)` revienta con NPE.
- `@WhenEvent trigger<EventoEnUpperCamel>(entidad, original, eventContext)` — uno por evento, `throws BusinessException`.
  Aquí va la lógica de negocio y **el cambio de estado se decide llamando a `eventContext.updateState(State.XXX)`** (puede depender de los datos, p. ej. según `tipoResolucion` se va a `ACEPTADO`, `RECHAZADO` o de vuelta a `ENTRADA_DATOS`).
  También se generan aquí los PDF (§7), los registros de entrada/salida y las firmas de servidor.
- `@OnEnterState onEnter<EstadoEnUpperCamel>(entidad, eventContext)` — uno por estado, se ejecuta al entrar en él (notificaciones, etc.).
  Pueden quedarse vacíos pero deben existir.

**API de `EventContext`** disponible en los triggers:
- `updateState(State)` — fija el estado destino.
- `getProfile()` — el perfil con el que actúa el usuario; `getCentro()` — el centro activo.
- `createRegistroEntrada(persona, asunto, documento, anexos)` / `createRegistroSalida(...)` — crean el registro y **devuelven** el objeto registro; solo se permite **UN** registro de entrada y **UNO** de salida por evento ("Ya existe un registro de entrada definido"); los anexos se clonan y exigen `fileName` no nulo.
- `getRegistroEntrada()` / `getRegistroSalida()` — recuperan el registro creado en este evento.

**Patrón: guardar el documento sellado del registro.**
El valor de retorno de los registros es la otra mitad del patrón: el registro de entrada devuelve el resguardo de presentación (`getDocumentoResguardoPresentacion()`) y el de salida el documento registrado (`getDocumento()`), y ambos se guardan en campos de la entidad convirtiéndolos con `MetaFileHelper.createMetaFile(documentoPdf)`:

```java
RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(expediente.getPersonaSolicitante(), asunto, documentoFirmado, null);
expediente.setPdfJustificanteRegistroEntrada(MetaFileHelper.createMetaFile(registroEntrada.getDocumentoResguardoPresentacion()));
```

**Patrón: evento disparable desde varios estados.**
Si un mismo evento (p. ej. `BACK`) se declara en varios estados, su trigger lee el estado actual y decide el destino:

```java
State currentState = State.valueOf(expediente.getCodeState());
switch (currentState) {
    case PENDIENTE_PRESENTACION -> eventContext.updateState(State.ENTRADA_DATOS);
    ...
}
```

El flujo completo de un evento normal (`Tramitador.triggerEvent`): validar que el evento es legal en el estado actual → obtener las reglas del validator para (estado, evento) → **copiar del request SOLO los campos que tienen reglas** (`AllowProperties`, defensa de mass-assignment) → ejecutar las validaciones (si fallan: `BusinessException` y los mensajes se pintan en el panel de error del footer) → `trigger<Evento>` → guardar historial de estados → `onEnter` → persistir.

Los eventos comunes siguen un camino distinto: `EXIT` lo intercepta `ExpedienteController` antes de llegar al `Tramitador` (no pasa por el EventManager) y `DELETE` no valida ni copia campos — hace directamente `repository.remove` (por eso el validator no necesita método para DELETE); ambos responden al cliente con `refresh-app`.

Nota: el `EventManagerFaltaProfesor` de referencia además implementa `TareaFirmaNotifier` y crea una `TareaFirma` (integración con el subsistema Firmas, con callback `notify(...)`); ese bloque está marcado en el código como "solo una prueba" pendiente de quitar — no lo tomes como parte del patrón estándar sin confirmar.

## 6. El `StateEventValidator` — validación por estado+evento (Kotlin)

Clase Kotlin que implementa la interfaz marcadora `StateEventValidator`.
En **runtime** se invoca un método por cada par estado+evento disparado (nunca para `DELETE`, que no valida); el esqueleto generado trae un método por **cada** evento declarado, incluido DELETE si aparece en `events` (§2.2 — ese método sobra pero no molesta).

```kotlin
// El esqueleto ya trae este alias; es lo que hace funcionar `model::...`
import com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesorado as model

@BeanValidationRulesForStateAndEvent
fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules = rules {
    field(model::getDias) {
        +Required()
        +Pattern("^...$")
    }
    field(model::getHoraFin) {
        +ifValueIn(model::getTipoJornadaFalta, listOf(JORNADA_PARCIAL)) {
            +Required()
            +GreaterThan(model::getHoraInicio)
        }
    }
    field(model::getJustificante) {
        +Required()
        +FileType(listOf("image/png","image/jpeg","application/pdf"))
        +FileMaxSize(5, SizeUnit.MB)
    }
}
```

- Convención de nombre: `getForState<Estado>InEvent<Evento>` en UpperCamel.
- El DSL `rules { field(...) { +Regla() } }` usa las reglas de `base.infrastructure.validation.rules`: `Required`, `Pattern`, `Min/MaxValue`, `Min/MaxLength`, `GreaterThan`, `NoAllUpperCase`, `FileType`, `FileMaxSize`, condicionales `ifValueIn(...)`, y **`FirmaPdf(original, dniGetter)`**.
  `FirmaPdf` valida que el PDF subido es el original firmado con AutoFirma por el DNI exigido; en concreto comprueba: que hay exactamente una firma nueva, que el certificado está en la lista de confiables, que no es un sello de tiempo, que el texto plano del PDF es idéntico al original y que el DNI del certificado coincide — los mensajes de error que ve el usuario salen de esas comprobaciones.
- **Doble función**: además de validar, la lista de campos con reglas define qué propiedades puede enviar el cliente en ese evento (lo que no aparece, no se copia).
  Un evento sin datos igualmente necesita su método con `rules { }` vacío.

## 7. Los documentos PDF

- Cada `*.pdf` de `documentospdf/` (del tipo, más los de `tramites/shared/documentospdf/` si esa carpeta existe — hoy no existe) genera una constante en el enum `TipoDocumentoPdf` de la entidad (extra-code del `domains.xml`, regenerado por el build) con su ruta classpath.
  En el EventManager: `expediente.getDocumentoPdf(TipoDocumentoPdf.SOLICITUD)` (delega en `ExpedienteUtil.getDocumentoPdf`).
  Reglas de nombrado y caveats:
  la constante se deriva del nombre de fichero sin `.pdf` insertando `_` antes de cada mayúscula y pasando a mayúsculas (`solicitudFirmada.pdf` → `SOLICITUD_FIRMADA`);
  no hay validación, así que un fichero con espacios o guiones genera un identificador Java inválido que rompe la compilación después;
  solo se detectan extensiones `.pdf` en minúsculas (un `.PDF` se ignora en silencio);
  y un PDF con el mismo nombre en la carpeta propia y en `shared/` genera constantes duplicadas (error de compilación posterior).
  La ruta de `shared` se calcula cortando el path en la primera aparición de "tramites", y la ruta classpath cortando entre "java" y "documentospdf": rutas de workspace que contengan esas palabras pueden producir resultados erróneos.
- **Los nombres de los campos del formulario (AcroForm) del PDF son expresiones Groovy.**
  Al generar el documento (`DocumentoPdfUtil.generate`) se evalúa cada nombre de campo con el contexto `self` (el expediente) y `now`, y el resultado se estampa como valor del campo, aplanando después el formulario.
  Ejemplos de nombre de campo: `self.personaInteresada.nombre`, `self.dias`, `now.format(...)`.
  Para checkboxes, la expresión debe devolver un booleano (`Yes`/`Off`).
- **Origen del PDF, dos vías**:
  1. **ODT → PDF**: se maqueta un `.odt` (LibreOffice) con campos de formulario cuyo nombre es la expresión Groovy y se exporta a `.pdf` (en la carpeta quedan ambos; el build solo usa los `.pdf`).
  2. **PDF directo de la GVA**: si el organismo ya publica el PDF con formulario (p. ej. `originales/F1.PS04_justificacio_faltes_professorat.pdf`), se renombran sus campos con las expresiones Groovy y se usa directamente, sin ODT.
- **Firma en servidor**: `documentoPdf.firmar(almacenClaveResolver.getDirector(centro), new CampoFirma(rectangulo))`.
  `AlmacenClaveResolver` también ofrece `getSecretario(centro)`, `getByDNI(dni)` y `getDummy()` (pruebas).
  `CampoFirma` es un builder: `setMensaje/setMotivo/setFontSize/setNumeroPagina/setImage/setFechaFirma`.
  `DocumentoPdf` tiene más operaciones útiles: `anyadirDocumentoPdf` (concatenar), `estamparTextoConAppend`, `addNewPage`, `getPlainText`, `removePdfAConformance`.
  Para asignar el resultado a un campo de la entidad: `MetaFileHelper.createMetaFile(documentoPdf)` (ver §5).

### 7.1 Firma del usuario con AutoFirma

La firma del usuario se monta con tres piezas (ver la vista `PENDIENTE_PRESENTACION` de justificación):

1. **Par de campos en `domains.xml`**: el PDF a firmar y el firmado (`pdfSolicitud` / `pdfSolicitudFirmado`, ambos `many-to-one` a `MetaFile`).
2. **Una `<action-method>` declarada en el propio `views.xml`** que llama al controlador genérico de firmas, encadenada antes del evento en el botón:

```xml
<action-method name="exp-JustificacionFaltaProfesorado-firmarDocumentacionParaPresentar-action">
    <call class="com.educaflow.subsystem.expedientes.controllers.FirmaController" method="firmarDocumentoEntrada(id,'pdfSolicitud','pdfSolicitudFirmado',100,20,600,100,1)"/>
</action-method>
...
<button name="PRESENTAR" title="Presentar" onClick="serial:exp-...-firmarDocumentacionParaPresentar-action,subsysExpedientes-event-action"/>
```

   `FirmaController.firmarDocumentoEntrada(id, sourceField, targetField, x, y, width, height, página)` lanza AutoFirma sobre el MetaFile del campo *source*, deja el PDF firmado en el campo *target* y exige firmar con el `dniFirmaDocumentoEntrada` del expediente (falla si está vacío); el rectángulo posiciona la firma visible.
3. **La regla `FirmaPdf` en el validator** del evento `PRESENTAR`, que verifica en servidor que lo subido es el original firmado por ese DNI (§6).

## 8. Pasos para crear un nuevo tipo de expediente

1. **Dar de alta el trámite** (si no existe): crear `tramites/<tramite>/TramiteInstance.xml` (§1.1) con `code`, `name`, `tipoTramite` (ALUMNO/PROFESOR/TUTOR/DIRECCION/ADMINISTRATIVO/CONSERJE…), opcionalmente `publico`/`privado`, y la ayuda en el CDATA de `<help>`.
   El `<defaultTipoExpediente>` se deja sin declarar de momento (se añade en el paso 9).
   La tarea `generateDataInitTramites` genera de él el data-init que da de alta el trámite; es lo que aparece en el árbol de "Crear un nuevo expediente".
2. **Crear la carpeta** `src/main/java/com/educaflow/tramites/<tramite>/<tipo>/` con solo el `TipoExpedienteInstance.xml` (§3): código, trámite y la máquina de estados (recuerda: `events` obligatorio en cada estado, nombres en `UPPER_SNAKE`, `BACK` no viene gratis).
   Conviene dibujar antes el `estados.puml`.
3. **Compilar** (`./run.sh` o `./gradlew clean build`).
   `CreateFilesTask` genera los esqueletos de `domains.xml`, `views.xml`, EventManager y StateEventValidator con todos los métodos requeridos vacíos (contenido exacto: §2.2; con FQCN custom, los ficheros toman el nombre simple de la clase).
4. **Modelo**: añadir a `domains.xml` los campos de la entidad (que `extends="Expediente"`; hereda persona interesada, número, estado, historial…), incluyendo los `many-to-one` a `MetaFile` para cada PDF que se guarde (para AutoFirma, el par original/firmado, §7.1) y los `<enum>` propios que necesite — convención: sufijar el nombre del enum con el de la entidad (`TipoJornadaFaltaJustificacionFaltaProfesorado`) porque todos comparten el paquete `db`; se admiten enums `numeric="true"` con `value=`.
   No tocar `<extra-code-model>` ni cambiar el `package` del `<module>` (está hardcodeado en el check del EventManager, §2.1 punto 10).
5. **PDFs**: crear `documentospdf/` con los `.pdf` de solicitud/resolución con los campos nombrados con expresiones Groovy (§7; nombres de fichero en camelCase sin espacios ni guiones, extensión `.pdf` en minúsculas).
6. **EventManager**: rellenar `triggerInitialEvent` (incluidos `dniFirmaDocumentoEntrada` y `personaSolicitante`) y cada `trigger<Evento>` / `onEnter<Estado>` con la lógica y los `eventContext.updateState(...)` (§5), guardando los documentos devueltos por los registros con `MetaFileHelper.createMetaFile` (§5).
7. **Validator**: rellenar las `rules { }` de cada estado+evento (§6) — recuerda que solo los campos con reglas llegan del cliente, y que el build no comprueba esta clase (§2.1).
8. **Vistas**: en `views.xml`, montar los paneles con nombre en el form `exp-<Code>-Templates` (incluidos los visores de PDF, §4.1) y componer cada `<form state=... profile=...>` con `<include-panels>` y `<footer>` (§4).
   Botón = evento; para firmar con AutoFirma, la `<action-method>` encadenada del §7.1.
   Recuerda que el atributo de cabecera es `header`, no el `includeHeader` que trae el esqueleto.
9. **Activar el tipo**: declarar `<defaultTipoExpediente><Code></defaultTipoExpediente>` en el `TramiteInstance.xml` del trámite (el árbol de trámites crea el expediente del `defaultTipoExpediente` del trámite).
   Nota: el data-init que se genera de ese campo es `update="true" create="false"` y con `priority="-1"`, posterior a la definición (`1`) y al `TipoExpediente` generado (`0`) — solo casa trámites/tipos ya existentes en BD, por eso el orden de carga importa.
10. **Compilar y arrancar**: el build ejecuta todas las comprobaciones de §2.1 (métodos del EventManager que faltan o sobran, paneles inexistentes, form plantilla, i18n…), regenera extra-code/i18n/data-init, y al arrancar se cargan/actualizan en BD el trámite y el tipo.
    Recuerda que el validator y las vistas por estado solo fallan en runtime: pruébalos navegando por todos los estados.
    Probar con un usuario del perfil adecuado (con centro activo y DNI): menú **Expedientes → Trámites** → desplegar el grupo → clic en el trámite.

## 9. Verificación realizada tras la migración

- `./gradlew clean build` en verde (284 tests, ArchUnit y tests de vistas incluidos).
- Aplicación arrancada; data-init cargó los 8 trámites y los 5 tipos con los **FQCN nuevos** (`com.educaflow.tramites.*`) en `expedientes_tipo_expediente`.
- Con Playwright: login `admin`, pantalla "Crear un nuevo expediente", despliegue de "Trámites si eres profesor" y creación real del expediente **00001/2026** de "Justificación de falta del profesorado", con su formulario de `ENTRADA_DATOS` renderizado desde las vistas preprocesadas.
  Sin errores JS ni de servidor.
- Tras el troceo de los data-init globales en data-init por trámite (§1): `./gradlew clean build` en verde, `copyDataInit` copió los 11 data-init nuevos, el procesador i18n regeneró los CSV por carpeta con las mismas traducciones, y con la app arrancada la BD quedó con los 8 trámites en `expedientes_tramite`, los 3 `defaultTipoExpediente` asignados (`CertificadoTutor`, `JustificacionFaltaProfesorado`, `Prueba`) y los 5 tipos de `expedientes_tipo_expediente` enlazados a su trámite.
- Tras eliminar el nivel intermedio `tiposexpedientes/` (cada tipo cuelga ahora directamente de su trámite, `tramites/<tramite>/<tipo>/`): `./gradlew clean build` en verde (284 tests) **sin tocar EducaFlowBuildTools** — el BuildTools deriva el paquete de la ruta tras `/java/` y los shared de la raíz `tramites`, así que solo hubo que mover las carpetas, quitar el segmento `.tiposexpedientes` de packages/FQCN y re-congelar en `archunit_store` la violación conocida de `System.out` de `EventManagerFaltaProfesor` con su paquete nuevo.
- Tras sustituir `definicion/` y `tipo_expediente_activo/` por el fichero maestro `TramiteInstance.xml` (§1.1): `./gradlew clean build` en verde; `generateDataInitTramites` generó los 22 ficheros de data-init en `build/resources/main/tramites/` lógicamente idénticos a los antiguos (mismos atributos, misma ayuda CDATA, mismas `priority`); los CSV i18n movidos a la raíz de cada trámite conservaron las traducciones (nuevo extractor `TitleExtractorImplTramiteInstance` en EducaFlowBuildTools); y con la app arrancada la BD quedó igual que antes de la migración (8 trámites, 3 `defaultTipoExpediente`).
