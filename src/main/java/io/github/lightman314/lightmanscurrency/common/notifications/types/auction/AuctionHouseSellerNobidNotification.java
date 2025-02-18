package io.github.lightman314.lightmanscurrency.common.notifications.types.auction;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.notifications.types.ItemTradeNotification.ItemData;
import io.github.lightman314.lightmanscurrency.trader.tradedata.AuctionTradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class AuctionHouseSellerNobidNotification extends AuctionHouseNotification{

	public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID, "auction_house_seller_nobid");
	
	List<ItemData> items;
	
	public AuctionHouseSellerNobidNotification(AuctionTradeData trade) {
		
		this.items = new ArrayList<>();
		for(int i = 0; i < trade.getAuctionItems().size(); ++i)
			this.items.add(new ItemData(trade.getAuctionItems().get(i)));
		
	}
	
	public AuctionHouseSellerNobidNotification(CompoundTag compound) { this.load(compound); }
	
	@Override
	protected ResourceLocation getType() { return TYPE; }

	@Override
	public MutableComponent getMessage() {
		
		Component itemText = getItemNames(this.items);
		
		//Create log from stored data
		return Component.translatable("notifications.message.auction.seller.nobid", itemText);
		
	}

	@Override
	protected void saveAdditional(CompoundTag compound) {
		
		ListTag itemList = new ListTag();
		for(ItemData item : this.items)
			itemList.add(item.save());
		compound.put("Items", itemList);
		
	}

	@Override
	protected void loadAdditional(CompoundTag compound) {
		
		ListTag itemList = compound.getList("Items", Tag.TAG_COMPOUND);
		this.items = new ArrayList<>();
		for(int i = 0; i < itemList.size(); ++i)
			this.items.add(new ItemData(itemList.getCompound(i)));
		
	}
	
}
