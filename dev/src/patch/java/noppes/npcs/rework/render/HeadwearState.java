package noppes.npcs.rework.render;

import java.util.List;

import net.minecraft.client.model.ModelRenderer;

/**
 * Posnetek stanja otrok pokrivala, na katerem stoji en display list (M5-K K14).
 *
 * <p>List zapece ukaze {@code postRender} vsakega otroka (tocka vrtenja in koti) z merilom ob
 * izdelavi in izpusti otroke, ki se ne risejo ({@code showModel}, {@code isHidden}). Vsebina
 * listov otrok je ze v originalu zapecena ob prvem izrisu, zato se tukaj ne spremlja. Ce se
 * katerakoli spremljana vrednost spremeni, list ne velja vec. Brez GL, zato je testljiv.
 */
public final class HeadwearState {
    /** Najvec prevezav lista na primerek; nato se primerek trajno vrne na original. */
    public static final int MAX_REBUILDS = 8;

    private ModelRenderer[] refs = new ModelRenderer[0];
    private float[] values = new float[0];
    private boolean[] flags = new boolean[0];
    private float scale = Float.NaN;
    private boolean valid;
    private int rebuilds;

    /** Ali posnetek velja za te otroke in to merilo (zaporedje, stevilo, vrednosti). */
    public boolean matches(List<ModelRenderer> children, float scale) {
        if (!this.valid || children.size() != this.refs.length || Float.floatToIntBits(scale) != Float.floatToIntBits(this.scale)) {
            return false;
        }
        for (int i = 0; i < this.refs.length; i++) {
            ModelRenderer c = children.get(i);
            if (c != this.refs[i] || c.showModel != this.flags[2 * i] || c.isHidden != this.flags[2 * i + 1]) {
                return false;
            }
            int o = 6 * i;
            if (!same(c.rotationPointX, this.values[o]) || !same(c.rotationPointY, this.values[o + 1])
                    || !same(c.rotationPointZ, this.values[o + 2]) || !same(c.rotateAngleX, this.values[o + 3])
                    || !same(c.rotateAngleY, this.values[o + 4]) || !same(c.rotateAngleZ, this.values[o + 5])) {
                return false;
            }
        }
        return true;
    }

    public void capture(List<ModelRenderer> children, float scale) {
        int n = children.size();
        this.refs = new ModelRenderer[n];
        this.values = new float[6 * n];
        this.flags = new boolean[2 * n];
        for (int i = 0; i < n; i++) {
            ModelRenderer c = children.get(i);
            this.refs[i] = c;
            this.flags[2 * i] = c.showModel;
            this.flags[2 * i + 1] = c.isHidden;
            int o = 6 * i;
            this.values[o] = c.rotationPointX;
            this.values[o + 1] = c.rotationPointY;
            this.values[o + 2] = c.rotationPointZ;
            this.values[o + 3] = c.rotateAngleX;
            this.values[o + 4] = c.rotateAngleY;
            this.values[o + 5] = c.rotateAngleZ;
        }
        this.scale = scale;
        this.valid = true;
    }

    /**
     * Zabelezi prevezavo veljavnega lista (stanje se je spremenilo po prvi izdelavi).
     * @return ali se list sme prevezati; po {@link #MAX_REBUILDS} prevezavah ne vec
     */
    public boolean allowRebuild() {
        if (!this.valid) {
            return true;
        }
        return ++this.rebuilds <= MAX_REBUILDS;
    }

    public void invalidate() {
        this.valid = false;
    }

    public int rebuilds() {
        return this.rebuilds;
    }

    private static boolean same(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }
}
