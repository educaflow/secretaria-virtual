# Catálogo de fases

Una **fase** es un grupo de estados consecutivos que persiguen un mismo objetivo dentro de la tramitación ("informar", "recoger datos y presentarlos", "resolver y notificar", "terminar").
Este fichero es el **catálogo de las fases estándar**: describe, para cada una, los estados que aporta, sus eventos, sus perfiles, sus vistas y sus efectos.

Las fases no son narrativa: son **piezas normalizadas y componibles** de máquina de estados, y la base sobre la que se genera después el código (`TipoExpedienteInstance.xml`, `EventManagerImpl`, `StateEventValidatorImpl`, `views.xml`).

**CRITICAL — las fases son GUÍAS, no una camisa de fuerza.**
Este catálogo describe **cómo se resuelven los casos habituales**: el punto de partida para crear los estados de un trámite, no una lista cerrada de lo único que se puede hacer.
Un trámite **PUEDE modificar lo que una fase fija** —añadir o quitar estados, cambiar un perfil, añadir un evento, cambiar un efecto, saltarse una fase entera o crear estados que no encajan en ninguna— **siempre que la especificación lo requiera**.
Manda el negocio: si el trámite necesita algo distinto, se hace distinto.

Lo único que **MUST** cumplirse al desviarse:

- La desviación se **declara explícitamente** en la spec, con su motivo (*"desviación de `F_ENTRADA`: no hay revisión porque la solicitud se resuelve automáticamente"*).
- Se mantienen las **reglas transversales** que no son de la fase sino de la máquina: nomenclatura (§2), un solo estado inicial, todo estado alcanzable, ningún estado no cerrado sin salida, y las dos vistas por estado.

Empezar por el catálogo y desviarse cuando haga falta ahorra decisiones y hace comparables los trámites entre sí; copiarlo cuando no encaja produce un trámite que no hace lo que el negocio pide.

**MUST NOT** copiarse ningún bloque de este fichero al output: la spec instancia las fases, no las reproduce (ver §8).

---

## 1. Qué es una fase y qué no es

| Una fase **es**                                                            | Una fase **no es**                                                                 |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| Un conjunto de estados con un objetivo común y un orden interno            | Un estado (un estado pertenece a exactamente una fase)                             |
| Una **guía** con parte recomendada-fija y parte parametrizable             | Una norma inviolable: se puede modificar si la spec lo requiere (§7)               |
| Una pieza con **un puerto de entrada** y **puertos de salida** declarados  | Una etiqueta descriptiva que se pone a posteriori sobre estados ya inventados      |
| Vocabulario compartido negocio↔diseño↔implementación                       | Una unidad de ejecución en runtime: la plataforma **no conoce** las fases          |

**CRITICAL — la fase no existe en runtime.** La máquina que se ejecuta es la lista plana de estados del enum `State`.
La fase vive en la spec y en el generador: es lo que permite decir "esta parte es la fase de entrada estándar" y emitir siempre los mismos estados, eventos y efectos sin volver a decidirlos.

## 2. Identidad y nomenclatura

| Elemento                                | Formato                                              | Ejemplo                                      |
|-----------------------------------------|------------------------------------------------------|----------------------------------------------|
| Código de fase                          | `F_<NOMBRE>` (UPPER_SNAKE)                           | `F_ENTRADA`                                  |
| Código de estado                        | `<CÓDIGO_DE_FASE>_<SUFIJO>`                          | `F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA` |
| Código de evento                        | UPPER_SNAKE, **único en todo el tipo de expediente** | `PRESENTAR_AUTOFIRMA`                        |
| Paso (§6: cada pantalla del asistente de un estado) | `PASO_<NOMBRE>`, único **dentro de su estado**        | `PASO_SUBSANAR`                              |

- **MUST** — todo estado empieza por el código de su fase seguido de `_`. De ahí se deduce a qué fase pertenece cada estado **sin declararlo**: la columna "Fase" de la tabla de estados es una comprobación, no una decisión.
- **MUST** — los nombres de evento son **únicos por tipo de expediente**, no por estado. Motivo: el `EventManager` tiene **un solo método por evento**, así que dos botones distintos que compartan nombre de evento comparten implementación y obligan a ramificar por estado de origen.
  - Se admite **compartir** un evento entre varios estados **solo** cuando la acción y el destino son idénticos en todos ellos (p. ej. `VOLVER_A_DATOS` desde los dos estados de presentación).
- ✅ CORRECTO: `F_SALIDA_DATOS`, evento `EMITIR`, paso `PASO_INICIO`
- ❌ INCORRECTO: `REVISION_DATOS` (no lleva el prefijo de su fase), `SIGUIENTE` como evento de tres estados distintos con destinos distintos (obliga a un `switch` por estado de origen y hace ilegible la máquina)

### Instanciar la misma fase dos veces

Un trámite puede necesitar **dos salidas** (una propuesta y una resolución) o **dos entradas**.
En ese caso la fase se instancia con un **sufijo distintivo en su código** y todos sus estados lo heredan: `F_SALIDA_PROPUESTA` → `F_SALIDA_PROPUESTA_DATOS`; `F_SALIDA_RESOLUCION` → `F_SALIDA_RESOLUCION_DATOS`.
**MUST NOT** instanciarse dos veces la misma fase con el mismo código.

## 3. Anatomía de la ficha de una fase

Cada fase de este catálogo (§5) se describe **siempre** con los mismos apartados, y en este orden:

| Apartado              | Qué declara                                                                                                                                 |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| **Objetivo**          | Para qué existe la fase, en una frase de negocio                                                                                            |
| **Obligatoriedad**    | `obligatoria` \| `opcional` \| `opcional, repetible`                                                                                        |
| **Puerto de entrada** | El estado por el que se entra a la fase desde la fase anterior                                                                              |
| **Puertos de salida** | Qué estados salen de la fase, con qué evento y hacia qué **fase** (nunca hacia un estado concreto: eso lo fija el trámite al ensamblar, §7) |
| **Estados**           | Una ficha por estado (§4)                                                                                                                   |
| **Aporta al modelo**  | Los campos que la fase necesita en el expediente (si no existen, la fase no se puede usar)                                                  |
| **Parámetros**        | Los huecos que el trámite **MUST** rellenar al instanciarla, con su valor por defecto si lo tiene                                           |

## 4. Anatomía de la ficha de un estado

Cada estado de una fase se describe con esta tabla de cabecera más sus tres bloques:

| Atributo            | Contenido                                                                    |
|---------------------|------------------------------------------------------------------------------|
| Título              | Lo que ve el usuario en pantalla                                             |
| Perfil con el turno | El perfil que puede actuar, o `—` si nadie (estado de espera o cerrado)      |
| Inicial / Cerrado   | Si es el estado inicial de la máquina y si es terminal                       |
| ¿Se puede borrar?   | Si la vista ofrece el borrado del expediente (evento `DELETE` de plataforma) |

**Eventos** — una tabla con una fila por botón, que es la unidad que se proyecta a `TR-` en `estados.md`:

| Columna          | Contenido                                                                                   |
|------------------|---------------------------------------------------------------------------------------------|
| Evento           | El código UPPER_SNAKE                                                                       |
| Botón            | La etiqueta visible                                                                         |
| Destino          | El estado destino, o la **fase** destino cuando sale de la fase, o `—` para `DELETE`/`EXIT` |
| Campos editables | La lista **cerrada** de campos que ese evento admite del cliente, o `(ninguno)`             |

**Vistas** — qué muestra la **vista del perfil con el turno** y qué muestra la **vista genérica** (la que ve cualquier otro perfil con acceso: todo en lectura y solo «Salir»).
**MUST** declararse las dos siempre; un estado sin perfil con el turno tiene **solo** genérica.

**Efectos al entrar** — lo que ocurre automáticamente al entrar en el estado, entre por donde entre (`RN-<ESTADO>-NNN` en `estados.md`).

- Los eventos `EXIT` («Salir») y `DELETE` («Borrar»/«Cancelar») **los aporta la plataforma**: se declaran en la tabla de eventos porque el botón existe, pero **MUST NOT** generar ficha `TR-` propia ni validaciones.
- **MUST NOT** existir un estado no cerrado sin al menos un evento que lo abandone.

## 5. Las fases estándar

### 5.1 `F_INICIO` — Inicio

**Objetivo:** informar al usuario de qué es el trámite y qué necesita **antes** de que empiece a rellenar nada.

**Obligatoriedad:** opcional (recomendada cuando el trámite exige documentación previa o tiene requisitos que conviene leer antes).
Si se omite, el estado inicial de la máquina pasa a ser `F_ENTRADA_DATOS`.

**Puerto de entrada:** `F_INICIO_AYUDA` (es el estado inicial de la máquina: el expediente **ya existe** al llegar aquí, se creó al pulsar el trámite en el árbol).

**Puertos de salida:** `F_INICIO_AYUDA --EMPEZAR--> F_ENTRADA`.

#### `F_INICIO_AYUDA` — «Información del trámite»

| Atributo            | Valor                                                                              |
|---------------------|------------------------------------------------------------------------------------|
| Título              | Información del trámite                                                            |
| Perfil con el turno | `CREADOR`                                                                          |
| Inicial / Cerrado   | inicial: **sí** / cerrado: no                                                      |
| ¿Se puede borrar?   | sí — «Cancelar» es el `DELETE` de plataforma: el expediente recién creado se borra |

**Eventos:**

| Evento    | Botón       | Destino                            | Campos editables |
|-----------|-------------|------------------------------------|------------------|
| `EMPEZAR` | «Siguiente» | `F_ENTRADA` (su puerto de entrada) | `(ninguno)`      |
| `DELETE`  | «Cancelar»  | — (borra el expediente)            | `(ninguno)`      |

**Vistas:**

- Del turno (`CREADOR`): **solo contenido de lectura**. Texto explicativo, requisitos, plazos y los enlaces o visores de los PDF informativos.
- Genérica: el mismo contenido, sin más botón que «Salir».

**Efectos al entrar:** ninguno.

**Aporta al modelo:** nada. **CRITICAL** — `F_INICIO_AYUDA` **MUST NOT** tener campos editables: el usuario no introduce nada aquí. Si hay algo que rellenar, es `F_ENTRADA_DATOS`.

**Parámetros:**

| Parámetro    | Contenido                                                                        | Por defecto     |
|--------------|----------------------------------------------------------------------------------|-----------------|
| `texto`      | El contenido informativo, redactado en castellano (el valenciano se genera solo) | — (obligatorio) |
| `documentos` | Los PDF informativos que se muestran o se enlazan                                | ninguno         |

**Widgets admitidos** (referencia: `k-vistas/references/widgets.md`) — todos de presentación, **ninguno de captura**:

| Widget                      | Para qué                                                 |
|-----------------------------|----------------------------------------------------------|
| `Help`                      | Avisos y notas destacadas (`variant` info/warning)       |
| `Static`                    | Texto fijo que no proviene de un campo                   |
| `Label`                     | Rótulos de sección                                       |
| `Text`                      | Texto largo de un campo, en lectura                      |
| `HTML`                      | Contenido con formato (listas, negritas, enlaces)        |
| `BinaryLink` / visor de PDF | Enlace o previsualización de los documentos informativos |

### 5.2 `F_ENTRADA` — Entrada

**Objetivo:** que el usuario aporte los datos y los adjuntos, presente la solicitud **firmada** por registro de entrada, y que el responsable compruebe que lo presentado sirve.

**Obligatoriedad:** obligatoria.

**Puerto de entrada:** `F_ENTRADA_DATOS`.

**Puertos de salida:** `F_ENTRADA_REVISION_DATOS --ACEPTAR_DATOS--> F_SALIDA` (o, si el trámite no tiene fase de salida, `F_TERMINADO`).

**Forma de la fase** (los cuatro estados y sus caminos):

```
F_ENTRADA_DATOS ──GUARDAR_DATOS──► [¿certificado en el servidor?]
                                    ├─ no  ─► F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA
                                    └─ sí  ─► F_ENTRADA_PENDIENTE_PRESENTACION_FIRMA_SERVIDOR
                                                        │
   ▲──────────VOLVER_A_DATOS───────────────────────────┘
   │                                    └──PRESENTAR_*──► F_ENTRADA_REVISION_DATOS
   │                                                              │
   └───────────────SUBSANAR_DATOS─────────────────────────────────┤
                                                ACEPTAR_DATOS ────┴──► (fase siguiente)
```

#### `F_ENTRADA_DATOS` — «Datos»

| Atributo            | Valor                                                               |
|---------------------|---------------------------------------------------------------------|
| Título              | Datos de la solicitud                                               |
| Perfil con el turno | `CREADOR`                                                           |
| Inicial / Cerrado   | inicial: **sí, solo si el trámite no usa `F_INICIO`** / cerrado: no |
| ¿Se puede borrar?   | sí — botón «Borrar» (`DELETE` de plataforma)                        |

**Eventos:**

| Evento          | Botón       | Destino                                                                                                   | Campos editables                                      |
|-----------------|-------------|-----------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| `GUARDAR_DATOS` | «Siguiente» | `F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA` \| `F_ENTRADA_PENDIENTE_PRESENTACION_FIRMA_SERVIDOR` (ramas) | los datos y adjuntos del trámite (parámetro `campos`) |
| `DELETE`        | «Borrar»    | — (borra el expediente)                                                                                   | `(ninguno)`                                           |

**Ramas de `GUARDAR_DATOS`** — **CRITICAL**: la elige el sistema, **nunca** el usuario:

- si la persona que debe firmar la presentación (el DNI de firma del expediente) **tiene su certificado digital en el servidor** → `F_ENTRADA_PENDIENTE_PRESENTACION_FIRMA_SERVIDOR`;
- si **no lo tiene** → `F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA`.

**Vistas:**

- Del turno (`CREADOR`): los paneles de datos **en edición** y los de subida de adjuntos. Si viene de una subsanación, arriba y en lectura, el **motivo de subsanación** escrito por el responsable.
- Genérica: los mismos paneles en lectura.

**Efectos al entrar:** ninguno.

**Parámetros:**

| Parámetro | Contenido | Por defecto |
|---|---|---|
| `campos` | Los campos y adjuntos que el usuario rellena, y su reparto en paneles | — (obligatorio) |
| `validaciones` | Las comprobaciones bloqueantes de `GUARDAR_DATOS` (empezando por la obligatoriedad campo a campo) | — (obligatorio) |
| `modo_presentacion` | `ambos` \| `solo_autofirma` \| `solo_servidor`. Con `solo_*` la rama contraria y su estado **no se generan** | `ambos` |

#### `F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA` — «Pendiente de presentación»

| Atributo | Valor |
|---|---|
| Título | Pendiente de presentación |
| Perfil con el turno | `CREADOR` |
| Inicial / Cerrado | no / no |
| ¿Se puede borrar? | no |

**Eventos:**

| Evento | Botón | Destino | Campos editables |
|---|---|---|---|
| `PRESENTAR_AUTOFIRMA` | «Firmar con AutoFirma y presentar» | `F_ENTRADA_REVISION_DATOS` | el documento firmado que devuelve AutoFirma |
| `VOLVER_A_DATOS` | «Atrás» | `F_ENTRADA_DATOS` | `(ninguno)` |

**Vistas:**

- Del turno (`CREADOR`): **el visor del PDF de la solicitud** con todos los datos ya volcados, para que el usuario lea lo que va a firmar. Nada editable.
- Genérica: el mismo visor, solo «Salir».

**Efectos al entrar:** `RN-…-001` — generar el PDF de la solicitud a partir de los datos del expediente y guardarlo. Se regenera **cada vez** que se entra (el usuario puede haber vuelto atrás y cambiado datos).

**Efectos de `PRESENTAR_AUTOFIRMA`:** presentar el documento **firmado** por registro de entrada, con los adjuntos, y guardar el resguardo sellado que devuelve el registro.

#### `F_ENTRADA_PENDIENTE_PRESENTACION_FIRMA_SERVIDOR` — «Pendiente de presentación»

Idéntico al anterior salvo en la firma:

| Evento | Botón | Destino | Campos editables |
|---|---|---|---|
| `PRESENTAR_FIRMA_SERVIDOR` | «Presentar» | `F_ENTRADA_REVISION_DATOS` | `(ninguno)` |
| `VOLVER_A_DATOS` | «Atrás» | `F_ENTRADA_DATOS` | `(ninguno)` |

- La vista del turno **MUST** avisar de que el documento se firmará con el certificado que el usuario tiene **en el servidor**, sin AutoFirma.
- **Campos editables `(ninguno)`**: el PDF firmado no viene del cliente, lo produce el servidor. Aquí no hay nada que copiar del request.
- Mismos efectos al entrar (generar el PDF) y mismo efecto al presentar (firmar en el servidor → registro de entrada → guardar resguardo).

#### `F_ENTRADA_REVISION_DATOS` — «Revisión de los datos»

| Atributo | Valor |
|---|---|
| Título | Revisión de los datos |
| Perfil con el turno | `RESPONSABLE` |
| Inicial / Cerrado | no / no |
| ¿Se puede borrar? | no |

**Eventos:**

| Evento | Botón | Destino | Campos editables |
|---|---|---|---|
| `ACEPTAR_DATOS` | «Aceptar los datos» | fase siguiente (`F_SALIDA` o `F_TERMINADO`) | `(ninguno)` |
| `SUBSANAR_DATOS` | «Finalizar» (del paso de subsanación) | `F_ENTRADA_DATOS` | el motivo de subsanación |

**Botonera por pasos** — la vista del `RESPONSABLE` es un **asistente de dos pasos** (§6):

| Paso | Contenido | Botones |
|---|---|---|
| `PASO_INICIO` (inicial) | Solo lectura: el resguardo del registro de entrada y los datos presentados | «Aceptar los datos» → evento `ACEPTAR_DATOS`; «Subsanar los datos» → paso `PASO_SUBSANAR` |
| `PASO_SUBSANAR` | Una caja de texto para escribir qué hay que subsanar | «Atrás» → paso `PASO_INICIO`; «Finalizar» → evento `SUBSANAR_DATOS` |

**Vistas:**

- Del turno (`RESPONSABLE`): el visor del resguardo del registro de entrada y/o los datos presentados en lectura, más la botonera por pasos.
- Genérica (la que ve el `CREADOR` mientras espera): el visor del **resguardo del registro de entrada**, en lectura, con «Salir».

**Efectos al entrar:** ninguno.

**Aporta al modelo (toda la fase):** los datos propios del trámite; el PDF de la solicitud en sus tres versiones (original, firmado y resguardo sellado del registro de entrada); el **motivo de subsanación**.

**Parámetros:**

| Parámetro | Contenido | Por defecto |
|---|---|---|
| `documento_solicitud` | El documento PDF que se genera, se firma y se presenta | — (obligatorio) |
| `hay_revision` | Si el trámite incluye `F_ENTRADA_REVISION_DATOS`. Con `no`, la presentación lleva directamente a la fase siguiente y **no hay subsanación** | `sí` |
| `contenido_revision` | Qué ve el responsable: el resguardo, los datos, o ambos | ambos |

### 5.3 `F_SALIDA` — Salida

**Objetivo:** que el responsable produzca la respuesta del centro, la emita por **registro de salida** y se la notifique al creador.

**Obligatoriedad:** opcional, repetible (un trámite sin respuesta documental no la usa; uno con propuesta + resolución la instancia dos veces, §2).

**Puerto de entrada:** `F_SALIDA_DATOS`.

**Puertos de salida:** `F_SALIDA_DATOS --EMITIR--> F_TERMINADO` (o la siguiente instancia de `F_SALIDA`).

#### `F_SALIDA_DATOS` — «Datos de la respuesta»

| Atributo | Valor |
|---|---|
| Título | Datos de la respuesta |
| Perfil con el turno | `RESPONSABLE` |
| Inicial / Cerrado | no / no |
| ¿Se puede borrar? | no |

**Eventos:**

| Evento | Botón | Destino | Campos editables |
|---|---|---|---|
| `EMITIR` | «Siguiente» | fase siguiente | los campos de la respuesta (parámetro `campos`) |
| `VOLVER_A_<ESTADO>` | «Atrás» | el estado que fije el trámite en la fase **anterior** (parámetro `destino_atras`) | `(ninguno)` |

**CRITICAL — el «Atrás» que sale de la fase.** Una fase **no conoce** a las demás: el destino de este botón **MUST** declararlo el trámite al ensamblar (§7).
Lo habitual es `F_ENTRADA_REVISION_DATOS`. **MUST NOT** dejarse implícito ("vuelve a la fase anterior" no es un destino: una fase tiene varios estados).

**Vistas:**

- Del turno (`RESPONSABLE`): los campos de la respuesta en edición y, en lectura, lo que necesite para decidir (los datos presentados, el resguardo del registro de entrada).
- Genérica: los mismos paneles en lectura.

**Efectos al entrar:** ninguno.

**Efectos de `EMITIR`, en este orden:**

1. Generar el documento de respuesta con los datos del expediente.
2. Firmarlo **en el servidor** con el certificado del cargo que corresponda.
3. Emitirlo por **registro de salida** y guardar el documento sellado que este devuelve.
4. Enviar al creador un **correo** con ese documento adjunto (`fase: después_de_commit` — un fallo del correo **MUST NOT** revertir la emisión).

**Aporta al modelo:** los campos de la respuesta y el documento de salida en su versión registrada.

**Parámetros:**

| Parámetro | Contenido | Por defecto |
|---|---|---|
| `campos` | Los campos de la respuesta y sus validaciones | — (obligatorio) |
| `documento_respuesta` | El documento PDF que se genera y se registra de salida | — (obligatorio) |
| `firmante` | El cargo cuyo certificado del servidor firma el documento | — (obligatorio) |
| `destino_atras` | El estado concreto al que vuelve «Atrás», o `sin_atras` si la fase no admite volver | — (obligatorio) |
| `destinatarios_correo` | A quién se notifica además del creador | solo el creador |

### 5.4 `F_TERMINADO` — Terminado

**Objetivo:** dejar constancia de que el expediente ha terminado y de cuál fue su resultado.

**Obligatoriedad:** obligatoria (toda máquina necesita al menos un estado cerrado).

**Puerto de entrada:** `F_TERMINADO_TERMINADO`.

**Puertos de salida:** ninguno — es el final.

#### `F_TERMINADO_TERMINADO` — «Terminado»

| Atributo | Valor |
|---|---|
| Título | Terminado |
| Perfil con el turno | `—` (nadie: el expediente está cerrado) |
| Inicial / Cerrado | no / **cerrado: sí** |
| ¿Se puede borrar? | no |

**Eventos:**

| Evento | Botón | Destino | Campos editables |
|---|---|---|---|
| `EXIT` | «Salir» | — (cierra la pestaña) | `(ninguno)` |

**Vistas:** **solo la genérica** — todo en lectura, para cualquier perfil con acceso. Muestra el resultado final: la respuesta emitida y los documentos registrados de entrada y salida.

**CRITICAL — "todos" no es un perfil.** Un estado cerrado **MUST NOT** declarar perfil con el turno: no hay nada que disparar. Que "lo vean todos" se consigue **no** teniendo vista de turno, solo genérica.

**Efectos al entrar:** ninguno de serie. Si el trámite necesita un aviso final, va aquí como efecto de entrada (no en la transición: así ocurre se llegue por donde se llegue).

**Parámetros:**

| Parámetro | Contenido | Por defecto |
|---|---|---|
| `contenido` | Qué documentos y campos se muestran como resultado final | los documentos de entrada y salida |

**Varios finales.** Si el trámite distingue desenlaces (aceptado / rechazado / desistido), se instancia con sufijo: `F_TERMINADO_ACEPTADO`, `F_TERMINADO_RECHAZADO` — cada uno un estado cerrado con su propio contenido.

## 6. Pasos: el asistente dentro de un estado

### Qué es un paso

Un **paso** es cada una de las pantallas de un **asistente** (un *wizard*) que vive **dentro de un único estado**.

Cuando el usuario que tiene el turno no toma una decisión simple («pulsa este botón y sigue») sino que **elige entre varias opciones y cada opción le pide cosas distintas**, meterlo todo en una sola pantalla la vuelve confusa (campos que sobran, botones que no aplican), y partirlo en estados de la máquina es peor todavía: nace un estado por el que nadie "está", con su perfil, sus vistas y sus transiciones de ida y vuelta, que además ensucia el historial del expediente.
El paso resuelve justo eso: **la pantalla cambia, el expediente no se mueve**.

Cómo se ve desde fuera:

- **El usuario** ve pantallas que se suceden dentro de la misma ventana del expediente, con sus propios botones («Subsanar los datos» → aparece la caja de texto; «Atrás» → vuelve a la pantalla anterior).
- **El expediente** sigue en el mismo estado todo el rato. En su cabecera se lee el mismo estado, y su historial no registra nada mientras el usuario navega por el asistente.
- **La máquina de estados** no se entera: para ella, el usuario entró en un estado y, un rato después, disparó uno de sus eventos.

El caso típico es `F_ENTRADA_REVISION_DATOS`: el responsable acepta (un clic) o subsana (necesita escribir el motivo antes de finalizar). Dos recorridos, dos pantallas, **un** estado.

**CRITICAL — un paso NO es un estado.** Cambiar de paso **no** cambia el estado del expediente, **no** deja rastro en el historial, **no** cambia el perfil con el turno, **no** persiste nada y **no** se puede retomar: si el usuario cierra la ventana en `PASO_SUBSANAR`, al volver a entrar empieza otra vez en el paso inicial y lo que hubiera escrito se pierde.
Es navegación **de pantalla**: se especifica en la vista del estado, con reglas de UI (`RUI-`) que muestran u ocultan los paneles y botones de cada paso.

### Paso o estado: cómo decidir

| | **Paso** | **Estado** |
|---|---|---|
| ¿Cambia quién tiene el turno? | no — siempre el mismo perfil | sí, puede cambiar |
| ¿Queda en el historial del expediente? | no | sí |
| ¿Se guarda algo al pasar? | no — solo al disparar el evento de salida | sí, cada transición persiste |
| ¿Sobrevive a cerrar la ventana? | no | sí |
| ¿Sirve para esperar a alguien o a algo? | no | sí (es para lo que existen los estados de espera) |
| Dónde se declara | `vistas.md`, dentro de la vista del estado | `estados.md`, tabla de estados y fichas `TR-` |

Si **cualquier** respuesta de la columna "Paso" no te sirve, lo que necesitas es un **estado**, no un paso.

### Reglas

- **MUST** haber exactamente **un** paso inicial (el que se ve al entrar en el estado).
- Cada botón de un paso es **una** de estas dos cosas, nunca ambas:
  - **de navegación** — lleva a otro paso del mismo estado. **MUST NOT** generar ficha `TR-`, ni validaciones, ni efectos.
  - **de salida** — dispara un evento del estado y abandona el estado. Es una transición normal, con su `TR-`, sus campos editables y sus validaciones.
- **MUST** poderse alcanzar todo paso desde el inicial, y **MUST** haber al menos un botón de salida alcanzable desde cada paso (si no, el usuario queda encerrado en el asistente).
- Los campos que un paso recoge **MUST** aparecer en los "Campos editables" del evento de salida que los envía — el paso los pinta, el evento los admite.
- ✅ CORRECTO: `PASO_SUBSANAR` recoge el motivo, y su botón «Finalizar» dispara `SUBSANAR_DATOS`, que declara ese motivo como campo editable.
- ❌ INCORRECTO: modelar `PASO_SUBSANAR` como un estado `F_ENTRADA_SUBSANANDO` (ensucia el historial con un estado por el que nadie "está", y obliga a darle perfil, vistas y transiciones de ida y vuelta).

## 7. Ensamblado: cómo un trámite compone sus fases

1. **Elegir las fases**, en este orden: `F_INICIO` (opcional) → `F_ENTRADA` (obligatoria) → `F_SALIDA` (0..N) → `F_TERMINADO` (obligatoria, 1..N).
2. **Rellenar los parámetros** de cada fase instanciada. Un parámetro obligatorio sin valor es un error de la spec, no un hueco a decidir después.
3. **Cablear los puertos**: cada puerto de salida apunta al **puerto de entrada** de la fase siguiente. Es lo único que el trámite decide sobre la conexión entre fases.
4. **Fijar los destinos "hacia atrás"**: todo botón «Atrás» que cruce una frontera de fase **MUST** nombrar el **estado** concreto de destino (§5.3).
5. **Comprobar la máquina resultante**: un solo estado inicial, todo estado alcanzable, ningún estado no cerrado sin salida, todo evento con nombre único (§2).

**Reglas de composición:**

- **MUST** — todo estado del trámite pertenece a exactamente una fase instanciada (aunque sea un estado que el trámite añade por su cuenta: se adscribe a la fase cuyo objetivo comparte y hereda su prefijo).
- **MUST** — las **desviaciones** se declaran. Desviarse está permitido y es lo correcto cuando el negocio lo pide (ver el aviso del encabezado); lo que no vale es hacerlo **en silencio**, porque entonces la spec y el código generado dejan de coincidir. La spec lo escribe como *desviación de `F_<FASE>`* con su motivo.
- Desviaciones habituales y legítimas: quitar un estado que el trámite no necesita (`hay_revision: no`), añadir un estado propio a una fase (un informe previo dentro de `F_SALIDA`), cambiar el perfil de un estado, añadir un evento (desistir, archivar) o encadenar más de una fase de salida.
- Si la misma desviación aparece en **varios trámites**, deja de ser una desviación: se propone como parámetro nuevo de la fase o como fase nueva del catálogo (§9).

## 8. Cómo se proyecta una fase al resto de la spec

Las fases **no sustituyen** a ningún fichero: lo alimentan. La spec instanciada **MUST** escribir sus estados y transiciones completos —la máquina tiene que leerse sin este catálogo delante— y **MUST** declarar de qué fase viene cada estado.

| De la ficha de fase | Va a | Cómo |
|---|---|---|
| Lista de fases y su objetivo | `specification.md`, apartado "Fases" | Una fila por fase instanciada: código, objetivo y estados que agrupa |
| Ficha de estado (§4) | `estados.md`, tabla de estados | Una fila por estado, con la columna "Fase" rellena |
| Tabla de eventos | `estados.md`, fichas `TR-NNN` | Una ficha por evento **que no sea** `EXIT`/`DELETE`, con sus ramas, campos editables, validaciones y efectos |
| Efectos al entrar | `estados.md`, ficha del estado | `RN-<ESTADO>-NNN` |
| Vistas del turno y genérica | `vistas.md` | Una `### Vista …` por cada una, con sus paneles, botones y `RUI-` |
| Pasos (§6) | `vistas.md`, en la vista del turno | La tabla de pasos y las `RUI-` que los muestran u ocultan |
| "Aporta al modelo" | `entity-<Expediente>.md` | Los campos, con cuándo se rellenan |
| `documento_solicitud` / `documento_respuesta` | `documento-<slug>.md` | Una ficha por documento, con sus firmas `FIR-` |

## 9. Ampliar el catálogo

El catálogo **crece con el uso**: un trámite puede resolver su caso como necesite (§7), y cuando una solución se repite en varios trámites se sube aquí para que el siguiente no tenga que volver a inventarla.

Una fase nueva **MUST** cumplir, para entrar aquí:

- resolver un objetivo **reconocible por el negocio** y reutilizable por más de un trámite;
- tener **un solo puerto de entrada** y sus puertos de salida declarados;
- traer sus estados con **perfil, eventos, vistas (turno y genérica) y efectos** completos;
- separar con claridad su parte **invariante** de sus **parámetros**;
- no solaparse con una fase existente (si es una variante de una que ya está, es un **parámetro** de aquella, no una fase nueva).

Candidatas conocidas, aún **no** catalogadas: espera de firmas de terceros en el portafirmas (estado sin turno + transición automática), trámite de audiencia con plazo, y silencio administrativo por vencimiento de plazo.

## 10. Checklist y anti-patrones

Antes de dar por buena la parte de fases de una spec:

- [ ] Toda fase instanciada tiene **todos** sus parámetros obligatorios con valor.
- [ ] Todo estado empieza por el código de su fase (§2) y aparece en la tabla de estados con su columna "Fase".
- [ ] Todo evento tiene nombre único en el tipo de expediente, salvo los compartidos con **acción y destino idénticos**.
- [ ] Todo estado tiene declaradas sus **dos** vistas (o solo la genérica si no tiene turno).
- [ ] Todo botón «Atrás» que cruza fases nombra su estado destino.
- [ ] Los pasos, si los hay, tienen paso inicial y salida alcanzable desde cada paso.
- [ ] Las desviaciones respecto al catálogo están escritas como tales, con su motivo.

**Anti-patrones:**

- **MUST NOT** desviarse en silencio: cambiar lo que la fase fija está permitido (§7), no declararlo no.
- **MUST NOT** forzar el trámite dentro del catálogo: si la fase estándar no hace lo que el negocio pide, se modifica la fase, no el negocio.
- **MUST NOT** convertir un paso en un estado (§6) ni un estado en un paso (si cambia el perfil con el turno, debe quedar rastro en el historial o tiene que sobrevivir a cerrar la ventana, es un **estado**).
- **MUST NOT** dejar que el usuario elija entre AutoFirma y firma en servidor: la rama la decide el sistema según dónde esté el certificado (§5.2).
- **MUST NOT** declarar perfil con el turno en un estado cerrado (§5.4).
- **MUST NOT** poner campos editables en `F_INICIO_AYUDA` (§5.1).
- **MUST NOT** meter en una fase efectos que solo tienen sentido para un trámite: si no es reutilizable, es un efecto del trámite, no de la fase.
