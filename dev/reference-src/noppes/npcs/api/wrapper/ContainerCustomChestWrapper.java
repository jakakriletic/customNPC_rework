/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 */
package noppes.npcs.api.wrapper;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import noppes.npcs.Server;
import noppes.npcs.api.IContainerCustomChest;
import noppes.npcs.api.wrapper.ContainerWrapper;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.containers.ContainerNpcInterface;
import noppes.npcs.controllers.ScriptContainer;

public class ContainerCustomChestWrapper
extends ContainerWrapper
implements IContainerCustomChest {
    public ScriptContainer script = null;
    public String name = "";

    public ContainerCustomChestWrapper(IInventory inventory) {
        super(inventory);
    }

    public ContainerCustomChestWrapper(Container container) {
        super(container);
    }

    @Override
    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        if (this.name.equals(name)) {
            return;
        }
        this.name = name;
        Server.sendDataDelayed((EntityPlayerMP)((ContainerNpcInterface)this.getMCContainer()).player, EnumPacketClient.CHEST_NAME, 10, name);
    }

    @Override
    public String getName() {
        return this.name;
    }
}

