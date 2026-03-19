const FirmaService = {

    async firmar(signatureRequest) {
        const signedFileName = FileNameUtils.addSuffixBeforeExtension(signatureRequest.metaFileDocumentoOriginal.fileName, signatureRequest.sufijo);

        const base64DocumentoOriginal     = await MetaFileService.downloadMetaFile(signatureRequest.metaFileDocumentoOriginal.id,signatureRequest.metaFileDocumentoOriginal.version);

        const base64DocumentoFirmado = await AutoFirmaService.firmarDocumento(base64DocumentoOriginal, {
            signaturePositionOnPage: signatureRequest.signaturePositionOnPage,
            pageNumber:              signatureRequest.pageNumber,
            fontSize:                signatureRequest.fontSize,
            signReason:              signatureRequest.signReason,
            nif:                     signatureRequest.nif
        });



        const metaFileDocumentoFirmado = await MetaFileService.uploadMetaFile(base64DocumentoFirmado, signedFileName);

        return metaFileDocumentoFirmado;
    }

};
