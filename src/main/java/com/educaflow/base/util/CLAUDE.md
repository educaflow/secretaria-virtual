# CLAUDE.md — `base.util`

Clases de utilidad de **bajo nivel** del proyecto. Su objetivo es no repetir pequeños trozos de lógica (conversiones, hashing, validaciones de DNI/email, manipulación de XML, acceso al usuario autenticado, etc.).

**DEBES usar estas clases** en lugar de reimplementar la misma lógica. Antes de escribir cualquier conversión, hashing, validación o manipulación de XML/JSON/MetaFile, comprueba si ya existe el helper aquí.

## Convenciones

- Paquete: `com.educaflow.base.util`.
- Son helpers **stateless**: métodos `static` y sin estado mutable. No son servicios Guice ni entidades JPA.
- No dependen del dominio de negocio (expedientes, firmas, etc.); solo de Java, librerías de terceros y, como mucho, del framework Axelor (`AxelorUtil`, `MetaFileUtil`, `SecurityUtil`).
- Si necesitas un helper nuevo de carácter genérico y reutilizable, añádelo aquí (y actualiza este fichero); si la lógica es específica de un subsistema, va en ese subsistema, no aquí.

## Clases disponibles

### `AllowProperties` — whitelist de propiedades aceptadas desde el cliente (ver `k-secure-coding`)
- `createAllowProperties` — construye la whitelist a partir de un `Map` anidado.
- `createAllowAllProperties` — whitelist que admite todas las propiedades (`*`).
- `allowProperty` — indica si una propiedad concreta está permitida.
- `innerAllowProperties` — devuelve la whitelist anidada para una propiedad (sub-objeto).

### `AsciiTableUtil` — tablas ASCII para logs/consola
- `renderTable(name, exception)` — renderiza el stack trace completo de una excepción como tabla.
- `renderTable(name, heads, rows)` — renderiza una tabla genérica con cabeceras y filas.

### `AxelorUtil` — helpers sobre el framework Axelor
- `existsView` — comprueba si una vista Axelor existe por nombre/tipo/modelo.

### `CodigoVerificacionUtil` — códigos de verificación cortos
- `generar` — devuelve un código de 8 caracteres hex en mayúsculas (basado en UUID).

### `Convert` — conversión y formateo de valores
- `objectToLong` / `objectToInt` / `objectToBoolean` — convierte un `Object` numérico/booleano al tipo destino o lanza si no es compatible.
- `coerceToLong` / `coerceToInt` — versión laxa que también acepta `String` y trata `null`/vacío como `0`.
- `objectToUserString` — formatea cualquier valor (número, fecha, enum con `@EnumWidget`, booleano Sí/No…) al string visible para el usuario.
- Constantes `defaultLocale` (es-ES) y `defaultZoneId` (Europe/Madrid).

### `CryptoUtil` — hashing
- `sha256` — calcula el SHA-256 de un `byte[]` y lo devuelve como hex.

### `DniUtil` — validación de documentos de identidad españoles
- `clean` — normaliza un DNI/NIE quitando ceros de relleno (`0XXXXXXXXL`, `0YXXXXXXXL`, `Y0XXXXXXXL`).
- `isValid` — valida DNI, NIE, NIF especial (K/L/M) y CIF (con DC numérico o letra).

### `EMailUtil` — validación de email
- `isValid` — valida una dirección de email con el validador de Hibernate.

### `JsonUtil` — JSON con Jackson
- `toJson` — serializa un objeto a JSON.
- `fromJson` — deserializa JSON a una clase concreta.

### `MetaFileUtil` — operaciones sobre `MetaFile` de Axelor
- `downloadContent` — lee el contenido del fichero como `byte[]`.
- `uploadContent` — sube/actualiza el contenido de un `MetaFile` existente.
- `cloneMetaFile` — crea una copia nueva del fichero (con `filePath` correcto).
- `sha256` — SHA-256 del contenido del fichero.
- `createMetaFileInstance` — crea una instancia vacía de `MetaFile`.
- `getMetaFile` — recupera el `MetaFile` cuando Axelor entrega un `Map` con el `id` (en contextos de acción).
- `delete` — borra el `MetaFile` de forma segura (resolviendo el proxy de Hibernate).

### `ReflectionUtil` — reflexión
- `hasMethod` — indica si existe un método que cumpla una combinación de nombre/retorno/anotación/parámetros.
- `getMethod` — devuelve ese método (o `null`); lanza si hay ambigüedad.
- `getEnumConstant` — resuelve una constante enum por nombre.
- `getFieldValue` — lee un campo de un objeto por nombre (saltándose el `private`).

### `SecurityUtil` — usuario autenticado
- `getUser` — devuelve el `User` autenticado actual.
- `isAdmin` — indica si un usuario es administrador.

### `TextUtil` — strings
- `humanize` — convierte `SCREAMING_SNAKE_CASE` a texto humano.
- `toFirstsLetterToUpperCase` — capitaliza la primera letra.
- `sanitizeFileName` — sanea un nombre de fichero (quita acentos, caracteres peligrosos, reservados de Windows, trunca a 255).
- `isIdentifier` — `true` si el `String` empieza por letra y solo lleva letras sin acentos, dígitos y `_` (más estricta que `Character.isJavaIdentifierStart/Part`). Para los nombres que llegan del cliente y se resuelven como constante de enum o se concatenan en un nombre de vista.
- `isNullOrBlank` — `true` si el `String` es `null` o solo espacios.

### `TokenUtil` — tokens
- `generar` — devuelve un UUID completo como `String`.

### `XMLUtil` — DOM XML
- `getDocument(Path)` / `getDocument(byte[])` — parsea un XML desde fichero o bytes.
- `validarConSchema` — valida un XML contra un XSD y devuelve el error como `Optional<String>`.
- `getChilds` — devuelve todos los elementos hijo directos.
- `getChildsFilterByTagName` / `getChildFilterByTagName` — hijos directos cuyo tag coincide (lista / único).
- `getChildsFilterByTagNameAndAttributeName` / `getChildFilterByTagNameAndAttributeName` — además filtra por el atributo `name`.
- `getChildsFilterByPrefixTagName` / `getChildFilterByPrefixTagName` — hijos cuyo tag empieza por un prefijo.
- `getChildsFilterByPrefixTagNameAndAttributeName` / `getChildFilterByPrefixTagNameAndAttributeName` — prefijo de tag + atributo `name`.
- `getElementsFromEvaluateXPath` / `getElementFromEvaluateXPath` — evalúa una expresión XPath (lista / único).
- `getBooleanAttribute` / `getIntegerAttribute` / `getStringAttribute` — lee un atributo con valor por defecto si falta o está vacío.
- `importElement` / `importElements` — importa elementos a otro `Document`.
- `cloneDocument` — clona un `Document` completo.
- `printNode` — imprime un nodo en stdout con indentación.
- `getNodeFromString` — parsea un fragmento XML como `String` e importa el nodo a un `Document`.