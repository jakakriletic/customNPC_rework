/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTUtil
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.translation.I18n
 */
package noppes.npcs.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.SubGuiNpcAvailability;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcButtonYesNo;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.schematics.ISchematic;
import noppes.npcs.schematics.SchematicWrapper;

public class GuiBlockBuilder
extends GuiNPCInterface
implements IGuiData,
ICustomScrollListener,
IScrollData,
GuiYesNoCallback {
    private int x;
    private int y;
    private int z;
    private TileBuilder tile;
    private GuiCustomScroll scroll;
    private ISchematic selected = null;

    public GuiBlockBuilder(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 216;
        this.closeOnEsc = true;
        this.tile = (TileBuilder)this.player.world.getTileEntity(new BlockPos(x, y, z));
    }

    @Override
    public void initPacket() {
        Client.sendData(EnumPacketServer.SchematicsTile, this.x, this.y, this.z);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.scroll == null) {
            this.scroll = new GuiCustomScroll(this, 0);
            this.scroll.setSize(125, 208);
        }
        this.scroll.guiLeft = this.guiLeft + 4;
        this.scroll.guiTop = this.guiTop + 4;
        this.addScroll(this.scroll);
        if (this.selected != null) {
            int y = this.guiTop + 4;
            int size = this.selected.getWidth() * this.selected.getHeight() * this.selected.getLength();
            this.addButton(new GuiNpcButtonYesNo(3, this.guiLeft + 200, y, TileBuilder.DrawPos != null && this.tile.getPos().equals((Object)TileBuilder.DrawPos)));
            this.addLabel(new GuiNpcLabel(3, "schematic.preview", this.guiLeft + 130, y + 5));
            this.addLabel(new GuiNpcLabel(0, I18n.translateToLocal((String)"schematic.width") + ": " + this.selected.getWidth(), this.guiLeft + 130, y += 21));
            this.addLabel(new GuiNpcLabel(1, I18n.translateToLocal((String)"schematic.length") + ": " + this.selected.getLength(), this.guiLeft + 130, y += 11));
            this.addLabel(new GuiNpcLabel(2, I18n.translateToLocal((String)"schematic.height") + ": " + this.selected.getHeight(), this.guiLeft + 130, y += 11));
            this.addButton(new GuiNpcButtonYesNo(4, this.guiLeft + 200, y += 14, this.tile.enabled));
            this.addLabel(new GuiNpcLabel(4, I18n.translateToLocal((String)"gui.enabled"), this.guiLeft + 130, y + 5));
            this.addButton(new GuiNpcButtonYesNo(7, this.guiLeft + 200, y += 22, this.tile.finished));
            this.addLabel(new GuiNpcLabel(7, I18n.translateToLocal((String)"gui.finished"), this.guiLeft + 130, y + 5));
            this.addButton(new GuiNpcButtonYesNo(8, this.guiLeft + 200, y += 22, this.tile.started));
            this.addLabel(new GuiNpcLabel(8, I18n.translateToLocal((String)"gui.started"), this.guiLeft + 130, y + 5));
            this.addTextField(new GuiNpcTextField(9, this, this.guiLeft + 200, y += 22, 50, 20, this.tile.yOffest + ""));
            this.addLabel(new GuiNpcLabel(9, I18n.translateToLocal((String)"gui.yoffset"), this.guiLeft + 130, y + 5));
            this.getTextField((int)9).numbersOnly = true;
            this.getTextField(9).setMinMaxDefault(-10, 10, 0);
            this.addButton(new GuiNpcButton(5, this.guiLeft + 200, y += 22, 50, 20, new String[]{"0", "90", "180", "270"}, this.tile.rotation));
            this.addLabel(new GuiNpcLabel(5, I18n.translateToLocal((String)"movement.rotation"), this.guiLeft + 130, y + 5));
            this.addButton(new GuiNpcButton(6, this.guiLeft + 130, y += 22, 120, 20, "availability.options"));
            this.addButton(new GuiNpcButton(10, this.guiLeft + 130, y += 22, 120, 20, "schematic.instantBuild"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        if (guibutton.id == 3) {
            GuiNpcButtonYesNo button = (GuiNpcButtonYesNo)guibutton;
            if (button.getBoolean()) {
                TileBuilder.SetDrawPos(new BlockPos(this.x, this.y, this.z));
                this.tile.setDrawSchematic(new SchematicWrapper(this.selected));
            } else {
                TileBuilder.SetDrawPos(null);
                this.tile.setDrawSchematic(null);
            }
        }
        if (guibutton.id == 4) {
            this.tile.enabled = ((GuiNpcButtonYesNo)guibutton).getBoolean();
        }
        if (guibutton.id == 5) {
            this.tile.rotation = ((GuiNpcButton)guibutton).getValue();
            TileBuilder.Compiled = false;
        }
        if (guibutton.id == 6) {
            this.setSubGui(new SubGuiNpcAvailability(this.tile.availability));
        }
        if (guibutton.id == 7) {
            this.tile.finished = ((GuiNpcButtonYesNo)guibutton).getBoolean();
            Client.sendData(EnumPacketServer.SchematicsSet, this.x, this.y, this.z, this.scroll.getSelected());
        }
        if (guibutton.id == 8) {
            this.tile.started = ((GuiNpcButtonYesNo)guibutton).getBoolean();
        }
        if (guibutton.id == 10) {
            this.save();
            GuiYesNo guiyesno = new GuiYesNo((GuiYesNoCallback)this, "", I18n.translateToLocal((String)"schematic.instantBuildText"), 0);
            this.displayGuiScreen((GuiScreen)guiyesno);
        }
    }

    @Override
    public void save() {
        if (this.getTextField(9) != null) {
            this.tile.yOffest = this.getTextField(9).getInteger();
        }
        Client.sendData(EnumPacketServer.SchematicsTileSave, this.x, this.y, this.z, this.tile.writePartNBT(new NBTTagCompound()));
    }

    @Override
    public void setGuiData(final NBTTagCompound compound) {
        if (compound.hasKey("Width")) {
            final ArrayList<IBlockState> states = new ArrayList<IBlockState>();
            NBTTagList list = compound.getTagList("Data", 10);
            for (int i = 0; i < list.tagCount(); ++i) {
                states.add(NBTUtil.readBlockState((NBTTagCompound)list.getCompoundTagAt(i)));
            }
            this.selected = new ISchematic(){

                @Override
                public short getWidth() {
                    return compound.getShort("Width");
                }

                @Override
                public int getTileEntitySize() {
                    return 0;
                }

                @Override
                public NBTTagCompound getTileEntity(int i) {
                    return null;
                }

                @Override
                public String getName() {
                    return compound.getString("SchematicName");
                }

                @Override
                public short getLength() {
                    return compound.getShort("Length");
                }

                @Override
                public short getHeight() {
                    return compound.getShort("Height");
                }

                @Override
                public IBlockState getBlockState(int i) {
                    return (IBlockState)states.get(i);
                }

                @Override
                public IBlockState getBlockState(int x, int y, int z) {
                    return this.getBlockState((y * this.getLength() + z) * this.getWidth() + x);
                }

                @Override
                public NBTTagCompound getNBT() {
                    return null;
                }
            };
            if (TileBuilder.DrawPos != null && TileBuilder.DrawPos.equals((Object)this.tile.getPos())) {
                SchematicWrapper wrapper = new SchematicWrapper(this.selected);
                wrapper.rotation = this.tile.rotation;
                this.tile.setDrawSchematic(wrapper);
            }
            this.scroll.setSelected(this.selected.getName());
            this.scroll.scrollTo(this.selected.getName());
        } else {
            this.tile.readPartNBT(compound);
        }
        this.initGui();
    }

    public void confirmClicked(boolean flag, int i) {
        if (flag) {
            Client.sendData(EnumPacketServer.SchematicsBuild, this.x, this.y, this.z);
            this.close();
            this.selected = null;
        } else {
            NoppesUtil.openGUI((EntityPlayer)this.player, this);
        }
    }

    @Override
    public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
        if (!scroll.hasSelected()) {
            return;
        }
        if (this.selected != null) {
            this.getButton(3).setDisplay(0);
        }
        TileBuilder.SetDrawPos(null);
        this.tile.setDrawSchematic(null);
        Client.sendData(EnumPacketServer.SchematicsSet, this.x, this.y, this.z, scroll.getSelected());
    }

    @Override
    public void setData(Vector<String> list, HashMap<String, Integer> data) {
        this.scroll.setList(list);
        if (this.selected != null) {
            this.scroll.setSelected(this.selected.getName());
        }
        this.initGui();
    }

    @Override
    public void setSelected(String selected) {
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
    }
}

