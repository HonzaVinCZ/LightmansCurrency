package io.github.lightman314.lightmanscurrency.trader.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.SettingsLogger;
import io.github.lightman314.lightmanscurrency.client.gui.settings.SettingsTab;
import io.github.lightman314.lightmanscurrency.client.gui.settings.core.*;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.teams.Team.TeamReference;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount.AccountType;
import io.github.lightman314.lightmanscurrency.money.CoinValue;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.permissions.PermissionsList;
import io.github.lightman314.lightmanscurrency.trader.permissions.options.BooleanPermission;
import io.github.lightman314.lightmanscurrency.trader.permissions.options.PermissionOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.NonNullSupplier;

public class CoreTraderSettings extends Settings{

	public static final ResourceLocation TYPE = new ResourceLocation(LightmansCurrency.MODID,"trader_core");
	
	private static final String UPDATE_CUSTOM_NAME = "customName";
	private static final String UPDATE_ADD_ALLY = "addAlly";
	private static final String UPDATE_REMOVE_ALLY = "removeAlly";
	private static final String UPDATE_ALLY_PERMISSIONS = "allyPermissions";
	private static final String UPDATE_CREATIVE = "creative";
	private static final String UPDATE_OWNERSHIP = "transferOwnership";
	private static final String UPDATE_TEAM = "changeTeam";
	private static final String UPDATE_BANK_LINK = "bankLink";
	private static final String UPDATE_NOTIFICATION = "notificationSettings";
	
	public static PermissionsList getAllyDefaultPermissions(@Nonnull ITrader trader)
	{
		Map<String,Integer> defaultPermissions = Maps.newHashMap();
		defaultPermissions.put(Permissions.OPEN_STORAGE, 1);
		defaultPermissions.put(Permissions.EDIT_TRADES, 1);
		defaultPermissions.put(Permissions.EDIT_TRADE_RULES, 1);
		defaultPermissions.put(Permissions.EDIT_SETTINGS, 1);
		defaultPermissions.put(Permissions.CHANGE_NAME, 1);
		defaultPermissions.put(Permissions.NOTIFICATION, 1);
		
		try {
			trader.getAllyDefaultPermissions().forEach((key,value) -> defaultPermissions.put(key, value));
		} catch(Exception e) { LightmansCurrency.LogError("Error getting additional default ally permissions for trader type " + trader.getClass().getName(), e); }
		
		return new PermissionsList(trader, UPDATE_ALLY_PERMISSIONS, defaultPermissions);
	}
	
	//Owner
	PlayerReference owner = null;
	private String customOwnerName = "";
	/**
	 * Used to define the owner name for persistent traders, which have no owner.
	 */
	public void setCustomOwnerName(String ownerName) { this.customOwnerName = ownerName; }
	//Team
	TeamReference team = null;
	public Team getTeam()
	{
		if(this.team == null)
			return null;
		return this.team.getTeam(this.trader.isClient());
	}
	//Ally Permissions
	List<PlayerReference> allies = Lists.newArrayList();
	PermissionsList allyPermissions = getAllyDefaultPermissions(this.trader);
	public PermissionsList getAllyPermissions() { return this.allyPermissions; }

	//Custom Name
	String customName = "";
	public boolean hasCustomName() { return !this.customName.isEmpty(); }
	public String getCustomName() { return this.customName; }
	public void forceCustomName(String customName) { this.customName = customName; }
	public CompoundTag setCustomName(Player requestor, String newName)
	{
		if(!this.hasPermission(requestor, Permissions.CHANGE_NAME))
		{
			PermissionWarning(requestor, "change the traders name", Permissions.CHANGE_NAME);
			return null;
		}
		if(!this.customName.contentEquals(newName))
		{
			String oldName = this.customName;
			this.customName = newName;
			this.logger.LogNameChange(requestor, oldName, this.customName);
			CompoundTag updateInfo = initUpdateInfo(UPDATE_CUSTOM_NAME);
			updateInfo.putString("NewName", this.customName);
			return updateInfo;
		}
		return null;
	}
	
	//Creative Mode
	boolean isCreative = false;
	
	//Bank Account Link
	boolean bankAccountLinked = false;
	public boolean canLinkBankAccount()
	{
		Team t = this.getTeam();
		if(t != null)
			return t.hasBankAccount();
		return true;
	}
	public boolean isBankAccountLinked() { return this.bankAccountLinked; }
	public boolean hasBankAccount() { return this.getBankAccount() != null; }
	public BankAccount getBankAccount() {
		if(!this.bankAccountLinked)
			return null;
		Team t = this.getTeam();
		if(t != null)
			return t.getBankAccount();
		else if(this.owner != null)
			return BankAccount.GenerateReference(this.trader.isClient(), AccountType.Player, this.owner.id).get();
		return null;
		
	}
	
	//Notifications
	private boolean notificationsEnabled = true;
	public boolean notificationsEnabled() { return this.notificationsEnabled; }
	//Chat Notifications
	private boolean chatNotifications = true;
	public boolean notificationsToChat() { return this.chatNotifications; }
	//0 for members, 1 for admins, 2 for owners only
	private int teamNotificationLevel = 2;
	public int getTeamNotificationLevel() { return this.teamNotificationLevel; }
	
	public void pushNotification(NonNullSupplier<Notification> notificationSource) {
		//Notifications are disabled
		if(!this.notificationsEnabled || this.trader.isClient())
			return;
		Team team = this.getTeam();
		if(team != null)
		{
			List<PlayerReference> sendTo = new ArrayList<>();
			if(this.teamNotificationLevel < 1)
				sendTo.addAll(team.getMembers());
			if(this.teamNotificationLevel < 2)
				sendTo.addAll(team.getAdmins());
			sendTo.add(team.getOwner());
			for(PlayerReference player : sendTo)
			{
				if(player != null && player.id != null)
				{
					TradingOffice.pushNotification(player.id, notificationSource.get(), this.chatNotifications);
				}
			}
		}
		else if(this.owner != null)
		{
			//Push to owner
			TradingOffice.pushNotification(this.owner.id, notificationSource.get(), this.chatNotifications);
		}
	}
	
	SettingsLogger logger = new SettingsLogger();
	
	public CoreTraderSettings(ITrader trader, IMarkDirty marker, BiConsumer<ResourceLocation,CompoundTag> sendToServer) { super(trader, marker, sendToServer, TYPE); }
	
	public PlayerReference getOwner() { return this.owner; }
	
	/**
	 * Returns the display name of the traders owner.
	 * If the trader is owned by a team (WIP), it will display the team name instead.
	 */
	public String getOwnerName()
	{
		if(this.getTeam() != null)
			return this.getTeam().getName();
		if(this.owner != null)
			return this.owner.lastKnownName();
		return this.customOwnerName;
	}
	
	/**
	 * Initialized the traders owner.
	 * Does nothing if the owner is already defined.
	 * Does not flag it as dirty, as it is assumed to be run during a traders initialization.
	 */
	public void initializeOwner(PlayerReference owner)
	{
		if(this.owner == null)
			this.owner = owner;
		else
			LightmansCurrency.LogWarning("Attempted to initialize the owner for a trader that is already owned.");
	}
	
	public CompoundTag setOwner(Player requestor, String newOwnerName){
		if(!this.hasPermission(requestor, Permissions.TRANSFER_OWNERSHIP))
		{
			PermissionWarning(requestor, "transfer ownership", Permissions.TRANSFER_OWNERSHIP);
			return null;
		}
		else if(this.owner == null)
		{
			Team oldTeam = this.getTeam();
			if(oldTeam != null)
				this.team = null;
			this.owner = PlayerReference.of(newOwnerName);
			if(this.owner != null && oldTeam != null)
				this.logger.LogTeamChange(requestor, this.owner, oldTeam, null);
			
			//Reset the bank link when the owner is changed
			if(this.bankAccountLinked)
				this.bankAccountLinked = false;
			
			CompoundTag updateInfo = initUpdateInfo(UPDATE_OWNERSHIP);
			updateInfo.putString("newOwner", newOwnerName);
			return updateInfo;
		}
		else
		{
			PlayerReference newOwner = PlayerReference.of(newOwnerName);
			if(newOwner != null && (!this.owner.is(newOwner) || this.team != null))
			{
				//Reset the teams owner, if setting the traders owner to a player
				Team oldTeam = this.getTeam();
					this.team = null;
				
				PlayerReference oldOwner = this.owner;
				this.owner = newOwner;
				if(oldTeam == null)
					this.logger.LogOwnerChange(requestor, oldOwner, this.owner);
				else
					this.logger.LogTeamChange(requestor, this.owner, oldTeam, null);
				
				//Reset the bank link when the owner is changed
				if(this.bankAccountLinked)
					this.bankAccountLinked = false;
				
				CompoundTag updateInfo = initUpdateInfo(UPDATE_OWNERSHIP);
				updateInfo.putString("newOwner", newOwnerName);
				return updateInfo;
			}
		}
		return null;
	}
	
	public CompoundTag setTeam(Player requestor, @Nullable UUID newTeamID)
	{
		if(!this.hasPermission(requestor, Permissions.TRANSFER_OWNERSHIP))
		{
			PermissionWarning(requestor, "transfer team ownership", Permissions.TRANSFER_OWNERSHIP);
			return null;
		}
		
		Team oldTeam = this.getTeam();
		if(oldTeam != null && this.team.getID().equals(newTeamID))
			return null;
		
		this.team = Team.referenceOf(newTeamID);
		
		Team newTeam = this.getTeam();
		
		this.logger.LogTeamChange(requestor, this.owner, oldTeam, newTeam);
		
		//Reset the bank link when the owner is changed (including moving to a team owner)
		if(this.bankAccountLinked)
			this.bankAccountLinked = false;
		
		CompoundTag updateInfo = initUpdateInfo(UPDATE_TEAM);
		if(newTeamID != null)
			updateInfo.putUUID("Team", newTeamID);
		return updateInfo;
	}
	
	public List<PlayerReference> getAllies() { return this.allies; }
	public CompoundTag addAlly(Player requestor, String newAllyName)
	{
		if(!this.hasPermission(requestor, Permissions.ADD_REMOVE_ALLIES))
		{
			PermissionWarning(requestor, "add an ally", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		if(!PlayerReference.listContains(this.allies, newAllyName))
		{
			PlayerReference newAlly = PlayerReference.of(newAllyName);
			if(newAlly != null)
			{
				this.allies.add(newAlly);
				this.logger.LogAllyChange(requestor, newAlly, true);
			}
			if(newAlly != null || requestor.level.isClientSide)
			{
				CompoundTag updateInfo = initUpdateInfo(UPDATE_ADD_ALLY);
				updateInfo.putString("AllyName", newAllyName);
				return updateInfo;
			}
		}
		return null;
	}
	public CompoundTag removeAlly(Player requestor, String removedAllyName)
	{
		if(!this.hasPermission(requestor, Permissions.ADD_REMOVE_ALLIES))
		{
			PermissionWarning(requestor, "remove an ally", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		if(PlayerReference.listContains(this.allies, removedAllyName))
		{
			PlayerReference removedAlly = null;
			for(int i = 0; i < this.allies.size() && removedAlly == null; ++i)
			{
				if(this.allies.get(i).is(removedAllyName))
				{
					removedAlly = this.allies.get(i);
					this.allies.remove(i);
				}
			}
			if(removedAlly == null)
				return null;
			this.logger.LogAllyChange(requestor, removedAlly, false);
			CompoundTag updateInfo = initUpdateInfo(UPDATE_REMOVE_ALLY);
			updateInfo.putString("AllyName", removedAllyName);
			return updateInfo;
		}
		return null;
	}
	
	public CompoundTag toggleNotifications(Player requestor)
	{
		if(!this.hasPermission(requestor, Permissions.NOTIFICATION))
		{
			PermissionWarning(requestor, "toggle notifications", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		this.notificationsEnabled = !this.notificationsEnabled;
		this.logger.LogSettingsChange(requestor, "traderNotifications", this.notificationsEnabled);
		CompoundTag updateInfo = initUpdateInfo(UPDATE_NOTIFICATION);
		updateInfo.putBoolean("nowEnabled", this.notificationsEnabled);
		return updateInfo;
	}
	
	public CompoundTag toggleChatNotifications(Player requestor)
	{
		if(!this.hasPermission(requestor, Permissions.NOTIFICATION))
		{
			PermissionWarning(requestor, "toggle chat notifications", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		this.chatNotifications = !this.chatNotifications;
		this.logger.LogSettingsChange(requestor, "traderChatNotifications", this.chatNotifications);
		CompoundTag updateInfo = initUpdateInfo(UPDATE_NOTIFICATION);
		updateInfo.putBoolean("chatEnabled", this.chatNotifications);
		return updateInfo;
	}
	
	public CompoundTag setTeamNotificationLevel(Player requestor, int newLevel) {
		
		if(!this.hasPermission(requestor, Permissions.NOTIFICATION))
		{
			PermissionWarning(requestor, "set team notification level", Permissions.ADD_REMOVE_ALLIES);
			return null;
		}
		if(this.teamNotificationLevel != newLevel)
		{
			this.teamNotificationLevel = newLevel;
			this.logger.LogSettingsChange(requestor, "teamNotifications", this.teamNotificationLevel);
		}
		CompoundTag updateInfo = initUpdateInfo(UPDATE_NOTIFICATION);
		updateInfo.putInt("newLevel", newLevel);
		return updateInfo;
	}
	
	/**
	 * Updates the last known name of all matching player references in the owner/ally/custom permissions list.
	 * @deprecated No longer needed due to updates made to how the PlayerReference data is stored.
	 */
	@Deprecated
	public void updateNames(Player player) { }
	
	public boolean hasPermission(Player player, String permission)
	{
		return getPermissionLevel(player, permission) > 0;
	}
	
	public int getPermissionLevel(Player player, String permission)
	{
		if(player == null || TradingOffice.isAdminPlayer(player))
			return Integer.MAX_VALUE;
		return this.getPermissionLevel(PlayerReference.of(player), permission);
	}
	
	public int getPermissionLevel(PlayerReference player, String permission)
	{
		if(player == null)
			return 0;
		AtomicInteger level = new AtomicInteger(0);
		if(PlayerReference.listContains(this.allies, player.id))
			level.set(this.allyPermissions.getLevel(permission));
		if(this.team != null)
		{
			Team actualTeam = this.team.getTeam(this.trader.isClient());
			if(actualTeam != null)
			{
				if(actualTeam.isAdmin(player.id))
					return Integer.MAX_VALUE;
				else if(actualTeam.isMember(player.id))
				{
					int l = this.allyPermissions.getLevel(permission);
					if(l > level.get())
						level.set(l);
				}
			}
		}
		else if(this.owner != null && this.owner.is(player))
			return Integer.MAX_VALUE;
		return level.get();
	}
	
	
	
	public boolean isCreative() { return this.isCreative; }
	public CompoundTag toggleCreative(Player requestor)
	{
		if(!TradingOffice.isAdminPlayer(requestor))
		{
			PermissionWarning(requestor, "toggle creative mode", Permissions.ADMIN_MODE);
			return null;
		}
		this.isCreative = !this.isCreative;
		this.logger.LogCreativeToggle(requestor, this.isCreative);
		CompoundTag updateInfo = initUpdateInfo(UPDATE_CREATIVE);
		updateInfo.putBoolean("isCreative", this.isCreative);
		return updateInfo;
	}
	
	/**
	 * Used to force persistent traders as creative
	 */
	public void forceCreative() { this.isCreative = true; }
	
	public CompoundTag toggleBankAccountLink(Player requestor)
	{
		if(!this.hasPermission(requestor, Permissions.BANK_LINK))
		{
			PermissionWarning(requestor, "toggle bank account link", Permissions.BANK_LINK);
			return null;
		}
		boolean sendValue = !this.bankAccountLinked;
		if(this.bankAccountLinked || this.canLinkBankAccount())
		{
			this.bankAccountLinked = !this.bankAccountLinked;
			//Push any currently stored money into the bank account.
			if(this.bankAccountLinked && this.getBankAccount() != null && this.trader.getInternalStoredMoney().getRawValue() > 0)
			{
				CoinValue money = this.trader.getInternalStoredMoney();
				this.getBankAccount().depositCoins(money);
				this.getBankAccount().LogInteraction(this.trader, money, true);
				this.trader.clearStoredMoney();
			}
			this.logger.LogSettingsChange(requestor, "bankAccountLink", this.bankAccountLinked);
		}	
		
		CompoundTag result = initUpdateInfo(UPDATE_BANK_LINK);
		result.putBoolean("IsLinked", sendValue);
		return result;
	}
	
	public SettingsLogger getLogger() { return this.logger; }
	
	
	
	@Override
	public void changeSetting(Player requestor, CompoundTag updateInfo) {
		if(this.isUpdateType(updateInfo, UPDATE_CUSTOM_NAME))
		{
			String newName = updateInfo.getString("NewName");
			CompoundTag result = this.setCustomName(requestor, newName);
			if(result != null)
				this.markDirty();
			LightmansCurrency.LogInfo("Custom Name changed to '" + newName + "' on the server.");
		}
		else if(this.isUpdateType(updateInfo, UPDATE_ADD_ALLY))
		{
			String allyName = updateInfo.getString("AllyName");
			CompoundTag result = this.addAlly(requestor, allyName);
			if(result != null)
				this.markDirty();
			LightmansCurrency.LogInfo("Attempted to add '" + allyName + "' as an ally.");
		}
		else if(this.isUpdateType(updateInfo, UPDATE_REMOVE_ALLY))
		{
			String allyName = updateInfo.getString("AllyName");
			CompoundTag result = this.removeAlly(requestor, allyName);
			if(result != null)
				this.markDirty();
			LightmansCurrency.LogInfo("Attempted to remove '" + allyName + "' from the ally list.");
		}
		else if(this.isUpdateType(updateInfo, UPDATE_CREATIVE))
		{
			boolean nowCreative = updateInfo.getBoolean("isCreative");
			if(nowCreative != this.isCreative)
			{
				CompoundTag result = this.toggleCreative(requestor);
				if(result != null)
					this.markDirty();
			}
		}
		else if(this.isUpdateType(updateInfo, UPDATE_ALLY_PERMISSIONS))
		{
			if(this.allyPermissions.changeLevel(requestor, updateInfo))
				this.markDirty();
		}
		else if(this.isUpdateType(updateInfo, UPDATE_OWNERSHIP))
		{
			String newOwnerName = updateInfo.getString("newOwner");
			CompoundTag result = this.setOwner(requestor, newOwnerName);
			if(result != null)
				this.markDirty();
		}
		else if(this.isUpdateType(updateInfo, UPDATE_TEAM))
		{
			UUID newTeamID = null;
			if(updateInfo.contains("Team"))
				newTeamID = updateInfo.getUUID("Team");
			CompoundTag result = this.setTeam(requestor, newTeamID);
			if(result != null)
				this.markDirty();
		}
		else if(this.isUpdateType(updateInfo, UPDATE_BANK_LINK))
		{
			boolean newValue = updateInfo.getBoolean("IsLinked");
			if(newValue != this.bankAccountLinked)
			{
				CompoundTag result = this.toggleBankAccountLink(requestor);
				if(result != null)
					this.markDirty();
			}
		}
		else if(this.isUpdateType(updateInfo, UPDATE_NOTIFICATION))
		{
			if(updateInfo.contains("nowEnabled"))
			{
				boolean newValue = updateInfo.getBoolean("nowEnabled");
				if(this.notificationsEnabled != newValue)
				{
					CompoundTag result = this.toggleNotifications(requestor);
					if(result != null)
						this.markDirty();
				}
			}
			if(updateInfo.contains("chatEnabled"))
			{
				boolean newValue = updateInfo.getBoolean("chatEnabled");
				if(this.chatNotifications != newValue)
				{
					CompoundTag result = this.toggleChatNotifications(requestor);
					if(result != null)
						this.markDirty();
				}
			}
			if(updateInfo.contains("newLevel"))
			{
				CompoundTag result = this.setTeamNotificationLevel(requestor, updateInfo.getInt("newLevel"));
				if(result != null)
					this.markDirty();
			}
		}
	}
	
	public CompoundTag save(CompoundTag compound)
	{
		
		this.saveOwner(compound);
		this.saveTeam(compound);
		this.saveAllyList(compound);
		this.saveAllyPermissions(compound);
		this.saveCustomName(compound);
		this.saveCreative(compound);
		this.saveBankAccountLink(compound);
		this.saveNotificationSettings(compound);
		this.saveLogger(compound);
		
		return compound;
	}
	
	public CompoundTag saveOwner(CompoundTag compound)
	{
		if(this.owner != null)
			compound.put("Owner", this.owner.save());
		if(!this.customOwnerName.isBlank())
			compound.putString("CustomOwnerName", this.customOwnerName);
		return compound;
	}
	
	public CompoundTag saveTeam(CompoundTag compound)
	{
		if(this.team != null)
			compound.putUUID("Team", this.team.getID());
		else
			compound.putBoolean("Team", false);
		return compound;
	}
	
	public CompoundTag saveAllyList(CompoundTag compound)
	{
		PlayerReference.saveList(compound, this.allies, "Allies");
		return compound;
	}
	
	public CompoundTag saveAllyPermissions(CompoundTag compound)
	{
		this.allyPermissions.save(compound, "AllyPermissions");
		return compound;
	}
	
	public CompoundTag saveCustomName(CompoundTag compound)
	{
		compound.putString("CustomName", this.customName);
		return compound;
	}
	
	public CompoundTag saveCreative(CompoundTag compound)
	{
		compound.putBoolean("Creative", this.isCreative);
		return compound;
	}
	
	public CompoundTag saveBankAccountLink(CompoundTag compound)
	{
		compound.putBoolean("BankLink", this.bankAccountLinked);
		return compound;
	}
	
	public CompoundTag saveNotificationSettings(CompoundTag compound)
	{
		compound.putBoolean("Notifications", this.notificationsEnabled);
		compound.putBoolean("ChatNotifications", this.chatNotifications);
		compound.putInt("TeamNotifications", this.teamNotificationLevel);
		return compound;
	}
	
	public CompoundTag saveLogger(CompoundTag compound)
	{
		this.logger.write(compound);
		return compound;
	}
	
	public void loadFromOldUniversalData(CompoundTag compound)
	{
		//Don't send debug message, as it triggers when the client receives a partial update packet
		//LightmansCurrency.LogInfo("Loading Core Trader Settings from old UniversalData compound.");
		//Owner
		UUID ownerID = null;
		String ownerName = "";
		if(compound.contains("OwnerID"))
			ownerID = compound.getUUID("OwnerID");
		if(compound.contains("OwnerName", Tag.TAG_STRING))
			ownerName = compound.getString("OwnerName");
		if(ownerID != null)
			this.owner = PlayerReference.of(ownerID, ownerName);
		
		//Creative
		if(compound.contains("Creative"))
			this.isCreative = compound.getBoolean("Creative");
		
		//Read allies
		if(compound.contains("Allies", Tag.TAG_LIST))
		{
			this.allies.clear();
			ListTag allyList = compound.getList("Allies", Tag.TAG_COMPOUND);
			for(int i = 0; i < allyList.size(); i++)
			{
				CompoundTag thisAlly = allyList.getCompound(i);
				if(thisAlly.contains("name", Tag.TAG_STRING))
				{
					this.addAlly(null, thisAlly.getString("name"));
				}
			}
		}
		
		//TraderName
		if(compound.contains("TraderName", Tag.TAG_STRING))
			this.customName = compound.getString("TraderName");
	}
	
	public void loadFromOldTraderData(CompoundTag compound)
	{
		LightmansCurrency.LogInfo("Loading Core Trader Settings from old TileEntity compound.");
		//Owner
		UUID ownerID = null;
		String ownerName = "";
		if(compound.contains("OwnerID"))
			ownerID = compound.getUUID("OwnerID");
		if(compound.contains("OwnerName", Tag.TAG_STRING))
			ownerName = compound.getString("OwnerName");
		if(ownerID != null)
			this.owner = PlayerReference.of(ownerID, ownerName);
		
		//Creative
		if(compound.contains("Creative"))
			this.isCreative = compound.getBoolean("Creative");
		
		//Custom Name
		if(compound.contains("CustomName", Tag.TAG_STRING))
			this.customName = compound.getString("CustomName");
		
		//Read Allies
		if(compound.contains("Allies", Tag.TAG_LIST))
		{
			this.allies.clear();
			ListTag allyList = compound.getList("Allies", Tag.TAG_COMPOUND);
			for(int i = 0; i < allyList.size(); i++)
			{
				CompoundTag thisAlly = allyList.getCompound(i);
				if(thisAlly.contains("name", Tag.TAG_STRING))
				{
					this.addAlly(null, thisAlly.getString("name"));
				}
			}
		}
		
	}
	
	public void load(CompoundTag compound)
	{
		//Owner
		if(compound.contains("Owner", Tag.TAG_COMPOUND))
			this.owner = PlayerReference.load(compound.getCompound("Owner"));
		if(compound.contains("CustomOwnerName", Tag.TAG_STRING))
			this.customOwnerName = compound.getString("CustomOwnerName");
		//Team
		if(compound.contains("Team", Tag.TAG_BYTE))
			this.team = null;
		else if(compound.contains("Team"))
			this.team = Team.referenceOf(compound.getUUID("Team"));
		//Ally List
		if(compound.contains("Allies", Tag.TAG_LIST))
			this.allies = PlayerReference.loadList(compound, "Allies");
		//Ally Permissions
		if(compound.contains("AllyPermissions", Tag.TAG_LIST))
			this.allyPermissions = PermissionsList.load(this.trader, UPDATE_ALLY_PERMISSIONS, compound, "AllyPermissions");
		
		//Custom Name
		if(compound.contains("CustomName", Tag.TAG_STRING))
			this.customName = compound.getString("CustomName");
			
		//Creative
		if(compound.contains("Creative"))
			this.isCreative = compound.getBoolean("Creative");
		
		//Bank Account Link
		if(compound.contains("BankLink"))
			this.bankAccountLinked = compound.getBoolean("BankLink");
		
		//Notification settings
		if(compound.contains("Notifications"))
			this.notificationsEnabled = compound.getBoolean("Notifications");
		if(compound.contains("ChatNotifications"))
			this.chatNotifications = compound.getBoolean("ChatNotifications");
		if(compound.contains("TeamNotifications"))
			this.teamNotificationLevel = compound.getInt("TeamNotifications");
		
		//Logger
		this.logger.read(compound);
		
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public List<SettingsTab> getSettingsTabs() {
		return Lists.newArrayList(MainTab.INSTANCE, AllyTab.INSTANCE, AllyPermissionsTab.INSTANCE, NotificationTab.INSTANCE);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public List<SettingsTab> getBackEndSettingsTabs() {
		return Lists.newArrayList(LoggerTab.INSTANCE, OwnershipTab.INSTANCE);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public List<PermissionOption> getPermissionOptions() {
		List<PermissionOption> options = Lists.newArrayList(
				BooleanPermission.of(Permissions.OPEN_STORAGE),
				BooleanPermission.of(Permissions.CHANGE_NAME),
				BooleanPermission.of(Permissions.EDIT_TRADES),
				BooleanPermission.of(Permissions.COLLECT_COINS),
				BooleanPermission.of(Permissions.STORE_COINS),
				BooleanPermission.of(Permissions.EDIT_TRADE_RULES),
				BooleanPermission.of(Permissions.EDIT_SETTINGS),
				BooleanPermission.of(Permissions.ADD_REMOVE_ALLIES),
				BooleanPermission.of(Permissions.EDIT_PERMISSIONS),
				BooleanPermission.of(Permissions.CLEAR_LOGS),
				BooleanPermission.of(Permissions.NOTIFICATION),
				BooleanPermission.of(Permissions.BANK_LINK),
				BooleanPermission.of(Permissions.BREAK_TRADER),
				BooleanPermission.of(Permissions.TRANSFER_OWNERSHIP)
			);
		if(this.trader.canInteractRemotely())
			options.add(BooleanPermission.of(Permissions.INTERACTION_LINK));
		return options;
	}
	
}
