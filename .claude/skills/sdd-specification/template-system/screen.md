# Pantalla: <Nombre>

## Identidad

- **Quién la usa:** <roles que ven o usan la pantalla, y en qué modo cada uno>
- **Qué muestra:** <qué presenta el conjunto, sobre qué modelo(s), con qué filtro en lenguaje natural y en qué modo (lectura / edición)>

## Menú

<!-- El menú que da entrada a esta pantalla: dónde cuelga en la jerarquía y quién lo ve. Si la pantalla no se abre desde un menú (p. ej. se abre desde el listado de otra pantalla), indícalo. -->

- <Ruta jerárquica> — lo ve <roles>; lleva a esta pantalla.

## Estructura jerárquica de las vistas

<!-- Casi toda pantalla se compone de VARIAS vistas que ALTERNAN listado y formulario: un LISTADO (grid) abre el FORMULARIO de detalle (al pulsar una fila o con «Nuevo»); ese formulario contiene un panel maestro-detalle que es a su vez un LISTADO de hijos, que abre el FORMULARIO del hijo, y así sucesivamente (grid → formulario → grid → formulario → …). Dibuja el árbol e indica entre paréntesis CÓMO se llega de la vista padre a la hija.
     Solo los hijos MAESTRO-DETALLE (paneles que listan los hijos que pertenecen al registro) son nodos del árbol. Los SELECTORES de un campo que referencia otra entidad (el popup para elegir un registro existente de otra entidad) NO son nodos ni llevan sección «## Vista»: esa entidad tiene su propia pantalla y aquí es solo un campo del panel.
     La estructura es siempre la misma haya una o varias vistas: si la pantalla es UNA sola vista (un formulario sin listado, o un grid suelto), el árbol es ese único nodo y abajo hay una sola sección «## Vista». -->

```
<Listado de X>
└── <Formulario de X>  (se abre al pulsar una fila o con «Nuevo»)
    └── <Listado de hijos Y>  (panel maestro-detalle «Y» del formulario de X)
        └── <Formulario de Y>  (se abre al pulsar una fila del listado de Y o con «Añadir»)
```

---

## Vista: <Nombre de la vista>

<!-- Una sección «## Vista» por cada vista del árbol, en el mismo orden. La ficha (Tipo / Qué muestra / Se abre desde) es igual para todas; las subsecciones de abajo DEPENDEN del «Tipo»: un LISTADO trae «Propiedades» de grid (columnas, orden, búsqueda…) y NO trae «Paneles»; un FORMULARIO trae «Propiedades» (modo) y «Paneles»; una GRÁFICA describe sus parámetros. Usa SOLO el bloque que corresponda al tipo, y borra los demás. «Reglas de UI» es común y va siempre la última. -->

- **Slug:** <identificador corto de la vista en kebab-case, único dentro de la pantalla: listado | formulario | listado-<hijos> | formulario-<hijo> | …>
- **Tipo:** <listado | formulario | gráfica | …>
- **Qué muestra:** <sobre qué modelo, con qué filtro en lenguaje natural y en qué modo (lectura / edición)>
- **Se abre desde:** <la vista padre y la acción que la abre, o «es la vista de entrada de la pantalla»>

<!-- ───────── Si «Tipo: listado» (grid) — un listado NO tiene paneles ───────── -->

### Propiedades

- **Columnas (en orden):** <campos de negocio visibles como columnas, en el orden en que aparecen>
- **Ordenación por defecto:** <por qué campo y en qué sentido; o «sin orden definido»>
- **Búsqueda / filtros:** <si el usuario puede filtrar y por qué campos, o «no»>
- **Al pulsar una fila abre:** <qué formulario abre, o «no abre detalle»>
- **Mensaje de ayuda (opcional):** <texto de ayuda que se muestra al usuario en el listado; omite la viñeta si no hay>

### Botones

<!-- El ÚNICO botón estándar de un listado es «Nuevo» (o «Añadir» en un hijo maestro-detalle) en la barra superior: crea un registro. Nada más es estándar.
     REGLA CRUD (fija en /k-vistas — no la contradigas): el listado NO borra ni edita en línea. Al pulsar una fila se abre el FORMULARIO, y el BORRADO y la EDICIÓN son botones del formulario, nunca del listado. MUST NOT poner un botón de fila «Eliminar», «Borrar» o «Editar» — ese borrado va en el formulario (ver la sección «Botones» del formulario y el «Modelo CRUD» del README).
     EXCEPCIONAL: cualquier OTRO botón del listado —un botón de barra superior distinto de «Nuevo», o cualquier botón de FILA/COLUMNA (Descargar, Imprimir, Ver un documento…)— NO se inventa. Solo se incluye si el usuario lo pide explícitamente, o si se le pregunta explícitamente y lo acepta.
     Si el listado solo permite crear, lista solo «Nuevo». Si tampoco permite crear, *(sin botones)*. -->

- **Nuevo** (barra superior) — <abre el formulario de alta; para quién es visible>
- **<Etiqueta>** (acción de fila) — <SOLO acción de dominio extra (nunca borrar/editar) y SOLO si el usuario la pidió o la aceptó al preguntársela>

<!-- ───────── Si «Tipo: formulario» ───────── -->

### Propiedades

- **Modo:** <cuándo es editable y cuándo solo lectura; p. ej. «editable en el alta; en detalle, solo lectura»>
- **Mensaje de ayuda (opcional):** <texto de ayuda que se muestra al usuario en el formulario; omite la viñeta si no hay>

### Paneles

<!-- Bloques visibles del formulario. Por cada panel: título, TIPO entre paréntesis y campos/contenido.
     Tipo de panel: «normal» (campos del propio modelo) | «maestro-detalle → «<vista listado hija>»» (lista los hijos y abre su formulario; es a la vez un nodo del árbol) | «botonera» (solo botones).
     Un campo que referencia otra entidad (selector) es un campo más del panel, NO una vista. Solo si la vista del selector no es la por defecto, anótala entre paréntesis. -->

- **<Título>** (normal | maestro-detalle → «<vista hija>» | botonera) — <campos o contenido, en lenguaje de negocio>

### Botones

<!-- Guardar + Cancelar + Borrar es el panel de botones ESTÁNDAR que el diseño añade SIEMPRE por /k-vistas. Por eso NO se enumeran aquí. El BORRADO vive en el formulario, nunca en el listado.
     Enumera solo: (a) botones de DOMINIO (Enviar, Emitir, Rechazar, Aprobar…) con cuándo son visibles; y (b) DESVIACIONES del estándar: «no se puede borrar» (sin Borrar), o formulario de solo lectura (sin Guardar/Borrar) — una desviación de solo lectura se refleja además en «Modo» de ### Propiedades.
     Si el formulario solo lleva el panel estándar, indícalo con: *(solo los botones estándar: Guardar, Cancelar, Borrar)*. -->

- **<Etiqueta>** — <acción de DOMINIO o DESVIACIÓN del estándar; qué dispara y cuándo es visible>

<!-- ───────── Si «Tipo: gráfica» u otra vista no-formulario ───────── -->

### Propiedades

- **Parámetros de entrada:** <qué parámetros recibe>
- **Representa:** <qué datos agrega y cómo>

<!-- ───────── Común a cualquier tipo ───────── -->

### Reglas de UI

<!-- Condiciones que cambian lo que VE o puede editar el usuario en esta vista (no bloquean operaciones ni escriben en el sistema). Si no hay, elimina esta sección. El ID es RUI-<pantalla>-<slug de esta vista>-NNN y la numeración es POR VISTA: cada vista arranca en 001. -->

- RUI-<pantalla>-<vista>-NNN — <qué ve el usuario en esta vista>
  - disparador: continuo | al crear | al cargar | al cambiar <campo>
  - condición: <opcional>
  - actor: <opcional>

<!-- Repite el bloque «## Vista» por cada vista del árbol, en el mismo orden, usando las subsecciones del tipo de cada una. -->
