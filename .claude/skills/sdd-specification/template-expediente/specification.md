---
type: specification
template: <nombre de la carpeta de la plantilla activa sin el prefijo `template-`, o el valor reservado `external` si esa carpeta es externa; lo fija §8 del skill>
---

<!-- Plantilla del ÍNDICE de la especificación de un trámite y su tipo de expediente.
     Reprodúcela en su orden exacto. Sustituye cada placeholder <…> por contenido real
     y ELIMINA todos los comentarios como este. Ver la guía README.md de esta carpeta. -->

# Objetivo

<!-- Una sola frase: qué permite hacer el trámite y a quién. Nada más. -->
<Una frase con lo que permite hacer el trámite y a quién.>

# El trámite

<!-- Todo lo que identifica al trámite de cara al usuario, ANTES de que exista ningún expediente. -->

- **Nombre visible:** <el nombre con el que el usuario ve el trámite en la lista de trámites disponibles>
- **A qué colectivo va dirigido:** <profesorado | alumnado | familias y tutores legales | equipo directivo | personal administrativo | conserjería> — es la categoría bajo la que el usuario lo encuentra en la lista de trámites.
- **Quién puede iniciarlo:** <qué tipo de usuario o qué cargo del centro puede crear un expediente de este trámite>
- **Para qué sirve:** <dos o tres frases en lenguaje llano: qué problema resuelve y en qué situación se usa>
- **Texto de ayuda que ve el usuario antes de empezar:**

  > <El texto literal, redactado de cara al usuario, que se le muestra al consultar la ayuda del trámite antes de crear un expediente. Explica qué va a necesitar, qué documentación debe tener a mano y qué pasará después. Escríbelo tal cual lo verá; nada de notas para el desarrollador.>

- **Versión:** <la primera versión del trámite | una versión nueva de un trámite que ya existe (indicar cuál y qué cambia respecto a la anterior) | una modificación de una versión que ya existe (indicar trámite, versión y qué cambia; la spec es un delta — ver §3.8 de la guía)>

# Actores y perfiles

<!-- Quién interviene, con qué papel y quién ostenta cada papel. Los nombres de perfil SÍ se usan:
     son vocabulario de negocio. Se toman del catálogo CERRADO de la plataforma
     (CREADOR, RESPONSABLE, SECRETARIO, DIRECTOR, AUDITOR) y MUST NOT inventarse otros.
     Un trámite puede usar UNO, DOS o VARIOS de ellos: declara SOLO los que este trámite use,
     uno por fila. El papel que juega cada perfil lo define ESTE trámite; no viene predefinido. -->

| Perfil | Qué papel juega en este trámite | Quién lo ostenta |
|---|---|---|
| <PERFIL> | <qué hace en este trámite: qué aporta, qué revisa o qué decide> | <tipo de usuario o cargo del centro> |
| <PERFIL> | <qué hace> | <tipo de usuario o cargo del centro> |

<!-- Repite una fila por cada perfil que use algún estado del trámite. Un perfil sin nadie que lo
     ostente deja su estado inalcanzable. Si un perfil de la lista no se usa, no lo declares. -->

Además de los perfiles anteriores, <describe qué ve cualquier otro usuario con acceso al expediente: por defecto, cualquier perfil distinto del que tiene el turno ve el estado en solo consulta>.

# Historias de usuario

<!-- Una sección por historia; sus escenarios van DEBAJO de ella, en pasos numerados,
     con usuarios y centros reales de los datos de demo. -->

## HU-001 — Como <Actor> quiero <lo que quiere hacer> para <motivo>

- ESC-001 — <Nombre corto>:
  1. <El actor inicia sesión con una cuenta concreta de los datos de demo.>
  2. <Crea el expediente del trámite y llega al estado que se quiere probar, paso a paso.>
  3. <Rellena cada dato con su valor concreto.>
  4. <Pulsa el botón concreto.>
  5. <El sistema responde: el mensaje literal y/o el estado al que queda el expediente.>
- ESC-002 — <Nombre corto>:
  1. …
  4. Si <condición>: <el sistema hace esto>.
  5. Si no: <el sistema hace esto otro y no hace aquello>.

## HU-002 — Como <Actor> quiero <lo que quiere hacer> para <motivo>

- ESC-003 — <Nombre corto>:
  1. …

# Fases y estados

<!-- El ciclo de vida vive entero en estados.md. Aquí solo el resumen y la tabla de enlaces. -->

- **Estado en el que nace el expediente:** <ESTADO> (fase <FASE>)
- **Estados que cierran el expediente:** <ESTADO>, <ESTADO>
- **Desde qué estado se puede borrar el expediente:** <ESTADO> | <desde ninguno>

El ciclo de vida completo —fases, estados, acciones, comprobaciones, efectos y transiciones— está en [estados.md](./estados.md).

| Fase | Título que ve el usuario | Estados | Pantallas |
|---|---|---|---|
| <FASE> | <título de la fase> | <ESTADO>, <ESTADO> | [pantallas-<fase>.md](./pantallas-<fase>.md) |

# Pantallas

<!-- Una fila por FASE. El detalle de cada pantalla (pareja estado + perfil) vive en su fichero. -->

| Fichero | Fase | Qué pantallas contiene |
|---|---|---|
| [pantallas-<fase>.md](./pantallas-<fase>.md) | <FASE> | <las pantallas de los estados de esa fase, una por pareja estado + perfil> |

# Documentos

<!-- Si el trámite no genera ningún documento, escribe la frase y omite el enlace. -->

El trámite genera <N> documento(s). Su contenido, cuándo se genera cada uno, quién lo firma y si se registra está en [documentos.md](./documentos.md).

<!-- Si no genera ninguno: «Este trámite no genera ningún documento.» y NO se crea documentos.md. -->

# Registros de entrada y salida, y notificaciones

<!-- Qué queda constancia oficial de que entró o salió del centro, y qué avisos se envían. -->

- **Registros de entrada:** <en qué momento del trámite se registra oficialmente la entrada de documentación, con qué documento principal y con qué anexos> | *(ninguno)*
- **Registros de salida:** <en qué momento se registra oficialmente la salida, con qué documento y con qué anexos> | *(ninguno)*
- **Avisos que se envían:** <a quién, en qué momento, por qué medio y con qué contenido> | *(ninguno)*

# Seguridad

<!-- Declara SOLO los roles con algún acceso; lo no declarado queda denegado. Incluye el alcance por centro. -->

- **<Tipo de usuario o cargo>:** <qué puede hacer con los expedientes de este trámite> — <alcance: solo los suyos | los de su centro | los de todos los centros>.

# Datos iniciales

<!-- Lo que debe existir precargado para que el trámite funcione. Este es el ÚNICO estado previo
     que los escenarios pueden presuponer, además de los datos de demo. -->

- **Asignación de perfiles:** <qué perfil se concede a qué tipo de usuario o cargo, y con qué alcance: para todo el trámite o solo para esta versión>
- **Categoría del trámite:** <la categoría a la que se adscribe, y si ya existe o hay que crearla>
- **Otros datos maestros que el trámite necesita:** <catálogos, cargos, certificados del centro…> | *(no aplica)*

# Fuera de alcance

- <Lo que el negocio decide NO hacer en esta versión del trámite.>
