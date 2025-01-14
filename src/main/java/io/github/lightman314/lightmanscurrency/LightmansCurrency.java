package io.github.lightman314.lightmanscurrency;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.Reference.Color;
import io.github.lightman314.lightmanscurrency.Reference.WoodType;
import io.github.lightman314.lightmanscurrency.common.atm.ATMIconData;
import io.github.lightman314.lightmanscurrency.common.capability.ISpawnTracker;
import io.github.lightman314.lightmanscurrency.common.capability.IWalletHandler;
import io.github.lightman314.lightmanscurrency.common.capability.WalletCapability;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification.Category;
import io.github.lightman314.lightmanscurrency.common.notifications.categories.AuctionHouseCategory;
import io.github.lightman314.lightmanscurrency.common.notifications.categories.BankCategory;
import io.github.lightman314.lightmanscurrency.common.notifications.categories.TraderCategory;
import io.github.lightman314.lightmanscurrency.common.notifications.types.ItemTradeNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.LowBalanceNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.OutOfStockNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.PaygateNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseBidNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseBuyerNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseCancelNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseSellerNobidNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseSellerNotification;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.auction.AuctionHouseTrader;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalItemTraderData;
import io.github.lightman314.lightmanscurrency.common.universal_traders.traderSearching.ItemTraderSearchFilter;
import io.github.lightman314.lightmanscurrency.common.universal_traders.traderSearching.TraderSearchFilter;
import io.github.lightman314.lightmanscurrency.core.ModBlocks;
import io.github.lightman314.lightmanscurrency.core.ModItems;
import io.github.lightman314.lightmanscurrency.core.ModRegistries;
import io.github.lightman314.lightmanscurrency.discord.CurrencyMessages;
import io.github.lightman314.lightmanscurrency.discord.DiscordListenerRegistration;
import io.github.lightman314.lightmanscurrency.enchantments.LCEnchantmentCategories;
import io.github.lightman314.lightmanscurrency.gamerule.ModGameRules;
import io.github.lightman314.lightmanscurrency.integration.curios.LCCurios;
import io.github.lightman314.lightmanscurrency.items.WalletItem;
import io.github.lightman314.lightmanscurrency.loot.LootManager;
import io.github.lightman314.lightmanscurrency.menus.slots.WalletSlot;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.time.MessageSyncClientTime;
import io.github.lightman314.lightmanscurrency.proxy.*;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.*;
import io.github.lightman314.lightmanscurrency.upgrades.UpgradeType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import top.theillusivec4.curios.api.SlotTypeMessage;

@Mod("lightmanscurrency")
public class LightmansCurrency {
	
	public static final String MODID = "lightmanscurrency";
	
	public static final CommonProxy PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
	
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final CustomCreativeTab COIN_GROUP = new CustomCreativeTab(MODID + ".coins", () -> ModBlocks.COINPILE_GOLD.get());
    public static final CustomCreativeTab MACHINE_GROUP = new CustomCreativeTab(MODID + ".machines", () -> ModBlocks.MACHINE_ATM.get());
    public static final CustomCreativeTab UPGRADE_GROUP = new CustomCreativeTab(MODID + ".upgrades", () -> ModItems.ITEM_CAPACITY_UPGRADE_1.get());
    public static final CustomCreativeTab TRADING_GROUP = new CustomCreativeTab(MODID + ".trading", () -> ModBlocks.DISPLAY_CASE.get());
    
    private static boolean discordIntegrationLoaded = false;
    public static boolean isDiscordIntegrationLoaded() { return discordIntegrationLoaded; }
    
    private static boolean curiosLoaded = false;
    /**
     * Whether the Curios API mod is installed
     */
    public static boolean isCuriosLoaded() { return curiosLoaded; }
    /**
     * Whether the Curios API mod is installed, and a valid Wallet Slot is present on the given entity.
     */
    public static boolean isCuriosValid(LivingEntity player) {
    	try {
    		if(curiosLoaded)
        		return LCCurios.hasWalletSlot(player);
    	} catch(Throwable t) {}
    	return false;
    }
    
	public LightmansCurrency() {
    	
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doCommonStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCapabilities);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::imc);
        
        //Register configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        //Setup Deferred Registries
        ModRegistries.register(FMLJavaModLoadingContext.get().getModEventBus());
        
        //Register the proxy so that it can run custom events
        MinecraftForge.EVENT_BUS.register(PROXY);
        
        discordIntegrationLoaded = ModList.get().isLoaded("lightmansdiscord");
        curiosLoaded = ModList.get().isLoaded("curios");
        
        if(discordIntegrationLoaded)
        {
        	MinecraftForge.EVENT_BUS.register(DiscordListenerRegistration.class);
        	MinecraftForge.EVENT_BUS.register(CurrencyMessages.class);
        }
        
    }
	
	private void imc(InterModEnqueueEvent event) {
		if(isCuriosLoaded())
		{
			//Add a wallet slot
			InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder(LCCurios.WALLET_SLOT).icon(WalletSlot.EMPTY_WALLET_SLOT).size(1).build());
		}
	}
    
    private void doCommonStuff(final FMLCommonSetupEvent event)
    {
    	
    	//LOGGER.info("PacketHandler init");
    	LightmansCurrencyPacketHandler.init();
    	
    	//Initialize the UniversalTraderData deserializers
    	TradingOffice.RegisterDataType(UniversalItemTraderData.TYPE, UniversalItemTraderData::new);
    	TradingOffice.RegisterDataType(AuctionHouseTrader.TYPE, AuctionHouseTrader::new);
    	
    	//Register the custom game rules
    	ModGameRules.registerRules();
    	
    	//Initialize the Trade Rule deserializers
    	TradeRule.RegisterDeserializer(PlayerWhitelist.TYPE, PlayerWhitelist::new);
    	TradeRule.RegisterDeserializer(PlayerBlacklist.TYPE, PlayerBlacklist::new);
    	TradeRule.RegisterDeserializer(PlayerTradeLimit.TYPE, PlayerTradeLimit::new);
    	TradeRule.RegisterDeserializer(PlayerTradeLimit.OLD_TYPE, PlayerTradeLimit::new, true);
    	TradeRule.RegisterDeserializer(PlayerDiscounts.TYPE, PlayerDiscounts::new);
    	TradeRule.RegisterDeserializer(TimedSale.TYPE, TimedSale::new);
    	TradeRule.RegisterDeserializer(TradeLimit.TYPE, TradeLimit::new);
    	TradeRule.RegisterDeserializer(TradeLimit.OLD_TYPE, TradeLimit::new, true);
    	TradeRule.RegisterDeserializer(FreeSample.TYPE, FreeSample::new);
    	
    	//Initialize the Notification deserializers
    	Notification.register(ItemTradeNotification.TYPE, ItemTradeNotification::new);
    	Notification.register(PaygateNotification.TYPE, PaygateNotification::new);
    	Notification.register(OutOfStockNotification.TYPE, OutOfStockNotification::new);
    	Notification.register(LowBalanceNotification.TYPE, LowBalanceNotification::new);
    	Notification.register(AuctionHouseSellerNotification.TYPE, AuctionHouseSellerNotification::new);
    	Notification.register(AuctionHouseBuyerNotification.TYPE, AuctionHouseBuyerNotification::new);
    	Notification.register(AuctionHouseSellerNobidNotification.TYPE, AuctionHouseSellerNobidNotification::new);
    	Notification.register(AuctionHouseBidNotification.TYPE, AuctionHouseBidNotification::new);
    	Notification.register(AuctionHouseCancelNotification.TYPE, AuctionHouseCancelNotification::new);
    	
    	//Initialize the Notification Category deserializers
    	Category.register(Category.GENERAL_TYPE, compound -> Category.GENERAL);
    	Category.register(TraderCategory.TYPE, TraderCategory::new);
    	Category.register(BankCategory.TYPE, BankCategory::new);
    	Category.register(AuctionHouseCategory.TYPE, compound -> AuctionHouseCategory.INSTANCE);
    	
    	//Register Trader Search Filters
    	TraderSearchFilter.addFilter(new ItemTraderSearchFilter());
    	
    	//Register Upgrade Types
    	MinecraftForge.EVENT_BUS.post(new UpgradeType.RegisterUpgradeTypeEvent());
    	
    	//Initialized the sorting lists
    	COIN_GROUP.setEnchantmentCategories(LCEnchantmentCategories.WALLET_CATEGORY, LCEnchantmentCategories.WALLET_PICKUP_CATEGORY);
		COIN_GROUP.initSortingList2(Lists.newArrayList(ModItems.COIN_COPPER, ModItems.COIN_IRON, ModItems.COIN_GOLD,
				ModItems.COIN_EMERALD, ModItems.COIN_DIAMOND, ModItems.COIN_NETHERITE, ModBlocks.COINPILE_COPPER,
				ModBlocks.COINPILE_IRON, ModBlocks.COINPILE_GOLD, ModBlocks.COINPILE_EMERALD,
				ModBlocks.COINPILE_DIAMOND, ModBlocks.COINPILE_NETHERITE, ModBlocks.COINBLOCK_COPPER,
				ModBlocks.COINBLOCK_IRON, ModBlocks.COINBLOCK_GOLD, ModBlocks.COINBLOCK_EMERALD,
				ModBlocks.COINBLOCK_DIAMOND, ModBlocks.COINBLOCK_NETHERITE, ModItems.TRADING_CORE, ModItems.TICKET,
				ModItems.TICKET_MASTER, ModItems.TICKET_STUB, ModItems.WALLET_COPPER, ModItems.WALLET_IRON, ModItems.WALLET_GOLD,
				ModItems.WALLET_EMERALD, ModItems.WALLET_DIAMOND, ModItems.WALLET_NETHERITE
			));
		
		MACHINE_GROUP.initSortingList2(Lists.newArrayList(ModBlocks.MACHINE_ATM, ModItems.PORTABLE_ATM, ModBlocks.MACHINE_MINT, ModBlocks.CASH_REGISTER,
				ModBlocks.TERMINAL, ModItems.PORTABLE_TERMINAL, ModBlocks.ITEM_TRADER_INTERFACE, ModBlocks.PAYGATE, ModBlocks.TICKET_MACHINE
			));
		
		UPGRADE_GROUP.initSortingList2(Lists.newArrayList(ModItems.ITEM_CAPACITY_UPGRADE_1, ModItems.ITEM_CAPACITY_UPGRADE_2,
				ModItems.ITEM_CAPACITY_UPGRADE_3, ModItems.SPEED_UPGRADE_1, ModItems.SPEED_UPGRADE_2, ModItems.SPEED_UPGRADE_3,
				ModItems.SPEED_UPGRADE_4, ModItems.SPEED_UPGRADE_5
			));
		
		TRADING_GROUP.initSortingList(Lists.newArrayList(ModBlocks.SHELF.get(WoodType.OAK), ModBlocks.SHELF.get(WoodType.BIRCH),
				ModBlocks.SHELF.get(WoodType.SPRUCE), ModBlocks.SHELF.get(WoodType.JUNGLE), ModBlocks.SHELF.get(WoodType.ACACIA), 
				ModBlocks.SHELF.get(WoodType.DARK_OAK), ModBlocks.SHELF.get(WoodType.CRIMSON), ModBlocks.SHELF.get(WoodType.WARPED), 
				ModBlocks.DISPLAY_CASE.get(), ModBlocks.ARMOR_DISPLAY.get(), ModBlocks.CARD_DISPLAY.get(WoodType.OAK),
				ModBlocks.CARD_DISPLAY.get(WoodType.BIRCH), ModBlocks.CARD_DISPLAY.get(WoodType.SPRUCE), ModBlocks.CARD_DISPLAY.get(WoodType.JUNGLE),
				ModBlocks.CARD_DISPLAY.get(WoodType.ACACIA), ModBlocks.CARD_DISPLAY.get(WoodType.DARK_OAK), ModBlocks.CARD_DISPLAY.get(WoodType.CRIMSON),
				ModBlocks.CARD_DISPLAY.get(WoodType.WARPED), ModBlocks.VENDING_MACHINE.get(Color.WHITE), ModBlocks.VENDING_MACHINE.get(Color.ORANGE),
				ModBlocks.VENDING_MACHINE.get(Color.MAGENTA), ModBlocks.VENDING_MACHINE.get(Color.LIGHTBLUE),ModBlocks.VENDING_MACHINE.get(Color.YELLOW),
				ModBlocks.VENDING_MACHINE.get(Color.LIME), ModBlocks.VENDING_MACHINE.get(Color.PINK), ModBlocks.VENDING_MACHINE.get(Color.GRAY),
				ModBlocks.VENDING_MACHINE.get(Color.LIGHTGRAY), ModBlocks.VENDING_MACHINE.get(Color.CYAN), ModBlocks.VENDING_MACHINE.get(Color.PURPLE),
				ModBlocks.VENDING_MACHINE.get(Color.BLUE), ModBlocks.VENDING_MACHINE.get(Color.BROWN), ModBlocks.VENDING_MACHINE.get(Color.GREEN),
				ModBlocks.VENDING_MACHINE.get(Color.RED), ModBlocks.VENDING_MACHINE.get(Color.BLACK), ModBlocks.FREEZER.get(),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.WHITE), ModBlocks.VENDING_MACHINE_LARGE.get(Color.ORANGE),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.MAGENTA), ModBlocks.VENDING_MACHINE_LARGE.get(Color.LIGHTBLUE),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.YELLOW), ModBlocks.VENDING_MACHINE_LARGE.get(Color.LIME),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.PINK), ModBlocks.VENDING_MACHINE_LARGE.get(Color.GRAY),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.LIGHTGRAY), ModBlocks.VENDING_MACHINE_LARGE.get(Color.CYAN),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.PURPLE), ModBlocks.VENDING_MACHINE_LARGE.get(Color.BLUE),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.BROWN), ModBlocks.VENDING_MACHINE_LARGE.get(Color.GREEN),
				ModBlocks.VENDING_MACHINE_LARGE.get(Color.RED), ModBlocks.VENDING_MACHINE_LARGE.get(Color.BLACK),
				ModBlocks.TICKET_KIOSK.get(), ModBlocks.ITEM_TRADER_SERVER_SMALL.get(), ModBlocks.ITEM_TRADER_SERVER_MEDIUM.get(),
				ModBlocks.ITEM_TRADER_SERVER_LARGE.get(), ModBlocks.ITEM_TRADER_SERVER_EXTRA_LARGE.get()
			));
		
		ATMIconData.init();
		
    }
    
    private void doClientStuff(final FMLClientSetupEvent event) {
    	
        PROXY.setupClient();
        
    }
    
    private void onConfigLoad(ModConfigEvent event)
    {
    	if(event.getConfig().getModId().equals(MODID) && event.getConfig().getSpec() == Config.commonSpec)
    	{
    		//Have the loot manager validate the entity loot contents
    		LootManager.validateEntityDropList();
    		LootManager.debugLootConfigs();
    	}
    }
    
    private void registerCapabilities(RegisterCapabilitiesEvent event)
    {
    	event.register(IWalletHandler.class);
    	event.register(ISpawnTracker.class);
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
    	
    	//Preload target
    	PacketTarget target = LightmansCurrencyPacketHandler.getTarget(event.getEntity());
    	//Sync time
    	LightmansCurrencyPacketHandler.instance.send(target, new MessageSyncClientTime());
    	//Sync admin list
    	LightmansCurrencyPacketHandler.instance.send(target, TradingOffice.getAdminSyncMessage());
    	
    }
    
    /**
     * Easy public access to the equipped wallet.
     * Also confirms that the equipped wallet is either empty or a valid WalletItem.
     * Returns an empty stack if no wallet is equipped, or if the equipped item is not a valid wallet.
     */
    public static ItemStack getWalletStack(Player player)
    {
    	ItemStack wallet = ItemStack.EMPTY;
    	
    	IWalletHandler walletHandler = WalletCapability.getWalletHandler(player).orElse(null);
    	if(walletHandler != null)
    		wallet = walletHandler.getWallet();
    	//Safety check to confirm that the Item Stack found is a valid wallet
    	if(!WalletItem.validWalletStack(wallet))
    	{
    		LightmansCurrency.LogError(player.getName().getString() + "'s equipped wallet is not a valid WalletItem.");
    		LightmansCurrency.LogError("Equipped wallet is of type " + wallet.getItem().getClass().getName());
			return ItemStack.EMPTY;
    	}
    	return wallet;
    	
    }
    
    public static void LogDebug(String message)
    {
    	LOGGER.debug(message);
    }
    
    public static void LogInfo(String message)
    {
    	if(Config.COMMON != null && Config.COMMON.debugLevel.get() > 0)
    		LOGGER.debug("INFO: " + message);
    	else
    		LOGGER.info(message);
    }
    
    public static void LogWarning(String message)
    {
    	if(Config.COMMON != null && Config.COMMON.debugLevel.get() > 1)
    		LOGGER.debug("WARN: " + message);
    	else
    		LOGGER.warn(message);
    }
    
    public static void LogError(String message, Object... objects)
    {
    	if(Config.COMMON != null && Config.COMMON.debugLevel.get() > 2)
    		LOGGER.debug("ERROR: " + message, objects);
    	else
    		LOGGER.error(message, objects);
    }
    
    public static void LogError(String message)
    {
    	if(Config.COMMON != null && Config.COMMON.debugLevel.get() > 2)
    		LOGGER.debug("ERROR: " + message);
    	else
    		LOGGER.error(message);
    }
    
}
