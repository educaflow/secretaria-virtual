const FileNameUtils = {

    addSuffixBeforeExtension(fileName, suffix, defaultExtension = '.pdf') {
        const dotIndex = fileName.lastIndexOf('.');
        const name = dotIndex !== -1 ? fileName.substring(0, dotIndex) : fileName;
        const extension = dotIndex !== -1 ? fileName.substring(dotIndex) : defaultExtension;
        return `${name}${suffix}${extension}`;
    },

};
