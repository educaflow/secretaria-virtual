---
type: test-e2e
id: T-001
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-001-alta-de-un-sms-y-resultado-del-envio.desc.md
     Iniciativa: 2026-09-30_19-35_subsistema-sms
     Test: T-001  |  Origen ESC: ESC-001
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-001 — Alta de un SMS y resultado del envío

**Origen ESC:** ESC-001
**Verifica:** V-Sms-001, V-Sms-003, V-Sms-004, V-Sms-005, V-Sms-007, V-Sms-009, R-Sms-001, R-Sms-002, R-Sms-003, R-Sms-004, R-Sms-005, R-Sms-006, U-sms-todos-002, U-sms-todos-008
**Pantalla principal:** screen-sms-todos.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas: gestión de centro y usuarios) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Centros: «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuario administrador global, con acceso a cualquier centro.
- Cuentas de gestión de «CIPFP Mislata»: un Supervisor (`supervisor1@mislata.es`) y un Administrativo (`administrativo1@mislata.es`).
- Cuenta de Supervisor de los dos centros a la vez: `supervisordoscentros@mislata.es`.
- Cuentas de alumno con DNI, sin ningún cargo de gestión: `alumno1@mislata.es` (DNI «95591733F», de «CIPFP Mislata») y `alumno2@mislata.es` (DNI «99024353S», de «CIPFP Mislata»). El DNI «92898219T» es el del alumno de «CIPFP Batoi».
- La aplicación tiene configurada (o no) una cuenta del proveedor de SMS: las tres propiedades `sms.credentials.twilio.*` / `sms.twilio.from` son de instalación y **no** son datos de la aplicación. Ningún test depende de que estén rellenas.
- No hay ningún SMS dado de alta: cada test crea los suyos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (cualquier centro) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata |
| supervisordoscentros@mislata.es | demo1234 | Supervisor | CIPFP Mislata y CIPFP Batoi |
| alumno1@mislata.es | demo1234 | Alumno, destinatario (DNI 95591733F) | CIPFP Mislata |

## Precondiciones

- Solo el «Estado inicial de la base de datos».

## Pasos

1. **Dado** que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
3. **Y** elige el centro "CIPFP Mislata", escribe el DNI del destinatario «95591733F», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema guarda el SMS y vuelve al listado, donde aparece el SMS de «Alumno1» con el teléfono «+34600111222».
6. **Y** espera unos segundos y recarga el listado.
7. **Y** pulsa sobre el SMS de «Alumno1».

## Resultado esperado

- En el listado, el SMS aparece con el teléfono «+34600111222» (normalizado a formato internacional aunque se escribiera sin prefijo).
- Tras la espera, el SMS ya no está en estado "Pendiente": está en "Enviado" con fecha de envío, o en "Fallido" sin fecha de envío.
- Al abrir el detalle, el sistema lo muestra en solo lectura con el panel "Datos del envío" y 1 reintento; si está "Fallido", con la descripción del último fallo y sin fecha de envío.
