---
type: specification
---


# Objetivo
Una frase con lo que tiene que hacer.

Además: si se sabe, indicar si es un **sistema** o un **subsistema**, y las **dependencias funcionales** de subsistemas existentes (en lenguaje de negocio, sin rutas de código).

# Actores y modelos

## Actores
Los actores que actúan en esta spec

## Modelos
Los modelos de negocio que se usan en la spec. Es como el vocabulario

# Historias de usuario
Usando los actores y el vocabulario de los modelos crea las historias de usuario

Como [Actor] quiero [feature] para [motivo]

# Menús y pantallas

## Menús
Nuevos menús o menús que ya existen que estarán involucrados en la spec

## Pantallas
Nuevas pantallas o pantallas que ya existen que estarán involucradas en la spec

# Escenarios
Cada una de las secuencias de acciones que va a seguir el usuario. Deben estar tanto el camino feliz (happy path) como el resto y también si hay errores o excepciones.

Cada escenario lleva un identificador estable `ESC-NNN` (numeración global desde 001, tres dígitos, sin huecos; si se borra un escenario su número se conserva como hueco y no se reutiliza). Los tests E2E del análisis se materializan a partir de estos escenarios, así que cada `ESC-NNN` debe describir una secuencia completa y verificable.

**Formato:**
```
ESC-001 — <Nombre corto>: <secuencia de acciones del usuario y respuesta del sistema>
ESC-002 — <Nombre corto>: …
```

# Validaciones, reglas, restricciones, reglas de UI y campos calculados

Para cada entidad indicar sus restricciones, campos calculados y, por cada evento, sus validaciones, reglas de negocio y reglas de UI.

**Numeración:** cada elemento lleva un identificador estable con el prefijo de su categoría — `RES-NNN` (restricciones), `VAL-NNN` (validaciones), `RN-NNN` (reglas de negocio), `RUI-NNN` (reglas de UI), `CC-NNN` (campos calculados). La numeración es **global a toda la spec** por prefijo (no por entidad ni por evento), empieza en 001, con tres dígitos y sin huecos. Si se borra un elemento, su número se conserva como hueco (no se reutiliza) para no romper la trazabilidad con análisis ya generados. El análisis usa estos IDs para comprobar que ninguna regla se pierde.

## Restricciones
**Qué son:** Invariantes de una entidad. Condiciones que deben cumplirse siempre, independientemente del evento que se ejecute.

**Dónde viven en el código:** En la propia entidad. Si se viola una restricción, el objeto está en un estado inválido.

**Cómo se asocian:** A la entidad, no a un evento concreto.

**Ejemplo:**
```
Entidad: Expediente
Restricciones:
  - RES-001 — El número de expediente es único en el sistema
  - RES-002 — Un expediente no puede tener dos interesados con el mismo NIF
  - RES-003 — La fecha de cierre no puede ser anterior a la fecha de apertura
```

**Regla de clasificación:** Si la condición debe cumplirse en todos los eventos de la entidad, es una restricción. Si solo aplica a un evento concreto, es una validación.

## Validaciones
**Qué son:** Condiciones bloqueantes asociadas a un evento concreto. Si fallan, el evento se cancela y no ocurre ningún cambio en el sistema.

**Dónde viven en el código:** Las materializa el pipeline más adelante — el análisis las convierte en validaciones `V-…` que el servidor comprueba antes de confirmar el evento. En la spec basta con clasificarlas bien.

**Cómo se asocian:** A un evento de una entidad.

**Atributos opcionales:**
- `estado`: el estado en que debe estar la entidad para que la validación aplique
- `mensaje`: el error que se devuelve al cliente si falla
- `actor`: el rol que dispara el evento (las validaciones pueden diferir por actor)

**Ejemplo:**
```
Entidad: Expediente
Evento: Resolver

Validaciones:
  - VAL-001 — El expediente está en estado TRAMITACIÓN
      estado: [TRAMITACIÓN]
  - VAL-002 — Existe al menos un informe adjunto
  - VAL-003 — El usuario tiene rol TRAMITADOR o DIRECTOR
      actor: [TRAMITADOR, DIRECTOR]
      mensaje: "No tiene permisos para resolver este expediente"
```

---

## Reglas de negocio

**Qué son:** Acciones que el sistema ejecuta automáticamente como reacción a un evento ya confirmado. No bloquean. No deciden si el evento ocurre. Solo actúan.

**Dónde viven en el código:** Las materializa el pipeline más adelante — el análisis las convierte en reglas `R-…` que el servidor ejecuta automáticamente ante el evento. En la spec basta con clasificarlas bien e indicar su `fase`.

**Atributos obligatorios:**
- `fase`: `antes_de_commit` | `después_de_commit`

**`fase: antes_de_commit`** → se ejecuta en la misma transacción. Si falla, hace rollback.

**`fase: después_de_commit`** → se ejecuta una vez confirmada la transacción. Un fallo no revierte el evento principal.

**Atributos opcionales:**
- `estado`: el estado de la entidad para que la regla aplique
- `condición`: cualquier otra condición adicional

**Ejemplo:**
```
Entidad: Expediente
Evento: Abrir

Reglas de negocio:
  - RN-001 — Enviar notificación al interesado
      fase: después_de_commit
      condición: el interesado tiene email registrado

  - RN-002 — Registrar entrada en el libro de registro
      fase: antes_de_commit

  - RN-003 — Asignar tramitador por defecto
      fase: antes_de_commit
      condición: no se ha indicado tramitador explícitamente
```

---

## Reglas de UI

**Qué son:** Condiciones que cambian el aspecto o el estado de un formulario en función del valor de uno o varios campos, del usuario actual, del registro padre o de un evento (al crear, al cargar, al cambiar un campo). Solo afectan a lo que **ve** y puede editar el usuario en pantalla — **no bloquean operaciones ni modifican el estado del sistema**.

**Dónde viven en el código:** En la propia vista (formulario), mediante atributos condicionales o acciones que ajustan la pantalla. Nunca persisten nada por sí solas.

**Cómo se asocian:** A un formulario y, normalmente, a un campo o panel concreto de ese formulario.

**Regla mnemotécnica para distinguirlas:**

- Validación → "no puedes" (bloquea la operación). Si la regla impide guardar, es una **validación**, no una regla de UI.
- Regla de negocio → "ahora hago esto" (escribe en BD o produce efectos colaterales). Si la regla escribe en BD o envía un correo, es una **regla de negocio**, no una regla de UI.
- Regla de UI → "ahora ves esto" (solo cambia el formulario).

**Atributos opcionales:**
- `disparador`: cuándo se aplica — un evento (al crear, al cargar, al cambiar un campo concreto) o `continuo` cuando se evalúa permanentemente.
- `condición`: la expresión que decide si el efecto se aplica. Si es `Siempre`, no hay condición.
- `actor`: el rol del usuario, cuando el efecto depende de quién mira la pantalla.

**Efectos típicos:** mostrar/ocultar un campo o panel, marcar un campo como solo lectura, marcar un campo como obligatorio, fijar un valor por defecto al crear, filtrar las opciones de un campo relacional, cambiar el título de un campo.

**Convención de redacción:** describir qué **ve** el usuario, no cómo se implementa. Un valor por defecto al crear es una regla de UI (no una regla de negocio), porque no se escribe nada hasta que el usuario pulsa Guardar. Si una regla combina varios efectos sobre campos distintos, conviene separarla en varias reglas de UI.

**Ejemplo:**
```
Entidad: Expediente

Reglas de UI:
  - RUI-001 — El campo motivo de rechazo solo se muestra cuando el estado es RECHAZADO
      disparador: continuo
      condición: estado == RECHAZADO

  - RUI-002 — El campo fecha de resolución es de solo lectura cuando el expediente está cerrado
      disparador: continuo
      condición: estado == CERRADO

  - RUI-003 — Al crear un expediente, el centro se rellena con el centro del usuario actual
      disparador: al crear
      condición: Siempre

  - RUI-004 — El botón Publicar solo lo ve el administrador
      disparador: al cargar
      condición: Siempre
      actor: [ADMIN]
```

---

## Campos calculados
**Qué son:** Valores de la entidad que el sistema calcula automáticamente. Nunca los proporciona el cliente; siempre los calcula el servidor.

**Atributos obligatorios:**

| Atributo | Valores | Descripción |
|---|---|---|
| `momento` | `lectura` \| `escritura` | Cuándo se calcula |
| `sobreescribible` | `nunca` \| lista de roles | Quién puede forzar un valor manual |

**`momento: lectura`** → el valor se deriva en memoria cada vez que se lee la entidad. No se persiste.

**`momento: escritura`** → el valor se calcula antes de persistir y se guarda en base de datos.

**Ejemplo:**
```
Entidad: Factura
Campos calculados:
  - CC-001 — total
      momento: escritura
      sobreescribible: nunca
      cálculo: suma de (cantidad × precio_unitario) de todas las líneas

  - CC-002 — estado_mora
      momento: lectura
      sobreescribible: nunca
      cálculo: true si fecha_vencimiento < hoy y not pagada

  - CC-003 — descuento_especial
      momento: escritura
      sobreescribible: [ADMIN]
      cálculo: 0 por defecto; el administrador puede indicar un valor distinto
```

**Convención global:** Un campo calculado nunca se acepta del cliente. Si el cliente envía un valor para un campo calculado, se ignora, salvo que el rol del usuario esté en la lista `sobreescribible`.

---

**Estructura completa**

```
Entidad: [Nombre]

Restricciones:
  - RES-NNN — [condición que siempre debe cumplirse]

Campos calculados:
  - CC-NNN — [nombre_campo]
      momento: lectura | escritura
      sobreescribible: nunca | [ROL1, ROL2]
      cálculo: [descripción]

---

Evento: [NombreEvento]

Validaciones:
  - VAL-NNN — [condición bloqueante]
      estado: [opcional]
      actor: [opcional]
      mensaje: [opcional]

Reglas de negocio:
  - RN-NNN — [acción automática]
      fase: antes_de_commit | después_de_commit
      condición: [opcional]

Reglas de UI:
  - RUI-NNN — [qué ve el usuario en el formulario]
      disparador: continuo | al crear | al cargar | al cambiar <campo>
      condición: [opcional]
      actor: [opcional]
```

---

**Ejemplo completo: Entidad Pedido**

```
Entidad: Pedido

Restricciones:
  - RES-001 — Un pedido tiene al menos una línea
  - RES-002 — No puede haber dos líneas con el mismo producto
  - RES-003 — La fecha de entrega no puede ser anterior a la fecha de creación

Campos calculados:
  - CC-001 — subtotal
      momento: escritura
      sobreescribible: nunca
      cálculo: suma de (cantidad × precio_unitario) de todas las líneas

  - CC-002 — total
      momento: escritura
      sobreescribible: nunca
      cálculo: subtotal + gastos_envio - descuento

  - CC-003 — dias_hasta_entrega
      momento: lectura
      sobreescribible: nunca
      cálculo: fecha_entrega - hoy

  - CC-004 — descuento
      momento: escritura
      sobreescribible: [COMERCIAL, ADMIN]
      cálculo: 0 por defecto

---

Evento: Confirmar

Validaciones:
  - VAL-001 — El pedido está en estado BORRADOR
  - VAL-002 — El stock de cada línea es suficiente
  - VAL-003 — El cliente no tiene deudas pendientes
      mensaje: "El cliente tiene facturas vencidas sin pagar"

Reglas de negocio:
  - RN-001 — Reservar stock de cada línea
      fase: antes_de_commit
  - RN-002 — Enviar email de confirmación al cliente
      fase: después_de_commit
  - RN-003 — Notificar al almacén
      fase: después_de_commit
  - RN-004 — Registrar en el histórico de pedidos del cliente
      fase: antes_de_commit

---

Evento: Cancelar

Validaciones:
  - VAL-004 — El pedido no está en estado ENVIADO ni ENTREGADO
      mensaje: "No se puede cancelar un pedido ya enviado"
  - VAL-005 — Si el actor es CLIENTE, el pedido tiene menos de 24 horas
      actor: [CLIENTE]
      mensaje: "Solo puede cancelar pedidos en las primeras 24 horas"

Reglas de negocio:
  - RN-005 — Liberar stock reservado
      fase: antes_de_commit
  - RN-006 — Emitir abono si el pedido estaba pagado
      fase: antes_de_commit
      condición: pedido.pagado = true
  - RN-007 — Enviar email de cancelación al cliente
      fase: después_de_commit
```

---



# Seguridad

Quién puede ver/crear/editar/borrar cada cosa, en lenguaje natural, cubriendo **todos** los roles del proyecto (tipos de usuario y cargos de `CLAUDE.md`) — el silencio sobre un rol no significa "sin acceso": hay que declararlo explícitamente.

- **Multicentro:** sí | no
- **<Rol>:** <qué puede ver/hacer>
- **<Rol>:** <qué puede ver/hacer>

# Recursos y datos iniciales

Recursos estáticos que necesita la funcionalidad (plantillas PDF, esquemas XSD, certificados…) y datos que deben precargarse al arrancar (catálogos, registros iniciales). Si no hay, indicar `*(no aplica)*`.

# Fuera de alcance
Cosas que no tienen que hacerse
