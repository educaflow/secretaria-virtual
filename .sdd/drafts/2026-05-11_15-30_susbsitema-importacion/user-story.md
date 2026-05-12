---
type: user-story
---

# Importación de usuarios

En la secretaría virtual se necesita importar datos de usuarios desde un XML o CSV externo. Queremos que esos datos se guarden en una tabla de usuarios que se pueden registrar (no los usuarios de Axelor), inlcuyendo de qué tipos son.  

## Quién interviene

- **El importador**: el usuario que quiere importar los datos de usuarios. Puede ser cualquier admin de Axelor.
- **El sistema de importación**: el código que se encarga de recibir el fichero, procesarlo, y guardar los datos en la tabla de usuarios importados.

## Qué tiene que pasar

1. El importador entra en la aplicación y, en el menú "Importar usuarios", ve el listado de importaciones que se han hecho, con su fecha, tipo de usuarios del fichero, nombre del fichero, el usuario que hizo la importación y si la importación ha sido correcta o no.

2. Cuando se abren los detalles de una importación, se mostrarán los datos anteriores y un log con el resultado de la importación.
   
3. El importador puede subir un nuevo fichero de importación. El sistema pedirá que se indique el tipo de usuarios (PROFESOR, ALUMNO o FAMILIAR) que se están importando y un fichero. Si no se indica el tipo o el fichero, no se puede continuar.

4. Cuando se sube el fichero, el sistema de importación lo procesa. Antes de guardar ningún dato, se realizan las siguientes comprobaciones:
   - Si ya existe una importación con la misma `fechaExportacion`, tipo de usuario y centro para el curso activo, la importación se guarda como fallida con un error en el log.
   - Si el formato del fichero no es correcto, se guarda la importación como fallida con un error en el log.

5. Si las comprobaciones son correctas, se procesan los usuarios del fichero. Los DNIs se validan: si un DNI no tiene el formato correcto, ese usuario se omite, se registra el error en el log y la importación continúa con el resto. Al finalizar, los usuarios válidos se insertan en la tabla de usuarios importados, con su tipo, centro, curso y fechaExportacion. Cada registro es único por la combinación (centro, dni, tipoUsuario, curso, fechaExportacion); nunca se actualizan registros existentes, solo se insertan nuevos. Se guarda la importación como correcta y se muestra un log con el resultado (número de usuarios importados, número de errores, etc.).

6. Una vez importados, se actualizan los usuarios registrados en el sistema con los nuevos datos de la importación. Si este proceso falla, se revierte la importación completa (no se guardan ni los usuarios importados ni los cambios en usuarios registrados) y el error se registra en el log.  

## Tipos de usuarios

- **PROFESOR**: Formato XML. Los nodos y atributos relevantes son:
```xml
<centro codigo="12345" curso="2025" fechaExportacion="11/05/2026 16:13:00">
    <docentes>
        <docente documento="11111111A"/>
    </docentes>
</centro>
```

- **ALUMNO**: Formato XML. Los nodos y atributos relevantes son:
```xml
<centro codigo="12345" curso="2025" fechaExportacion="11/05/2026 16:13:00">
    <alumnos>
        <alumno documento="22222222B"/>
    </alumnos>
</centro>
```

- **FAMILIAR**: Formato XML. Los nodos y atributos relevantes son:
```xml
<centro codigo="12345" curso="2025" fechaExportacion="11/05/2026 16:13:00">
    <familiares>
        <familiar documento="33333333C"/>
    </familiares>
</centro>
```

Para los tres tipos XML, el centro y el curso son los que vienen en el propio fichero. El sistema comprueba que el centro del fichero coincide con el centro activo del importador; si no coincide, la importación se registra como fallida.

- **PROFESOR_EXTERNO**: Formato CSV. El fichero solo contiene documentos; el centro y el curso se obtienen del **centro activo del importador** y del curso activo de ese centro. La fecha de exportación será la **fecha en que se realice la importación**. El fichero puede incluir o no una línea de cabecera; si la incluye, se ignora. El formato del CSV es el siguiente:
```
44444444D
55555555E
66666666F
```

## Tipos de usuarios registrados 

Los mismos que los tipos de usuarios anteriores más los siguientes tipos de usuarios registrados, que se marcan como EX:

- **EXPROFESOR**: Usuario que no es profesor en la última importación, pero sí lo fue en alguna importación anterior.
- **EXALUMNO**: Usuario que no es alumno en la última importación, pero sí lo fue en alguna importación anterior.
- **EXFAMILIAR**: Usuario que no es familiar en la última importación, pero sí lo fue en alguna importación anterior.

No existe tipo EXPROFESOR_EXTERNO: los profesores externos son un tipo especial que solo está activo durante el curso en que se importan; fuera de ese periodo simplemente no tienen ese tipo.

Un usuario registrado no puede tener simultáneamente un tipo base y su correspondiente tipo EX. Al añadir el tipo base se elimina el EX, y viceversa.

## Proceso de actualización de usuarios registrados

### Importación XML (PROFESOR, ALUMNO, FAMILIAR)

Antes de procesar el fichero, el sistema comprueba que el **centro activo del importador coincide con el centro del fichero XML**. Si no coinciden, la importación se guarda como fallida con un error en el log.

Si la validación es correcta, para cada usuario registrado del mismo centro y tipo que el de la importación, se comprueban dos condiciones:

- **UsuarioImportadoActual**: existe un usuario importado con el mismo tipo de usuario, documento y centro, con la **última** fecha de exportación para el curso activo de ese centro.
- **UsuarioImportadoAnterior**: existe un usuario importado con el mismo tipo de usuario, documento y centro, con una fecha de exportación **anterior** a la última para el curso activo de ese centro.

| UsuarioImportadoActual | UsuarioImportadoAnterior | Resultado |
|:---:|:---:|:---|
| No | No | Elimina ese tipo de usuario *(caso defensivo, no debería ocurrir)* |
| No | Sí | Añade tipo EX (EXPROFESOR / EXALUMNO / EXFAMILIAR) y elimina el tipo base si lo tenía |
| Sí | No | Añade tipo base (PROFESOR / ALUMNO / FAMILIAR) y elimina el tipo EX si lo tenía |
| Sí | Sí | Añade tipo base (PROFESOR / ALUMNO / FAMILIAR) y elimina el tipo EX si lo tenía |

### Importación CSV (PROFESOR_EXTERNO)

El proceso es diferente: el sistema busca directamente los usuarios registrados cuyos documentos coincidan con los del fichero CSV y les añade el tipo PROFESOR_EXTERNO si no lo tenían ya. No se aplica la tabla anterior ni se generan tipos EX.

## Restricciones que no pueden romperse

- Solo un admin de Axelor puede hacer importaciones.
- El formato del fichero debe ser correcto para que la importación se considere correcta.
- El tipo de usuario debe ser indicado para que la importación se considere correcta.
- No se pueden modificar ni eliminar importaciones una vez hechas — son solo lectura.
- No se pueden modificar ni eliminar usuarios importados una vez hechos — son solo lectura.
- La actualización de usuarios registrados se hace automáticamente después de cada importación correcta, no se puede hacer manualmente ni saltarse.

## Lo que aporta valor

- El importador puede importar datos de usuarios de forma masiva, sin tener que hacerlo uno a uno.
- El sistema de importación se encarga de procesar el fichero y guardar los datos de forma estructurada, lo que facilita su uso posterior.
- La actualización automática de usuarios registrados garantiza que los datos de los usuarios estén siempre actualizados con la última importación, sin necesidad de intervención manual