package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderStorageScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.CoinValueInput;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ItemEditWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ItemEditWidget.IItemEditListener;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollBarWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TradeButtonArea.InteractionConsumer;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton.ITradeData;
import io.github.lightman314.lightmanscurrency.menus.traderstorage.TraderStorageClientTab;
import io.github.lightman314.lightmanscurrency.menus.traderstorage.item.ItemTradeEditTab;
import io.github.lightman314.lightmanscurrency.money.CoinValue;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class ItemTradeEditClientTab extends TraderStorageClientTab<ItemTradeEditTab> implements InteractionConsumer, IItemEditListener {

	private static final int X_OFFSET = 13;
	private static final int Y_OFFSET = 71;
	private static final int COLUMNS = 10;
	private static final int ROWS = 2;
	
	public ItemTradeEditClientTab(TraderStorageScreen screen, ItemTradeEditTab commonTab) {
		super(screen, commonTab); 
	}
	
	@Override
	public IconData getIcon() { return IconData.BLANK; }

	@Override
	public MutableComponent getTooltip() { return Component.empty(); }

	@Override
	public boolean tabButtonVisible() { return false; }
	
	@Override
	public boolean blockInventoryClosing() { return true; }
	
	@Override
	public int getTradeRuleTradeIndex() { return this.commonTab.getTradeIndex(); }

	TradeButton tradeDisplay;
	CoinValueInput priceSelection;
	EditBox customNameInput;
	
	ItemEditWidget itemEdit;
	ScrollBarWidget itemEditScroll;
	
	Button buttonToggleTradeType;
	
	private int selection = -1;
	private int itemEditScrollValue = -1;
	
	@Override
	public void onOpen() {
		
		ItemTradeData trade = this.getTrade();
		
		this.tradeDisplay = this.screen.addRenderableTabWidget(new TradeButton(this.menu::getContext, this.commonTab::getTrade, button -> {}));
		this.tradeDisplay.move(this.screen.getGuiLeft() + 10, this.screen.getGuiTop() + 18);
		this.priceSelection = this.screen.addRenderableTabWidget(new CoinValueInput(this.screen.getGuiLeft() + TraderScreen.WIDTH / 2 - CoinValueInput.DISPLAY_WIDTH / 2, this.screen.getGuiTop() + 40, Component.empty(), trade == null ? CoinValue.EMPTY : trade.getCost(), this.font, this::onValueChanged, this.screen::addRenderableTabWidget));
		this.priceSelection.drawBG = false;
		this.priceSelection.init();
		
		this.itemEdit = this.screen.addRenderableTabWidget(new ItemEditWidget(this.screen.getGuiLeft() + X_OFFSET, this.screen.getGuiTop() + Y_OFFSET, COLUMNS, ROWS, this));
		this.itemEdit.init(this.screen::addRenderableTabWidget, this.screen::addTabListener);
		if(this.itemEditScrollValue >= 0)
			this.itemEdit.setScroll(itemEditScrollValue);
		
		this.itemEditScroll = this.screen.addRenderableTabWidget(new ScrollBarWidget(this.screen.getGuiLeft() + X_OFFSET + 18 * COLUMNS, this.screen.getGuiTop() + Y_OFFSET, 18 * ROWS, this.itemEdit));
		this.itemEditScroll.smallKnob = true;
		
		int labelWidth = this.font.width(Component.translatable("gui.lightmanscurrency.customname"));
		this.customNameInput = this.screen.addRenderableTabWidget(new EditBox(this.font, this.screen.getGuiLeft() + 15 + labelWidth, this.screen.getGuiTop() + 38, this.screen.getXSize() - 28 - labelWidth, 18, Component.empty()));
		if(this.selection >= 0 && this.selection < 2 && trade != null)
			this.customNameInput.setValue(trade.getCustomName(this.selection));
		
		this.buttonToggleTradeType = this.screen.addRenderableTabWidget(new Button(this.screen.getGuiLeft() + 113, this.screen.getGuiTop() + 15, 80, 20, Component.empty(), this::ToggleTradeType));
		
		
		
	}
	
	@Override
	public void onClose() { this.selection = -1; this.itemEditScrollValue = -1; }

	@Override
	public void renderBG(PoseStack pose, int mouseX, int mouseY, float partialTicks) {
		
		if(this.getTrade() == null)
			return;
		
		this.validateRenderables();
		
		if(this.itemEditScroll.visible)
			this.itemEditScroll.beforeWidgetRender(mouseY);
		
		//Render a down arrow over the selected position
		RenderSystem.setShaderTexture(0, TraderScreen.GUI_TEXTURE);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		
		this.screen.blit(pose, this.getArrowPosition(), this.screen.getGuiTop() + 10, TraderScreen.WIDTH + 8, 18, 8, 6);
		
		if(this.customNameInput.visible)
			this.font.draw(pose, Component.translatable("gui.lightmanscurrency.customname"), this.screen.getGuiLeft() + 13, this.screen.getGuiTop() + 42, 0x404040);
		
	}
	
	private int getArrowPosition() {
		
		ItemTradeData trade = this.getTrade();
		if(this.selection == -1)
		{
			if(trade.isSale())
				return this.screen.getGuiLeft() + 25;
			if(trade.isPurchase())
				return this.screen.getGuiLeft() + 81;
			else
				return -100;
		}
		else
		{
			if(this.selection >= 2 && !trade.isBarter())
				return -100;
			int horizSlot = this.selection;
			if(trade.isSale() || trade.isBarter())
				horizSlot += 2;
			int spacing = horizSlot % 4 >= 2 ? 20 : 0;
			return this.screen.getGuiLeft() + 16 + (18 * (horizSlot % 4)) + spacing;
		}
	}
	
	private void validateRenderables() {
		
		this.priceSelection.visible = this.selection < 0 && !this.getTrade().isBarter();
		if(this.priceSelection.visible)
			this.priceSelection.tick();
		this.itemEditScroll.visible = this.itemEdit.visible = this.selection >= 0 && (this.getTrade().isBarter() ? this.selection < 4 : this.selection < 2);
		this.customNameInput.visible = this.selection >= 0 && this.selection < 2 && !this.getTrade().isPurchase();
		if(this.customNameInput.visible && !this.customNameInput.getValue().contentEquals(this.getTrade().getCustomName(this.selection)))
			this.commonTab.setCustomName(this.selection, this.customNameInput.getValue());
		this.buttonToggleTradeType.setMessage(Component.translatable("gui.button.lightmanscurrency.tradedirection." + this.getTrade().getTradeType().name().toLowerCase()));
	}
	
	@Override
	public void tick() {
		if(this.customNameInput.visible)
			this.customNameInput.tick();
		if(this.itemEdit.visible)
		{
			this.itemEdit.tick();
			this.itemEditScrollValue = this.itemEdit.currentScroll();
		}
	}

	@Override
	public void renderTooltips(PoseStack pose, int mouseX, int mouseY) {
		
		this.tradeDisplay.renderTooltips(pose, mouseX, mouseY);
		
		if(this.selection >= 0)
			this.itemEdit.renderTooltips(this.screen, pose, mouseX, mouseY);
		
	}
	
	@Override
	public void receiveSelfMessage(CompoundTag message) {
		if(message.contains("TradeIndex"))
			this.commonTab.setTradeIndex(message.getInt("TradeIndex"));
		if(message.contains("StartingSlot"))
			this.selection = message.getInt("StartingSlot");
	}

	@Override
	public void onTradeButtonInputInteraction(ITrader trader, ITradeData trade, int index, int mouseButton) {
		if(trade instanceof ItemTradeData)
		{
			ItemTradeData t = (ItemTradeData)trade;
			if(t.isSale())
				this.changeSelection(-1);
			else if(t.isPurchase())
				this.changeSelection(index);
			else if(t.isBarter())
				this.changeSelection(index + 2);
		}
	}

	@Override
	public void onTradeButtonOutputInteraction(ITrader trader, ITradeData trade, int index, int mouseButton) {
		if(trade instanceof ItemTradeData)
		{
			ItemTradeData t = (ItemTradeData)trade;
			if(t.isSale() || t.isBarter())
				this.changeSelection(index);
			else if(t.isPurchase())
				this.changeSelection(-1);
		}
	}
	
	private void changeSelection(int newSelection) {
		this.selection = newSelection;
		if(this.selection == -1)
			this.priceSelection.setCoinValue(this.getTrade().getCost());
		if(this.selection >= 0 && this.selection < 2)
			this.customNameInput.setValue(this.commonTab.getTrade().getCustomName(this.selection));
		if(this.selection >= 0)
			this.itemEdit.refreshSearch();
	}

	@Override
	public void onTradeButtonInteraction(ITrader trader, ITradeData trade, int localMouseX, int localMouseY, int mouseButton) { }
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.tradeDisplay.onInteractionClick((int)mouseX, (int)mouseY, button, this);
		this.itemEditScroll.onMouseClicked(mouseX, mouseY, button);
		return false;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.itemEditScroll.onMouseReleased(mouseX, mouseY, button);
		return false;
	}

	public void onValueChanged(CoinValue value) { this.commonTab.setPrice(value.copy()); }

	@Override
	public ItemTradeData getTrade() { return this.commonTab.getTrade(); }

	@Override
	public boolean restrictItemEditItems() { return this.selection < 2; }

	@Override
	public void onItemClicked(ItemStack item) { this.commonTab.setSelectedItem(this.selection, item); }
	
	private void ToggleTradeType(Button button) {
		if(this.getTrade() != null)
			this.commonTab.setType(this.getTrade().getTradeType().next());
	}
	
}
