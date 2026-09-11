/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.CapabilityInject
 *  net.minecraftforge.common.capabilities.ICapabilityProvider
 *  net.minecraftforge.event.AttachCapabilitiesEvent
 */
package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import noppes.npcs.Server;
import noppes.npcs.api.entity.data.IMark;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.data.Availability;

public class MarkData
implements ICapabilityProvider {
    @CapabilityInject(value=MarkData.class)
    public static Capability<MarkData> MARKDATA_CAPABILITY = null;
    private static final String NBTKEY = "cnpcmarkdata";
    private static final ResourceLocation CAPKEY = new ResourceLocation("customnpcs", "markdata");
    private EntityLivingBase entity;
    public List<Mark> marks = new ArrayList<Mark>();

    public void setNBT(NBTTagCompound compound) {
        ArrayList<Mark> marks = new ArrayList<Mark>();
        NBTTagList list = compound.func_150295_c("marks", 10);
        for (int i = 0; i < list.func_74745_c(); ++i) {
            NBTTagCompound c = list.func_150305_b(i);
            Mark m = new Mark();
            m.type = c.func_74762_e("type");
            m.color = c.func_74762_e("color");
            m.availability.readFromNBT(c.func_74775_l("availability"));
            marks.add(m);
        }
        this.marks = marks;
    }

    public NBTTagCompound getNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Mark m : this.marks) {
            NBTTagCompound c = new NBTTagCompound();
            c.func_74768_a("type", m.type);
            c.func_74768_a("color", m.color);
            c.func_74782_a("availability", (NBTBase)m.availability.writeToNBT(new NBTTagCompound()));
            list.func_74742_a((NBTBase)c);
        }
        compound.func_74782_a("marks", (NBTBase)list);
        return compound;
    }

    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == MARKDATA_CAPABILITY;
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (this.hasCapability(capability, facing)) {
            return (T)this;
        }
        return null;
    }

    public static void register(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(CAPKEY, (ICapabilityProvider)new MarkData());
    }

    public void save() {
        this.entity.getEntityData().func_74782_a(NBTKEY, (NBTBase)this.getNBT());
    }

    public IMark addMark(int type) {
        Mark m = new Mark();
        m.type = type;
        this.marks.add(m);
        if (!this.entity.field_70170_p.field_72995_K) {
            this.syncClients();
        }
        return m;
    }

    public IMark addMark(int type, int color) {
        Mark m = new Mark();
        m.type = type;
        m.color = color;
        this.marks.add(m);
        if (!this.entity.field_70170_p.field_72995_K) {
            this.syncClients();
        }
        return m;
    }

    public static MarkData get(EntityLivingBase entity) {
        MarkData data = (MarkData)entity.getCapability(MARKDATA_CAPABILITY, null);
        if (data.entity == null) {
            data.entity = entity;
            data.setNBT(entity.getEntityData().func_74775_l(NBTKEY));
        }
        return data;
    }

    public void syncClients() {
        Server.sendToAll(this.entity.func_184102_h(), EnumPacketClient.MARK_DATA, this.entity.func_145782_y(), this.getNBT());
    }

    public class Mark
    implements IMark {
        public int type = 0;
        public Availability availability = new Availability();
        public int color = 16772433;

        @Override
        public IAvailability getAvailability() {
            return this.availability;
        }

        @Override
        public int getColor() {
            return this.color;
        }

        @Override
        public void setColor(int color) {
            this.color = color;
        }

        @Override
        public int getType() {
            return this.type;
        }

        @Override
        public void setType(int type) {
            this.type = type;
        }

        @Override
        public void update() {
            MarkData.this.syncClients();
        }
    }
}

