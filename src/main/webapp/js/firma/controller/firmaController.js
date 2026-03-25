


globalThis.firmaController = async function(context, payload) {
    try {
        const sourceTargetFields = payload.sourceTargetFields;

        FirmaService.cargarAppFirma();

        for (const sourceTargetField of sourceTargetFields) {
            const sourceField = sourceTargetField.sourceField;
            const targetField = sourceTargetField.targetField;

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


            const metaFileDocumentoFirmado = await FirmaService.firmar(signatureRequest);
            ObjectUtils.setValueByPath(context, targetField, metaFileDocumentoFirmado);
            console.log('Documento firmado procesado correctamente');

        };

        FirmaService.descargarAppFirma();




    } catch (error) {
        console.error('Error en signDocument:', error);
        alert('Error al firmar el documento: ' + error.message);
    }
};

