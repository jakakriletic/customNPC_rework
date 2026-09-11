/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.world.World
 */
package noppes.npcs.entity.old;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import noppes.npcs.ModelData;
import noppes.npcs.ModelPartData;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNPCFurryMale
extends EntityNPCInterface {
    public EntityNPCFurryMale(World world) {
        super(world);
        this.display.setSkinTexture("customnpcs:textures/entity/furrymale/WolfGrey.png");
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
            ModelPartData ears = data.getOrCreatePart(EnumParts.EARS);
            ears.type = 0;
            ears.color = 0x5E5855;
            ModelPartData snout = data.getOrCreatePart(EnumParts.SNOUT);
            snout.type = (byte)2;
            snout.color = 0x5E5855;
            ModelPartData tail = data.getOrCreatePart(EnumParts.TAIL);
            tail.type = 0;
            tail.color = 0x5E5855;
            this.field_70170_p.func_72838_d((Entity)npc);
        }
        super.func_70071_h_();
    }
}

