


globalThis.firmaController = async function(context, payload) {
    try {
        const sourceField = payload.sourceField;
        const targetField = payload.targetField;

        const metaFileDocumentoOriginal = ObjectUtils.getValueByPath(context, sourceField);

        const signatureRequest = {
            metaFileDocumentoOriginal: {
                id: metaFileDocumentoOriginal.id,
                version: metaFileDocumentoOriginal.$version,
                fileName: metaFileDocumentoOriginal.fileName,
            },
            sufijo: payload.sufijo,

            signaturePositionOnPage: {
                lowerLeftX: payload.signaturePositionOnPageLowerLeftX,
                lowerLeftY: payload.signaturePositionOnPageLowerLeftY,
                upperRightX: payload.signaturePositionOnPageUpperRightX,
                upperRightY: payload.signaturePositionOnPageUpperRightY,
            },
            pageNumber: payload.pageNumber,
            fontSize: payload.fontSize,
            signReason: payload.motivo,
            nif: payload.nif
        };
        const metaFileDocumentoFirmado= await FirmaService.firmar(signatureRequest);

        ObjectUtils.setValueByPath(context, targetField, metaFileDocumentoFirmado);

        console.log('Documento firmado procesado correctamente');

    } catch (error) {
        console.error('Error en signDocument:', error);
        alert('Error al firmar el documento: ' + error.message);
    }
};
