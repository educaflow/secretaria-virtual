# Modelo: AsistenteNuevoExpediente

La ficha que el usuario tiene delante en el último paso del asistente «Nuevo expediente». Reúne lo que hace falta para crear un expediente: el centro elegido en el primer paso, el trámite elegido en el árbol, cómo se presenta y para quién es. No se guarda nunca: nace al entrar en el último paso y desaparece cuando se crea el expediente o el usuario abandona el asistente; por eso no se puede dar de alta ni modificar como un registro, y su única acción es «Crear expediente». No tiene ciclo de vida.

«Cómo se presenta» admite dos valores: lo presenta el propio usuario, o el usuario está registrando un trámite recibido en papel. «Para quién es» admite dos valores: para el propio usuario, o en representación de otra persona (hijo/a menor de edad o persona tutelada).

## Campos

- **centro** — el centro en el que se creará el expediente; es el elegido en el primer paso (o el único centro candidato del usuario).
- **trámite** — el trámite elegido en el árbol, del que se creará el expediente.
- **nombre del trámite** — el nombre del trámite elegido, para mostrarlo.
- **ayuda del trámite** — el texto explicativo del trámite elegido, para mostrarlo.
- **cómo se presenta** — si lo presenta el propio usuario o si está registrando un trámite recibido en papel.
- **para quién es** — si el expediente es para el propio usuario o en representación de otra persona.

## Campos calculados

- CC-AsistenteNuevoExpediente-001 — nombre del trámite
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el nombre del trámite elegido
- CC-AsistenteNuevoExpediente-002 — ayuda del trámite
  - momento: lectura
  - sobreescribible: nunca
  - cálculo: el texto de ayuda del trámite elegido

## Acción: Crear

**Input AllowProperties:** (ninguna — la ficha no se guarda nunca; no existe un alta de este modelo)

## Acción: Modificar

**Input AllowProperties:** (ninguna — la ficha no se guarda nunca; no existe una modificación de este modelo)

## Acción: Crear expediente

**Input AllowProperties:** centro, trámite, cómo se presenta, para quién es

**Validaciones:**

- VAL-AsistenteNuevoExpediente-001 — El centro está indicado
  - mensaje: "Debe indicar el centro"
- VAL-AsistenteNuevoExpediente-003 — Cómo se presenta está indicado
  - mensaje: "Debe indicar cómo se presenta el expediente"
- VAL-AsistenteNuevoExpediente-004 — Para quién es está indicado
  - mensaje: "Debe indicar para quién es el expediente"
- VAL-AsistenteNuevoExpediente-005 — El usuario pertenece al centro indicado y puede iniciar en él ese trámite
  - mensaje: "No puede crear expedientes de este trámite en el centro indicado"
- VAL-AsistenteNuevoExpediente-006 — El usuario puede iniciar ese trámite en ese centro con la forma de presentar indicada (presentarlo él mismo o registrarlo en papel)
  - mensaje: "No puede presentar el expediente de esa forma en el centro indicado"
- VAL-AsistenteNuevoExpediente-007 — Si el expediente es en representación de otra persona, el trámite admite presentarse en representación
  - mensaje: "Este trámite no permite presentar la solicitud en representación de otra persona"
- VAL-AsistenteNuevoExpediente-008 — Si el expediente es para el propio usuario y lo presenta él mismo en un trámite que admite representación, el usuario es, en ese centro, del tipo de usuario al que va dirigido el trámite, o no es Familiar
  - mensaje: "No puede crear este expediente para usted mismo en el centro indicado"
- VAL-AsistenteNuevoExpediente-009 — Si el expediente es en representación de otra persona y lo presenta el propio usuario, el usuario es Familiar en ese centro, o no es del tipo de usuario al que va dirigido el trámite
  - mensaje: "No puede crear este expediente en representación de otra persona en el centro indicado"

**Reglas de negocio:**

- RN-AsistenteNuevoExpediente-001 — Crear el expediente del trámite en el centro indicado, con la forma de presentar y el «para quién es» resultantes, y abrirlo en su primer estado cerrando el asistente
  - fase: antes_de_commit
