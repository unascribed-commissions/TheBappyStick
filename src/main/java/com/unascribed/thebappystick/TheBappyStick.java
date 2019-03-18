package com.unascribed.thebappystick;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.primitives.Doubles;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@Mod(modid="thebappystick", name="The Bappy Stick", version="@VERSION@")
public class TheBappyStick {

	public static final Logger log = LogManager.getLogger("TheBappyStick");

	@Instance
	public static TheBappyStick inst;

	@SidedProxy(clientSide="com.unascribed.thebappystick.ClientProxy", serverSide="com.unascribed.thebappystick.Proxy")
	public static Proxy proxy;

	public static ItemBappyStick THEBAPPYSTICK;
	public static ItemTalisman TALISMAN;
	public static Item LAPIS_NUGGET;
	public static Item RENDER_DUMMY;

	private Configuration cfg;

	public boolean allowAutosmelt;
	public boolean allowTorchPlacement;
	public List<Predicate<ItemStack>> torches;
	public boolean inheritToolEnchants;
	public boolean inheritWeaponEnchants;
	public boolean inheritDurabilityEnchants;
	public Set<Enchantment> whitelistedEnchants;
	public Set<Enchantment> blacklistedEnchants;
	public boolean autosmeltFortuneAll;
	public List<String> autosmeltFortuneOrePrefixes;
	public boolean showEnchantGlint;

	public boolean freeTorches;
	public double fuelRate;
	public double durabilityRate;
	public double overuseRate;
	public double attackSpeed;
	public double attackDamage;
	public double miningSpeed;

	public boolean heavy;

	public List<BappyResource> resources;

	public SimpleNetworkWrapper network;


	@EventHandler
	public void onPreInit(FMLPreInitializationEvent e) {
		File cfgFile = e.getSuggestedConfigurationFile();
		if (!cfgFile.exists()) {
			try {
				Resources.asByteSource(getClass().getResource("default.cfg")).copyTo(Files.asByteSink(cfgFile));
			} catch (IOException e1) {
				log.warn("Failed to copy default config", e1);
			}
		}
		this.cfg = new Configuration(cfgFile);
		cfg.load();

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new BappyGuiHandler());

		network = NetworkRegistry.INSTANCE.newSimpleChannel("thebappystick");
		network.registerMessage(RenameBappyStickHandler.class, RenameBappyStickMessage.class, 0, Side.SERVER);

		proxy.onPreInit();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@EventHandler
	public void onPostInit(FMLPostInitializationEvent e) {
		// comments are irrelevant as the file is never saved, and the
		// initial version is copied from a well-documented default
		allowAutosmelt = cfg.getBoolean("allow_autosmelt", "general", true, "");
		allowTorchPlacement = cfg.getBoolean("allow_torch_placement", "general", true, "");
		torches = Lists.newArrayList();
		for (String s : cfg.getStringList("torches", "general", new String[] {"minecraft:torch"}, "")) {
			if (s.startsWith("ore:")) {
				int oreId = OreDictionary.getOreID(s.substring(4));
				torches.add((is) -> {
					for (int i : OreDictionary.getOreIDs(is)) {
						if (i == oreId) return true;
					}
					return false;
				});
			} else {
				Item item = Item.getByNameOrId(s);
				if (item == null) {
					log.warn("Item with id {} does not exist, ignoring", s);
				} else {
					torches.add((is) -> {
						return is.getItem() == item;
					});
				}
			}
		}
		inheritToolEnchants = cfg.getBoolean("inherit_tool_enchants", "general", true, "");
		inheritWeaponEnchants = cfg.getBoolean("inherit_weapon_enchants", "general", true, "");
		inheritDurabilityEnchants = cfg.getBoolean("inherit_durability_enchants", "general", true, "");
		whitelistedEnchants = Sets.newHashSet();
		for (String s : cfg.getStringList("talisman_enchant_whitelist", "general", new String[] {}, "")) {
			whitelistedEnchants.add(Enchantment.getEnchantmentByLocation(s));
		}
		blacklistedEnchants = Sets.newHashSet();
		for (String s : cfg.getStringList("talisman_enchant_blacklist", "general", new String[] {}, "")) {
			blacklistedEnchants.add(Enchantment.getEnchantmentByLocation(s));
		}
		autosmeltFortuneAll = cfg.getBoolean("autosmelt_fortune_all", "general", false, "");
		autosmeltFortuneOrePrefixes = Lists.newArrayList();
		for (String s : cfg.getStringList("autosmelt_fortune_ore_prefixes", "general", new String[] {"ingot","gem"}, "")) {
			autosmeltFortuneOrePrefixes.add(s);
		}
		showEnchantGlint = cfg.getBoolean("show_enchant_glint", "general", true, "");

		freeTorches = cfg.getBoolean("free_torches", "balance", false, "");
		fuelRate = cfg.getFloat("fuel_rate", "balance", 50, 0, 65536, "")/100D;
		durabilityRate = cfg.getFloat("durability_rate", "balance", 50, 0, 65536, "")/100D;
		overuseRate = cfg.getFloat("overuse_rate", "balance", 50, 0, 65536, "")/100D;
		attackSpeed = cfg.getFloat("attack_speed", "balance", 80, 0, 65536, "")/100D;
		attackDamage = cfg.getFloat("attack_damage", "balance", 100, 0, 65536, "")/100D;
		miningSpeed = cfg.getFloat("mining_speed", "balance", 100, 0, 65536, "")/100D;
		heavy = cfg.getBoolean("heavy", "silly", false, "");
		resources = Lists.newArrayList();
		outer: for (Map.Entry<String, Property> en : cfg.getCategory("resources").entrySet()) {
			if (en.getKey().endsWith("_resources")) {
				String name = en.getKey().substring(0, en.getKey().length()-10);
				Property delegates = cfg.getCategory("resources").get(name+"_delegates");
				if (delegates == null) {
					log.error("Failed to load resource \"{}\" - there was a resources definition but no delegates", name);
				} else {
					List<ToDoubleFunction<ItemStack>> validItems = Lists.newArrayList();
					for (String s : en.getValue().getStringList()) {
						if (!s.contains(" ")) s = s+" 100%";
						int spaceIdx = s.indexOf(' ');
						String itemStr = s.substring(0, spaceIdx);
						String pctStr = s.substring(spaceIdx+1);
						if (!pctStr.endsWith("%")) {
							log.error("Failed to load resource \"{}\" - resource definition is invalid (percentage does not end in percent sign): {}", name, s);
							continue outer;
						}
						Double pct = Doubles.tryParse(pctStr.substring(0, pctStr.length()-1));
						if (pct == null) {
							log.error("Failed to load resource \"{}\" - resource definition is invalid (percentage is not a valid number): {}", name, s);
							continue outer;
						}
						double factor = Math.max(0, pct)/100D;
						if (itemStr.startsWith("ore:")) {
							int oreId = OreDictionary.getOreID(itemStr.substring(4));
							validItems.add((is) -> {
								for (int i : OreDictionary.getOreIDs(is)) {
									if (i == oreId) return factor;
								}
								return 0;
							});
						} else {
							Item item = Item.getByNameOrId(itemStr);
							if (item == null) {
								log.warn("Item with id {} does not exist, ignoring", itemStr);
							} else {
								validItems.add((is) -> {
									return is.getItem() == item ? factor : 0;
								});
							}
						}
					}
					List<ItemStack> delegateItems = Lists.newArrayList();
					for (String s : delegates.getStringList()) {
						Item item = Item.getByNameOrId(s);
						if (item == null) {
							log.warn("Item with id {} does not exist, ignoring", s);
						} else {
							delegateItems.add(new ItemStack(item));
						}
					}
					resources.add(new BappyResource(name, validItems, delegateItems));
				}
			} else if (en.getKey().endsWith("_delegates")) {
				String name = en.getKey().substring(0, en.getKey().length()-10);
				Property resources = cfg.getCategory("resources").get(name+"_resources");
				if (resources == null) {
					log.error("Failed to load resource \"{}\" - there was a delegates definition but no resources", name);
				}
			} else {
				log.error("Unrecognized key \"{}\" in resources section", en.getKey());
			}
		}
	}

	@SubscribeEvent
	public void onRegisterItems(RegistryEvent.Register<Item> e) {
		e.getRegistry().register((THEBAPPYSTICK = new ItemBappyStick())
				.setTranslationKey("thebappystick").setRegistryName("thebappystick")
				.setCreativeTab(CreativeTabs.TOOLS));
		e.getRegistry().register((TALISMAN = new ItemTalisman())
				.setTranslationKey("thebappystick.talisman").setRegistryName("talisman")
				.setCreativeTab(CreativeTabs.TOOLS));
		e.getRegistry().register((LAPIS_NUGGET = new Item())
				.setTranslationKey("thebappystick.lapis_nugget").setRegistryName("lapis_nugget")
				.setCreativeTab(CreativeTabs.MATERIALS));
		e.getRegistry().register((RENDER_DUMMY = new Item() {
			@Override public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {}
		}).setTranslationKey("thebappystick.talisman").setRegistryName("render_dummy"));

		OreDictionary.registerOre("nuggetLapis", LAPIS_NUGGET);
	}

	@SubscribeEvent
	public void onRegisterRecipes(RegistryEvent.Register<IRecipe> e) {
		e.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(LAPIS_NUGGET, 9),
				"gemLapis").setRegistryName("lapis_to_nuggets"));
		e.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(Items.DYE, 1, 4),
				"nuggetLapis", "nuggetLapis", "nuggetLapis",
				"nuggetLapis", "nuggetLapis", "nuggetLapis",
				"nuggetLapis", "nuggetLapis", "nuggetLapis"
			).setRegistryName("nuggets_to_lapis"));
		e.getRegistry().register(new ShapedOreRecipe(null, THEBAPPYSTICK,
				"dad",
				" d ",
				" s ",

				'd', "gemDiamond",
				'a', Blocks.ANVIL,
				's', Items.IRON_SWORD
			).setRegistryName("thebappystick"));
		e.getRegistry().register(new ShapedOreRecipe(null, TALISMAN,
				"lnl",
				"nin",
				"lnl",

				'l', "nuggetLapis",
				'i', "ingotGold",
				'n', "nuggetGold"
			).setRegistryName("talisman"));
	}
}
