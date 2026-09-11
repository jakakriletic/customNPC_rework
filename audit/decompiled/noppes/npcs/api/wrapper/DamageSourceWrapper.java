/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.DamageSource
 */
package noppes.npcs.api.wrapper;

import net.minecraft.util.DamageSource;
import noppes.npcs.api.IDamageSource;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;

public class DamageSourceWrapper
implements IDamageSource {
    private DamageSource source;

    public DamageSourceWrapper(DamageSource source) {
        this.source = source;
    }

    @Override
    public String getType() {
        return this.source.func_76355_l();
    }

    @Override
    public boolean isUnblockable() {
        return this.source.func_76363_c();
    }

    @Override
    public boolean isProjectile() {
        return this.source.func_76352_a();
    }

    @Override
    public DamageSource getMCDamageSource() {
        return this.source;
    }

    @Override
    public IEntity getTrueSource() {
        return NpcAPI.Instance().getIEntity(this.source.func_76346_g());
    }

    @Override
    public IEntity getImmediateSource() {
        return NpcAPI.Instance().getIEntity(this.source.func_76364_f());
    }
}

