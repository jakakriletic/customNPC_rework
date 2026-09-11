/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.util.EnumHand
 *  net.minecraft.world.World
 */
package noppes.npcs.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityDialogNpc
extends EntityNPCInterface {
    public EntityDialogNpc(World world) {
        super(world);
    }

    @Override
    public boolean func_98034_c(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean func_82150_aj() {
        return true;
    }

    @Override
    public void func_70071_h_() {
    }

    @Override
    public boolean func_184645_a(EntityPlayer player, EnumHand hand) {
        return false;
    }
}

