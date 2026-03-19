const Base64Utils = {

    async blobToBase64(blob) {
        const buffer = await blob.arrayBuffer();
        const bytes = new Uint8Array(buffer);
        let binary = '';
        bytes.forEach(b => binary += String.fromCharCode(b));
        return btoa(binary);
    },

    base64ToBlob(base64, mime) {
        const byteChars = atob(base64);
        const byteNumbers = new Array(byteChars.length);
        for (let i = 0; i < byteChars.length; i++) {
            byteNumbers[i] = byteChars.charCodeAt(i);
        }
        return new Blob([new Uint8Array(byteNumbers)], { type: mime });
    },

};
