# Modelo: NuevoExpediente

Lo que el usuario contesta en la ventana «Nuevo expediente» antes de empezar a tramitar —en qué centro crea el expediente, si lo presenta a partir de un documento en papel y para quién es—, junto con los datos que la ventana necesita para saber qué preguntarle y qué no.
Es un modelo de pantalla: **no se guarda** y solo existe mientras la ventana está abierta.
Al pulsar «Crear expediente», lo contestado se convierte en un ContextoTramitacion, que es lo que recibe el tramitador; el tramitador no conoce este modelo.
Sustituye, con otro nombre, al papel de pantalla que hasta ahora hacía ContextoTramitacion.

## Campos

- **trámite** — el trámite del que se va a crear el expediente; es el que el usuario eligió en la pantalla «Trámites» antes de abrirse la ventana.
- **centro** — el centro en el que se crea el expediente, elegido entre los centros disponibles.
- **presentado en papel** — si el expediente se registra a partir de una solicitud que alguien entregó en papel en el centro (sí) o se presenta telemáticamente (no).
- **presentado en representación** — para quién es el expediente: «para mí» (no) o «para otra persona a la que represento» (sí). Mientras el usuario no contesta, no tiene valor.

## Campos calculados

- CC-NuevoExpediente-001 — nombre del trámite
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el nombre del trámite elegido, en el idioma del usuario
- CC-NuevoExpediente-002 — ayuda del trámite
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el texto de ayuda del trámite elegido
- CC-NuevoExpediente-003 — centros disponibles
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: los centros del usuario en los que tiene, sobre el trámite elegido, el perfil de creador o el de tramitador, ordenados por nombre
- CC-NuevoExpediente-004 — hay que preguntar cómo se presenta
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: sí cuando, en el centro elegido, el usuario tiene a la vez el perfil de creador y el de tramitador sobre el trámite; no en cualquier otro caso, incluido que no haya centro elegido
- CC-NuevoExpediente-005 — se puede crear para uno mismo
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: sí cuando, en el centro elegido, el usuario tiene sobre el trámite el perfil que corresponde a la forma de presentar (tramitador si es en papel, creador si no); no si no hay centro elegido
- CC-NuevoExpediente-006 — se puede crear en representación
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: sí cuando se cumple lo mismo que para crear para uno mismo y, además, el trámite admite presentar en representación de otra persona
- CC-NuevoExpediente-007 — forma de presentar deducida
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: cuando no hay que preguntar cómo se presenta, «en papel» si el único perfil que el usuario tiene sobre el trámite en el centro elegido es el de tramitador, y «telemáticamente» si es el de creador; sin valor si no hay centro elegido o si hay que preguntarlo

## Acción: Preparar

Es lo que ocurre al abrirse la ventana para el trámite elegido.

**Input AllowProperties:** trámite

**Validaciones:**

- VAL-NuevoExpediente-001 — El usuario tiene al menos un centro disponible para el trámite
  - mensaje: "No puede crear expedientes de este trámite en ninguno de sus centros"

## Acción: Recalcular

Es lo que ocurre cada vez que el usuario cambia el centro o «presentado en papel» con la ventana abierta: los campos calculados que dependen de ellos («hay que preguntar cómo se presenta», «se puede crear para uno mismo», «se puede crear en representación» y «forma de presentar deducida») se vuelven a calcular con el centro y la forma de presentar que hay en ese momento en la ventana. No crea ni guarda nada.

**Input AllowProperties:** trámite, centro, presentado en papel

## Acción: Crear

Las comprobaciones que bloquean el alta son las de la acción «Iniciar expediente» de ContextoTramitacion: la ventana las solicita al pulsar «Crear expediente» para mostrar sus mensajes, y el tramitador las vuelve a hacer al iniciar el expediente.

**Input AllowProperties:** trámite, centro, presentado en papel, presentado en representación

**Reglas de negocio:**

- RN-NuevoExpediente-001 — Se inicia un expediente con un ContextoTramitacion formado por el trámite, el centro elegido, el perfil que corresponde a la forma de presentar (tramitador si es presentado en papel, creador si no) y el «para quién» contestado. El perfil lo decide el sistema: nunca se acepta el que pudiera enviar la ventana
  - fase: antes_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — el modelo no se guarda: no existe nada que modificar después de cerrarse la ventana)
