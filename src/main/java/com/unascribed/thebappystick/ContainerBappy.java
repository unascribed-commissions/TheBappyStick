package com.unascribed.thebappystick;

import java.util.function.ToDoubleFunction;

import com.google.common.base.Predicate;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.common.util.Constants.NBT;

public class ContainerBappy extends Container {

	private final EntityPlayer player;

	private static class FakeSlot extends Slot {
		private final boolean enabled;
		private ItemStack lastReturnedStack;

		public FakeSlot(int index, int xPosition, int yPosition, boolean enabled) {
			super(null, index, xPosition, yPosition);
			this.enabled = enabled;
		}

		@Override
		public ItemStack getStack() {
			return ItemStack.EMPTY;
		}

		@Override
		public void putStack(ItemStack stack) {
		}

		protected ItemStack setLastReturnedStack(ItemStack stack) {
			lastReturnedStack = stack;
			return stack;
		}

		@Override
		public void onSlotChanged() {
			putStack(lastReturnedStack);
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}

		@Override
		public int getSlotStackLimit() {
			return 64;
		}

		@Override
		public boolean isHere(IInventory inv, int slotIn) {
			return false;
		}

		@Override
		public ItemStack decrStackSize(int amount) {
			ItemStack stack = getStack();
			ItemStack rtrn = stack.splitStack(amount);
			putStack(stack);
			return rtrn;
		}
	}

	public ContainerBappy(EntityPlayer player) {
		this.player = player;

		addSlotToContainer(new FakeSlot(0, 7, 19, true) {
			@Override
			public ItemStack getStack() {
				return setLastReturnedStack(TheBappyStick.THEBAPPYSTICK.getInputResources(getItem()));
			}

			@Override
			public void putStack(ItemStack stack) {
				setLastReturnedStack(stack);
				TheBappyStick.THEBAPPYSTICK.setInputResources(getItem(), stack);
			}

			@Override
			public boolean isItemValid(ItemStack stack) {
				return isValidResource(stack);
			}

			@Override
			public void onSlotChanged() {
				super.onSlotChanged();
				if (TheBappyStick.THEBAPPYSTICK.getCurrentResource(getItem()).isEmpty()) {
					TheBappyStick.THEBAPPYSTICK.consumeResource(player, getItem());
				}
			}

		});

		addSlotToContainer(new FakeSlot(1, 7, 43, TheBappyStick.inst.allowAutosmelt) {
			@Override
			public ItemStack getStack() {
				return setLastReturnedStack(TheBappyStick.THEBAPPYSTICK.getFuelInput(getItem()));
			}

			@Override
			public void putStack(ItemStack stack) {
				setLastReturnedStack(stack);
				TheBappyStick.THEBAPPYSTICK.setFuelInput(getItem(), stack);
			}

			@Override
			public boolean isItemValid(ItemStack stack) {
				return TileEntityFurnace.isItemFuel(stack);
			}

		});

		for (int i = 0; i < 6; i++) {
			final int j = i;
			addSlotToContainer(new FakeSlot(i+1, 7 + (i * 18), 106, TheBappyStick.inst.allowTorchPlacement && !TheBappyStick.inst.freeTorches) {

				@Override
				public ItemStack getStack() {
					return setLastReturnedStack(TheBappyStick.THEBAPPYSTICK.getTorch(getItem(), j));
				}

				@Override
				public void putStack(ItemStack stack) {
					setLastReturnedStack(stack);
					TheBappyStick.THEBAPPYSTICK.setTorch(getItem(), j, stack);
				}

				@Override
				public boolean isItemValid(ItemStack stack) {
					return isValidTorch(stack);
				}
			});
		}

		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 3; col++) {
				int i = col + (row * 3);
				addSlotToContainer(new FakeSlot(col + (row * 9) + 9, 131 + (col * 18), 19 + (row * 18), true) {
					@Override
					public ItemStack getStack() {
						return setLastReturnedStack(TheBappyStick.THEBAPPYSTICK.getTalisman(getItem(), i));
					}

					@Override
					public void putStack(ItemStack stack) {
						setLastReturnedStack(stack);
						TheBappyStick.THEBAPPYSTICK.setTalisman(getItem(), i, stack);
					}

					@Override
					public boolean isItemValid(ItemStack stack) {
						return isValidTalisman(stack, i);
					}
				});
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlotToContainer(new Slot(player.inventory, col + (row * 9) + 9, 7 + (col * 18), 137 + (row * 18)));
			}
		}

		for (int i = 0; i < 9; i++) {
			boolean editable = i != player.inventory.currentItem;
			addSlotToContainer(new Slot(player.inventory, i, 7 + (i * 18), 195) {
				@Override
				public boolean canTakeStack(EntityPlayer playerIn) {
					return editable;
				}
			});
		}

	}

	private boolean isValidResource(ItemStack stack) {
		for (BappyResource res : TheBappyStick.inst.resources) {
			for (ToDoubleFunction<ItemStack> func : res.validItems) {
				if (func.applyAsDouble(stack) > 0) return true;
			}
		}
		return false;
	}

	private boolean isValidTorch(ItemStack stack) {
		for (Predicate<ItemStack> pred : TheBappyStick.inst.torches) {
			if (pred.test(stack)) return true;
		}
		return false;
	}

	private EnchantmentData getEnchantmentData(ItemStack stack) {
		if (stack.getItem() == TheBappyStick.TALISMAN) {
			if (!stack.hasTagCompound()) return null;
			NBTTagList enchList = stack.getTagCompound().getTagList("ench", NBT.TAG_COMPOUND);
			if (enchList.tagCount() == 0) return null;
			NBTTagCompound first = enchList.getCompoundTagAt(0);
			int id = first.getInteger("id");
			int lvl = first.getInteger("lvl");
			Enchantment ench = Enchantment.getEnchantmentByID(id);
			if (ench == null) return null;
			return new EnchantmentData(ench, lvl);
		}
		return null;
	}

	private boolean isValidTalisman(ItemStack stack, int excludingSlot) {
		EnchantmentData ed = getEnchantmentData(stack);
		if (ed == null) return true;
		for (int i = 0; i < 18; i++) {
			if (i == excludingSlot) continue;
			ItemStack existing = TheBappyStick.THEBAPPYSTICK.getTalisman(getItem(), i);
			EnchantmentData existingEd = getEnchantmentData(existing);
			if (existingEd == null) continue;
			if (!existingEd.enchantment.isCompatibleWith(ed.enchantment)) return false;
		}
		return true;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack copy = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);

		if (slot != null && slot.getHasStack()) {
			ItemStack original = slot.getStack();
			copy = original.copy();
			boolean torchesEnabled = TheBappyStick.inst.allowTorchPlacement && !TheBappyStick.inst.freeTorches;
			boolean fuelEnabled = TheBappyStick.inst.allowAutosmelt;
			if (index > 25 && isValidResource(copy) && mergeItemStack(copy, 0, 1, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			} else if (index > 25 && fuelEnabled && TileEntityFurnace.isItemFuel(copy) && mergeItemStack(copy, 1, 2, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			} else if (index > 25 && torchesEnabled && isValidTorch(copy) && mergeItemStack(copy, 2, 8, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			} else if (index > 25 && isValidTalisman(copy, -1) && mergeItemStack(copy, 8, 26, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			} else if (index > 52 && mergeItemStack(copy, 26, 53, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			} else if (index > 25 && index < 53 && mergeItemStack(copy, 53, 62, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			} else if (index <= 25 && mergeItemStack(copy, 26, 62, false)) {
				slot.putStack(copy);
				return ItemStack.EMPTY;
			}
			if (original.getCount() == copy.getCount()) {
				return ItemStack.EMPTY;
			}
		}
		return copy;
	}

	@Override
	public boolean enchantItem(EntityPlayer playerIn, int id) {
		if (id == 0) {
			TheBappyStick.THEBAPPYSTICK.consumeResource(playerIn, getItem());
		} else if (id == 1) {
			playerIn.openGui(TheBappyStick.inst, 1, playerIn.world, 0, 0, 0);
		}
		return super.enchantItem(playerIn, id);
	}

	public ItemStack getItem() {
		ItemStack held = player.getHeldItemMainhand();
		if (held.getItem() == TheBappyStick.THEBAPPYSTICK) {
			return held;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return playerIn == player && !getItem().isEmpty();
	}

}
