/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.DimensionType
 */
package noppes.npcs.api.wrapper;

import net.minecraft.world.DimensionType;
import noppes.npcs.api.IDimension;

public class DimensionWrapper
implements IDimension {
    private int id;
    private DimensionType type;

    public DimensionWrapper(int id, DimensionType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.type.func_186065_b();
    }

    @Override
    public String getSuffix() {
        return this.type.func_186067_c();
    }
}

