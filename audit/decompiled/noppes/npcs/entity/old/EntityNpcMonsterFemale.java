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
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityNpcMonsterFemale
extends EntityNPCInterface {
    public EntityNpcMonsterFemale(World world) {
        super(world);
        this.scaleZ = 0.9075f;
        this.scaleY = 0.9075f;
        this.scaleX = 0.9075f;
        this.display.setSkinTexture("customnpcs:textures/entity/monsterfemale/ZombieStephanie.png");
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
            data.getOrCreatePart((EnumParts)EnumParts.BREASTS).type = (byte)2;
            data.getPartConfig(EnumParts.LEG_LEFT).setScale(0.92f, 0.92f);
            data.getPartConfig(EnumParts.HEAD).setScale(0.95f, 0.95f);
            data.getPartConfig(EnumParts.ARM_LEFT).setScale(0.8f, 0.92f);
            data.getPartConfig(EnumParts.BODY).setScale(0.92f, 0.92f);
            npc.ais.animationType = 3;
            this.field_70170_p.func_72838_d((Entity)npc);
        }
        super.func_70071_h_();
    }
}

