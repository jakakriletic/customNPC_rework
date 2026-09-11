/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNPCGolem
extends EntityNPCInterface {
    public EntityNPCGolem(World world) {
        super(world);
        this.display.setSkinTexture("customnpcs:textures/entity/golem/Iron Golem.png");
        this.field_70130_N = 1.4f;
        this.field_70131_O = 2.5f;
    }

    @Override
    public void updateHitbox() {
        this.currentAnimation = (Integer)this.field_70180_af.func_187225_a(Animation);
        if (this.currentAnimation == 2) {
            this.field_70131_O = 0.5f;
            this.field_70130_N = 0.5f;
        } else if (this.currentAnimation == 1) {
            this.field_70130_N = 1.4f;
            this.field_70131_O = 2.0f;
        } else {
            this.field_70130_N = 1.4f;
            this.field_70131_O = 2.5f;
        }
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
            data.setEntityClass(EntityNPCGolem.class);
            this.field_70170_p.func_72838_d((Entity)npc);
        }
        super.func_70071_h_();
    }
}

