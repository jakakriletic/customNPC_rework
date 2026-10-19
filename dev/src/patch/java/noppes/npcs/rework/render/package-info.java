/**
 * M5-K: klientska upodobitev NPC-jev z manj dela na NPC, z isto sliko.
 *
 * <p>Profil render niti M5-K.1 (18. 10.) je pokazal, da je pri 200 in 500 vidnih NPC-jih ~90 %
 * casa slicice v upodabljanju NPC-jev, lastni cas pa v nativnih klicih GL na NPC. K14 (en list
 * pokrivala) je pokazal, da pri pokrivalu steje geometrija (816 oglisc na NPC), ne stevilo klicev;
 * K14b ({@link noppes.npcs.rework.render.HeadwearSkip}) izpusti geometrijo, ki ne pusti fragmenta.
 */
package noppes.npcs.rework.render;
