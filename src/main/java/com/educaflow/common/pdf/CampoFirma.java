package com.educaflow.common.pdf;

import java.time.LocalDateTime;

/**
 *
 * @author logongas
 */
public class CampoFirma {

    public final static int DEFAULT_FONT_SIZE=8;
    public final static int DEFAULT_NUMERO_PAGINA=-1;

    private String mensaje=null;
    private String motivo=null;
    private Rectangulo rectanguloMensaje=null;
    private int fontSize=DEFAULT_FONT_SIZE;
    private int numeroPagina=DEFAULT_NUMERO_PAGINA;
    private byte[] image=null;
    private LocalDateTime fechaFirma= LocalDateTime.now();

    public CampoFirma(Rectangulo rectanguloMensaje) {
        this.rectanguloMensaje=rectanguloMensaje;
    }


    public CampoFirma setMensaje(String mensaje) {
        this.mensaje=mensaje;
        return this;
    }

    public CampoFirma setMotivo(String motivo) {
        this.motivo=motivo;
        return this;
    }

    public CampoFirma setFontSize(int fontSize) {
        this.fontSize=fontSize;
        return this;
    }

    public CampoFirma setNumeroPagina(int numeroPagina) {
        this.numeroPagina=numeroPagina;
        return this;
    }
    public CampoFirma setRectanguloMensaje(Rectangulo rectanguloMensaje) {
        this.rectanguloMensaje=rectanguloMensaje;
        return this;
    }

    public CampoFirma setImage(byte[] image) {
        this.image=image;
        return this;
    }

    public CampoFirma setFechaFirma(LocalDateTime fechaFirma) {
        if (fechaFirma==null) {
            throw new IllegalArgumentException("Fecha de firma no puede ser nulo");
        }
        this.fechaFirma=fechaFirma;
        return this;
    }

    /**
     * @return the mensaje
     */
    public String getMensaje() {
        return mensaje;
    }

    public String getMotivo() {
        return motivo;
    }

    /**
     * @return the rectanguloMensaje
     */
    public Rectangulo getRectanguloMensaje() {
        return rectanguloMensaje;
    }

    /**
     * @return the fontSize
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * @return the numerpoPagina
     */
    public int getNumeroPagina() {
        return numeroPagina;
    }

    public byte[] getImage() {
        return image;
    }

    public LocalDateTime getFechaFirma() {
        return fechaFirma;
    }
    
}
