package io.github.lightman314.lightmanscurrency.trader.tradedata.rules;

import com.google.common.base.Supplier;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TradeRuleScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TimeWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.client.util.TextInputUtil;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.TradeCostEvent;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import io.github.lightman314.lightmanscurrency.util.TimeUtil;
import io.github.lightman314.lightmanscurrency.util.TimeUtil.TimeData;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TimedSale extends TradeRule {

	public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID, "timed_sale");
	
	long startTime = 0;
	boolean isActive() { return this.startTime != 0; }
	long duration = 0;
	int discount = 10;
	public int getDiscountPercent() { return this.discount; }
	public void setDiscountPercent(int percent) { this.discount = MathUtil.clamp(percent, 0, 100); }
	private double getDiscountMult() { return 1d - ((double)discount/100d); }
	private double getIncreaseMult() { return 1d + ((double)discount/100d); }
	
	public TimedSale() { super(TYPE); }
	
	@Override
	public void beforeTrade(PreTradeEvent event)
	{
		if(this.isActive() && TimeUtil.compareTime(this.duration, this.startTime))
		{
			switch(event.getTrade().getTradeDirection())
			{
			case SALE:
				event.addHelpful(Component.translatable("traderule.lightmanscurrency.timed_sale.info.sale", this.discount, this.getTimeRemaining().toString()));
				break;
			case PURCHASE:
				event.addHelpful(Component.translatable("traderule.lightmanscurrency.timed_sale.info.purchase", this.discount, this.getTimeRemaining().toString()));
				break;
				default: //Nothing if direction is NONE
			}
		}
	}
	
	@Override
	public void tradeCost(TradeCostEvent event)
	{
		if(isActive() && TimeUtil.compareTime(this.duration, this.startTime))
		{
			switch(event.getTrade().getTradeDirection())
			{
			case SALE:
				event.applyCostMultiplier(this.getDiscountMult());
				break;
			case PURCHASE:
				event.applyCostMultiplier(this.getIncreaseMult());
				break;
				default: //Nothing if direction is NONE
			}
		}
	}
	
	@Override
	public void afterTrade(PostTradeEvent event)
	{
		if(confirmStillActive())
			event.markDirty();
	}
	
	private boolean confirmStillActive()
	{
		if(!isActive())
			return false;
		else if(!TimeUtil.compareTime(this.duration, this.startTime))
		{
			this.startTime = 0;
			return true;
		}
		return false;
	}
	
	@Override
	protected CompoundTag write(CompoundTag compound) {
		
		//Write start time
		compound.putLong("startTime", this.startTime);
		//Save sale duration
		compound.putLong("duration", this.duration);
		//Save discount
		compound.putInt("discount", this.discount);
		
		return compound;
	}
	
	@Override
	public JsonObject saveToJson(JsonObject json) {
		
		json.addProperty("duration", this.duration);
		json.addProperty("discount", this.discount);
		
		return json;
	}

	@Override
	public void readNBT(CompoundTag compound) {
		
		//Load start time
		if(compound.contains("startTime", Tag.TAG_LONG))
			this.startTime = compound.getLong("startTime");
		//Load duration
		if(compound.contains("duration", Tag.TAG_LONG))
			this.duration = compound.getLong("duration");
		//Load discount
		if(compound.contains("discount", Tag.TAG_INT))
			this.discount = compound.getInt("discount");
		
	}
	
	@Override
	public void loadFromJson(JsonObject json) {
		if(json.has("duration"))
			this.duration = json.get("duration").getAsLong();
		if(json.has("discount"))
			this.discount = MathUtil.clamp(this.discount, 0, 100);
	}
	
	@Override
	public void handleUpdateMessage(CompoundTag updateInfo) {
		if(updateInfo.contains("Discount"))
		{
			this.discount = updateInfo.getInt("Discount");
		}
		else if(updateInfo.contains("Duration"))
		{
			this.duration = updateInfo.getLong("Duration");
		}
		else if(updateInfo.contains("StartSale"))
		{
			if(this.isActive() == updateInfo.getBoolean("StartSale"))
				return;
			if(this.isActive())
				this.startTime = 0;
			else
				this.startTime = TimeUtil.getCurrentTime();
		}
		
	}
	
	@Override
	public CompoundTag savePersistentData() {
		CompoundTag compound = new CompoundTag();
		compound.putLong("startTime", this.startTime);
		return compound;
	}
	@Override
	public void loadPersistentData(CompoundTag data) {
		if(data.contains("startTime", Tag.TAG_LONG))
			this.startTime = data.getLong("startTime");
	}
	
	public TimeData getTimeRemaining()
	{
		if(!isActive())
			return new TimeData(0);
		else
		{
			return new TimeData(this.startTime + this.duration - TimeUtil.getCurrentTime());
		}
	}
	
	public IconData getButtonIcon() { return IconAndButtonUtil.ICON_TIMED_SALE; }
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public TradeRule.GUIHandler createHandler(TradeRuleScreen screen, Supplier<TradeRule> rule)
	{
		return new GUIHandler(screen, rule);
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class GUIHandler extends TradeRule.GUIHandler implements TimeWidget.ITimeInput
	{
		
		protected final TimedSale getRule()
		{
			if(getRuleRaw() instanceof TimedSale)
				return (TimedSale)getRuleRaw();
			return null;
		}
		
		GUIHandler(TradeRuleScreen screen, Supplier<TradeRule> rule)
		{
			super(screen, rule);
		}
		
		EditBox discountInput;
		
		Button buttonSetDiscount;
		Button buttonStartSale;
		
		TimeWidget durationInput;
		
		@Override
		public void initTab() {
			
			
			this.discountInput = this.addCustomRenderable(new EditBox(screen.getFont(), screen.guiLeft() + 10, screen.guiTop() + 9, 20, 20, Component.empty()));
			this.discountInput.setMaxLength(2);
			this.discountInput.setValue(Integer.toString(this.getRule().discount));
			this.buttonSetDiscount = this.addCustomRenderable(new Button(screen.guiLeft() + 110, screen.guiTop() + 10, 50, 20, Component.translatable("gui.button.lightmanscurrency.discount.set"), this::PressSetDiscountButton));
			
			this.buttonStartSale = this.addCustomRenderable(new Button(screen.guiLeft() + 10, screen.guiTop() + 45, 156, 20, this.getButtonText(), this::PressStartButton));
			
			this.durationInput = this.addCustomRenderable(new TimeWidget(screen.guiLeft(), screen.guiTop() + 75, screen.getFont(), this.getRule().duration, this, this, Component.translatable("gui.widget.lightmanscurrency.timed_sale.noduration")));
			
		}
		
		@Override
		public void renderTab(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
			
			if(getRule() == null)
				return;
			
			this.screen.getFont().draw(matrixStack, Component.translatable("gui.lightmanscurrency.discount.tooltip").getString(), this.discountInput.x + this.discountInput.getWidth() + 4, this.discountInput.y + 3, 0xFFFFFF);
			
			Component infoText = Component.translatable("gui.button.lightmanscurrency.timed_sale.info.inactive", new TimeData(this.getRule().duration).toString());
			if(this.getRule().isActive())
				infoText = Component.translatable("gui.button.lightmanscurrency.timed_sale.info.active", this.getRule().getTimeRemaining().toString());
			
			this.screen.getFont().draw(matrixStack, infoText.getString(), screen.guiLeft() + 10, screen.guiTop() + 35, 0xFFFFFF);
			
			if(this.buttonStartSale.isMouseOver(mouseX, mouseY))
			{
				screen.renderTooltip(matrixStack, this.getButtonTooltip(), mouseX, mouseY);
			}
			
		}
		
		@Override
		public void onScreenTick()
		{
			this.buttonStartSale.setMessage(getButtonText());
			this.buttonStartSale.active = this.getRule().isActive() || this.getRule().duration > 0;
			TextInputUtil.whitelistInteger(this.discountInput, 0, 99);
			
		}
		
		private Component getButtonText()
		{
			return Component.translatable("gui.button.lightmanscurrency.timed_sale." + (this.getRule().isActive() ? "stop" : "start"));
		}
		
		private Component getButtonTooltip()
		{
			return Component.translatable("gui.button.lightmanscurrency.timed_sale." + (this.getRule().isActive() ? "stop" : "start") + ".tooltip");
		}
		
		@Override
		public void onTabClose() {
			
			this.removeCustomWidget(this.discountInput);
			this.removeCustomWidget(this.buttonSetDiscount);
			this.removeCustomWidget(this.buttonStartSale);
			this.durationInput.getWidgets().forEach(button -> this.removeCustomWidget(button));
			this.removeCustomWidget(this.durationInput);
		}
		
		void PressSetDiscountButton(Button button)
		{
			int discount = TextInputUtil.getIntegerValue(this.discountInput, 1);
			this.getRule().discount = discount;
			CompoundTag updateInfo = new CompoundTag();
			updateInfo.putInt("Discount", discount);
			this.screen.updateServer(TYPE, updateInfo);
		}
		
		void PressStartButton(Button button)
		{
			boolean setActive = !this.getRule().isActive();
			this.getRule().startTime = this.getRule().isActive() ? 0 : TimeUtil.getCurrentTime();
			CompoundTag updateInfo = new CompoundTag();
			updateInfo.putBoolean("StartSale", setActive);
			this.screen.updateServer(TYPE, updateInfo);
		}
		
		@Override
		public void onTimeSet(long newTime)
		{
			this.getRule().duration = newTime;
			CompoundTag updateInfo = new CompoundTag();
			updateInfo.putLong("Duration", newTime);
			this.screen.updateServer(TYPE, updateInfo);
		}
		
	}
	
	
}
