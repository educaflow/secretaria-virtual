---
type: test-e2e
id: T-010
---

# T-010 — Mensaje con acentos en el límite de un SMS

**Origen ESC:** ESC-010
**Verifica:** V-Sms-008
**Pantalla principal:** screen-sms-todos.md
**Tipo:** error

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
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y como mensaje la letra «ú» seguida de la letra «a» repetida 70 veces (71 caracteres en total).
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no guarda el SMS.
6. **Cuando** borra una letra «a» para dejar el mensaje en 70 caracteres (la «ú» seguida de 69 letras «a») y pulsa "Guardar".

## Resultado esperado

- El sistema guarda el SMS y vuelve al listado.
- El SMS aparece con el teléfono «+34600111222» y como mensaje la «ú» seguida de 69 letras «a».
