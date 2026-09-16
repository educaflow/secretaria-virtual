# Comentarios

## El código se explica solo

El comentario es la **excepción**, no el acompañamiento del código. Nadie lo verifica —ni el compilador, ni los tests, ni el build—, así que al primer cambio del código se queda mintiendo, y un comentario desalineado engaña más que la ausencia de comentario. Leer el código no cuesta nada; leer una explicación falsa sí.

Aplica igual a Java, a Kotlin y a los XML del proyecto (dominios, vistas, documentos).

- **MUST NOT** escribirse Javadoc/KDoc ni comentarios que narren **lo que el código ya dice**: qué hace una clase, qué hace un método, qué asigna una línea, qué comprueba una rama.
- **MUST NOT** volcarse al código la justificación de una tarea o de un diseño, ni sus identificadores (`RN-…`, `VAL-…`, `RUI-…`, `CC-…`, `ESC-…`, `HU-…`). Eso vive en `.sdd/`; el árbol del proyecto no es su copia.
- **MUST NOT** comentarse una decisión de diseño «para que no se pierda»: si hay que dejarla escrita, va al artefacto que corresponda (`agent_docs/`, un skill, `CLAUDE.md`), nunca esparcida por los métodos.
- **LIMIT**: máximo **1** comentario explicativo por método. Si un método parece necesitar más, el problema es el método: pártelo o renombra sus variables. Los separadores de bloque no cuentan para este límite.

**Violación:** un Javadoc de clase que resume sus guardas, su flujo y por qué se eligió cada una; un comentario que repite la línea que tiene debajo; una referencia a la regla de la spec que originó el código.

**Correcto:** ningún comentario, y el nombre del método, de la variable o de la clase colaboradora diciendo lo que el comentario iba a decir.

---

## El único comentario que se escribe

El comentario admitido es el de un trozo que **leerlo no basta** para entender: un comportamiento del framework que el código no revela, un orden que parece arbitrario y no lo es, un acoplamiento con otro fichero que desde aquí no se ve, o algo contraintuitivo que el siguiente lector desharía. Dice **por qué**, nunca **qué**.

Correctos:

- `// Sin auto-flush: la consulta toca la misma tabla y persistiría datos aún sin validar.`
- `// Tramitador.checkPerfilDelEstado hace return para el administrador: esta guarda es la misma exigencia sin esa exención.`
- `<!-- Lo reescribe entero el build (RichDomainXmlTask): MUST NOT escribirse aquí nada propio. -->`

Incorrectos:

- `/** Servicio que gestiona los expedientes. */` (repite el nombre de la clase)
- `// Se marca la fecha de revisión y quién revisa` (narra las dos líneas que tiene debajo)
- `// VAL-PENDIENTE_REVISION-SUBSANAR-004: el texto es obligatorio` (identificador de la spec)

---

## Los separadores de bloque SÍ se escriben

No explican nada: **estructuran**. Parten un fichero largo en zonas y por eso mismo **MUST NOT** quitarse — no dicen qué hace el código, así que no pueden quedarse desalineados con él, y sin ellos un fichero de cientos de líneas se lee como un muro.

- **MUST** conservarse los que trae el esqueleto que genera el build: son idioma del proyecto y están en todos los ficheros de su familia.
- **MUST** mantenerse el rótulo de cada zona de un fichero largo: el bloque de métodos de un servicio, el grupo de campos de un dominio, la sección de paneles o de acciones de una vista, el estado al que pertenece un grupo de forms.
- **MUST** ser un rótulo, no una frase: nombra la zona y nada más. En cuanto un separador empieza a explicar, es un comentario y le aplican las dos secciones anteriores.

Correctos:

- `/*************************************** Estados ***************************************/`
- `<!-- ************ PENDIENTE_REVISION ************ -->` sobre los forms de ese estado
- `<!-- Documentos -->` sobre el grupo de campos `MetaFile` de un dominio

Incorrecto:

- `<!-- Revisión: el sentido es obligatorio y cada texto lo es cuando el sentido lo pide -->` (ya no rotula: explica)
