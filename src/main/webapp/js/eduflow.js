const { protocol, hostname, port, pathname } = window.location;
const baseUrl = `${protocol}//${hostname}${port ? `:${port}` : ''}${pathname.replace(/\/[^/]*$/, '')}`;


globalThis.signDocument = async function(context, payload) {
    try {
        const source = context[payload.sourceField];
        const idSource = source.id;
        const versionSource = source.$version;
        const originalFileName = source.fileName;

        // 2. Crear expedienteContext local para no pisar variables globales
        const expedienteContext = {
            id: context.id,
            version: context.version,
            model: context._model,
            source: {
                id: idSource,
                version: versionSource,
                fileName: originalFileName,
                field: payload.sourceField,
                fieldClass:payload.sourceFieldClass
            },
            target: {
                field: payload.targetField,
                fieldClass: payload.targetFieldClass
            },
            sufijo: payload.sufijo,
            nif: payload.nif,
            signaturePositionOnPage: {
                "lowerLeftX": payload.signaturePositionOnPageLowerLeftX,
                "lowerLeftY": payload.signaturePositionOnPageLowerLeftY,
                "upperRightX": payload.signaturePositionOnPageUpperRightX,
                "upperRightY": payload.signaturePositionOnPageUpperRightY
            },
            pageNumber: payload.pageNumber,
            fontSize: payload.fontSize,
            signReason: payload.motivo
        };

        console.log("Contexto del expediente:", expedienteContext);

        // 3. Descargar PDF base64
        const urlPdf = `${baseUrl}/ws/rest/${expedienteContext.source.fieldClass}/${expedienteContext.source.id}/content/download?version=${expedienteContext.source.version}`;
        const base64Pdf = await fetchPdfBase64(urlPdf);
        console.log('PDF base64 descargado');

        // 4. Firmar PDF
        const firmaB64 = await firmarBase64(base64Pdf, expedienteContext);

        // 5. Subir PDF firmado
        target = await subirAFCTComoMetaFile(firmaB64, expedienteContext);

        console.log("Documento firmado procesado correctamente");
        context[payload.targetField] = target;
        //guardarYPresentar(context);

    } catch (error) {
        console.error("Error en signDocument:", error);
        alert("Error al firmar el documento: " + error.message);
    }
}

async function guardarYPresentar(context) {
    const newContext = { ...context };
    newContext._source = "PRESENTAR_DOCUMENTOS_FIRMADOS";
    newContext._signal = "PRESENTAR_DOCUMENTOS_FIRMADOS";
    const payload = {
        action: "action-event-expediente",
        model: newContext._model,
        data: {
            context: newContext
        }
    };

    console.log("Payload:", JSON.stringify(payload)); // Aquí puedes ver si es objeto o array


    const csrfToken = getCookie('CSRF-TOKEN');
    if (!csrfToken) throw new Error("CSRF-TOKEN no encontrado");

    const response = await fetch(`${baseUrl}/ws/action`, {
        method: "POST",
        body: JSON.stringify(payload),
        credentials: "include",
        headers: {
            "X-CSRF-TOKEN": csrfToken,
            "Content-Type": "application/json", // ← esto faltaba
            "Accept": "application/json"
        },
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(`Error subiendo a MetaFile: ${response.status} ${response.statusText} - ${text}`);
    }
    console.log("Expediente guardado y presentado correctamente");
    return response;
}


async function fetchPdfBase64(url) {
    const response = await fetch(url);
    if (!response.ok) throw new Error('Error al descargar PDF');
    const blob = await response.blob();

    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onloadend = () => {
            const base64data = reader.result.split(',')[1];
            resolve(base64data);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
    });
}

// Convertir firmarBase64 a async que devuelve promesa
function firmarBase64(base64Pdf, expedienteContext) {
    return new Promise((resolve, reject) => {
        AutoScript.cargarAppAfirma();

        const servletsBase = window.location.href.includes("/firmaMovil/")
            ? window.location.href.substring(0, window.location.href.indexOf("/firmaMovil/") + "/firmaMovil/".length - 1)
            : window.location.origin;

        let params = "mode=explicit\n" +
            "serverUrl=" + servletsBase + "/afirma-server-triphase-signer/SignatureService";

        params += "\n" +
                "signaturePositionOnPageLowerLeftX=" + expedienteContext.signaturePositionOnPage.lowerLeftX + "\n" +
                "signaturePositionOnPageLowerLeftY=" + expedienteContext.signaturePositionOnPage.lowerLeftY + "\n" +
                "signaturePositionOnPageUpperRightX=" + expedienteContext.signaturePositionOnPage.upperRightX + "\n" +
                "signaturePositionOnPageUpperRightY=" + expedienteContext.signaturePositionOnPage.upperRightY + "\n" +
                "signaturePage=" + expedienteContext.pageNumber + "\n" +
                "layer2FontSize=" + expedienteContext.fontSize + "";

        if (expedienteContext.signReason && expedienteContext.signReason.trim() !== "") {
            params += "\nsignReason=" + expedienteContext.signReason;
        }

        if (expedienteContext.nif && expedienteContext.nif.trim() !== "") {
            params += "\nheadless=true\nfilters.1=subject.rfc2254:(SERIALNUMBER=*" + expedienteContext.nif + "*);nonexpired:\nfilters.2=subject.rfc2254:(CN=*" + expedienteContext.nif + "*);nonexpired:";
        }
        AutoScript.sign(
            base64Pdf,
            "SHA256withRSA",
            "PAdES",
            params,
            (signatureB64, certB64, extraData) => {
                console.log("Firma OK");
                resolve(signatureB64);
            },
            (errorType, errorMessage) => {
                console.error("Error al firmar:", errorType, errorMessage);
                reject(new Error(errorMessage));
            }
        );
    });
}

async function subirAFCTComoMetaFile(base64Firmado, expedienteContext) {
    const blob = base64ToBlob(base64Firmado, "application/pdf");
    const fileName = agregarSufijoAntesDeExtension(
        expedienteContext.source.fileName,
        expedienteContext.sufijo
    );
    const fileType = "application/pdf";
    const fileSize = blob.size;

    const formData = new FormData();
    formData.append("file", blob, fileName);
    formData.append("field", expedienteContext.target.field);

    const requestPayload = {
        data: {
            fileName,
            fileType,
            fileSize,
            "$upload": { file: {} }
        }
    };
    formData.append("request", JSON.stringify(requestPayload));

    const csrfToken = getCookie('CSRF-TOKEN');
    if (!csrfToken) throw new Error("CSRF-TOKEN no encontrado");

    const response = await fetch(`${baseUrl}/ws/rest/${expedienteContext.target.fieldClass}/upload`, {
        method: "POST",
        body: formData,
        credentials: "include",
        headers: { "X-CSRF-TOKEN": csrfToken }
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(`Error subiendo a MetaFile: ${response.status} ${response.statusText} - ${text}`);
    }

    const metaFileData = await response.json();

    const mf = metaFileData?.data?.[0];

    if (!mf?.id || !mf?.filePath) throw new Error("MetaFile no válido");

    return {
        id: mf.id,
        $version: mf.version,
        fileName: mf.fileName,
        filePath: mf.filePath,
        fileType: mf.fileType,
        fileSize: mf.fileSize
    };
    //return actualizarFCTConMetaFile(metaFileId, expedienteContext);
}

function agregarSufijoAntesDeExtension(nombreArchivo, sufijo, extensionPorDefecto = ".pdf") {
    const dotIndex = nombreArchivo.lastIndexOf(".");
    const nombreSinExtension = dotIndex !== -1 ? nombreArchivo.substring(0, dotIndex) : nombreArchivo;
    const extension = dotIndex !== -1 ? nombreArchivo.substring(dotIndex) : extensionPorDefecto;

    return `${nombreSinExtension}${sufijo}${extension}`;
}


// Utilidades
function base64ToBlob(base64, mime) {
    const byteChars = atob(base64);
    const byteNumbers = new Array(byteChars.length);
    for (let i = 0; i < byteChars.length; i++) {
        byteNumbers[i] = byteChars.charCodeAt(i);
    }
    return new Blob([new Uint8Array(byteNumbers)], { type: mime });
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}
