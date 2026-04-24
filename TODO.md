# Tareas pendientes — Seguridad

Análisis realizado el 2026-04-13. Revisar y corregir los siguientes problemas.

## Severidad MEDIA

[ ] 1. Código de verificación en logs en texto plano
      Fichero: subsystem/registro_usuario/service/impl/RegistroServiceImpl.java:209
      Problema: logger.info loguea el código de 6 dígitos junto al email.
      Fix: eliminar el código del log, dejar solo el email.

[ ] 2. Sin rate limiting en endpoints públicos de registro
      Fichero: subsystem/registro_usuario/controllers/RegistroController.java:46-93
      Problema: /ws/public/registro/* permiten fuerza bruta ilimitada (1M combinaciones para código 6 dígitos).
      Fix: añadir limitación de intentos por IP (máx 5/min para validarCodigo, máx 10/hora para iniciar registro).

[ ] 3. Token de registro expuesto en body JSON
      Fichero: subsystem/registro_usuario/controllers/RegistroController.java:62
      Problema: se devuelve {"token": "..."} en el body en lugar de una cookie segura.
      Fix: usar cookie HttpOnly + Secure en lugar de JSON.

[ ] 4. Sin validación de longitud/formato en nombre, apellidos, idioma (paso 2/3 del registro)
      Fichero: subsystem/registro_usuario/controllers/RegistroController.java:104-112
      Problema: campos aceptan valores vacíos o de longitud arbitraria.
      Fix: validar longitud mínima/máxima y valores permitidos para idioma.

[ ] 5. NullPointerException si el centro no existe en importación
      Fichero: system/gestion/centro/services/impl/CentroImporterServiceImpl.java:52-53
      Problema: .fetchOne().getId() explota con NPE si el centro no existe, devuelve 500.
      Fix: comprobar null antes de llamar .getId() y devolver error descriptivo.

[ ] 6. Sin validación de permisos en el controlador de importación de centro
      Fichero: system/gestion/centro/controllers/CentroImporterController.java:35
      Problema: los permisos solo se controlan en la vista, no en el controlador.
      Fix: verificar en el controlador que el usuario es ADMINISTRADOR del centro antes de importar.

[ ] 7. Posible XSS en mensaje de error con código XML del centro
      Fichero: system/gestion/centro/services/impl/CentroImporterServiceImpl.java:76-96
      Problema: el valor codigoXml (procedente del fichero subido) se concatena sin escapar en el mensaje de error.
      Fix: escapar caracteres especiales (<, >, &) o no incluir el valor en el mensaje.

## Pendiente de investigar

[ ] Comportamiento del atributo `prompt` en botones dentro de `panel-dashlet`
      Vista: system/gestion/centro/views/CambioCurso.xml
      Síntoma: en algún momento al cancelar el prompt no se recalculaban los tipos de usuario
      pero sí se cambiaba el curso. Se resolvió solo (posiblemente al corregir el bulk SQL
      en CentroUsuarioTipoUsuarioRepository). Verificar que el cancel del prompt previene
      completamente la ejecución de la acción `cambiarCurso`.

## Severidad BAJA

[ ] 8. Validación de email con regex demasiado permisivo
      Fichero: subsystem/registro_usuario/service/impl/RegistroServiceImpl.java:62
      Fix: usar Apache Commons EmailValidator o similar.

[ ] 9. getCentroCode() puede ser null sin validación previa
      Fichero: system/gestion/centro/controllers/CentroImporterController.java:35
      Fix: validar que centroCode no es null ni blank antes de pasarlo al servicio.

[ ] 10. NullPointerException si tipoUsuario no existe en importación
       Fichero: system/gestion/centro/services/impl/CentroImporterServiceImpl.java:62-63
       Fix: comprobar null tras fetchOne() y registrar error descriptivo.

[ ] 11. Sin validación de longitud del token en el repositorio
       Fichero: subsystem/registro_usuario/db/repo/RegistroPendienteRepository.java:25-29
       Fix: validar en el servicio que el token tiene la longitud esperada antes de consultar.

[ ] 12. Password no se normaliza (sin trim) a diferencia del resto de campos
       Fichero: subsystem/registro_usuario/controllers/RegistroController.java:108-109
       Fix: rechazar passwords con espacios al inicio o al final.

[] 13. Crear EmailUtil para centralizar validación y envío de emails, evitando duplicación de código en RegistroServiceImpl y CentroImporterServiceImpl.