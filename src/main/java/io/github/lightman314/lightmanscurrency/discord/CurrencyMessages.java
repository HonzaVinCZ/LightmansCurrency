package io.github.lightman314.lightmanscurrency.discord;

import io.github.lightman314.lightmansconsole.events.CreateMessageEntriesEvent;
import io.github.lightman314.lightmansconsole.message.MessageManager.MessageEntry;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CurrencyMessages {
	
	//Lightman's Currency !help
	public static final MessageEntry M_HELP_LC_NOTIFICATIONS = MessageEntry.create("help_lc_notifications", "Help message for lightman's currency !notifications.", "Handle private currency notifications.");
	public static final MessageEntry M_HELP_LC_SEARCH1 = MessageEntry.create("help_lc_search1", "Help message for lightman's currency !search <sales|purchases|barters|trades>.", "List all universal trades selling items containing the searchText.");
	public static final MessageEntry M_HELP_LC_SEARCH2 = MessageEntry.create("help_lc_search2", "Help message for lightman's currency !search <players|shops>.", "List all trades for universal traders with player/shop names containing the searchText. Leave searchText empty to see all traders trades.");
	public static final MessageEntry M_HELP_LC_SEARCH3 = MessageEntry.create("help_lc_search3", "Help message for lightman's currency !search <all>.", "List all trades.");
	
	//Lightman's Currency Bot
	//!notifications help
	public static final MessageEntry M_NOTIFICATIONS_ENABLED = MessageEntry.create("command_notifications_enabled", "Message sent when running !messages help while notifications are enabled.", "Personal notifications are enabled.");
	public static final MessageEntry M_NOTIFICATIONS_DISABLED = MessageEntry.create("command_notifications_disabled", "Message sent when running !messages help while notifications are disabled.", "Personal notifications are disabled.");
	public static final MessageEntry M_NOTIFICATIONS_NOTLINKED = MessageEntry.create("command_notifications_not_linked", "Message sent when running !messages help when their account is not linked.", "Your account must be linked in order to set your notification preferences.");
	public static final MessageEntry M_NOTIFICATIONS_HELP = MessageEntry.create("command_notifications_help", "Remaining message sent when running !messages help.", "If personal notifications are enabled you will receive copies of your in-game notifications via Discord PM.");
	//!notifications enable
	public static final MessageEntry M_NOTIFICATIONS_ENABLE_SUCCESS = MessageEntry.create("command_notifications_enable_successs", "Message sent when running !messages enable successfully.", "Personal notifications are now enabled.");
	public static final MessageEntry M_NOTIFICATIONS_ENABLE_FAIL = MessageEntry.create("command_notifications_enable_fail", "Message sent when failing to run !messages enable.", "Personal notifications were already enabled.");
	//!notifications disable
	public static final MessageEntry M_NOTIFICATIONS_DISABLE_SUCCESS = MessageEntry.create("command_notifications_disable_successs", "Message sent when running !messages disable successfully.", "Personal notifications are now disabled.");
	public static final MessageEntry M_NOTIFICATIONS_DISABLE_FAIL = MessageEntry.create("command_notifications_disable_fail", "Message sent when failing to run !messages disable.", "Personal notifications were already disabled.");
	
	//!search
	public static final MessageEntry M_SEARCH_BAD_INPUT = MessageEntry.create("command_search_badinput", "Message sent when !search is run with an invalid sub-command (sales,purchases,players, etc.).", "Invalid search type.");
	public static final MessageEntry M_SEARCH_NORESULTS = MessageEntry.create("command_search_noresults", "Message sent when !search is run and no search results were found.", "No results found.");
	
	//Trade Notification
	public static final MessageEntry M_NOTIFICATION_OUTOFSTOCK = MessageEntry.create("lightmanscurrency_notification_outofstock", "Message added to the end of a trade notification informing you that your trade is out of stock.", "**This trade is now out of stock!**");
	
	//Trader Announcement
	public static final MessageEntry M_NEWTRADER = MessageEntry.create("lightmanscurrency_newtrader", "Announcement made in the currency bot channel when a new universal trader has been made.\n{player} for the traders owner name.", "{player} has made a new Trading Server!", "player");
	public static final MessageEntry M_NEWTRADER_NAMED = MessageEntry.create("lightmanscurrency_newtrader_named", "Announcement made in the currency bot channel when a new universal trader with a custom name has been made.\n{player} for the traders owner name.\n{trader} for the traders custom name.", "{player} has made a new Trading Server '{trader}'!", "player", "trader");
	
	@SubscribeEvent
	public static void confirmMessageInit(CreateMessageEntriesEvent event) 
	{
		//Don't need to do anything, as just subscribing to the event will cause the static values to be loaded.
	}
	
}
