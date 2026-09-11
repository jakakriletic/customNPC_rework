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

public class EntityNPCDwarfMale
extends EntityNPCInterface {
    public EntityNPCDwarfMale(World world) {
        super(world);
        this.scaleZ = 0.85f;
        this.scaleX = 0.85f;
        this.scaleY = 0.6875f;
        this.display.setSkinTexture("customnpcs:textures/entity/dwarfmale/Simon.png");
    }

    @Override
    public void onUpdate() {
        this.isDead = true;
        this.setNoAI(true);
        if (!this.world.isRemote) {
            NBTTagCompound compound = new NBTTagCompound();
            this.writeToNBT(compound);
            EntityCustomNpc npc = new EntityCustomNpc(this.world);
            npc.readFromNBT(compound);
            ModelData data = npc.modelData;
            data.getPartConfig(EnumParts.LEG_LEFT).setScale(1.1f, 0.7f, 0.9f);
            data.getPartConfig(EnumParts.ARM_LEFT).setScale(0.9f, 0.7f);
            data.getPartConfig(EnumParts.BODY).setScale(1.2f, 0.7f, 1.5f);
            data.getPartConfig(EnumParts.HEAD).setScale(0.85f, 0.85f);
            this.world.spawnEntity((Entity)npc);
        }
        super.onUpdate();
    }
}

