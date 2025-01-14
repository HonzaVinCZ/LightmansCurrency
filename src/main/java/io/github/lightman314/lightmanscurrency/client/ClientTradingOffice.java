package io.github.lightman314.lightmanscurrency.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.gui.screen.NotificationScreen;
import io.github.lightman314.lightmanscurrency.common.notifications.NotificationData;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount.AccountReference;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalTraderData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientTradingOffice {

	private static Map<UUID, UniversalTraderData> loadedTraders = new HashMap<>();
	private static Map<UUID,Team> loadedTeams = new HashMap<>();
	private static Map<UUID,BankAccount> loadedBankAccounts = new HashMap<>();
	private static NotificationData myNotifications = new NotificationData();
	private static AccountReference lastSelectedAccount = null;
	
	public static List<UniversalTraderData> getTraderList()
	{
		return loadedTraders.values().stream().collect(Collectors.toList());
	}
	
	public static UniversalTraderData getData(UUID traderID)
	{
		if(loadedTraders.containsKey(traderID))
			return loadedTraders.get(traderID);
		return null;
	}
	
	public static void clearData()
	{
		loadedTraders.forEach((id,data) -> data.onRemoved());
		loadedTraders.clear();
	}
	
	public static void updateTrader(CompoundTag compound)
	{
		UUID traderID = compound.getUUID("ID");
		if(loadedTraders.containsKey(traderID)) //Have existing trader read the data if present
			loadedTraders.get(traderID).read(compound);
		else //New trader was added, so deserialize the data and add it to the map
			loadedTraders.put(traderID, TradingOffice.Deserialize(compound).flagAsClient());
	}
	
	public static void removeTrader(UUID traderID)
	{
		if(loadedTraders.containsKey(traderID))
		{
			UniversalTraderData data = loadedTraders.get(traderID);
			loadedTraders.remove(traderID);
			data.onRemoved();
		}
			
	}
	
	public static List<Team> getTeamList()
	{
		return loadedTeams.values().stream().collect(Collectors.toList());
	}
	
	public static Team getTeam(UUID teamID)
	{
		if(loadedTeams.containsKey(teamID))
			return loadedTeams.get(teamID);
		return null;
	}
	
	public static void initTeams(List<Team> teams)
	{
		loadedTeams.clear();
		teams.forEach(team -> loadedTeams.put(team.getID(), team));
	}
	
	public static void updateTeam(CompoundTag compound)
	{
		Team updatedTeam = Team.load(compound);
		loadedTeams.put(updatedTeam.getID(), updatedTeam);
	}
	
	public static void removeTeam(UUID teamID)
	{
		if(loadedTeams.containsKey(teamID))
			loadedTeams.remove(teamID);
	}
	
	public static BankAccount getPlayerBankAccount(Player player)
	{
		return getPlayerBankAccount(player.getUUID());
	}
	
	public static BankAccount getPlayerBankAccount(UUID playerID)
	{
		if(loadedBankAccounts.containsKey(playerID))
			return loadedBankAccounts.get(playerID);
		//Return an empty account until the server notifies us of the new accounts creation.
		LightmansCurrency.LogWarning("No bank account for player with id " + playerID.toString() + " is present on the client.");
		return new BankAccount();
	}
	
	public static void initBankAccounts(Map<UUID,BankAccount> bankAccounts)
	{
		loadedBankAccounts.clear();
		bankAccounts.forEach((id,account) -> loadedBankAccounts.put(id, account));
	}
	
	public static void updateBankAccount(CompoundTag compound)
	{
		try {
			UUID owner = compound.getUUID("Player");
			BankAccount account = new BankAccount(compound);
			if(owner != null && account != null)
				loadedBankAccounts.put(owner, account);
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public static void updateNotifications(NotificationData data) {
		myNotifications = data;
		Minecraft mc = Minecraft.getInstance();
		if(mc.screen instanceof NotificationScreen)
			((NotificationScreen)mc.screen).reinit();
	}
	
	public static NotificationData getNotifications() { return myNotifications; }
	
	public static void updateLastSelectedAccount(AccountReference reference) {
		lastSelectedAccount = reference;
	}
	
	public static AccountReference getLastSelectedAccount() {
		return lastSelectedAccount;
	}
	
	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		//Reset loaded traders, teams, and bank accounts
		loadedTraders = new HashMap<>();
		loadedTeams = new HashMap<>();
		loadedBankAccounts = new HashMap<>();
		myNotifications = new NotificationData();
		lastSelectedAccount = null;
	}
	
}
