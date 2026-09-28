package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.TextoBilingue;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.modelo.Visibilidad;
import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.texto.Fuente;

import java.util.List;

/** Un {@code <parrafo>} o el {@code <item>} de una lista. */
public record Texto(TipoTexto tipo, TextoBilingue textos, Alineacion alineacion,
                    boolean negrita, boolean mayusculas, Visibilidad visibilidad) implements Bloque {

    /** La fuente con la que se estampa el texto; el valor de sus inline no la usa. */
    public Fuente fuente() {
        return negrita ? Fuente.SEMINEGRITA : Fuente.REGULAR;
    }

    @Override
    public List<Elemento> hijos() {
        return List.of();
    }

    @Override
    public List<String> expresionesInline() {
        return textos.expresionesInline();
    }
}
