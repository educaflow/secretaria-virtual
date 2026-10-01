---
type: test-e2e
id: T-024
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-024-el-destinatario-ve-un-sms-enviado-desde-otro-centro.desc.md
     Iniciativa: 2026-09-30_19-35_subsistema-sms
     Test: T-024  |  Origen ESC: ESC-024
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-024 — El destinatario ve un SMS enviado desde otro centro

**Origen ESC:** ESC-024
**Verifica:** —
**Pantalla principal:** screen-mis-sms.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas: gestión de centro y usuarios) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Centros: «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuario administrador global, con acceso a cualquier centro.
- Cuentas de gestión de «CIPFP Mislata»: un Supervisor (`supervisor1@mislata.es`) y un Administrativo (`administrativo1@mislata.es`).
- Cuenta de Supervisor de los dos centros a la vez: `supervisordoscentros@mislata.es`.
- Cuentas de alumno con DNI, sin ningún cargo de gestión: `alumno1@mislata.es` (DNI «86862719E», de «CIPFP Mislata») y `alumno2@mislata.es` (DNI «03532821K», de «CIPFP Mislata»). El DNI «65399546N» es el del alumno de «CIPFP Batoi».
- La aplicación tiene configurada (o no) una cuenta del proveedor de SMS: las tres propiedades `sms.credentials.twilio.*` / `sms.twilio.from` son de instalación y **no** son datos de la aplicación. Ningún test depende de que estén rellenas.
- No hay ningún SMS dado de alta: cada test crea los suyos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (cualquier centro) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata |
| supervisordoscentros@mislata.es | demo1234 | Supervisor | CIPFP Mislata y CIPFP Batoi |
| alumno1@mislata.es | demo1234 | Alumno, destinatario (DNI 86862719E) | CIPFP Mislata |

## Precondiciones

- Solo el «Estado inicial de la base de datos».

## Pasos

1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Batoi", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso desde Batoi», y pulsa "Guardar".
3. **Y** comprueba que en el listado aparece con el teléfono «+34600111222».
4. **Y** espera unos segundos, recarga el listado y anota el estado del SMS «Aviso desde Batoi».
5. **Y** cierra sesión.
6. **Y** el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Recibidos".

## Resultado esperado

- Si el SMS «Aviso desde Batoi» estaba en "Enviado": el listado lo muestra con el teléfono «+34600111222», su mensaje y su fecha de envío, aunque lo haya enviado un centro en el que el alumno no está.
- Si estaba en "Fallido": el listado no lo muestra.
