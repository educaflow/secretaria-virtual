# Asistente «Nuevo expediente» (ventanilla)

Es **la única** forma de crear un expediente desde la interfaz.
Cualquier test E2E que necesite un expediente recién creado MUST llegar a él por aquí.
El antiguo menú «Expedientes» → «Trámites» y su ventana modal «Nuevo expediente» **ya no existen**.

## Entrada

Hay dos entradas, y **la entrada fija la forma de presentar** (`presentadoEnPapel`): dentro del asistente ya no se pregunta ni se puede cambiar.

| Menú | Grupo (`data-testid`) | Entrada (`data-testid`) | Forma de presentar | Perfil con el que se inicia |
|---|---|---|---|---|
| «Mis trámites» → «Nuevo trámite» | `item:misTramites-menuitem` | `item:misTramites-nuevoTramite-menuitem` | lo presenta el propio usuario | CREADOR |
| «Tramitación» → «Trámite en papel» | `item:tramitacion-menuitem` | `item:tramitacion-tramiteEnPapel-menuitem` | registra un trámite recibido en papel | TRAMITADOR |

Los pasos 1 y 2 solo ofrecen los centros y trámites que el usuario puede iniciar **con el perfil de la entrada elegida**, así que la misma persona puede ver listas distintas por cada entrada.
«Atrás» conserva la forma de presentar: siempre vuelve a la entrada por la que se entró.

El grupo se pliega y despliega al pulsarlo: solo se pulsa si la entrada no está visible, porque si ya venía abierto se cerraría.

```ts
const ENTRADA_NUEVO_TRAMITE = {
  misTramites: 'misTramites-nuevoTramite-menuitem',
  tramitacion: 'tramitacion-tramiteEnPapel-menuitem',
} as const;

async function abrirNuevoTramite(page: Page, grupo: keyof typeof ENTRADA_NUEVO_TRAMITE) {
  const entrada = page.getByTestId(`item:${ENTRADA_NUEVO_TRAMITE[grupo]}`);
  if (!(await entrada.isVisible())) {
    await page.getByTestId(`item:${grupo}-menuitem`).getByTestId('title').first().click();
  }
  await entrada.click();
}
```

## Las tres pantallas

Cada pantalla es una **pestaña** (no un diálogo), y al pasar a la siguiente la anterior se cierra.
Se identifican por el título de la pestaña: `page.getByRole('tab', { name: <título>, exact: true })`.

| Paso | Título de la pestaña | Qué se hace |
|---|---|---|
| 1 | `Nuevo expediente: elija el centro` | Pulsar la fila del centro. |
| 2 | `Nuevo expediente: elija el trámite` | Desplegar el tipo de trámite y pulsar la fila del trámite. |
| 3 | `Nuevo expediente` | Contestar la pregunta si aparece y pulsar «Crear expediente». |

### Paso 1 — elegir centro

Solo aparece si el usuario puede crear expedientes, con la forma de presentar de la entrada, en **más de un** centro.
- Con un solo centro se salta y se abre directamente el paso 2 con ese centro.
- Sin ningún centro se muestra el aviso «No puede crear expedientes en ninguno de sus centros» y no queda abierta ninguna pantalla.

Los centros son las filas de un grid dentro de `panel:centrosPanel`, ordenadas por nombre: `page.getByTestId('panel:centrosPanel').locator('[data-testid^="row:"]').filter({ hasText: 'CIPFP Batoi' })`.
El único botón es «Cancelar».

### Paso 2 — elegir trámite

Encima está el centro elegido, de solo lectura: `page.getByTestId('field:centro').getByRole('textbox')`.
Debajo, dentro de `panel:tramitesPanel`, un **árbol** (`treegrid`) con los trámites que el usuario puede iniciar en ese centro con la forma de presentar de la entrada, agrupados por tipo de trámite (p. ej. «Trámites para el alumno», «Trámites para el profesor»).
- Los nodos no llevan `data-testid`: se localizan por `aria-level`.
  - `[role="row"][aria-level="1"]` son los tipos de trámite, y nacen **plegados**: hay que pulsarlos para que existan sus hijos en el DOM.
  - `[role="row"][aria-level="2"]` son los trámites; pulsar uno abre el paso 3.
- El botón es «Atrás» (vuelve al paso 1) si hubo elección de centro, o «Cancelar» si el paso 1 se saltó.

```ts
const arbol = page.getByTestId('panel:tramitesPanel');
await arbol.locator('[role="row"][aria-level="1"]').filter({ hasText: 'Trámites para el alumno' }).click();
await arbol.locator('[role="row"][aria-level="2"]').filter({ hasText: 'Anulación de matrícula en ciclo formativo' }).click();
```

### Paso 3 — contexto del trámite y presentación

Panel «Trámite» (`panel:tramitePanel`), todo de solo lectura:
- `field:nombreTramite` con el nombre del trámite.
- `field:centro` con el centro.
- `field:ayudaTramite` con el texto de ayuda del trámite (solo si el trámite tiene ayuda).

Panel «Presentación» (`panel:presentacionPanel`), con **como mucho** dos preguntas (si no se hace ninguna, el panel no aparece):

| Pregunta | Campo | Cuándo | Opciones |
|---|---|---|---|
| «¿Para quién es el expediente?» | `field:presentadoEnRepresentacion` | lo decide el servidor (ver abajo) | «Para la persona que lo presenta» / «Para otra persona a la que representa quien lo presenta (hijo/a menor de edad o persona tutelada)» |
| «Idioma» | `field:idioma` | solo en papel («Tramitación») | «Castellano» / «Valencià» |

El idioma es el del expediente.
- En papel lo indica quien registra y viene ya rellenado con el idioma de ese usuario; es obligatorio.
- Por «Mis trámites» no se pregunta: el expediente toma el idioma del usuario que lo presenta.

Botones: «Atrás» (vuelve al paso 2) y «Crear expediente».

## Cuándo se hace la pregunta

La forma de presentar ya viene fijada por la entrada.
El servidor prueba, para esa forma, las dos respuestas («Para la persona que lo presenta» y «en representación») contra los permisos del usuario en ese centro y ese trámite, y **solo pregunta si las dos son válidas**; si solo vale una la rellena él sin mostrarla.
- Por «Mis trámites» (lo presenta el propio usuario), el destinatario lo deduce el tipo de usuario: un alumno que no es familiar solo puede «Para la persona que lo presenta», un familiar que no es alumno solo puede «en representación», y quien es las dos cosas tiene que elegir.
- Por «Tramitación» (en papel), en un trámite que admite representación, siempre se pregunta («Para la persona que lo presenta» significa para la persona que entregó el papel).
- Si el trámite no admite representación, no se pregunta nunca.

Casos habituales con los usuarios de demo en «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo»:

| Usuario | Entrada | «¿Para quién es el expediente?» |
|---|---|---|
| `alumno1@mislata.es` | «Mis trámites» | no (para él) |
| `familiar1@mislata.es` | «Mis trámites» | no (en representación) |
| `alumnofamiliar@mislata.es` | «Mis trámites» | sí |
| `administrativo1@mislata.es` | «Tramitación» | sí |
| `administrativo2@mislata.es` (administrativo y alumno) | «Mis trámites» | no (para él) |
| `administrativo2@mislata.es` (administrativo y alumno) | «Tramitación» | sí |

## Cómo contestar la pregunta

Axelor pinta cada opción como `<div><input type="radio"><span>texto</span></div>` sin `<label>` (y con el mismo `id` en los dos inputs), así que el radio **no** se localiza por nombre accesible sino por el `div` que contiene el texto.
En pantalla la opción `true` va **antes** que la `false`: MUST NOT localizarlas por posición.

```ts
function opcion(page: Page, texto: string) {
  return page.getByTestId('field:presentadoEnRepresentacion')
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}
```

Para comprobar que la pregunta **no** se hace, se comprueba que su campo no existe: `expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0)`.

## Resultado de «Crear expediente»

Si falta la respuesta a una pregunta visible, el cliente muestra «Debe indicar para quién es el expediente» o «Debe indicar el idioma» y la pantalla sigue abierta.
Si el servidor rechaza la combinación, muestra el título «No es posible crear el expediente» con el motivo, y la pantalla también sigue abierta.

Si todo es válido, la pestaña «Nuevo expediente» se cierra y se abre la del expediente recién creado, en el primer estado que corresponda a cómo se presentó.
- Su título es `<número>/<año>-<nombre del tipo de expediente>`, p. ej. `00016/2026-Anulación de matrícula en ciclo formativo V1`.
  - El número lo asigna el servidor: el test MUST capturarlo del título de la pestaña y usarlo para identificar (y borrar al final) su expediente, nunca suponer uno fijo.
