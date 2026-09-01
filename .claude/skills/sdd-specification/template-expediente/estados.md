<!-- Plantilla del CICLO DE VIDA del expediente. Un único fichero por spec, llamado `estados.md`.
     Sustituye los placeholders <…> por contenido real y ELIMINA todos los comentarios como este.
     Repite las estructuras que aparecen una sola vez (una fase, un estado, una acción, una regla)
     tantas veces como haga falta. Ver la guía README.md de esta carpeta. -->

# Ciclo de vida del expediente

## Resumen

- **Fases:** <FASE> (<título>), <FASE> (<título>) <!-- una fase es solo una agrupación de estados; no es un paso obligatorio ni una entidad que el usuario vea como tal, pero su título SÍ lo ve en la cabecera de todas las pantallas de sus estados -->
- **Estado en el que nace el expediente:** <FASE> / <ESTADO> — hay **exactamente uno** en todo el trámite.
- **Estados que cierran el expediente:** <FASE> / <ESTADO> — desde ellos ya no se puede lanzar ninguna acción.
- **Desde qué estado se puede borrar el expediente:** <FASE> / <ESTADO>, y solo el perfil <PERFIL> | *(no se puede borrar desde ningún estado)*

## Al crear el expediente

<!-- Lo que ocurre entre que el usuario pulsa «crear un expediente de este trámite» y la primera
     pantalla que ve. Alimenta la inicialización del expediente en el diseño; MUST estar siempre. -->

Al crear el expediente, antes de mostrar la primera pantalla, el sistema rellena solo:

| Dato | Con qué valor | Por qué |
|---|---|---|
| <dato del expediente> | <de dónde sale: un dato del usuario que lo crea, la fecha de hoy, el curso académico en vigor, una constante…> | <para qué se necesita ya en el arranque> |

- **Quién queda registrado como interesado y como solicitante:** <normalmente, la persona que crea el expediente; di explícitamente si es otra> <!-- REQUIRED si el trámite crea algún registro de entrada -->
- **Con qué documento de identidad se firmará en el equipo del interesado:** <el de la persona que crea el expediente | no aplica: en este trámite nadie firma en su propio equipo> <!-- REQUIRED si algún documento se firma en el equipo del interesado -->
- El expediente nace en el estado <FASE> / <ESTADO>.

---

## Fase <FASE> — <título que ve el usuario en la cabecera>

<!-- Repite este bloque `## Fase …` por cada fase, en el orden en que se recorren. -->

### Estado <ESTADO> — <título que ve el usuario>

- **Quién actúa (tiene el turno):** <PERFIL> | *(ninguno: nadie puede hacer nada aquí)*
- **Cierra el expediente:** sí | no
- **Qué consulta el usuario en este estado:** <los datos y documentos que ve, aunque no los pueda tocar>
- **Qué datos introduce el usuario en este estado:** <!-- lista cerrada, una viñeta por dato; es la base de la lista por acción de más abajo -->
  - <dato> — <qué representa y qué valores admite>
  - *(ninguno: el estado es de solo consulta)*
- **Qué acciones puede lanzar:** <ACCION>, <ACCION> | *(ninguna)*

<!-- Una subsección `#### Acción …` por cada acción disponible en este estado. -->

#### Acción <ACCION> — botón «<texto que ve el usuario>»

- **Quién la lanza:** <PERFIL>
- **Pide confirmación antes de ejecutarse:** sí, con el texto «<texto literal del aviso>» | no
- **Datos que el usuario envía al lanzarla:** <!-- CRITICAL: lista CERRADA. Un dato que no esté aquí NO se guarda,
     aunque el usuario lo haya escrito en pantalla. Ver la guía, «La lista de datos por acción es una lista de permisos». -->
  - <dato>
  - *(ninguno: la acción no recibe datos del formulario)*
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-<ESTADO>-<ACCION>-001 — <qué debe cumplirse, redactado como afirmación>
    - mensaje: "<el error literal que ve el usuario si no se cumple>"
    - condición: <cuándo se comprueba, si no es siempre> <!-- opcional -->
    - actor: <PERFIL> <!-- opcional, solo si la comprobación cambia según quién la lance -->
  - *(ninguna)*
- **Qué produce la acción, en este orden:** <!-- CRITICAL: el ORDEN es normativo; es el orden en que ocurren las cosas -->
  1. RN-001 — <qué hace el sistema automáticamente: generar un documento, firmarlo, registrarlo, avisar a alguien, limpiar un dato de un intento anterior…>
     - condición: <si solo ocurre en algunos casos> <!-- opcional -->
  2. RN-002 — <…>
  - *(nada: la acción solo cambia de estado)*
- **A qué estado lleva:**
  - <FASE> / <ESTADO> <!-- caso simple -->
  - o, si ramifica según un dato: <!-- MUST cubrir TODOS los valores posibles del dato -->
    - si <dato> vale <VALOR> → <FASE> / <ESTADO>
    - si <dato> vale <VALOR> → <FASE> / <ESTADO>
  - o: *(se queda en el mismo estado)*
  - o, para el borrado: *(el expediente desaparece)*

<!-- Si desde este estado se puede BORRAR el expediente, decláralo con esta misma estructura,
     nombrándolo en lenguaje de negocio y no como una acción técnica. Omite el bloque si no se puede borrar. -->

#### Borrado del expediente — botón «<texto del botón>»

- **Quién lo lanza:** <PERFIL>
- **Pide confirmación antes de ejecutarse:** sí, con el texto «<texto literal del aviso>»
- **Datos que el usuario envía al lanzarlo:** *(ninguno)*
- **Comprobaciones que deben pasar antes de dejarlo ejecutarse:** <VAL-…, o *(ninguna)*>
- **Qué produce:** <lo que haya que deshacer fuera del expediente, o *(nada)*>
- **A qué estado lleva:** *(el expediente desaparece)*

---

## Tabla de transiciones

<!-- Una fila por cada combinación (estado, acción) declarada arriba, más la fila de arranque,
     más la del borrado si lo hay. Es la vista de conjunto y MUST coincidir con las secciones anteriores. -->

| Estado de partida | Acción | Condición | Estado siguiente | Qué produce |
|---|---|---|---|---|
| *(el expediente se crea)* | — | — | <FASE> / <ESTADO> | <lo que se rellena al crearlo> |
| <FASE> / <ESTADO> | <ACCION> | — | <FASE> / <ESTADO> | <resumen de una línea> |
| <FASE> / <ESTADO> | <ACCION> | <dato> = <VALOR> | <FASE> / <ESTADO> | <resumen de una línea> |
| <FASE> / <ESTADO> | *(borrar el expediente)* | — | *(el expediente desaparece)* | *(nada)* |

## Datos que rellena el sistema

<!-- Los valores que nunca aporta el usuario: los calcula o los produce el sistema.
     Si el usuario pudiera enviarlos, podría falsear el expediente. Si no hay ninguno, escribe *(ninguno)*. -->

- CC-001 — <nombre del dato>
  - momento: <al crear el expediente | al lanzar la acción <ACCION> desde <ESTADO> | cada vez que se consulta el expediente>
  - sobreescribible: nunca | <PERFIL>, <PERFIL>
  - cálculo: <de dónde sale el valor, en lenguaje de negocio>
