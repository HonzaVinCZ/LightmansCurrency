package io.github.lightman314.lightmanscurrency.trader.tradedata.rules;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TradeRuleScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollTextDisplay;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PlayerWhitelist extends TradeRule{
	
	public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID, "whitelist");
	
	List<PlayerReference> whitelistedPlayers = new ArrayList<>();
	
	public PlayerWhitelist() { super(TYPE); }
	
	@Override
	public void beforeTrade(PreTradeEvent event) {
		
		if(!this.isWhitelisted(event.getPlayerReference()))
			event.addDenial(Component.translatable("traderule.lightmanscurrency.whitelist.denial"));
		else
			event.addHelpful(Component.translatable("traderule.lightmanscurrency.whitelist.allowed"));
		
	}
	
	public boolean isWhitelisted(PlayerReference player)
	{
		for(int i = 0; i < this.whitelistedPlayers.size(); ++i)
		{
			if(this.whitelistedPlayers.get(i).is(player))
				return true;
		}
		return false;
	}

	@Override
	public CompoundTag write(CompoundTag compound) {
		//Save player names
		ListTag playerNameList = new ListTag();
		for(int i = 0; i < this.whitelistedPlayers.size(); i++)
		{
			playerNameList.add(this.whitelistedPlayers.get(i).save());
		}
		compound.put("WhitelistedPlayers", playerNameList);
		
		return compound;
	}
	
	@Override
	public JsonObject saveToJson(JsonObject json) {
		JsonArray whitelist = new JsonArray();
		for(int i = 0; i < this.whitelistedPlayers.size(); ++i)
		{
			whitelist.add(this.whitelistedPlayers.get(i).saveAsJson());
		}
		json.add("WhitelistedPlayers", whitelist);
		return json;
	}

	@Override
	public void readNBT(CompoundTag compound) {
		
		//Load whitelisted players
		if(compound.contains("WhitelistedPlayers", Tag.TAG_LIST))
		{
			this.whitelistedPlayers.clear();
			ListTag playerList = compound.getList("WhitelistedPlayers", Tag.TAG_COMPOUND);
			for(int i = 0; i < playerList.size(); ++i)
			{
				PlayerReference reference = PlayerReference.load(playerList.getCompound(i));
				if(reference != null)
					this.whitelistedPlayers.add(reference);
			}
		}
		//Load player names (old method) and convert them to player references
		if(compound.contains("WhitelistedPlayersNames", Tag.TAG_LIST))
		{
			this.whitelistedPlayers.clear();
			ListTag playerNameList = compound.getList("WhitelistedPlayersNames", Tag.TAG_COMPOUND);
			for(int i = 0; i < playerNameList.size(); i++)
			{
				CompoundTag thisCompound = playerNameList.getCompound(i);
				if(thisCompound.contains("name", Tag.TAG_STRING))
				{
					PlayerReference reference = PlayerReference.of(thisCompound.getString("name"));
					if(reference != null && !this.isWhitelisted(reference))
						this.whitelistedPlayers.add(reference);
				}
					
			}
		}
		
	}
	
	@Override
	public void loadFromJson(JsonObject json) {
		if(json.has("WhitelistedPlayers"))
		{
			this.whitelistedPlayers.clear();
			JsonArray whitelist = json.get("WhitelistedPlayers").getAsJsonArray();
			for(int i = 0; i < whitelist.size(); ++i) {
				PlayerReference reference = PlayerReference.load(whitelist.get(i).getAsJsonObject());
				if(reference != null && !this.isWhitelisted(reference))
					this.whitelistedPlayers.add(reference);
			}
		}
	}
	
	@Override
	public void handleUpdateMessage(CompoundTag updateInfo)
	{
		boolean add = updateInfo.getBoolean("Add");
		String name = updateInfo.getString("Name");
		PlayerReference player = PlayerReference.of(name);
		if(player == null)
			return;
		if(add && !this.isWhitelisted(player))
		{
			this.whitelistedPlayers.add(player);
		}
		else if(!add && this.isWhitelisted(player))
		{
			PlayerReference.removeFromList(this.whitelistedPlayers, name);
		}
	}
	
	@Override
	public CompoundTag savePersistentData() { return null; }
	@Override
	public void loadPersistentData(CompoundTag data) { }
	
	public IconData getButtonIcon() { return IconAndButtonUtil.ICON_WHITELIST; }

	@Override
	@OnlyIn(Dist.CLIENT)
	public TradeRule.GUIHandler createHandler(TradeRuleScreen screen, Supplier<TradeRule> rule)
	{
		return new GUIHandler(screen, rule);
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class GUIHandler extends TradeRule.GUIHandler
	{
		
		protected final PlayerWhitelist getWhitelistRule()
		{
			if(getRuleRaw() instanceof PlayerWhitelist)
				return (PlayerWhitelist)getRuleRaw();
			return null;
		}
		
		GUIHandler(TradeRuleScreen screen, Supplier<TradeRule> rule)
		{
			super(screen, rule);
		}
		
		EditBox nameInput;
		
		Button buttonAddPlayer;
		Button buttonRemovePlayer;
		
		ScrollTextDisplay playerDisplay;
		
		@Override
		public void initTab() {
			
			this.nameInput = this.addCustomRenderable(new EditBox(screen.getFont(), screen.guiLeft() + 10, screen.guiTop() + 9, screen.xSize - 20, 20, Component.empty()));
			
			this.buttonAddPlayer = this.screen.addCustomRenderable(new Button(screen.guiLeft() + 10, screen.guiTop() + 30, 78, 20, Component.translatable("gui.button.lightmanscurrency.whitelist.add"), this::PressWhitelistButton));
			this.buttonRemovePlayer = this.screen.addCustomRenderable(new Button(screen.guiLeft() + screen.xSize - 88, screen.guiTop() + 30, 78, 20, Component.translatable("gui.button.lightmanscurrency.whitelist.remove"), this::PressForgetButton));
			
			//Player list display
			this.playerDisplay = this.screen.addCustomRenderable(new ScrollTextDisplay(screen.guiLeft() + 7, screen.guiTop() + 55, this.screen.xSize - 14, 114, this.screen.getFont(), this::getWhitelistedPlayers));
			this.playerDisplay.setColumnCount(2);
			
		}
		
		private List<Component> getWhitelistedPlayers()
		{
			List<Component> playerList = Lists.newArrayList();
			if(getWhitelistRule() == null)
				return playerList;
			for(PlayerReference player : getWhitelistRule().whitelistedPlayers)
				playerList.add(player.lastKnownNameComponent());
			return playerList;
		}
		
		@Override
		public void renderTab(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) { }
		
		@Override
		public void onTabClose() {
			
			this.removeCustomWidget(this.nameInput);
			this.removeCustomWidget(this.buttonAddPlayer);
			this.removeCustomWidget(this.buttonRemovePlayer);
			this.removeCustomWidget(this.playerDisplay);
			
		}
		
		void PressWhitelistButton(Button button)
		{
			String name = nameInput.getValue();
			if(name != "")
			{
				nameInput.setValue("");
				PlayerReference reference = PlayerReference.of(name);
				if(reference != null)
				{
					if(!getWhitelistRule().isWhitelisted(reference))
					{
						getWhitelistRule().whitelistedPlayers.add(reference);
					}
				}
				CompoundTag updateInfo = new CompoundTag();
				updateInfo.putBoolean("Add", true);
				updateInfo.putString("Name", name);
				this.screen.updateServer(TYPE, updateInfo);
			}
		}
		
		void PressForgetButton(Button button)
		{
			String name = nameInput.getValue();
			if(name != "")
			{
				nameInput.setValue("");
				PlayerReference reference = PlayerReference.of(name);
				if(reference != null)
				{
					if(getWhitelistRule().isWhitelisted(reference))
					{
						boolean notFound = true;
						for(int i = 0; notFound && i < getWhitelistRule().whitelistedPlayers.size(); ++i)
						{
							if(getWhitelistRule().whitelistedPlayers.get(i).is(reference))
							{
								notFound = false;
								getWhitelistRule().whitelistedPlayers.remove(i);
							}
						}
					}
				}
				CompoundTag updateInfo = new CompoundTag();
				updateInfo.putBoolean("Add", false);
				updateInfo.putString("Name", name);
				this.screen.updateServer(TYPE, updateInfo);
			}
			
		}
		
	}
	
}
