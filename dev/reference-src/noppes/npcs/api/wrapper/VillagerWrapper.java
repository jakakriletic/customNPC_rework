/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.passive.EntityVillager
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.passive.EntityVillager;
import noppes.npcs.api.entity.IVillager;
import noppes.npcs.api.wrapper.EntityLivingWrapper;

public class VillagerWrapper<T extends EntityVillager>
extends EntityLivingWrapper<T>
implements IVillager {
    public VillagerWrapper(T entity) {
        super(entity);
    }

    public int getProfession() {
        return ((EntityVillager)this.entity).getProfession();
    }

    public String getCareer() {
        return ((EntityVillager)this.entity).getProfessionForge().getCareer(((EntityVillager)this.entity).careerId).getName();
    }

    @Override
    public int getType() {
        return 9;
    }

    @Override
    public boolean typeOf(int type) {
        return type == 9 ? true : super.typeOf(type);
    }
}

