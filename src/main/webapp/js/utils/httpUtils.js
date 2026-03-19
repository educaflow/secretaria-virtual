const HttpUtils = {

    async post(url, body) {
        const csrfToken = CookieUtils.getCookie('CSRF-TOKEN');
        if (!csrfToken) {
            throw new Error('CSRF-TOKEN no encontrado');
        }

        const response = await fetch(url, {
            method: "POST",
            body: body,
            credentials: 'include',
            headers: {
                'X-CSRF-TOKEN': csrfToken
            }
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(`HTTP ${response.status} ${response.statusText} - ${text}`);
        }

        return response;
    },

    async getBlob(url) {
        const csrfToken = CookieUtils.getCookie('CSRF-TOKEN');
        if (!csrfToken) throw new Error('CSRF-TOKEN no encontrado');

        const response = await fetch(url,{
            method: "GET",
            credentials: 'include',
            headers: {
                'X-CSRF-TOKEN': csrfToken
            }
        });
        if (!response.ok) throw new Error(`Error al descargar recurso: ${url}`);
        const blob = await response.blob();
        return blob;
    },

};
