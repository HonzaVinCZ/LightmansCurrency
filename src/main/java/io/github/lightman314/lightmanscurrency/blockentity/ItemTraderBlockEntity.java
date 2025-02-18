package io.github.lightman314.lightmanscurrency.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import io.github.lightman314.lightmanscurrency.client.gui.screen.ITradeRuleScreenHandler;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.core.ModBlockEntities;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.TradeCostEvent;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageUpdateTradeRule;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import io.github.lightman314.lightmanscurrency.trader.common.TraderItemStorage;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.settings.ItemTraderSettings;
import io.github.lightman314.lightmanscurrency.trader.settings.Settings;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.ITradeRuleHandler;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import io.github.lightman314.lightmanscurrency.util.BlockEntityUtil;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.ItemShopLogger;
import io.github.lightman314.lightmanscurrency.blockentity.ItemInterfaceBlockEntity.IItemHandlerBlock;
import io.github.lightman314.lightmanscurrency.blockentity.ItemInterfaceBlockEntity.IItemHandlerBlockEntity;
import io.github.lightman314.lightmanscurrency.blockentity.handler.TraderItemHandler;
import io.github.lightman314.lightmanscurrency.blocks.traderblocks.interfaces.IItemTraderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class ItemTraderBlockEntity extends TraderBlockEntity implements IItemTrader {
	
	public static final int VERSION = 1;
	
	TraderItemHandler itemHandler = new TraderItemHandler(this);
	
	public IItemHandler getItemHandler(Direction relativeSide)
	{
		return this.itemHandler.getHandler(relativeSide);
	}
	
	ItemTraderSettings itemSettings = new ItemTraderSettings(this, this::markItemSettingsDirty, this::sendSettingsUpdateToServer);
	
	@Override
	public Map<String,Integer> getAllyDefaultPermissions() { return ImmutableMap.of(Permissions.ItemTrader.EXTERNAL_INPUTS, 1); }
	
	protected TraderItemStorage storage = new TraderItemStorage(this);
	
	Container upgradeInventory = new SimpleContainer(5);
	public Container getUpgradeInventory() { return this.upgradeInventory; }
	
	public void markUpgradesDirty() {
		this.setChanged();
		if(!this.isClient())
			BlockEntityUtil.sendUpdatePacket(this, this.writeUpgradeInventory(new CompoundTag()));
	}
	
	private final ItemShopLogger logger = new ItemShopLogger();
	
	protected int tradeCount = 1;
	
	private long rotationTime = 0;
	
	protected List<ItemTradeData> trades;
	
	List<TradeRule> tradeRules = new ArrayList<>();
	
	public ItemTraderBlockEntity(BlockPos pos, BlockState state)
	{
		super(ModBlockEntities.ITEM_TRADER.get(), pos, state);
		this.trades = ItemTradeData.listOfSize(tradeCount);
		this.validateTradeRestrictions();
	}
	
	public ItemTraderBlockEntity(BlockPos pos, BlockState state, int tradeCount)
	{
		super(ModBlockEntities.ITEM_TRADER.get(), pos, state);
		this.tradeCount = tradeCount;
		this.trades = ItemTradeData.listOfSize(tradeCount);
		this.validateTradeRestrictions();
	}
	
	protected ItemTraderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
		this.trades = ItemTradeData.listOfSize(tradeCount);
		this.validateTradeRestrictions();
	}
	
	protected ItemTraderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int tradeCount)
	{
		super(type, pos, state);
		this.tradeCount = tradeCount;
		this.trades = ItemTradeData.listOfSize(tradeCount);
		this.validateTradeRestrictions();
	}
	
	public int getTradeCount()
	{
		return MathUtil.clamp(tradeCount, 1, ITrader.GLOBAL_TRADE_LIMIT);
	}
	
	public void addTrade(Player requestor)
	{
		if(this.level.isClientSide)
			return;
		if(tradeCount >= ITrader.GLOBAL_TRADE_LIMIT)
			return;
		if(TradingOffice.isAdminPlayer(requestor))
		{
			overrideTradeCount(tradeCount + 1);
			this.coreSettings.getLogger().LogAddRemoveTrade(requestor, true, this.tradeCount);
			this.markCoreSettingsDirty();
		}
		else
			Settings.PermissionWarning(requestor, "add a trade slot", Permissions.ADMIN_MODE);
	}
	
	public void removeTrade(Player requestor)
	{
		if(this.level.isClientSide)
			return;
		if(tradeCount <= 1)
			return;
		if(TradingOffice.isAdminPlayer(requestor))
		{
			overrideTradeCount(tradeCount - 1);
			this.coreSettings.getLogger().LogAddRemoveTrade(requestor, false, this.tradeCount);
			this.markCoreSettingsDirty();
		}
		else
			Settings.PermissionWarning(requestor, "remove a trade slot", Permissions.ADMIN_MODE);
	}
	
	public final void validateTradeRestrictions() {
		for(int i = 0; i < this.trades.size(); ++i)
		{
			ItemTradeData trade = this.trades.get(i);
			trade.setRestriction(this.getRestriction(i));
		}
	}
	
	public void overrideTradeCount(int newTradeCount)
	{
		if(tradeCount == newTradeCount)
			return;
		this.tradeCount = MathUtil.clamp(newTradeCount, 1, ITrader.GLOBAL_TRADE_LIMIT);
		List<ItemTradeData> oldTrades = trades;
		trades = ItemTradeData.listOfSize(getTradeCount());
		//Write the old trade data into the array.
		for(int i = 0; i < oldTrades.size() && i < trades.size(); i++)
		{
			trades.set(i, oldTrades.get(i));
		}
		this.validateTradeRestrictions();
		//Send an update to the client
		if(!this.level.isClientSide)
		{
			//Send update packet
			CompoundTag compound = this.writeTrades(new CompoundTag());
			BlockEntityUtil.sendUpdatePacket(this, this.writeStorage(compound));
		}
		
	}
	
	public ItemTradeData getTrade(int tradeSlot)
	{
		if(tradeSlot < 0 || tradeSlot >= this.trades.size())
		{
			LightmansCurrency.LogError("Cannot get trade in index " + tradeSlot + " from a trader with only " + this.trades.size() + " trades.");
			return new ItemTradeData();
		}
		return this.trades.get(tradeSlot);
	}
	
	public List<ItemTradeData> getAllTrades()
	{
		return this.trades;
	}
	
	public void markTradesDirty()
	{
		//Send an update to the client
		if(!this.level.isClientSide)
		{
			//Send update packet
			BlockEntityUtil.sendUpdatePacket(this, this.writeTrades(new CompoundTag()));
		}
		this.setChanged();
	}
	
	public ItemTraderSettings getItemSettings()
	{
		return this.itemSettings;
	}
	
	public List<Settings> getAdditionalSettings() { return Lists.newArrayList(this.itemSettings); }
	
	public void markItemSettingsDirty()
	{
		if(this.level.isClientSide)
		{
			BlockEntityUtil.sendUpdatePacket(this, this.writeItemSettings(new CompoundTag()));
		}
	}
	
	public void markStorageDirty()
	{
		//Send an update to the client
		if(!this.level.isClientSide)
		{
			//Send update packet
			BlockEntityUtil.sendUpdatePacket(this, this.writeStorage(new CompoundTag()));
		}
		this.setChanged();
	}
	
	public ItemShopLogger getLogger() {return this.logger; }
	
	public void clearLogger()
	{
		this.logger.clear();
		markLoggerDirty();
	}
	
	public void markLoggerDirty()
	{
		//Send an update to the client
		if(!this.level.isClientSide)
		{
			//Send update packet
			BlockEntityUtil.sendUpdatePacket(this, this.writeLogger(new CompoundTag()));
		}
		this.setChanged();
	}
	
	public int getTradeStock(int tradeSlot)
	{
		ItemTradeData trade = getTrade(tradeSlot);
		if(trade.sellItemsDefined())
		{
			if(this.coreSettings.isCreative())
				return Integer.MAX_VALUE;
			else
				return trade.stockCount(this);
		}
		return 0;
	}
	
	@OnlyIn(Dist.CLIENT)
	public List<Vector3f> GetStackRenderPos(int tradeSlot, boolean isDoubleTrade)
	{
		Block block = this.getBlockState().getBlock();
		if(block instanceof IItemTraderBlock)
		{
			IItemTraderBlock traderBlock = (IItemTraderBlock)block;
			return traderBlock.GetStackRenderPos(tradeSlot, this.getBlockState(), isDoubleTrade);
		}
		else
		{
			List<Vector3f> posList = new ArrayList<>();
			posList.add(new Vector3f(0.0f, 0.0f, 0.0f));
			return posList;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public List<Quaternion> GetStackRenderRot(int tradeSlot, float partialTicks)
	{
		Block block = this.getBlockState().getBlock();
		if(block instanceof IItemTraderBlock)
		{
			IItemTraderBlock traderBlock = (IItemTraderBlock)block;
			List<Quaternion> rotation = traderBlock.GetStackRenderRot(tradeSlot, this.getBlockState());
			//If null received. Rotate item based on world time
			if(rotation == null)
			{
				rotation = new ArrayList<>();
				rotation.add(Vector3f.YP.rotationDegrees((this.rotationTime + partialTicks) * 2.0F));
			}
			return rotation;
		}
		else
		{
			List<Quaternion> rotation = new ArrayList<>();
			rotation.add(Vector3f.YP.rotationDegrees(0f));
			return rotation;
		}
			
	}
	
	@OnlyIn(Dist.CLIENT)
	public float GetStackRenderScale(int tradeSlot)
	{
		Block block = this.getBlockState().getBlock();
		if(block instanceof IItemTraderBlock)
		{
			IItemTraderBlock traderBlock = (IItemTraderBlock)block;
			return traderBlock.GetStackRenderScale(tradeSlot, this.getBlockState());
		}
		else
			return 0f;
	}
	
	@OnlyIn(Dist.CLIENT)
	public int maxRenderIndex()
	{
		Block block = this.getBlockState().getBlock();
		if(block instanceof IItemTraderBlock)
		{
			IItemTraderBlock traderBlock = (IItemTraderBlock)block;
			return traderBlock.maxRenderIndex();
		}
		else
			return 0;
	}
	
	@Override
	public void saveAdditional(CompoundTag compound)
	{
		
		super.saveAdditional(compound);
		
		this.writeStorage(compound);
		this.writeTrades(compound);
		this.writeItemSettings(compound);
		this.writeUpgradeInventory(compound);
		this.writeLogger(compound);
		this.writeTradeRules(compound);
		
	}
	
	protected CompoundTag writeStorage(CompoundTag compound)
	{
		this.storage.save(compound, "Storage");
		return compound;
	}
	
	public CompoundTag writeTrades(CompoundTag compound)
	{
		compound.putInt("TradeLimit", this.tradeCount);
		ItemTradeData.saveAllData(compound, this.trades);
		return compound;
	}
	
	public CompoundTag writeItemSettings(CompoundTag compound)
	{
		compound.put("ItemSettings", this.itemSettings.save(new CompoundTag()));
		return compound;
	}
	
	public CompoundTag writeUpgradeInventory(CompoundTag compound)
	{
		InventoryUtil.saveAllItems("UpgradeInventory", compound, this.upgradeInventory);
		return compound;
	}
	
	public CompoundTag writeLogger(CompoundTag compound)
	{
		this.logger.write(compound);
		return compound;
	}
	
	public CompoundTag writeTradeRules(CompoundTag compound)
	{
		TradeRule.writeRules(compound, this.tradeRules);
		return compound;
	}
	
	@Override
	public void load(CompoundTag compound)
	{
		
		//Load the trade limit
		if(compound.contains("TradeLimit", Tag.TAG_INT))
			this.tradeCount = MathUtil.clamp(compound.getInt("TradeLimit"), 1, ITrader.GLOBAL_TRADE_LIMIT);
		//Load trades
		if(compound.contains(ItemTradeData.DEFAULT_KEY))
		{
			this.trades = ItemTradeData.loadAllData(compound, this.getTradeCount());
			this.validateTradeRestrictions();
		}
		
		//Load the inventory
		if(compound.contains("Storage"))
		{
			this.storage.load(compound, "Storage");
		}
		else if(compound.contains("Items"))
		{
			Container container = InventoryUtil.loadAllItems("Items", compound, this.getTradeCount() * 9);
			this.storage.loadFromContainer(container);
		}
		
		if(compound.contains("UpgradeInventory",Tag.TAG_LIST))
			this.upgradeInventory = InventoryUtil.loadAllItems("UpgradeInventory", compound, 5);
		
		if(compound.contains("ItemSettings", Tag.TAG_COMPOUND))
			this.itemSettings.load(compound.getCompound("ItemSettings"));
		
		//Load the shop logger
		this.logger.read(compound);
		
		//Load the trade rules
		if(compound.contains(TradeRule.DEFAULT_TAG, Tag.TAG_LIST))
			this.tradeRules = TradeRule.readRules(compound);
		
		super.load(compound);
		
	}
	
	@Override
	public void dumpContents(Level world, BlockPos pos)
	{
		super.dumpContents(world, pos);
		//Dump the storage
		InventoryUtil.dumpContents(world, pos, this.storage.getContents());
		//Dump the upgrades
		InventoryUtil.dumpContents(world, pos, this.upgradeInventory);
	}
	
	@Override
	public AABB getRenderBoundingBox()
	{
		if(this.getBlockState() != null)
			return this.getBlockState().getCollisionShape(this.level, this.worldPosition).bounds().move(this.worldPosition);
		return super.getRenderBoundingBox();
	}

	@Override
	public void clientTick() {
		
		super.clientTick();
		this.rotationTime++;
	}

	@Override
	public TraderItemStorage getStorage() {
		return this.storage;
	}

	@Override
	public int GetCurrentVersion() { return VERSION; }
	
	@Override
	protected void onVersionUpdate(int oldVersion) {
		//Updated so that the items in the trade data are not actual items, so place them in storage (if possible), or spawn them at the traders current position.
		if(oldVersion < 1)
		{
			for(ItemTradeData trade : trades)
			{
				ItemStack tradeStack = trade.getSellItem(0);
				if(!tradeStack.isEmpty())
					this.storage.forceAddItem(tradeStack);
			}
		}
		
	}

	@Override
	public void beforeTrade(PreTradeEvent event) {
		this.tradeRules.forEach(rule -> rule.beforeTrade(event));
	}
	
	@Override
	public void tradeCost(TradeCostEvent event)
	{
		this.tradeRules.forEach(rule -> rule.tradeCost(event));
	}

	@Override
	public void afterTrade(PostTradeEvent event) {
		this.tradeRules.forEach(rule -> rule.afterTrade(event));
	}
	
	public void addRule(TradeRule newRule)
	{
		if(newRule == null)
			return;
		//Confirm a lack of duplicate rules
		for(int i = 0; i < this.tradeRules.size(); i++)
		{
			if(newRule.type == this.tradeRules.get(i).type)
			{
				LightmansCurrency.LogInfo("Blocked rule addition due to rule of same type already present.");
				return;
			}
		}
		this.tradeRules.add(newRule);
	}
	
	public List<TradeRule> getRules() { return this.tradeRules; }
	
	public void setRules(List<TradeRule> rules) { this.tradeRules = rules; }
	
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
		//Send an update to the client
		if(!this.level.isClientSide)
		{
			//Send update packet
			BlockEntityUtil.sendUpdatePacket(this, this.writeTradeRules(new CompoundTag()));
		}
		this.setChanged();
	}
	
	public void closeRuleScreen(Player player)
	{
		this.openStorageMenu(player);
	}
	
	public ITradeRuleScreenHandler getRuleScreenHandler(int tradeIndex) { return new TraderScreenHandler(this, tradeIndex); }
	
	private static class TraderScreenHandler implements ITradeRuleScreenHandler
	{
		
		private final ItemTraderBlockEntity tileEntity;
		private final int tradeIndex;
		
		public TraderScreenHandler(ItemTraderBlockEntity tileEntity, int tradeIndex)
		{
			this.tileEntity = tileEntity;
			this.tradeIndex = tradeIndex;
		}
		
		@Override
		public ITradeRuleHandler ruleHandler() { 
			if(this.tradeIndex < 0)
				return this.tileEntity;
			return this.tileEntity.getTrade(this.tradeIndex);
		}
		
		@Override
		public void reopenLastScreen()
		{
			this.tileEntity.sendOpenStorageMessage();
		}
		
		@Override
		public void updateServer(ResourceLocation type, CompoundTag updateInfo)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageUpdateTradeRule(this.tileEntity.worldPosition, this.tradeIndex, type, updateInfo));
		}
		
		@Override
		public boolean stillValid() { return !this.tileEntity.isRemoved(); }
		
	}
	
	//Item capability for hopper and item automation
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if(cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
		{
			Block block = this.getBlockState().getBlock();
			if(block instanceof IItemHandlerBlock)
			{
				IItemHandlerBlock handlerBlock = (IItemHandlerBlock)block;
				IItemHandlerBlockEntity blockEntity = handlerBlock.getItemHandlerEntity(this.getBlockState(), this.level, this.worldPosition);
				if(blockEntity != null)
				{
					IItemHandler handler = blockEntity.getItemHandler(handlerBlock.getRelativeSide(this.getBlockState(), side));
					if(handler != null)
						return LazyOptional.of(() -> handler).cast();
					else
						return LazyOptional.empty();
				}
			}
		}
		return super.getCapability(cap, side);
	}
	
	@Override
	public void sendTradeRuleUpdateMessage(int tradeIndex, ResourceLocation type, CompoundTag updateInfo) {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageUpdateTradeRule(this.worldPosition, tradeIndex, type, updateInfo));
	}
	
	/*@Override
	public void sendSetTradeItemMessage(int tradeIndex, ItemStack sellItem, int slot) {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetTradeItem(this.worldPosition, tradeIndex, sellItem, slot));
	}
	
	@Override
	public void sendSetTradePriceMessage(int tradeIndex, CoinValue newPrice, String newCustomName, ItemTradeType newTradeType) {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageSetItemPrice(this.worldPosition, tradeIndex, newPrice, newCustomName, newTradeType.name()));
	}*/

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
			this.markTradesDirty();
		}
		else
		{
			this.updateRule(ruleType, updateInfo);
			this.markRulesDirty();
		}
	}
	
}
