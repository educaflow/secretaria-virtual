# Ciclo de vida del expediente

Este fichero es un **delta**: declara solo lo que la modificación cambia del ciclo de vida ya implementado. Todo lo que no se menciona aquí se conserva exactamente como está.

## Resumen

- **Fases:** RECEPCION (Recepción), TRAMITACION (Tramitación) — *(sin cambios)*
- **Estado en el que nace el expediente:** RECEPCION / ENTRADA_DATOS — *(sin cambios)*
- **Estados que cierran el expediente:** TRAMITACION / ACEPTADO, TRAMITACION / RECHAZADO — *(sin cambios)*
- **Desde qué estado se puede borrar el expediente:** RECEPCION / ENTRADA_DATOS, y solo el perfil CREADOR — *(sin cambios)*
- **Qué cambia esta modificación:** únicamente el estado RECEPCION / PENDIENTE_PRESENTACION y su acción PRESENTAR. Ningún otro estado, acción ni transición se toca.

## Al crear el expediente

*(sin cambios)* — lo que el sistema rellena al crear el expediente se conserva tal cual.

- **Quién queda registrado como interesado y como solicitante:** la persona que crea el expediente, es decir el propio profesor que falta. *(sin cambios)*
- **Con qué documento de identidad se firmará la solicitud:** el de la persona que crea el expediente. *(sin cambios)* — lo que esta modificación añade es que ese mismo documento de identidad es, además, el que decide **cómo** se firmará: si la secretaría virtual custodia un certificado digital habilitado para él, la firma la pone el servidor; si no, la pone el profesor en su equipo.
- El expediente nace en el estado RECEPCION / ENTRADA_DATOS. *(sin cambios)*

---

## Fase RECEPCION — Recepción

### Estado ENTRADA_DATOS — Entrada de datos

*(sin cambios)* — este estado, sus datos, sus acciones y su borrado se conservan tal cual.

### Estado PENDIENTE_PRESENTACION — Pendiente de presentación

- **Quién actúa (tiene el turno):** CREADOR — *(sin cambios)*
- **Cierra el expediente:** no — *(sin cambios)*
- **Qué consulta el usuario en este estado:** la solicitud que el sistema ha generado con los datos de la falta. **Nuevo:** además, un aviso que le dice cómo se va a firmar esa solicitud —en su propio equipo o en el servidor— y, cuando el servidor necesita que él aporte la clave del certificado, se lo pide.
- **Qué datos introduce el usuario en este estado:**
  - **Nuevo** — la clave de su certificado digital: la contraseña del fichero de certificado o el PIN de su dispositivo criptográfico. Solo se pide cuando corresponde firmar en el servidor y la secretaría virtual **no** tiene guardada esa clave. Es un dato de un solo uso: no se guarda en el expediente ni en ningún otro sitio.
  - *(el resto de datos del estado se conserva: en este estado el profesor no rellena ningún otro dato de la falta)*
- **Qué acciones puede lanzar:** BACK, PRESENTAR — *(sin cambios en cuáles son)*

#### Acción BACK — botón «Atrás»

Devuelve el expediente a RECEPCION / ENTRADA_DATOS sin comprobar nada. *(sin cambios salvo lo siguiente)*

- **Datos que el usuario envía al lanzarla:** *(ninguno)* — *(sin cambios)*
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:** *(ninguna)* — *(sin cambios)*
- **Qué produce la acción, en este orden:**
  1. RN-007 — **Nuevo.** El sistema descarta la clave del certificado digital que el profesor hubiera tecleado antes de devolver el expediente al estado anterior: no se guarda, no se devuelve a la pantalla y no se escribe en ninguna traza. Es la regla que sostiene lo que comprueba ESC-005; que el campo aparezca vacío al volver (RUI-PENDIENTE_PRESENTACION-CREADOR-009) es solo lo que el usuario ve, y una regla de pantalla nunca es una defensa.
- **A qué estado lleva:** RECEPCION / ENTRADA_DATOS — *(sin cambios)*

#### Acción PRESENTAR — botón «Firmar y Presentar la solicitud» o botón «Firmar con AutoFirma y Presentar la solicitud»

Es **una sola acción** con **dos botones excluyentes**: el sistema muestra uno u otro según cómo corresponda firmar, y el profesor no elige.

- **Quién la lanza:** CREADOR — *(sin cambios)*
- **Pide confirmación antes de ejecutarse:** sí, con el texto «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción» — *(sin cambios)*
- **Datos que el usuario envía al lanzarla:**
  - la solicitud firmada — **excepción declarada**: es un documento que produce el sistema, pero entra en la lista porque cuando la firma se pone en el equipo del propio profesor este es el único sitio donde puede llegar al servidor y comprobarse. *(conservado)*
  - **Nuevo** — la clave de su certificado digital (la contraseña del fichero o el PIN del dispositivo). Solo se usa cuando corresponde firmar en el servidor y la secretaría virtual no guarda esa clave; nunca se guarda ni se devuelve a la pantalla.
- **Comprobaciones que deben pasar antes de dejarla ejecutarse:**
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-001 — El profesor tiene un documento de identidad con el que firmar.
    - mensaje: "No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador."
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-002 — La clave del certificado está rellena.
    - mensaje: "La contraseña es obligatoria"
    - condición: solo cuando corresponde firmar en el servidor con un certificado guardado en un fichero cuya contraseña **no** custodia la secretaría virtual.
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-003 — El PIN del dispositivo criptográfico está relleno.
    - mensaje: "El PIN es obligatorio"
    - condición: solo cuando corresponde firmar en el servidor con un certificado que está en un dispositivo criptográfico cuyo PIN **no** custodia la secretaría virtual.
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-004 — La contraseña abre el certificado del profesor.
    - mensaje: "La contraseña indicada no es correcta"
    - condición: solo cuando corresponde firmar en el servidor con un certificado guardado en un fichero cuya contraseña **no** custodia la secretaría virtual.
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-005 — La contraseña que custodia la secretaría virtual abre el certificado del profesor.
    - mensaje: "La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador"
    - condición: solo cuando corresponde firmar en el servidor con un certificado guardado en un fichero cuya contraseña **sí** custodia la secretaría virtual. Se distingue de la anterior porque este error el profesor no lo puede corregir por sí mismo.
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-006 — La solicitud está firmada. *(conservado, con una guardia nueva)*
    - condición: solo cuando corresponde firmar en el equipo del propio profesor. Cuando corresponde firmar en el servidor, la solicitud firmada todavía no existe en el momento de comprobar, porque la produce la propia acción.
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-007 — La firma de la solicitud es válida, es una sola, la ha puesto un certificado de confianza, no ha alterado el documento y corresponde al documento de identidad del profesor. *(conservado, con una guardia nueva)*
    - condición: solo cuando corresponde firmar en el equipo del propio profesor, por el mismo motivo que la anterior.
  - VAL-PENDIENTE_PRESENTACION-PRESENTAR-008 — Quien lanza la acción es el propio profesor que creó el expediente, es decir aquel a cuyo documento de identidad corresponde la firma.
    - mensaje: "Solo puede presentar la solicitud la persona que la firma"
    - actor: CREADOR
    - Es una comprobación **nueva y necesaria**: hasta ahora firmar exigía tener el certificado en el propio equipo, así que nadie podía firmar por otro aunque llegase a lanzar la acción. Con la firma en el servidor esa barrera desaparece, porque la firma se produce sola a partir del documento de identidad guardado en el expediente.

  Las comprobaciones VAL-PENDIENTE_PRESENTACION-PRESENTAR-004 y VAL-PENDIENTE_PRESENTACION-PRESENTAR-005 se hacen **solo** con los certificados guardados en un fichero. Con un certificado que está en un dispositivo criptográfico no se comprueba la clave por anticipado, y es a propósito: la única forma de saber si un PIN es correcto es intentar abrir el dispositivo con él, y los intentos fallidos bloquean la tarjeta. En ese caso el error lo da la propia firma (RN-002).

  Todas las comprobaciones se hacen contra la **situación real en el servidor en el momento de presentar**, nunca contra lo que la pantalla tuviera pintado. Si a alguien le deshabilitan el certificado mientras tiene el expediente abierto, la acción se cancela con el mensaje que corresponda y no presenta nada.

- **Qué produce la acción, en este orden:**
  1. RN-001 — **Nuevo.** El sistema firma la solicitud en el servidor con el certificado digital habilitado que custodia para el documento de identidad del profesor, y **descarta** cualquier solicitud firmada que el formulario hubiera enviado.
     - condición: solo cuando corresponde firmar en el servidor, es decir cuando existe un certificado digital habilitado para el documento de identidad del profesor.
  2. RN-002 — **Nuevo.** Si la firma en el servidor falla, la acción se cancela entera: el sistema muestra un error que explica el motivo, no presenta nada, no deja constancia de ninguna entrada y el expediente se queda donde estaba, para que el profesor pueda volver a intentarlo. Cuando el motivo es una clave que el profesor ha tecleado, el error se lo dice para que la corrija; cuando es una clave que custodia la secretaría virtual, se le remite al administrador.
     - condición: solo cuando corresponde firmar en el servidor.
  3. RN-003 — Deja constancia oficial de la entrada de la documentación, con la solicitud firmada como documento principal y el justificante como anexo. *(conservado)*
  4. RN-004 — Guarda en el expediente el justificante de esa entrada. *(conservado)*
  5. RN-005 — Limpia el motivo de disconformidad y el motivo del rechazo de intentos anteriores. *(conservado)*
  6. RN-006 — **Nuevo.** El sistema descarta la clave que el profesor haya tecleado en cuanto termina la acción, tanto si la solicitud se ha presentado como si la acción se ha cancelado por una comprobación o por un fallo de firma. La clave no se guarda, no se devuelve a la pantalla y no se escribe en ninguna traza.
- **A qué estado lleva:** TRAMITACION / PENDIENTE_RESOLUCION — *(sin cambios)*

---

## Fase TRAMITACION — Tramitación

*(sin cambios)* — ninguno de sus estados, acciones ni transiciones se toca.

---

## Tabla de transiciones

Solo las filas que esta modificación toca. El resto de la tabla del trámite se conserva tal cual.

| Estado de partida | Acción | Condición | Estado siguiente | Qué produce |
|---|---|---|---|---|
| RECEPCION / PENDIENTE_PRESENTACION | PRESENTAR | existe un certificado digital habilitado para el documento de identidad del profesor | TRAMITACION / PENDIENTE_RESOLUCION | El servidor firma la solicitud, deja constancia de la entrada, guarda su justificante, limpia los motivos anteriores y descarta la clave tecleada |
| RECEPCION / PENDIENTE_PRESENTACION | PRESENTAR | no existe ningún certificado digital habilitado para el documento de identidad del profesor | TRAMITACION / PENDIENTE_RESOLUCION | La solicitud llega ya firmada desde el equipo del profesor; el sistema deja constancia de la entrada, guarda su justificante y limpia los motivos anteriores |
| RECEPCION / PENDIENTE_PRESENTACION | BACK | — | RECEPCION / ENTRADA_DATOS | El sistema descarta la clave del certificado que el profesor hubiera tecleado |

## Datos que rellena el sistema

- CC-001 — **Nuevo.** Cómo se va a firmar la solicitud (la situación de firma del profesor).
  - momento: cada vez que se consulta el expediente
  - sobreescribible: nunca
  - cálculo: a partir del documento de identidad con el que se firma la solicitud y del certificado digital **habilitado** que la secretaría virtual custodie para ese documento. Resulta una de estas seis situaciones: el profesor no tiene documento de identidad; no hay ningún certificado para él; su certificado está en un dispositivo criptográfico cuyo PIN custodia la secretaría virtual; está en un dispositivo cuyo PIN no custodia; está en un fichero cuya contraseña custodia; o está en un fichero cuya contraseña no custodia.
- CC-002 — La solicitud firmada.
  - momento: al lanzar la acción PRESENTAR desde RECEPCION / PENDIENTE_PRESENTACION
  - sobreescribible: solo el CREADOR, y **solo** cuando corresponde firmar en el equipo del propio profesor; cuando corresponde firmar en el servidor, el valor lo pone el servidor y lo que envíe el formulario se descarta.
  - cálculo: la solicitud generada, con la firma del profesor estampada en el mismo recuadro y la misma página de siempre. La pone el servidor con el certificado custodiado, o el propio profesor desde su equipo, según la situación de CC-001.
- CC-003 — El justificante del registro de entrada. *(conservado)*
- CC-004 — **Nuevo.** La clave con la que se abre el certificado digital al firmar en el servidor.
  - momento: al lanzar la acción PRESENTAR desde RECEPCION / PENDIENTE_PRESENTACION
  - sobreescribible: solo el CREADOR, y **solo** cuando corresponde firmar en el servidor y la secretaría virtual **no** custodia la clave del certificado. En las demás situaciones —el profesor sin documento de identidad, sin certificado custodiado, o con la clave también custodiada— el valor lo pone la secretaría virtual y lo que envíe el formulario se descarta.
  - cálculo: la contraseña (o el PIN) que la secretaría virtual custodia junto al certificado digital habilitado del profesor, cuando la custodia; y solo si no la custodia, la que el profesor teclea en la pantalla. Cuál de las dos se usa lo decide la situación de firma de CC-001. La clave custodiada gana siempre a la tecleada.
