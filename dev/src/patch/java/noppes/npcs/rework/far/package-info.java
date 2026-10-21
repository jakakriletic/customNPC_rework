/**
 * M5-V: vidnost NPC-jev na daljavo (D-034, D-035; raziskava {@code docs/raziskave/M5-V.0-daljna-vidnost.md}).
 *
 * <p>Vanilla sledilnik NPC-je klientu poslje samo do 64 blokov (CNPC registrira {@code tracker(64, 3,
 * true)}); dvig dometa (varianta A) je M5-V.1 izmeril in zavrnil (D-043). Varianta B: streznik NPC-je v
 * pasu do {@code min(RwFarRange, (view-distance - 1) * 16)} poslje kot lahke nadomestke (~16 B, ~1 Hz,
 * samo spremembe) po lastnem kanalu {@code rwfar}, samo klientom, ki to zahtevajo ({@code HELLO}).
 * M5-V.2 je streznik ({@link noppes.npcs.rework.far.FarServer}, {@link noppes.npcs.rework.far.FarRegistry})
 * in klientska shramba brez izrisa ({@link noppes.npcs.rework.far.FarClient}).
 */
package noppes.npcs.rework.far;
