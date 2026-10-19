package noppes.npcs.rework.render;

import java.util.List;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.client.model.Model2DRenderer;
import noppes.npcs.client.model.ModelScaleRenderer;
import org.lwjgl.opengl.GL11;

/**
 * M5-K K14: pokrivalo ({@code ModelHeadwear}, {@code HeadWearType=1}) v enem display listu.
 *
 * <p>Original za vsakega od 6 otrok {@code Model2DRenderer} poklice {@code pushMatrix},
 * {@code postRender} (premik in do dve rotaciji), {@code callList} in {@code popMatrix}: ~30
 * klicev GL na NPC, profil M5-K.1 6,7 us na NPC (19-26 % render niti). Tocke vrtenja in koti
 * otrok so konstantni (nastavi jih konstruktor, otroci so zasebni v {@code LayerHeadwear}), zato
 * se isto zaporedje ukazov enkrat posname v list in vsako slicico poklice z enim klicem.
 *
 * <p>Nacin 1: list poklice nespremenjene liste otrok (gonilnik dobi iste ukaze v istem
 * zaporedju). Nacin 2: list vsebuje tudi geometrijo otrok ({@link Model2DRenderer#rwCompileBody}),
 * en raven list brez gnezdenja. Starsev {@code postRender} in njegov (prazen) list ostaneta zunaj
 * lista kot v originalu. Ce se spremljano stanje otrok spremeni ({@link HeadwearState}), se list
 * prezgradi; po {@link HeadwearState#MAX_REBUILDS} prevezavah se primerek vrne na original.
 */
@SideOnly(Side.CLIENT)
public final class HeadwearList {
    /** Statistika za harness (render nit). */
    public static long drawsList;
    public static long compiles;
    public static long rebuilds;
    public static long fallbacks;

    private final HeadwearState state = new HeadwearState();
    private int list = -1;
    private int mode;
    private boolean disabled;

    /**
     * Narise starsa in otroke kot {@code ModelScaleRenderer.render}, otroke z enim listom.
     * @return false, ce mora klicatelj narisati original (nic ni bilo narisano)
     */
    public boolean render(ModelScaleRenderer self, float scale, int mode) {
        if (this.disabled || !self.showModel || self.isHidden || self.childModels == null) {
            if (this.disabled) {
                fallbacks++;
            }
            return false;
        }
        List<ModelRenderer> children = self.childModels;
        if (mode != this.mode || !this.state.matches(children, scale)) {
            if (this.list >= 0 && mode == this.mode) {
                rebuilds++;
                if (!this.state.allowRebuild()) {
                    GLAllocation.deleteDisplayLists(this.list);
                    this.list = -1;
                    this.disabled = true;
                    fallbacks++;
                    return false;
                }
            }
            if (!build(children, scale, mode)) {
                this.disabled = true;
                fallbacks++;
                return false;
            }
        }
        if (!self.isCompiled) {
            self.compile(scale);
        }
        GlStateManager.pushMatrix();
        self.postRender(scale);
        GlStateManager.callList(self.displayList);
        GlStateManager.callList(this.list);
        GlStateManager.popMatrix();
        drawsList++;
        return true;
    }

    private boolean build(List<ModelRenderer> children, float scale, int mode) {
        for (int i = 0; i < children.size(); i++) {
            if (!(children.get(i) instanceof Model2DRenderer)) {
                return false;
            }
        }
        // Kar original naredi ob prvem izrisu otroka zunaj lista: lasten list otroka in prazen
        // vanilla list (ModelRenderer.postRender ga prevede, ce se ni). glNewList se ne gnezdi.
        for (int i = 0; i < children.size(); i++) {
            Model2DRenderer c = (Model2DRenderer)children.get(i);
            if (c.showModel && !c.isHidden) {
                c.rwEnsureCompiled(scale);
                GlStateManager.pushMatrix();
                c.postRender(scale);
                GlStateManager.popMatrix();
            }
        }
        if (this.list < 0) {
            this.list = GLAllocation.generateDisplayLists(1);
        }
        GlStateManager.glNewList(this.list, GL11.GL_COMPILE);
        for (int i = 0; i < children.size(); i++) {
            Model2DRenderer c = (Model2DRenderer)children.get(i);
            // Model2DRenderer.render: if (!showModel || isHidden) return
            if (!c.showModel || c.isHidden) {
                continue;
            }
            GlStateManager.pushMatrix();
            c.postRender(scale);
            if (mode == 2) {
                c.rwCompileBody(scale);
            } else {
                GlStateManager.callList(c.rwDisplayList());
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.glEndList();
        this.state.capture(children, scale);
        this.mode = mode;
        compiles++;
        return true;
    }
}
