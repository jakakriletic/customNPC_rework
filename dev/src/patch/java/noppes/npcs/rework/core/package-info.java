/**
 * M5.15 (D-031): lasten FML coremod, zapakiran v CNPC jar.
 *
 * <p>Zakaj: najvecji preostanek idle ticka je vanilla {@code Entity.move} (26-29 % CPU strezniske
 * niti, od tega poizvedba entitet za trke 11,8 %), ki ga CNPC prek lastnih razredov ne doseze.
 * D-031 dovoli poseg v vanilla razrede, ce je zapakiran v CNPC jar: brez novega obveznega moda in
 * brez vgrajene kopije Mixina (ta bi trcila z modpacki, ki Mixin ze nalozijo).
 *
 * <p>Ta paket je **samo infrastruktura**: transformer ne spremeni nicesar. Prvi pravi poseg je
 * M5.16.
 */
package noppes.npcs.rework.core;
