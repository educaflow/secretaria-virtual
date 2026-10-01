---
type: test-e2e
id: T-020
---

# T-020 — Datos del envío según el estado, en el detalle

**Origen ESC:** ESC-020
**Verifica:** R-Sms-004, R-Sms-005, R-Sms-006, U-sms-todos-003, U-sms-todos-004
**Pantalla principal:** screen-sms-todos.md
**Tipo:** UI

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
2. **Cuando** abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata", escribe el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso detalle», y pulsa "Guardar".
3. **Y** espera unos segundos, recarga el listado y pulsa sobre el SMS «Aviso detalle».

## Resultado esperado

- El panel "Datos del envío" muestra 1 reintento y las fechas de creación, del primer intento de envío y del último intento de envío.
- Si el SMS está en estado "Enviado": muestra la fecha de envío y no muestra la descripción del último fallo.
- Si está en estado "Fallido": muestra la descripción del último fallo y no muestra la fecha de envío.
