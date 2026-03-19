const ObjectUtils = {

    getValueByPath(obj, path) {
        return path.split('.').reduce((current, part) => {
            if (current === null || current === undefined) return undefined;
            return current[part];
        }, obj);
    },

    setValueByPath(obj, path, value) {
        const parts = path.split('.');
        const last = parts.pop();
        const target = parts.reduce((current, part) => {
            if (!(part in current)) current[part] = {};
            return current[part];
        }, obj);
        target[last] = value;
    },

};
