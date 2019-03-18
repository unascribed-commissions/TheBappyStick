package com.unascribed.thebappystick;

import java.util.List;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.ImmutableList;

import net.minecraft.item.ItemStack;

public class BappyResource {

	public final String name;

	public final ImmutableList<ToDoubleFunction<ItemStack>> validItems;
	public final ImmutableList<ItemStack> delegateItems;

	public BappyResource(String name, List<ToDoubleFunction<ItemStack>> validItems, List<ItemStack> delegateItems) {
		this.name = name;
		this.validItems = ImmutableList.copyOf(validItems);
		this.delegateItems = ImmutableList.copyOf(delegateItems);
	}

}
