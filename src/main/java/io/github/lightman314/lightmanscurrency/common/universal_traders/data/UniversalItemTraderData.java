package io.github.lightman314.lightmanscurrency.common.universal_traders.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.ItemShopLogger;
import io.github.lightman314.lightmanscurrency.blockentity.handler.TraderItemHandler;
import io.github.lightman314.lightmanscurrency.blocks.ItemTraderServerBlock;
import io.github.lightman314.lightmanscurrency.client.ClientTradingOffice;
import io.github.lightman314.lightmanscurrency.client.gui.screen.ITradeRuleScreenHandler;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.core.ModBlocks;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.TradeCostEvent;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.universal_trader.MessageAddOrRemoveTrade2;
import io.github.lightman314.lightmanscurrency.network.message.universal_trader.MessageOpenStorage2;
import io.github.lightman314.lightmanscurrency.network.message.universal_trader.MessageUpdateTradeRule2;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import io.github.lightman314.lightmanscurrency.trader.common.TraderItemStorage;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.settings.ItemTraderSettings;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import io.github.lightman314.lightmanscurrency.trader.settings.Settings;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.ITradeRuleHandler;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.util.FileUtil;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import io.github.lightman314.lightmanscurrency.money.CoinValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

public class UniversalItemTraderData extends UniversalTraderData implements IItemTrader {
	
	public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID, "item_trader");
	
	public static final int VERSION = 1;
	
	TraderItemHandler itemHandler = new TraderItemHandler(this);
	
	public IItemHandler getItemHandler(Direction relativeSide)
	{
		return this.itemHandler.getHandler(relativeSide);
	}
	
	private ItemTraderSettings itemSettings = new ItemTraderSettings(this, this::markItemSettingsDirty, this::sendSettingsUpdateToServer);
	
	int tradeCount = 1;
	List<ItemTradeData> trades = null;
	
	TraderItemStorage storage = new TraderItemStorage(this);
	
	Container upgradeInventory = new SimpleContainer(5);
	public Container getUpgradeInventory() { return this.upgradeInventory; }
	public void markUpgradesDirty() { this.markDirty(this::writeUpgrades); }
	
	private final ItemShopLogger logger = new ItemShopLogger();
	
	List<TradeRule> tradeRules = new ArrayList<>();
	
	public UniversalItemTraderData() {}
	
	public UniversalItemTraderData(PlayerReference owner, BlockPos pos, ResourceKey<Level> world, UUID traderID, int tradeCount)
	{
		super(owner, pos, world, traderID);
		this.tradeCount = MathUtil.clamp(tradeCount, 1, ITrader.GLOBAL_TRADE_LIMIT);
		this.trades = ItemTradeData.listOfSize(this.tradeCount);
	}
	
	@Override
	protected ItemLike getCategoryItem() {
		int tradeCount = this.isCreative() ? ITrader.GLOBAL_TRADE_LIMIT : this.getTradeCount();
		if(tradeCount <= ItemTraderServerBlock.SMALL_SERVER_COUNT)
			return ModBlocks.ITEM_TRADER_SERVER_SMALL.get();
		else if(tradeCount <= ItemTraderServerBlock.MEDIUM_SERVER_COUNT)
			return ModBlocks.ITEM_TRADER_SERVER_MEDIUM.get();
		else if(tradeCount <= ItemTraderServerBlock.LARGE_SERVER_COUNT)
			return ModBlocks.ITEM_TRADER_SERVER_LARGE.get();
		return ModBlocks.ITEM_TRADER_SERVER_EXTRA_LARGE.get();
		
	}

	@Override
	public void read(CompoundTag compound)
	{
		if(compound.contains("TradeLimit", Tag.TAG_INT))
			this.tradeCount = MathUtil.clamp(compound.getInt("TradeLimit"), 1, ITrader.GLOBAL_TRADE_LIMIT);
		
		if(compound.contains(ItemTradeData.DEFAULT_KEY, Tag.TAG_LIST))
			this.trades = ItemTradeData.loadAllData(compound, this.tradeCount);
		
		if(compound.contains("Storage", Tag.TAG_LIST))
		{
			this.storage.load(compound, "Storage");
			ListTag list = compound.getList("Storage", Tag.TAG_COMPOUND);
			if(list.size() <= 0 || !list.getCompound(0).contains("Slot"))
			{
				this.storage.load(compound, "Storage");
			}
			else
			{
				Container container = InventoryUtil.loadAllItems("Storage", compound, this.getTradeCount() * 9);
				this.storage.loadFromContainer(container);
			}
		}
		
		if(compound.contains("UpgradeInventory", Tag.TAG_LIST))
			this.upgradeInventory = InventoryUtil.loadAllItems("UpgradeInventory", compound, 5);
		
		this.logger.read(compound);
		
		if(compound.contains(TradeRule.DEFAULT_TAG, Tag.TAG_LIST))
			this.tradeRules = TradeRule.readRules(compound);
		
		if(compound.contains("ItemSettings", Tag.TAG_COMPOUND))
			this.itemSettings.load(compound.getCompound("ItemSettings"));
		
		super.read(compound);
	}
	
	@Override
	public CompoundTag write(CompoundTag compound)
	{

		this.writeTrades(compound);
		this.writeStorage(compound);
		this.writeUpgrades(compound);
		this.writeLogger(compound);
		this.writeRules(compound);
		this.writeItemSettings(compound);
		
		return super.write(compound);
		
	}
	
	protected final CompoundTag writeTrades(CompoundTag compound)
	{
		compound.putInt("TradeLimit", this.trades.size());
		ItemTradeData.saveAllData(compound, trades);
		return compound;
	}
	
	protected final CompoundTag writeStorage(CompoundTag compound)
	{
		this.storage.save(compound, "Storage");
		return compound;
	}
	
	protected final CompoundTag writeUpgrades(CompoundTag compound)
	{
		InventoryUtil.saveAllItems("UpgradeInventory", compound, this.upgradeInventory);
		return compound;
	}
	
	protected final CompoundTag writeLogger(CompoundTag compound)
	{
		this.logger.write(compound);
		return compound;
	}
	
	protected final CompoundTag writeRules(CompoundTag compound)
	{
		TradeRule.writeRules(compound, this.tradeRules);
		return compound;
	}
	
	protected final CompoundTag writeItemSettings(CompoundTag compound)
	{
		compound.put("ItemSettings", this.itemSettings.save(new CompoundTag()));
		return compound;
	}
	
	public int getTradeCount()
	{
		return this.tradeCount;
	}
	
	public void requestAddOrRemoveTrade(boolean isAdd)
	{
		LightmansCurrencyPacketHandler.instance.sendToServer(new MessageAddOrRemoveTrade2(this.traderID, isAdd));
	}
	
	public void addTrade(Player requestor)
	{
		if(!TradingOffice.isAdminPlayer(requestor))
		{
			Settings.PermissionWarning(requestor, "add trade slot", Permissions.ADMIN_MODE);
			return;
		}
		if(this.getTradeCount() >= ITrader.GLOBAL_TRADE_LIMIT)
			return;
		this.overrideTradeCount(this.tradeCount + 1);
		//this.forceReopen();
		this.coreSettings.getLogger().LogAddRemoveTrade(requestor, true, this.tradeCount);
		this.markCoreSettingsDirty();
	}
	
	public void removeTrade(Player requestor)
	{
		if(!TradingOffice.isAdminPlayer(requestor))
		{
			Settings.PermissionWarning(requestor, "remove trade slot", Permissions.ADMIN_MODE);
			return;
		}
		if(this.getTradeCount() <= 1)
			return;
		this.overrideTradeCount(this.tradeCount - 1);
		//this.forceReopen();
		this.coreSettings.getLogger().LogAddRemoveTrade(requestor, true, this.tradeCount);
		this.markCoreSettingsDirty();
	}
	
	public void overrideTradeCount(int newTradeCount)
	{
		if(this.tradeCount == newTradeCount)
			return;
		this.tradeCount = MathUtil.clamp(newTradeCount, 1, ITrader.GLOBAL_TRADE_LIMIT);
		List<ItemTradeData> oldTrades = this.trades;
		this.trades = ItemTradeData.listOfSize(getTradeCount());
		//Write the old trade data into the array.
		for(int i = 0; i < oldTrades.size() && i < this.trades.size(); i++)
		{
			this.trades.set(i, oldTrades.get(i));
		}
		//Mark as dirty (both trades & storage)
		CompoundTag compound = this.writeTrades(new CompoundTag());
		this.writeStorage(compound);
		this.markDirty(compound);
	}
	
	public ItemTradeData getTrade(int tradeIndex)
	{
		if(tradeIndex >= 0 && tradeIndex < getTradeCount())
		{
			return this.trades.get(tradeIndex);
		}
		return new ItemTradeData();
	}
	
	public int getTradeStock(int tradeIndex)
	{
		return getTrade(tradeIndex).stockCount(this);
	}
	
	public List<ItemTradeData> getAllTrades()
	{
		return this.trades;
	}
	
	public void markTradesDirty()
	{
		//Send update to the client with only the trade data.
		this.markDirty(this::writeTrades);
	}
	
	public ItemTraderSettings getItemSettings() { return this.itemSettings; }
	
	public List<Settings> getAdditionalSettings() { return Lists.newArrayList(); }
	
	public void markItemSettingsDirty()
	{
		this.markDirty(this::writeItemSettings);
	}
	
	public ItemShopLogger getLogger() { return this.logger; }
	
	public void clearLogger()
	{
		this.logger.clear();
		this.markLoggerDirty();
	}
	
	public void markLoggerDirty()
	{
		this.markDirty(this::writeLogger);
	}
	
	public int inventorySize()
	{
		return this.tradeCount * 9;
	}
	
	public TraderItemStorage getStorage()
	{
		return this.storage;
	}
	
	public void markStorageDirty()
	{
		this.markDirty(this::writeStorage);
	}
	
	@Override
	public ResourceLocation getTraderType() {
		return TYPE;
	}
	
	@Override
	public MutableComponent getDefaultName()
	{
		return Component.translatable("gui.lightmanscurrency.universaltrader.item");
	}

	/*@Override
	protected MenuProvider getTradeMenuProvider() {
		return new TraderProvider(this.traderID);
	}
	
	private static class TraderProvider implements MenuProvider
	{
		final UUID traderID;
		private TraderProvider(UUID traderID) { this.traderID = traderID; }
		@Override
		public AbstractContainerMenu createMenu(int menuID, Inventory inventory, Player player) {
			return new ItemTraderMenu.ItemTraderMenuUniversal(menuID, inventory, this.traderID);
		}
		@Override
		public Component getDisplayName() { return new TextComponent(""); }
	}*/

	/*@Override
	protected MenuProvider getStorageMenuProvider() {
		return new StorageProvider(this.traderID);
	}
	
	protected MenuProvider getItemEditMenuProvider(int tradeIndex) { return new ItemEditProvider(this.traderID, tradeIndex); }
	
	public void openItemEditMenu(Player player, int tradeIndex)
	{
		MenuProvider provider = getItemEditMenuProvider(tradeIndex);
		if(provider == null)
		{
			LightmansCurrency.LogError("No storage container provider was given for the universal trader of type " + this.getTraderType().toString());
			return;
		}
		if(player instanceof ServerPlayer)
			NetworkHooks.openGui((ServerPlayer)player, provider, new TradeIndexDataWriter(this.getTraderID(), tradeIndex));
		else
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the trade menu.");
	}
	
	private static class StorageProvider implements MenuProvider
	{
		final UUID traderID;
		private StorageProvider(UUID traderID) { this.traderID = traderID; }
		@Override
		public AbstractContainerMenu createMenu(int menuID, Inventory inventory, Player player) {
			return new ItemTraderStorageMenu.ItemTraderStorageMenuUniversal(menuID, inventory, this.traderID);
		}
		@Override
		public Component getDisplayName() { return new TextComponent(""); }
	}
	
	private static class ItemEditProvider implements MenuProvider
	{
		final UUID traderID;
		final int tradeIndex;
		private ItemEditProvider(UUID traderID, int tradeIndex) { this.traderID = traderID; this.tradeIndex = tradeIndex; }
		
		@Override
		public AbstractContainerMenu createMenu(int menuID, Inventory inventory, Player player) {
			return new ItemEditMenu.UniversalItemEditMenu(menuID, inventory, this.traderID, this.tradeIndex);
		}

		@Override
		public Component getDisplayName() { return new TextComponent(""); }
	}*/
	
	@Override
	public IconData getIcon() { return IconAndButtonUtil.ICON_TRADER; }
	
	@Override
	public int GetCurrentVersion() { return VERSION; }

	@Override
	protected void onVersionUpdate(int oldVersion) {
		//Updated so that the items in the trade data are not actual items, so place them in storage (if possible), or spawn them at the traders current position.
		if(oldVersion < 1)
		{
			for(ItemTradeData trade : trades)
			{
				this.storage.forceAddItem(trade.getSellItem(0));
			}
		}
	}
	
	@Override
	public void beforeTrade(PreTradeEvent event) {
		this.tradeRules.forEach(rule -> rule.beforeTrade(event));
	}
	
	@Override
	public void tradeCost(TradeCostEvent event) {
		this.tradeRules.forEach(rule -> rule.tradeCost(event));
	}

	@Override
	public void afterTrade(PostTradeEvent event) {
		this.tradeRules.forEach(rule -> rule.afterTrade(event));
	}

	public List<TradeRule> getRules() { return this.tradeRules; }
	
	public void setRules(List<TradeRule> rules) { this.tradeRules = rules; }
	
	public void addRule(TradeRule newRule)
	{
		if(newRule == null)
			return;
		//Confirm a lack of duplicate rules
		for(int i = 0; i < this.tradeRules.size(); i++)
		{
			if(newRule.type == this.tradeRules.get(i).type)
				return;
		}
		this.tradeRules.add(newRule);
	}
	
	public void removeRule(TradeRule rule)
	{
		if(this.tradeRules.contains(rule))
			this.tradeRules.remove(rule);
	}
	
	public void clearRules()
	{
		this.tradeRules.clear();
	}
	
	public void markRulesDirty()
	{
		this.markDirty(this::writeRules);
	}
	
	public ITradeRuleScreenHandler getRuleScreenHandler(int tradeIndex) { return new TradeRuleScreenHandler(this, tradeIndex); }
	
	private static class TradeRuleScreenHandler implements ITradeRuleScreenHandler
	{
		
		private final UniversalItemTraderData trader;
		private final int tradeIndex;
		
		public TradeRuleScreenHandler(UniversalItemTraderData trader, int tradeIndex)
		{
			this.trader = trader;
			this.tradeIndex = tradeIndex;
		}
		
		@Override
		public ITradeRuleHandler ruleHandler() {
			if(this.tradeIndex < 0)
				return this.trader;
			return this.trader.getTrade(this.tradeIndex);
		}
		
		@Override
		public void reopenLastScreen()
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenStorage2(this.trader.traderID));
		}
		
		public void updateServer(ResourceLocation type, CompoundTag updateInfo)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageUpdateTradeRule2(this.trader.traderID, this.tradeIndex, type, updateInfo));
		}
		
		@Override
		public boolean stillValid() { return ClientTradingOffice.getData(this.trader.traderID) != null; }
		
	}

	/*@Override
	protected void forceReopen(List<Player> users) {
		for(Player player : users)
		{
			if(player.containerMenu instanceof ItemTraderStorageMenu)
				this.openStorageMenu(player);
			else if(player.containerMenu instanceof ItemTraderMenuUniversal)
				this.openTradeMenu(player);
		}
		
	}*/
	
	@Override
	public void sendTradeRuleUpdateMessage(int tradeIndex, ResourceLocation type, CompoundTag updateInfo) {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageUpdateTradeRule2(this.traderID, tradeIndex, type, updateInfo));
	}

	/*@Override
	public void sendSetTradeItemMessage(int tradeIndex, ItemStack sellItem, int slot) {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetTradeItem2(this.traderID, tradeIndex, sellItem, slot));
	}
	
	@Override
	public void sendSetTradePriceMessage(int tradeIndex, CoinValue newPrice, String newCustomName, ItemTradeType newTradeType) {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetItemPrice2(this.traderID, tradeIndex, newPrice, newCustomName, newTradeType.name()));
	}*/

	@Override
	public CompoundTag getPersistentData() {
		CompoundTag data = new CompoundTag();
		ITradeRuleHandler.savePersistentRuleData(data, this, this.trades);
		this.logger.write(data);
		return data;
	}

	@Override
	public void loadPersistentData(CompoundTag data) {
		ITradeRuleHandler.readPersistentRuleData(data, this, this.trades);
		this.logger.read(data);
	}
	
	@Override
	public void loadFromJson(JsonObject json) throws Exception {
		super.loadFromJson(json);
		
		if(!json.has("Trades"))
			throw new Exception("Item Trader must have a trade list.");
		JsonArray tradeList = json.get("Trades").getAsJsonArray();
		
		this.trades = new ArrayList<>();
		for(int i = 0; i < tradeList.size() && this.trades.size() < ITrader.GLOBAL_TRADE_LIMIT; ++i)
		{
			try {
				JsonObject tradeData = tradeList.get(i).getAsJsonObject();
				ItemTradeData newTrade = new ItemTradeData();
				//Sell Item
				JsonObject sellItem = tradeData.get("SellItem").getAsJsonObject();
				newTrade.setItem(FileUtil.parseItemStack(sellItem), 0);
				if(tradeData.has("SellItem2"))
					newTrade.setItem(FileUtil.parseItemStack(tradeData.get("SellItem2").getAsJsonObject()), 1);
				//Trade Type
				if(tradeData.has("TradeType"))
					newTrade.setTradeType(ItemTradeData.loadTradeType(tradeData.get("TradeType").getAsString()));
				//Trade Price
				if(tradeData.has("Price"))
				{
					if(newTrade.isBarter())
						LightmansCurrency.LogWarning("Price is being defined for a barter trade. Price will be ignored.");
					else
						newTrade.setCost(CoinValue.Parse(tradeData.get("Price")));
				}
				else if(!newTrade.isBarter())
				{
					LightmansCurrency.LogWarning("Price is not defined on a non-barter trade. Price will be assumed to be free.");
					newTrade.getCost().setFree(true);
				}
				if(tradeData.has("BarterItem"))
				{
					if(newTrade.isBarter())
					{
						JsonObject barterItem = tradeData.get("BarterItem").getAsJsonObject();
						newTrade.setItem(FileUtil.parseItemStack(barterItem), 2);
						if(tradeData.has("BarterItem2"))
							newTrade.setItem(FileUtil.parseItemStack(tradeData.get("BarterItem2").getAsJsonObject()), 3);
					}
					else
					{
						LightmansCurrency.LogWarning("BarterItem is being defined for a non-barter trade. Barter item will be ignored.");
					}
				}
				if(tradeData.has("DisplayName"))
				{
					newTrade.setCustomName(0, tradeData.get("DisplayName").getAsString());
				}
				if(tradeData.has("DisplayName2"))
				{
					newTrade.setCustomName(1, tradeData.get("DisplayName2").getAsString());
				}
				if(tradeData.has("TradeRules"))
				{
					newTrade.setRules(TradeRule.Parse(tradeData.get("TradeRules").getAsJsonArray()));
				}
				this.trades.add(newTrade);
				
			} catch(Exception e) { LightmansCurrency.LogError("Error parsing item trade at index " + i, e); }
			
			if(this.trades.size() <= 0)
				throw new Exception("Trader has no valid trades!");
			
			this.tradeCount = this.trades.size();
			//Lock the storage for json loaded traders
			this.storage = new TraderItemStorage.LockedTraderStorage(this);
			
		}
		
		if(json.has("TradeRules"))
		{
			this.tradeRules = TradeRule.Parse(json.get("TradeRules").getAsJsonArray());
		}
		
	}
	
	@Override
	public JsonObject saveToJson(JsonObject json) {
		super.saveToJson(json);
		
		JsonArray trades = new JsonArray();
		for(ItemTradeData trade : this.trades) {
			if(trade.isValid())
			{
				JsonObject tradeData = new JsonObject();
				tradeData.addProperty("TradeType", trade.getTradeType().name());
				if(trade.getSellItem(0).isEmpty())
				{
					tradeData.add("SellItem", FileUtil.convertItemStack(trade.getSellItem(1)));
					if(trade.hasCustomName(0))
						tradeData.addProperty("DisplayName", trade.getCustomName(0));
				}
				else
				{
					tradeData.add("SellItem", FileUtil.convertItemStack(trade.getSellItem(0)));
					if(trade.hasCustomName(0))
						tradeData.addProperty("DisplayName", trade.getCustomName(0));
					if(!trade.getSellItem(1).isEmpty())
					{
						tradeData.add("SellItem2", FileUtil.convertItemStack(trade.getSellItem(1)));
						if(trade.hasCustomName(1))
							tradeData.addProperty("DisplayName2", trade.getCustomName(1));
					}
						
				}
				if(trade.isSale() || trade.isPurchase())
					tradeData.add("Price", trade.getCost().toJson());
				if(trade.isBarter())
				{
					if(trade.getBarterItem(0).isEmpty())
					{
						tradeData.add("BarterItem", FileUtil.convertItemStack(trade.getBarterItem(1)));
					}
					else
					{
						tradeData.add("BarterItem", FileUtil.convertItemStack(trade.getBarterItem(0)));
						if(!trade.getBarterItem(1).isEmpty())
							tradeData.add("BarterItem2", FileUtil.convertItemStack(trade.getBarterItem(1)));
					}
				}
				if(trade.getRules().size() > 0)
					tradeData.add("TradeRules", TradeRule.saveRulesToJson(trade.getRules()));
				
				trades.add(tradeData);
			}
		}
		json.add("Trades", trades);
		
		if(this.tradeRules.size() > 0)
			json.add("TradeRules", TradeRule.saveRulesToJson(this.tradeRules));
		
		return json;
	}

	@Override
	public boolean canInteractRemotely() { return true; }

	@Override
	public void receiveTradeRuleMessage(Player player, int index, ResourceLocation ruleType, CompoundTag updateInfo) {
		if(!this.hasPermission(player, Permissions.EDIT_TRADE_RULES))
		{
			Settings.PermissionWarning(player, "edit trade rule", Permissions.EDIT_TRADE_RULES);
			return;
		}
		if(index >= 0)
		{
			this.getTrade(index).updateRule(ruleType, updateInfo);
		}
		else
		{
			this.updateRule(ruleType, updateInfo);
		}
		
	}
	
}
