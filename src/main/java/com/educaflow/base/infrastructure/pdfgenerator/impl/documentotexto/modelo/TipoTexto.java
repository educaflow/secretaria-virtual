package com.educaflow.base.infrastructure.pdfgenerator.impl.documentotexto.modelo;

import com.educaflow.base.infrastructure.pdfgenerator.impl.comun.dibujo.Alineacion;

/** Los dos elementos del XML que son un texto suelto, y lo que los distingue al dibujarlos. */
public enum TipoTexto {
    PARRAFO(Alineacion.JUSTIFICADO),
    /** El {@code <item>} de una lista: va sangrado y detrás de su viñeta. */
    ITEM(Alineacion.IZQUIERDA);

    private final Alineacion alineacionPorDefecto;

    TipoTexto(Alineacion alineacionPorDefecto) {
        this.alineacionPorDefecto = alineacionPorDefecto;
    }

    public Alineacion getAlineacionPorDefecto() {
        return alineacionPorDefecto;
    }
}
