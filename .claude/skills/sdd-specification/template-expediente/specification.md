---
type: specification
---

# El trámite

<Qué es el trámite en una frase; a quién va dirigido (en qué rama del árbol de crear expedientes aparece: profesores, alumnos, tutores, dirección, administrativos o conserjes); si es público o privado; y si es la primera versión de un trámite nuevo o una versión nueva de uno existente (y en ese caso, qué cambia).>

**Texto de ayuda del árbol:** <el texto que verá el usuario al elegir el trámite en el árbol; admite formato>

# Actores y perfiles

- **<Actor>**: <quién es y qué papel juega>

<!-- La tabla de perfiles es el reparto de turnos de la tramitación. CREADOR y RESPONSABLE ya existen en la plataforma; cualquier otro es NUEVO y MUST aparecer también en «Recursos y datos iniciales». Deny by default: un rol que no aparece aquí no ve el expediente. El ámbito es: individual (cada uno ve solo sus expedientes) | centro | departamento. -->

| Perfil | Quién lo recibe | Ámbito | ¿Nuevo? |
|---|---|---|---|
| CREADOR | <p. ej. el alumno — quien crea el expediente; o un cargo del centro si el trámite se inicia de oficio> | individual | no |
| RESPONSABLE | <p. ej. el secretario del centro> | centro | no |
| <PERFIL_NUEVO> | <p. ej. el director del centro, que firma las resoluciones> | centro | sí |

# Fases

<!-- OBLIGATORIO: aquí se compone la máquina a alto nivel a partir del catálogo fases.md, en orden: F_INICIO (opcional) → F_ENTRADA → F_SALIDA (0..N) → F_TERMINADO (1..N, o con sufijo por desenlace). Una fila por fase instanciada. Las fases son GUÍAS: pueden modificarse si el negocio lo pide, pero toda desviación se declara con su motivo — nunca en silencio. La máquina detallada de estados.md deriva de este apartado (mismos estados, cada uno con el prefijo de su fase). -->

| Fase | Objetivo en este trámite | Estados que aporta | Desviaciones del catálogo |
|---|---|---|---|
| <F_INICIO> | <informar de los requisitos antes de empezar> | <F_INICIO_AYUDA> | — |
| <F_ENTRADA> | <qué datos aporta y presenta el usuario> | <F_ENTRADA_DATOS, F_ENTRADA_PENDIENTE_PRESENTACION_AUTOFIRMA, …> | <— | p. ej. sin revisión: la revisión la hace el responsable al resolver> |
| <F_SALIDA> | <qué respuesta emite el centro> | <F_SALIDA_DATOS, …> | <—> |
| <F_TERMINADO> | <cómo queda el expediente al acabar> | <F_TERMINADO_TERMINADO> | <—> |

**Parámetros de <F_FASE>:** <!-- un bloque por fase que tenga parámetros; los obligatorios de su ficha en fases.md MUST tener valor -->

- <parámetro>: <valor; p. ej. modo_presentacion: solo_autofirma; destino_atras: F_ENTRADA_REVISION_DATOS>

# Historias de usuario

<!-- Una historia por cada `## HU-NNN`; debajo de cada una, sus escenarios `ESC-NNN`. Cada escenario va SIEMPRE como una lista de pasos numerados (un paso por línea), con ramas condicionales si hace falta — nunca como varias frases en una sola línea. Entre todos los escenarios deben recorrerse TODAS las transiciones de estados.md (incluidas las ramas y las automáticas). Cuando un paso lo hace otro perfil, el escenario lo dice: cierra sesión, inicia sesión el otro usuario (de los datos de demo), abre el expediente desde su bandeja. -->

## HU-001 — Como <Actor> quiero <feature> para <motivo>

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión con una cuenta de los datos de demo.>
  2. <Crea el expediente desde el árbol de trámites y lo hace avanzar hasta el estado a probar.>
  3. <Pulsa el botón del evento que se prueba.>
  4. <El sistema responde: mensaje, estado resultante, efectos visibles.>
- ESC-002 — <Nombre corto>:
  1. <…>
  2. Si <condición>: <el sistema hace esto>.
  3. Si no: <el sistema hace esto otro y no hace aquello>.

# Modelos

<!-- Tabla índice: la primera fila es SIEMPRE el expediente; debajo, sus entidades hija si las hay. Cada modelo se describe a fondo en su entity-<Nombre>.md. -->

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-<Code>.md](./entity-<Code>.md) | <Code> | el expediente: <una línea> |
| [entity-<Hija>.md](./entity-<Hija>.md) | <Hija> | <una línea; p. ej. cada módulo profesional al que se renuncia> |

<Relaciones en lenguaje de negocio: p. ej. el expediente tiene N <hijas>, que se borran con él.>

# Estados y transiciones

<El ciclo de vida en dos o tres frases: de qué estado a cuáles se llega y cómo acaba.> El detalle vive en [estados.md](./estados.md).

![Diagrama de estados](./estados.png)

# Vistas

<Una línea por estado: qué ve quien tiene el turno.> El detalle vive en [vistas.md](./vistas.md).

# Documentos

<!-- Tabla índice: una fila por documento PDF. Si el trámite no genera ni recibe documentos: *(no aplica)*. -->

| Fichero | Documento | Qué es |
|---|---|---|
| [documento-<slug>.md](./documento-<slug>.md) | <Nombre> | <una línea: qué es y cuándo aparece> |

# Subsistemas utilizados

<!-- Vista de conjunto DERIVADA de las fichas TR-/RN-/FIR-: no añade información nueva; si discrepa, mandan las fichas. Elimina las filas que no apliquen. -->

| Subsistema | Dónde se usa |
|---|---|
| Documentos PDF | <RN-TR-NNN-NNN (generar <documento>)> |
| Registro de entrada | <RN-TR-NNN-NNN (presentar <documento>)> |
| Registro de salida | <RN-TR-NNN-NNN (emitir <documento>)> |
| Firma en pantalla (AutoFirma / servidor) | <FIR-<slug>-NNN> |
| Portafirmas (subsistema Firmas) | <FIR-<slug>-NNN, RN-<ESTADO>-NNN> |
| Correos | <RN-TR-NNN-NNN> |

# Recursos y datos iniciales

<Perfiles nuevos (todo perfil de la tabla que no sea CREADOR/RESPONSABLE), catálogos que los selectores necesitan, plazos configurables, certificados… Si no hay: *(no aplica)*. Este apartado es el único estado previo que los escenarios pueden presuponer.>

# Fuera de alcance

- <Cosa que el negocio decide no hacer; p. ej. el recurso de alzada ante la Dirección Territorial, o la recogida del título (otro trámite)>
