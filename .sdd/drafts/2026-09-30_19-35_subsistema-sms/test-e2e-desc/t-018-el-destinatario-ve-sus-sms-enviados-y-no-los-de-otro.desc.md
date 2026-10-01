---
type: test-e2e
id: T-018
---

# T-018 — El destinatario ve sus SMS enviados y no los de otro

**Origen ESC:** ESC-018
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
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso para Alumno1».
3. **Y** da de alta otro SMS del centro "CIPFP Mislata" con el DNI «03532821K», el nombre «Alumno2», los apellidos «CIPFP Mislata», el teléfono «600555666» y el mensaje «Aviso para Alumno2».
4. **Y** espera unos segundos, recarga el listado y anota el estado del SMS «Aviso para Alumno1».
5. **Y** cierra sesión.
6. **Y** el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Recibidos".

## Resultado esperado

- Si el SMS «Aviso para Alumno1» estaba en "Enviado": el listado lo muestra con el teléfono «+34600111222», el mensaje «Aviso para Alumno1» y su fecha de envío, y al pulsarlo se abre en solo lectura.
- Si estaba en "Fallido": el listado no lo muestra.
- En ningún caso el listado muestra el SMS «Aviso para Alumno2».
