---
type: specification
---

# Objetivo

Permitir deshabilitar una entrada de certificado digital para que, cuando el sistema busque el certificado de una persona por su DNI, se comporte como si esa entrada no existiera; así se puede anular desde la propia aplicación una entrada cuyo dispositivo criptográfico falla, sin parar la aplicación. Es una modificación del **subsistema** de criptografía; no depende de ningún otro subsistema.

**Modifica:** subsystem/criptografia

# Actores

- **Administrador**: gestiona las entradas de certificados digitales de toda la aplicación desde la administración de la secretaría virtual.

# Historias de usuario

## HU-001 — Como Administrador quiero deshabilitar una entrada de certificado digital para que la aplicación deje de usarla sin tener que borrarla

- ESC-001 — Alta con «Habilitado» marcado por defecto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. El sistema muestra el formulario de alta con la casilla «Habilitado» marcada.
  4. Rellena el DNI «85432016B», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del del WAR», rellena la ruta «firma/mi_certificado.p12» y la contraseña «nadanada».
  5. Pulsa «Guardar».
  6. El sistema guarda la entrada y vuelve al listado, donde la fila del DNI «85432016B» aparece con «Habilitado» marcado.
- ESC-002 — Deshabilitar una entrada:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Rellena el DNI «85432016B», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del del WAR», rellena la ruta «firma/mi_certificado.p12» y la contraseña «nadanada».
  4. Pulsa «Guardar».
  5. El sistema guarda la entrada y vuelve al listado.
  6. Abre la fila del DNI «85432016B» y desmarca la casilla «Habilitado».
  7. Pulsa «Guardar».
  8. El sistema guarda el cambio y el listado muestra la fila del DNI «85432016B» con «Habilitado» sin marcar.
- ESC-003 — Modificar una entrada sin tocar «Habilitado» la mantiene habilitada:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Rellena el DNI «85432016B», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del del WAR», rellena la ruta «firma/mi_certificado.p12» y la contraseña «nadanada».
  4. Pulsa «Guardar».
  5. El sistema guarda la entrada y vuelve al listado.
  6. Abre la fila del DNI «85432016B» y cambia la contraseña a «otraclave».
  7. Pulsa «Guardar».
  8. El sistema guarda el cambio y el listado muestra la fila del DNI «85432016B» con «Habilitado» marcado.
- ESC-004 — Alta con «Habilitado» desmarcado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Rellena el DNI «85432016B», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del del WAR», rellena la ruta «firma/mi_certificado.p12» y la contraseña «nadanada», y desmarca la casilla «Habilitado».
  4. Pulsa «Guardar».
  5. El sistema guarda la entrada y vuelve al listado, donde la fila del DNI «85432016B» aparece con «Habilitado» sin marcar.
- ESC-005 — Volver a habilitar una entrada deshabilitada:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Rellena el DNI «85432016B», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del del WAR», rellena la ruta «firma/mi_certificado.p12» y la contraseña «nadanada».
  4. Pulsa «Guardar».
  5. El sistema guarda la entrada y vuelve al listado.
  6. Abre la fila del DNI «85432016B» y desmarca la casilla «Habilitado».
  7. Pulsa «Guardar».
  8. El sistema guarda el cambio y vuelve al listado.
  9. Abre de nuevo la fila del DNI «85432016B»; el formulario muestra la casilla «Habilitado» sin marcar.
  10. Marca la casilla «Habilitado».
  11. Pulsa «Guardar».
  12. El sistema guarda el cambio y el listado muestra la fila del DNI «85432016B» con «Habilitado» marcado.
- ESC-006 — Borrar una entrada deshabilitada sigue funcionando:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Administración SV» → «Certificados digitales» y pulsa «Añadir certificado digital».
  3. Rellena el DNI «85432016B», elige el tipo de certificado «Usar un fichero con el certificado que ya está dentro del del WAR», rellena la ruta «firma/mi_certificado.p12» y la contraseña «nadanada».
  4. Pulsa «Guardar».
  5. El sistema guarda la entrada y vuelve al listado.
  6. Abre la fila del DNI «85432016B» y desmarca la casilla «Habilitado».
  7. Pulsa «Guardar».
  8. El sistema guarda el cambio y vuelve al listado.
  9. Abre la fila del DNI «85432016B» y pulsa «Borrar».
  10. El sistema pide confirmar el borrado y el administrador confirma.
  11. El sistema borra la entrada y el listado ya no muestra ninguna fila con el DNI «85432016B».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-CertificadoDigital.md](./entity-CertificadoDigital.md) | CertificadoDigital | Entrada que asocia el DNI de una persona con su certificado digital |

Modelo existente que se modifica: sus relaciones actuales (con los dispositivos criptográficos y sus alias) no cambian.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-certificados-digitales.md](./screen-certificados-digitales.md) | Certificados digitales | Mantenimiento de las entradas de certificados digitales, para el Administrador |

# Seguridad

- **Administrador:** ve, crea, edita y borra las entradas de certificados digitales de toda la aplicación (la pantalla es de administración global, no por centro). Sin cambios respecto al acceso actual.

# Recursos y datos iniciales

- El certificado de ejemplo que ya viene dentro de la aplicación (ruta «firma/mi_certificado.p12», contraseña «nadanada»), usado por los escenarios para dar de alta entradas de prueba.

# Fuera de alcance

- Deshabilitar dispositivos criptográficos completos o sus alias: solo se deshabilitan entradas individuales de certificado digital.
- Cambiar los procesos de firma o sellado que consumen los certificados: siguen funcionando igual; simplemente una entrada deshabilitada se comporta para ellos como inexistente.
