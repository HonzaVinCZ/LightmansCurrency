package io.github.lightman314.lightmanscurrency.api;

import io.github.lightman314.lightmanscurrency.money.CoinValue;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;

public class BankAccountLogger extends TextLogger {

	public BankAccountLogger() {
		super("BankAccountHistory");
	}

	public void AddLog(ITrader trader, CoinValue amount, boolean isDeposit) {
		this.AddLog(new TranslatableComponent("log.bank", this.getTraderText(trader), this.getDepositText(isDeposit), getCostText(amount)));
	}
	
	public void AddLog(Player player, CoinValue amount, boolean isDeposit) {
		this.AddLog(new TranslatableComponent("log.bank", this.getPlayerText(player), this.getDepositText(isDeposit), getCostText(amount)));
	}
	
	public void AddLog(Player player, CoinValue amount, Component destination, boolean wasReceived) {
		this.AddLog(new TranslatableComponent("log.bank.transfer", this.getPlayerText(player), getCostText(amount), getToFromText(wasReceived), destination));
	}
	
	protected final Component getToFromText(boolean wasReceived) {
		return new TranslatableComponent("log.bank.transfer." + (wasReceived ? "from" : "to"));
	}
	
	protected final Component getDepositText(boolean isDeposit) {
		return new TranslatableComponent("log.bank." + (isDeposit ? "deposit" : "withdraw"));
	}
	
	protected final Component getTraderText(ITrader trader) {
		return new TextComponent(trader.getName().getString()).withStyle(ChatFormatting.YELLOW);
	}
	
	protected final Component getPlayerText(Player player) {
		return new TextComponent(player.getDisplayName().getString()).withStyle(ChatFormatting.GREEN);
	}
	
}