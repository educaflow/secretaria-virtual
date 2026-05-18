## ¿Para qué sirve esto?

Permite cargar masivamente usuarios autorizados desde un fichero XML o CSV. La importación se ejecuta sincrónicamente dentro de `insert()` del servicio: el fichero se procesa, se persiste el resultado y el log queda guardado en la misma `TareaImportacion`.

## Lo no obvio

- **La importación ocurre dentro de `insert()`**, no en un job asíncrono ni en un endpoint separado. Si falla, el servicio captura la `ImportadorException` y guarda el error en `log` con `estado = false` — el registro se persiste igualmente.
- **`ImportadorUsuarioXML` no está implementado**: lanza `ImportadorException("@TODO: Importación no implementada todavía")`. Cubre los tipos `PROFESOR`, `ALUMNO` y `FAMILIAR`.
- **`ImportadorUsuarioCSV` sí está implementado**: procesa un fichero de una columna donde cada línea es un DNI. Valida el DNI con `DniUtil`, busca duplicados por `(centro, dni, tipoUsuario, curso)` y crea `UsuarioAutorizado` si no existe. Solo cubre el tipo `PROFESOR_EXTERNO`.
- **El contexto de importación lo resuelve el propio importador**, no el servicio: toma `centro` y `curso` del usuario en sesión (`AuthUtils.getUser().getCentroActivo()`) y busca el `TipoUsuario` por el código del enum (`tipoFichero.name()`).
- **El formulario simula un wizard de un solo paso**: `canSave="false"` y `canNew="false"` en el form; el botón "Importar" encadena validación local → validación remota → `save`. Tras el save, el formulario pasa a modo solo lectura (`id != null` muestra `panelResultado`, `id == null` muestra `panelEntrada`). No hay edición ni borrado posibles.
- **Los campos de sistema son asignados exclusivamente por el servidor** (`fireActionRule_asignarCamposSistema` + `fireActionRule_ejecutarImportacion`). El `AllowProperties` del controlador solo permite pasar `tipoFichero` y `fichero` desde el cliente.
- **`ImportadorFicheroFactory`** es una clase final con constructor privado — no es inyectable vía Guice. Se instancia directamente con `ImportadorFicheroFactory.create(tipoFichero, fichero)`.

## Controladores y métodos

### `TareaImportacionController`
| Método | Qué hace en una línea |
|---|---|
| `validateSave(ActionRequest, ActionResponse)` | Valida `tipoFichero` y `fichero` antes del save; distingue insert vs update y devuelve errores de negocio al cliente |

## Servicios y métodos públicos

### `TareaImportacionService` / `TareaImportacionServiceImpl`
| Método | Qué hace en una línea |
|---|---|
| `validateInsert(TareaImportacion)` | Comprueba que `tipoFichero` y `fichero` no sean null |
| `validateUpdate(TareaImportacion, TareaImportacion)` | Siempre devuelve error — las importaciones son inmutables |
| `validateRemove(TareaImportacion)` | Siempre devuelve error — no se permite borrar importaciones |
| `insert(TareaImportacion)` | Asigna campos de sistema, ejecuta la importación y persiste; si falla guarda el error en `log` con `estado=false` |

## Vistas

| Vista | Para qué |
|---|---|
| `subsysImportacion.TareaImportacion@Main-action` | Action-view principal con `forceEdit=true` y `reload-grid=true` |
| `subsysImportacion.TareaImportacion@Main-grid` | Lista de importaciones ordenada por `-fechaImportacion`; solo permite crear nuevas, no editar ni borrar |
| `subsysImportacion.TareaImportacion@Main-form` | Formulario doble panel: `panelEntrada` (solo si `id==null`) y `panelResultado` de solo lectura (solo si `id!=null`) |

## Dependencias

### Subsistemas
| Subsistema | Para qué |
|---|---|
| `common` | `TipoUsuarioService.findByCodigo()` para resolver el `TipoUsuario` a partir del enum; `Centro` como resultado de la importación |
| `registrousuario` | `UsuarioAutorizadoService` para buscar duplicados e insertar nuevos `UsuarioAutorizado` en la importación CSV |

### Infraestructura
| Infraestructura | Para qué |
|---|---|
| `validation` (`BusinessMessages` / `BusinessMessage`) | Devolver errores de validación estructurados al cliente desde el controlador y el servicio |
| `axelorhelper` (`ActionRequestHelper`, `ActionResponseHelper`) | Extraer el modelo del request con `AllowProperties` y enviar los errores al response |
| `DniUtil` | Normalizar y validar DNIs leídos del CSV |
