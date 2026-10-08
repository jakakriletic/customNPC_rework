// Model stevila ocen tipa vozlisca v vanilla isDirectPathBetweenPoints (1.12.2) na ravnih tleh.
// Tla: y-1 trden blok, y in vec zrak. Entiteta sirine 0,6 / visine 1,9 -> k = 1, l = 2.
// Steje: single = klici WalkNodeProcessor.getPathNodeType(world,x,y,z), reads = branja blokov,
// unique = razlicne (x,y,z) single ocene v enem klicu (S14b: pomnjenje za cas klica).
const GROUND = 64;
function single(x, y, z, st) {
  st.single++;
  st.keys.add(x + ',' + y + ',' + z);
  st.reads++; // raw
  if (y >= GROUND) { // zrak -> OPEN
    st.reads += 2; // blok pod + raw pod
    if (y === GROUND) { st.reads += 8; } // WALKABLE -> checkNeighborBlocks
  }
}
function box(x, y, z, sx, sy, sz, st) {
  for (let i = 0; i < sx; i++) for (let j = 0; j < sy; j++) for (let k = 0; k < sz; k++) single(x + i, y + j, z + k, st);
}
function safe(x, y, z, sx, sy, sz, ox, oz, dx, dz, st) {
  const i0 = x - Math.trunc(sx / 2), j0 = z - Math.trunc(sz / 2);
  // isPositionClear
  for (let a = i0; a < i0 + sx; a++) for (let b = y; b < y + sy; b++) for (let c = j0; c < j0 + sz; c++) {
    if ((a + 0.5 - ox) * dx + (c + 0.5 - oz) * dz >= 0) st.reads++;
  }
  for (let a = i0; a < i0 + sx; a++) for (let c = j0; c < j0 + sz; c++) {
    if ((a + 0.5 - ox) * dx + (c + 0.5 - oz) * dz >= 0) {
      box(a, y - 1, c, sx, sy, sz, st);
      box(a, y, c, sx, sy, sz, st);
    }
  }
}
function direct(ox, oy, oz, tx, tz, st) {
  let i = Math.floor(ox), j = Math.floor(oz);
  let d0 = tx - ox, d1 = tz - oz; const d2 = d0 * d0 + d1 * d1;
  if (d2 < 1e-8) return;
  const d3 = 1 / Math.sqrt(d2); d0 *= d3; d1 *= d3;
  safe(i, oy, j, 3, 2, 3, ox, oz, d0, d1, st);
  const d4 = 1 / Math.abs(d0), d5 = 1 / Math.abs(d1);
  let d6 = i - ox, d7 = j - oz; if (d0 >= 0) d6++; if (d1 >= 0) d7++;
  d6 /= d0; d7 /= d1;
  const k = d0 < 0 ? -1 : 1, l = d1 < 0 ? -1 : 1;
  const i1 = Math.floor(tx), j1 = Math.floor(tz);
  let k1 = i1 - i, l1 = j1 - j;
  while (k1 * k > 0 || l1 * l > 0) {
    if (d6 < d7) { d6 += d4; i += k; k1 = i1 - i; } else { d7 += d5; j += l; l1 = j1 - j; }
    safe(i, oy, j, 1, 2, 1, ox, oz, d0, d1, st);
  }
}
let seed = 12345; const rnd = () => (seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648;
for (const L of [1, 2, 4, 8, 16]) {
  let single = 0, reads = 0, uniq = 0, n = 2000, shared = new Set(), sharedSingle = 0;
  for (let t = 0; t < n; t++) {
    const ox = 100 + rnd(), oz = 100 + rnd(), a = rnd() * 2 * Math.PI;
    const st = { single: 0, reads: 0, keys: new Set() };
    direct(ox, GROUND, oz, ox + L * Math.cos(a), oz + L * Math.sin(a), st);
    single += st.single; reads += st.reads; uniq += st.keys.size;
  }
  console.log(`L=${L}: branj/klic ${(reads / n).toFixed(0)}, single/klic ${(single / n).toFixed(0)}, unikatnih/klic ${(uniq / n).toFixed(0)}, faktor ${(single / uniq).toFixed(2)}`);
}
// vec kandidatov v istem pathFollow: isti zacetek, razlicne smeri/razdalje (ciljne tocke poti)
for (const C of [1, 2, 4]) {
  let single = 0, uniq = 0, n = 2000;
  for (let t = 0; t < n; t++) {
    const ox = 100 + rnd(), oz = 100 + rnd();
    const st = { single: 0, reads: 0, keys: new Set() };
    let a = rnd() * 2 * Math.PI;
    for (let c = 0; c < C; c++) { a += (rnd() - 0.5) * 0.6; const L = 2 + c * 2; direct(ox, GROUND, oz, ox + L * Math.cos(a), oz + L * Math.sin(a), st); }
    single += st.single; uniq += st.keys.size;
  }
  console.log(`kandidatov=${C}: single/pathFollow ${(single / n).toFixed(0)}, unikatnih ${(uniq / n).toFixed(0)}, faktor ${(single / uniq).toFixed(2)}`);
}
