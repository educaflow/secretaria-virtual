#!/usr/bin/env python3
"""Tabla markdown alineada con los métodos de un crap.csv que superan el umbral, de más a menos CRAP.

Uso:
  tabla-crap.py <crap.csv> <umbral>                  -> todos los métodos con CRAP > umbral
  tabla-crap.py <crap-antes.csv> <crap.csv> <umbral> <N>
                                                     -> los N primeros de la tabla anterior (medida
                                                        sobre <crap-antes.csv>), con 3 columnas más
                                                        con sus valores en <crap.csv>

Cada método es un enlace `fichero:linea` a su definición (la línea sale del CSV más reciente).
Los métodos se emparejan entre los dos CSV por clase + metodo + descriptor, nunca por línea.
"""
import csv
import sys


def leer(ruta):
    with open(ruta, newline='', encoding='utf-8') as f:
        return list(csv.DictReader(f))


def clave(fila):
    return fila['clase'] + '#' + fila['metodo'] + fila['descriptor']


def nombre(fila):
    return fila['clase'].rsplit('.', 1)[-1] + '.' + fila['metodo']


def enlace(fila):
    return f"[`{nombre(fila)}`]({fila['fichero']}:{fila['linea']})"


def metricas(fila):
    if fila is None:
        return ['—', '—', '—']
    return [f"{float(fila['crap']):.2f}", fila['cc'], f"{round(float(fila['coberturaInstrucciones']) * 100)}%"]


def imprimir(cabecera, filas):
    anchos = [max([len(c)] + [len(f[i]) for f in filas]) for i, c in enumerate(cabecera)]
    # '#' y todas las métricas a la derecha; el método a la izquierda
    derecha = [i != 1 for i in range(len(cabecera))]

    def linea(celdas):
        return '| ' + ' | '.join(c.rjust(a) if d else c.ljust(a) for c, a, d in zip(celdas, anchos, derecha)) + ' |'

    print(linea(cabecera))
    print('|' + '|'.join(('-' * (a + 1) + ':') if d else ('-' * (a + 2)) for a, d in zip(anchos, derecha)) + '|')
    for f in filas:
        print(linea(f))


def main(args):
    if len(args) not in (2, 4):
        sys.exit(__doc__)
    umbral = float(args[1] if len(args) == 2 else args[2])
    antes = sorted((f for f in leer(args[0]) if float(f['crap']) > umbral),
                   key=lambda f: float(f['crap']), reverse=True)
    if len(args) == 2:
        filas = [[str(i), enlace(f), *metricas(f)] for i, f in enumerate(antes, 1)]
        imprimir(['#', 'Método', 'CRAP', 'CC', 'Cobertura'], filas)
        return
    despues = {clave(f): f for f in leer(args[1])}
    filas = []
    for i, f in enumerate(antes[:int(args[3])], 1):
        nueva = despues.get(clave(f))
        filas.append([str(i), enlace(nueva or f), *metricas(f), *metricas(nueva)])
    imprimir(['#', 'Método', 'CRAP', 'CC', 'Cobertura', 'CRAP nuevo', 'CC nuevo', 'Cobertura nueva'], filas)


if __name__ == '__main__':
    main(sys.argv[1:])
