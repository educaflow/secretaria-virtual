# Modelo: InstanciaGeneral

El expediente de la instancia general: recoge lo que el alumno expone y solicita, los adjuntos que aporta, y la resolución del centro (respuesta o petición de subsanación). Su ciclo de vida es la máquina de [estados.md](./estados.md).

## Campos

- **exposición** — los hechos y motivos que el solicitante expone *(se rellena: en TR-001)*
- **solicitud** — lo que el solicitante pide al centro *(se rellena: en TR-001)*
- **tipo de resolución** — la decisión del secretario; valores: RESPONDER, SUBSANAR_DATOS *(se rellena: en TR-004)*
- **respuesta** — el texto de la respuesta del centro *(se rellena: en TR-004)*
- **datos a subsanar** — lo que el centro pide corregir al solicitante *(se rellena: en TR-004)*
- **documento de la instancia** — el PDF de la instancia (ver [documento-instancia.md](./documento-instancia.md)), en sus versiones original, firmada y resguardo sellado por el registro de entrada *(las fija el sistema)*
- **documento de la respuesta** — el PDF de la respuesta (ver [documento-respuesta.md](./documento-respuesta.md)), en sus versiones original, firmada por el director y sellada por el registro de salida *(las fija el sistema)*

## Campos calculados

- CC-InstanciaGeneral-001 — fecha de presentación
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: la fecha en que la instancia se presentó por registro de entrada
- CC-InstanciaGeneral-002 — fecha de resolución
  - momento: escritura
  - sobreescribible: nunca
  - cálculo: la fecha en que la respuesta firmada se emitió por registro de salida
