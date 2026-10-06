---
type: test-e2e
id: T-022
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-022-el-supervisor-no-puede-dar-de-alta-sms.desc.md
     Iniciativa: 2026-09-30_19-35_subsistema-sms
     Test: T-022  |  Origen ESC: ESC-022
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-022 — El supervisor no puede dar de alta SMS

**Origen ESC:** ESC-022
**Verifica:** —
**Pantalla principal:** screen-sms-centro.md
**Tipo:** UI

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

1. **Dado** que el supervisor «supervisor1@mislata.es» ha iniciado sesión con contraseña «demo1234».
2. **Cuando** despliega el menú "SMS".
3. **Y** abre el menú "SMS" → "Del centro".

## Resultado esperado

- El menú "SMS" no muestra la entrada "Todos".
- El listado de "Del centro" se muestra sin el botón "Nuevo SMS".
