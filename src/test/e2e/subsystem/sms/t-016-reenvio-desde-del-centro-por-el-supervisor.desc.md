---
type: test-e2e
id: T-016
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-016-reenvio-desde-del-centro-por-el-supervisor.desc.md
     Iniciativa: 2026-09-30_19-35_subsistema-sms
     Test: T-016  |  Origen ESC: ESC-016
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-016 — Reenvío desde «Del centro» por el supervisor

**Origen ESC:** ESC-016
**Verifica:** V-Sms-014, V-Sms-015, R-Sms-003, R-Sms-004, U-sms-centro-003
**Pantalla principal:** screen-sms-centro.md
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
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata», y comprueba en el listado que aparece con el teléfono «+34600111222».
3. **Y** cierra sesión.
4. **Y** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234», espera unos segundos y abre el menú "SMS" → "Del centro".
5. **Y** pulsa sobre el SMS «Aviso Mislata».

## Resultado esperado

- Si el SMS está en estado "Fallido": el sistema muestra el botón "Reenviar"; al pulsarlo muestra «El reenvío del SMS se ha puesto en marcha.» y, pasados unos segundos, al recargar, el SMS muestra 2 reintentos y está en "Enviado" (con fecha de envío) o de nuevo en "Fallido" (con la descripción del nuevo fallo).
- Si el SMS está en estado "Enviado": el sistema no muestra el botón "Reenviar".
