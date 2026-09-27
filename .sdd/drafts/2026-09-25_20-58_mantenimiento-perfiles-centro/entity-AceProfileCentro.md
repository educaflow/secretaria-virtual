# Modelo: AceProfileCentro

**Modelo existente:** sí

Una fila dice que, **en un centro** y **sobre un trámite**, un destinatario recibe un **perfil** (Creador, Tramitador, Colaborador, Afectado, Secretario, Director o Auditor). El destinatario es un tipo de usuario, un cargo o un usuario concreto del centro. El modelo ya existe y el sistema ya lo usa para calcular los perfiles de cada usuario, pero hasta ahora nadie podía rellenarlo. Esta iniciativa no cambia sus campos: añade las reglas que tiene que cumplir cada fila y quién puede crearla, modificarla y borrarla. No tiene ciclo de vida: una fila existe mientras no se borra.

## Campos

*(sin cambios: el modelo conserva sus campos actuales — centro, trámite, perfil, tipo de usuario, cargo y usuario)*

## Restricciones

- RES-AceProfileCentro-001 — El centro está indicado
  - mensaje: "El centro es obligatorio"
- RES-AceProfileCentro-002 — El trámite está indicado
  - mensaje: "El trámite es obligatorio"
- RES-AceProfileCentro-003 — El perfil está indicado
  - mensaje: "El perfil es obligatorio"
- RES-AceProfileCentro-004 — Al menos uno de tipo de usuario, cargo o usuario está indicado
  - mensaje: "Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario"
- RES-AceProfileCentro-005 — Como mucho uno de tipo de usuario, cargo o usuario está indicado
  - mensaje: "Indica solo uno: un tipo de usuario, un cargo o un usuario"
- RES-AceProfileCentro-006 — Si hay usuario, ese usuario pertenece al centro de la fila
  - mensaje: "El usuario no pertenece al centro"
- RES-AceProfileCentro-007 — No existen dos filas con el mismo centro, trámite, perfil, tipo de usuario, cargo y usuario
  - mensaje: "Ya existe esa asignación de perfil"

## Acción: Crear

**Input AllowProperties:** centro, trámite, perfil, tipo de usuario, cargo, usuario

**Validaciones:**

- VAL-AceProfileCentro-001 — El centro elegido es uno de los centros en los que el usuario es supervisor
  - actor: Supervisor
  - mensaje: "Solo puedes gestionar perfiles de los centros de los que eres supervisor"

## Acción: Modificar

**Input AllowProperties:** trámite, perfil, tipo de usuario, cargo, usuario

**Validaciones:**

- VAL-AceProfileCentro-002 — El centro de la fila es uno de los centros en los que el usuario es supervisor
  - actor: Supervisor
  - mensaje: "Solo puedes gestionar perfiles de los centros de los que eres supervisor"

## Acción: Borrar

**Validaciones:**

- VAL-AceProfileCentro-003 — El centro de la fila es uno de los centros en los que el usuario es supervisor
  - actor: Supervisor
  - mensaje: "Solo puedes gestionar perfiles de los centros de los que eres supervisor"
