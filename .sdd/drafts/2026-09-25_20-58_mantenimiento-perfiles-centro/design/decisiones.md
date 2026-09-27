# Decisiones de diseño

## D1 — Quién decide «los centros que supervisa el usuario»

**Problema:** la misma clasificación («centros en los que el usuario tiene el tipo de usuario `SUPERVISOR`») la necesitan cuatro consumidores: el permiso de Axelor que filtra el listado y bloquea editar/borrar filas ajenas, el selector de centro del formulario (RUI-…-mi-centro-formulario-001), el prellenado del centro cuando solo supervisa uno (RUI-…-mi-centro-formulario-005) y las validaciones de servidor VAL-AceProfileCentro-001/002/003.
El permiso de Axelor solo admite una condición JPQL, así que esa copia es inevitable; el resto admite más de una forma.

**Alternativas:**
- **A — JPQL declarativo en cada vista:** el campo `centro` lleva `domain="self.id IN (SELECT cu.centro.id FROM CentroUsuario cu … WHERE cu.usuario = :__user__ AND tu.codigo = 'SUPERVISOR')"` (la misma subconsulta que el permiso), el prellenado se hace con otra expresión Groovy que vuelve a consultar lo mismo, y el servicio lo recorre en Java para validar. Coste: tres copias de la misma clasificación en dos lenguajes (permiso, vista, servicio) más una cuarta para el prellenado. El siguiente desarrollador tendrá que recordar: si mañana también el ADMINISTRATIVO gestiona perfiles (como ya pasó en correos), tocar el permiso, el `domain` del selector, la expresión del prellenado y el servicio, y que ninguna diga algo distinto.
- **B — Un único dueño en código, preguntado por la vista:** el finder JPQL `AceProfileCentroRepository.findCentrosSupervisados(User)` es el único sitio del subsistema que calcula la lista; lo llaman directamente las validaciones y la acción `getCentrosSupervisados()`, que la vista recibe una sola vez al abrir la pantalla vía `<context name="idsCentrosSupervisados" expr="call:…AceProfileCentroController:idsCentrosSupervisados()"/>` en el `<action-view>`. El `domain` del selector (`self.id IN (:idsCentrosSupervisados)`) y el prellenado (`if="idsCentrosSupervisados.size() == 1"`) leen ese valor. Coste: un controlador con un método de tipo 3 y una acción de servicio con su validador. El siguiente desarrollador tendrá que recordar: que los permisos `AceProfileCentro.supervisor` y `Tramite.supervisor` repiten en JPQL la subconsulta del finder (lo dice el comentario de cada uno en `auth-security.xml`).

**Elegida:** B — dentro del subsistema la clasificación vive en un solo sitio de código y la vista la consulta en vez de repetirla (evita el olor «una decisión con varios dueños»); el prellenado y el selector salen del mismo valor, así que no pueden discrepar. Quedan dos copias fuera del finder, declaradas aquí: las dos condiciones JPQL que impone el framework (`AceProfileCentro.supervisor` y `Tramite.supervisor`, cada una con el comentario «si cambia una, cambia la otra»; como el dueño es un finder JPQL y no un recorrido en memoria, son la misma subconsulta casi literal y se comparan a simple vista). `MenuVisibilidadServiceImpl.isSupervisor()` solo decide si se ve el menú y nada del subsistema depende de que coincida con el finder: la lista vacía se resuelve como en el hermano `BandejaController.idsPendientesDeMi()`, con el centinela `List.of(-1L)` en `AceProfileCentroController.idsCentrosSupervisados` (selector vacío y V-008 rechaza cualquier alta), no con una guarda. Si mañana también el ADMINISTRATIVO gestiona perfiles, hay que cambiar el finder y los dos permisos (y, para que vea el menú, `isSupervisor()`). El finder va en el repositorio personalizado de la tabla, como exige el `CLAUDE.md` de `security` para las consultas. Los ids viajan al cliente y son manipulables, pero no son la defensa: la defensa es V-AceProfileCentro-008 en el servicio (evita «defensa solo en la vista»).

**Patrón nuevo:** NO — es el mismo mecanismo que `BandejaController.idsPendientesDeMi()` en `sysVentanilla.PendientesDeMi@Expediente-action` (método de tipo 3 del controlador llamado con `call:` desde el `<context>` de un `<action-view>`, que devuelve ids calculados por el servidor sobre el usuario autenticado). Aquí los ids alimentan el `domain` de un campo en vez del del `<action-view>`; el formulario ve el contexto del `<action-view>` (axelor-front `usePrepareContext` lo mezcla en el contexto del formulario).

## D2 — Cómo llegan al grupo `users` los permisos nuevos del supervisor

**Problema:** el supervisor es un usuario del grupo `users`; sin un permiso de `AceProfileCentro` enlazado a ese grupo, Axelor le deniega todo (y el administrador no lo necesita: `AuthUtils.isAdmin` se salta los permisos). Hoy **todos** los enlaces permiso→grupo viven en el `auth.xml` global (`src/main/resources/data-init/input/auth.xml`), pero `design-guidelines.md` solo autoriza tocar el paquete `com.educaflow.subsystem.security` y el `menus.xml`.

**Alternativas:**
- **A — Añadir las dos líneas al `<group code="users">` del `auth.xml` global:** sigue la convención actual. Coste: toca un fichero fuera del alcance autorizado por las guías. El siguiente desarrollador tendrá que recordar: nada nuevo (es lo de siempre).
- **B — El data-init de `security` enlaza sus propios permisos al grupo:** `auth-security.xml` declara, además de los `<permission>`, un `<group code="users">` con los dos nombres, y su `input-config.xml` añade al `<input>` de `auth-security.xml` el mismo `<bind node="group" …>` que usa el global. Funciona porque `XMLBinder` hace `property.addAll(...)` sobre las colecciones (suma, no reemplaza: el `auth.xml` global, que se carga después con priority `-1`, no los borra) y porque `ModuleManager.createDefault()` crea los grupos `admins`/`users` antes de cargar cualquier data-init. Coste: un segundo sitio donde se enlazan permisos a grupos. El siguiente desarrollador tendrá que recordar: que los permisos de `security` se enlazan en su propio `auth-security.xml` (lo dice el `CLAUDE.md` del subsistema, que este diseño actualiza).

**Elegida:** B — respeta la restricción explícita de las guías y deja el permiso y su enlace al grupo juntos, en el subsistema dueño de la tabla (coherente con la regla de `k-datainit` de que cada subsistema es dueño de sus permisos). Evita el olor «inventar un patrón sin declararlo» declarándolo aquí.

**Patrón nuevo:** SÍ — «un subsistema enlaza sus permisos a los grupos desde su propio data-init». La pieza común no es código sino una receta: faltaría escribirla en `k-datainit` (`SKILL.md` §3.4, junto al formato de `auth-<nombre>.xml`), explicando el `<bind node="group" … create="false" update="true">` y por qué suma (`addAll`) en vez de reemplazar. Hasta entonces la documenta el `CLAUDE.md` de `subsystem/security`.

## D3 — Cómo ve el supervisor los trámites en el selector

**Problema:** el selector de trámite busca en `Tramite` por el endpoint REST, que aplica el filtro de lectura de Axelor. Las condiciones actuales (`Tramite.por*` de `auth-expedientes.xml`) solo dejan leer los trámites sobre los que el usuario ya tiene algún perfil, y un supervisor puro no tiene ninguno: el selector saldría vacío y ESC-001…ESC-005 no se podrían hacer. Las guías prohíben tocar esas condiciones.

**Alternativas:**
- **A — Permiso nuevo `Tramite.supervisor` en `auth-security.xml`:** lectura de todos los trámites para quien es `SUPERVISOR` en algún centro, enlazado al grupo `users` (D2). Coste: el supervisor lee trámites sobre los que no puede crear expedientes, lo que rompe la coherencia «lo que se ve = lo que se puede hacer» que describe el `CLAUDE.md` de `security`. No cambia qué trámites le ofrece «Nuevo trámite» (la ventanilla filtra por `PerfilesUsuarioService`, no por el permiso de lectura) ni qué expedientes ve. El siguiente desarrollador tendrá que recordar: que ese permiso existe por esta pantalla (lo dice su comentario y el `CLAUDE.md`).
- **B — Selector de trámite alimentado por ids del servidor (como D1):** no sirve: el `domain` se aplica **además** del filtro de lectura de `Resource.search`, así que no puede enseñar filas que el permiso oculta.
- **C — Dar al supervisor perfiles sobre todos los trámites:** cambia el cálculo de perfiles, que las guías y el «Fuera de alcance» prohíben.

**Elegida:** A — es la única que cumple los escenarios sin tocar `auth-expedientes.xml` ni el cálculo de perfiles; el alcance del permiso queda acotado a lectura y a quien tiene el tipo `SUPERVISOR`. La excepción a la coherencia «ver = hacer» se documenta en el `CLAUDE.md` del subsistema para que no sea conocimiento tácito.

**Patrón nuevo:** NO — es un `<permission>` condicionado como `Correo.propio-centro-supervisor`.

## D4 — Cómo se comprueba que una asignación no está repetida (RES-AceProfileCentro-007)

**Problema:** la fila es única por (centro, trámite, perfil, tipo de usuario, cargo, usuario), pero en cada fila dos de los tres destinatarios son nulos.

**Alternativas:**
- **A — `<unique-constraint columns="centro,tramite,perfil,tipoUsuario,cargo,usuario"/>` en el dominio:** declarativo. Coste: PostgreSQL trata los `NULL` como distintos, así que dos filas iguales con cargo y usuario vacíos **no** violan la restricción: no protege el caso normal. Además el error sería el genérico de JPA, no «Ya existe esa asignación de perfil». El siguiente desarrollador tendrá que recordar: que la restricción «parece» cubrir el caso y no lo cubre.
- **B — Método del repositorio propio + `validateInsert`/`validateUpdate`:** `AceProfileCentroRepository.existeOtraIgual(AceProfileCentro)` compara centro, trámite, perfil y los tres destinatarios tratando `null` igual a `null`, excluyendo la propia fila por id. Coste: un método de repositorio. El siguiente desarrollador tendrá que recordar: nada (la consulta vive en el repositorio propio de la tabla, como exige el `CLAUDE.md` de `security`).

**Elegida:** B — es la única que valida de verdad y con el mensaje de la spec; la consulta va al repositorio personalizado que ya existe (`AceProfileCentroRepository`), sin tocar el XML de dominio (la spec no cambia el modelo).

**Patrón nuevo:** NO.

## D5 — Cómo se ordenan las validaciones de una fila para que ninguna delegue en otra

**Problema:** RES-006 («el usuario pertenece al centro»), RES-007 (unicidad) y VAL-001/002 (centro supervisado) necesitan el centro, el trámite o el perfil; si faltan, esas reglas no pueden decidir. Hay que evitar que cada una «se calle» por dentro cuando falta un dato, confiando en que otra ya lo haya dicho.

**Alternativas:**
- **A — Acumular todas las reglas, cada una con su guarda interna:** cada helper empieza con `if (fila.getCentro() == null) return;`. Coste: cada regla decide sola si aplica y todas delegan en V-001…003 (olores «una regla que decide sola si aplica» y «el retorno defensivo que delega»). El siguiente desarrollador tendrá que recordar: poner la guarda en cada regla nueva y que su corrección depende de que V-001…003 sigan existiendo.
- **B — Dos fases explícitas en un único método compartido por alta y modificación:** fase 1, forma de la fila: obligatorios (V-001…003) y exactamente un destinatario (V-004/005); si alguno falla se devuelven solo esos mensajes. Fase 2, sobre una fila completa y bien formada: centro gestionable (V-008), pertenencia del usuario al centro bajo la rama visible `if (fila.getUsuario() != null)` —la condición es la de la propia spec, «si hay usuario»— y unicidad (V-007), que así solo se evalúa sobre una fila con exactamente un destinatario, el único caso en que RES-007 tiene sentido. Coste: ninguno añadido. El siguiente desarrollador tendrá que recordar: que una regla nueva que necesite la fila bien formada va en la fase 2.

**Elegida:** B — la condición de cada regla se lee en el sitio donde se declaran y cada regla es total en su rama (evita «una regla que decide sola si aplica», «el retorno defensivo que delega» y «ramas que no cubren todos los casos»). V-004 y V-005 son **un solo** helper que cuenta los destinatarios (cero → mensaje de RES-004; más de uno → mensaje de RES-005), para no tener dos piezas que se conocen (olor «dos piezas que se conocen entre sí»). La rama administrador/resto de V-008 es complementaria por construcción (`isAdmin` / `!isAdmin`).

**Patrón nuevo:** NO.

## D6 — Cómo se hace inmutable el centro y con qué datos se valida el borrado

**Problema:** el centro se elige en el alta y no se puede cambiar (fuera de alcance; RUI-…-002 lo pinta de solo lectura, pero eso no es defensa). Además VAL-003 («el centro de la fila es uno de los que supervisa») debe mirar el centro **real** de la fila: la acción global `remote-validationDelete-action` construye la entidad con `allowPropertiesRemove()`, que por defecto es abierto, así que un cliente podría mandar otro centro y pasar la validación.

**Alternativas:**
- **A — Sobrescribir `update` para restaurar `centro` desde `original` y dejar `remove` abierto:** Coste: sobrescribir `update` obliga a reescribir el patrón validate + `repository.save`, y el borrado sigue validando un centro dictado por el cliente. El siguiente desarrollador tendrá que recordar: mantener la restauración en `update` y que `validateRemove` no es fiable.
- **B — Whitelists explícitas:** `allowPropertiesInsert` con los seis campos (la línea `Input AllowProperties` de Crear), `allowPropertiesUpdate` sin `centro` (la de Modificar), y `allowPropertiesRemove` = `createDenyAllProperties()` (borrar no necesita ningún campo del cliente: la fila se valida tal como está en BD). La whitelist es el único dueño de la inmutabilidad: la fila que llega a `validateUpdate` ya trae el centro de BD, así que `validateInsert` y `validateUpdate` hacen lo mismo (`validarFila(fila)`, V-008 sobre `fila.getCentro()`). Sin sobrescribir `insert`/`update`/`remove`. Coste: tres métodos de una línea. El siguiente desarrollador tendrá que recordar: nada fuera de `k-secure-coding` §3.2.

**Elegida:** B — es exactamente la regla de `design-contract.md` §8.2 para campos inmutables (fuera de la whitelist de `update`) y cierra el hueco de VAL-003 con menos código que A (evita «defensa solo en la vista»).

**Patrón nuevo:** NO.
