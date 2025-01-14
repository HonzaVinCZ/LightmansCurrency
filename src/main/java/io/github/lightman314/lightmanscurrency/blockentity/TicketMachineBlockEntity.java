package io.github.lightman314.lightmanscurrency.blockentity;

import io.github.lightman314.lightmanscurrency.core.ModBlockEntities;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TicketMachineBlockEntity extends BlockEntity{

	Container storage = new SimpleContainer(2);
	public Container getStorage() { return this.storage; }
	
	public TicketMachineBlockEntity(BlockPos pos, BlockState state)
	{
		super(ModBlockEntities.TICKET_MACHINE.get(), pos, state);
	}
	
	@Override
	public void saveAdditional(CompoundTag compound)
	{
		
		InventoryUtil.saveAllItems("Items", compound, this.storage);
		
		super.saveAdditional(compound);
	}
	
	public void load(CompoundTag compound)
	{
		
		this.storage = InventoryUtil.loadAllItems("Items", compound, 2);
		super.load(compound);
		
	}
	
}
