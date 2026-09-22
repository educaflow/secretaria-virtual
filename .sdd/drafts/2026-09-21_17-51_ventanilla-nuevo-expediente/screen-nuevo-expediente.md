# Pantalla: Nuevo expediente

## Identidad

- **Quién la usa:** cualquier usuario con sesión iniciada (todos los tipos de usuario y cargos, incluido el Administrador como un usuario más), para crear un expediente.
- **Qué muestra:** un asistente de tres pasos encadenados. Primero los centros del usuario en los que puede iniciar al menos un trámite; después, para el centro elegido, el árbol de tipos de trámite y trámites que puede iniciar en él; por último la ficha AsistenteNuevoExpediente con el trámite, su ayuda, el centro y las preguntas de cómo se presenta y para quién es. Los dos primeros pasos son de solo lectura (solo se elige); el último es editable. El primer paso solo se muestra si el usuario tiene más de un centro candidato. Si no tiene ninguno, no se abre nada y se muestra el aviso «No puede crear expedientes en ninguno de sus centros».

## Menú

- Ventanilla → Nuevo expediente — «Ventanilla» es un menú propio de primer nivel cuyo único hijo es «Nuevo expediente»; lo ve cualquier usuario con sesión iniciada; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de centros   (vista de entrada; solo se muestra si el usuario tiene más de un centro candidato)
└── Árbol de trámites   (se abre al pulsar una fila del listado de centros; es la vista de entrada si solo hay un centro candidato)
    └── Formulario de contexto del trámite   (se abre al pulsar un trámite del árbol)
```

---

## Vista: Listado de centros

- **Slug:** listado-centros
- **Tipo:** listado
- **Qué muestra:** en una ventana emergente titulada «Nuevo expediente: elija el centro», los centros a los que pertenece el usuario y en los que puede iniciar al menos un trámite, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla cuando el usuario tiene más de un centro candidato. Con un solo centro candidato no se muestra y se entra directamente al árbol de trámites con ese centro; sin ninguno no se abre nada y se muestra el aviso «No puede crear expedientes en ninguno de sus centros».

### Propiedades

- **Columnas (en orden):** nombre del centro
- **Ordenación por defecto:** por nombre del centro, ascendente
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** el árbol de trámites de ese centro

### Botones

- **Cancelar** (parte inferior de la ventana) — Cierra el asistente sin crear nada. Pedido expresamente: es el único paso, junto con el árbol cuando este es el primero, que lleva «Cancelar».

### Reglas de UI

- RUI-nuevo-expediente-listado-centros-001 — Al entrar desde el menú «Ventanilla → Nuevo expediente», si el usuario no tiene ningún centro en el que pueda iniciar al menos un trámite, no se abre ninguna vista del asistente y se muestra el aviso «No puede crear expedientes en ninguno de sus centros»
  - disparador: al cargar
  - condición: el usuario no tiene ningún centro candidato

---

## Vista: Árbol de trámites

- **Slug:** arbol-tramites
- **Tipo:** árbol de dos niveles
- **Qué muestra:** titulada «Nuevo expediente: elija el trámite» y con el nombre del centro elegido visible encima, los tipos de trámite y, dentro de cada uno, los trámites que el usuario puede iniciar en ese centro, en lectura. Un tipo de trámite que se quede sin ningún trámite no se muestra.
- **Se abre desde:** el listado de centros, al pulsar una fila; o es la vista de entrada de la pantalla cuando el usuario solo tiene un centro candidato.

### Propiedades

- **Columnas (en orden):** primer nivel, nombre del tipo de trámite; segundo nivel, nombre del trámite
- **Ordenación por defecto:** por nombre, ascendente, en los dos niveles
- **Búsqueda / filtros:** no
- **Al pulsar una fila abre:** al pulsar un trámite (segundo nivel), el formulario de contexto del trámite para ese trámite y ese centro; pulsar un tipo de trámite solo lo despliega o lo pliega

### Botones

- **Atrás** (debajo del árbol) — Vuelve al listado de centros. Solo existe si el listado de centros se mostró. Pedido expresamente.
- **Cancelar** (debajo del árbol) — Cierra el asistente sin crear nada. Solo existe si el listado de centros no se mostró (un único centro candidato). Pedido expresamente.

### Reglas de UI

- RUI-nuevo-expediente-arbol-tramites-001 — Debajo del árbol se ve un único botón: «Atrás» si antes se mostró el listado de centros, o «Cancelar» si no se mostró
  - disparador: al cargar
- RUI-nuevo-expediente-arbol-tramites-002 — Encima del árbol se ve el nombre del centro en el que se va a crear el expediente
  - disparador: al cargar
  - condición: Siempre
- RUI-nuevo-expediente-arbol-tramites-003 — Al abrirse, el árbol muestra todos los tipos de trámite ya desplegados, con sus trámites a la vista, de modo que el usuario puede pulsar un trámite sin tener que desplegar antes su tipo de trámite
  - disparador: al cargar
  - condición: Siempre

---

## Vista: Formulario de contexto del trámite

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** en una ventana emergente titulada «Nuevo expediente», la ficha AsistenteNuevoExpediente del trámite y el centro elegidos, en edición solo para las dos preguntas.
- **Se abre desde:** el árbol de trámites, al pulsar un trámite.

### Propiedades

- **Modo:** el nombre del trámite, su ayuda y el centro, siempre de solo lectura; «¿Cómo se presenta?» y «¿Para quién es el expediente?», editables cuando se muestran.

### Paneles

- **Trámite** (normal) — nombre del trámite, ayuda del trámite, centro
- **Presentación** (normal) — cómo se presenta («¿Cómo se presenta?», con las opciones «Lo presento yo mismo» y «Estoy registrando un trámite recibido en papel»), para quién es («¿Para quién es el expediente?», con las opciones «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)»)
- **Botones** (botonera) — Atrás, Crear expediente

### Botones

- **Atrás** — Vuelve al árbol de trámites del mismo centro sin crear nada. Siempre visible.
- **Crear expediente** — Comprueba que las preguntas mostradas están contestadas, comprueba en el servidor que el usuario puede crear ese expediente con esos datos y, si todo es correcto, crea el expediente, cierra el asistente y abre el expediente recién creado. Siempre visible.
- Desviación del estándar: este formulario no lleva «Guardar», «Cancelar» ni «Borrar» (la ficha no se guarda nunca).

### Reglas de UI

- RUI-nuevo-expediente-formulario-001 — Al abrirse, el centro, el trámite, el nombre del trámite y su ayuda aparecen ya rellenos con lo elegido en los pasos anteriores, y no se pueden cambiar
  - disparador: al crear
  - condición: Siempre
- RUI-nuevo-expediente-formulario-002 — «¿Cómo se presenta?» solo se muestra si el usuario puede, para ese trámite y ese centro, tanto presentarlo él mismo como registrarlo en papel
  - disparador: al crear
- RUI-nuevo-expediente-formulario-003 — Cuando «¿Cómo se presenta?» se muestra, aparece sin ninguna opción marcada y es obligatoria
  - disparador: al crear
- RUI-nuevo-expediente-formulario-004 — Cuando «¿Cómo se presenta?» no se muestra, su valor queda fijado con la única forma de presentar que el usuario tiene para ese trámite y ese centro; nunca queda sin valor
  - disparador: al crear
- RUI-nuevo-expediente-formulario-005 — «¿Para quién es el expediente?» solo se muestra si, con la forma de presentar vigente, el usuario puede elegir entre las dos opciones: el trámite admite representación y, además, o se está registrando un trámite recibido en papel, o el usuario es en ese centro a la vez del tipo de usuario al que va dirigido el trámite y Familiar, o no es ninguna de las dos cosas
  - disparador: al crear, y al cambiar «¿Cómo se presenta?»
- RUI-nuevo-expediente-formulario-006 — Cuando «¿Para quién es el expediente?» se muestra, aparece sin ninguna opción marcada y es obligatoria
  - disparador: al crear, y al cambiar «¿Cómo se presenta?»
- RUI-nuevo-expediente-formulario-007 — Cuando «¿Para quién es el expediente?» no se muestra y la forma de presentar ya está determinada, su valor queda fijado sin preguntar: «Para mí» si el trámite no admite representación o si el usuario es del tipo de usuario al que va dirigido el trámite y no es Familiar; en representación si el usuario es Familiar y no es del tipo de usuario al que va dirigido el trámite
  - disparador: al crear, y al cambiar «¿Cómo se presenta?»
- RUI-nuevo-expediente-formulario-008 — Mientras «¿Cómo se presenta?» se muestre y esté sin contestar, «¿Para quién es el expediente?» permanece oculta y sin valor
  - disparador: continuo
  - condición: «¿Cómo se presenta?» se muestra y no tiene ninguna opción marcada
- RUI-nuevo-expediente-formulario-009 — El panel «Presentación» solo se muestra cuando se muestra al menos una de sus dos preguntas; si no se muestra ninguna, el panel entero no se ve
  - disparador: continuo
  - condición: se muestra «¿Cómo se presenta?» o se muestra «¿Para quién es el expediente?»
- RUI-nuevo-expediente-formulario-010 — La ayuda del trámite solo se muestra si el trámite elegido tiene texto de ayuda; si no lo tiene, no se ve ni el campo ni un hueco vacío
  - disparador: al crear
  - condición: el trámite elegido tiene texto de ayuda
- RUI-nuevo-expediente-formulario-011 — Cuando al pulsar «Crear expediente» el servidor rechaza la creación, el motivo se muestra en un aviso titulado «No es posible crear el expediente» y el formulario sigue abierto con lo que el usuario había contestado, para que pueda corregirlo y volver a pulsar «Crear expediente»; los avisos de pregunta sin contestar («Debe indicar cómo se presenta el expediente», «Debe indicar para quién es el expediente») se muestran sin ese título y también dejan el formulario abierto
  - disparador: al terminar «Crear expediente», si ha sido rechazada
