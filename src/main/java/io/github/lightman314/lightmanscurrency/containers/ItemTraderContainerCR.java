package io.github.lightman314.lightmanscurrency.containers;

import io.github.lightman314.lightmanscurrency.containers.interfaces.ITraderCashRegisterContainer;
import io.github.lightman314.lightmanscurrency.core.ModContainers;
import io.github.lightman314.lightmanscurrency.tileentity.CashRegisterTileEntity;
import io.github.lightman314.lightmanscurrency.tileentity.ItemTraderTileEntity;
import net.minecraft.entity.player.PlayerInventory;

public class ItemTraderContainerCR extends ItemTraderContainer implements ITraderCashRegisterContainer{
	
	public CashRegisterTileEntity cashRegister;
	
	public ItemTraderContainerCR(int windowId, PlayerInventory inventory, ItemTraderTileEntity tileEntity, CashRegisterTileEntity cashRegister)
	{
		
		super(ModContainers.ITEMTRADERCR, windowId, inventory, tileEntity);
		this.cashRegister = cashRegister;
		
	}
	
	public int getThisIndex()
	{
		return this.cashRegister.getTraderIndex(this.tileEntity);
	}
	
	public int getTotalCount()
	{
		return this.cashRegister.getPairedTraderSize();
	}
	
	public void OpenNextContainer(int direction)
	{
		this.cashRegister.OpenContainer(getThisIndex(), getThisIndex() + direction, direction, this.player);
	}
	
	public void OpenContainerIndex(int index)
	{
		int previousIndex = index - 1;
		if(previousIndex  < 0)
			previousIndex = this.cashRegister.getPairedTraderSize() - 1;
		this.cashRegister.OpenContainer(previousIndex, index, 1, this.player);
	}
	
}