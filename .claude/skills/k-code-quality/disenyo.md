# Calidad de diseño — los olores que distinguen un diseño senior de una chapuza

Aplica a cualquier diseño o código que introduzca una **pieza nueva** (una regla, una rama, un campo, un patrón): lo que un revisor senior mira antes de preguntarse si "funciona". Un diseño que cumple la especificación y marca todas las casillas del contrato **puede seguir siendo una chapuza**: estos olores son lo que la delata.

## La prueba del segundo desarrollador

Antes de dar por bueno un diseño, escribe cómo lo reproduciría el **siguiente** desarrollador en el **siguiente** caso parecido (otro trámite, otra entidad). Si la explicación contiene «acuérdate de poner X, Y y Z» o «esto solo funciona si también haces W», el diseño exige conocimiento tácito y **falla la prueba**: hay que empaquetar X, Y y Z en una sola pieza o en una receta, hasta que la explicación quepa en una frase.

Entre dos diseños que cubren lo mismo, **gana el que exige recordar menos cosas**, no el que tiene más piezas.

---

## Una decisión con varios dueños

Una clasificación o una decisión (qué casos van por un camino y cuáles por otro) **MUST** vivir en un único sitio, y todo lo demás **MUST** preguntárselo a ese sitio en lugar de repetirla.

**Violación:** la misma lista de valores aparece en el validador, en la vista y en el trigger; al añadir un valor nuevo hay que acordarse de tocar los tres, y el que se olvida falla en silencio.

**Correcto:** el dueño expone un método (`isFirmaEnServidor()`), y el validador, la vista y el trigger lo llaman; la vista recibe del servidor el boolean ya calculado.

- ❌ INCORRECTO: `ifValueIn(listOf(Situacion.A, Situacion.B, Situacion.C))` en el validador y `showIf="situacion=='A' || situacion=='B' || situacion=='C'"` en la vista (dos copias de la misma clasificación)
- ✅ CORRECTO: `ifSituacion({ it.isEnServidor() })` y `showIf="enServidor"` (un dueño, dos consultas)

---

## Una regla que decide sola si aplica

La **condición** de cuándo aplica una regla va en la **rama** que la envuelve, visible en el sitio donde se declaran las reglas. Una regla **MUST NOT** preguntar por dentro «¿estoy en el caso que me toca?» y callarse si no.

**Violación:** para saber cuándo actúa una regla hay que abrir su clase; dos campos que dependen de la misma condición la expresan de dos formas (uno con rama explícita, otro escondida en la regla).

**Correcto:** rama explícita con la condición, y dentro reglas tontas que dan por hecho que están en su caso.

- ❌ INCORRECTO: `field(x) { +ReglaQueMiraLaSituacionPorDentro() }` (la condición no se ve; el lector no sabe cuándo aplica)
- ✅ CORRECTO: `field(x) { +ifSituacion({ it.isEnServidor() }) { +ReglaTonta() } }` (la condición se lee donde se declara)

---

## El retorno defensivo que delega

Una pieza **MUST** ser **total dentro de su rama**: para cada entrada que le llega, o la acepta con motivo o la rechaza con motivo. Un `return null` / `return true` «por si acaso», ante un caso que no sabe tratar, es un hueco que confía en que otra pieza lo tape.

**Violación:** una regla devuelve «válido» cuando falta un dato que necesita para validar (sin DNI no puede comprobar la firma, y devuelve válido); si la otra regla que tapaba ese caso desaparece, el hueco queda abierto sin que nadie se entere.

**Correcto:** si falta lo que necesita, **falla** con un mensaje que lo diga; si el caso no le corresponde, es la rama quien la excluye, no ella. Un caso que solo puede darse por error de programación se rechaza con `IllegalStateException`, no con una excepción de negocio.

- ❌ INCORRECTO: `if (dni.isNullOrBlank()) return null` dentro de `FirmaPdf` (delega en que alguien haya comprobado antes el DNI)
- ✅ CORRECTO: `if (dni.isNullOrBlank()) return error("no se puede comprobar la firma sin DNI")` (total: sin DNI nunca puede dar válido)

---

## Dos piezas que se conocen entre sí

Si una pieza tiene que saber qué hace otra para comportarse (se calla para no duplicar el mensaje de la otra, o da por hecho que la otra ya se ejecutó), **son un solo concepto partido en dos** y **MUST** fundirse.

**Violación:** `ClaveRequerida` y `ClaveCorrecta` por separado, donde la segunda tiene un `if` especial para no repetir el mensaje de la primera; quien las usa tiene que acordarse de poner las dos y en ese orden.

**Correcto:** una única `ClaveValida` que decide en un solo sitio si la clave hace falta y si es correcta.

---

## Ramas que no cubren todos los casos

Cuando un flujo se divide en ramas por una condición, las ramas **MUST** ser **complementarias**: todo caso posible cae en exactamente una. Un caso que no cae en ninguna pasa sin validar, sin que nadie lo haya decidido.

- ❌ INCORRECTO: rama «servidor» si `isEnServidor()` y rama «AutoFirma» si `situacion == SIN_CERTIFICADO` (el caso `SIN_DNI` no cae en ninguna y pasa limpio)
- ✅ CORRECTO: `isEnServidor()` / `!isEnServidor()` (complementarias por construcción)

---

## Defensa solo en la vista

`readonly`, `showIf`, `hidden`, `required` de la vista son **comodidad**, no defensa: el cliente puede saltárselos por REST. Toda restricción que importe **MUST** existir en el servidor; la de la vista es una copia de cortesía (`k-secure-coding`).

**Violación:** el diseño justifica que un campo «no se puede cambiar» porque la vista lo pinta `readonly`.

---

## Inventar un patrón sin declararlo

Si la especificación pide algo que **ninguna receta ni patrón documentado cubre**, el diseño está creando un patrón nuevo, y eso es una decisión que **MUST** declararse, no improvisarse en silencio.

**Correcto:** el diseño lo marca explícitamente como **patrón nuevo**, dice dónde va la pieza común (p. ej. `tramites/util/<propósito>/` si la compartirán varios tipos, o `base`/`subsystem` si es genérica), qué receta faltaría escribir, y por qué no encaja en las existentes. Así el humano lo ve antes de que se implemente.

**Violación:** el diseño resuelve el caso nuevo con piezas ad hoc dentro del trámite, sin decir que es la primera vez que se hace ni dónde debería vivir.

---

## Complejidad heredada por «incorporar ventajas»

Sumar al diseño elegido las ventajas de otros diseños **no** lo mejora si cada ventaja añade una pieza más que recordar. Una ventaja **MUST** incorporarse solo si **no** introduce ninguno de los olores anteriores; si lo hace, la ventaja es aparente.

**Regla práctica:** tras incorporar algo, vuelve a pasar la prueba del segundo desarrollador. Si la explicación se alargó, la incorporación fue un error.
