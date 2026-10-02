package com.educaflow.base.infrastructure.pdf;

import java.time.LocalDateTime;
import com.educaflow.base.util.Convert;

/**
 *
 * @author logongas
 */
public class CampoFirma {

    public final static int DEFAULT_FONT_SIZE=8;
    public final static int DEFAULT_NUMERO_PAGINA=-1;

    private String nombreCampo=null;
    private String mensaje=null;
    private String motivo=null;
    private Rectangulo rectanguloMensaje=null;
    private int fontSize=DEFAULT_FONT_SIZE;
    private int numeroPagina=DEFAULT_NUMERO_PAGINA;
    private byte[] image=null;
    private PosicionImagen posicionImagen=null;
    private LocalDateTime fechaFirma= LocalDateTime.now(Convert.defaultZoneId);

    /** Firma en un rectángulo de una página (la última, si no se indica otra con {@link #setNumeroPagina(int)}). */
    public CampoFirma(Rectangulo rectanguloMensaje) {
        this.rectanguloMensaje=rectanguloMensaje;
    }

    /**
     * Firma en un campo de firma vacío que ya existe en el PDF: la posición y la página son las del
     * campo, así que no hay rectángulo y el número de página se ignora.
     */
    public CampoFirma(String nombreCampo) {
        if ((nombreCampo==null) || nombreCampo.isBlank()) {
            throw new IllegalArgumentException("El nombre del campo de firma no puede estar vacio");
        }
        this.nombreCampo=nombreCampo;
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

    /**
     * Añade una imagen a la firma. El recuadro de la firma no crece: la imagen ocupa, dentro de él,
     * el sitio que deja libre el mensaje, en el lado que indica {@code posicionImagen}.
     */
    public CampoFirma setImage(byte[] image, PosicionImagen posicionImagen) {
        if (image==null) {
            throw new IllegalArgumentException("La imagen no puede ser nula");
        }
        if (posicionImagen==null) {
            throw new IllegalArgumentException("La posicion de la imagen no puede ser nula");
        }
        this.image=image;
        this.posicionImagen=posicionImagen;
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
     * @return el nombre del campo de firma existente en el que se firma, o null si se firma en un rectángulo
     */
    public String getNombreCampo() {
        return nombreCampo;
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

    /**
     * @return dónde va la imagen respecto al mensaje, o null si la firma no lleva imagen
     */
    public PosicionImagen getPosicionImagen() {
        return posicionImagen;
    }

    public LocalDateTime getFechaFirma() {
        return fechaFirma;
    }
    
}
