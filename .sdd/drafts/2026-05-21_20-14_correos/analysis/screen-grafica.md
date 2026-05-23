# Pantalla: "Gráfica de correos"

## Identidad

- **Nombre**: "Gráfica de correos".
- **Quién la usa**: solo el **Administrador** (E-UN-008). El acceso restringido al Administrador se controla por el menú/rol que da entrada a esta pantalla.
- **Qué muestra**: datos **agregados** sobre los Correos del sistema en forma de gráfica de **barras apiladas por estado** entre dos fechas, con un selector de **granularidad temporal** (día, semana o mes). No es un listado de registros: no se muestran Correos individuales, sino el número de Correos por estado y por intervalo temporal dentro del rango de fechas indicado.
- **Parámetros que indica el usuario**: fecha inicial, fecha final y granularidad (día / semana / mes).

## Menú

- Correos → **Gráfica de correos** (visible solo para el Administrador).

## Estructura jerarquica de las pantallas

```
(sin entidad raíz persistida — datos agregados sobre Correo)
```

Esta pantalla no sigue el patrón Grid → Formulario de una entidad. No tiene entidad raíz persistida propia: presenta una agregación calculada sobre la entidad Correo (campos `estado` y `fechaCreacion`), agrupada por estado y por intervalo temporal dentro del rango de fechas elegido.

## Gráfica — "Gráfica de correos"

| Propiedad | Valor |
|-----------|-------|
| Tipo de gráfica | barras apiladas |
| Entidad de origen de los datos | Correo (agregado, no se listan registros) |
| Series / apilado por | estado (PENDIENTE, ENVIADO, FALLIDO) |
| Eje temporal | intervalos según la granularidad elegida (día / semana / mes), tomados sobre la fecha de creación del Correo |
| Métrica | número de Correos por estado y por intervalo |
| Parámetros de entrada | fecha inicial, fecha final, granularidad (día/semana/mes) |
| Rango | entre la fecha inicial y la fecha final indicadas |

### Reglas de UI / parámetros (U-grafica-NNN)

| ID | Parámetro / elemento | Descripción | Efecto | Mensaje al usuario | Origen EARS |
|----|----------------------|-------------|--------|--------------------|-------------|
| U-grafica-001 | granularidad | Selector de granularidad temporal con los valores día, semana y mes. El valor elegido determina la anchura de los intervalos del eje temporal sobre los que se agrupan los Correos. | Cambia el parámetro de granularidad y, con ello, el agrupamiento temporal de las barras apiladas. | — | E-OP-003, E-EV-008 |
| U-grafica-002 | fecha inicial, fecha final | Validación de parámetros de la gráfica: si la fecha final es anterior a la fecha inicial, el sistema rechaza la consulta y no la ejecuta. | No se ejecuta la consulta; se muestra un mensaje de error y la gráfica no se actualiza hasta corregir las fechas. | "La fecha final no puede ser anterior a la fecha inicial." | E-UN-010 |

NOTA sobre U-grafica-002: aunque las reglas de UI normalmente no bloquean en sentido estricto, esta es la única ubicación funcional natural para una validación de parámetros de una gráfica que no tiene entidad persistida detrás. Se documenta aquí como validación de los parámetros de entrada de la gráfica, describiendo el mensaje mostrado al usuario y dejando claro que la consulta no se ejecuta mientras las fechas sean incoherentes. Así queda cubierto E-UN-010.
