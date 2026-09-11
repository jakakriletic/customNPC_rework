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

public class EntityNPCHumanFemale
extends EntityNPCInterface {
    public EntityNPCHumanFemale(World world) {
        super(world);
        this.scaleZ = 0.9075f;
        this.scaleY = 0.9075f;
        this.scaleX = 0.9075f;
        this.display.setSkinTexture("customnpcs:textures/entity/humanfemale/Stephanie.png");
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
            data.getOrCreatePart((EnumParts)EnumParts.BREASTS).type = (byte)2;
            data.getPartConfig(EnumParts.LEG_LEFT).setScale(0.92f, 0.92f);
            data.getPartConfig(EnumParts.HEAD).setScale(0.95f, 0.95f);
            data.getPartConfig(EnumParts.ARM_LEFT).setScale(0.8f, 0.92f);
            data.getPartConfig(EnumParts.BODY).setScale(0.92f, 0.92f);
            this.world.spawnEntity((Entity)npc);
        }
        super.onUpdate();
    }
}

