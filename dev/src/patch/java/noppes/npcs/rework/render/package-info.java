/**
 * M5-K: klientska upodobitev NPC-jev z manj klici na NPC, z isto sliko.
 *
 * <p>Profil render niti M5-K.1 (18. 10.) je pokazal, da je pri 200 in 500 vidnih NPC-jih ~90 %
 * casa slicice v upodabljanju NPC-jev in da je lastni cas v nativnih klicih GL na NPC
 * ({@code glCallList}, {@code glPushMatrix}), ne v GPU. Razredi tega paketa zmanjsajo stevilo
 * teh klicev, ne da bi spremenili ukaze, ki jih dobi gonilnik.
 */
package noppes.npcs.rework.render;
