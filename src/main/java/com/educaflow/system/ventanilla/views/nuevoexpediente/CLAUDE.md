# Asistente «Nuevo expediente» (ventanilla)

Es **la única** forma de crear un expediente desde la interfaz.
Cualquier test E2E que necesite un expediente recién creado MUST llegar a él por aquí.
El antiguo menú «Expedientes» → «Trámites» y su ventana modal «Nuevo expediente» **ya no existen**.

## Entrada

Menú «Ventanilla» → «Nuevo expediente».
- `data-testid="item:ventanilla-menuitem"` es el grupo y `item:ventanilla-nuevoExpediente-menuitem` la entrada.
  - El grupo se pliega y despliega al pulsarlo: solo se pulsa si la entrada no está visible, porque si ya venía abierto se cerraría.

```ts
const entrada = page.getByTestId('item:ventanilla-nuevoExpediente-menuitem');
if (!(await entrada.isVisible())) {
  await page.getByTestId('item:ventanilla-menuitem').getByTestId('title').first().click();
}
await entrada.click();
```

## Las tres pantallas

Cada pantalla es una **pestaña** (no un diálogo), y al pasar a la siguiente la anterior se cierra.
Se identifican por el título de la pestaña: `page.getByRole('tab', { name: <título>, exact: true })`.

| Paso | Título de la pestaña | Qué se hace |
|---|---|---|
| 1 | `Nuevo expediente: elija el centro` | Pulsar la fila del centro. |
| 2 | `Nuevo expediente: elija el trámite` | Desplegar el tipo de trámite y pulsar la fila del trámite. |
| 3 | `Nuevo expediente` | Contestar las preguntas que aparezcan y pulsar «Crear expediente». |

### Paso 1 — elegir centro

Solo aparece si el usuario puede crear expedientes en **más de un** centro.
- Con un solo centro se salta y se abre directamente el paso 2 con ese centro.
- Sin ningún centro se muestra el aviso «No puede crear expedientes en ninguno de sus centros» y no queda abierta ninguna pantalla.

Los centros son las filas de un grid dentro de `panel:centrosPanel`, ordenadas por nombre: `page.getByTestId('panel:centrosPanel').locator('[data-testid^="row:"]').filter({ hasText: 'CIPFP Batoi' })`.
El único botón es «Cancelar».

### Paso 2 — elegir trámite

Encima está el centro elegido, de solo lectura: `page.getByTestId('field:centro').getByRole('textbox')`.
Debajo, dentro de `panel:tramitesPanel`, un **árbol** (`treegrid`) con los trámites que el usuario puede iniciar en ese centro, agrupados por tipo de trámite (p. ej. «Trámites para el alumno», «Trámites para el profesor»).
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

Panel «Presentación» (`panel:presentacionPanel`), con **como mucho** dos preguntas que el servidor decide si se hacen:

| Pregunta | Campo | Opciones |
|---|---|---|
| «¿Cómo se presenta?» | `field:presentadoEnPapel` | «Lo presento yo mismo» / «Estoy registrando un trámite recibido en papel» |
| «¿Para quién es el expediente?» | `field:presentadoEnRepresentacion` | «Para mí» / «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)» |

Botones: «Atrás» (vuelve al paso 2) y «Crear expediente».

## Cuándo se hace cada pregunta

El servidor prueba las cuatro combinaciones (forma de presentar × para quién) contra los permisos del usuario en ese centro y ese trámite, y **solo pregunta lo que tiene más de una respuesta válida**; lo que solo admite una respuesta lo rellena él sin mostrarlo.
- «¿Cómo se presenta?» se hace si el usuario puede tanto presentarlo él mismo (perfil CREADOR) como registrarlo en papel (perfil TRAMITADOR).
- «¿Para quién es el expediente?» se hace solo cuando ya se sabe la forma de presentar y, para esa forma, valen tanto «Para mí» como «en representación».
  - Si hay que contestar «¿Cómo se presenta?», nace oculta y aparece o desaparece según la respuesta.
  - Al cambiar la forma de presentar la respuesta que tuviera se borra: nunca se conserva de una forma a otra.
- Presentándolo el propio usuario, el destinatario lo deduce el tipo de usuario: un alumno que no es familiar solo puede «Para mí», un familiar que no es alumno solo puede «en representación», y quien es las dos cosas tiene que elegir.
- Registrándolo en papel, en un trámite que admite representación, siempre se pregunta «¿Para quién es el expediente?» («Para mí» significa para la persona que entregó el papel).
- Si el trámite no admite representación, «¿Para quién es el expediente?» no se pregunta nunca.

Casos habituales con los usuarios de demo en «CIPFP Mislata» y el trámite «Anulación de matrícula en ciclo formativo»:

| Usuario | «¿Cómo se presenta?» | «¿Para quién es el expediente?» |
|---|---|---|
| `alumno1@mislata.es` | no (lo presenta él) | no (para él) |
| `familiar1@mislata.es` | no (lo presenta él) | no (en representación) |
| `alumnofamiliar@mislata.es` | no (lo presenta él) | sí |
| `administrativo1@mislata.es` | no (en papel) | sí |
| `administrativo2@mislata.es` (administrativo y alumno) | sí | no si «Lo presento yo mismo» (para él); sí si «en papel» |

## Cómo contestar las preguntas

Axelor pinta cada opción como `<div><input type="radio"><span>texto</span></div>` sin `<label>` (y con el mismo `id` en los dos inputs), así que el radio **no** se localiza por nombre accesible sino por el `div` que contiene el texto.
En pantalla la opción `true` va **antes** que la `false`: MUST NOT localizarlas por posición.

```ts
function opcion(page: Page, campo: 'presentadoEnPapel' | 'presentadoEnRepresentacion', texto: string) {
  return page.getByTestId(`field:${campo}`)
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}
```

Contestar «¿Cómo se presenta?» lanza un `onChange` al servidor que recalcula si hay que preguntar «¿Para quién es el expediente?».
MUST esperarse esa respuesta antes de comprobar si la segunda pregunta aparece o no; sin la espera la aserción se hace sobre la pantalla anterior y puede pasar en falso.

```ts
const recalculo = page.waitForResponse((r) =>
  r.url().endsWith('/ws/action') &&
  (r.request().postData() ?? '').includes('AsistenteNuevoExpediente-onChange-presentadoEnPapel-action'));
await opcion(page, 'presentadoEnPapel', 'Estoy registrando un trámite recibido en papel').click();
await recalculo;
```

Para comprobar que una pregunta **no** se hace, se comprueba que su campo no existe: `expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0)`.

## Resultado de «Crear expediente»

Si falta una respuesta visible, el cliente muestra «Debe indicar cómo se presenta el expediente» o «Debe indicar para quién es el expediente» y la pantalla sigue abierta.
Si el servidor rechaza la combinación, muestra el título «No es posible crear el expediente» con el motivo, y la pantalla también sigue abierta.

Si todo es válido, la pestaña «Nuevo expediente» se cierra y se abre la del expediente recién creado, en el primer estado que corresponda a cómo se presentó.
- Su título es `<número>/<año>-<nombre del tipo de expediente>`, p. ej. `00016/2026-Anulación de matrícula en ciclo formativo V1`.
  - El número lo asigna el servidor: el test MUST capturarlo del título de la pestaña y usarlo para identificar (y borrar al final) su expediente, nunca suponer uno fijo.
