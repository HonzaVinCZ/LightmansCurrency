package io.github.lightman314.lightmanscurrency.common.universal_traders.traderSearching;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalItemTraderData;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalTraderData;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ItemTraderSearchFilter extends TraderSearchFilter{

	@Override
	public boolean filter(UniversalTraderData data, String searchText) {
		
		//Search the items being sold
		if(data instanceof UniversalItemTraderData)
		{
			List<ItemTradeData> trades = ((UniversalItemTraderData)data).getAllTrades();
			for(int i = 0; i < trades.size(); i++)
			{
				if(trades.get(i).isValid())
				{
					ItemStack sellItem = trades.get(i).getSellItem(0);
					ItemStack sellItem2 = trades.get(i).getSellItem(1);
					//Search item name
					if(sellItem.getHoverName().getString().toLowerCase().contains(searchText))
						return true;
					if(sellItem2.getHoverName().getString().toLowerCase().contains(searchText))
						return true;
					//Search custom name
					if(trades.get(i).getCustomName(0).toLowerCase().contains(searchText))
						return true;
					if(trades.get(i).getCustomName(1).toLowerCase().contains(searchText))
						return true;
					//Search enchantments
					AtomicBoolean foundEnchantment = new AtomicBoolean(false);
					EnchantmentHelper.getEnchantments(sellItem).forEach((enchantment, level) ->{
						if(enchantment.getFullname(level).getString().toLowerCase().contains(searchText))
							foundEnchantment.set(true);
					});
					EnchantmentHelper.getEnchantments(sellItem2).forEach((enchantment, level) ->{
						if(enchantment.getFullname(level).getString().toLowerCase().contains(searchText))
							foundEnchantment.set(true);
					});
					if(foundEnchantment.get())
						return true;
					
					//Check the barter item if applicable
					if(trades.get(i).isBarter())
					{
						ItemStack barterItem = trades.get(i).getBarterItem(0);
						ItemStack barterItem2 = trades.get(i).getBarterItem(1);
						//Search item name
						if(barterItem.getHoverName().getString().toLowerCase().contains(searchText))
							return true;
						if(barterItem2.getHoverName().getString().toLowerCase().contains(searchText))
							return true;
						//Search enchantments
						foundEnchantment.set(false);
						EnchantmentHelper.getEnchantments(barterItem).forEach((enchantment, level) ->{
							if(enchantment.getFullname(level).getString().toLowerCase().contains(searchText))
								foundEnchantment.set(true);
						});
						if(foundEnchantment.get())
							return true;
					}
				}
			}
		}
		return false;
	}

}
