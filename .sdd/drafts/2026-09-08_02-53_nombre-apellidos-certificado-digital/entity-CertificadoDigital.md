# Modelo: CertificadoDigital

**Modelo existente:** sí

Un certificado digital custodiado por la aplicación para firmar en el servidor en nombre de la persona cuyo DNI figura en él. Este delta añade el **nombre y los apellidos** del titular, solo orientativos (sirven para saber de quién es el certificado; no se usan para firmar ni para nada más), y cambia la identidad del certificado: el **DNI deja de ser único**, de modo que una persona puede tener varios certificados dados de alta, pero **solo uno de ellos habilitado a la vez**. Cuando la firma en servidor busca el certificado de una persona por su DNI, obtiene el certificado habilitado de ese DNI; si ninguno está habilitado (o no hay ninguno), se comporta como si la persona no tuviera certificado, igual que hoy con una única entrada deshabilitada. El DNI se fija al crear el certificado y ya no se puede cambiar. Los certificados dados de alta antes de este cambio quedan con nombre y apellidos vacíos y se consideran escritos por el administrador (editables), que deberá rellenarlos al modificarlos.

## Campos

- **nombre** — nombre de pila de la persona titular del certificado. Si al crear el certificado existe un usuario de la aplicación cuyo documento es ese DNI, se copia de su ficha; si no, lo escribe el administrador.
- **apellidos** — apellidos de la persona titular del certificado. Se obtienen igual que el nombre.
- **nombre tomado del usuario** — indica si el nombre y los apellidos se copiaron de la ficha de un usuario al crear el certificado (y por tanto no se pueden cambiar) o los escribió el administrador (y por tanto puede corregirlos).

## Restricciones

- RES-CertificadoDigital-001 — Para un mismo DNI solo puede haber un certificado habilitado; puede haber cualquier número de certificados deshabilitados con ese DNI. Si se intenta guardar un certificado habilitado cuando ya existe otro habilitado con el mismo DNI, el sistema lo rechaza con el mensaje «Ya existe un certificado digital habilitado para el DNI <DNI>».
- RES-CertificadoDigital-002 — El DNI ya no es único: varios certificados pueden tener el mismo DNI (se retira la unicidad actual del DNI y su mensaje «Ya existe un certificado digital con el DNI …»).

## Campos calculados

- CC-CertificadoDigital-001 — nombre tomado del usuario
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: al crear el certificado vale «sí» si existe un usuario de la aplicación cuyo documento es el DNI del certificado y «no» en caso contrario; después no cambia nunca.

## Acción: Crear

**Input AllowProperties:** DNI, tipo de certificado, fichero, contraseña, dispositivo criptográfico, alias, ruta classpath, ruta sistema de archivos, habilitado, nombre, apellidos

**Validaciones:**

- VAL-CertificadoDigital-001 — El nombre está indicado
  - condición: no existe ningún usuario de la aplicación cuyo documento sea el DNI del certificado (si existe, el nombre lo pone el sistema y esta validación no aplica)
  - mensaje: «El nombre es obligatorio»
- VAL-CertificadoDigital-002 — Los apellidos están indicados
  - condición: no existe ningún usuario de la aplicación cuyo documento sea el DNI del certificado (si existe, los apellidos los pone el sistema y esta validación no aplica)
  - mensaje: «Los apellidos son obligatorios»

**Reglas de negocio:**

- RN-CertificadoDigital-001 — Si existe un usuario de la aplicación cuyo documento es el DNI del certificado, el sistema fija el nombre y los apellidos con los de la ficha de ese usuario, ignorando lo que llegue del formulario, y marca «nombre tomado del usuario» como «sí»
  - fase: antes_de_commit
- RN-CertificadoDigital-002 — Si no existe ningún usuario con ese DNI, el sistema conserva el nombre y los apellidos escritos por el administrador y marca «nombre tomado del usuario» como «no»
  - fase: antes_de_commit

## Acción: Modificar

**Input AllowProperties:** tipo de certificado, fichero, contraseña, dispositivo criptográfico, alias, ruta classpath, ruta sistema de archivos, habilitado, nombre, apellidos

**Validaciones:**

- VAL-CertificadoDigital-003 — El nombre está indicado
  - condición: el certificado tiene «nombre tomado del usuario» a «no» (incluidos los certificados anteriores a este cambio)
  - mensaje: «El nombre es obligatorio»
- VAL-CertificadoDigital-004 — Los apellidos están indicados
  - condición: el certificado tiene «nombre tomado del usuario» a «no» (incluidos los certificados anteriores a este cambio)
  - mensaje: «Los apellidos son obligatorios»

**Reglas de negocio:**

- RN-CertificadoDigital-003 — El DNI no cambia: el sistema conserva siempre el DNI guardado, ignorando cualquier valor que llegue del formulario
  - fase: antes_de_commit
- RN-CertificadoDigital-004 — Si el certificado tiene «nombre tomado del usuario» a «sí», el sistema conserva el nombre y los apellidos guardados, ignorando lo que llegue del formulario
  - fase: antes_de_commit
