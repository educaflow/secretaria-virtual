const ObjectUtils = {

    getValueByPath(obj, path) {
        const partesPath=this._parsePath(path);

        return partesPath.reduce((current, key) => {
            if (current === null || current === undefined) return undefined;
            return current[key];
        }, obj);
    },
    setValueByPath(obj, path, value) {
        const keys = this._parsePath(path);
        const lastKey = keys.pop();

        const target = keys.reduce((current, key, i) => {
            if (current[key] === null || current[key] === undefined) {
                // Create array if the NEXT key is a numeric index, object otherwise
                const nextKey = keys[i + 1] ?? lastKey;
                current[key] = typeof nextKey === 'number' ? [] : {};
            }
            return current[key];
        }, obj);

        target[lastKey] = value;
    },
    _parsePath(path) {
        return path
            .replace(/\[(\d+)\]/g, '.$1') // convert [2] → .2
            .split('.')
            .filter(Boolean)
            .map(part => (/^\d+$/.test(part) ? Number(part) : part));
    }
};
