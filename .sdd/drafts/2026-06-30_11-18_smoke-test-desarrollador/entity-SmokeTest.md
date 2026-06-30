# Modelo: SmokeTest

## Descripción

Registro de prueba **sin valor de negocio**, usado únicamente para comprobar de forma rápida y segura que la aplicación funciona (alta, lectura, modificación y borrado) y que se accede al servidor. Cada registro tiene un texto introducido por el Administrador y dos fechas que sella el servidor: la fecha en que se creó y la fecha de su última modificación. No tiene ciclo de vida ni relación con ninguna otra entidad.

## Campos

- **texto** — el texto libre que el Administrador escribe en el registro de prueba.
- **fecha de creación** — momento en que se creó el registro; lo pone el servidor.
- **fecha de última modificación** — momento de la última modificación del registro; lo pone el servidor.

## Restricciones

- RES-001 — El texto es obligatorio.

## Campos calculados

- CC-001 — fecha de creación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: la fecha y hora actuales del servidor en el momento del alta; no vuelve a cambiar.
- CC-002 — fecha de última modificación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: la fecha y hora actuales del servidor; se fija al crear y se recalcula en cada modificación del registro.

## Acción: Crear

**Input AllowProperties:** texto

## Acción: Modificar

**Input AllowProperties:** texto