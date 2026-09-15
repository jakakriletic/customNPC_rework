/**
 * M2.1 — diagnostika in instrumentacija.
 *
 * <p>Jedro tega paketa ({@link noppes.npcs.rework.diag.Diag},
 * {@link noppes.npcs.rework.diag.DiagKey},
 * {@link noppes.npcs.rework.diag.Distribution},
 * {@link noppes.npcs.rework.diag.DiagSnapshot}) namerno nima nobene odvisnosti na
 * Minecraft ali Forge, zato ga je mogoce testirati z navadnim JUnitom.
 *
 * <p>Razreda {@link noppes.npcs.rework.diag.DiagEventCollector} in
 * {@link noppes.npcs.rework.diag.CommandRwDiag} sta edina, ki se dotikata Minecrafta.
 *
 * <p>Instrumentacija je privzeto <b>izklopljena</b>. Dokler je izklopljena, zbiralnik
 * sploh ni prijavljen na Forge event bus, tako da je cena v navadnem obratovanju nic.
 * Vklopi se z ukazom {@code /rwdiag on} ali z lastnostjo JVM {@code -Drwdiag=on}.
 */
package noppes.npcs.rework.diag;
