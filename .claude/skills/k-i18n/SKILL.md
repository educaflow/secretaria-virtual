---
name: k-i18n
description: Internacionalización (i18n) en la secretaría virtual sobre Axelor — cómo se traducen mensajes, títulos de campos, etiquetas de vistas y enums al idioma del usuario; uso de `I18n.get(...)` en Java, `__!!` para palabras no traducibles, y el patrón estándar para obtener el `title` de un campo desde un servicio.
---

# k-i18n

Este skill describe cómo gestionar la **internacionalización (i18n)** en la aplicación: cómo se obtiene un texto traducido al idioma del usuario, cómo se referencian los títulos de los campos de las entidades, y qué convenciones usa el proyecto para los ficheros de traducciones.

## Idiomas soportados

La aplicación se usa en centros educativos de España y soporta como mínimo:

- **Español (`es`)** — idioma por defecto.
- **Catalán (`ca`)** — segunda lengua oficial en parte del territorio.

El idioma efectivo de cada usuario lo gestiona Axelor: lo determina la configuración del usuario logueado (campo `language` del `User`) y, en su defecto, la configuración global de la aplicación.

## Ficheros de traducciones

Las traducciones viven en ficheros CSV bajo `src/main/resources/i18n/`:

- `i18n_es.csv` — traducciones al español.
- `i18n_ca.csv` — traducciones al catalán.

> **IMPORTANTE — no editar a mano.** Estos CSV se **generan automáticamente** mediante un script del proyecto. Nunca crees ni edites `i18n_es.csv` ni `i18n_ca.csv` manualmente: cualquier cambio se perderá en la siguiente regeneración. Lo que tienes que hacer es escribir los textos en castellano en su sitio (`title`/`help` del XML del dominio, literales en las vistas, mensajes pasados a `I18n.get(...)` en el código), y el script se encarga de extraer las claves y rellenar los CSV.

## El sufijo `__!!` — palabras que no se traducen

A veces aparece un texto acabado en `__!!`, por ejemplo:

```
AutoFirma__!!
```

Este sufijo indica al script de generación de i18n que esa palabra **no debe traducirse** — es un nombre propio, una marca, un acrónimo, etc. El script copia el texto tal cual a los CSV pero **eliminando el sufijo** `__!!` del valor final. Por tanto:

- En el XML / código fuente escribes `AutoFirma__!!`.
- En la UI el usuario ve `AutoFirma`.
- Si necesitas usar esa palabra en código (p.ej. en `camelCase`), primero **quita el `__!!`** y después transforma: `AutoFirma__!!` → `AutoFirma` → `autoFirma`.

## `I18n.get(...)` — traducir desde Java

En el código Java, para devolver un texto traducido al idioma del usuario se usa la API estándar de Axelor:

```java
import com.axelor.i18n.I18n;

String texto = I18n.get("Mensaje a traducir");
```

`I18n.get(...)` busca la clave en los CSV del idioma activo del usuario; si no la encuentra, devuelve la clave original (es decir, el texto en castellano que pasaste como argumento). Por eso la convención del proyecto es **escribir el literal en castellano dentro de `I18n.get(...)`**, y dejar que el script de i18n genere las entradas del CSV catalán automáticamente.

Usos típicos:

- Mensajes de validación / error que se muestran al usuario (`BusinessMessage`, excepciones).
- Texto dinámico construido en el servidor que acaba en la UI (asuntos de correos, contenido de PDFs, etc.).
- Cualquier literal que el usuario final vaya a ver y que no esté ya cubierto por una vista XML.

## Título de un campo de una entidad

Cuando necesitas mostrar el **nombre de un campo** de una entidad (por ejemplo como `label` de un `BusinessMessage`, o en un mensaje del tipo *"El campo 'Nombre' es obligatorio"*), **no escribas el literal a mano**. El `title` del campo ya está declarado en el XML del dominio y traducido vía los CSV de i18n, así que lo correcto es obtenerlo desde la metainformación.

Patrón estándar:

```java
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;

String label = I18n.get(Mapper.of(LeyEducativa.class).getProperty("name").getTitle());
```

Qué hace cada pieza:

- `Mapper.of(LeyEducativa.class)` — obtiene el `Mapper` de la entidad (caché interno de Axelor sobre su metainformación).
- `.getProperty("name")` — devuelve la `Property` del campo `name` declarado en el XML del dominio.
- `.getTitle()` — devuelve el `title` declarado en el dominio (p.ej. `"Nombre"`); si no hay `title` definido, devuelve `null`.
- `I18n.get(...)` — traduce ese título al idioma del usuario activo.

Ejemplo de uso real en un `validate*` de un `*ServiceImpl`:

```java
import com.axelor.db.mapper.Mapper;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.i18n.I18n;

@Override
public Optional<BusinessMessages> validateInsert(LeyEducativa leyEducativa) {
    BusinessMessages messages = new BusinessMessages();

    if (leyEducativa.getName() != null && leyEducativa.getName().trim().equalsIgnoreCase("aa")) {
        messages.add(new BusinessMessage(
            "name",
            I18n.get("No puede ser 'aa'"),
            I18n.get(Mapper.of(LeyEducativa.class).getProperty("name").getTitle())
        ));
    }

    return messages.isValid() ? Optional.empty() : Optional.of(messages);
}
```

Reglas para el `label` de un `BusinessMessage`:

- Si el mensaje hace referencia a un campo concreto (`fieldName` != null), **usa el `title` del campo** con el patrón de arriba, no un literal en castellano. Así se mantiene consistente con la etiqueta que el usuario ve en el formulario y se traduce automáticamente al cambiar de idioma.
- Si el `title` del campo no está definido en el XML del dominio (`getTitle()` devuelve `null`), añade el `title` al dominio en lugar de poner un literal en el servicio — el `title` es información del modelo, no del servicio.

## Títulos en vistas y dominios

En los ficheros XML de dominios y vistas, los atributos `title=""` y `help=""` se escriben **directamente en castellano**. El script de generación de i18n los extrae como claves y rellena los CSV. Por tanto:

- En `LeyEducativa.xml` (dominio): `<string name="name" title="Nombre"/>`.
- En `LeyEducativa.xml` (vista): `<field name="name" title="Nombre"/>` (solo si el título debe ser distinto del declarado en el dominio).
- En menús: `<menuitem ... title="Leyes educativas"/>`.

No hay que envolver estos atributos en nada — Axelor los traduce solo en tiempo de render usando el idioma del usuario.

## Resumen — reglas operativas

1. **Escribe los textos en castellano** allí donde corresponda: `title`/`help` en XML, primer argumento de `I18n.get(...)` en Java.
2. **Nunca edites manualmente** `i18n_es.csv` ni `i18n_ca.csv`: se generan con un script.
3. **Usa `__!!`** para marcar palabras intraducibles (nombres propios, marcas); recuerda quitar el sufijo al usarlas en código.
4. **Para el label de un campo**, usa siempre `I18n.get(Mapper.of(Entidad.class).getProperty("campo").getTitle())` en lugar de literales.
5. **Para mensajes de usuario** (validaciones, errores, correos, PDFs) usa `I18n.get("Texto en castellano")`.