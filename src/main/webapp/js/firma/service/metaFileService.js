const MetaFileService = {

    async uploadMetaFile(base64Firmado, fileName) {
        const blob = Base64Utils.base64ToBlob(base64Firmado, 'application/pdf');
        const uploadUrl= this._getUploadUrl();

        const bodyData = this._createBodyData(blob, fileName);

        const response = await HttpUtils.post(uploadUrl, bodyData);

        const jsonResponse = await response.json();
        const metaFile=this._getMetaFileFromResponse(jsonResponse)
        return metaFile;
    },
    async downloadMetaFile(id,version) {
        const dowloadUrl=this._getDownloadUrl(id,version)
        const blob     = await HttpUtils.getBlob(dowloadUrl);
        const base64MetaFile= await Base64Utils.blobToBase64(blob);
        return base64MetaFile;
    },




    _createBodyData(blob, fileName) {
        const formData = new FormData();
        formData.append('file', blob, fileName);
        formData.append('field', null); //El campo es requerido en AOP aunque esté a null
        formData.append('request', JSON.stringify({
            data: {
                fileName,
                fileType: 'application/pdf',
                fileSize: blob.size,
                '$upload': { file: {} },
            },
        }));
        return formData;
    },
    _getMetaFileFromResponse(jsonResponse) {
        const mf = jsonResponse?.data?.[0];
        if (!mf?.id || !mf?.filePath) throw new Error('MetaFile no válido');

        return {
            id: mf.id,
            $version: mf.version,
            fileName: mf.fileName,
            filePath: mf.filePath,
            fileType: mf.fileType,
            fileSize: mf.fileSize,
        };
    },
    _getDownloadUrl(id,version) {
        return `${Config.baseUrl}/ws/rest/com.axelor.meta.db.MetaFile/${id}/content/download?version=${version}`;
    },
    _getUploadUrl() {
        return `${Config.baseUrl}/ws/rest/com.axelor.meta.db.MetaFile/upload`;
    }
};