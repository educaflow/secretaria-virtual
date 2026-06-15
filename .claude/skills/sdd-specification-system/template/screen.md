# Pantalla: <Nombre>

## Identidad

- **Quién la usa:** <roles que ven o usan la pantalla, y en qué modo cada uno>
- **Qué muestra:** <qué presenta, sobre qué modelo, con qué filtro en lenguaje natural y en qué modo (lectura / edición)>

## Menú

<!-- El menú que da entrada a esta pantalla: dónde cuelga en la jerarquía y quién lo ve. Si la pantalla no se abre desde un menú (p. ej. se abre desde un listado), indícalo. -->

- <Ruta jerárquica> — lo ve <roles>; lleva a esta pantalla.

## Paneles

<!-- Los bloques visibles de la pantalla, en lenguaje de negocio (sin nombres técnicos de vista). Un panel por viñeta. Para una pantalla que no es un formulario (p. ej. una gráfica), describe aquí sus parámetros de entrada y qué representa. -->

- **<Título del panel>** — <campos o contenido que agrupa, en lenguaje de negocio>

## Botones

<!-- Las acciones disponibles en la pantalla. Si no hay botones, escribe *(sin botones)*. -->

- **<Etiqueta>** — <qué acción dispara y cuándo es visible>

## Reglas de UI

<!-- Condiciones que cambian lo que VE o puede editar el usuario en este formulario (no bloquean operaciones ni escriben en el sistema). Si no hay, elimina esta sección. -->

- RUI-NNN — <qué ve el usuario en el formulario>
  - disparador: continuo | al crear | al cargar | al cambiar <campo>
  - condición: <opcional>
  - actor: <opcional>
