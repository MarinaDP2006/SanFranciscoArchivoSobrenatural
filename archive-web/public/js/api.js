import { CONFIG } from "./config.js";

export async function getFeedPublico() {
  const r = await fetch(`${CONFIG.API_BASE}/incidentes/publicos`);
  if (!r.ok) throw new Error("FEED ERROR");
  return r.json();
}

export async function getContratosReporter(token) {
  const r = await fetch(`${CONFIG.API_BASE}/contratos/mios`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return r.json();
}

export async function crearContrato(payload, token) {
  const r = await fetch(`${CONFIG.API_BASE}/contratos`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });
  return r.json();
}
