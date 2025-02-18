package io.github.lightman314.lightmanscurrency.blockentity;

import io.github.lightman314.lightmanscurrency.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class FreezerTraderBlockEntity extends ItemTraderBlockEntity{

	/** The current angle of the door (between 0 and 1) */
	private float doorAngle;
	/** The angle of the door last tick */
	private float prevDoorAngle;
	
	public FreezerTraderBlockEntity(BlockPos pos, BlockState state)
	{
		super(ModBlockEntities.FREEZER_TRADER.get(), pos, state);
	}
	
	public FreezerTraderBlockEntity(BlockPos pos, BlockState state, int tradeCount)
	{
		super(ModBlockEntities.FREEZER_TRADER.get(), pos, state, tradeCount);
	}
	
	public float getDoorAngle(float partialTicks) {
		return Mth.lerp(partialTicks, this.prevDoorAngle, this.doorAngle);
	}
	
	private final float distancePerTick = 0.1f;
	
	@Override
	public void clientTick()
	{
		
		super.clientTick();
		
		int userCount = this.getUserCount();
		
		this.prevDoorAngle = this.doorAngle;
		//Play the opening sound
		if (userCount > 0 && this.doorAngle == 0.0F) {
			this.level.playLocalSound(this.worldPosition.getX() + 0.5d, this.worldPosition.getY() + 0.5d, this.worldPosition.getZ() + 0.5d, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
			//this.level.playSound(null, this.worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
		}
		if(userCount > 0 && this.doorAngle < 1f)
		{
			this.doorAngle += distancePerTick;
		}
		else if(userCount <= 0 && doorAngle > 0f)
		{
			this.doorAngle -= distancePerTick;
			if (this.doorAngle < 0.5F && this.prevDoorAngle >= 0.5F) {
				this.level.playLocalSound(this.worldPosition.getX() + 0.5d, this.worldPosition.getY() + 0.5d, this.worldPosition.getZ() + 0.5d, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
			}
		}
		if(this.doorAngle > 1f)
			this.doorAngle = 1f;
		else if(this.doorAngle < 0f)
			this.doorAngle = 0f;
		
		//LightmansCurrency.LogInfo("FreezerTraderBlockEntity.clientTick().\nUsers: " + this.getUserCount() + "\nPreviousAngle: " + this.prevDoorAngle + "\nNewAngle: " + this.doorAngle);
		
	}

}
