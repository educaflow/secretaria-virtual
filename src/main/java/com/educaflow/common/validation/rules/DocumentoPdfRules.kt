package com.educaflow.common.validation.rules

import com.educaflow.common.domains.db.MetaFilePdf
import com.educaflow.common.pdf.DocumentoPdfUtil
import com.educaflow.common.validation.engine.ValidationRule
import com.educaflow.common.validation.messages.BusinessMessage
import com.educaflow.common.validation.messages.BusinessMessages

class DocumentoPdfFirmaValida : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? {
        val metaFilePdf: MetaFilePdf = if (value is MetaFilePdf) value else return null

        val documentoPdf=metaFilePdf.documentoPdf;

        val businessMessages=BusinessMessages();

        for(resultadoFirma in documentoPdf.firmasPdf){
            if (resultadoFirma.isCorrecta() == false) {
                businessMessages.add(BusinessMessage(null,"La firma realizada por '${resultadoFirma.datosCertificado.cnSubject}' está dañada o se ha alterado el documento",null))
            } else if (resultadoFirma.datosCertificado.isValidoEnListaCertificadosConfiables==false) {
                businessMessages.add(BusinessMessage(null,"La firma realizada por '${resultadoFirma.datosCertificado.cnSubject}' no es válida ya que el emisor no es de confianza: '${resultadoFirma.datosCertificado.cnIssuer}'",null));
            }
        }

        if (businessMessages.isEmpty()==false) {
            return businessMessages;
        } else {
            return null;
        }

    }
}

