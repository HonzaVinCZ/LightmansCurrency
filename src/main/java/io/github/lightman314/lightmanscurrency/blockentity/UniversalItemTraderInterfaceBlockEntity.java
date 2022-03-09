package io.github.lightman314.lightmanscurrency.blockentity;

import io.github.lightman314.lightmanscurrency.blockentity.handler.ItemInterfaceHandler;
import io.github.lightman314.lightmanscurrency.common.universal_traders.RemoteTradeData;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalTraderData;
import io.github.lightman314.lightmanscurrency.core.ModBlockEntities;
import io.github.lightman314.lightmanscurrency.menus.ItemInterfaceMenu;
import io.github.lightman314.lightmanscurrency.menus.containers.LockableContainer;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.BlockEntityUtil;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;

public class UniversalItemTraderInterfaceBlockEntity extends UniversalTraderInterfaceBlockEntity<ItemTradeData>{

	public static final int BUFFER_SIZE = 9;
	
	private LockableContainer itemBuffer = new LockableContainer(BUFFER_SIZE, this::setItemBufferDirty);
	public LockableContainer getItemBuffer() { return this.itemBuffer; }
	
	public UniversalItemTraderInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TRADER_INTERFACE_ITEM, pos, state, ItemTradeData::loadData);
		this.addHandler(new ItemInterfaceHandler(this, this::getItemBuffer));
	}

	@Override
	public RemoteTradeData getRemoteTradeData() {
		return new RemoteTradeData(this.owner, this.getBankAccount(), null, this.itemBuffer, null, null);
	}
	
	public boolean allowAnyInput() {
		if(this.getInteractionType().trades)
		{
			ItemTradeData trade = this.getReferencedTrade();
			return trade != null && (trade.isPurchase() || trade.isBarter());
		}
		else if(this.getInteractionType().restocks)
		{
			IItemTrader trader = this.getItemTrader();
			if(trader != null)
			{
				for(int i = 0; i < trader.getTradeCount(); ++i)
				{
					ItemTradeData trade = trader.getTrade(i);
					if(trade.isValid() && (trade.isSale() || trade.isBarter()))
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Whether the current interaction allows for this item to be placed into the buffer.
	 * Used to prevent undesired items from filling up the buffer.
	 */
	public boolean allowItemInput(ItemStack item) {
		if(this.getInteractionType().trades)
		{
			//Only allow inputs for the trade purchase/barter items
			ItemTradeData trade = this.getReferencedTrade();
			if(trade != null)
			{
				if(trade.isPurchase())
					return InventoryUtil.ItemMatches(item, trade.getSellItem());
				if(trade.isBarter())
					return InventoryUtil.ItemMatches(item, trade.getBarterItem());
			}
		}
		else if(this.getInteractionType().restocks)
		{
			//Only allow inputs for any trades sell item
			IItemTrader trader = this.getItemTrader();
			if(trader != null)
			{
				for(int i = 0; i < trader.getTradeCount(); ++i)
				{
					ItemTradeData trade = trader.getTrade(i);
					if(trade.isValid() && (trade.isSale() || trade.isBarter()) && InventoryUtil.ItemMatches(item, trade.getSellItem()))
						return true;
				}
			}
		}
		//Otherwise do not allow inputs.
		return false;
	}
	
	public boolean allowAnyOutput() {
		if(this.getInteractionType().trades)
		{
			ItemTradeData trade = this.getReferencedTrade();
			return trade != null && (trade.isSale() || trade.isBarter());
		}
		if(this.getInteractionType().drains)
		{
			IItemTrader trader = this.getItemTrader();
			for(int i = 0; i < trader.getTradeCount(); ++i)
			{
				ItemTradeData trade = trader.getTrade(i);
				if(trade.isValid() && (trade.isSale() || trade.isBarter()))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Whether the current interaction allows for this item to be removed from the buffer.
	 * Used to prevent items that will be used to maintain trader stock from being extracted.
	 */
	public boolean allowItemOutput(ItemStack item) {
		//Allow anything that cannot be input as an output.
		return !this.allowItemInput(item);
	}
	
	@Override
	protected ItemTradeData deserializeTrade(CompoundTag compound) { return ItemTradeData.loadData(compound); } 
	
	@Override
	protected void saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		this.saveItemBuffer(compound);
	}
	
	protected final CompoundTag saveItemBuffer(CompoundTag compound) {
		compound.put("ItemBuffer", this.itemBuffer.save(new CompoundTag()));
		return compound;
	}
	
	public void setItemBufferDirty() {
		this.setChanged();
		if(!this.isClient())
			BlockEntityUtil.sendUpdatePacket(this, this.saveItemBuffer(new CompoundTag()));
	}
	
	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		if(compound.contains("ItemBuffer", Tag.TAG_COMPOUND))
			this.itemBuffer = new LockableContainer(BUFFER_SIZE, compound.getCompound("ItemBuffer"), this::setItemBufferDirty);
	}

	@Override
	protected boolean validTraderType(UniversalTraderData trader) { return trader instanceof IItemTrader; }

	protected final IItemTrader getItemTrader() {
		UniversalTraderData trader = this.getTrader();
		if(trader instanceof IItemTrader)
			return (IItemTrader)trader;
		return null;
	}
	
	@Override
	protected void drainTick() {
		IItemTrader trader = this.getItemTrader();
		if(trader != null && trader.hasPermission(this.owner, Permissions.INTERACTION_LINK))
		{
			for(int i = 0; i < trader.getTradeCount(); ++i)
			{
				ItemTradeData trade = trader.getTrade(i);
				if(trade.isValid())
				{
					ItemStack drainItem = ItemStack.EMPTY;
					if(trade.isPurchase())
						drainItem = trade.getSellItem();
					if(trade.isBarter())
						drainItem = trade.getBarterItem();
					if(!drainItem.isEmpty())
					{
						//Drain the item from the trader
						int drainableAmount = InventoryUtil.GetItemCount(trader.getStorage(), drainItem);
						if(drainableAmount > 0)
						{
							ItemStack movingStack = drainItem.copy();
							movingStack.setCount(Math.min(movingStack.getMaxStackSize(), drainableAmount));
							//Remove the stack from storage
							InventoryUtil.RemoveItemCount(trader.getStorage(), movingStack);
							//Put the stack in the item buffer (if possible)
							ItemStack leftovers = ItemHandlerHelper.insertItemStacked(this.itemBuffer, movingStack, false);
							//If some items couldn't be put in the item buffer, put them back in storage
							if(!leftovers.isEmpty())
							{
								leftovers = InventoryUtil.TryPutItemStack(trader.getStorage(), movingStack);
								//Failed to put them all back in storage, so pop them out into the worl
								if(!leftovers.isEmpty())
									InventoryUtil.dumpContents(this.level, this.worldPosition, leftovers);
							}
							trader.markStorageDirty();
						}
					}
				}
			}
		}
	}

	@Override
	protected void restockTick() {
		IItemTrader trader = this.getItemTrader();
		if(trader != null && trader.hasPermission(this.owner, Permissions.INTERACTION_LINK))
		{
			for(int i = 0; i < trader.getTradeCount(); ++i)
			{
				ItemTradeData trade = trader.getTrade(i);
				if(trade.isValid() && (trade.isBarter() || trade.isSale()))
				{
					ItemStack stockItem = trade.getSellItem();
					if(!stockItem.isEmpty())
					{
						int stockableAmount = InventoryUtil.GetItemCount(this.itemBuffer.getContainer(), stockItem);
						if(stockableAmount > 0)
						{
							ItemStack movingStack = stockItem.copy();
							movingStack.setCount(Math.min(movingStack.getMaxStackSize(), stockableAmount));
							//Remove the item from the item buffer
							if(InventoryUtil.RemoveItemCount(this.itemBuffer.getContainer(), movingStack))
							{
								ItemStack leftovers = InventoryUtil.TryPutItemStack(trader.getStorage(), movingStack);
								if(!leftovers.isEmpty())
								{
									//Attempt to place the items back in the item buffer while obeying the rules (use the IItemHandler version of the item buffer)
									leftovers = ItemHandlerHelper.insertItemStacked(this.itemBuffer, leftovers, false);
									if(!leftovers.isEmpty())
									{
										//Attempt to place the items back in the item buffer while ignoring the rules (use the container directly)
										leftovers = InventoryUtil.TryPutItemStack(this.itemBuffer.getContainer(), leftovers);
										if(!leftovers.isEmpty())
										{
											//Could not place them back in the item buffer, so dump them in the world.
											if(!leftovers.isEmpty())
												InventoryUtil.dumpContents(this.level, this.worldPosition, leftovers);
										}
									}
								}
							}
							trader.markStorageDirty();
						}
					}
				}
			}
		}
	}

	@Override
	protected void tradeTick() {
		ItemTradeData trade = this.getTrueTrade();
		if(trade != null && trade.isValid())
		{
			this.itemBuffer.dontHonorFullLocks();
			this.itemBuffer.setAdditionalRules(null);
			if(trade.isSale())
			{
				//Confirm that we have enough space to store the purchased item(s)
				if(ItemHandlerHelper.insertItemStacked(this.itemBuffer, trade.getSellItem(), true).isEmpty())
				{
					this.interactWithTrader();
					this.setItemBufferDirty();
				}
			}
			else if(trade.isPurchase())
			{
				//Confirm that we have enough of the item in storage to sell the item(s)
				if(InventoryUtil.GetItemCount(this.itemBuffer.getContainer(), trade.getSellItem()) >= trade.getSellItem().getCount())
				{
					this.interactWithTrader();
					this.setItemBufferDirty();
				}
			}
			else if(trade.isBarter())
			{
				//Confirm that we have enough space to store the purchased item AND
				//That we have enough of the item in storage to barter away.
				if(InventoryUtil.GetItemCount(this.itemBuffer.getContainer(), trade.getBarterItem()) > trade.getBarterItem().getCount() &&
				   ItemHandlerHelper.insertItemStacked(this.itemBuffer, trade.getSellItem(), true).isEmpty())
				{
					this.interactWithTrader();
					this.setItemBufferDirty();
				}
			}
			this.itemBuffer.honorFullLocks();
		}
	}

	@Override
	protected MenuProvider getMenuProvider() {
		return new Provider(this);
	}

	@Override
	public void dumpContents(Level level, BlockPos pos) {
		InventoryUtil.dumpContents(level, pos, this.itemBuffer.getContainer());
	}
	
	private static class Provider implements MenuProvider {

		private final UniversalItemTraderInterfaceBlockEntity blockEntity;
		
		Provider(UniversalItemTraderInterfaceBlockEntity blockEntity) {
			this.blockEntity = blockEntity;
		}
		
		@Override
		public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
			return new ItemInterfaceMenu(windowId, inventory, this.blockEntity);
		}

		@Override
		public Component getDisplayName() {
			return new TextComponent("");
		}
		
	}
}
