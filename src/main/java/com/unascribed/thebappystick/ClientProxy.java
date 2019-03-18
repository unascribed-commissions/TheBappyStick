package com.unascribed.thebappystick;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends Proxy {

	@Override
	public void onPreInit() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent e) {
		e.getModelRegistry().putObject(new ModelResourceLocation("thebappystick:talisman_dynamic#inventory"), new DynamicBakedModel());
	}

	@SubscribeEvent
	public void onModelRegister(ModelRegistryEvent e) {
		ModelLoader.setCustomModelResourceLocation(TheBappyStick.TALISMAN, 0, new ModelResourceLocation("thebappystick:talisman_dynamic#inventory"));
		TheBappyStick.TALISMAN.setTileEntityItemStackRenderer(new RenderTalisman());

		ModelLoader.setCustomModelResourceLocation(TheBappyStick.THEBAPPYSTICK, 0,
				new ModelResourceLocation("thebappystick:thebappystick#inventory"));
		ModelLoader.setCustomModelResourceLocation(TheBappyStick.RENDER_DUMMY, 0,
				new ModelResourceLocation("thebappystick:talisman#inventory"));
		ModelLoader.setCustomModelResourceLocation(TheBappyStick.LAPIS_NUGGET, 0,
				new ModelResourceLocation("thebappystick:lapis_nugget#inventory"));
	}

	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void onTooltip(ItemTooltipEvent e) {
		if (e.getItemStack().getItem() == TheBappyStick.TALISMAN) {
			if (e.getItemStack().hasTagCompound() && e.getItemStack().getTagCompound().getTagList("ench", NBT.TAG_COMPOUND).tagCount() > 1) {
				// hide enchantments until onUpdate can kick in
				for (int i = 2; i < e.getToolTip().size(); i++) {
					if (!e.getToolTip().get(i).startsWith("\u00A7")) {
						e.getToolTip().remove(i);
						i--;
					}
				}
			}
		}
	}

}
