# Calidad de métodos

## Descomposición de métodos

Cuando un método tiene N ≥ 2 pasos o responsabilidades distintas (validar, parsear, extraer, verificar, procesar, construir…) deben existir N métodos privados con nombres descriptivos — uno por paso.

Enumerar los pasos como puntos numerados en el comentario del método público no es suficiente. Cada paso necesita su propia firma privada.

**Violación:** el método describe varios pasos pero no hay métodos privados correspondientes.

**Correcto:** un método privado por cada paso, con nombre de verbo de acción del dominio.

Nombres correctos: `validarContraXSD`, `parsearDocumento`, `extraerAtributosCentro`, `construirResultado`.
Nombres incorrectos: `paso1`, `procesarPaso2`, `helper`, `ejecutarLogica`.

---

## Responsabilidad única

Un método público no debe mezclar acciones que pertenezcan a capas o entidades diferentes sin delegar en métodos privados o colaboradores.

Mezclar en un mismo método "valida el fichero", "persiste la tarea" y "actualiza usuarios registrados" es una violación si no hay delegación explícita.

- Si la mezcla es entre entidades distintas → método privado de orquestación o clase colaboradora.
- Si es dentro de la misma entidad → métodos privados.

---

## Cálculo puro y efectos secundarios

Separa el cálculo puro de los efectos secundarios: un método que calcula no debe además leer o escribir fuera.

- **Puro:** cálculos, transformaciones, validaciones de formato y reglas de negocio. Recibe como parámetros todo lo que necesita y devuelve el resultado sin modificar estado externo.
- **Impuro:** repositorios y base de datos, ficheros, red y correo, logging y métricas.

La fecha/hora actual queda **fuera** de esta regla: llamar a `LocalDateTime.now()` allí donde se necesita es correcto y **MUST NOT** marcarse como violación. No hay que inyectar un reloj ni pasar la hora como parámetro.

**Violación:** un método que en el mismo cuerpo calcula el resultado y además lo persiste, envía el correo o escribe el fichero, de forma que el cálculo no se puede ejercitar por separado.

**Corrección:** extraer el cálculo a un método privado —o a una clase colaboradora— que recibe los datos ya leídos y devuelve el resultado; el método llamante hace la E/S antes y después. Al refactorizar, inclínate por hacer más puro el núcleo y empujar los efectos secundarios hacia fuera.

Las integraciones externas (firma, correo, PDF, importación de ficheros) viven en clases frontera pequeñas y con nombre explícito, situadas en el borde del subsistema, no repartidas por la lógica de negocio.

---

## Nombrado de métodos

Los métodos deben tener nombres que describan **qué hacen** en el dominio del problema, no cómo están organizados internamente ni cuál es su posición en el flujo.

**Correcto:** `calcularTotal`, `validarFirma`, `generarPDF`, `resolverCentroActivo`, `procesarDnis`.
**Incorrecto:** `procesarPaso2`, `doWork`, `ejecutarLogica`, `manejar`, `helper1`, `metodo`.

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

## Operaciones sobre colecciones

Usar la API de streams para cualquier operación sobre una colección: filtrar, transformar, agrupar, reducir, buscar, contar.

API relevante: `filter`, `map`, `flatMap`, `collect`, `findFirst`, `anyMatch`, `allMatch`, `groupingBy`, `toList`, `count`.

**Violación:** "recorre la lista", "itera sobre Y y acumula los que cumplen Z", "busca el elemento que…" sin usar streams.

| Bucle imperativo | Equivalente con streams |
|------------------|-------------------------|
| `for` con acumulación en lista temporal | `stream().filter(...).collect(toList())` |
| `for` buscando el primero que cumple | `stream().filter(...).findFirst()` |
| Contador de coincidencias | `stream().filter(...).count()` |
| Transformación de elementos | `stream().map(...).collect(toList())` |