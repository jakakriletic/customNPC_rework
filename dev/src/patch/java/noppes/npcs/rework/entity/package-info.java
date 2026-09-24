/**
 * M3 — jedro entitete: jahanje (R1) in kasneje trdni hitbox (R6).
 *
 * <p>{@link noppes.npcs.rework.entity.RiderState} je enoten vir resnice o jahanju: kaksno
 * vlogo ima entiteta (jahac, nosilec, oboje) in kdo krmili nosilca. Odlocitev je cista
 * funkcija brez Minecrafta, zato je enotsko testljiva.
 * {@link noppes.npcs.rework.entity.MountGuard} je edini del paketa, ki se dotika
 * Minecrafta: odlocitev izvede na nosilcu.
 *
 * <p>Privzeto je nacin {@link noppes.npcs.rework.entity.RiderState#ORIGINAL} (D-007):
 * obnasanje je enako originalu, cena je en branje staticnega polja na tick jahaca.
 */
package noppes.npcs.rework.entity;
