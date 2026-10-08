# Decisiones de diseño

## D1 — Dónde vive el ciclo de vida común de los canales (alta, reenvío, envío, validaciones comunes)

**Problema:** `CorreoServiceImpl` y `SmsServiceImpl` repiten casi línea a línea el alta (valores iniciales, envío tras el commit), el reenvío, el envío con bloqueo pesimista y sus tres reglas de estado, y las validaciones de destinatario, centro y estado del expediente; `cpdCheck` lo va a exigir y la spec pide que un canal nuevo no obligue a tocar lo existente.
Solo cambian tres cosas por canal: qué datos propios valida, qué hace antes de guardar (normalizar el teléfono, copiar los adjuntos) y cómo se envía; más los textos de los mensajes, que la spec quiere «los de su canal».
Hay varias formas razonables de compartirlo.

**Alternativas:**
- **A — Clase base abstracta con plantilla (`NotificacionCanalServiceImpl<T extends Notificacion>`) + interfaz genérica (`NotificacionCanalService<T>`):** la base implementa `insert`/`update`/`remove`/`reenviar`/`validateInsert`/`validateReenviar`/`allowProperties*` y el envío, y declara cinco ganchos abstractos (`validateDatosDelCanal`, `allowPropertiesDelCanal`, `aplicarReglasDelCanalAntesDeGuardar`, `enviarPorCanal`, `textosDelCanal`). Coste: una jerarquía de dos niveles en `service.impl` y una interfaz genérica. El siguiente desarrollador tendrá que recordar: nada que el compilador no le pida — al extender la base, los cinco ganchos son abstractos y no compila sin ellos.
- **B — Composición: una colaboradora `CicloEnvioNotificacion` que cada `*ServiceImpl` llama desde su `insert`, `reenviar`, `enviar` y `validateInsert`.** Coste: cada canal sigue escribiendo el esqueleto de `insert`/`reenviar`/`update`/`remove`/`validateReenviar` y lo cablea con la colaboradora (unas 8 llamadas por canal, en el orden correcto). El siguiente desarrollador tendrá que recordar: llamar a cada método de la colaboradora en el sitio y orden correctos (valores iniciales antes de su regla propia y antes de `save`, programar el envío después), sobrescribir `update`/`remove` con los textos de su canal, y declarar el `allowPropertiesInsert` con los seis campos comunes.
- **C — Mantener dos servicios independientes y marcar `// CPD-OFF`.** Coste: cero piezas nuevas, pero toda corrección del ciclo (p. ej. la del bloqueo) se hace dos veces. El siguiente desarrollador tendrá que recordar: copiar entero un servicio existente y no olvidar ninguna de sus quince piezas.

**Elegida:** A — es la única en la que la prueba del segundo desarrollador cabe en una frase («extiende `NotificacionCanalServiceImpl<X>` y rellena lo que te pide el compilador»).
Frente a `k-code-quality/clases.md` §«Composición frente a herencia»: no es violación, porque sobre `DefaultModelService` (jerarquía del framework, exenta) solo hay un nivel propio y todos los métodos y ganchos de la base los usan todos los canales; la composición (B) se descarta porque reparte el orden del alta entre los canales.
Evita «una decisión con varios dueños» (el orden del alta y las reglas de estado viven solo en la base; en B cada canal las re-ordena), «dos piezas que se conocen entre sí» (en B la colaboradora da por hecho que el servicio ya validó y asignó los valores iniciales) y la duplicación deliberada de C.
Los textos se piden como un único `record TextosCanal` por canal, construido con `I18n.get("…")` literales para que el script de i18n los recoja: así el «mensaje de su canal» de la spec tiene un único dueño por canal.

**Patrón nuevo:** SÍ — pieza común en `subsystem/notificaciones/service/impl/NotificacionCanalServiceImpl.java` (+ `service/NotificacionCanalService.java`): primera jerarquía de `ModelService` del proyecto (servicio base genérico para las subclases de una entidad JOINED). La receta que faltaría escribir va en `k-sistemas/servicios.md` §«Entidades con herencia»: base abstracta `<Base>…ServiceImpl<T>` que cumple C16 por nombre y paquete, ganchos abstractos `protected` antes del primer header, e interfaz genérica `<Base>…Service<T>` que declara las acciones comunes (C23).

**Cambio en la especificación:** NO

## D2 — Modelo polimórfico: estrategia de herencia, quién sabe qué canal es cada clase y de dónde sale el destino

**Problema:** la spec pide un canal (`tipoNotificacion`) que fija siempre el servidor según la clase, un `destino` calculado de lectura que cada canal declara («en un correo, el para; en un SMS, el teléfono») y que se puede filtrar en el listado, y que un canal nuevo no obligue a reabrir un `if` por tipo.
Además hay que elegir la estrategia JPA (las guías recomiendan `JOINED`).

**Alternativas:**
- **A — `JOINED`; `tipoNotificacion` columna normal asignada incondicionalmente en el servicio desde `TipoNotificacion.deClase(clase)`; el enum `TipoNotificacion` (con `extra-code-model`, como `Profile.getPrioridad()`) es el único dueño de «qué clase es cada canal» (`getClaseNotificacion()`); `destino` es un campo función Java de `Notificacion` cuyo `computeDestino()` sobrescribe cada subclase en su `extra-code-model`.** Coste: un `switch` en el enum (en el mismo fichero que sus ítems) y un `@Override` de una línea por subclase. El siguiente desarrollador tendrá que recordar: al añadir un canal, su ítem y su `case` en `TipoNotificacion` (mismo fichero) y el `computeDestino()` en su entidad (si lo olvida, el primer `save` falla con un `IllegalStateException` que lo dice).
- **B — `destino` como `formula="true"` con un `CASE`/`COALESCE` de subconsultas a `notificaciones_correo` y `notificaciones_sms`; el tipo asignado por un gancho `tipoNotificacion()` en cada `*ServiceImpl`.** Coste: el `CASE` SQL es justo el `if` por tipo que la spec pide evitar, y Correo↔CORREO lo saben dos sitios (el gancho y el `CASE`). El siguiente desarrollador tendrá que recordar: reabrir la fórmula de `Notificacion` y declarar el gancho del tipo.
- **C — `destino` persistido por una R-Antes en cada servicio.** Coste: contradice `momento: lectura` del spec y `k-validaciones/reglas-negocio.md` §2.3 (un CC de lectura no se persiste con una R). El siguiente desarrollador tendrá que recordar: asignarlo en su servicio.
- **D — `SINGLE_TABLE`.** Coste: columnas nulas de todos los canales en una tabla y `NOT NULL` imposibles por canal; ninguna ventaja aquí porque las subclases casi no comparten campos propios.

**Elegida:** A — un solo dueño por pregunta: «qué clase es el canal X» lo responde el enum, y «cuál es el destino de esta notificación» la propia entidad.
Evita «una decisión con varios dueños» (B), «ramas que no cubren todos los casos» (el `switch` sobre un enum es exhaustivo y no compila si falta un `case`) y el «retorno defensivo que delega» (la base de `computeDestino()` lanza `IllegalStateException` en lugar de devolver `null`: una notificación sin canal no puede persistirse).
El campo función de Axelor (`@Access(PROPERTY)`) se guarda en su columna al hacer `flush`, así que el destino se puede filtrar y ordenar en el grid como pide la spec, y como la notificación es inmutable no se desincroniza nunca.
Las factorías de `NotificacionService` (`createCorreo()`/`createSms()`) devuelven el objeto **vacío** con el tipo ya asignado: los datos los rellena quien llama (hoy, `VerificacionHelper`), que es quien los conoce; recibirlos rellenos obligaría a un DTO por canal que repetiría los campos de la entidad.
Se descarta que `tipoNotificacion` sea también un campo función: Axelor no tiene precedente de campo función sobre un enumerado y su conversor, y el riesgo no compensa frente a una asignación incondicional en el servicio (que es lo que piden las guías).

**Patrón nuevo:** SÍ — campo función polimórfico (`computeX()` de la entidad base sobrescrito en el `extra-code-model` de cada subclase) y enumerado-catálogo de subclases. Pieza común: ninguna fuera del subsistema; la receta faltaría en `k-sistemas/modelos.md` §«Herencia: campos calculados que cada subclase define».

**Cambio en la especificación:** NO

## D3 — Cómo se abre el formulario del canal desde el listado común y desde la elección de canal

**Problema:** los tres listados son de `Notificacion`, pero cada fila se abre en el formulario de **su** canal, y «Continuar» de la elección de canal abre el alta del canal elegido.
Axelor no tiene apertura polimórfica: la pestaña de un `<action-view>` tiene un solo `model`, y un grid de `Notificacion` abre un form de `Notificacion`.
Lo que haya que hacer tiene que funcionar para un canal nuevo sin tocar lo existente y tiene que volver al listado refrescado.

**Alternativas:**
- **A — Apertura por código en popup.** El grid declara `action` (el mecanismo del fork que ya usa `ventanilla` para abrir el expediente según su tipo) y un `NotificacionController` lee el `tipoNotificacion` de la fila, pide su clase al enum (D2) y abre en popup el form `subsysNotificaciones.<Variante>@<ClaseDelCanal>-form`; el listado de debajo lo refresca la acción global `remote-refreshTab-action` (ver «Refresco del listado tras el alta»). «Continuar» hace lo mismo en alta. Coste: los forms de canal no tienen `<action-view>` con grid, así que su «Salir/Cancelar» cierra con `close` y su `btnSave` termina en `save` → `close`; esto último hoy lo prohíbe `VAR-7.2` (que exige `save` → `force-back` a todo maestro con `btnSave`), y hay que ampliar la rama del «asistente» de esa regla a «form que ningún `action-view` abre con grid». El siguiente desarrollador tendrá que recordar: nombrar los forms de su canal con la convención `<Variante>@<Clase>-form` (la misma convención de nombres de `k-vistas`, verificada por `VAR-2.1`/`VAR-2.3`).
- **B — Un `<action-view>` por canal con su propio grid (`Main@Correo-action`, `Main@Sms-action`) y «Continuar» con un `<action name="…" if="tipoNotificacion == 'CORREO'"/>` por canal.** Coste: el `back` tras guardar vuelve a un listado de solo correos, no al común (la spec dice «vuelve al listado»), duplica grids y abre un `if` por canal en la elección. El siguiente desarrollador tendrá que recordar: añadir su `if` en la elección, su `action-view` y su grid.
- **C — Abrir en pestaña nueva por código (como el expediente).** Coste: `back`/`force-back` no hacen nada en una pestaña sin grid, el listado no se refresca al volver y hay que cerrar con `setCanClose` desde Java. Mismo cambio de regla que A, peor experiencia.
- **D — (Solo para la ventana de elección de canal) entidad no persistible `EleccionCanal` (como `AsistenteNuevoExpediente`) con su form y su controlador.** Coste: un dominio, una vista y un controlador más por una pantalla de un solo campo. Ventaja: ninguna `Notificacion` genérica en memoria. El siguiente desarrollador tendrá que recordar: que el canal se elige en una entidad aparte cuyo enumerado es el mismo `TipoNotificacion`.

**Elegida:** A — es la única que vuelve al listado común refrescado y no reabre nada al añadir un canal.
Evita «una decisión con varios dueños» (la correspondencia variante+clase→form vive solo en un método privado del controlador; la clase del canal, solo en el enum) y la rama por tipo de B.
Lo que no se evita es el cambio de `VAR-7.2` (y, por `remote-refreshTab-action`, de `VAR-7.3` y del glosario «Acciones globales/predefinidas»): se declara aquí como patrón nuevo y se materializa en el diseño (fila `Modificar` de `agent_docs/view-rules.md`, regeneración de `Categoria7BotonesTest` e `Index` con `/developer-create-view-tests`, y sincronía de `k-vistas/forms.md`, `k-vistas/actions.md`, `k-sistemas/controladores.md` y `sdd-designer/template-system/vistas.md`; Paso 11).
La elección de canal es también un popup de `Notificacion` abierto desde un botón de la barra del grid (no el form de la pestaña), para que la pestaña de «Todas» siga en modo grid y sea ella la que se refresca.
Basta ese popup de `Notificacion` y se descarta D: el form tiene `popup-save=false` y no se guarda nunca (`tipoNotificacion` queda fuera de todos los `AllowProperties`), y `NotificacionController.continuarAlta` solo lee `tipoNotificacion` del contexto, sin construir el bean ni pedir su destino; el único riesgo, que algún camino del framework llamara a `computeDestino()` de esa `Notificacion` genérica (que lanza `IllegalStateException`, D2), está registrado en «Notas y supuestos» del `design.md` y se resolvería en ese camino.
Tras reenviar, el form del popup se recarga con `setReload(true)` (lo pide «recarga el formulario»), y el `btnReenviar` de `Main@<Canal>-form` termina en `remote-refreshTab-action`, que refresca el listado de debajo (el de `Centro@<Canal>-form` es solo ese grupo de `Main@`, así que la secuencia del reenvío vive en un único sitio por canal): `refresh-tab` actúa sobre la pestaña activa, no sobre el popup.

**Refresco del listado tras el alta (corregido tras la depuración de T-001):** el refresco de Axelor al cerrar un popup (`popup="reload"`) no llega al listado en el alta.
Al montarse, el popup guarda como «padre» la ventana inmediatamente debajo (`getActiveTabId(1)` en `view-popup.tsx`), y el alta se monta mientras la elección de canal sigue abierta (`executor.ts` ejecuta `close` el último), así que el `tab:refresh` de `popup="reload"` va a la elección, ya cerrada, y el respaldo `__onPopupReload` no se instala porque hay un popup abierto.
Ni el servidor puede cambiar ese padre ni `setCanClose(true)` en `continuarAlta` lo evita (la vista se abre antes que el `close`).
- **Elegida — remoto global de refresco entre `save` y `close`:** el `btnSave` de `Main@Correo-form`/`Main@Sms-form` pasa a `… → save → remote-refreshTab-action → close`; `remote-refreshTab-action` es una acción global sin `model` de `DefaultModelController.xml` (junto a `remote-validationSave-action`), y `DefaultModelController.refreshTab` responde `setSignal("refresh-tab")`, que `axelor-front` manda a `getActiveTabId(-1)` (la pestaña, ignorando los popups).
  El `btnReenviar` de `Main@<Canal>-form` también termina en ella (el de `Centro@<Canal>-form` delega en ese grupo), y `CorreoController`/`SmsController.reenviar` ya no responden `refresh-tab` (solo el aviso y `setReload(true)`): `refreshTab` es el **único** dueño de «refrescar el listado desde un form en popup».
  Coste: un método de una línea y un `action-method` global, ambos en `base/infrastructure/controller/` (fuera del subsistema, lo aplica `/sdd-implementer`); el siguiente desarrollador tendrá que recordar poner `remote-refreshTab-action` en el `btnSave` del `Main@<Canal>-form` de su canal (lo exige la ampliación de `VAR-7.2`, que lo verifica el test con ese nombre fijo) y al final del `btnReenviar` de su `Main@<Canal>-form`; el de `Centro@` es solo `<action name="…Main@<Canal>-btnReenviar-action"/>`.
  Como `remote-refreshTab-action` no está bajo `views/`, entra en el glosario «Acciones globales/predefinidas» y en las globales admitidas por `VAR-7.3` (Paso 11).
- **Descartada — rehacer el flujo Eleccion → alta para que el alta no se abra sobre otro popup** (p. ej. la elección en el propio form de la pestaña, o abrir el alta en pestaña): cambia la decisión de que la pestaña de «Todas» quede en modo grid (la elección de canal pasaría a ser otra pantalla o el alta a ser la alternativa C, con su peor experiencia), toca la vista de elección y el `action-view` de «Todas», y deja dos mecanismos de refresco distintos (`popup="reload"` para el alta, `refresh-tab` para el reenvío).

`ActionResponseHelper.doResponseViewFormEnPopup` abre con `popup="true"`, no con `popup="reload"`: ningún popup de este diseño depende del refresco al cerrar (el alta y el reenvío refrescan con `remote-refreshTab-action` y los detalles son de solo lectura con `popup-save=false`), así que hay un solo mecanismo de refresco.

**Patrón nuevo:** SÍ — «formulario de subclase abierto por código en popup desde un listado de la clase base». Piezas comunes: el método `ActionResponseHelper.doResponseViewFormEnPopup(…)` en `base/infrastructure/axelorhelper/` y la acción global `remote-refreshTab-action` (`DefaultModelController.refreshTab`) en `base/infrastructure/controller/` (genéricas, las podrá usar cualquier jerarquía). Receta que faltaría: `k-vistas/forms.md` §«Form abierto por código en popup» (botones `close`, `btnSave` con `save` → `remote-refreshTab-action` → `close`, sin `action-view` con grid) y la ampliación de `VAR-7.2`/`VAR-7.3` y del glosario de `view-rules.md`; además la «Excepción» de `k-vistas/actions.md` § Convenciones de nombres pasa a tres globales, y `k-vistas/forms.md`/`k-sistemas/controladores.md` dejan de decir que `popup="reload"` refresca el listado.

**Cambio en la especificación:** NO

## D4 — Quién es el dueño de «gestor del centro»

**Problema:** «gestor del centro» (tipo Supervisor o Administrativo, o cargo Director, Jefe de estudios o Secretario, en ese centro) decide cuatro cosas: la visibilidad del menú «Del centro», el `<domain>` de su listado, quién puede reenviar y los permisos de lectura de `Notificacion`, `Correo`, `Sms` y `Adjunto`.
Los permisos de Axelor solo admiten JPQL en `condition`, pero cada `conditionParams` distinto de `__user__` lo evalúa `AuthSecurity.Condition` con `GroovyScriptHelper` (con `__user__` ligado) bajo la `ScriptPolicy` del framework, que admite cualquier clase anotada `@ScriptAllowed` (precedentes en el proyecto: `SecurityUtil`, `MetaFileUtil`, `MenuSecurityService`); `JPQLFilter` pasa el valor tal cual a la query, así que una `List<Long>` sirve para un `IN (?)`.

**Alternativas:**
- **A — Un único dueño real, `GestorNotificacionesUtil` (`util/` del subsistema, como `expedientes/util/*Util`, anotado `@ScriptAllowed`), con `esGestorEnAlgunCentro(user)` e `idsCentrosGestionados(user)` (este último con el centinela `List.of(-1L)` si no gestiona ninguno); el menú, el `validateReenviar` (`idsCentrosGestionados(user).contains(centro.getId())`), el `<domain>` (vía `<context expr="eval: …GestorNotificacionesUtil.idsCentrosGestionados(__user__)">`, la misma expresión que los permisos) y los cuatro permisos `*.propio-centro-gestion` (condition `self.centro.id IN (?)`, conditionParams `…GestorNotificacionesUtil.idsCentrosGestionados(__user__)`) le preguntan.** Coste: una clase de utilidad. El siguiente desarrollador tendrá que recordar: si cambia quién es gestor, tocar solo `GestorNotificacionesUtil`.
- **A' — Lo mismo que A, pero los permisos con la clasificación copiada en JPQL (subconsulta sobre `CentroUsuarioTipoUsuario`/`CentroUsuarioCargo`) y un comentario que apunta al dueño.** Coste: cuatro copias JPQL de la lista de tipos y cargos sin ningún test que las ate al Java. El siguiente desarrollador tendrá que recordar: tocar `GestorNotificacionesUtil` y los cuatro permisos a la vez.
- **B — JPQL en el `<domain>` y en los permisos, y Java repetido en el menú y en el reenvío (lo que hay hoy en correos y SMS).** Coste: la misma lista de tipos y cargos en cinco sitios; hoy ya están desincronizados (`sms-delCentro-menuitem` no tiene `case`). El siguiente desarrollador tendrá que recordar: los cinco sitios.
- **C — Método `esGestorDeNotificaciones(centro)` en el `extra-code` de `User` (`subsystem/common`).** Coste: mete un concepto de notificaciones en el dominio común. El siguiente desarrollador tendrá que recordar: lo mismo que en A, pero en un subsistema ajeno.

**Elegida:** A — un único dueño para los cuatro usos, incluidos los permisos, y la clasificación en el subsistema al que pertenece.
Se descarta A' porque la copia JPQL no es obligada (los permisos pueden preguntar al dueño por `conditionParams`) y sería «una decisión con varios dueños» sin test que la vigile.
Evita también «defensa solo en la vista» (el reenvío lo vuelve a comprobar el servicio con el mismo dueño).
La expresión de `conditionParams` no puede llevar comas (`AuthSecurity` la parte por «,»), por eso el método recibe solo `__user__`.

**Patrón nuevo:** NO

**Cambio en la especificación:** NO

## D5 — El límite de 10 MB de un adjunto en la ventana del adjunto

**Problema:** ESC-062 y RUI-notificaciones-todas-formulario-adjunto-004 piden que, al pulsar «Guardar» en la ventana del adjunto con un fichero de 11 MB, salga «El adjunto no puede superar los 10 MB».
Pero `data.upload.max-size = 10` (en `axelor-config.properties`) hace que el subidor de ficheros de la plataforma (`axelor-front/src/utils/files.ts`, `validateFileSize`) rechace el fichero **al subirlo**, con su propio aviso («You are not allowed to upload a file bigger than 10 MB.»), antes de que exista ningún «Guardar» que pulsar.

**Alternativas:**
- **A — Mantener el límite de la plataforma y corregir la spec** (el aviso sale al subir, con el texto de la plataforma; la `VAL-Adjunto-003` del servidor sigue protegiendo la puerta REST con el mensaje de la spec). Coste: ninguna pieza. El siguiente desarrollador tendrá que recordar: nada.
- **B — Subir `data.upload.max-size` (p. ej. a 25) y comprobar los 10 MB en un `Local-validateSave` del adjunto.** Coste: cambia el límite de subida de **toda** la aplicación por una pantalla, y la comprobación en cliente depende de un `fileSize` que dicta el cliente. El siguiente desarrollador tendrá que recordar: que el límite global ya no protege nada y que cada pantalla con ficheros tiene que limitar el suyo.

**Elegida:** A — el comportamiento pedido no se puede conseguir con el stack sin debilitar un límite global; evita «defensa solo en la vista».

**Patrón nuevo:** NO

**Cambio en la especificación:** APLICADO — `specification.md` § ESC-062 pasos 4-5: dice «sube un fichero «grande.pdf» de 11 MB y pulsa «Guardar» en la ventana del adjunto. 5. El sistema muestra en la ventana del adjunto «El adjunto no puede superar los 10 MB» y no añade el adjunto»; debe decir «intenta subir un fichero «grande.pdf» de 11 MB. 5. El sistema avisa de que no se puede subir un fichero de más de 10 MB y no lo adjunta»; y `screen-notificaciones-todas.md` § RUI-notificaciones-todas-formulario-adjunto-004: dice «al pulsar «Guardar» en la ventana del adjunto se avisa con «El adjunto no puede superar los 10 MB»»; debe decir «al subir el fichero, la plataforma avisa de que no se puede subir un fichero de más de 10 MB y no lo adjunta». Motivo: el subidor de Axelor aplica `data.upload.max-size` al subir, antes de cualquier botón, con su propio texto.

## D6 — Qué se hace con los tests E2E de `correos` y `sms`

**Problema:** las guías dicen «mover y adaptar los E2E de `src/test/e2e/subsystem/correos` y `sms` a `notificaciones`»; pero la spec trae 76 escenarios que ya cubren todo lo que esos 48 tests comprueban (con los menús, columnas y botones nuevos), y `tests-e2e.md` exige que el bloque `T-NNN` de esta iniciativa arranque en el primer número libre de `src/test/e2e/subsystem/notificaciones/`, que hoy no existe.

**Alternativas:**
- **A — Declararlos supersedidos (se borran) y materializar los 76 escenarios como `T-001`…`T-076` en `notificaciones/`.** Coste: se pierde el histórico de esos ficheros, que no aporta nada que los escenarios nuevos no cubran. El siguiente desarrollador tendrá que recordar: nada.
- **B — Moverlos a `notificaciones/` renumerados y, encima, materializar los escenarios.** Coste: dos tests por cada comportamiento (uno «antiguo» adaptado y uno de la spec), con ficheros `.desc.md` cuyo `id:` hay que renumerar a mano (`/sdd-create-tests-e2e` tiene prohibido renumerar). El siguiente desarrollador tendrá que recordar: cuál de los dos manda cuando divergen.

**Elegida:** A — evita la duplicación y deja un único dueño de cada comportamiento (el escenario de la spec). Se registra en `## Tests E2E supersedidos` y en «Notas y supuestos» como desviación consciente de la guía.

**Patrón nuevo:** NO

**Cambio en la especificación:** NO

## D7 — El «en copia» de un correo recibido sin copias

**Problema:** `specification.md` § ESC-053 paso 6 espera ver «el «en copia» vacío» en el formulario del correo recibido, pero RUI-notificaciones-recibidas-formulario-correo-001 dice que el «en copia» solo se muestra si tiene alguna dirección; el escenario y la regla se contradicen para un correo sin copias.

**Alternativas:**
- **A — Mantener la RUI (`enCopia` con `showIf="enCopia"` en `Mis@Correo-form`) y corregir el escenario: sin copias no aparece el campo.** Coste: una errata de redacción en la spec. El siguiente desarrollador tendrá que recordar: nada.
- **B — Mostrar siempre el «en copia», aunque esté vacío, para que cuadre con el escenario.** Coste: contradice la RUI y deja un campo vacío sin información. El siguiente desarrollador tendrá que recordar: que la RUI no se cumple.

**Elegida:** A — la RUI es la regla normativa de la pantalla; el escenario solo describía mal el resultado.

**Patrón nuevo:** NO

**Cambio en la especificación:** APLICADO — `specification.md` § ESC-053 paso 6: dice «el «para» «alumno1@mislata.es», el «en copia» vacío, la fecha de envío»; debe decir «el «para» «alumno1@mislata.es», sin el campo «en copia» (el correo no tiene copias), la fecha de envío». Motivo: RUI-notificaciones-recibidas-formulario-correo-001 solo muestra el «en copia» si tiene alguna dirección.

## D8 — Cómo se muestra el «Estado del expediente» en el selector del alta

**Problema:** el `historialEstado` de `Main@Correo-form`/`Main@Sms-form` apunta a `com.educaflow.subsystem.expedientes.db.HistorialEstado`, que no tiene `name`, `namecolumn` ni ningún campo que junte el número del expediente con la fase o el estado; sin `target-name` ni vista de selección, Axelor busca y muestra solo ids («1», «2») y el usuario no puede elegir el estado de ESC-070/ESC-071 (T-028, T-029).

**Alternativas:**
- **A — Añadir a `HistorialEstado` un `namecolumn` o un nombre calculado (número de expediente + «Fase / Estado»).** Coste: modifica el dominio `subsystem/expedientes`, que el diseño **MUST NOT** tocar (README §4.2) y que heredan todos los tipos de expediente. El siguiente desarrollador tendrá que recordar: que un nombre de presentación de expedientes lo puso notificaciones.
- **B — Resolverlo en las vistas: `target-name="nameState"`, `canSuggest="false"` y unas vistas de referencia `Ref@HistorialEstado-grid`/`-form` cuyo buscador muestra número de expediente, expediente, fase, estado y fecha.** Coste: un fichero `Ref-*.xml` más; el campo enseña solo el estado («Pendiente de verificación»), no el número, que se ve en el buscador y en la columna «Expediente» del listado. El siguiente desarrollador tendrá que recordar: que la elección se hace siempre en el buscador (sin sugerencias, que serían ambiguas entre expedientes con el mismo estado).

**Elegida:** B, con las vistas de referencia en `subsystem/expedientes/views/Ref-HistorialEstado.xml` (`subsysExpedientes.Ref@HistorialEstado-grid`/`-form`, vista nueva de solo lectura), no en `notificaciones`: VAR-1.2(b) exige que la `{Entidad}` del nombre del fichero exista en `../domains` del módulo, y `HistorialEstado` se declara en `subsystem/expedientes/domains/`, igual que `Ref@Centro` vive en `subsystem/common/views/Ref-Centro.xml`. B es preferible a A porque no cambia el dominio de `expedientes` (que heredan todos los tipos de expediente): solo añade una vista de solo lectura en su módulo. `canSuggest="false"` evita que el desplegable ofrezca estados homónimos de expedientes distintos. `Centro@Correo-form`/`Centro@Sms-form` (solo lectura) reutilizan las mismas vistas de referencia (`target-name="nameState"` + `grid-view`/`form-view` `subsysExpedientes.Ref@HistorialEstado`), para que el estado ligado se abra en la consulta con número de expediente, fase y estado.

Excepción a template-system/README.md §4.2 (MUST NOT crear ni modificar nada en subsystem/expedientes) autorizada por el usuario en persona: «acepto la opción A, crea la vista en expedientes». Es acotada: solo autoriza esa vista de solo lectura Ref-HistorialEstado.xml y no autoriza cambiar el dominio, el servicio ni nada más de expedientes. Motivo: VAR-1.2(b) exige que la vista viva en el módulo dueño de la entidad (precedente Ref-Centro).

**Patrón nuevo:** NO

**Cambio en la especificación:** NO

## D9 — Cómo llega un test E2E a «Verificación» / «Pendiente de verificación» de una justificación

**Problema:** los escenarios que presentan una «Justificación de falta del profesorado» (ESC-070, ESC-071, ESC-057, ESC-075; T-028, T-029, T-075, T-076) usan la fecha fija «10/09/2026», que el trámite ya rechaza (solo admite una fecha de los últimos 7 días y no futura: «Debe tener como mínimo el valor de 2026-09-30»), y pulsan «Firmar y Presentar la solicitud», que solo aparece si el director tiene un certificado de firma en el servidor; los datos de demo no lo traen (los aporta otra iniciativa) y el otro botón, «Firmar con AutoFirma y Presentar la solicitud», necesita el cliente de escritorio y no se puede pilotar desde el navegador.

**Alternativas:**
- **A — Dar de alta el certificado desde el navegador antes de presentar (el administrador, en «Criptografía» → «Certificados digitales», con el `firma/mi_certificado.p12` del classpath) y usar la fecha de hoy; si aun así no aparece la firma en el servidor, el test declara que depende de la otra iniciativa.** Coste: dos valores reutilizables en `test-e2e-desc.md` y un paso más en cada test. El siguiente desarrollador tendrá que recordar: nada (cuando los datos de demo traigan el certificado, el alta del certificado se reutiliza o sobra).
- **B — Solo declarar la dependencia y dejar esos cuatro tests sin automatizar hasta que se fusione la otra iniciativa.** Coste: cuatro tests sin ejecutar. El siguiente desarrollador tendrá que recordar: reactivarlos.

**Elegida:** A — hace la precondición alcanzable con lo que hoy existe en el código y deja declarada la dependencia por si no lo fuera.

**Patrón nuevo:** NO

**Cambio en la especificación:** APLICADO — `specification.md` § ESC-070, ESC-071, ESC-057 y ESC-075, paso 3: dice «rellena «Fecha» con «10/09/2026»»; debe decir «rellena «Fecha» con la fecha de hoy». Motivo: el trámite solo admite una fecha de los últimos 7 días y no futura, así que una fecha fija caduca.

**Cambio en la especificación:** APLICADO — `specification.md` § Recursos y datos iniciales: dice «Los certificados digitales de los usuarios de demo para firmar en el servidor (los añade otra iniciativa a los datos de demo). Los escenarios que presentan un expediente («Justificación de falta del profesorado») firman la solicitud en el servidor con el certificado del profesor que la presenta, sin AutoFirma.»; debe decir «Los certificados digitales de los usuarios de demo para firmar en el servidor: hoy los datos de demo no los traen (los aportará otra iniciativa). Mientras tanto, los escenarios que presentan una «Justificación de falta del profesorado» (ESC-070, ESC-071, ESC-057, ESC-075) dan de alta antes, desde «Criptografía» → «Certificados digitales», el certificado del director, y entonces firman la solicitud en el servidor, sin AutoFirma.». Motivo: los datos de demo solo traen `centros-demo.xml` y `usuarios-demo.xml`, así que sin ese alta previa no aparece «Firmar y Presentar la solicitud».

**Cambio en la especificación:** APLICADO — `specification.md` § ESC-070, ESC-071, ESC-057 y ESC-075: dice que el profesor presenta la justificación firmando en el servidor sin paso previo; debe decir que, como primer paso, el administrador da de alta desde «Criptografía» → «Certificados digitales» el certificado de firma del director (DNI «85432016B», «Ruta classpath» «firma/mi_certificado.p12», contraseña «nadanada») y cierra sesión antes de que el profesor presente la justificación. Motivo: el certificado de firma en el servidor no está en los datos de demo.
