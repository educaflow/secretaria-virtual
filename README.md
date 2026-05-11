# Prueba de Lorenzo de Axelor

```bash

./run.sh

```

Iconos:

https://fonts.google.com/icons?icon.set=Material+Icons

# Parametros de Tomcat

TomcatSupport: Crea el fichero de configuración de Tomcat llamado axelor-tomcat.properties
TomcatRun: Parsea lo parámetros de entrada de "./gradlew --no-daemon run --port 8080 --contextPath /"
TomcatRunner: Es la tarea de Grandle que ejecuta la clase que levanta Tomcat
TomcatServer: Es la clase que ejecuta Tomcat finalmente

# Página de inicio

En las view-action añadir el atributo home="true"
Luego en el grupo hay un atributo llamado "homeAction" que hay que poner alguna de las acciones que tengan el atributo home="true"

# TypeScript

/src/view-containers/action/executor.ts
/src/hooks/use-relation/use-editor.tsx#handleConfirm   El botoón OK de las ventanas Popup

# freeSearch

Al poner nombres de campos en la propiedad "freeSearch" de las view-action,
se pueden buscar por esos campos en la barra de búsqueda de la parte superior de la pantalla.
Pero NO se puede buscar el campo es "Enum" pero se podría modificar añadiendo el tipo "enum"
en el switch de "axelor-front/src/view-containers/advance-search/utils.ts" línea 292 en la función "getFreeSearchCriteria"

# Traducción

Es necesario instalar `sudo apt install apertium apertium-spa-cat cg3 vislcg3 apertium-all-dev`

# Funciones que se pueden usar en las plantillas de React cuando se usa \<template\>

Están en el fichero `/hooks/use-parser/context/script-context.ts`

# Que un pdf no sea PDF/A

`gs -dPDFA=0 -dBATCH -dNOPAUSE -sDEVICE=pdfwrite -sOutputFile=salida.pdf entrada.pdf`

# Portafirmas

Documentación oficial: [Descargas](https://administracionelectronica.gob.es/ctt/portafirmas/descargas)

# Certificados FNMT:

```
AC RAÍZ FNMT-RCM
├── AC FNMT Usuarios          → Persona Física
├── AC Representación         → Representantes de entidades
├── AC Sello FNMT-RCM         → Sello electrónico de entidad
└── AC Administración Pública → Entidades públicas
```

# Ejemplos de iText
https://kb.itextpdf.com/itext/examples


# Creador de temas
https://shivantra.com/axelor-theme-designer/#/designer


# Para cargar directamente una fila en un `<action-view>` usar lo siguiente:
```
<action-view name="subsysExpedientes.TipoExpediente@Prueba-action" title="Tipo Expediente" model="com.educaflow.subsystem.expedientes.db.TipoExpediente"  >
    <view type="form" name="subsysExpedientes.TipoExpediente-form" />
    <context name="_showRecord" expr="3" />
</action-view>  
```     

# MCP IntelliJ
Instala este plugin que es un servidor de MCP: https://plugins.jetbrains.com/plugin/29174-ide-index-mcp-server
Por si está instalado quita el plugin de MCP genérico de JetBrains para evitar conflictos: `claude mcp remove jetbrains`
Ejecuta: `claude mcp add --transport http --scope user intellij-index http://127.0.0.1:29170/index-mcp/streamable-http` 

