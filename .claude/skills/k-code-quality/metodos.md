# Calidad de métodos

## Descomposición de métodos

Cuando un método tiene N ≥ 2 pasos o responsabilidades distintas (validar, parsear, extraer, verificar, procesar, construir…) deben existir N métodos privados con nombres descriptivos — uno por paso.

Enumerar los pasos como puntos numerados en el comentario del método público no es suficiente. Cada paso necesita su propia firma privada.

**Violación:** el método describe varios pasos pero no hay métodos privados correspondientes.

**Correcto:** un método privado por cada paso, con nombre de verbo de acción del dominio (ver «Nombrado de métodos»).

Un paso que ya es **una única llamada con nombre** (a un servicio, a un colaborador, a una utilidad) ya está extraído y no cuenta para esta regla: **MUST NOT** envolverse en un método privado (ver «Extraer solo lo que aporta»).

### El método público cuenta la historia entera

Quien lee el método público **MUST** ver todos los pasos sin bajar a ningún privado: cada línea es un paso con nombre.
**MUST NOT** haber un privado intermedio de orquestación que esconda parte de la historia (un «publicar» que por dentro hace dos exportaciones): los pasos que ese intermedio esconde se llaman desde el público.

### Trocear por cosa, no por fase

Cuando el método hace lo mismo para varias **cosas** (varios formatos, varios documentos, varios destinos), se extrae **un privado por cosa**, y cada privado hace su trabajo de principio a fin (crear, rellenar, guardar).
**MUST NOT** trocearse por **fase** (un `crearX` para cada cosa y un `guardar` común) solo para poder meter un método compartido en medio: obliga a leer dos sitios para entender una cosa.

### Pocos casos: un método lineal por caso

Antes de abstraer, cuenta cuántos casos puede haber **en el dominio real**, no en teoría.
Si son pocos (**LIMIT**: hasta ~4), un método lineal por caso con algo de repetición es la forma correcta: se lee de arriba abajo y un LLM no se salta ninguno al cambiarlos.
**MUST NOT** introducirse un genérico, una interfaz ni un bucle sobre una lista de casos para evitar repetir dos o tres líneas.

- ✅ CORRECTO: `exportarPdf(...)` y `exportarCsv(...)`, cada uno con su create → rellenar → insert, llamados uno tras otro desde el público
- ❌ INCORRECTO: `publicar(...)` que llama a `crearPdf` y `crearCsv` y pasa los dos a un `insertarSiEsValido<T extends Documento>(...)` (fase en vez de cosa, y un genérico para ahorrar una línea)

---

## ¿Hace falta el método?

Antes de escribir un método, **MUST** preguntarse si el **dueño del concepto** ya lo expone, aunque con otro nombre: la clase que define ese concepto (el motor para los estados, `DocumentoPdf` para las firmas de un PDF) es donde se busca primero.
Escribir uno nuevo cuando el dueño ya lo tiene deja dos formas de hacer lo mismo y el lector no sabe cuál es la buena.

**MUST NOT** crearse un método para que CPD no rompa el build.
Si CPD marca una repetición, es una de estas dos cosas, y ninguna se arregla con un método en un helper:

- Una **decisión repetida** de verdad: se extrae a su dueño (ver `disenyo.md`, «Dónde va un método»).
- Un **idioma del proyecto** (una guarda, una comparación que se escribe igual en todas partes): se deja en línea y la repetición se declara deliberada con `// CPD-OFF` … `// CPD-ON`.

Un javadoc que diga «escrito en línea CPD rompería el build» es la señal de que el método no debía existir.

- ❌ INCORRECTO: `static boolean estaEn(Expediente expediente, State estado)` en un helper de `tramites/shared`, cuando el motor ya tenía `ExpedienteUtil.getState(expediente)`
- ✅ CORRECTO: `ExpedienteUtil.getState(original) == States.Entrada.ENTRADA_DATOS` en línea en el `triggerBack`

---

## Extraer solo lo que aporta

Un método privado se justifica por lo que **nombra u oculta**, no por existir. Aplica igual al escribir código que al revisarlo: «podría extraerse» no es un defecto.

Se extrae cuando el método nuevo cumple al menos una:

- Nombra un concepto del dominio que la expresión original no dice.
- Oculta varios pasos o una decisión que puede cambiar.
- Elimina una duplicación real de lógica.

**MUST NOT** extraerse:

- Un método cuyo cuerpo es una única llamada que solo reenvía sus parámetros: no nombra nada nuevo y obliga a saltar para leerlo.
- Una expresión que es un **idioma del proyecto**: la que se escribe igual en todo el código y cualquier desarrollador reconoce. Se escribe en línea, como en el resto del proyecto; que se repita no la convierte en duplicación.
- Una condición o guarda de **una línea** cuyo nombre no dice más que el propio código, aunque se repita en varios métodos o clases: el nombre solo obliga a saltar para ver qué compara o qué lanza.
- Un método genérico (`<T extends …>`) o un parámetro de tipo función cuyo único fin es no repetir una llamada de una o dos líneas en dos o tres sitios (ver «Pocos casos: un método lineal por caso»).
- Un método que envuelve una consulta que el dueño del concepto ya ofrece con otro nombre (ver «¿Hace falta el método?»).

**MUST** comprobar cómo resuelven lo mismo las clases hermanas antes de dar a un código una forma distinta: la coherencia con el resto del proyecto gana a la preferencia genérica de estilo.

- ✅ CORRECTO: `final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);` al principio del método, y debajo `miEntidadService.validateDescargar(csv)` y `miEntidadService.descargar(csv)` en línea
- ❌ INCORRECTO: `private MiEntidadService getMiEntidadService() { return (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class); }` (envuelve un idioma del proyecto: solo lo renombra)
- ❌ INCORRECTO: `private Optional<BusinessMessages> validateDescargar(String csv) { return getMiEntidadService().validateDescargar(csv); }` (solo reenvía: el servicio ya le dio nombre)
- ✅ CORRECTO: `Preconditions.checkState(Boolean.TRUE.equals(original.getPresentadoEnPapel()), "El expediente %s (presentadoEnPapel=%s) no puede disparar este evento desde el estado '%s/%s'.", ...);` en línea en cada `trigger*` que es de un solo modo
- ❌ INCORRECTO: `static void exigePresentadoEnPapel(Expediente expediente, boolean presentadoEnPapel) { if (...) throw new IllegalStateException(...); }` en un helper común (guarda de una línea: el nombre no dice más que el código)

---

## Responsabilidad única

Un método público no debe mezclar acciones que pertenezcan a capas o entidades diferentes sin delegar en métodos privados o colaboradores.

Mezclar en un mismo método "valida el fichero", "persiste la tarea" y "actualiza usuarios registrados" es una violación si no hay delegación explícita.

- Si la mezcla es entre entidades distintas → clase colaboradora, o un paso con nombre por entidad llamado desde el público.
- Si es dentro de la misma entidad → métodos privados.
- En los dos casos el público sigue llamando a cada paso: **MUST NOT** aparecer un privado de orquestación (ver «El método público cuenta la historia entera»).

---

## Cálculo puro y efectos secundarios

Separa el cálculo puro de los efectos secundarios: un método que calcula no debe además leer o escribir fuera.

- **Puro:** cálculos, transformaciones, validaciones de formato y reglas de negocio. Recibe como parámetros todo lo que necesita y devuelve el resultado sin modificar estado externo.
- **Impuro:** repositorios y base de datos, ficheros, red y correo, logging y métricas.

La fecha/hora actual queda **fuera** de esta regla: llamar a `LocalDateTime.now(Convert.defaultZoneId)` allí donde se necesita es correcto (siempre con la zona: ver `proyecto.md`, «Zona horaria») y **MUST NOT** marcarse como violación. No hay que inyectar un reloj ni pasar la hora como parámetro.

**Violación:** un método que en el mismo cuerpo calcula el resultado y además lo persiste, envía el correo o escribe el fichero, de forma que el cálculo no se puede ejercitar por separado.

**Corrección:** extraer el cálculo a un método privado —o a una clase colaboradora— que recibe los datos ya leídos y devuelve el resultado; el método llamante hace la E/S antes y después. Al refactorizar, inclínate por hacer más puro el núcleo y empujar los efectos secundarios hacia fuera.

Las integraciones externas (firma, correo, PDF, importación de ficheros) viven en clases frontera pequeñas y con nombre explícito, situadas en el borde del subsistema, no repartidas por la lógica de negocio.

---

## Nombrado de métodos

Los métodos deben tener nombres que describan **qué hacen** en el dominio del problema, no cómo están organizados internamente ni cuál es su posición en el flujo.

- ✅ CORRECTO: `validarContraXSD`, `extraerAtributosCentro`, `resolverCentroDelExpediente`, `generarPDF`
- ❌ INCORRECTO: `procesarPaso2`, `paso1` (posición en el flujo, no qué hace)
- ❌ INCORRECTO: `doWork`, `ejecutarLogica`, `manejar`, `helper`, `metodo` (no dicen nada del dominio)

Los métodos deben tener **un único nivel de abstracción** en su interior. Si un método mezcla orquestación de alto nivel con lógica de detalle (manipulación de strings, bucles sobre bytes), extraer los detalles a métodos privados.

---

## Tamaño y número de parámetros

Un método con más de ~20 líneas en implementación o más de ~10 líneas en descripción funcional es una señal de demasiada responsabilidad. Valorar si merece clase colaboradora (ver `clases.md`).

Número de parámetros:
- 0–2 → ideal.
- 3 → aceptable.
- Más de 3 → el método tiene demasiadas responsabilidades, o los parámetros deberían agruparse en un objeto si están justificados (ver `clases.md` — DTOs).

No usar parámetros booleanos que cambien el comportamiento del método: dos booleanos equivalen a cuatro métodos distintos mezclados en uno.

---

## Complejidad ciclomática y CRAP

**CRITICAL**: por la tarea `crapCheck` (ver `CLAUDE.md`), un método con mucha CC y sin tests unitarios rompe el build aunque funcione.

- **MUST** diseñar e implementar cada método con una CC baja: cada `if`, `else if`, `case`, `&&`, `||`, `?:`, `catch` y bucle suma 1.
- **MUST** acompañar de tests unitarios todo método con ramas: con cobertura total `CRAP = CC`.
- Si la CC ya supera `crapUmbral`, los tests no bastan: **MUST** trocear el método (ver "Descomposición de métodos").
- Para bajar el CRAP de métodos existentes está el skill `developer-reduce-crap`.

---

## Operaciones sobre colecciones

Usar la API de streams para cualquier operación sobre una colección: filtrar, transformar, agrupar, reducir, buscar, contar.

API relevante: `filter`, `map`, `flatMap`, `collect`, `findFirst`, `anyMatch`, `allMatch`, `groupingBy`, `toList`, `count`.

**Violación:** "recorre la lista", "itera sobre Y y acumula los que cumplen Z", "busca el elemento que…" sin usar streams.

Si la colección ya tiene un método directo para lo que se quiere (`getFirst`, `getLast`, `contains`, `isEmpty`, `size`), se llama a ese método: un stream no es señal de calidad.
Y si lo que el stream recalcula es una propiedad que la colección debería tener siempre (un orden), la propiedad va al modelo (ver `proyecto.md`, «Las invariantes del modelo se declaran en el modelo»).

| Bucle imperativo | Equivalente con streams |
|------------------|-------------------------|
| `for` con acumulación en lista temporal | `stream().filter(...).collect(toList())` |
| `for` buscando el primero que cumple | `stream().filter(...).findFirst()` |
| Contador de coincidencias | `stream().filter(...).count()` |
| Transformación de elementos | `stream().map(...).collect(toList())` |

---

## La guarda de cortesía

Cuando una acción es una **cortesía** que no se hace si falta un dato (no hay destino configurado, no hay nada que adjuntar), la decisión de no hacerla es una **condición explícita sobre ese dato**, al principio del método, con `return` temprano.
Así la regla de negocio se lee donde se declara y no hay que ir a otra clase a averiguar cuándo no se envía.

- **MUST** leer el dato **una vez** a una variable local y reutilizarla en la guarda y en el uso: **MUST NOT** repetir la misma cadena de getters dos veces.
- **MUST** comprobar un `String` con `TextUtil.isNullOrBlank` (ver `java-idioms.md`).
- **MUST NOT** loguear el camino de la guarda: que el dato falte es un caso normal del negocio, no un incidente.
- **MUST NOT** sustituir la guarda por capturar el resultado de `validate*` del servicio (ver `disenyo.md`, «La validación del servicio no es control de flujo»).

- ✅ CORRECTO: `String urlWebhook = integracion.getUrlWebhook(); if (TextUtil.isNullOrBlank(urlWebhook)) { return; } … peticion.setUrl(urlWebhook);`
- ❌ INCORRECTO: `if (TextUtil.isNullOrBlank(integracion.getUrlWebhook())) { return; } … peticion.setUrl(integracion.getUrlWebhook());` (lee el dato dos veces)
- ❌ INCORRECTO: `if (…) { log.info("No se llama al webhook…"); return; }` (loguea un camino normal del negocio)