package com.unascribed.thebappystick;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class GuiBappy extends GuiContainer implements GuiResponder {
	private static final ResourceLocation BG = new ResourceLocation("thebappystick", "textures/gui/thebappystick.png");
	private static final ResourceLocation KATLOAF = new ResourceLocation("thebappystick", "textures/gui/katloaf.png");

	private final ContainerBappy container;
	private int fun = ThreadLocalRandom.current().nextInt(40000);

	private boolean hoveringCurrent = false;

	private int cooldownTicks = 0;

	private GuiTextField name;

	public GuiBappy(ContainerBappy container) {
		super(container);
		this.container = container;
		xSize = 194;
		ySize = 219;
	}

	@Override
	public void initGui() {
		super.initGui();
		buttonList.add(new GuiButtonExt(0, ((width-xSize)/2)+72, ((height-ySize)/2)+18, 22, 18, ""));
		buttonList.add(new GuiButtonExt(1, ((width-xSize)/2)+98, ((height-ySize)/2)+18, 22, 18, ""));
		boolean oldFocus = name != null ? name.isFocused() : false;
		name = new GuiTextField(2, fontRenderer, ((width-xSize)/2)+4, ((height-ySize)/2)+4, 118, 12);
		name.setCanLoseFocus(true);
		name.setFocused(oldFocus);
		name.setText(container.getItem().getDisplayName());
		name.setTextColor(-1);
		name.setGuiResponder(this);
		name.setMaxStringLength(35);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		mc.playerController.sendEnchantPacket(container.windowId, button.id);
		if (button.id == 0) {
			button.enabled = false;
			cooldownTicks = 10;
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		name.updateCursorCounter();
		if (!name.isFocused()) {
			name.setText(container.getItem().getDisplayName());
		}
		buttonList.get(0).enabled = (cooldownTicks <= 0 && !TheBappyStick.THEBAPPYSTICK.getCurrentResource(container.getItem()).isEmpty());
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		GlStateManager.disableLighting();
		name.drawTextBox();
		renderHoveredToolTip(mouseX, mouseY);
		if (hoveringCurrent) {
			ItemStack current = TheBappyStick.THEBAPPYSTICK.getCurrentResource(container.getItem());
			if (current.isEmpty()) {
				drawTooltip("current_resource", mouseX, mouseY);
			} else {
				List<String> tooltip = getItemToolTip(current);
				int hitsRemaining = TheBappyStick.THEBAPPYSTICK.getHitsRemaining(container.getItem());
				int maxHitsRemaining = TheBappyStick.THEBAPPYSTICK.getMaxHitsRemaining(container.getItem());
				tooltip.add(1, "\u00A77"+I18n.format("gui.thebappystick.durability", hitsRemaining, maxHitsRemaining));
				drawHoveringText(tooltip, mouseX, mouseY);
			}
		} else if (TheBappyStick.inst.allowAutosmelt && isPointInRegion(27, 43, 16, 16, mouseX, mouseY)) {
			List<String> tooltip = Lists.newArrayList();
			int fuelRemaining = TheBappyStick.THEBAPPYSTICK.getFuelRemaining(container.getItem());
			tooltip.add(I18n.format("gui.thebappystick.fuel_remaining", fuelRemaining/200));
			drawHoveringText(tooltip, mouseX, mouseY);
		} else if (buttonList.get(0).isMouseOver()) {
			drawTooltip("purge", mouseX, mouseY);
		} else if (buttonList.get(1).isMouseOver()) {
			drawTooltip("anvil", mouseX, mouseY);
		} else if (isPointInRegion(7, 19, 16, 16, mouseX, mouseY)
				&& TheBappyStick.THEBAPPYSTICK.getInputResources(container.getItem()).isEmpty()) {
			drawTooltip("input_resource", mouseX, mouseY);
		} else if (TheBappyStick.inst.allowAutosmelt && isPointInRegion(7, 43, 16, 16, mouseX, mouseY)
				&& TheBappyStick.THEBAPPYSTICK.getFuelInput(container.getItem()).isEmpty()) {
			drawTooltip("fuel", mouseX, mouseY);
		} else if (!name.isFocused() && isPointInRegion(4, 4, 118, 12, mouseX, mouseY)) {
			drawTooltip("rename", mouseX, mouseY);
		}
		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 3; col++) {
				int i = col + (row * 3);
				if (isPointInRegion(131 + (col * 18), 19 + (row * 18), 16, 16, mouseX, mouseY)
						&& TheBappyStick.THEBAPPYSTICK.getTalisman(container.getItem(), i).isEmpty()) {
					drawTooltip("talismans", mouseX, mouseY);
					break;
				}
			}
		}
		if (TheBappyStick.inst.allowTorchPlacement) {
			for (int i = 0; i < 6; i++) {
				if (isPointInRegion(7+(i*18), 106, 16, 16, mouseX, mouseY)
						&& TheBappyStick.THEBAPPYSTICK.getTorch(container.getItem(), i).isEmpty()) {
					drawTooltip("torches", mouseX, mouseY);
					break;
				}
			}
		}
	}

	private void drawTooltip(String key, int mouseX, int mouseY) {
		List<String> li = Lists.newArrayList();
		int i = 0;
		while (I18n.hasKey("gui.thebappystick."+key+"."+i)) {
			li.add(I18n.format("gui.thebappystick."+key+"."+i));
			i++;
		}
		drawHoveringText(li, mouseX, mouseY);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (name.isFocused()) {
			if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
				name.setFocused(false);
			}
			name.textboxKeyTyped(typedChar, keyCode);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		name.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.pushMatrix();
		GlStateManager.translate((width-xSize)/2, (height-ySize)/2, 0);
		GlStateManager.color(1, 1, 1);
		mc.renderEngine.bindTexture(BG);
		drawTexturedModalRect(0, 0, 0, 0, xSize, ySize);
		if (TheBappyStick.inst.allowTorchPlacement && !TheBappyStick.inst.freeTorches) {
			drawTexturedModalRect(6, 105, 0, 233, 118, 18);
		}
		if (TheBappyStick.inst.allowAutosmelt) {
			drawTexturedModalRect(6, 42, 118, 233, 40, 18);
		}
		fontRenderer.drawString(I18n.format("gui.thebappystick.talismans"), 130, 9, 0xBFBFBF);
		fontRenderer.drawString(I18n.format("container.inventory"), 6, 126, 0x404040);
		if (fun == 1337) {
			mc.renderEngine.bindTexture(KATLOAF);
			GlStateManager.color(1, 1, 1);
			GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.enableBlend();
			drawModalRectWithCustomSizedTexture(6, 62, 0, 0, 40, 40, 40, 40);
			mc.renderEngine.bindTexture(BG);
			GlStateManager.color(1, 1, 1, 0.9f);
			drawTexturedModalRect(6, 62, 6, 62, 40, 40);
		}
		ItemStack current = TheBappyStick.THEBAPPYSTICK.getCurrentResource(container.getItem());
		if (!current.isEmpty()) {
			GlStateManager.disableLighting();
			double atkDmg = container.getItem().getAttributeModifiers(EntityEquipmentSlot.MAINHAND)
					.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName()).iterator().next().getAmount();
			fontRenderer.drawString(I18n.format("gui.thebappystick.stat.mining_speed", (int)(container.getItem().getDestroySpeed(Blocks.DIRT.getDefaultState())*100)), 52, 44, 0x404040);
			fontRenderer.drawString(I18n.format("gui.thebappystick.stat.attack_damage", ItemStack.DECIMALFORMAT.format(atkDmg)), 52, 56, 0x404040);
			int lvl = container.getItem().getItem().getHarvestLevel(container.getItem(), "pickaxe", null, null);
			fontRenderer.drawString(I18n.format("gui.thebappystick.stat.mining_level", lvl), 52, 68, 0x404040);
			if (I18n.hasKey("gui.thebappystick.stat.mining_level."+lvl)) {
				fontRenderer.drawString("  "+I18n.format("gui.thebappystick.stat.mining_level."+lvl), 52, 80, 0x404040);
			}
		} else {
			fontRenderer.drawString(I18n.format("gui.thebappystick.no_item"), 52, 44, 0x404040);
		}
		GlStateManager.popMatrix();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		RenderHelper.enableGUIStandardItemLighting();
		ItemStack current = TheBappyStick.THEBAPPYSTICK.getCurrentResource(container.getItem());
		itemRender.renderItemAndEffectIntoGUI(mc.player, current, 51, 19);
		RenderHelper.disableStandardItemLighting();
		if (!current.isEmpty()) {
			int hitsRemaining = TheBappyStick.THEBAPPYSTICK.getHitsRemaining(container.getItem());
			int maxHitsRemaining = TheBappyStick.THEBAPPYSTICK.getMaxHitsRemaining(container.getItem());
			float health = hitsRemaining == 0 ? 0 : ((float)(maxHitsRemaining-hitsRemaining)/(float)maxHitsRemaining);
			int rgb = hitsRemaining == 0 ? 0xFFD50000 : MathHelper.hsvToRGB(Math.max(0, (1 - health)) / 3, 1, 1);
			GlStateManager.disableDepth();
			GlStateManager.disableTexture2D();
			int width = Math.round(13 - (health * 13));
			drawRect(51 + 2, 19 + 13, 51 + 2 + 13, 19 + 13 + 2, 0xFF000000);
			drawRect(51 + 2, 19 + 13, 51 + 2 + width, 19 + 13 + 1, rgb|0xFF000000);
			GlStateManager.enableTexture2D();
			GlStateManager.enableDepth();
		}
		if (isPointInRegion(51, 19, 16, 16, mouseX, mouseY)) {
			hoveringCurrent = true;
			GlStateManager.disableLighting();
			GlStateManager.disableDepth();
			GlStateManager.colorMask(true, true, true, false);
			drawGradientRect(51, 19, 51 + 16, 19 + 16, 0x80FFFFFF, 0x80FFFFFF);
			GlStateManager.colorMask(true, true, true, true);
			GlStateManager.enableLighting();
			GlStateManager.enableDepth();
		} else {
			hoveringCurrent = false;
		}
		int fuelRemaining = TheBappyStick.THEBAPPYSTICK.getFuelRemaining(container.getItem());
		int maxFuelRemaining = TheBappyStick.THEBAPPYSTICK.getMaxFuelRemaining(container.getItem());
		float burnRemaining = fuelRemaining/(float)maxFuelRemaining;
		int height = (int)(burnRemaining*14);
		GlStateManager.disableLighting();
		mc.renderEngine.bindTexture(BG);
		GlStateManager.color(1, 1, 1);
		drawTexturedModalRect(29, 44+(14-height), 0, 219+(14-height), 14, height);
		GlStateManager.enableAlpha();
		drawTexturedModalRect(100, 18, 158, 233, 18, 18);
		if (!buttonList.get(0).enabled) {
			GlStateManager.color(0.5f, 0.5f, 0.5f);
		}
		drawTexturedModalRect(74, 18, 177, 233, 18, 18);
		GlStateManager.disableAlpha();
		int dx = 10;
		int dy = 64;
		int amt = 0;
		for (ItemStack delegate : TheBappyStick.THEBAPPYSTICK.getDelegateItems(container.getItem())) {
			if (amt >= 4) break;
			mc.getRenderItem().renderItemIntoGUI(delegate, dx, dy);
			dx += 18;
			if (dx+16 >= 46) {
				dx = 10;
				dy += 18;
			}
			amt++;
		}
	}

	@Override
	public void setEntryValue(int id, boolean value) {
	}

	@Override
	public void setEntryValue(int id, float value) {
	}

	@Override
	public void setEntryValue(int id, String value) {
		if (id == 2) {
			RenameBappyStickMessage msg = new RenameBappyStickMessage();
			msg.windowId = container.windowId;
			msg.name = value;
			TheBappyStick.inst.network.sendToServer(msg);
		}
	}


}
