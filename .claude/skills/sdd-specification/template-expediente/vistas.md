# Vistas

<!-- UN único fichero para todas las vistas del expediente. NO hay listados ni menús que especificar (los da la plataforma: árbol de trámites, bandejas, cabecera con estado e historial, panel de errores, botón «Salir»). Primero el almacén de paneles; luego una sección por estado, en el orden de la tabla de estados.md. Por cada estado: la vista del perfil con el turno (si lo tiene) y la vista genérica (lo que ven los demás perfiles con acceso, todo en lectura). Toda combinación alcanzable necesita su vista. -->

## Paneles

<!-- Los paneles se declaran UNA sola vez y las vistas los referencian, cada una con su modo (edición | lectura). Nombre corto kebab-case en negrita (es su identificador dentro del fichero), tipo entre paréntesis y, tras un «—», su contenido en lenguaje de negocio. Tipos: normal | visor de documento | maestro-detalle | ayuda. Si la versión de lectura de un panel necesita mostrar OTRA cosa que la de edición (menos campos, otra disposición conceptual), declara dos paneles (p. ej. <nombre> y <nombre>-resumen); si basta "lo mismo sin editar", es el mismo panel en modo lectura. -->

- **<datos-solicitud>** (normal) — <campos que agrupa, en lenguaje de negocio>
- **<visor-solicitud>** (visor de documento) — muestra embebido <el PDF de la solicitud> (ver documento-<slug>)
- **<lineas-X>** (maestro-detalle) — lista <las hijas> del expediente y abre el formulario de alta/edición de cada una, que pide: <campos del formulario de la hija>
- **<ayuda-X>** (ayuda) — <texto informativo que muestra>

## Estado: <ESTADO>

### Vista del perfil <PERFIL>

<!-- La vista de quien puede actuar: sus paneles (los suyos editables, los del pasado en lectura) y los botones de sus eventos. El botón de borrar (si el estado lo permite según estados.md) es estándar y no se enumera; solo se declaran los botones de eventos. Un botón que firma en pantalla lo dice y referencia su FIR-. -->

- **Paneles (en orden):** <panel> (edición), <panel> (lectura), …
- **Botones:**
  - **<Etiqueta visible>** — dispara TR-NNN<; firma <el documento X> (FIR-<slug>-NNN)>
- **Mensaje de ayuda (opcional):** <texto de ayuda de la vista; omite la viñeta si no hay>

<!-- Pasos (solo si la vista es un asistente por pasos — fases.md §6): sub-pantallas dentro del MISMO estado; cambiar de paso no cambia el estado, no queda en el historial y no persiste nada. Exactamente un paso inicial. Cada botón es de navegación (→ otro paso, sin TR-) o de salida (→ dispara un evento del estado, es su TR- normal). En ese caso, sustituye la viñeta «Botones» por esta tabla, y muestra/oculta los pasos con las Reglas de UI de esta vista. Si la vista no tiene pasos, elimina el bloque. -->

- **Pasos:**

  | Paso | Contenido | Botones |
  |---|---|---|
  | PASO_<INICIO> (inicial) | <paneles o contenido que muestra> | «<Etiqueta>» → evento <EVENTO> (TR-NNN); «<Etiqueta>» → paso PASO_<OTRO> |
  | PASO_<OTRO> | <p. ej. la caja de texto del motivo> | «Atrás» → paso PASO_<INICIO>; «<Etiqueta>» → evento <EVENTO> (TR-NNN) |

#### Reglas de UI

<!-- Condiciones que cambian lo que VE el usuario en esta vista (no bloquean ni escriben): mostrar/ocultar según valores (las horas solo si la jornada es parcial; el "especificar" solo si la circunstancia es OTRAS), marcas visuales de obligatorio espejo de una VAL-TR-, ayudas condicionadas (el aviso de aportar justificante según el motivo elegido), valores por defecto. Numeración POR VISTA desde 001. Si no hay, elimina la subsección. -->

- RUI-<ESTADO>-<PERFIL>-NNN — <qué ve el usuario>
  - disparador: continuo | al crear | al cargar | al cambiar <campo>
  - condición: <opcional>
  - actor: <opcional>

### Vista genérica

<!-- Lo que ve cualquier otro perfil con acceso: todo en lectura. El botón «Salir» es de la plataforma y no se declara. Un estado sin perfil con el turno (cerrado, de espera) solo tiene esta vista. -->

- **Paneles (en orden):** <panel> (lectura), …
- **Mensaje de ayuda (opcional):** <…>

#### Reglas de UI

- RUI-<ESTADO>-GENERAL-NNN — <qué ve el usuario>
  - disparador: continuo | al cargar
  - condición: <opcional>

<!-- Repite la sección «## Estado» por cada estado de la tabla de estados.md, en el mismo orden. -->
