package com.educaflow.subsystem.pdfutilities.actions;

import com.axelor.meta.CallMethod;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.educaflow.base.infrastructure.criptografia.AlmacenClave;
import com.educaflow.base.infrastructure.metafile.MetaFileHelper;
import com.educaflow.base.infrastructure.pdf.CampoFirma;
import com.educaflow.base.infrastructure.pdf.DocumentoPdf;
import com.educaflow.base.infrastructure.pdf.Rectangulo;
import com.educaflow.base.util.ActionRequestHelper;
import com.educaflow.base.util.Convert;
import com.educaflow.subsystem.certificados.AlmacenClaveLoader;
import com.educaflow.subsystem.pdfutilities.db.PdfUtilities;
import com.educaflow.base.infrastructure.autofirma.AutoFirma;
import jakarta.inject.Inject;

public class PdfUtilitiesActions {

    @Inject
    AlmacenClaveLoader almacenClaveLoader;


    @CallMethod
    public String getInfo(MetaFile metaFilePdf) {
        String info = "Sin información";

        if (metaFilePdf != null) {
            DocumentoPdf documentoPdf = MetaFileHelper.getDocumentoPdf(metaFilePdf);

            info=documentoPdf.toString();
        }

        return info;
    }


    @CallMethod
    public MetaFile getPdfTodasPosicionesFirma(MetaFile metaFilePdf) {
        MetaFile metaFilePdfFirmado = null;

        if (metaFilePdf != null) {
            DocumentoPdf documentoPdf = MetaFileHelper.getDocumentoPdf(metaFilePdf);


            for(int x=0;x<=500;x+=100) {
                for(int y=0;y<=700;y+=50) {
                    CampoFirma campoFirma=new CampoFirma(new Rectangulo(x,y,100,20)).setNumeroPagina(1).setMensaje(x+"," + y);
                    AlmacenClave almacenClave=almacenClaveLoader.getDummy();
                    documentoPdf=documentoPdf.firmar(almacenClave,campoFirma);
                }
            }

            metaFilePdfFirmado=MetaFileHelper.createMetaFile(documentoPdf);
        }

        return metaFilePdfFirmado;
    }



    @CallMethod
    public void pdfAutoFirma(ActionRequest actionRequest, ActionResponse actionResponse) {

        ActionRequestHelper requestHelper=new ActionRequestHelper(actionRequest,PdfUtilities.class);

        int x= Convert.coerceToInt(requestHelper.getRequestData().get("x"));
        int y=Convert.coerceToInt(requestHelper.getRequestData().get("y"));;
        int width=Convert.coerceToInt(requestHelper.getRequestData().get("width"));;
        int height=Convert.coerceToInt(requestHelper.getRequestData().get("height"));;

        if (width==0) {
            width=200;
        }

        if (height==0) {
            height=100;
        }
        AutoFirma autofirma = (new AutoFirma(PdfUtilities.class))
                .setRectangulo(new Rectangulo(x,y,width,height))
                .setPageNumber(1)
                .addSourceTargetField("pdf","pdfFirmado")
                .setMotivo(x+","+y);

        AutoFirma.sendToActionResponse(autofirma,actionResponse);
    }
}
