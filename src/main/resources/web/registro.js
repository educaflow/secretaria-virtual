const API = '/ws/public/registro';
let token = null;

// ── UI helpers ────────────────────────────────────────────────────────────────

const EYE = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
  <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
  <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
</svg>`;

const EYE_SLASH = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
  <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A5.944 5.944 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.134 13.134 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755-.165.165-.337.328-.517.486l.708.709z"/>
  <path d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829l.822.822zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829z"/>
  <path d="M3.35 5.47c-.18.16-.353.322-.518.487A13.134 13.134 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7.029 7.029 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709z"/>
  <path d="M13.646 14.354l-12-12 .708-.708 12 12-.708.708z"/>
</svg>`;

function togglePassword(id, btn) {
    const input = document.getElementById(id);
    const visible = input.type === 'text';
    input.type = visible ? 'password' : 'text';
    btn.innerHTML = visible ? EYE : EYE_SLASH;
    btn.setAttribute('aria-label', visible ? 'Mostrar contraseña' : 'Ocultar contraseña');
}

function mostrarError(msgs) {
    const box = document.getElementById('errorBox');
    const list = Array.isArray(msgs) ? msgs : [msgs];
    if (list.length === 1) {
        box.textContent = list[0];
    } else {
        box.innerHTML = '<ul class="mb-0 ps-3">' + list.map(m => `<li>${m}</li>`).join('') + '</ul>';
    }
    box.style.display = 'block';
}

function ocultarError() {
    document.getElementById('errorBox').style.display = 'none';
}

function setLoading(n, loading) {
    document.getElementById('btn' + n).disabled = loading;
    document.getElementById('spin' + n).classList.toggle('d-none', !loading);
}

function actualizarBarra(paso) {
    ['s1', 's2', 's3'].forEach((id, i) => {
        const el = document.getElementById(id);
        el.className = 'step ' + (i + 1 < paso ? 'done' : i + 1 === paso ? 'active' : '');
    });
}

function mostrarPaso(n) {
    [1, 2, 3].forEach(i => {
        document.getElementById('paso' + i).style.display = (i === n) ? '' : 'none';
    });
    actualizarBarra(n);
}

function reiniciar() {
    token = null;
    ocultarError();
    document.getElementById('email').value = '';
    document.getElementById('dni').value = '';
    document.getElementById('codigo').value = '';
    document.getElementById('stepBar').style.display = '';
    document.getElementById('success').style.display = 'none';
    mostrarPaso(1);
}

// ── API ───────────────────────────────────────────────────────────────────────

async function post(path, data) {
    const resp = await fetch(API + path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    const json = await resp.json();
    if (!resp.ok) throw Object.assign(new Error(), { errors: json.errors || ['Error desconocido.'] });
    return json;
}

// ── Pasos ─────────────────────────────────────────────────────────────────────

async function enviarPaso1() {
    ocultarError();
    const email   = document.getElementById('email').value.trim();
    const dni     = document.getElementById('dni').value.trim().toUpperCase();
    const tipoDoc = document.getElementById('tipoDoc').value;
    if (!email || !dni) { mostrarError('Rellene todos los campos.'); return; }

    setLoading(1, true);
    try {
        const res = await post('/registrosPendientes', { email, dni, tipoDoc });
        token = res.token;
        document.getElementById('emailMostrado').textContent = email;
        mostrarPaso(2);
    } catch (e) {
        mostrarError(e.errors || [e.message]);
    } finally {
        setLoading(1, false);
    }
}

async function enviarPaso2() {
    ocultarError();
    const codigo = document.getElementById('codigo').value.trim().toUpperCase();
    if (!codigo) { mostrarError('Introduzca el código.'); return; }

    setLoading(2, true);
    try {
        await post('/validarCodigo', { token, codigo });
        mostrarPaso(3);
    } catch (e) {
        mostrarError(e.errors || [e.message]);
    } finally {
        setLoading(2, false);
    }
}

async function enviarPaso3() {
    ocultarError();
    const nombre         = document.getElementById('nombre').value.trim();
    const apellidos      = document.getElementById('apellidos').value.trim();
    const password       = document.getElementById('password').value;
    const passwordRepeat = document.getElementById('passwordRepeat').value;
    const idioma         = document.getElementById('idioma').value;

    if (!nombre || !apellidos)  { mostrarError('Nombre y apellidos son obligatorios.'); return; }
    if (password.length < 8)    { mostrarError('La contraseña debe tener al menos 8 caracteres.'); return; }
    if (password !== passwordRepeat) { mostrarError('Las contraseñas no coinciden.'); return; }

    setLoading(3, true);
    try {
        await post('/usuarios', { token, nombre, apellidos, password, passwordRepeat, idioma });
        document.getElementById('stepBar').style.display = 'none';
        [1, 2, 3].forEach(i => document.getElementById('paso' + i).style.display = 'none');
        document.getElementById('success').style.display = '';
    } catch (e) {
        mostrarError(e.errors || [e.message]);
    } finally {
        setLoading(3, false);
    }
}

// ── Enter ─────────────────────────────────────────────────────────────────────

document.addEventListener('keydown', e => {
    if (e.key !== 'Enter') return;
    const pasoVisible = [1, 2, 3].find(i => document.getElementById('paso' + i).style.display !== 'none');
    if (pasoVisible === 1) enviarPaso1();
    else if (pasoVisible === 2) enviarPaso2();
    else if (pasoVisible === 3) enviarPaso3();
});