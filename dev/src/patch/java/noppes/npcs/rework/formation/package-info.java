/**
 * M4.14 — formacije: skupina NPC-jev se premika kot ena enota.
 *
 * <p>Jedro ({@link noppes.npcs.rework.formation.SquadPlanner},
 * {@link noppes.npcs.rework.formation.FormationShape},
 * {@link noppes.npcs.rework.formation.SlotAssigner},
 * {@link noppes.npcs.rework.formation.PathTrack},
 * {@link noppes.npcs.rework.formation.FormationMath}) nima nobene odvisnosti na Minecraft
 * in je testirano z navadnim JUnitom v simulaciji.
 *
 * <p>Minecrafta se dotikajo {@link noppes.npcs.rework.formation.Squad},
 * {@link noppes.npcs.rework.formation.SquadManager},
 * {@link noppes.npcs.rework.formation.FormationMoveTask},
 * {@link noppes.npcs.rework.formation.WorldTerrain},
 * {@link noppes.npcs.rework.formation.CommandRwSquad} in
 * {@link noppes.npcs.rework.formation.FormationApi}.
 *
 * <p>Funkcija je vklopljena samo z ukazom {@code /rwsquad} ali klicem iz skripte; brez tega
 * paket ni prijavljen na event bus in ne doda nobenega AI taska. Zasnova:
 * {@code docs/06-FORMACIJE.md}.
 */
package noppes.npcs.rework.formation;
