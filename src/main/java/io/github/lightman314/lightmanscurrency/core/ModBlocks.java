package io.github.lightman314.lightmanscurrency.core;

import java.util.function.BiFunction;

import javax.annotation.Nullable;

import com.google.common.base.Supplier;

import io.github.lightman314.lightmanscurrency.blocks.*;
import io.github.lightman314.lightmanscurrency.blocks.tradeinterface.ItemTraderInterfaceBlock;
import io.github.lightman314.lightmanscurrency.blocks.traderblocks.*;
import io.github.lightman314.lightmanscurrency.core.groups.RegistryObjectBundle;
import io.github.lightman314.lightmanscurrency.items.CashRegisterItem;
import io.github.lightman314.lightmanscurrency.items.CoinBlockItem;
import io.github.lightman314.lightmanscurrency.items.CoinJarItem;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.Reference;
import io.github.lightman314.lightmanscurrency.Reference.Color;
import io.github.lightman314.lightmanscurrency.Reference.WoodType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

	/**
	 * Placeholder function to force the static class loading
	 */
	public static void init() { }
	
	private static BiFunction<Block,CreativeModeTab,Item> getDefaultGenerator() {
		return (block, tab) -> {
			Item.Properties properties = new Item.Properties();
			if(tab != null)
				properties.tab(tab);
			return new BlockItem(block, properties);
		};
	}
	private static BiFunction<Block,CreativeModeTab,Item> getCoinGenerator(boolean fireResistant) {
		return (block, tab) -> {
			Item.Properties properties = new Item.Properties();
			if(tab != null)
				properties.tab(tab);
			if(fireResistant)
				properties.fireResistant();
			return new CoinBlockItem(block, properties);
		};
	}
	private static BiFunction<Block,CreativeModeTab,Item> getCoinJarGenerator() {
		return (block, tab) -> {
			Item.Properties properties = new Item.Properties();
			if(tab != null)
				properties.tab(tab);
			return new CoinJarItem(block, properties);
		};
	}
	
	static {
		//Coin Piles
		COINPILE_COPPER = register("coinpile_copper", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinpileBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_COPPER::get
				)
		);
		COINPILE_IRON = register("coinpile_iron", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinpileBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_IRON::get
				)
		);
		COINPILE_GOLD = register("coinpile_gold", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinpileBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_GOLD::get
				)
		);
		COINPILE_EMERALD = register("coinpile_emerald", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinpileBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_EMERALD::get
				)
		);
		COINPILE_DIAMOND = register("coinpile_diamond", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinpileBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_DIAMOND::get
				)
		);
		COINPILE_NETHERITE = register("coinpile_netherite", LightmansCurrency.COIN_GROUP, getCoinGenerator(true), () -> new CoinpileBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_NETHERITE::get
				)
		);
		
		//Coin Blocks
		COINBLOCK_COPPER = register("coinblock_copper", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_COPPER::get
				)
		);
		COINBLOCK_IRON = register("coinblock_iron", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_IRON::get
				)
		);
		COINBLOCK_GOLD = register("coinblock_gold", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_GOLD::get
				)
		);
		COINBLOCK_EMERALD = register("coinblock_emerald", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_EMERALD::get
				)
		);
		COINBLOCK_DIAMOND = register("coinblock_diamond", LightmansCurrency.COIN_GROUP, getCoinGenerator(false), () -> new CoinBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_DIAMOND::get
				)
		);
		COINBLOCK_NETHERITE = register("coinblock_netherite", LightmansCurrency.COIN_GROUP, getCoinGenerator(true), () -> new CoinBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL),
				ModItems.COIN_NETHERITE::get
				)
		);
		
		//Machines
		MACHINE_ATM = register("atm", LightmansCurrency.MACHINE_GROUP, () -> new ATMBlock(
				Block.Properties.of(Material.METAL)
				.strength(3.0f, 6.0f)
				.sound(SoundType.METAL)
				)
		);
		MACHINE_MINT = register("coinmint", LightmansCurrency.MACHINE_GROUP, () -> new CoinMintBlock(
			Block.Properties.of(Material.GLASS)
				.strength(2.0f, Float.POSITIVE_INFINITY)
				.sound(SoundType.GLASS)
			)
		);
		CASH_REGISTER = register("cash_register", LightmansCurrency.MACHINE_GROUP, (block, tab) -> {
			Item.Properties properties = new Item.Properties();
			if(tab != null)
				properties.tab(tab);
			return new CashRegisterItem(block, properties);
		},
				() -> new CashRegisterBlock(
				Block.Properties.of(Material.METAL)
					.strength(3.0f, 6.0f)
					.sound(SoundType.METAL),
					Block.box(1d,0d,1d,15d,10d,15d)
				)
		);
		
		//Item Traders
		//Display Case
		DISPLAY_CASE = register("display_case", LightmansCurrency.TRADING_GROUP, () -> new DisplayCaseBlock(
			Block.Properties.of(Material.GLASS)
				.strength(2.0f, Float.POSITIVE_INFINITY)
				.sound(SoundType.GLASS)
			)
		);
		
		//Vending Machine
		VENDING_MACHINE = registerColored("vending_machine", LightmansCurrency.TRADING_GROUP, () -> new VendingMachineBlock(
			Block.Properties.of(Material.METAL)
				.strength(5.0f, Float.POSITIVE_INFINITY)
				.sound(SoundType.METAL)
			),
			Color.WHITE
		);
		
		//Large Vending Machine
		VENDING_MACHINE_LARGE = registerColored("vending_machine_large", LightmansCurrency.TRADING_GROUP, () -> new VendingMachineLargeBlock(
			Block.Properties.of(Material.METAL)
				.strength(5.0f, Float.POSITIVE_INFINITY)
				.sound(SoundType.METAL)
			),
			Color.WHITE
		);
		
		//Shelves
		SHELF = registerWooden("shelf", LightmansCurrency.TRADING_GROUP, () -> new ShelfBlock(
				Block.Properties.of(Material.WOOD)
					.strength(2.0f, Float.POSITIVE_INFINITY)
				)
		);
		
		//Card Display
		CARD_DISPLAY = registerWooden("card_display", LightmansCurrency.TRADING_GROUP, () -> new CardDisplayBlock(
				Block.Properties.of(Material.WOOD)
					.strength(2.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.WOOD)
				)
		);
		
		//Freezer
		FREEZER = register("freezer", LightmansCurrency.TRADING_GROUP, () -> new FreezerBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL)
				)
		);
		
		//Armor Display
		ARMOR_DISPLAY = register("armor_display", LightmansCurrency.TRADING_GROUP, () -> new ArmorDisplayBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL)
				)
		);
		
		//Ticket Kiosk
		TICKET_KIOSK = register("ticket_kiosk",LightmansCurrency.TRADING_GROUP, () -> new TicketKioskBlock(
				Block.Properties.of(Material.METAL)
					.strength(3.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL)
				)
		);
		
		
		
		//Network Traders
		ITEM_TRADER_SERVER_SMALL = register("item_trader_server_sml", LightmansCurrency.TRADING_GROUP, () -> new ItemTraderServerBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL),
					ItemTraderServerBlock.SMALL_SERVER_COUNT
				)
		);
		ITEM_TRADER_SERVER_MEDIUM = register("item_trader_server_med", LightmansCurrency.TRADING_GROUP, () -> new ItemTraderServerBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL),
					ItemTraderServerBlock.MEDIUM_SERVER_COUNT
				)
		);
		ITEM_TRADER_SERVER_LARGE = register("item_trader_server_lrg", LightmansCurrency.TRADING_GROUP, () -> new ItemTraderServerBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL),
					ItemTraderServerBlock.LARGE_SERVER_COUNT
				)
		);
		ITEM_TRADER_SERVER_EXTRA_LARGE = register("item_trader_server_xlrg", LightmansCurrency.TRADING_GROUP, () -> new ItemTraderServerBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL),
					ItemTraderServerBlock.EXTRA_LARGE_SERVER_COUNT
				)
		);
		
		//Trader Interface
		ITEM_TRADER_INTERFACE = register("item_trader_interface", LightmansCurrency.MACHINE_GROUP, () -> new ItemTraderInterfaceBlock(
				Block.Properties.of(Material.METAL)
					.strength(5.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL)
				)
		);
		
		//Terminal
		TERMINAL = register("terminal", LightmansCurrency.MACHINE_GROUP, () -> new TerminalBlock(
				Block.Properties.of(Material.METAL)
					.strength(3.0f, 6.0f)
					.sound(SoundType.METAL),
					Block.box(1d,0d,1d,15d,15d,15d)
				)
		);
		
		//Paygate
		PAYGATE = register("paygate", LightmansCurrency.MACHINE_GROUP, () -> new PaygateBlock(
				Block.Properties.of(Material.METAL)
					.strength(3.0f, Float.POSITIVE_INFINITY)
					.sound(SoundType.METAL)
				)
		);
		
		//Ticket Machine
		TICKET_MACHINE = register("ticket_machine", LightmansCurrency.MACHINE_GROUP, () -> new TicketMachineBlock(
				Block.Properties.of(Material.METAL)
					.strength(3.0f, 6.0f)
					.sound(SoundType.METAL)
				)
		);
		
		//Coin Jars
		PIGGY_BANK = register("piggy_bank", CreativeModeTab.TAB_DECORATIONS, getCoinJarGenerator(), () -> new CoinJarBlock(
				Block.Properties.of(Material.STONE)
					.strength(0.1f, 2.0f)
					.sound(SoundType.STONE),
					Block.box(4d, 0d, 4d, 12d, 8d, 12d)
				)
		);
		COINJAR_BLUE = register("coinjar_blue", CreativeModeTab.TAB_DECORATIONS, getCoinJarGenerator(), () -> new CoinJarBlock(
				Block.Properties.of(Material.STONE)
				.strength(0.1f, 2.0f)
				.sound(SoundType.STONE),
				Block.box(4d, 0d, 4d, 12d, 8d, 12d)
				)
		);
		
	}
	
	//Coin piles
	public static final RegistryObject<Block> COINPILE_COPPER;
	public static final RegistryObject<Block> COINPILE_IRON;
	public static final RegistryObject<Block> COINPILE_GOLD;
	public static final RegistryObject<Block> COINPILE_DIAMOND;
	public static final RegistryObject<Block> COINPILE_EMERALD;
	public static final RegistryObject<Block> COINPILE_NETHERITE;
	
	//Coin blocks
	public static final RegistryObject<Block> COINBLOCK_COPPER;
	public static final RegistryObject<Block> COINBLOCK_IRON;
	public static final RegistryObject<Block> COINBLOCK_GOLD;
	public static final RegistryObject<Block> COINBLOCK_EMERALD;
	public static final RegistryObject<Block> COINBLOCK_DIAMOND;
	public static final RegistryObject<Block> COINBLOCK_NETHERITE;
	
	//Machines
	//Misc Machines
	public static final RegistryObject<Block> MACHINE_ATM;
	public static final RegistryObject<Block> MACHINE_MINT;
	
	//Display Case
	public static final RegistryObject<Block> DISPLAY_CASE;
	
	//Vending Machines
	public static final RegistryObjectBundle<Block,Color> VENDING_MACHINE;
	
	//Large Vending Machines
	public static final RegistryObjectBundle<Block,Color> VENDING_MACHINE_LARGE;
	
	//Wooden Shelves
	public static final RegistryObjectBundle<Block,WoodType> SHELF;
	
	//Card Shelves
	public static final RegistryObjectBundle<Block,WoodType> CARD_DISPLAY;
	
	//Armor Display
	public static final RegistryObject<Block> ARMOR_DISPLAY;
	
	//Freezer
	public static final RegistryObject<Block> FREEZER;
	
	
	//Network Traders
	public static final RegistryObject<Block> ITEM_TRADER_SERVER_SMALL;
	public static final RegistryObject<Block> ITEM_TRADER_SERVER_MEDIUM;
	public static final RegistryObject<Block> ITEM_TRADER_SERVER_LARGE;
	public static final RegistryObject<Block> ITEM_TRADER_SERVER_EXTRA_LARGE;
	
	//Trader Interface
	public static final RegistryObject<Block> ITEM_TRADER_INTERFACE;
	
	//Cash Register
	public static final RegistryObject<Block> CASH_REGISTER;
	
	//Terminal
	public static final RegistryObject<Block> TERMINAL;
	
	//Paygate
	public static final RegistryObject<Block> PAYGATE;
	
	//Ticket Kiosk
	public static final RegistryObject<Block> TICKET_KIOSK;
	
	//Ticket Machine
	public static final RegistryObject<Block> TICKET_MACHINE;
	
	//Coin Jars
	public static final RegistryObject<Block> PIGGY_BANK;
	public static final RegistryObject<Block> COINJAR_BLUE;
	
	
	/**
	* Block Registration Code
	*/
	private static RegistryObject<Block> register(String name, CreativeModeTab itemGroup, Supplier<Block> sup)
	{
		return register(name, itemGroup, getDefaultGenerator(), sup);
	}
	
	private static RegistryObject<Block> register(String name, CreativeModeTab itemGroup, BiFunction<Block,CreativeModeTab,Item> itemGenerator, Supplier<Block> sup)
	{
		RegistryObject<Block> block = ModRegistries.BLOCKS.register(name, sup);
		if(block != null)
			ModRegistries.ITEMS.register(name, () -> itemGenerator.apply(block.get(), itemGroup));
		return block;
	}
	
	/**
	 * Colored block registration code
	 */
	private static RegistryObjectBundle<Block,Color> registerColored(String name, CreativeModeTab itemGroup, Supplier<Block> block, @Nullable Color dontNameThisColor)
	{
		return registerColored(name, itemGroup, getDefaultGenerator(), block, dontNameThisColor);
	}
	
	private static RegistryObjectBundle<Block,Color> registerColored(String name, CreativeModeTab itemGroup, BiFunction<Block,CreativeModeTab,Item> itemGenerator, Supplier<Block> block, @Nullable Color dontNameThisColor)
	{
		RegistryObjectBundle<Block,Color> bundle = new RegistryObjectBundle<>();
		for(Color color : Reference.Color.values())
		{
			String thisName = name;
			if(color != dontNameThisColor) //Add the color name to the end unless this is the color flagged to not be named
				thisName += "_" + color.toString().toLowerCase();
			//Register the block normally
			bundle.put(color, register(thisName, itemGroup, itemGenerator, block));
		}
		return bundle.lock();
	}
	
	/**
	 * Wooden block registration code
	 */
	private static RegistryObjectBundle<Block,WoodType> registerWooden(String name, CreativeModeTab itemGroup, Supplier<Block> block)
	{
		return registerWooden(name, itemGroup, getDefaultGenerator(), block);
	}
	
	private static RegistryObjectBundle<Block,WoodType> registerWooden(String name, CreativeModeTab itemGroup, BiFunction<Block,CreativeModeTab,Item> itemGenerator, Supplier<Block> block)
	{
		RegistryObjectBundle<Block,WoodType> bundle = new RegistryObjectBundle<>();
		for(WoodType woodType : WoodType.values())
		{
			String thisName = name + "_" + woodType.toString().toLowerCase();
			//Register the block normally
			bundle.put(woodType, register(thisName, itemGroup, itemGenerator, block));
		}
		return bundle.lock();
	}
	
}
