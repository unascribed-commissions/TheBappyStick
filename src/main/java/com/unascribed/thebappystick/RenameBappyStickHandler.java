package com.unascribed.thebappystick;

import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RenameBappyStickHandler implements IMessageHandler<RenameBappyStickMessage, IMessage> {

	@Override
	public IMessage onMessage(RenameBappyStickMessage message, MessageContext ctx) {
		Container c = ctx.getServerHandler().player.openContainer;
		if (c != null && c.windowId == message.windowId) {
			String name = ChatAllowedCharacters.filterAllowedCharacters(message.name);
			if (name.length() <= 35) {
				ItemStack held = ctx.getServerHandler().player.getHeldItemMainhand();
				if (held.getItem() == TheBappyStick.THEBAPPYSTICK) {
					if (name.trim().isEmpty()) {
						held.clearCustomName();
					} else {
						held.setStackDisplayName(name);
					}
				}
			}
		}
		return null;
	}

}
