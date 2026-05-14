## ¿Para qué sirve esto?

Permite cargar masivamente usuarios (profesores, alumnos, familiares y profesores externos) desde un fichero XML o CSV. La importación se ejecuta **sincrónicamente en el `insert`** del servicio: el fichero se procesa, se persiste el resultado y el log queda guardado en la misma `TareaImportacion`.

## Lo no obvio

- **La importación ocurre dentro de `insert()`**, no en un job asíncrono ni en un endpoint separado. Si falla lanza `ImportadorException` (checked), que el servicio captura y guarda en `log` sin propagar — el registro se guarda igualmente con `estado = false`.
- **`ImportadorUsuarioCSV` e `ImportadorUsuarioXML` no están implementados**: ambos lanzan `ImportadorException("@TODO: Importación no implementada todavía")`. El esqueleto de la estrategia existe pero la lógica real está pendiente.
- **El formulario simula un wizard de un solo paso**: la vista forma tiene `canSave="false"` y `canNew="false"`; el botón "Importar" encadena validación local → validación remota → `save`. Al volver, el formulario pasa a modo solo-lectura (`id != null` muestra el panel de resultado). No hay edición ni borrado posibles.
- **Los campos `usuario`, `fechaImportacion`, `estado`, `log`, `centro`, `curso` y `fechaExportacion` son asignados exclusivamente por el servidor** (`fireActionRule_asignarCamposSistema` + `fireActionRule_ejecutarImportacion`). El `AllowProperties` del controlador solo permite pasar `tipoFichero` y `fichero` desde el cliente.
- **Routing de implementaciones por tipo**: `PROFESOR`, `ALUMNO` y `FAMILIAR` → `ImportadorUsuarioXML`; `PROFESOR_EXTERNO` → `ImportadorUsuarioCSV`. Lo gestiona `ImportadorFicheroFactory` (clase final con constructor privado, no inyectable).

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
| `subsysImportacion.TareaImportacion@Main-form` | Formulario doble panel: panel de entrada (solo si `id==null`) y panel de resultado de solo lectura (solo si `id!=null`) |

## Dependencias

### Subsistemas
| Subsistema | Para qué |
|---|---|
| `common` | `Centro` se usa como resultado de la importación para saber a qué centro pertenecen los datos importados |

### Infraestructura
| Infraestructura | Para qué |
|---|---|
| `validation` (`BusinessMessages` / `BusinessMessage`) | Devolver errores de validación estructurados al cliente desde el controlador y el servicio |
| `axelorhelper` (`ActionRequestHelper`, `ActionResponseHelper`) | Extraer el modelo del request con `AllowProperties` y enviar los errores al response |