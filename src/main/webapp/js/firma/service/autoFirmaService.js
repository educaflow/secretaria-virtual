const AutoFirmaService = {

    cargarAppAutoFirma() {
        console.log('Cargando Autofirma');
        AutoScript.cargarAppAfirma();
        console.log('Cargado Autofirma');
        AutoScript.setStickySignatory(true);
    },

    descargarAppAutoFirma() {
        console.log('Descargando Autofirma');
        AutoScript.setStickySignatory(false);
    },

    firmarDocumento(base64Pdf, signatureOptions) {
        return new Promise((resolve, reject) => {
            const params = this._buildSignatureParams(signatureOptions);

            AutoScript.sign(
                base64Pdf,
                'SHA256withRSA',
                'PAdES',
                params,
                (signatureBase64) => {
                    console.log('Firma OK');
                    resolve(signatureBase64);
                },
                (errorType, errorMessage) => {
                    console.error('Error al firmar:', errorType, errorMessage);
                    reject(new Error(errorMessage));
                }
            );
        });
    },

    _buildSignatureParams(signatureOptions) {
        const { lowerLeftX, lowerLeftY, upperRightX, upperRightY } = signatureOptions.signaturePositionOnPage;

        let params =
            `mode=implicit\n` +  // ✅ firma local, sin servidor intermedio
            `signaturePositionOnPageLowerLeftX=${lowerLeftX}\n` +
            `signaturePositionOnPageLowerLeftY=${lowerLeftY}\n` +
            `signaturePositionOnPageUpperRightX=${upperRightX}\n` +
            `signaturePositionOnPageUpperRightY=${upperRightY}\n` +
            `signaturePage=${signatureOptions.pageNumber}\n` +
            `layer2FontSize=${signatureOptions.fontSize}`;

        if (signatureOptions.signReason?.trim()) {
            const safeReason = signatureOptions.signReason.replace(/[\n\r=]/g, ' ').trim();
            params += `\nsignReason=${safeReason}`;
        }

        if (signatureOptions.nif?.trim()) {
            const safeNif = signatureOptions.nif.replace(/[\n\r=*()]/g, '').trim();
            params +=
                `\nheadless=true` +
                `\nfilters.1=subject.rfc2254:(|(SERIALNUMBER=*${safeNif}*)(CN=*${safeNif}*));nonexpired:`;
        }

        return params;
    }

};
