# Estados y transiciones

<!-- El corazón de la spec. La máquina NO se inventa estado a estado: se deriva de las fases instanciadas en el apartado «Fases» del índice (catálogo fases.md) — pero aquí se escribe COMPLETA, estado a estado y transición a transición, porque el código que se genera de ella solo sabe de estados, no de fases. Dibuja ANTES el diagrama en estados.puml y mantenlo coherente con estas fichas (si discrepan, manda el texto). Nombres de estados, eventos y perfiles en UPPER_SNAKE; todo estado lleva el prefijo de su fase (F_ENTRADA_DATOS pertenece a F_ENTRADA); cada estado lleva además su título en lenguaje natural. -->

## Estados

<!-- Exactamente UN estado inicial. Los cerrados son terminales (el expediente queda cerrado pero consultable). Un estado de espera (aguarda firmas de terceros o un plazo) no tiene perfil con el turno y su salida es una transición automática. Ningún estado no-cerrado sin salida; todo estado alcanzable desde el inicial. La columna Fase es redundante con el prefijo del nombre a propósito: es la comprobación contra el apartado «Fases» del índice. -->

| Estado | Título | Fase | Perfil con el turno | Inicial | Cerrado | ¿Se puede borrar? |
|---|---|---|---|---|---|---|
| <F_ENTRADA_DATOS> | <Datos de la solicitud> | <F_ENTRADA> | CREADOR | sí | no | sí |
| <F_SALIDA_PENDIENTE_RESOLUCION> | <Pendiente de resolución> | <F_SALIDA> | RESPONSABLE | no | no | no |
| <F_SALIDA_PENDIENTE_FIRMA> | <Pendiente de firma> | <F_SALIDA> | — | no | no | no |
| <F_TERMINADO_ACEPTADO> | <Aceptado> | <F_TERMINADO> | — | no | sí | no |

<!-- Ficha por estado que lo necesite: qué significa estar en él y, si al ENTRAR ocurre algo automáticamente (entre por la transición que entre), sus efectos RN-<ESTADO>-NNN. Si un estado no necesita ficha, no la pongas. -->

### <ESTADO>

<Qué significa estar en este estado.>

**Efectos al entrar:**

- RN-<ESTADO>-NNN — <operación automática; p. ej. poner el documento de la resolución a firmar al director en el portafirmas (FIR-<slug>-NNN)>
  - fase: antes_de_commit | después_de_commit
  - condición: <opcional>

## Creación del expediente

<Qué ocurre al crear el expediente desde el árbol de trámites: nace en <ESTADO_INICIAL> y el sistema precarga <los campos derivados del usuario que lo crea: la persona solicitante e interesada, el DNI con el que se exigirá firmar, el centro…>. La creación no recibe datos del usuario: los datos se rellenan en el estado inicial y entran al expediente con su primer evento.>

## Transición: TR-NNN — <EVENTO>: <ORIGEN> → <DESTINO>

<!-- Una ficha por transición, con este encabezado. Si el mismo evento lleva a destinos distintos según una condición, es UNA sola ficha con ramas: `TR-NNN — RESOLVER: PENDIENTE_RESOLUCION → ACEPTADO | RECHAZADO | ENTRADA_DATOS`. «Volver atrás» no existe de serie: si un estado permite volver, es una transición más. Etiquetas en este orden; omite la que no aplique. La precondición de estado NO se declara: la garantiza la máquina. -->

**Disparador:** botón<, con confirmación: "<texto de la pregunta>"> | automática — <el suceso concreto: "cuando todos los firmantes del documento <X> lo han firmado en el portafirmas", "cuando vence el plazo de resolución (silencio estimatorio)">

**Ramas:** <solo si hay más de un destino: la condición en lenguaje de negocio que decide cada uno>

**Campos editables:** <lista CERRADA de campos que el usuario puede enviar al disparar esta transición; deben existir en los «Campos» de un entity-*.md; un CC- nunca; una entidad hija se declara como "las <hijas> (alta, edición y borrado)"> | (ninguna — <motivo>)

**Validaciones:**

<!-- Empieza SIEMPRE por la obligatoriedad, campo a campo de los «Campos editables». El texto es la aserción; `condición` es la guardia (cuándo se evalúa). -->

- VAL-TR-<NNN>-NNN — <aserción que debe cumplirse; si no se da, la transición no ocurre>
  - condición: <opcional: la guardia>
  - mensaje: <opcional: el error que ve el usuario>
  - rama: <opcional: si solo aplica a una rama>

**Efectos:**

<!-- Lo que el sistema hace automáticamente al confirmarse la transición (ver catalogo-acciones-transicion.md): generar un documento, presentarlo/emitirlo por registro, firmar (referencia FIR-), enviar un correo, fijar campos… -->

- RN-TR-<NNN>-NNN — <operación automática>
  - fase: antes_de_commit | después_de_commit
  - condición: <opcional>
  - rama: <opcional>
