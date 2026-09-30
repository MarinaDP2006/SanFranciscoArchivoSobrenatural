async function renderFeed() {
  const r = await fetch("./data/mock-incidents.json");
  const lista = await r.json();
  const feed = document.getElementById("feed");
  feed.innerHTML = lista.map(i => `
    <div class="card">
      <div class="tipo">${i.tipo}</div>
      <div class="lugar">${i.ciudad}, ${i.pais} — ${i.fecha}</div>
    </div>
  `).join("");
}
renderFeed();
