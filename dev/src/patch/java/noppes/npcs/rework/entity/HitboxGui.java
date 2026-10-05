package noppes.npcs.rework.entity;

import net.minecraft.nbt.NBTTagCompound;

/**
 * M3.8b: nacin hitboxa v GUI-ju Display (zavihek "Display" urejevalnika NPC-ja).
 *
 * <p>Pot podatkov brez novih paketov (packet ID-ji ostanejo):
 * <ul>
 *   <li>server -> klient: spawn podatki NPC-ja nosijo shranjeni nacin ({@link #KEY_STORED}), samo
 *       ce ni ORIGINAL - privzeti NPC ima spawn podatke enake originalu. Klient ima v
 *       {@code RwHitboxMode} samo ucinkovit nacin (ORIGINAL pri izklopljenem stikalu {@code RwHitbox}
 *       ali brez hitboxa); GUI bi z njim ob shranjevanju povozil shranjeni nacin;</li>
 *   <li>klient -> server: obstojeci paket {@code MainmenuDisplaySave} z NBT-jem {@code DataDisplay}
 *       in dodatnim kljucem {@link #KEY_GUI}. Server ga uposteva samo, ce kljuc obstaja: ostali
 *       klici {@code DataDisplay.readToNBT} (urejevalnik modela, transformacija, povezani NPC-ji,
 *       nalaganje) ga nimajo in nacina ne spremenijo.</li>
 * </ul>
 */
public final class HitboxGui {
    /** Zahteva iz GUI-ja Display; samo v paketu MainmenuDisplaySave. */
    public static final String KEY_GUI = "RwHitboxGui";
    /** Shranjeni nacin v spawn podatkih (samo, ce ni ORIGINAL). */
    public static final String KEY_STORED = "RwHitboxStored";

    private static final int NBT_NUMBER = 99;
    private static final String[] LABELS = {"Original", "Solid", "Smart"};
    /** Namig ob gumbu: nacin velja samo z globalnim stikalom (klient stikala ne pozna). */
    public static final String HINT = "only with RwHitbox=1";

    private HitboxGui() {
    }

    /** Napisi gumba v vrstnem redu nacinov (indeks = nacin). */
    public static String[] labels() {
        return LABELS.clone();
    }

    public static void writeRequest(NBTTagCompound compound, int mode) {
        compound.setInteger(KEY_GUI, HitboxWeights.sanitize(mode));
    }

    /** Zahtevani nacin ali -1, ce zahteve ni (NBT ni prisel iz GUI-ja Display). */
    public static int readRequest(NBTTagCompound compound) {
        if (!compound.hasKey(KEY_GUI, NBT_NUMBER)) {
            return -1;
        }
        return HitboxWeights.sanitize(compound.getInteger(KEY_GUI));
    }

    public static void writeSpawn(NBTTagCompound compound, int stored) {
        int mode = HitboxWeights.sanitize(stored);
        if (mode != HitboxWeights.ORIGINAL) {
            compound.setInteger(KEY_STORED, mode);
        }
    }

    public static int readStored(NBTTagCompound compound) {
        return HitboxWeights.sanitize(compound.getInteger(KEY_STORED));
    }
}
