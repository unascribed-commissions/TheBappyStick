package com.unascribed.thebappystick;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

public class ItemTalisman extends Item {

	public ItemTalisman() {
		setMaxStackSize(1);
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		if (TheBappyStick.inst.blacklistedEnchants.contains(enchantment)) {
			return false;
		}
		if (TheBappyStick.inst.inheritDurabilityEnchants && enchantment.type == EnumEnchantmentType.BREAKABLE) {
			return true;
		}
		if (TheBappyStick.inst.inheritToolEnchants && enchantment.type == EnumEnchantmentType.DIGGER) {
			return true;
		}
		if (TheBappyStick.inst.inheritWeaponEnchants && enchantment.type == EnumEnchantmentType.WEAPON) {
			return true;
		}
		return TheBappyStick.inst.whitelistedEnchants.contains(enchantment);
	}

	@Override
	public int getItemEnchantability() {
		return 22;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
	}

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		if (stack.isItemEnchanted()) {
			NBTTagList ench = stack.getTagCompound().getTagList("ench", NBT.TAG_COMPOUND);
			while (ench.tagCount() > 1) {
				ench.removeTag(1);
			}
		}
	}

}
