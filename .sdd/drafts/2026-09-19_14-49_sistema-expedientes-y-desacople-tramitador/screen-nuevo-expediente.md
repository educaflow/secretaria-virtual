# Pantalla: Nuevo expediente

## Identidad

- **Quién la usa:** cualquier usuario con perfil de creador o de tramitador sobre el trámite elegido en alguno de sus centros, en edición.
- **Qué muestra:** una ventana emergente, previa al alta de un expediente, sobre el modelo NuevoExpediente: enseña el trámite elegido con su ayuda y pregunta en qué centro se crea el expediente, si se presenta a partir de un documento en papel y para quién es. Solo pregunta lo que el usuario puede decidir: lo que tiene una única respuesta posible lo contesta la propia ventana. Se comporta exactamente igual que la ventana actual.

## Menú

- No cuelga de ningún menú: se abre desde la pantalla «Trámites» (menú Expedientes → Trámites), al elegir el trámite del que se quiere crear un expediente.

## Estructura jerárquica de las vistas

```
Formulario de nuevo expediente
```

---

## Vista: Formulario de nuevo expediente

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** un NuevoExpediente para el trámite elegido, en edición.
- **Se abre desde:** es la vista de entrada de la pantalla; se abre como ventana emergente desde la pantalla «Trámites», al elegir un trámite.

### Propiedades

- **Modo:** editable. No se guarda: solo sirve para crear el expediente.

### Paneles

- **Trámite** (normal) — una tarjeta con el nombre del trámite como cabecera y, debajo, su texto de ayuda; solo lectura.
- **Presentación** (normal) — centro; presentado en papel (un interruptor titulado «Presentado el expediente a partir de un documento en papel»); presentado en representación (una pregunta con dos opciones: «Para mí» y «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)»).
- **Botones** (botonera) — Cancelar y Crear expediente.

### Botones

- **Cancelar** — Cierra la ventana sin crear ningún expediente. Siempre visible.
- **Crear expediente** — Comprueba lo contestado, inicia el expediente, cierra la ventana y abre el expediente recién creado en su primer estado. Si alguna comprobación falla, muestra su mensaje y la ventana sigue abierta. Siempre visible.
- Desviación del estándar: no hay Guardar ni Borrar, ni barra de herramientas, porque el formulario no se guarda.

### Reglas de UI

- RUI-nuevo-expediente-formulario-001 — La tarjeta del trámite muestra el nombre y la ayuda del trámite elegido
  - disparador: al crear
  - condición: Siempre
- RUI-nuevo-expediente-formulario-002 — El campo centro solo ofrece los centros disponibles
  - disparador: al crear
  - condición: Siempre
- RUI-nuevo-expediente-formulario-003 — El centro aparece ya elegido con el único centro disponible
  - disparador: al crear
  - condición: hay un solo centro disponible
- RUI-nuevo-expediente-formulario-004 — El campo centro es de solo lectura
  - disparador: continuo
  - condición: hay un solo centro disponible
- RUI-nuevo-expediente-formulario-005 — El campo centro es obligatorio
  - disparador: continuo
  - condición: Siempre
- RUI-nuevo-expediente-formulario-006 — El interruptor «presentado en papel» solo se muestra cuando hay que preguntar cómo se presenta
  - disparador: continuo
  - condición: hay que preguntar cómo se presenta
- RUI-nuevo-expediente-formulario-007 — «Presentado en papel» arranca sin marcar cuando hay que preguntarlo; cuando no, toma la forma de presentar deducida del único perfil del usuario en el centro elegido: marcado si es tramitador, sin marcar si es creador. Mientras no hay ningún centro elegido (al abrirse la ventana con varios centros disponibles, o si el usuario vacía el centro) queda sin marcar
  - disparador: al crear y al cambiar el centro
  - condición: Siempre
- RUI-nuevo-expediente-formulario-008 — La respuesta a «¿para quién?» se vacía
  - disparador: al cambiar el centro
  - condición: Siempre
- RUI-nuevo-expediente-formulario-009 — La respuesta a «¿para quién?» se vacía
  - disparador: al cambiar presentado en papel
  - condición: Siempre
- RUI-nuevo-expediente-formulario-010 — La pregunta de para quién se titula «¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)» cuando es presentado en papel, y «¿Para quién es el expediente?» cuando no
  - disparador: continuo
  - condición: Siempre
- RUI-nuevo-expediente-formulario-011 — La pregunta de para quién solo se muestra cuando hay centro elegido y el usuario puede elegir entre las dos opciones
  - disparador: continuo
  - condición: hay centro elegido, se puede crear para uno mismo y se puede crear en representación
- RUI-nuevo-expediente-formulario-012 — Cuando solo cabe una de las dos opciones, «¿para quién?» queda contestada con ella sin preguntar: «para mí» si solo se puede crear para uno mismo, «para otra persona» si solo se puede crear en representación
  - disparador: al crear, al cambiar el centro y al cambiar presentado en papel
  - condición: se puede crear para uno mismo o en representación, pero no las dos cosas
- RUI-nuevo-expediente-formulario-013 — La pregunta de para quién es obligatoria
  - disparador: continuo
  - condición: la pregunta se muestra
- RUI-nuevo-expediente-formulario-014 — El trámite queda fijado con el que el usuario eligió en la pantalla «Trámites»; no se enseña como campo y el usuario no puede cambiarlo en la ventana
  - disparador: al crear
  - condición: Siempre
- RUI-nuevo-expediente-formulario-015 — En el campo centro el usuario solo puede elegir uno de los centros ofrecidos: no puede dar de alta un centro nuevo, ni modificar el centro elegido, ni abrir su ficha desde la ventana
  - disparador: continuo
  - condición: Siempre
- RUI-nuevo-expediente-formulario-016 — Cuando la ventana se abre con la pregunta de para quién a la vista, la pregunta arranca sin ninguna de las dos opciones elegida: ni «Para mí» ni «Para otra persona» aparecen preseleccionadas, de modo que el usuario tiene que contestar
  - disparador: al crear
  - condición: hay centro elegido, se puede crear para uno mismo y se puede crear en representación
- RUI-nuevo-expediente-formulario-017 — El texto de ayuda del trámite se muestra en la tarjeta con su formato (saltos de línea, texto resaltado o en color), no como texto plano con las marcas de formato a la vista
  - disparador: al crear
  - condición: Siempre
- RUI-nuevo-expediente-formulario-018 — Al pulsar «Cancelar» o cerrar la ventana, esta se cierra directamente, sin preguntar si se quieren descartar los cambios, aunque el usuario ya haya elegido centro o contestado alguna pregunta
  - disparador: al pulsar Cancelar o cerrar la ventana
  - condición: Siempre
- RUI-nuevo-expediente-formulario-019 — Cuando al pulsar «Crear expediente» alguna comprobación falla, sus mensajes se muestran juntos en un aviso de error titulado «No es posible crear el expediente», como una lista con un mensaje por línea
  - disparador: al pulsar Crear expediente
  - condición: alguna comprobación del alta falla
