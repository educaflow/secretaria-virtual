# Tests

## El test no copia los valores del código que prueba

Un test que redeclara una constante de producción (una medida, un umbral, un margen, un tamaño) no comprueba ningún comportamiento: comprueba que nadie ha cambiado ese número.
Falla cuando alguien ajusta el valor a propósito, aunque el código siga funcionando bien, y obliga a tocar dos sitios por cada cambio.

- **MUST NOT** redeclararse en el test, ni escribirse como literal, un valor que el código de producción ya tiene en una constante.
- **MUST** tomarse de la constante de producción, y expresarse lo esperado como una **relación** entre valores (centrado entre A y B, por debajo de C, A más su separación), no como un número ya calculado.
- Si el valor está enterrado como literal dentro de un método de producción, **MUST** extraerse allí a una constante con nombre y usarse desde los dos sitios. **MUST NOT** copiarse al test.
- Una constante propia del test derivada de las de producción (`TOP_CUERPO = MedidasPagina.PAGE_H - MedidasPagina.MARGIN`) es correcta: da nombre a la relación, no copia el valor.

La prueba, por cada número o texto que aparezca en un test: **si alguien cambia ese valor en producción a propósito, ¿debe fallar este test?**

- **No** → sale de la constante de producción.
- **Sí**, porque el valor lo fija algo externo al código y cambiarlo es un error → va literal en el test. Es el caso del mensaje exacto de una validación que define la spec, o de un formato que impone una norma.

Los datos del propio test no son valores de producción: las entradas, los valores de un fichero de fixture y los resultados que se siguen de ellos van literales.

- ✅ CORRECTO: `assertEquals(MedidasTexto.MARGEN_IZQUIERDO, linea.xInicio(), 0.01)` (el test dice «arranca en el margen», valga lo que valga el margen).
- ✅ CORRECTO: `assertEquals(ALTO_LINEA + 40, arriba.y() - medio.y(), 0.01)` con un `<espacio alto="40"/>` en el fixture (el 40 es un dato del test).
- ✅ CORRECTO: `assertEquals("No se pueden modificar notas de un grupo cerrado", ex.getMessage())` (el texto lo fija la spec: si el código lo cambia, el test debe fallar).
- ❌ INCORRECTO: `private static final double MARGEN_IZQUIERDO = 78;` en el test (copia de `MedidasTexto.MARGEN_IZQUIERDO`: cambiar el margen rompe el test sin que nada funcione mal).
- ❌ INCORRECTO: `assertEquals(302.17, titulo.ancho(), 0.01)` (un resultado calculado a mano a partir de constantes de producción: es la misma copia, más escondida).
- ❌ INCORRECTO: `private static final double BASE = 0.82 * ALTO_LINEA;` cuando el `0.82` es un literal dentro de un método de producción (hay que extraerlo a una constante allí, no duplicarlo aquí).
