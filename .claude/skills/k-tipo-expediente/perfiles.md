# Perfiles: `CREADOR`, `TRAMITADOR` y `presentadoEnPapel`

Cómo se presenta un expediente y qué implica para el tipo. Complementa `SKILL.md` §2.1 (el `profile` de un `<state>`) y `vistas.md` §2 (el `profile` de un `<form>`).

## 1. Solo dos perfiles son especiales

Del enum `Profile` (`subsystem/expedientes/domains/Profile.xml`), **solo `CREADOR` y `TRAMITADOR`** tienen significado propio: son las dos formas de presentar un expediente.

Los demás (`COLABORADOR`, `AFECTADO`, `SECRETARIO`, `DIRECTOR`, `AUDITOR`) son etiquetas corrientes: dan el turno en un estado y eligen qué vista se pinta, nada más. **MUST NOT** darles trato particular en el código del tipo.

## 2. La equivalencia

```text
presentadoEnPapel == true   ⟺  actúa el TRAMITADOR  (le entregaron la solicitud en papel y la registra)
presentadoEnPapel == false  ⟺  actúa el CREADOR     (presenta el expediente telemáticamente)
```

- Cómo se presenta lo resuelve la pantalla «Nuevo expediente» según los perfiles del usuario (solo pregunta si tiene los dos); no hay que programarlo en el tipo.
- En el `InitialEventManagerImpl` se consulta con `expediente.getPresentadoEnPapel()`. `InitialEventContext` **no** tiene `getProfile()`.
- En un `PhaseEventManagerImpl`, `eventContext.getProfile()` es el perfil con el que actúa quien dispara el evento.
- `presentadoEnRepresentacion` es **otra cosa**: dice para quién es el expediente (`personaSolicitante` ≠ `personaInteresada`), no cómo se presentó. Solo es posible si el trámite tiene `<permitidoPresentarEnRepresentacion>true` (`k-tramite` §3).

## 3. Qué implica al escribir el tipo

1. Si el tipo admite papel, el estado inicial suele depender de `presentadoEnPapel`: en papel se empieza adjuntando la solicitud escaneada y después se copian sus datos; telemáticamente se empieza por la entrada de datos.
2. Un estado de perfil `CREADOR` por el que también pasa el papel **MUST** tener además un `<form state="X" profile="TRAMITADOR">`: si no, quien registra el papel cae en la vista de solo lectura y el expediente se atasca.
3. En papel el `CREADOR` no interviene, así que el form `profile="CREADOR"` de ese estado **MUST NOT** llevar botones ni avisos para el caso en papel.

- ✅ CORRECTO: `initialEventContext.updateState(Boolean.TRUE.equals(expediente.getPresentadoEnPapel()) ? States.Recepcion.PENDIENTE_ESCANEADO : States.Recepcion.ENTRADA_DATOS)`
- ❌ INCORRECTO: `initialEventContext.getProfile()` (no existe; en el alta se lee del expediente)
- ❌ INCORRECTO: tratar `AFECTADO` o `COLABORADOR` como si decidieran algo del alta (no son especiales)
