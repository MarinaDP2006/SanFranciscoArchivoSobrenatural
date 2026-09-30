// Mapa 2D oscuro - San Francisco Archive
const map = L.map("map", { zoomControl: true, attributionControl: false })
             .setView([40.4168, -3.7038], 3);

L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", {
  subdomains: "abcd", maxZoom: 19,
}).addTo(map);

async function cargarIncidentes() {
  const r = await fetch("./data/mock-incidents.json");
  const lista = await r.json();
  lista.forEach(i => {
    const color = i.tipo === "ASESINATO" ? "#ff3b3b"
                : i.tipo === "SECUESTRO" ? "#ffb020" : "#00d4ff";
    L.circleMarker([i.lat, i.lon], {
      radius: 7, color, fillColor: color, fillOpacity: 0.7, weight: 1,
    }).addTo(map).bindPopup(`<b>${i.tipo}</b><br>${i.ciudad}, ${i.pais}`);
  });
}
cargarIncidentes();

// Auto-refresh cada 30s (futuro: llamará a la API del VPS)
setInterval(cargarIncidentes, 30000);
