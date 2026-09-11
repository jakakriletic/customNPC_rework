/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.util.math.MathHelper
 */
package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.wrapper.ContainerCustomChestWrapper;
import noppes.npcs.api.wrapper.ContainerWrapper;
import noppes.npcs.containers.ContainerCustomChest;

public class ContainerNpcInterface
extends Container {
    private int posX;
    private int posZ;
    public EntityPlayer player;
    public IContainer scriptContainer;

    public ContainerNpcInterface(EntityPlayer player) {
        this.player = player;
        this.posX = MathHelper.floor((double)player.posX);
        this.posZ = MathHelper.floor((double)player.posZ);
        player.motionX = 0.0;
        player.motionZ = 0.0;
    }

    public boolean canInteractWith(EntityPlayer player) {
        return !player.isDead && this.posX == MathHelper.floor((double)player.posX) && this.posZ == MathHelper.floor((double)player.posZ);
    }

    public static IContainer getOrCreateIContainer(ContainerNpcInterface container) {
        if (container.scriptContainer != null) {
            return container.scriptContainer;
        }
        if (container instanceof ContainerCustomChest) {
            container.scriptContainer = new ContainerCustomChestWrapper(container);
            return container.scriptContainer;
        }
        container.scriptContainer = new ContainerWrapper(container);
        return container.scriptContainer;
    }
}

