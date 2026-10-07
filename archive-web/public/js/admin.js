import { CONFIG } from "./config.js";

async function api(path, options = {}) {
  const response = await fetch(`${CONFIG.API_BASE}${path}`, {
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  const text = await response.text();
  let payload = {};
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = { error: text };
    }
  }

  if (!response.ok) {
    throw new Error(payload.error || `Error ${response.status}`);
  }

  return payload;
}

function setStatus(message, ok = true) {
  const status = document.querySelector("#login-status");
  if (!status) return;
  status.className = `alert ${ok ? "ok" : "error"}`;
  status.textContent = message;
}

async function cargarDashboard() {
  try {
    const session = await api("/admin/session");
    const stats = await api("/estadisticas");
    const usuario = session.usuario;

    const role = usuario.rol === "ADMIN" ? "Administrador" : "Potencial";
    document.querySelector("#admin-role").textContent = role;
    document.querySelector("#admin-user").textContent = `${usuario.nombre} · ${usuario.username}`;

    const tipoStats = stats.porTipo || {};
    document.querySelector("#stat-total").textContent = stats.total ?? 0;
    document.querySelector("#stat-SECUESTRO").textContent = tipoStats.SECUESTRO ?? 0;
    document.querySelector("#stat-ASESINATO").textContent = tipoStats.ASESINATO ?? 0;
    document.querySelector("#stat-DESAPARICION").textContent = tipoStats.DESAPARICION ?? 0;
  } catch (error) {
    window.location.href = "admin-login.html";
  }
}

function bindLoginForm() {
  const form = document.querySelector("#login-form");
  if (!form) return;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(form);
    const username = String(formData.get("username") || "").trim();
    const password = String(formData.get("password") || "");
    const button = form.querySelector("button[type='submit']");

    if (!username || !password) {
      setStatus("Usuario y contraseña son obligatorios.", false);
      return;
    }

    button.disabled = true;
    setStatus("Comprobando credenciales…", true);

    try {
      await api("/admin/login", {
        method: "POST",
        body: JSON.stringify({ username, password })
      });
      window.location.href = "admin.html";
    } catch (error) {
      setStatus(error.message || "No se pudo iniciar sesión.", false);
      button.disabled = false;
    }
  });
}

function bindLogout() {
  const button = document.querySelector("#logout-btn");
  if (!button) return;

  button.addEventListener("click", async () => {
    try {
      await api("/admin/logout", { method: "POST" });
      window.location.href = "admin-login.html";
    } catch {
      window.location.href = "admin-login.html";
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  bindLoginForm();
  bindLogout();

  if (document.body.dataset.page === "admin") {
    cargarDashboard();
  }
});
