package io.github.lightman314.lightmanscurrency.trader.tradedata.restrictions;

import java.util.Map;

import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.trader.common.TraderItemStorage;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemTradeRestriction {

	public static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation(LightmansCurrency.MODID, "items/empty_item_slot");
	public static final Pair<ResourceLocation,ResourceLocation> BACKGROUND = Pair.of(InventoryMenu.BLOCK_ATLAS, DEFAULT_BACKGROUND);
	
	static Map<ResourceLocation,ItemTradeRestriction> ITEM_TRADE_RESTRICTIONS;
	
	public static final ItemTradeRestriction NONE = new ItemTradeRestriction();
	
	public ItemTradeRestriction() { }
	
	public ItemStack modifySellItem(ItemStack sellItem, String customName, ItemTradeData trade) { return sellItem; }
	
	public boolean allowSellItem(ItemStack itemStack) { return true; }
	
	public ItemStack filterSellItem(ItemStack itemStack) { return itemStack; }
	
	public boolean allowItemSelectItem(ItemStack itemStack) { return true; }
	
	public boolean allowExtraItemInStorage(ItemStack itemStack) { return false; }
	
	public int getSaleStock(TraderItemStorage traderStorage, ItemStack... sellItemList) {
		int minStock = Integer.MAX_VALUE;
		for(ItemStack sellItem : InventoryUtil.combineQueryItems(sellItemList))
			minStock = Math.min(this.getItemStock(sellItem, traderStorage), minStock);
		return minStock;
	}
	
	protected final int getItemStock(ItemStack sellItem, TraderItemStorage traderStorage)
	{
		if(sellItem.isEmpty())
			return Integer.MAX_VALUE;
		return traderStorage.getItemCount(sellItem) / sellItem.getCount();
	}
	
	public void removeItemsFromStorage(TraderItemStorage traderStorage, ItemStack... sellItemList)
	{
		for(ItemStack sellItem : sellItemList)
			this.removeFromStorage(sellItem, traderStorage);
	}
	
	protected final void removeFromStorage(ItemStack sellItem, TraderItemStorage traderStorage)
	{
		if(sellItem.isEmpty())
			return;
		traderStorage.removeItem(sellItem);
	}
	
	@OnlyIn(Dist.CLIENT)
	public Pair<ResourceLocation,ResourceLocation> getEmptySlotBG() { return BACKGROUND; }
	
}
