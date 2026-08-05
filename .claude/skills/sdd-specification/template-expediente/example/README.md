# Ejemplo completo de especificación de un expediente

Esta carpeta es un ejemplo **instanciado** de una especificación terminada (la de la "instancia general": el alumno presenta una solicitud genérica y el centro responde, con posible subsanación): todos los ficheros tal y como quedarían en la carpeta de una iniciativa real. Sirve como referencia del aspecto final.

- `specification.md` — el índice (único con frontmatter `type: specification`): trámite, actores y perfiles (con la nota de que el firmante ajeno **no** necesita perfil), el apartado **Fases** (la composición F_ENTRADA → F_SALIDA → F_TERMINADO sin F_INICIO, con los parámetros de cada fase y las **desviaciones declaradas** — sin revisión en la entrada, firma por portafirmas en la salida: muestra que las fases son guías y cómo se documenta apartarse de ellas), historias con escenarios que recorren todas las transiciones, y las tablas de modelos, documentos y subsistemas.
- `estados.md` — la máquina derivada de las fases: cinco estados con el **prefijo de su fase** y su columna Fase (con un **estado de espera** sin turno), el **bucle de subsanación** (RESOLVER con ramas que devuelven el expediente al creador), una transición **automática** disparada por la firma del portafirmas, y efectos de transición y de **entrada a estado** (el aviso al secretario es de entrada porque debe ocurrir también al re-presentar tras subsanar).
- `estados.puml` / `estados.png` — el diagrama de la máquina (con guardas en las ramas y el suceso de la transición automática).
- `entity-InstanciaGeneral.md` — el expediente: campos con su *"se rellena en TR-…"*, los dos campos-documento por versiones y los campos calculados (fechas de hito). Sin sección de restricciones (no tiene: muestra cómo se omite).
- `entity-AdjuntoInstancia.md` — entidad hija (los adjuntos de la instancia), con sus restricciones `RES-`.
- `model.puml` / `model.png` — el diagrama de clases (expediente, enum, hija).
- `vistas.md` — el almacén de paneles (normal, visores de documento, **maestro-detalle**, ayuda) y las vistas por estado: la del perfil con el turno y la genérica, los botones→`TR-` (incluido el que **firma**), y las `RUI-` (visibilidad condicionada, marcas espejo de validaciones).
- `documento-instancia.md` — documento **propio** (con secciones), firmado por quien **tiene el turno** con **AutoFirma** y presentado por registro de entrada.
- `documento-respuesta.md` — documento propio firmado por un **ajeno al expediente** (el director) vía **portafirmas**: la tríada completa efecto-de-entrada + estado de espera + transición automática, y emitido por registro de salida.

Lo que este ejemplo **no** ejercita (ver la guía para su formato): un documento con `procedencia: impreso oficial` (tabla de mapeo en vez de secciones), la fase `F_INICIO`, los **pasos** (asistente dentro de un estado, `fases.md` §6), la firma de presentación **en el servidor** (`modo_presentacion: ambos` con sus dos ramas), los perfiles nuevos, el inicio de oficio con audiencia y los plazos con silencio administrativo.

**MUST NOT** copiarse el contenido de estos ficheros al output: es solo referencia de forma.
