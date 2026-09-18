package noppes.npcs.rework.formation;

/**
 * Eno mesto v formaciji, v lokalnem koordinatnem sistemu formacije.
 *
 * <p>{@code side} je odmik v desno od sredisca (gledano v smeri formacije), {@code back}
 * odmik nazaj. Sredisce formacije je sidro, ki se premika po poti; mesto v svetu je
 * {@code sidro + side * desno - back * naprej}.
 *
 * <p>{@code faceOutward} pomeni, da NPC na tem mestu ob prihodu ne gleda v smer formacije,
 * ampak stran od sredisca (krog, obramba).
 */
public final class Slot {
    public final double side;
    public final double back;
    public final boolean faceOutward;

    public Slot(double side, double back, boolean faceOutward) {
        this.side = side;
        this.back = back;
        this.faceOutward = faceOutward;
    }

    public Slot(double side, double back) {
        this(side, back, false);
    }

    @Override
    public String toString() {
        return "Slot(" + side + ", " + back + (faceOutward ? ", ven" : "") + ")";
    }
}
