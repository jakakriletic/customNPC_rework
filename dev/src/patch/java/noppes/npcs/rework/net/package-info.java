/**
 * M5.13: prejemniki paketov CNPC-ja.
 *
 * <p>Original poisce prejemnike paketa s poizvedbo po kvadru okoli entitete
 * ({@code World.getEntitiesWithinAABB(EntityPlayerMP.class, ...)}), ki pregleda vse chunke v
 * kvadru. Pri utripu oci je kvader 160 blokov (~441 chunkov) in je po profilu JFR (8. 10.)
 * 6,0-6,5 % CPU strezniske niti. Paket vsebuje enakovredno iskanje po {@code world.playerEntities}.
 */
package noppes.npcs.rework.net;
