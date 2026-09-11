/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.fml.common.eventhandler.Event
 */
package noppes.npcs.api.event;

import net.minecraftforge.fml.common.eventhandler.Event;
import noppes.npcs.api.NpcAPI;

public class CustomNPCsEvent
extends Event {
    public final NpcAPI API = NpcAPI.Instance();
}

