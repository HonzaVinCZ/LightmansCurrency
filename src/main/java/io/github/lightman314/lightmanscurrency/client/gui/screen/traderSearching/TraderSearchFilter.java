package io.github.lightman314.lightmanscurrency.client.gui.screen.traderSearching;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalTraderData;

public abstract class TraderSearchFilter {

	private static final List<TraderSearchFilter> REGISTERED_FILTERS = new ArrayList<>();
	
	public static void addFilter(TraderSearchFilter filter)
	{
		if(filter != null)
			REGISTERED_FILTERS.add(filter);
	}
	
	public static boolean checkFilters(UniversalTraderData data, String searchText)
	{
		for(int i = 0; i < REGISTERED_FILTERS.size(); i++)
		{
			if(REGISTERED_FILTERS.get(i).filter(data, searchText))
				return true;
		}
		return false;
	}
	
	public abstract boolean filter(UniversalTraderData data, String searchText);
	
}