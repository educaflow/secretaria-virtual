# Modelo: ContextoTramitacion

**Modelo existente:** sí

Los datos mínimos con los que el tramitador inicia un expediente: de qué trámite, en qué centro, con qué perfil y si se presenta en representación de otra persona.
Hasta ahora este modelo era a la vez la ventana «Nuevo expediente», de modo que el tramitador dependía de una pantalla.
Deja de serlo: pasa a ser únicamente la entrada del tramitador, y la ventana tiene su propio modelo (NuevoExpediente), que se convierte en un ContextoTramitacion al crear.
Así un expediente puede iniciarse también sin pasar por la ventana, y las comprobaciones de quién puede iniciar qué se hacen siempre, entre por donde entre la petición.
Sigue sin guardarse: son datos de paso.

Qué cambia:

- Sus campos quedan reducidos a cuatro: **trámite**, **centro**, **perfil** y **presentado en representación**. Los cuatro existían ya.
- Se retiran **nombre del trámite** y **ayuda del trámite**, que eran solo de la ventana (pasan a NuevoExpediente).
- Se retira **presentado en papel**: se deduce del perfil (ver RN-ContextoTramitacion-001), así que no puede llegar un dato incoherente con él.
- El perfil solo admite dos de sus valores para iniciar un expediente: **creador** y **tramitador**.

## Acción: Iniciar expediente

**Input AllowProperties:** trámite, centro, perfil, presentado en representación

**Validaciones:**

- VAL-ContextoTramitacion-001 — El centro está indicado
  - mensaje: "Debe indicar el centro"
- VAL-ContextoTramitacion-002 — Quien inicia el expediente tiene, sobre ese trámite y en ese centro, el perfil de creador o el de tramitador
  - mensaje: "No puede crear expedientes de este trámite en el centro indicado"
- VAL-ContextoTramitacion-003 — El perfil indicado es creador o tramitador, y es uno de los que quien inicia el expediente tiene sobre ese trámite en ese centro
  - mensaje: "No puede presentar el expediente de esa forma en el centro indicado"
- VAL-ContextoTramitacion-004 — Está indicado si el expediente se presenta en representación de otra persona o no
  - mensaje: "Debe indicar para quién es el expediente"
- VAL-ContextoTramitacion-005 — No se presenta en representación de otra persona si el trámite no lo admite
  - mensaje: "Este trámite no permite presentar la solicitud en representación de otra persona"

**Reglas de negocio:**

- RN-ContextoTramitacion-001 — El expediente que se crea queda marcado como presentado en papel cuando el perfil es tramitador, y como presentado telemáticamente cuando es creador
  - fase: antes_de_commit

## Acción: Crear

**Input AllowProperties:** (ninguna — el modelo no se guarda: son datos de paso para iniciar un expediente)

## Acción: Modificar

**Input AllowProperties:** (ninguna — el modelo no se guarda: son datos de paso para iniciar un expediente)
