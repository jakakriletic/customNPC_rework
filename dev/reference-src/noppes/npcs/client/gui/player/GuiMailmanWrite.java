/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiButton
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.client.gui.GuiYesNo
 *  net.minecraft.client.gui.GuiYesNoCallback
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.resources.I18n
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.util.ChatAllowedCharacters
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentTranslation
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  org.lwjgl.input.Keyboard
 */
package noppes.npcs.client.gui.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiButtonNextPage;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiClose;
import noppes.npcs.client.gui.util.IGuiError;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.containers.ContainerMail;
import noppes.npcs.controllers.data.PlayerMail;
import org.lwjgl.input.Keyboard;

@SideOnly(value=Side.CLIENT)
public class GuiMailmanWrite
extends GuiContainerNPCInterface
implements ITextfieldListener,
IGuiError,
IGuiClose,
GuiYesNoCallback {
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
    private static final ResourceLocation bookWidgets = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation bookInventory = new ResourceLocation("textures/gui/container/inventory.png");
    private int updateCount;
    private int bookImageWidth = 192;
    private int bookImageHeight = 192;
    private int bookTotalPages = 1;
    private int currPage;
    private NBTTagList bookPages;
    private GuiButtonNextPage buttonNextPage;
    private GuiButtonNextPage buttonPreviousPage;
    private boolean canEdit;
    private boolean canSend;
    private boolean hasSend = false;
    public static GuiScreen parent;
    public static PlayerMail mail;
    private Minecraft mc = Minecraft.getMinecraft();
    private String username = "";
    private GuiNpcLabel error;

    public GuiMailmanWrite(ContainerMail container, boolean canEdit, boolean canSend) {
        super(null, container);
        this.title = "";
        this.canEdit = canEdit;
        this.canSend = canSend;
        if (GuiMailmanWrite.mail.message.hasKey("pages")) {
            this.bookPages = GuiMailmanWrite.mail.message.getTagList("pages", 8);
        }
        if (this.bookPages != null) {
            this.bookPages = this.bookPages.copy();
            this.bookTotalPages = this.bookPages.tagCount();
            if (this.bookTotalPages < 1) {
                this.bookTotalPages = 1;
            }
        } else {
            this.bookPages = new NBTTagList();
            this.bookPages.appendTag((NBTBase)new NBTTagString(""));
            this.bookTotalPages = 1;
        }
        this.xSize = 360;
        this.ySize = 260;
        this.drawDefaultBackground = false;
        this.closeOnEsc = true;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ++this.updateCount;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        Keyboard.enableRepeatEvents((boolean)true);
        if (this.canEdit && !this.canSend) {
            this.addLabel(new GuiNpcLabel(0, "mailbox.sender", this.guiLeft + 170, this.guiTop + 32, 0));
        } else {
            this.addLabel(new GuiNpcLabel(0, "mailbox.username", this.guiLeft + 170, this.guiTop + 32, 0));
        }
        if (this.canEdit && !this.canSend) {
            this.addTextField(new GuiNpcTextField(2, (GuiScreen)this, this.fontRenderer, this.guiLeft + 170, this.guiTop + 42, 114, 20, GuiMailmanWrite.mail.sender));
        } else if (this.canEdit) {
            this.addTextField(new GuiNpcTextField(0, (GuiScreen)this, this.fontRenderer, this.guiLeft + 170, this.guiTop + 42, 114, 20, this.username));
        } else {
            this.addLabel(new GuiNpcLabel(10, GuiMailmanWrite.mail.sender, this.guiLeft + 170, this.guiTop + 42, 0));
        }
        this.addLabel(new GuiNpcLabel(1, "mailbox.subject", this.guiLeft + 170, this.guiTop + 72, 0));
        if (this.canEdit) {
            this.addTextField(new GuiNpcTextField(1, (GuiScreen)this, this.fontRenderer, this.guiLeft + 170, this.guiTop + 82, 114, 20, GuiMailmanWrite.mail.subject));
        } else {
            this.addLabel(new GuiNpcLabel(11, GuiMailmanWrite.mail.subject, this.guiLeft + 170, this.guiTop + 82, 0));
        }
        this.error = new GuiNpcLabel(2, "", this.guiLeft + 170, this.guiTop + 114, 0xFF0000);
        this.addLabel(this.error);
        if (this.canEdit && !this.canSend) {
            this.addButton(new GuiNpcButton(0, this.guiLeft + 200, this.guiTop + 171, 60, 20, "gui.done"));
        } else if (this.canEdit) {
            this.addButton(new GuiNpcButton(0, this.guiLeft + 200, this.guiTop + 171, 60, 20, "mailbox.send"));
        }
        if (!this.canEdit && !this.canSend) {
            this.addButton(new GuiNpcButton(4, this.guiLeft + 200, this.guiTop + 171, 60, 20, "selectWorld.deleteButton"));
        }
        if (!this.canEdit || this.canSend) {
            this.addButton(new GuiNpcButton(3, this.guiLeft + 200, this.guiTop + 194, 60, 20, "gui.cancel"));
        }
        this.buttonNextPage = new GuiButtonNextPage(1, this.guiLeft + 120, this.guiTop + 156, true);
        this.buttonList.add(this.buttonNextPage);
        this.buttonPreviousPage = new GuiButtonNextPage(2, this.guiLeft + 38, this.guiTop + 156, false);
        this.buttonList.add(this.buttonPreviousPage);
        this.updateButtons();
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents((boolean)false);
    }

    private void updateButtons() {
        this.buttonNextPage.setVisible(this.currPage < this.bookTotalPages - 1 || this.canEdit);
        this.buttonPreviousPage.setVisible(this.currPage > 0);
    }

    public void confirmClicked(boolean flag, int i) {
        if (flag) {
            NoppesUtilPlayer.sendData(EnumPlayerPacket.MailDelete, GuiMailmanWrite.mail.time, GuiMailmanWrite.mail.sender);
            this.close();
        } else {
            NoppesUtil.openGUI((EntityPlayer)this.player, this);
        }
    }

    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
        if (par1GuiButton.enabled) {
            int id = par1GuiButton.id;
            if (id == 0) {
                GuiMailmanWrite.mail.message.setTag("pages", (NBTBase)this.bookPages);
                if (this.canSend) {
                    if (!this.hasSend) {
                        this.hasSend = true;
                        NoppesUtilPlayer.sendData(EnumPlayerPacket.MailSend, this.username, mail.writeNBT());
                    }
                } else {
                    this.close();
                }
            }
            if (id == 3) {
                this.close();
            }
            if (id == 4) {
                GuiYesNo guiyesno = new GuiYesNo((GuiYesNoCallback)this, "", I18n.translateToLocal((String)"gui.deleteMessage"), 0);
                this.displayGuiScreen((GuiScreen)guiyesno);
            } else if (id == 1) {
                if (this.currPage < this.bookTotalPages - 1) {
                    ++this.currPage;
                } else if (this.canEdit) {
                    this.addNewPage();
                    if (this.currPage < this.bookTotalPages - 1) {
                        ++this.currPage;
                    }
                }
            } else if (id == 2 && this.currPage > 0) {
                --this.currPage;
            }
            this.updateButtons();
        }
    }

    private void addNewPage() {
        if (this.bookPages != null && this.bookPages.tagCount() < 50) {
            this.bookPages.appendTag((NBTBase)new NBTTagString(""));
            ++this.bookTotalPages;
        }
    }

    @Override
    public void keyTyped(char par1, int par2) {
        if (!GuiNpcTextField.isActive() && this.canEdit) {
            this.keyTypedInBook(par1, par2);
        } else {
            super.keyTyped(par1, par2);
        }
    }

    private void keyTypedInBook(char par1, int par2) {
        switch (par1) {
            case '\u0016': {
                this.func_74160_b(GuiScreen.getClipboardString());
                return;
            }
        }
        switch (par2) {
            case 14: {
                String s = this.func_74158_i();
                if (s.length() > 0) {
                    this.func_74159_a(s.substring(0, s.length() - 1));
                }
                return;
            }
            case 28: 
            case 156: {
                this.func_74160_b("\n");
                return;
            }
        }
        if (ChatAllowedCharacters.isAllowedCharacter((char)par1)) {
            this.func_74160_b(Character.toString(par1));
        }
    }

    private String func_74158_i() {
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount()) {
            return this.bookPages.getStringTagAt(this.currPage);
        }
        return "";
    }

    private void func_74159_a(String par1Str) {
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount()) {
            this.bookPages.set(this.currPage, (NBTBase)new NBTTagString(par1Str));
        }
    }

    private void func_74160_b(String par1Str) {
        String s1 = this.func_74158_i();
        String s2 = s1 + par1Str;
        int i = this.mc.fontRenderer.getWordWrappedHeight(s2 + "" + TextFormatting.BLACK + "_", 118);
        if (i <= 118 && s2.length() < 256) {
            this.func_74159_a(s2);
        }
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        this.drawWorldBackground(0);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.getTextureManager().bindTexture(bookGuiTextures);
        this.drawTexturedModalRect(this.guiLeft + 130, this.guiTop + 22, 0, 0, this.bookImageWidth, this.bookImageHeight / 3);
        this.drawTexturedModalRect(this.guiLeft + 130, this.guiTop + 22 + this.bookImageHeight / 3, 0, this.bookImageHeight / 2, this.bookImageWidth, this.bookImageHeight / 2);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop + 2, 0, 0, this.bookImageWidth, this.bookImageHeight);
        this.mc.getTextureManager().bindTexture(bookInventory);
        this.drawTexturedModalRect(this.guiLeft + 20, this.guiTop + 173, 0, 82, 180, 55);
        this.drawTexturedModalRect(this.guiLeft + 20, this.guiTop + 228, 0, 140, 180, 28);
        String s = net.minecraft.client.resources.I18n.format((String)"book.pageIndicator", (Object[])new Object[]{this.currPage + 1, this.bookTotalPages});
        String s1 = "";
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount()) {
            s1 = this.bookPages.getStringTagAt(this.currPage);
        }
        if (this.canEdit) {
            s1 = this.mc.fontRenderer.getBidiFlag() ? s1 + "_" : (this.updateCount / 6 % 2 == 0 ? s1 + "" + TextFormatting.BLACK + "_" : s1 + "" + TextFormatting.GRAY + "_");
        }
        int l = this.mc.fontRenderer.getStringWidth(s);
        this.mc.fontRenderer.drawString(s, this.guiLeft - l + this.bookImageWidth - 44, this.guiTop + 18, 0);
        this.mc.fontRenderer.drawSplitString(s1, this.guiLeft + 36, this.guiTop + 18 + 16, 116, 0);
        this.drawGradientRect(this.guiLeft + 175, this.guiTop + 136, this.guiLeft + 269, this.guiTop + 154, -1072689136, -804253680);
        GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.getTextureManager().bindTexture(bookWidgets);
        for (int i = 0; i < 4; ++i) {
            this.drawTexturedModalRect(this.guiLeft + 175 + i * 24, this.guiTop + 134, 0, 22, 24, 24);
        }
        super.drawScreen(par1, par2, par3);
    }

    @Override
    public void close() {
        this.mc.displayGuiScreen(parent);
        parent = null;
        mail = new PlayerMail();
    }

    @Override
    public void unFocused(GuiNpcTextField textField) {
        if (textField.id == 0) {
            this.username = textField.getText();
        }
        if (textField.id == 1) {
            GuiMailmanWrite.mail.subject = textField.getText();
        }
        if (textField.id == 2) {
            GuiMailmanWrite.mail.sender = textField.getText();
        }
    }

    @Override
    public void setError(int i, NBTTagCompound data) {
        if (i == 0) {
            this.error.label = I18n.translateToLocal((String)"mailbox.errorUsername");
        }
        if (i == 1) {
            this.error.label = I18n.translateToLocal((String)"mailbox.errorSubject");
        }
        this.hasSend = false;
    }

    @Override
    public void setClose(int i, NBTTagCompound data) {
        this.player.sendMessage((ITextComponent)new TextComponentTranslation("mailbox.succes", new Object[]{data.getString("username")}));
    }

    @Override
    public void save() {
    }

    static {
        mail = new PlayerMail();
    }
}

