---
type: specification
template: system
---

# Objetivo

Registrar y enviar los SMS que la aplicación manda a las personas (un único móvil de España por SMS, con un texto que cabe en un solo SMS), por centro, y permitir consultarlos y reenviar los que fallan, igual que ya se hace con los correos. Es un **subsistema** («sms»), hermano del subsistema de correos, que otros subsistemas y los trámites usarán para avisar por SMS. Depende funcionalmente de los centros y usuarios de la aplicación y, de forma opcional, de los expedientes (para saber en qué estado de un expediente se envió cada SMS).

# Actores

- **Administrador**: ve los SMS de todos los centros, es el único que puede dar de alta un SMS a mano y puede reenviar los que han fallado.
- **Supervisor**: consulta los SMS de los centros en los que es Supervisor y reenvía los que han fallado.
- **Administrativo**: consulta los SMS de los centros en los que es Administrativo y reenvía los que han fallado.
- **Destinatario** (cualquier usuario de la aplicación): consulta los SMS que se le han enviado con éxito a su DNI.
- **Trámites y otros subsistemas de la aplicación**: dan de alta SMS automáticamente para avisar a las personas (por ejemplo, al cambiar un expediente de estado).
- **Proveedor de SMS**: servicio externo que entrega el SMS al móvil del destinatario; acepta o rechaza cada envío.

# Historias de usuario

## HU-001 — Como Administrador quiero dar de alta un SMS para avisar a una persona en su móvil

- ESC-001 — Alta correcta y envío:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS de «Alumno1» con el teléfono «+34600111222».
  6. Espera unos segundos y recarga el listado.
  7. Si el proveedor de SMS ha aceptado el envío: el SMS aparece con estado «Enviado» y con fecha de envío.
  8. Si no: el SMS aparece en el listado con estado «Fallido» y sin fecha de envío.
  9. En ese caso, el administrador pulsa sobre el SMS de «Alumno1».
  10. El sistema muestra el SMS en solo lectura y, en el panel «Datos del envío», 1 reintento y la descripción del último fallo, sin fecha de envío.
- ESC-002 — Teléfono escrito ya en formato internacional:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «+34600111222» y el mensaje «Recogida de notas el viernes».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y en el listado aparece con el teléfono «+34600111222».
- ESC-003 — Alta sin DNI del destinatario:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», deja vacío el DNI del destinatario y escribe el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema muestra «El DNI del destinatario es obligatorio» y no guarda el SMS.
- ESC-004 — Alta con DNI con la letra incorrecta:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719A», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema muestra «El DNI del destinatario no es válido; compruebe la letra» y no guarda el SMS.
- ESC-005 — Alta sin nombre ni apellidos:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», deja vacíos el nombre y los apellidos y escribe el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema muestra «El nombre es obligatorio» y «Los apellidos son obligatorios» y no guarda el SMS.
- ESC-006 — Alta sin teléfono:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», deja vacío el teléfono y escribe el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema muestra «El teléfono es obligatorio» y no guarda el SMS.
- ESC-007 — Alta con un teléfono que no es un móvil de España:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono fijo «963000000» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema muestra «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)» y no guarda el SMS.
  6. Cambia el teléfono por el número incompleto «60011» y pulsa «Guardar».
  7. El sistema muestra de nuevo «El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)» y no guarda el SMS.
- ESC-008 — Alta sin mensaje:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y deja vacío el mensaje.
  4. Pulsa «Guardar».
  5. El sistema muestra «El mensaje es obligatorio» y no guarda el SMS.
- ESC-009 — Mensaje sin acentos en el límite de un SMS:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y como mensaje la letra «a» repetida 161 veces.
  4. Pulsa «Guardar».
  5. El sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no guarda el SMS.
  6. Borra una letra para dejar el mensaje con la letra «a» repetida 160 veces y pulsa «Guardar».
  7. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS de «Alumno1» con el teléfono «+34600111222» y como mensaje la letra «a» repetida 160 veces.
- ESC-010 — Mensaje con acentos en el límite de un SMS:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y como mensaje la letra «ú» seguida de la letra «a» repetida 70 veces (71 caracteres en total).
  4. Pulsa «Guardar».
  5. El sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales» y no guarda el SMS.
  6. Borra una letra «a» para dejar el mensaje en 70 caracteres (la «ú» seguida de 69 letras «a») y pulsa «Guardar».
  7. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS de «Alumno1» con el teléfono «+34600111222» y como mensaje la letra «ú» seguida de 69 letras «a».
- ESC-011 — Alta sin centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. No elige centro y escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema muestra «El centro es obligatorio» y no guarda el SMS.
- ESC-019 — Cancelar el alta de un SMS:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. El sistema muestra el formulario con los botones «Guardar» y «Cancelar», sin el botón «Salir» y sin el panel «Datos del envío».
  4. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Alta cancelada».
  5. Pulsa «Cancelar».
  6. El sistema vuelve al listado y no aparece ningún SMS con el mensaje «Alta cancelada».

## HU-002 — Como Administrador quiero consultar los SMS de todos los centros para comprobar qué se ha enviado y qué ha fallado

- ESC-012 — Consulta de SMS de varios centros:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado.
  6. Pulsa «Nuevo SMS».
  7. Elige el centro «CIPFP Batoi», escribe el DNI del destinatario «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
  8. Pulsa «Guardar».
  9. El sistema guarda el SMS y vuelve al listado.
  10. El sistema muestra los dos SMS: «Aviso Batoi» con el centro «CIPFP Batoi» en la primera fila y «Aviso Mislata» con el centro «CIPFP Mislata» en la fila siguiente, porque el listado va del más reciente al más antiguo.
- ESC-013 — Un SMS ya creado no se puede modificar ni borrar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado.
  6. Espera unos segundos y recarga el listado.
  7. En el listado, pulsa sobre el SMS «Mañana no hay clase».
  8. El sistema muestra el SMS con todos sus datos en solo lectura, con el panel «Datos del envío» (estado, número de reintentos, fechas de creación, del primer y del último intento), y sin botón «Guardar» ni «Borrar».
- ESC-020 — Datos del envío según el estado en el detalle:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso detalle».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado.
  6. Espera unos segundos, recarga el listado y pulsa sobre el SMS «Aviso detalle».
  7. El sistema muestra el panel «Datos del envío» con 1 reintento y con las fechas de creación, del primer intento y del último intento.
  8. Si el SMS está en estado «Enviado»: el sistema muestra la fecha de envío y no muestra la descripción del último fallo.
  9. Si está en estado «Fallido»: el sistema muestra la descripción del último fallo y no muestra la fecha de envío.

## HU-003 — Como Supervisor o Administrativo quiero consultar los SMS de mis centros para saber qué avisos se han mandado a su gente

- ESC-014 — El supervisor ve solo los SMS de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
  4. Pulsa «Guardar».
  5. Pulsa «Nuevo SMS».
  6. Elige el centro «CIPFP Batoi», escribe el DNI del destinatario «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
  7. Pulsa «Guardar».
  8. El administrador cierra sesión.
  9. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú «SMS → Del centro».
  10. El sistema muestra el SMS «Aviso Mislata» y no muestra el SMS «Aviso Batoi».
  11. Pulsa sobre el SMS «Aviso Mislata».
  12. El sistema muestra en solo lectura el panel «Datos del SMS» con el centro «CIPFP Mislata», el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «+34600111222» y el mensaje «Aviso Mislata», y el panel «Datos del envío» con el estado y el número de reintentos; muestra el botón «Salir» y no muestra los botones «Guardar» ni «Borrar».
- ESC-015 — El administrativo ve los SMS de su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
  4. Pulsa «Guardar».
  5. Pulsa «Nuevo SMS».
  6. Elige el centro «CIPFP Batoi», escribe el DNI del destinatario «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
  7. Pulsa «Guardar».
  8. El administrador cierra sesión.
  9. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú «SMS → Del centro».
  10. El sistema muestra el SMS «Aviso Mislata» y no muestra el SMS «Aviso Batoi».
- ESC-021 — Supervisor de dos centros ve los SMS de ambos con su centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
  4. Pulsa «Guardar».
  5. Pulsa «Nuevo SMS».
  6. Elige el centro «CIPFP Batoi», escribe el DNI del destinatario «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
  7. Pulsa «Guardar».
  8. El administrador cierra sesión.
  9. El supervisor «supervisordoscentros@mislata.es» inicia sesión con contraseña «demo1234» y abre el menú «SMS → Del centro».
  10. El sistema muestra los dos SMS: «Aviso Mislata» con el centro «CIPFP Mislata» y «Aviso Batoi» con el centro «CIPFP Batoi».
  11. En el buscador de la columna «centro» del listado escribe «CIPFP Batoi» y pulsa Intro.
  12. El sistema muestra solo el SMS «Aviso Batoi».
- ESC-022 — El supervisor no puede dar de alta SMS:
  1. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema no le muestra el menú «SMS → Todos».
  3. Abre el menú «SMS → Del centro».
  4. El sistema muestra el listado sin el botón «Nuevo SMS».

## HU-004 — Como Supervisor, Administrativo o Administrador quiero reenviar un SMS que ha fallado para que finalmente le llegue al destinatario

- ESC-016 — Reenvío desde «Del centro»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso Mislata» con el teléfono «+34600111222».
  6. El administrador cierra sesión.
  7. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  8. Espera unos segundos y abre el menú «SMS → Del centro».
  9. Pulsa sobre el SMS «Aviso Mislata».
  10. Si el SMS está en estado «Fallido»: el sistema muestra el botón «Reenviar»; el supervisor lo pulsa y el sistema muestra «El reenvío del SMS se ha puesto en marcha.»; pasados unos segundos, al recargar, el SMS muestra 2 reintentos y está en estado «Enviado» (con fecha de envío) o de nuevo «Fallido» (con la descripción del nuevo fallo).
  11. Si el SMS está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-017 — Reenvío desde «Todos»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Batoi», escribe el DNI del destinatario «65399546N», el nombre «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso Batoi» con el teléfono «+34600333444».
  6. Espera unos segundos y recarga el listado.
  7. Pulsa sobre el SMS «Aviso Batoi».
  8. Si el SMS está en estado «Fallido»: el sistema muestra el botón «Reenviar»; el administrador lo pulsa y el sistema muestra «El reenvío del SMS se ha puesto en marcha.».
  9. Si el SMS está en estado «Enviado»: el sistema no muestra el botón «Reenviar».
- ESC-023 — Reenvío por el administrativo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso Mislata».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso Mislata» con el teléfono «+34600111222».
  6. El administrador cierra sesión.
  7. El administrativo «administrativo1@mislata.es» inicia sesión con contraseña «demo1234».
  8. Espera unos segundos y abre el menú «SMS → Del centro».
  9. Pulsa sobre el SMS «Aviso Mislata».
  10. Si el SMS está en estado «Fallido»: el sistema muestra el botón «Reenviar»; el administrativo lo pulsa y el sistema muestra «El reenvío del SMS se ha puesto en marcha.»; pasados unos segundos, al recargar, el SMS muestra 2 reintentos.
  11. Si el SMS está en estado «Enviado»: el sistema no muestra el botón «Reenviar».

## HU-005 — Como destinatario quiero consultar los SMS que me han enviado para tenerlos disponibles aunque los haya borrado del móvil

- ESC-018 — El destinatario ve sus SMS enviados:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso para Alumno1».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso para Alumno1».
  6. Pulsa «Nuevo SMS».
  7. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «03532821K», el nombre «Alumno2», los apellidos «CIPFP Mislata», el teléfono «600555666» y el mensaje «Aviso para Alumno2».
  8. Pulsa «Guardar».
  9. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso para Alumno2».
  10. Espera unos segundos, recarga el listado y anota el estado del SMS «Aviso para Alumno1».
  11. El administrador cierra sesión.
  12. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  13. Abre el menú «SMS → Recibidos».
  14. Si el SMS «Aviso para Alumno1» estaba «Enviado»: el sistema lo muestra con el teléfono «+34600111222», el mensaje «Aviso para Alumno1» y su fecha de envío.
  15. En ese caso, pulsa sobre el SMS «Aviso para Alumno1».
  16. El sistema lo abre en solo lectura.
  17. Si estaba «Fallido»: el sistema no lo muestra.
  18. En ningún caso muestra el SMS «Aviso para Alumno2».
- ESC-024 — El destinatario ve un SMS enviado desde otro centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Batoi», escribe el DNI del destinatario «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Aviso desde Batoi».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso desde Batoi» con el teléfono «+34600111222».
  6. Espera unos segundos, recarga el listado y anota el estado del SMS «Aviso desde Batoi».
  7. El administrador cierra sesión.
  8. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  9. Abre el menú «SMS → Recibidos».
  10. Si el SMS «Aviso desde Batoi» estaba «Enviado»: el sistema lo muestra con su teléfono «+34600111222», su mensaje y su fecha de envío, aunque lo envió un centro en el que el alumno no está.
  11. Si estaba «Fallido»: el sistema no lo muestra.
- ESC-025 — Un usuario sin cargo de gestión no ve SMS de los centros:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «SMS → Todos» y pulsa «Nuevo SMS».
  3. Elige el centro «CIPFP Mislata», escribe el DNI del destinatario «03532821K», el nombre «Alumno2», los apellidos «CIPFP Mislata», el teléfono «600555666» y el mensaje «Aviso para Alumno2».
  4. Pulsa «Guardar».
  5. El sistema guarda el SMS y vuelve al listado, donde aparece el SMS «Aviso para Alumno2».
  6. El administrador cierra sesión.
  7. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  8. El sistema no le muestra el menú «SMS → Todos».
  9. Abre el menú «SMS → Del centro».
  10. El sistema no muestra ningún SMS; en particular, no muestra «Aviso para Alumno2».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-Sms.md](./entity-Sms.md) | Sms | Un SMS enviado (o por enviar) a una persona, con su destinatario, su texto, su centro y el resultado del envío. |

Un Sms pertenece a un centro de la aplicación. Puede referenciar, de forma opcional, el estado de un expediente en el que se encontraba ese expediente cuando se envió el SMS; el expediente existe con independencia de sus SMS. El Sms no tiene hijos.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-sms-todos.md](./screen-sms-todos.md) | Administración de SMS | Para el Administrador: listado de los SMS de todos los centros, alta a mano, consulta del detalle y reenvío de los fallidos. |
| [screen-sms-centro.md](./screen-sms-centro.md) | SMS de mis centros | Para Supervisor y Administrativo: listado y detalle en solo lectura de los SMS de sus centros, y reenvío de los fallidos. |
| [screen-mis-sms.md](./screen-mis-sms.md) | Mis SMS | Para cualquier usuario: listado y detalle en solo lectura de los SMS enviados con éxito a su DNI. |

# Seguridad

- **Administrador:** ve los SMS de todos los centros, puede darlos de alta a mano en cualquier centro y reenviar los fallidos de cualquier centro. No puede modificarlos ni borrarlos.
- **Supervisor:** ve, en solo lectura, los SMS de los centros en los que tiene el tipo de usuario Supervisor, y puede reenviar los fallidos de esos centros. No puede crearlos, modificarlos ni borrarlos.
- **Administrativo:** ve, en solo lectura, los SMS de los centros en los que tiene el tipo de usuario Administrativo, y puede reenviar los fallidos de esos centros. No puede crearlos, modificarlos ni borrarlos.
- **Cualquier usuario (Profesor, Exprofesor, Alumno, Exalumno, Externo, Familiar y cualquier cargo):** ve, en solo lectura, solo los SMS **enviados con éxito** a su propio DNI, sea cual sea el centro que los envió.

# Recursos y datos iniciales

- Una cuenta en el proveedor de SMS con los datos de acceso y el número de teléfono desde el que se envían los SMS; se configuran por instalación y no se guardan en la aplicación como datos.

# Fuera de alcance

- Enviar un mismo SMS a varios teléfonos: cada SMS va a un único móvil.
- Enviar SMS a teléfonos fijos o de fuera de España.
- Mensajes que ocupen más de un SMS (mensajes concatenados).
- Asunto, destinatarios en copia y adjuntos (propios del correo, sin sentido en un SMS).
- Un aviso en pantalla de los caracteres que quedan mientras se escribe el mensaje: el límite solo se comprueba al guardar.
- Modificar o borrar un SMS ya creado.
- Recibir SMS o respuestas de los destinatarios.
- Reenviar en bloque todos los SMS fallidos.
- Saber si el SMS ha llegado de verdad al móvil: «Enviado» significa que el proveedor de SMS aceptó el envío.
