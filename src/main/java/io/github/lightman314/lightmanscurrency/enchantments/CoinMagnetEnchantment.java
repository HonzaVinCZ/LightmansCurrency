package io.github.lightman314.lightmanscurrency.enchantments;

import java.util.List;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.CurrencySoundEvents;
import io.github.lightman314.lightmanscurrency.common.capability.WalletCapability;
import io.github.lightman314.lightmanscurrency.core.ModEnchantments;
import io.github.lightman314.lightmanscurrency.items.WalletItem;
import io.github.lightman314.lightmanscurrency.menus.wallet.WalletMenuBase;
import io.github.lightman314.lightmanscurrency.money.MoneyUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class CoinMagnetEnchantment extends WalletEnchantment {

	//Max enchantment level
	public static final int MAX_LEVEL = 3;
	//Max level to calculate range for
	public static final int MAX_CALCULATION_LEVEL = MAX_LEVEL + 2;
	
	public CoinMagnetEnchantment(Rarity rarity, EquipmentSlot... slots) {
		super(rarity, LCEnchantmentCategories.WALLET_PICKUP_CATEGORY, slots);
	}
	
	public int getMinCost(int level) { return 5 + (level - 1) * 8; }

	public int getMaxCost(int level) { return super.getMinCost(level) + 50; }

	public int getMaxLevel() { return MAX_LEVEL; }
	
	public static void runEntityTick(LivingEntity entity) {
		WalletCapability.getWalletHandler(entity).ifPresent(walletHandler ->{
			ItemStack wallet = walletHandler.getWallet();
			//Don't do anything if the stack is not a waller
			//Or if the wallet cannot pick up coins
			if(!WalletItem.isWallet(wallet) || !WalletItem.CanPickup((WalletItem)wallet.getItem()))
				return;
			//Get the level (-1 to properly calculate range)
			int enchantLevel = wallet.getEnchantmentLevel(ModEnchantments.COIN_MAGNET.get());
			//Don't do anything if the Coin Magnet enchantment is not present.
			if(enchantLevel <= 0)
				return;
			//Calculate the search radius
			float range = getCollectionRange(enchantLevel);
			Level level = entity.level;
			if(level == null)
				return;
			AABB searchBox = new AABB(entity.xo - range, entity.yo - range, entity.zo - range, entity.xo + range, entity.yo + range, entity.zo + range);
			boolean updateWallet = false;
			for(Entity e : level.getEntities(entity, searchBox, e -> e instanceof ItemEntity && MoneyUtil.isCoin(((ItemEntity)e).getItem(), false)))
			{
				ItemEntity ie = (ItemEntity)e;
				ItemStack coinStack = ie.getItem();
				ItemStack leftovers = WalletItem.PickupCoin(wallet, coinStack);
				if(leftovers.getCount() != coinStack.getCount())
				{
					updateWallet = true;
					if(leftovers.isEmpty())
						ie.discard();
					else
						ie.setItem(leftovers);
					level.playSound(null, entity, CurrencySoundEvents.COINS_CLINKING, SoundSource.PLAYERS, 0.4f, 1f);
				}
			}
			if(updateWallet)
			{
				walletHandler.setWallet(wallet);
				if(entity instanceof Player)
				{
					Player player = (Player)entity;
					if(player.containerMenu instanceof WalletMenuBase)
						((WalletMenuBase)player.containerMenu).reloadWalletContents();
				}
			}
		});
	}
	
	public static float getCollectionRange(int enchantLevel) {
		enchantLevel -= 1;
		if(enchantLevel < 0)
			return 0f;
		return Config.SERVER.coinMagnetRangeBase.get() + (Config.SERVER.coinMagnetRangeLevel.get() * Math.min(enchantLevel, MAX_CALCULATION_LEVEL - 1));
	}
	
	public static Component getCollectionRangeDisplay(int enchantLevel) {
		float range = getCollectionRange(enchantLevel);
		String display = range %1f > 0f ? String.valueOf(range) : String.valueOf(Math.round(range));
		return Component.literal(display).withStyle(ChatFormatting.GREEN);
	}
	
	@Override
	public void addWalletTooltips(List<Component> tooltips, int enchantLevel, ItemStack wallet) {
		if(wallet.getItem() instanceof WalletItem)
		{
			if(enchantLevel > 0 && WalletItem.CanPickup((WalletItem)wallet.getItem()))
			{
				tooltips.add(Component.translatable("tooltip.lightmanscurrency.wallet.pickup.magnet", getCollectionRangeDisplay(enchantLevel)).withStyle(ChatFormatting.YELLOW));
			}
		}
	}
	
}
