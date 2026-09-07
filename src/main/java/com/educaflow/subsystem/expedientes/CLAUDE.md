# `subsystem/expedientes` — el motor de tramitación, y nada más

Aquí vive **solo el motor**: la máquina que lleva un expediente de un estado a otro, sus entidades base, y los controladores y la gestión de vistas que ese tramitador necesita para funcionar.

**MUST** mantenerse **lo más pequeño posible**. Todo lo que entra aquí lo heredan **todos** los tipos de expediente, presentes y futuros: crece la superficie que cualquier trámite puede tocar, crece lo que hay que entender antes de crear un trámite nuevo, y cada cambio pasa a poder romper trámites que nadie estaba mirando.

## La regla

**Lo que un expediente concreto necesita para implementarse MUST NOT ir aquí.**

Que una pieza la use un expediente no la convierte en parte del motor. La pregunta no es «¿la necesita un expediente?», sino «¿la necesita el **tramitador** para tramitar **cualquier** expediente?». Si la respuesta es no, su sitio está fuera.

## Dónde va cada cosa

| Lo que quieres añadir | Dónde va |
|---|---|
| El tramitador la necesita para tramitar cualquier expediente | **aquí** |
| La necesitan varios tipos de expediente, pero **no** el motor | `tramites/util/<propósito>/` — cumpliendo las seis condiciones de su [`CLAUDE.md`](../../tramites/util/CLAUDE.md) |
| La necesita **un solo** tipo de expediente | su carpeta de versión `tramites/<tramite>/<vN>/` |
| Es dominio propio de otro subsistema (criptografía, correo, registro de entrada/salida, firmas…) | **ese** subsistema; el pegamento que lo conecta con un `trigger*` va en `tramites/util/` |
| Es una utilidad sin estado, o infraestructura reutilizable en cualquier proyecto | `base/util` o `base/infrastructure` |

## Señales de que una pieza no es de aquí

- Nombra un trámite, una fase, un estado, un evento, un perfil o un campo **concretos**.
- Solo la llaman `PhaseEventManagerImpl`, `InitialEventManagerImpl` o `StateEventValidatorImpl` de trámites.
- Aparece durante una iniciativa de un trámite y hubo que tocar el motor «de paso» para sacarla adelante: eso casi siempre significa que la pieza es del trámite, no del motor.
- Solo la necesita **hoy** un trámite, y se coloca aquí «por si otros la necesitan mañana». Cuando la necesite el segundo, sube a `tramites/util/`.

## Dirección de las dependencias

`tramites → subsystem/expedientes`, y nunca al revés.

**MUST NOT** existir ningún import de `com.educaflow.tramites..` en este paquete: el motor **no conoce ningún trámite concreto**, los descubre por sus ficheros maestros y los invoca por sus puntos de extensión.

## Ampliar el motor

Ampliarlo es legítimo solo cuando lo que se añade es una **capacidad del propio motor** que cualquier trámite puede usar: un punto de extensión nuevo, un tipo de acción nuevo, una capacidad nueva de la máquina de estados.

Es una decisión de arquitectura, no un detalle de implementación: **STOP** y consúltalo con el usuario antes de diseñarlo o implementarlo, aunque la iniciativa en curso parezca necesitarlo.