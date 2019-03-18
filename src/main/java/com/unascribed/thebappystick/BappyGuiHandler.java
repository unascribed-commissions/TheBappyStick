package com.unascribed.thebappystick;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiRepair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.Slot;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class BappyGuiHandler implements IGuiHandler {

	@Override
	@Nullable
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		if (id == 0) {
			return new ContainerBappy(player);
		} else if (id == 1) {
			return new ContainerRepair(player.inventory, player.world, player.getPosition(), player) {
				{
					replaceHeldSlot(player, this);
				}
				@Override
				public boolean canInteractWith(EntityPlayer playerIn) {
					return playerIn.getHeldItemMainhand().getItem() == TheBappyStick.THEBAPPYSTICK;
				}
			};
		}
		return null;
	}

	private void replaceHeldSlot(EntityPlayer player, Container c) {
		Slot old = c.getSlotFromInventory(player.inventory, player.inventory.currentItem);
		if (old != null) {
			Slot nw = new Slot(player.inventory, player.inventory.currentItem, old.xPos, old.yPos) {
				@Override
				public boolean canTakeStack(EntityPlayer playerIn) {
					return false;
				}
			};
			nw.slotNumber = old.slotNumber;
			c.inventorySlots.set(old.slotNumber, nw);
		}
	}

	@Override
	@Nullable
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		if (id == 0) {
			return new GuiBappy(new ContainerBappy(player));
		} else if (id == 1) {
			return new GuiRepair(player.inventory, world) {
				{
					replaceHeldSlot(player, inventorySlots);
				}
			};
		}
		return null;
	}

}
