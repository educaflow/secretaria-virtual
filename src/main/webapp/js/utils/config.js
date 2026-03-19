const { protocol, hostname, port, pathname } = window.location;

const Config = {
    baseUrl: `${protocol}//${hostname}${port ? `:${port}` : ''}${pathname.replace(/\/[^/]*$/, '')}`,
};
