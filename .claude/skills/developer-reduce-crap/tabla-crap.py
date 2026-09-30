#!/usr/bin/env python3
"""Tabla markdown alineada con los métodos de mayor CRAP de un crap.csv.

Uso:
  tabla-crap.py <crap.csv> [N]                  -> los N métodos con más CRAP (10 por defecto)
  tabla-crap.py <crap-antes.csv> <crap.csv> [N] -> los mismos N métodos de antes, con 3 columnas
                                                   más con sus valores en <crap.csv>

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
    anchos = [max(len(c), *(len(f[i]) for f in filas)) for i, c in enumerate(cabecera)]
    # '#' y todas las métricas a la derecha; el método a la izquierda
    derecha = [i != 1 for i in range(len(cabecera))]

    def linea(celdas):
        return '| ' + ' | '.join(c.rjust(a) if d else c.ljust(a) for c, a, d in zip(celdas, anchos, derecha)) + ' |'

    print(linea(cabecera))
    print('|' + '|'.join(('-' * (a + 1) + ':') if d else ('-' * (a + 2)) for a, d in zip(anchos, derecha)) + '|')
    for f in filas:
        print(linea(f))


def main(args):
    n = 10
    if args and args[-1].isdigit():
        n = int(args.pop())
    if len(args) not in (1, 2):
        sys.exit(__doc__)
    antes = sorted(leer(args[0]), key=lambda f: float(f['crap']), reverse=True)[:n]
    if len(args) == 1:
        filas = [[str(i), enlace(f), *metricas(f)] for i, f in enumerate(antes, 1)]
        imprimir(['#', 'Método', 'CRAP', 'CC', 'Cobertura'], filas)
        return
    despues = {clave(f): f for f in leer(args[1])}
    filas = []
    for i, f in enumerate(antes, 1):
        nueva = despues.get(clave(f))
        filas.append([str(i), enlace(nueva or f), *metricas(f), *metricas(nueva)])
    imprimir(['#', 'Método', 'CRAP', 'CC', 'Cobertura', 'CRAP nuevo', 'CC nuevo', 'Cobertura nueva'], filas)


if __name__ == '__main__':
    main(sys.argv[1:])
