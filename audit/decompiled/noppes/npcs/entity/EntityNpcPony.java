/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNpcPony
extends EntityNPCInterface {
    public boolean isPegasus = false;
    public boolean isUnicorn = false;
    public boolean isFlying = false;
    public ResourceLocation checked = null;

    public EntityNpcPony(World world) {
        super(world);
        this.display.setSkinTexture("customnpcs:textures/entity/ponies/MineLP Derpy Hooves.png");
    }

    @Override
    public void func_70071_h_() {
        this.field_70128_L = true;
        this.func_94061_f(true);
        if (!this.field_70170_p.field_72995_K) {
            NBTTagCompound compound = new NBTTagCompound();
            this.func_189511_e(compound);
            EntityCustomNpc npc = new EntityCustomNpc(this.field_70170_p);
            npc.func_70020_e(compound);
            ModelData data = npc.modelData;
            data.setEntityClass(EntityNpcPony.class);
            this.field_70170_p.func_72838_d((Entity)npc);
        }
        super.func_70071_h_();
    }
}

