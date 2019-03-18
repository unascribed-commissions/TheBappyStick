package com.unascribed.thebappystick;

import java.io.IOException;
import java.util.Map;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.util.Constants.NBT;

public class RenderTalisman extends TileEntityItemStackRenderer {
	private static final CharMatcher MATCHER_A_Z = CharMatcher.inRange('a', 'z');

	private final ItemStack dummy = new ItemStack(TheBappyStick.RENDER_DUMMY);

	private Map<Enchantment, ResourceLocation> iconCache = Maps.newHashMap();
	private boolean registeredListener = false;

	@Override
	public void renderByItem(ItemStack stack, float partialTicks) {
		if (!registeredListener) {
			((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener((irm) -> {
				iconCache.clear();
			});
			registeredListener = true;
		}
		TransformType tt = DynamicBakedModel.lastTransformType;
		GlStateManager.pushMatrix();
		switch (tt) {
			case FIRST_PERSON_LEFT_HAND:
				GlStateManager.translate(1, 0.5, 0);
				break;
			case FIRST_PERSON_RIGHT_HAND:
				GlStateManager.translate(0, 0.5, 0);
				break;
			case GROUND:
				case FIXED:
			case THIRD_PERSON_LEFT_HAND:
			case THIRD_PERSON_RIGHT_HAND:
				GlStateManager.translate(0.5, 0.5, 0.5);
				break;
			case GUI:
				GlStateManager.translate(0.5, 0.5, 0);
				break;
			case HEAD:
			case NONE:
				break;
			default:
				break;

		}
		Minecraft.getMinecraft().getRenderItem().renderItem(dummy, TransformType.NONE);
		if (stack.hasTagCompound()) {
			NBTTagList enchList = stack.getTagCompound().getTagList("ench", NBT.TAG_COMPOUND);
			if (enchList.tagCount() > 0) {
				NBTTagCompound firstEnch = enchList.getCompoundTagAt(0);
				int id = firstEnch.getInteger("id");
				Enchantment ench = Enchantment.getEnchantmentByID(id);
				if (ench != null) {
					ResourceLocation tex = null;
					if (iconCache.containsKey(ench)) {
						tex = iconCache.get(ench);
					} else {
						ResourceLocation sigil = new ResourceLocation("thebappystick", "textures/items/talisman_enchantments/"+ench.getRegistryName().getNamespace()+"/"+ench.getRegistryName().getPath()+".png");
						try {
							Minecraft.getMinecraft().getResourceManager().getResource(sigil);
							tex = sigil;
						} catch (IOException e) {
							String englishName = I18n.translateToFallback(ench.getName());
							if (!englishName.isEmpty()) {
								char ch = englishName.charAt(0);
								if (CharMatcher.ascii().matches(ch)) {
									char first = Ascii.toLowerCase(ch);
									if (MATCHER_A_Z.matches(first)) {
										tex = new ResourceLocation("thebappystick", "textures/items/talisman_enchantments/"+first+".png");
									}
								}
							}
						}
					}
					if (tex != null) {
						GlStateManager.disableCull();
						GlStateManager.disableLighting();
						GlStateManager.disableDepth();
						GlStateManager.color(1, 1, 1);
						GlStateManager.translate(-0.5, 0.5, 0);
						GlStateManager.scale(1, -1, 1);
						Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
						Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 1, 1, 1, 1);
						Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
						GlStateManager.enableCull();
						GlStateManager.enableLighting();
						GlStateManager.enableDepth();
					}
				}
			}
		}
		GlStateManager.popMatrix();
	}

}
