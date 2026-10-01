---
type: test-e2e
id: T-021
---

# T-021 — El supervisor de dos centros ve los SMS de ambos y filtra por centro

**Origen ESC:** ESC-021
**Verifica:** —
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
2. **Cuando** da de alta desde "SMS" → "Todos" un SMS del centro "CIPFP Mislata" con el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
3. **Y** da de alta otro SMS del centro "CIPFP Batoi" con el DNI «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
4. **Y** cierra sesión.
5. **Y** el supervisor «supervisordoscentros@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú "SMS" → "Del centro".
6. **Y** escribe «CIPFP Batoi» en el buscador de la columna "centro" del listado y pulsa Intro.

## Resultado esperado

- Antes de filtrar, el listado muestra los dos SMS: «Aviso Mislata» con el centro "CIPFP Mislata" y «Aviso Batoi» con el centro "CIPFP Batoi".
- Después de filtrar, el listado muestra solo el SMS «Aviso Batoi».
