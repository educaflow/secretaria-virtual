# Modelo: CertificadoDigital

**Modelo existente:** sí

Entrada que asocia el DNI de una persona con su certificado digital (guardado como fichero, en un dispositivo criptográfico, dentro de la aplicación o en el sistema de archivos). El cambio añade la posibilidad de **deshabilitar** una entrada: una entrada deshabilitada se comporta, cuando el sistema busca el certificado de esa persona por su DNI, como si no existiera.

## Campos

- **habilitado** — indica si la entrada está activa. Una entrada deshabilitada no se usa para obtener el certificado de la persona: la búsqueda por su DNI se comporta como si la entrada no existiera.

## Acción: Crear

**Input AllowProperties:** DNI, tipo de certificado, fichero, contraseña, dispositivo criptográfico, alias, ruta dentro de la aplicación, ruta del sistema de archivos, habilitado

**Reglas de negocio:**

- RN-CertificadoDigital-001 — Al crear una entrada, si no se indica si está habilitada, la entrada se guarda habilitada
  - fase: antes_de_commit
  - condición: la interfaz no envía valor para «habilitado»

## Acción: Modificar

**Input AllowProperties:** DNI, tipo de certificado, fichero, contraseña, dispositivo criptográfico, alias, ruta dentro de la aplicación, ruta del sistema de archivos, habilitado

## Acción: Obtener el certificado de una persona por su DNI

**Validaciones:**

- VAL-CertificadoDigital-001 — La entrada del DNI buscado está habilitada; una entrada deshabilitada se comporta exactamente igual que si no existiera ninguna entrada para ese DNI
  - mensaje: el mismo que cuando no existe la entrada, indicando que no existe certificado para ese DNI
