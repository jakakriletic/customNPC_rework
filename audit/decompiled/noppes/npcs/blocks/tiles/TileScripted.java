/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.NetworkManager
 *  net.minecraft.network.play.server.SPacketUpdateTileEntity
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.ITickable
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraftforge.fml.common.eventhandler.Event
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package noppes.npcs.blocks.tiles;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.CustomItems;
import noppes.npcs.EventHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.TextBlock;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.block.ITextPlane;
import noppes.npcs.api.wrapper.BlockScriptedWrapper;
import noppes.npcs.blocks.tiles.TileNpcEntity;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.IScriptBlockHandler;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.entity.data.DataTimers;
import noppes.npcs.util.ValueUtil;

public class TileScripted
extends TileNpcEntity
implements ITickable,
IScriptBlockHandler {
    public List<ScriptContainer> scripts = new ArrayList<ScriptContainer>();
    public String scriptLanguage = "ECMAScript";
    public boolean enabled = false;
    private IBlock blockDummy = null;
    public DataTimers timers = new DataTimers(this);
    public long lastInited = -1L;
    private short ticksExisted = 0;
    public ItemStack itemModel = new ItemStack(CustomItems.scripted);
    public Block blockModel = null;
    public boolean needsClientUpdate = false;
    public int powering = 0;
    public int activePowering = 0;
    public int newPower = 0;
    public int prevPower = 0;
    public boolean isPassible = false;
    public boolean isLadder = false;
    public int lightValue = 0;
    public float blockHardness = 5.0f;
    public float blockResistance = 10.0f;
    public int rotationX = 0;
    public int rotationY = 0;
    public int rotationZ = 0;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float scaleZ = 1.0f;
    public TileEntity renderTile;
    public boolean renderTileErrored = true;
    public ITickable renderTileUpdate = null;
    public TextPlane text1 = new TextPlane();
    public TextPlane text2 = new TextPlane();
    public TextPlane text3 = new TextPlane();
    public TextPlane text4 = new TextPlane();
    public TextPlane text5 = new TextPlane();
    public TextPlane text6 = new TextPlane();

    @Override
    public IBlock getBlock() {
        if (this.blockDummy == null) {
            this.blockDummy = new BlockScriptedWrapper(this.func_145831_w(), this.func_145838_q(), this.func_174877_v());
        }
        return this.blockDummy;
    }

    @Override
    public void func_145839_a(NBTTagCompound compound) {
        super.func_145839_a(compound);
        this.setNBT(compound);
        this.setDisplayNBT(compound);
        this.timers.readFromNBT(compound);
    }

    public void setNBT(NBTTagCompound compound) {
        this.scripts = NBTTags.GetScript(compound.func_150295_c("Scripts", 10), this);
        this.scriptLanguage = compound.func_74779_i("ScriptLanguage");
        this.enabled = compound.func_74767_n("ScriptEnabled");
        this.activePowering = this.powering = compound.func_74762_e("BlockPowering");
        this.prevPower = compound.func_74762_e("BlockPrevPower");
        if (compound.func_74764_b("BlockHardness")) {
            this.blockHardness = compound.func_74760_g("BlockHardness");
            this.blockResistance = compound.func_74760_g("BlockResistance");
        }
    }

    public void setDisplayNBT(NBTTagCompound compound) {
        this.itemModel = new ItemStack(compound.func_74775_l("ScriptBlockModel"));
        if (this.itemModel.func_190926_b()) {
            this.itemModel = new ItemStack(CustomItems.scripted);
        }
        if (compound.func_74764_b("ScriptBlockModelBlock")) {
            this.blockModel = Block.func_149684_b((String)compound.func_74779_i("ScriptBlockModelBlock"));
        }
        this.renderTileUpdate = null;
        this.renderTile = null;
        this.renderTileErrored = false;
        this.lightValue = compound.func_74762_e("LightValue");
        this.isLadder = compound.func_74767_n("IsLadder");
        this.isPassible = compound.func_74767_n("IsPassible");
        this.rotationX = compound.func_74762_e("RotationX");
        this.rotationY = compound.func_74762_e("RotationY");
        this.rotationZ = compound.func_74762_e("RotationZ");
        this.scaleX = compound.func_74760_g("ScaleX");
        this.scaleY = compound.func_74760_g("ScaleY");
        this.scaleZ = compound.func_74760_g("ScaleZ");
        if (this.scaleX <= 0.0f) {
            this.scaleX = 1.0f;
        }
        if (this.scaleY <= 0.0f) {
            this.scaleY = 1.0f;
        }
        if (this.scaleZ <= 0.0f) {
            this.scaleZ = 1.0f;
        }
        if (compound.func_74764_b("Text3")) {
            this.text1.setNBT(compound.func_74775_l("Text1"));
            this.text2.setNBT(compound.func_74775_l("Text2"));
            this.text3.setNBT(compound.func_74775_l("Text3"));
            this.text4.setNBT(compound.func_74775_l("Text4"));
            this.text5.setNBT(compound.func_74775_l("Text5"));
            this.text6.setNBT(compound.func_74775_l("Text6"));
        }
    }

    @Override
    public NBTTagCompound func_189515_b(NBTTagCompound compound) {
        this.getNBT(compound);
        this.getDisplayNBT(compound);
        this.timers.writeToNBT(compound);
        return super.func_189515_b(compound);
    }

    public NBTTagCompound getNBT(NBTTagCompound compound) {
        compound.func_74782_a("Scripts", (NBTBase)NBTTags.NBTScript(this.scripts));
        compound.func_74778_a("ScriptLanguage", this.scriptLanguage);
        compound.func_74757_a("ScriptEnabled", this.enabled);
        compound.func_74768_a("BlockPowering", this.powering);
        compound.func_74768_a("BlockPrevPower", this.prevPower);
        compound.func_74776_a("BlockHardness", this.blockHardness);
        compound.func_74776_a("BlockResistance", this.blockResistance);
        return compound;
    }

    public NBTTagCompound getDisplayNBT(NBTTagCompound compound) {
        NBTTagCompound itemcompound = new NBTTagCompound();
        this.itemModel.func_77955_b(itemcompound);
        if (this.blockModel != null) {
            ResourceLocation resourcelocation = (ResourceLocation)Block.field_149771_c.func_177774_c((Object)this.blockModel);
            compound.func_74778_a("ScriptBlockModelBlock", resourcelocation == null ? "" : resourcelocation.toString());
        }
        compound.func_74782_a("ScriptBlockModel", (NBTBase)itemcompound);
        compound.func_74768_a("LightValue", this.lightValue);
        compound.func_74757_a("IsLadder", this.isLadder);
        compound.func_74757_a("IsPassible", this.isPassible);
        compound.func_74768_a("RotationX", this.rotationX);
        compound.func_74768_a("RotationY", this.rotationY);
        compound.func_74768_a("RotationZ", this.rotationZ);
        compound.func_74776_a("ScaleX", this.scaleX);
        compound.func_74776_a("ScaleY", this.scaleY);
        compound.func_74776_a("ScaleZ", this.scaleZ);
        compound.func_74782_a("Text1", (NBTBase)this.text1.getNBT());
        compound.func_74782_a("Text2", (NBTBase)this.text2.getNBT());
        compound.func_74782_a("Text3", (NBTBase)this.text3.getNBT());
        compound.func_74782_a("Text4", (NBTBase)this.text4.getNBT());
        compound.func_74782_a("Text5", (NBTBase)this.text5.getNBT());
        compound.func_74782_a("Text6", (NBTBase)this.text6.getNBT());
        return compound;
    }

    private boolean isEnabled() {
        return this.enabled && ScriptController.HasStart && !this.field_145850_b.field_72995_K;
    }

    public void func_73660_a() {
        if (this.renderTileUpdate != null) {
            try {
                this.renderTileUpdate.func_73660_a();
            }
            catch (Exception e) {
                this.renderTileUpdate = null;
            }
        }
        this.ticksExisted = (short)(this.ticksExisted + 1);
        if (this.prevPower != this.newPower && this.powering <= 0) {
            EventHooks.onScriptBlockRedstonePower(this, this.prevPower, this.newPower);
            this.prevPower = this.newPower;
        }
        this.timers.update();
        if (this.ticksExisted >= 10) {
            EventHooks.onScriptBlockUpdate(this);
            this.ticksExisted = 0;
            if (this.needsClientUpdate) {
                this.func_70296_d();
                IBlockState state = this.field_145850_b.func_180495_p(this.field_174879_c);
                this.field_145850_b.func_184138_a(this.field_174879_c, state, state, 3);
                this.needsClientUpdate = false;
            }
        }
    }

    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.handleUpdateTag(pkt.func_148857_g());
    }

    public void handleUpdateTag(NBTTagCompound tag) {
        int light = this.lightValue;
        this.setDisplayNBT(tag);
        if (light != this.lightValue) {
            this.field_145850_b.func_175664_x(this.field_174879_c);
        }
    }

    public SPacketUpdateTileEntity func_189518_D_() {
        return new SPacketUpdateTileEntity(this.field_174879_c, 0, this.func_189517_E_());
    }

    public NBTTagCompound func_189517_E_() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.func_74768_a("x", this.field_174879_c.func_177958_n());
        compound.func_74768_a("y", this.field_174879_c.func_177956_o());
        compound.func_74768_a("z", this.field_174879_c.func_177952_p());
        this.getDisplayNBT(compound);
        return compound;
    }

    public void setItemModel(ItemStack item, Block b) {
        if (item == null || item.func_190926_b()) {
            item = new ItemStack(CustomItems.scripted);
        }
        if (NoppesUtilPlayer.compareItems(item, this.itemModel, false, false) && b != this.blockModel) {
            return;
        }
        this.itemModel = item;
        this.blockModel = b;
        this.needsClientUpdate = true;
    }

    public void setLightValue(int value) {
        if (value == this.lightValue) {
            return;
        }
        this.lightValue = ValueUtil.CorrectInt(value, 0, 15);
        this.needsClientUpdate = true;
    }

    public void setRedstonePower(int strength) {
        if (this.powering == strength) {
            return;
        }
        this.prevPower = this.activePowering = ValueUtil.CorrectInt(strength, 0, 15);
        this.field_145850_b.func_175685_c(this.field_174879_c, this.func_145838_q(), false);
        this.powering = this.activePowering;
    }

    public void setScale(float x, float y, float z) {
        if (this.scaleX == x && this.scaleY == y && this.scaleZ == z) {
            return;
        }
        this.scaleX = ValueUtil.correctFloat(x, 0.0f, 10.0f);
        this.scaleY = ValueUtil.correctFloat(y, 0.0f, 10.0f);
        this.scaleZ = ValueUtil.correctFloat(z, 0.0f, 10.0f);
        this.needsClientUpdate = true;
    }

    public void setRotation(int x, int y, int z) {
        if (this.rotationX == x && this.rotationY == y && this.rotationZ == z) {
            return;
        }
        this.rotationX = ValueUtil.CorrectInt(x, 0, 359);
        this.rotationY = ValueUtil.CorrectInt(y, 0, 359);
        this.rotationZ = ValueUtil.CorrectInt(z, 0, 359);
        this.needsClientUpdate = true;
    }

    @Override
    public void runScript(EnumScriptType type, Event event) {
        if (!this.isEnabled()) {
            return;
        }
        if (ScriptController.Instance.lastLoaded > this.lastInited) {
            this.lastInited = ScriptController.Instance.lastLoaded;
            if (type != EnumScriptType.INIT) {
                EventHooks.onScriptBlockInit(this);
            }
        }
        for (ScriptContainer script : this.scripts) {
            script.run(type, event);
        }
    }

    @Override
    public boolean isClient() {
        return this.func_145831_w().field_72995_K;
    }

    @Override
    public boolean getEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean bo) {
        this.enabled = bo;
    }

    @Override
    public String noticeString() {
        BlockPos pos = this.func_174877_v();
        return MoreObjects.toStringHelper((Object)this).add("x", pos.func_177958_n()).add("y", pos.func_177956_o()).add("z", pos.func_177952_p()).toString();
    }

    @Override
    public String getLanguage() {
        return this.scriptLanguage;
    }

    @Override
    public void setLanguage(String lang) {
        this.scriptLanguage = lang;
    }

    @Override
    public List<ScriptContainer> getScripts() {
        return this.scripts;
    }

    @Override
    public Map<Long, String> getConsoleText() {
        TreeMap<Long, String> map = new TreeMap<Long, String>();
        int tab = 0;
        for (ScriptContainer script : this.getScripts()) {
            ++tab;
            for (Map.Entry<Long, String> entry : script.console.entrySet()) {
                map.put(entry.getKey(), " tab " + tab + ":\n" + entry.getValue());
            }
        }
        return map;
    }

    @Override
    public void clearConsole() {
        for (ScriptContainer script : this.getScripts()) {
            script.console.clear();
        }
    }

    @SideOnly(value=Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return Block.field_185505_j.func_186670_a(this.func_174877_v());
    }

    public class TextPlane
    implements ITextPlane {
        public boolean textHasChanged = true;
        public TextBlock textBlock;
        public String text = "";
        public int rotationX = 0;
        public int rotationY = 0;
        public int rotationZ = 0;
        public float offsetX = 0.0f;
        public float offsetY = 0.0f;
        public float offsetZ = 0.5f;
        public float scale = 1.0f;

        @Override
        public String getText() {
            return this.text;
        }

        @Override
        public void setText(String text) {
            if (this.text.equals(text)) {
                return;
            }
            this.text = text;
            this.textHasChanged = true;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public int getRotationX() {
            return this.rotationX;
        }

        @Override
        public int getRotationY() {
            return this.rotationY;
        }

        @Override
        public int getRotationZ() {
            return this.rotationZ;
        }

        @Override
        public void setRotationX(int x) {
            if (this.rotationX == (x = ValueUtil.CorrectInt(x % 360, 0, 359))) {
                return;
            }
            this.rotationX = x;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public void setRotationY(int y) {
            if (this.rotationY == (y = ValueUtil.CorrectInt(y % 360, 0, 359))) {
                return;
            }
            this.rotationY = y;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public void setRotationZ(int z) {
            if (this.rotationZ == (z = ValueUtil.CorrectInt(z % 360, 0, 359))) {
                return;
            }
            this.rotationZ = z;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public float getOffsetX() {
            return this.offsetX;
        }

        @Override
        public float getOffsetY() {
            return this.offsetY;
        }

        @Override
        public float getOffsetZ() {
            return this.offsetZ;
        }

        @Override
        public void setOffsetX(float x) {
            if (this.offsetX == (x = ValueUtil.correctFloat(x, -1.0f, 1.0f))) {
                return;
            }
            this.offsetX = x;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public void setOffsetY(float y) {
            if (this.offsetY == (y = ValueUtil.correctFloat(y, -1.0f, 1.0f))) {
                return;
            }
            this.offsetY = y;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public void setOffsetZ(float z) {
            if (this.offsetZ == (z = ValueUtil.correctFloat(z, -1.0f, 1.0f))) {
                return;
            }
            System.out.println(this.rotationZ);
            this.offsetZ = z;
            TileScripted.this.needsClientUpdate = true;
        }

        @Override
        public float getScale() {
            return this.scale;
        }

        @Override
        public void setScale(float scale) {
            if (this.scale == scale) {
                return;
            }
            this.scale = scale;
            TileScripted.this.needsClientUpdate = true;
        }

        public NBTTagCompound getNBT() {
            NBTTagCompound compound = new NBTTagCompound();
            compound.func_74778_a("Text", this.text);
            compound.func_74768_a("RotationX", this.rotationX);
            compound.func_74768_a("RotationY", this.rotationY);
            compound.func_74768_a("RotationZ", this.rotationZ);
            compound.func_74776_a("OffsetX", this.offsetX);
            compound.func_74776_a("OffsetY", this.offsetY);
            compound.func_74776_a("OffsetZ", this.offsetZ);
            compound.func_74776_a("Scale", this.scale);
            return compound;
        }

        public void setNBT(NBTTagCompound compound) {
            this.setText(compound.func_74779_i("Text"));
            this.rotationX = compound.func_74762_e("RotationX");
            this.rotationY = compound.func_74762_e("RotationY");
            this.rotationZ = compound.func_74762_e("RotationZ");
            this.offsetX = compound.func_74760_g("OffsetX");
            this.offsetY = compound.func_74760_g("OffsetY");
            this.offsetZ = compound.func_74760_g("OffsetZ");
            this.scale = compound.func_74760_g("Scale");
        }
    }
}

