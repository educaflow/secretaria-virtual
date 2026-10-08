---
type: test-e2e
id: T-019
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-019-anadir-un-adjunto-sin-nombre-de-fichero.desc.md
     Iniciativa: 2026-10-06_15-00_subsistema-notificaciones
     Test: T-019  |  Origen ESC: ESC-019
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-019 — Añadir un adjunto sin nombre de fichero

**Origen ESC:** ESC-019
**Verifica:** V-Adjunto-002, U-notificaciones-todas-017
**Pantalla principal:** screen-notificaciones-todas.md
**Tipo:** error

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Base de datos recién reseteada con los datos de demo: **ninguna notificación precargada** (cada test crea las suyas).
- Centros «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuarios de demo con sus tipos de usuario y cargos en cada centro (tabla de abajo).
- Permisos del subsistema cargados por su `data-init` (`auth-notificaciones.xml`) y referenciados desde el grupo `users`.
- Sin configuración real de correo ni de SMS: el envío puede acabar «Enviado» o «Fallido»; por eso los tests que dependen del resultado tienen ramas «si está Enviado / si está Fallido».
- **Ningún certificado de firma en el servidor precargado** para los usuarios de demo (los certificados de demo los aporta otra iniciativa, aún no fusionada).
  Sin certificado, la justificación solo ofrece «Firmar con AutoFirma y Presentar la solicitud», que necesita el cliente de escritorio AutoFirma y no se puede pilotar desde el navegador.
  Por eso los tests que presentan una justificación dan de alta antes, desde el navegador, el **Certificado de firma del director** (valor reutilizable de abajo), y entonces aparece «Firmar y Presentar la solicitud», que firma en el servidor.
- Trámite «Justificación de falta del profesorado» disponible para el profesor «director@mislata.es» en «CIPFP Mislata».

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro | DNI |
|---|---|---|---|---|
| admin | admin | Administrador | — (todos) | — |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata | 30647328N |
| supervisordoscentros@mislata.es | demo1234 | Supervisor | CIPFP Mislata y CIPFP Batoi | 39517284H |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata | 97879650E |
| director@mislata.es | demo1234 | Profesor con cargo Director | CIPFP Mislata | 85432016B |
| jefeestudios1@mislata.es | demo1234 | Profesor con cargo Jefe de estudios | CIPFP Mislata | 15519084H |
| secretario@mislata.es | demo1234 | Profesor con cargo Secretario | CIPFP Mislata | 29050788V |
| vicesecretario@mislata.es | demo1234 | Profesor con cargo Vicesecretario | CIPFP Mislata | 56412323Q |
| profesor1@mislata.es | demo1234 | Profesor | CIPFP Mislata | 12345678Z |
| alumno1@mislata.es | demo1234 | Alumno | CIPFP Mislata | 86862719E |
| alumno2@mislata.es | demo1234 | Alumno | CIPFP Mislata | 03532821K |
| familiar1@mislata.es | demo1234 | Familiar | CIPFP Mislata | 43145636M |

**Valores de entrada reutilizables** (no son estado; los tests los citan por su nombre):

- **Correo de referencia**: centro «CIPFP Mislata», motivo «Aviso de prueba de correo», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», asunto «Reunión de inicio de curso», cuerpo «La reunión será el lunes a las 10:00.».
- **SMS de referencia**: centro «CIPFP Mislata», motivo «Aviso de prueba de SMS», DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», teléfono «600111222», mensaje «Mañana no hay clase».
- **Alta de un correo**: en «Notificaciones» → «Todas» pulsar «Nueva notificación», elegir «Correo», pulsar «Continuar», rellenar los datos indicados y pulsar «Guardar».
- **Alta de un SMS**: ídem eligiendo «SMS».
- **Certificado de firma del director**: el administrador inicia sesión, abre «Criptografía» → «Certificados digitales», pulsa nuevo, rellena el DNI «85432016B», el nombre «Director» y los apellidos «CIPFP Mislata», en «Tipo de certificado» elige «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Nueva contraseña» con «nadanada», deja el certificado habilitado, pulsa «Guardar» y cierra sesión.
- **Presentar una justificación**: con el **Certificado de firma del director** ya dado de alta en esa base de datos (si no lo está, se da de alta primero), el profesor «director@mislata.es» abre «Mis trámites» → «Nuevo trámite», elige «Justificación de falta del profesorado», crea un expediente en «CIPFP Mislata», en «Datos de la falta» elige «Un día completo», en «Fecha» pone **la fecha de hoy** (el trámite solo admite una fecha de los últimos 7 días y no futura, así que nunca una fecha fija), «Motivo falta» «Traslado de domicilio», adjunta «justificante.pdf» en «Foto o PDF del justificante», pulsa «Siguiente», pulsa «Firmar y Presentar la solicitud» (la firma en el servidor; si pide la clave del certificado, «nadanada»), confirma el aviso y anota el número del expediente, que queda en «Verificación» / «Pendiente de verificación».
  **Dependencia declarada:** si tras dar de alta el certificado sigue apareciendo solo «Firmar con AutoFirma y Presentar la solicitud», la precondición no es alcanzable desde el navegador: los tests que presentan una justificación (T-028, T-029, T-075 y T-076) dependen de la firma en el servidor y no son automatizables hasta que se fusione la iniciativa que aporta los certificados de demo; **MUST NOT** pulsarse el botón de AutoFirma.

## Precondiciones

- Estado inicial de la base de datos.

## Pasos

1. **Dado** que el administrador ha iniciado sesión y abre el alta de un correo.
2. **Cuando** en el panel «Adjuntos» pulsa «Añadir adjunto», sube «horario.pdf» y deja vacío el nombre de fichero.
3. **Y** pulsa «Guardar» en la ventana del adjunto.

## Resultado esperado

- El sistema muestra «El nombre del fichero es obligatorio» y no añade el adjunto (la ventana sigue abierta).
