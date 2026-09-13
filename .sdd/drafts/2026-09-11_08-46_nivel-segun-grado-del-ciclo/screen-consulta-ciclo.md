# Pantalla: Consulta de un ciclo

**Pantalla existente:** sí

## Identidad

- **Quién la usa:** Administrador, en solo lectura.
- **Qué muestra:** la ficha de un ciclo ya elegido en otro formulario, para consultarlo sin salir de él. El delta hace que el nivel deje de verse cuando el ciclo no lo lleva.

## Menú

No cuelga de ningún menú: se abre desde el formulario de un curso (pantalla Cursos), al pulsar sobre el ciclo que ya se ha elegido en él.

## Estructura jerárquica de las vistas

```
Formulario de consulta de ciclo   (se abre desde el formulario de curso, al pulsar sobre el ciclo elegido)
```

## Vista: Formulario de consulta de ciclo

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos del ciclo elegido, en solo lectura.
- **Se abre desde:** el formulario de curso, al pulsar sobre el ciclo ya elegido.

### Reglas de UI

- RUI-consulta-ciclo-formulario-001 — El campo Nivel solo se muestra cuando el grado del ciclo consultado admite nivel, para que un ciclo sin nivel no aparente tener un dato sin rellenar
  - disparador: continuo
  - condición: el grado del ciclo admite nivel
