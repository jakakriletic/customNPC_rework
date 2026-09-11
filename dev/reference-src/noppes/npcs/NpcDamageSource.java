/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.EntityDamageSource
 */
package noppes.npcs;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSource;

public class NpcDamageSource
extends EntityDamageSource {
    public NpcDamageSource(String par1Str, Entity par2Entity) {
        super(par1Str, par2Entity);
    }

    public boolean isDifficultyScaled() {
        return false;
    }
}

