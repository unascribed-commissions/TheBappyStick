package com.unascribed.thebappystick;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentDurability;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

public class ItemBappyStick extends Item {

	public ItemBappyStick() {
		setMaxStackSize(1);
		MinecraftForge.EVENT_BUS.register(this);
	}

	private boolean hasKey(ItemStack stack, String name, int tagType) {
		return stack.hasTagCompound() && stack.getTagCompound().hasKey(name, tagType);
	}

	private ItemStack getStackTag(ItemStack stack, String name) {
		if (!hasKey(stack, name, NBT.TAG_COMPOUND)) return ItemStack.EMPTY;
		return new ItemStack(stack.getTagCompound().getCompoundTag(name));
	}

	private void ensureHasTag(ItemStack stack) {
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
	}

	public List<ItemStack> getDelegateItems(ItemStack stack) {
		if (!hasKey(stack, "DelegateItems", NBT.TAG_LIST)) return Collections.emptyList();
		NBTTagList list = stack.getTagCompound().getTagList("DelegateItems", NBT.TAG_COMPOUND);
		List<ItemStack> out = Lists.newArrayListWithCapacity(list.tagCount());
		for (int i = 0; i < list.tagCount(); i++) {
			out.add(new ItemStack(list.getCompoundTagAt(i)));
		}
		return out;
	}

	public ItemStack getInputResources(ItemStack stack) {
		return getStackTag(stack, "InputResources");
	}

	public ItemStack getCurrentResource(ItemStack stack) {
		return getStackTag(stack, "CurrentResource");
	}

	public ItemStack getFuelInput(ItemStack stack) {
		return getStackTag(stack, "FuelInput");
	}

	public ItemStack getTorch(ItemStack stack, int slot) {
		if (slot < 0 || slot >= 6) throw new IndexOutOfBoundsException(""+slot);
		return getStackTag(stack, "Torch"+slot);
	}

	public ItemStack getTalisman(ItemStack stack, int slot) {
		if (slot < 0 || slot >= 18) throw new IndexOutOfBoundsException(""+slot);
		return getStackTag(stack, "Talisman"+slot);
	}

	public int getFuelRemaining(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger("FuelRemaining") : 0;
	}

	public int getMaxFuelRemaining(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger("MaxFuelRemaining") : 0;
	}

	public int getHitsRemaining(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger("HitsRemaining") : 0;
	}

	public int getMaxHitsRemaining(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger("MaxHitsRemaining") : 0;
	}

	public int getOveruseBreakChance(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger("OveruseBreakChance") : 0;
	}

	public void setDelegateItems(ItemStack stack, List<ItemStack> delegates) {
		if (delegates == null) delegates = Collections.emptyList();
		NBTTagList nbt = new NBTTagList();
		for (ItemStack is : delegates) {
			nbt.appendTag(is.serializeNBT());
		}
		ensureHasTag(stack);
		stack.getTagCompound().setTag("DelegateItems", nbt);
	}

	public void setInputResources(ItemStack stack, ItemStack inputResources) {
		ensureHasTag(stack);
		stack.getTagCompound().setTag("InputResources", inputResources.serializeNBT());
	}

	public void setCurrentResource(ItemStack stack, ItemStack currentResource) {
		ensureHasTag(stack);
		stack.getTagCompound().setTag("CurrentResource", currentResource.serializeNBT());
	}

	public void setFuelInput(ItemStack stack, ItemStack fuelInput) {
		ensureHasTag(stack);
		stack.getTagCompound().setTag("FuelInput", fuelInput.serializeNBT());
	}

	public void setTorch(ItemStack stack, int slot, ItemStack torch) {
		if (slot < 0 || slot >= 6) throw new IndexOutOfBoundsException(""+slot);
		ensureHasTag(stack);
		stack.getTagCompound().setTag("Torch"+slot, torch.serializeNBT());
	}

	public void setTalisman(ItemStack stack, int slot, ItemStack talisman) {
		if (slot < 0 || slot >= 18) throw new IndexOutOfBoundsException(""+slot);
		ensureHasTag(stack);
		stack.getTagCompound().setTag("Talisman"+slot, talisman.serializeNBT());
		updateEnchantments(stack);
	}

	public void setFuelRemaining(ItemStack stack, int fuelRemaining) {
		ensureHasTag(stack);
		stack.getTagCompound().setInteger("FuelRemaining", fuelRemaining);
	}

	public void setMaxFuelRemaining(ItemStack stack, int maxFuelRemaining) {
		ensureHasTag(stack);
		stack.getTagCompound().setInteger("MaxFuelRemaining", maxFuelRemaining);
	}

	public void setHitsRemaining(ItemStack stack, int hitsRemaining) {
		ensureHasTag(stack);
		stack.getTagCompound().setInteger("HitsRemaining", hitsRemaining);
	}

	public void setMaxHitsRemaining(ItemStack stack, int maxHitsRemaining) {
		ensureHasTag(stack);
		stack.getTagCompound().setInteger("MaxHitsRemaining", maxHitsRemaining);
	}

	public void setOveruseBreakChance(ItemStack stack, int overuseBreakChance) {
		ensureHasTag(stack);
		stack.getTagCompound().setInteger("OveruseBreakChance", overuseBreakChance);
	}

	@Override
	public boolean hasEffect(ItemStack stack) {
		return TheBappyStick.inst.showEnchantGlint && stack.isItemEnchanted();
	}

	@Override @SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		ItemStack current = getCurrentResource(stack);
		if (!current.isEmpty()) {
			int hitsRemaining = getHitsRemaining(stack);
			int maxHitsRemaining = getMaxHitsRemaining(stack);
			tooltip.add("\u00A77"+I18n.format("item.thebappystick.current_resource", current.getDisplayName()));
			tooltip.add("\u00A77"+I18n.format("gui.thebappystick.durability", hitsRemaining, maxHitsRemaining));
			if (TheBappyStick.inst.allowTorchPlacement) {
				int torches = 0;
				for (int i = 0; i < 6; i++) {
					torches += getTorch(stack, i).getCount();
				}
				tooltip.add("\u00A77"+I18n.format("item.thebappystick.torches_left", torches));
			}
			tooltip.add("");
		}
		int i = 0;
		while (I18n.hasKey("item.thebappystick."+i)) {
			tooltip.add(I18n.format("item.thebappystick."+i));
			i++;
		}
		tooltip.add("");
	}

	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
		if (slot != EntityEquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
		String atkDmg = SharedMonsterAttributes.ATTACK_DAMAGE.getName();
		String atkSpd = SharedMonsterAttributes.ATTACK_SPEED.getName();
		Multimap<String, AttributeModifier> out = HashMultimap.create();
		double dmg = Double.NEGATIVE_INFINITY;
		double spd = Double.POSITIVE_INFINITY;
		for (ItemStack is : getDelegateItems(stack)) {
			Multimap<String, AttributeModifier> mm = is.getAttributeModifiers(slot);
			if (mm.containsKey(atkDmg)) {
				for (AttributeModifier am : mm.get(atkDmg)) {
					if (am.getOperation() != 0) continue;
					dmg = Math.max(am.getAmount(), dmg);
				}
			}
			if (mm.containsKey(atkSpd)) {
				for (AttributeModifier am : mm.get(atkSpd)) {
					if (am.getOperation() != 0) continue;
					spd = Math.min(am.getAmount(), spd);
				}
			}
		}
		if (!Double.isFinite(dmg)) {
			dmg = 0;
		}
		if (!Double.isFinite(spd)) {
			spd = 0;
		}
		// adjust damage and speed to their actual values
		// makes the config options act how they'd be expected to work
		dmg += 1;
		spd += 4;
		dmg *= TheBappyStick.inst.attackDamage;
		spd *= TheBappyStick.inst.attackSpeed;
		dmg -= 1;
		spd -= 4;
		out.put(atkDmg, new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", dmg, 0));
		out.put(atkSpd, new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", spd, 0));
		return out;
	}

	private void updateEnchantments(ItemStack stack) {
		NBTTagList ench = new NBTTagList();
		for (int i = 0; i < 18; i++) {
			ItemStack talisman = getTalisman(stack, i);
			if (talisman.hasTagCompound()) {
				NBTTagList li = talisman.getTagCompound().getTagList("ench", NBT.TAG_COMPOUND);
				if (li.tagCount() >= 1) {
					ench.appendTag(li.get(0).copy());
				}
			}
		}
		ensureHasTag(stack);
		stack.getTagCompound().setTag("ench", ench);
	}

	@Override
	public Set<String> getToolClasses(ItemStack stack) {
		Set<String> set = Sets.newHashSet();
		for (ItemStack is : getDelegateItems(stack)) {
			set.addAll(is.getItem().getToolClasses(is));
		}
		return set;
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass, @Nullable EntityPlayer player, @Nullable IBlockState blockState) {
		int max = -1;
		for (ItemStack is : getDelegateItems(stack)) {
			max = Math.max(is.getItem().getHarvestLevel(stack, toolClass, player, blockState), max);
		}
		return max;
	}

	@Override
	public float getDestroySpeed(ItemStack stack, IBlockState state) {
		float max = 0;
		boolean hasAnyDelegate = false;
		for (ItemStack is : getDelegateItems(stack)) {
			max = Math.max(is.getItem().getDestroySpeed(stack, state), max);
			hasAnyDelegate = true;
		}
		return hasAnyDelegate ? (float)(max * TheBappyStick.inst.miningSpeed) : 1.0f;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (playerIn.isSneaking() && handIn == EnumHand.MAIN_HAND) {
			playerIn.openGui(TheBappyStick.inst, 0, worldIn, 0, 0, 0);
			return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer playerIn, EntityLivingBase entity, EnumHand hand) {
		if (entity.world.isRemote) return false;
		if (entity instanceof IShearable) {
			IShearable target = (IShearable) entity;
			BlockPos pos = new BlockPos(entity.posX, entity.posY, entity.posZ);
			if (target.isShearable(stack, entity.world, pos)) {
				List<ItemStack> drops = target.onSheared(stack, entity.world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack));
				Random rand = ThreadLocalRandom.current();
				for (ItemStack drop : drops) {
					EntityItem ent = entity.entityDropItem(drop, 1);
					ent.motionY += rand.nextFloat() * 0.05f;
					ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1f;
					ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1f;
				}
				takeDamage(playerIn, stack);
			}
			return true;
		}
		return super.itemInteractionForEntity(stack, playerIn, entity, hand);
	}

	@Override
	public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {
		if (player.world.isRemote) return false;
		Block block = player.world.getBlockState(pos).getBlock();
		if (EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, itemstack) > 0 && block instanceof IShearable) {
			IShearable target = (IShearable) block;
			if (target.isShearable(itemstack, player.world, pos)) {
				List<ItemStack> drops = target.onSheared(itemstack, player.world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, itemstack));
				Random rand = ThreadLocalRandom.current();

				for (ItemStack stack : drops) {
					float spread = 0.7f;
					double x = (rand.nextFloat() * spread) + (1f - spread) * 0.5;
					double y = (rand.nextFloat() * spread) + (1f - spread) * 0.5;
					double z = (rand.nextFloat() * spread) + (1f - spread) * 0.5;
					EntityItem ei = new EntityItem(player.world, pos.getX() + x, pos.getY() + y, pos.getZ() + z, stack);
					ei.setDefaultPickupDelay();
					player.world.spawnEntity(ei);
				}

				takeDamage(player, itemstack);
				player.addStat(StatList.getBlockStats(block));
				player.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
				return true;
			}
		}
		return super.onBlockStartBreak(itemstack, pos, player);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (player.isSneaking()) {
			player.openGui(TheBappyStick.inst, 0, worldIn, 0, 0, 0);
			return EnumActionResult.FAIL;
		}
		ItemStack stack = player.getHeldItem(hand);
		for (ItemStack delegate : getDelegateItems(stack)) {
			try {
				setHeldItemSilently(player, hand, delegate);
				int lastDmg = delegate.getItemDamage();
				EnumActionResult res = delegate.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
				int dmgDelta = delegate.getItemDamage()-lastDmg;
				for (int i = 0; i < dmgDelta; i++) {
					takeDamage(player, stack);
				}
				if (res != EnumActionResult.PASS) {
					return res;
				}
			} finally {
				setHeldItemSilently(player, hand, stack);
			}
		}
		if (TheBappyStick.inst.allowTorchPlacement) {
			for (int i = 5; i >= 0; i--) {
				ItemStack torch = getTorch(stack, i);
				setHeldItemSilently(player, hand, torch);
				try {
					EnumActionResult res = torch.getItem().onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
					setTorch(stack, i, player.getHeldItem(hand));
					if (res == EnumActionResult.SUCCESS) {
						return EnumActionResult.SUCCESS;
					}
				} finally {
					setHeldItemSilently(player, hand, stack);
				}
			}
		}
		return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}

	private void setHeldItemSilently(EntityPlayer player, EnumHand hand, ItemStack stack) {
		if (hand == EnumHand.OFF_HAND) {
			player.inventory.offHandInventory.set(0, stack);
		} else {
			player.inventory.mainInventory.set(player.inventory.currentItem, stack);
		}
	}

	@Override
	public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
		for (ItemStack is : getDelegateItems(stack)) {
			if (is.getItem().canHarvestBlock(state, is)) return true;
		}
		return false;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World worldIn, IBlockState state, BlockPos pos, EntityLivingBase entityLiving) {
		if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos) != 0) {
			takeDamage(entityLiving, stack);
		}
		return true;
	}

	@SubscribeEvent
	public void onHarvestDrops(HarvestDropsEvent e) {
		if (!TheBappyStick.inst.allowAutosmelt || e.getHarvester() == null) return;
		ItemStack stack = e.getHarvester().getHeldItemMainhand();
		if (stack.getItem() == this) {
			if (Math.random() < e.getDropChance()) {
				e.setDropChance(1);
				List<ItemStack> newDrops = Lists.newArrayList();
				int amountSmelted = 0;
				for (ItemStack is : e.getDrops()) {
					ItemStack res = FurnaceRecipes.instance().getSmeltingResult(is);
					if (!res.isEmpty() && consumeFuel(e.getHarvester(), stack, 200)) {
						amountSmelted++;
						is.shrink(1);
						ItemStack smelted = res.copy();
						boolean fortune = TheBappyStick.inst.autosmeltFortuneAll;
						if (!fortune) {
							outer: for (int id : OreDictionary.getOreIDs(smelted)) {
								String s = OreDictionary.getOreName(id);
								for (String prefix : TheBappyStick.inst.autosmeltFortuneOrePrefixes) {
									if (s.startsWith(prefix)) {
										fortune = true;
										break outer;
									}
								}
							}
						}
						int amtPerSmelt = smelted.getCount();
						int multiplier = 1;
						if (fortune) {
							multiplier = (Math.max(0, ThreadLocalRandom.current().nextInt(e.getFortuneLevel() + 2) - 1))+1;
						}
						smelted.setCount(amtPerSmelt * multiplier);
						while (!is.isEmpty() && consumeFuel(e.getHarvester(), stack, 200)) {
							amountSmelted++;
							is.shrink(1);
							multiplier = 1;
							if (fortune) {
								multiplier = (Math.max(0, ThreadLocalRandom.current().nextInt(e.getFortuneLevel() + 2) - 1))+1;
							}
							smelted.grow(amtPerSmelt * multiplier);
						}
						newDrops.add(smelted);
						newDrops.add(is);
					} else {
						newDrops.add(is);
					}
				}
				if (e.getHarvester().world instanceof WorldServer) {
					((WorldServer)e.getHarvester().world).spawnParticle(
							EnumParticleTypes.FLAME,
							e.getPos().getX()+0.5, e.getPos().getY()+0.5, e.getPos().getZ()+0.5,
							amountSmelted-1,
							0.25, 0.25, 0.25,
							0.1);
				}
				e.getDrops().clear();
				e.getDrops().addAll(newDrops);
			}
		}
	}

	public boolean consumeFuel(Entity e, ItemStack stack, int amount) {
		int fuelRemaining = getFuelRemaining(stack);
		boolean consumed = false;
		while (fuelRemaining < amount) {
			ItemStack input = getFuelInput(stack);
			if (!input.isEmpty()) {
				int time = TileEntityFurnace.getItemBurnTime(input);
				if (time == 0) return false;
				time *= TheBappyStick.inst.fuelRate;
				fuelRemaining += time;
				ItemStack copy = input.copy();
				copy.shrink(1);
				if (copy.isEmpty()) {
					ItemStack container = input.getItem().getContainerItem(input);
					System.out.println(container);
					setFuelInput(stack, container);
				} else {
					setFuelInput(stack, copy);
				}
				setFuelRemaining(stack, fuelRemaining);
				setMaxFuelRemaining(stack, fuelRemaining);
				consumed = true;
			} else {
				return false;
			}
		}
		if (consumed) {
			e.world.playSound(null, e.posX, e.posY, e.posZ, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
			Vec3d look = e.getLookVec().scale(0.4);
			if (e.world instanceof WorldServer) {
				((WorldServer) e.world).spawnParticle(
						EnumParticleTypes.FLAME,
						e.posX+look.x, e.posY+e.getEyeHeight()+look.y-0.25, e.posZ+look.z,
						10,
						0.1, 0.1, 0.1,
						0.1);
			}
		}
		fuelRemaining -= amount;
		setFuelRemaining(stack, fuelRemaining);
		return true;
	}

	public boolean takeDamage(Entity e, ItemStack stack) {
		if (getCurrentResource(stack).isEmpty()) return false;
		if (hasKey(stack, "HitsRemaining", NBT.TAG_ANY_NUMERIC)) {
			int hitsRemaining = stack.getTagCompound().getInteger("HitsRemaining");
			int lvl = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
			if (EnchantmentDurability.negateDamage(stack, lvl, ThreadLocalRandom.current())) {
				return true;
			}
			if (hitsRemaining > 0) {
				hitsRemaining--;
				stack.getTagCompound().setInteger("HitsRemaining", hitsRemaining);
				return true;
			} else {
				int overuseBreakChance = stack.getTagCompound().getInteger("OveruseBreakChance");
				if (overuseBreakChance == 0 || ThreadLocalRandom.current().nextInt(overuseBreakChance) == 1) {
					consumeResource(e, stack);
				}
				return true;
			}
		}
		return false;
	}

	public void consumeResource(Entity e, ItemStack stack) {
		ItemStack current = getCurrentResource(stack);
		if (!current.isEmpty()) {
			e.world.playSound(null, e.posX, e.posY, e.posZ, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5f, 0.9f);
			Vec3d look = e.getLookVec().scale(0.4);
			if (e.world instanceof WorldServer) {
				((WorldServer) e.world).spawnParticle(
						EnumParticleTypes.ITEM_CRACK,
						e.posX+look.x, e.posY+e.getEyeHeight()+look.y-0.25, e.posZ+look.z,
						10,
						0.1, 0.1, 0.1,
						0.25,
						Item.getIdFromItem(current.getItem()), current.getMetadata());
			}
		}
		ItemStack inputResources = getInputResources(stack);
		if (inputResources.getCount() >= 1) {
			double factor = 0;
			BappyResource resource = null;
			for (BappyResource bp : TheBappyStick.inst.resources) {
				for (ToDoubleFunction<ItemStack> func : bp.validItems) {
					factor = Math.max(factor, func.applyAsDouble(inputResources));
				}
				if (factor > 0) {
					resource = bp;
					break;
				}
			}
			if (resource != null) {
				long durabilityAccum = 0;
				for (ItemStack delegate : resource.delegateItems) {
					durabilityAccum += delegate.getMaxDamage();
				}
				int durability = (int)(durabilityAccum / resource.delegateItems.size());
				durability *= factor;
				durability *= TheBappyStick.inst.durabilityRate;
				int overuseBreakChance = (int)(durability * TheBappyStick.inst.overuseRate);
				durability -= overuseBreakChance;
				ItemStack currentResource = inputResources.splitStack(1);
				setMaxHitsRemaining(stack, durability);
				setHitsRemaining(stack, durability);
				setOveruseBreakChance(stack, overuseBreakChance);
				setDelegateItems(stack, resource.delegateItems);
				setInputResources(stack, inputResources);
				setCurrentResource(stack, currentResource);
			}
		} else {
			setCurrentResource(stack, ItemStack.EMPTY);
			setMaxHitsRemaining(stack, 0);
			setHitsRemaining(stack, 0);
			setOveruseBreakChance(stack, 0);
			setDelegateItems(stack, null);
			setInputResources(stack, ItemStack.EMPTY);
			setCurrentResource(stack, ItemStack.EMPTY);
		}
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return oldStack.getItem() != newStack.getItem() || slotChanged;
	}

}
