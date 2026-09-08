---
type: specification
template: system
---

# Objetivo

Ampliar el subsistema de certificados digitales para que cada certificado muestre el **nombre y apellidos** de la persona a la que pertenece y para que una misma persona (un mismo DNI) pueda tener **varios certificados dados de alta, de los que solo uno puede estar habilitado a la vez**. Es una modificación de un **subsistema** existente. La firma en servidor (bandeja de firmas y trámites) sigue obteniendo el certificado de una persona por su DNI: a partir de ahora usa el certificado habilitado de ese DNI y, si no hay ninguno habilitado, se comporta como si la persona no tuviera certificado.

**Modifica:** subsystem/criptografia

# Actores

- **Administrador**: mantiene los certificados digitales de todas las personas (alta, modificación, habilitación y borrado). Es el único que usa la pantalla.
- **Persona titular del certificado**: la persona cuyo DNI figura en el certificado. Puede ser un usuario de la aplicación (y entonces su nombre y apellidos se toman de su ficha de usuario) o no serlo (y entonces los escribe el administrador).

# Historias de usuario

## HU-001 — Como Administrador quiero que cada certificado digital lleve el nombre y apellidos de su titular para saber de quién es cada certificado sin tener que buscar el DNI

- ESC-001 — Alta con el DNI de un usuario de la aplicación:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Escribe en DNI «29050788V» (el documento del usuario «secretario@mislata.es»).
  4. El sistema rellena automáticamente el nombre con «Secretario» y los apellidos con «CIPFP Mislata», y ambos quedan de solo lectura.
  5. Elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del WAR» y escribe en la ruta classpath «firma/mi_certificado.p12».
  6. Pulsa «Guardar».
  7. El sistema guarda el certificado y vuelve al listado, donde aparece una fila con DNI «29050788V», nombre «Secretario», apellidos «CIPFP Mislata», ese tipo de certificado y habilitado.
- ESC-002 — Alta con un DNI que no es de ningún usuario:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Escribe en DNI «12345678Z» (no corresponde a ningún usuario de la aplicación).
  4. El sistema deja el nombre y los apellidos vacíos y editables.
  5. Escribe en nombre «Ana» y en apellidos «García López».
  6. Elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del WAR» y escribe en la ruta classpath «firma/mi_certificado.p12».
  7. Pulsa «Guardar».
  8. El sistema guarda el certificado y vuelve al listado, donde aparece una fila con DNI «12345678Z», nombre «Ana», apellidos «García López», ese tipo de certificado y habilitado.
- ESC-003 — Alta sin nombre o sin apellidos cuando los escribe el administrador:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Escribe en DNI «12345678Z», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del WAR» y escribe en la ruta classpath «firma/mi_certificado.p12».
  4. Si deja el nombre vacío y escribe en apellidos «García López» y pulsa «Guardar»: el sistema muestra el error «El nombre es obligatorio» y no guarda nada.
  5. Si escribe en nombre «Ana», deja los apellidos vacíos y pulsa «Guardar»: el sistema muestra el error «Los apellidos son obligatorios» y no guarda nada.
- ESC-004 — Corregir el nombre escrito a mano:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «12345678Z», en nombre «Ana», en apellidos «García López», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. En el listado pulsa la fila del DNI «12345678Z».
  4. El sistema abre el formulario con el DNI de solo lectura y el nombre y los apellidos editables.
  5. Cambia los apellidos a «García Pérez» y pulsa «Guardar».
  6. El sistema guarda el cambio y vuelve al listado, donde la fila del DNI «12345678Z» muestra apellidos «García Pérez».
- ESC-005 — El nombre tomado del usuario no se puede cambiar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. En el listado pulsa la fila del DNI «29050788V».
  4. El sistema abre el formulario con el DNI, el nombre «Secretario» y los apellidos «CIPFP Mislata» de solo lectura; el resto de campos siguen editables.

## HU-002 — Como Administrador quiero dar de alta varios certificados para un mismo DNI, con solo uno habilitado a la vez, para preparar el certificado de sustitución (por ejemplo ante una caducidad) sin perder el que está en uso

- ESC-006 — Segundo certificado del mismo DNI, deshabilitado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. Pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/instalar_certificado_criptografico/secretario.p12» y desmarca «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda el certificado y vuelve al listado, donde aparecen dos filas con DNI «29050788V», ambas con nombre «Secretario» y apellidos «CIPFP Mislata»: primero la habilitada (ruta «firma/mi_certificado.p12») y después la deshabilitada.
- ESC-007 — Segundo certificado del mismo DNI, habilitado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. Pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/instalar_certificado_criptografico/secretario.p12» y deja «Habilitado» marcado (viene marcado por defecto).
  4. Pulsa «Guardar».
  5. El sistema muestra el error «Ya existe un certificado digital habilitado para el DNI 29050788V» y no guarda nada; en el listado sigue habiendo una sola fila para ese DNI.
- ESC-008 — Habilitar un certificado cuando otro del mismo DNI ya está habilitado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. Pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/instalar_certificado_criptografico/secretario.p12», desmarca «Habilitado» y pulsa «Guardar».
  4. En el listado pulsa la fila deshabilitada del DNI «29050788V», marca «Habilitado» y pulsa «Guardar».
  5. El sistema muestra el error «Ya existe un certificado digital habilitado para el DNI 29050788V» y no guarda nada; en el listado la fila de «firma/mi_certificado.p12» sigue habilitada y la otra deshabilitada.
- ESC-009 — Cambiar el certificado vigente de una persona:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. Pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/instalar_certificado_criptografico/secretario.p12», desmarca «Habilitado» y pulsa «Guardar».
  4. En el listado pulsa la fila habilitada del DNI «29050788V» (ruta «firma/mi_certificado.p12»), desmarca «Habilitado» y pulsa «Guardar».
  5. El sistema guarda el cambio y vuelve al listado, donde las dos filas del DNI «29050788V» aparecen deshabilitadas.
  6. Pulsa la fila del DNI «29050788V» con ruta «firma/instalar_certificado_criptografico/secretario.p12», marca «Habilitado» y pulsa «Guardar».
  7. El sistema guarda el cambio y vuelve al listado, donde la fila de «firma/instalar_certificado_criptografico/secretario.p12» aparece primero y habilitada, y la de «firma/mi_certificado.p12» después y deshabilitada.
- ESC-010 — Borrar uno de los certificados de una persona:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales», pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/mi_certificado.p12» y pulsa «Guardar».
  3. Pulsa «Añadir certificado digital», escribe en DNI «29050788V», elige el tipo «Usar un fichero con el certificado que ya está dentro del WAR», escribe en la ruta classpath «firma/instalar_certificado_criptografico/secretario.p12», desmarca «Habilitado» y pulsa «Guardar».
  4. En el listado pulsa la fila deshabilitada del DNI «29050788V» y pulsa «Borrar».
  5. El sistema borra ese certificado y vuelve al listado, donde queda una sola fila con DNI «29050788V», habilitada, con ruta «firma/mi_certificado.p12».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-CertificadoDigital.md](./entity-CertificadoDigital.md) | CertificadoDigital | Un certificado digital custodiado por la aplicación para firmar en el servidor en nombre de una persona (modelo existente; solo se declara lo que cambia). |

El certificado digital no pertenece a ningún centro. Referencia de forma indirecta, por el DNI, a la persona titular: si en el momento del alta existe un usuario de la aplicación con ese documento, el nombre y los apellidos se copian de su ficha; no queda ningún enlace vivo con el usuario y un cambio posterior en su ficha no afecta al certificado. Varios certificados pueden compartir el mismo DNI.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-certificados-digitales.md](./screen-certificados-digitales.md) | Certificados digitales | Mantenimiento de los certificados digitales por el Administrador (pantalla existente; solo se declara lo que cambia). |

# Seguridad

- **Administrador:** ve, crea, modifica y borra certificados digitales de cualquier persona, sin restricción por centro (los certificados no pertenecen a ningún centro).

# Recursos y datos iniciales

- Los ficheros de certificado que ya van dentro de la aplicación y que los escenarios usan como certificado de tipo «Usar un fichero con el certificado que ya está dentro del WAR»: «firma/mi_certificado.p12» y «firma/instalar_certificado_criptografico/secretario.p12».
- Los usuarios y centros de demostración; en particular el usuario «secretario@mislata.es» con documento «29050788V», nombre «Secretario» y apellidos «CIPFP Mislata».

# Fuera de alcance

- Rellenar el nombre y los apellidos de los certificados dados de alta antes de este cambio: se quedan vacíos y, cuando el administrador modifique uno de ellos, deberá escribirlos a mano (se tratan como escritos por el administrador).
- Mantener sincronizados el nombre y los apellidos con la ficha del usuario después del alta: son solo orientativos y se guardan una vez.
- Desmarcar automáticamente «Habilitado» al crear un segundo certificado para un DNI que ya tiene uno habilitado: el valor por defecto sigue siendo «habilitado» y el sistema rechaza el guardado si ya hay otro habilitado.
- Cambiar el flujo de firma en servidor (bandeja de firmas y trámites): solo cambia qué certificado se obtiene para un DNI, no cómo se firma.
