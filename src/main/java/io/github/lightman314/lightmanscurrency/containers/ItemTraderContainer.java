package io.github.lightman314.lightmanscurrency.containers;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.client.gui.widget.button.interfaces.ITradeButtonContainer;
import io.github.lightman314.lightmanscurrency.common.ItemTraderUtil;
import io.github.lightman314.lightmanscurrency.containers.interfaces.ITraderContainer;
import io.github.lightman314.lightmanscurrency.containers.slots.CoinSlot;
import io.github.lightman314.lightmanscurrency.core.ModContainers;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.TradeCostEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.items.WalletItem;
import io.github.lightman314.lightmanscurrency.tileentity.ItemTraderTileEntity;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.settings.Settings;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil.CoinValue;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;

public class ItemTraderContainer extends Container implements ITraderContainer, ITradeButtonContainer{
	
	public final PlayerEntity player;
	
	protected static final ContainerType<?> type = ModContainers.ITEMTRADER;
	
	protected final IInventory coinSlots = new Inventory(5);
	protected final IInventory itemSlots = new Inventory(3);
	//protected final IInventory tradeDisplays;
	public final ItemTraderTileEntity tileEntity;
	
	public ItemTraderContainer(int windowId, PlayerInventory inventory, ItemTraderTileEntity tileEntity)
	{
		this(ModContainers.ITEMTRADER, windowId, inventory, tileEntity);
	}
	
	protected ItemTraderContainer(ContainerType<?> type, int windowId, PlayerInventory inventory, ItemTraderTileEntity tileEntity)
	{
		super(type, windowId);
		this.tileEntity = tileEntity;
		
		this.player = inventory.player;
		
		this.tileEntity.userOpen(this.player);
		
		//int tradeCount = this.getTradeCount();
		
		//Coin Slots
		for(int x = 0; x < coinSlots.getSizeInventory(); x++)
		{
			this.addSlot(new CoinSlot(this.coinSlots, x, ItemTraderUtil.getInventoryDisplayOffset(tileEntity) + 8 + (x + 4) * 18, getCoinSlotHeight()));
		}
		
		//Item Slots
		for(int x = 0; x < itemSlots.getSizeInventory(); x++)
		{
			this.addSlot(new Slot(this.itemSlots, x, ItemTraderUtil.getInventoryDisplayOffset(tileEntity) + 8 + x * 18, getCoinSlotHeight()));
		}
		
		//Player inventory
		for(int y = 0; y < 3; y++)
		{
			for(int x = 0; x < 9; x++)
			{
				this.addSlot(new Slot(inventory, x + y * 9 + 9, ItemTraderUtil.getInventoryDisplayOffset(tileEntity) + 8 + x * 18, getPlayerInventoryStartHeight() + y * 18));
			}
		}
		//Player hotbar
		for(int x = 0; x < 9; x++)
		{
			this.addSlot(new Slot(inventory, x, ItemTraderUtil.getInventoryDisplayOffset(tileEntity) + 8 + x * 18, getPlayerInventoryStartHeight() + 58));
		}
		
		/* Item Button Rendering is now handled by the ItemTradeButton
		tradeDisplays = new Inventory(tileEntity.getTradeCount());
		UpdateTradeDisplays();
		//Trade displays
		for(int i = 0; i < tradeCount; i++)
		{
			this.addSlot(new DisplaySlot(tradeDisplays, i, ItemTraderUtil.getSlotPosX(tradeCount, i), ItemTraderUtil.getSlotPosY(tradeCount, i)));
		}*/
		
	}
	
	public int getTradeCount()
	{
		return tileEntity.getTradeCount();
	}
	
	protected int getTradeButtonBottom()
	{
		return ItemTraderUtil.getTradeDisplayHeight(this.tileEntity);
	}
	
	protected int getCoinSlotHeight()
	{
		return getTradeButtonBottom() + 19;
	}
	
	protected int getPlayerInventoryStartHeight()
	{
		return getCoinSlotHeight() + 32;
	}
	
	@Override
	public boolean canInteractWith(PlayerEntity playerIn)
	{
		return true;
	}
	
	@Override
	public void onContainerClosed(PlayerEntity playerIn)
	{
		//CurrencyMod.LOGGER.info("Closing a Trader Container");
		super.onContainerClosed(playerIn);
		this.clearContainer(playerIn,  playerIn.world,  this.coinSlots);
		this.clearContainer(playerIn, playerIn.world, this.itemSlots);
		
		this.tileEntity.userClose(this.player);
	}

	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerEntity, int index)
	{
		
		ItemStack clickedStack = ItemStack.EMPTY;
		
		Slot slot = this.inventorySlots.get(index);
		
		if(slot != null && slot.getHasStack())
		{
			ItemStack slotStack = slot.getStack();
			clickedStack = slotStack.copy();
			if(index < this.coinSlots.getSizeInventory() + this.itemSlots.getSizeInventory())
			{
				if(!this.mergeItemStack(slotStack,  this.coinSlots.getSizeInventory() + this.itemSlots.getSizeInventory(), this.inventorySlots.size(), true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if(index < this.inventorySlots.size())
			{
				if(MoneyUtil.isCoin(slotStack.getItem()))
				{
					//Merge coins into coin slots
					if(!this.mergeItemStack(slotStack, 0, this.coinSlots.getSizeInventory(), false))
					{
						return ItemStack.EMPTY;
					}
				}
				else
				{
					//Merge non-coins into item slots
					if(!this.mergeItemStack(slotStack, this.coinSlots.getSizeInventory(), this.coinSlots.getSizeInventory() + this.itemSlots.getSizeInventory(), false))
					{
						return ItemStack.EMPTY;
					}
				}
			}
			
			if(slotStack.isEmpty())
			{
				slot.putStack(ItemStack.EMPTY);
			}
			else
			{
				slot.onSlotChanged();
			}
		}
		
		return clickedStack;
		
	}
	
	public long GetCoinValue()
	{
		long value = 0;
		for(int i = 0; i < coinSlots.getSizeInventory(); i++)
		{
			value += MoneyUtil.getValue(coinSlots.getStackInSlot(i));
		}
		ItemStack wallet = LightmansCurrency.getWalletStack(this.player);
		if(!wallet.isEmpty())
		{
			value += MoneyUtil.getValue(WalletItem.getWalletInventory(wallet));
		}
		//CurrencyMod.LOGGER.info("Coin value of the open trader is " + value);
		return value;
	}
	
	public IInventory GetItemInventory() { return itemSlots; }
	
	public boolean PermissionToTrade(int tradeIndex, List<ITextComponent> denialOutput)
	{
		ItemTradeData trade = tileEntity.getTrade(tradeIndex);
		if(trade == null)
			return false;
		PreTradeEvent event = new PreTradeEvent(this.player, trade, this, () -> this.tileEntity);
		if(!event.isCanceled())
			this.tileEntity.beforeTrade(event);
		if(!event.isCanceled())
			trade.beforeTrade(event);
		if(!event.isCanceled())
			MinecraftForge.EVENT_BUS.post(event);
		
		if(denialOutput != null)
			event.getDenialReasons().forEach(reason -> denialOutput.add(reason));
		
		return !event.isCanceled();
	}
	
	public IItemTrader getTrader()
	{
		return this.tileEntity;
	}
	
	public ItemTradeData GetTrade(int tradeIndex)
	{
		return this.tileEntity.getTrade(tradeIndex);
	}
	
	public TradeCostEvent TradeCostEvent(ItemTradeData trade)
	{
		TradeCostEvent event = new TradeCostEvent(this.player, trade, this, () -> this.tileEntity);
		this.tileEntity.tradeCost(event);
		trade.tradeCost(event);
		MinecraftForge.EVENT_BUS.post(event);
		return event;
	}
	
	private void PostTradeEvent(ItemTradeData trade, CoinValue pricePaid)
	{
		//Enclose post trade event to prevent the trader owner from not getting compensated for the trade.
		try {
			PostTradeEvent event = new PostTradeEvent(this.player, trade, this, () -> this.tileEntity, pricePaid);
			this.tileEntity.afterTrade(event);
			if(event.isDirty())
			{
				this.tileEntity.markRulesDirty();
				event.clean();
			}
			trade.afterTrade(event);
			if(event.isDirty())
			{
				this.tileEntity.markTradesDirty();
				event.clean();
			}
			MinecraftForge.EVENT_BUS.post(event);
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	public void ExecuteTrade(int tradeIndex)
	{
		
		//LightmansCurrency.LOGGER.info("Executing trade at index " + tradeIndex);
		if(this.tileEntity.isRemoved())
		{
			this.player.closeScreen();
			return;
		}
		
		ItemTradeData trade = tileEntity.getTrade(tradeIndex);
		//Abort if the trade is null
		if(trade == null)
		{
			LightmansCurrency.LogError("Trade at index " + tradeIndex + " is null. Cannot execute trade!");
			return;
		}
		
		//Abort if the trade is not valid
		if(!trade.isValid())
		{
			LightmansCurrency.LogWarning("Trade at index " + tradeIndex + " is not a valid trade. Cannot execute trade.");
			return;
		}
		
		//Check if the player is allowed to do the trade
		if(!PermissionToTrade(tradeIndex, null))
			return;
		
		//Get the cost of the trade
		CoinValue price = this.TradeCostEvent(trade).getCostResult();
		
		//Process a sale
		if(trade.isSale())
		{
			//Abort if not enough items in inventory
			if(!trade.hasStock(this.tileEntity) && !this.tileEntity.getCoreSettings().isCreative())
			{
				LightmansCurrency.LogDebug("Not enough items in storage to carry out the trade at index " + tradeIndex + ". Cannot execute trade.");
				return;
			}
			
			//Abort if not enough room to put the sold item
			if(!InventoryUtil.CanPutItemStack(this.itemSlots, trade.getSellItem()))
			{
				LightmansCurrency.LogInfo("Not enough room for the output item. Aborting trade!");
				return;
			}
			
			if(!MoneyUtil.ProcessPayment(this.coinSlots, this.player, price))
			{
				LightmansCurrency.LogDebug("Not enough money is present for the trade at index " + tradeIndex + ". Cannot execute trade.");
				return;
			}
			
			//We have enough money, and the trade is valid. Execute the trade
			//Get the trade itemStack
			ItemStack giveStack = trade.getSellItem();
			//Give the trade item
			if(!InventoryUtil.PutItemStack(this.itemSlots, giveStack))//If there's not enough room to give the item to the output item, abort the trade
			{
				LightmansCurrency.LogError("Not enough room for the output item. Giving refund & aborting Trade!");
				//Give a refund
				List<ItemStack> refundCoins = MoneyUtil.getCoinsOfValue(price);
				ItemStack wallet = LightmansCurrency.getWalletStack(this.player);
				
				for(int i = 0; i < refundCoins.size(); i++)
				{
					ItemStack coins = refundCoins.get(i);
					if(!wallet.isEmpty())
					{
						coins = WalletItem.PickupCoin(wallet, coins);
					}
					if(!coins.isEmpty())
					{
						coins = InventoryUtil.TryPutItemStack(this.coinSlots, coins);
						if(!coins.isEmpty())
						{
							IInventory temp = new Inventory(1);
							temp.setInventorySlotContents(0, coins);
							this.clearContainer(this.player, this.player.world, temp);
						}
					}
				}
				return;
			}
			
			//Log the successful trade
			this.tileEntity.getLogger().AddLog(player, trade, price, this.tileEntity.getCoreSettings().isCreative());
			this.tileEntity.markLoggerDirty();
			
			//Push the post-trade event
			PostTradeEvent(trade, price);
			
			//Ignore editing internal storage if this is flagged as creative.
			if(!this.tileEntity.getCoreSettings().isCreative())
			{
				//Remove the sold items from storage
				//InventoryUtil.RemoveItemCount(this.tileEntity, trade.getSellItem());
				trade.RemoveItemsFromStorage(this.tileEntity.getStorage());
				//Give the payed cost to storage
				tileEntity.addStoredMoney(price);
			}
			
		}
		//Process a purchase
		else if(trade.isPurchase())
		{
			//Abort if not enough items in the item slots
			if(InventoryUtil.GetItemCount(this.itemSlots, trade.getSellItem()) < trade.getSellItem().getCount())
			{
				LightmansCurrency.LogDebug("Not enough items in the item slots to make the purchase.");
				return;
			}
			
			//Abort if not enough room to store the purchased items (unless we're creative)
			if(!trade.hasSpace(this.tileEntity) && !this.tileEntity.getCoreSettings().isCreative())
			{
				LightmansCurrency.LogDebug("Not enough room in storage to store the purchased items.");
				return;
			}
			//Abort if not enough money to pay them back
			if(!trade.hasStock(this.tileEntity) && !this.tileEntity.getCoreSettings().isCreative())
			{
				LightmansCurrency.LogDebug("Not enough money in storage to pay for the purchased items.");
				return;
			}
			//Passed the checks. Take the item(s) from the input slot
			InventoryUtil.RemoveItemCount(this.itemSlots, trade.getSellItem());
			//Put the payment in the purchasers wallet, coin slot, etc.
			MoneyUtil.ProcessChange(this.coinSlots, this.player, price);
			
			//Log the successful trade
			this.tileEntity.getLogger().AddLog(player, trade, price, this.tileEntity.getCoreSettings().isCreative());
			this.tileEntity.markLoggerDirty();
			
			//Push the post-trade event
			PostTradeEvent(trade, price);
			
			//Ignore editing internal storage if this is flagged as creative.
			if(!this.tileEntity.getCoreSettings().isCreative())
			{
				//Put the item in storage
				InventoryUtil.TryPutItemStack(this.tileEntity.getStorage(), trade.getSellItem());
				//Remove the coins from storage
				this.tileEntity.removeStoredMoney(price);
			}
			
		}
		//Process a barter
		else if(trade.isBarter())
		{
			//Abort if not enough items in the item slots
			if(InventoryUtil.GetItemCount(this.itemSlots, trade.getBarterItem()) < trade.getBarterItem().getCount())
			{
				LightmansCurrency.LogDebug("Not enough items in the item slots to make the barter.");
				return;
			}
			//Abort if not enough room to store the purchased items (unless we're creative)
			if(!trade.hasSpace(this.tileEntity) && !this.tileEntity.getCoreSettings().isCreative())
			{
				LightmansCurrency.LogDebug("Not enough room in storage to store the purchased items.");
				return;
			}
			//Abort if not enough items in inventory
			if(!trade.hasStock(this.tileEntity) && !this.tileEntity.getCoreSettings().isCreative())
			{
				LightmansCurrency.LogDebug("Not enough items in storage to carry out the trade at index " + tradeIndex + ". Cannot execute trade.");
				return;
			}
			
			//Passed the checks. Take the item(s) from the input slot
			InventoryUtil.RemoveItemCount(this.itemSlots, trade.getBarterItem());
			//Check if there's room for the new items
			if(!InventoryUtil.CanPutItemStack(this.itemSlots, trade.getSellItem()))
			{
				//Abort if no room for the sold item
				LightmansCurrency.LogDebug("Not enough room for the output item. Aborting trade!");
				InventoryUtil.PutItemStack(this.itemSlots, trade.getBarterItem());
				return;
			}
			//Add the new item into the the item slots
			InventoryUtil.PutItemStack(this.itemSlots, trade.getSellItem());
			
			//Log the successful trade
			this.tileEntity.getLogger().AddLog(player, trade, CoinValue.EMPTY, this.tileEntity.getCoreSettings().isCreative());
			this.tileEntity.markLoggerDirty();
			
			//Push the post-trade event
			PostTradeEvent(trade, price);
			
			//Ignore editing internal storage if this is flagged as creative.
			if(!this.tileEntity.getCoreSettings().isCreative())
			{
				//Put the item in storage
				InventoryUtil.TryPutItemStack(this.tileEntity.getStorage(), trade.getBarterItem());
				//Remove the item from storage
				trade.RemoveItemsFromStorage(this.tileEntity.getStorage());
			}
			
		}
		
	}
	
	public void CollectCoinStorage()
	{
		if(this.tileEntity.isRemoved())
		{
			this.player.closeScreen();
			return;
		}
		if(!this.tileEntity.hasPermission(this.player, Permissions.COLLECT_COINS))
		{
			Settings.PermissionWarning(this.player, "collect stored coins", Permissions.COLLECT_COINS);
			return;
		}
		List<ItemStack> coinList = MoneyUtil.getCoinsOfValue(tileEntity.getStoredMoney());
		ItemStack wallet = LightmansCurrency.getWalletStack(this.player);
		if(!wallet.isEmpty())
		{
			List<ItemStack> spareCoins = new ArrayList<>();
			for(int i = 0; i < coinList.size(); i++)
			{
				ItemStack extraCoins = WalletItem.PickupCoin(wallet, coinList.get(i));
				if(!extraCoins.isEmpty())
					spareCoins.add(extraCoins);
			}
			coinList = spareCoins;
		}
		for(int i = 0; i < coinList.size(); i++)
		{
			if(!InventoryUtil.PutItemStack(this.coinSlots, coinList.get(i)))
			{
				IInventory inventory = new Inventory(1);
				inventory.setInventorySlotContents(0, coinList.get(i));
				this.clearContainer(player, player.getEntityWorld(), inventory);
			}
		}
		//Clear the coin storage
		tileEntity.clearStoredMoney();
		
	}
	
	public void tick()
	{
		if(this.tileEntity.isRemoved())
		{
			this.player.closeScreen();
			return;
		}
	}
	
	public boolean hasPermission(String permission)
	{
		return tileEntity.hasPermission(this.player, permission);
	}
	
	public int getPermissionLevel(String permission)
	{
		return tileEntity.getPermissionLevel(this.player, permission);
	}
	
}