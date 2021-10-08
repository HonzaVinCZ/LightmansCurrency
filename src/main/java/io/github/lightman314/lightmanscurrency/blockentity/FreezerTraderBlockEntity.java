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
		super(ModBlockEntities.FREEZER_TRADER, pos, state);
	}
	
	public FreezerTraderBlockEntity(BlockPos pos, BlockState state, int tradeCount)
	{
		super(ModBlockEntities.FREEZER_TRADER, pos, state, tradeCount);
	}
	
	public float getDoorAngle(float partialTicks) {
		return Mth.lerp(partialTicks, this.prevDoorAngle, this.doorAngle);
	}
	
	@Override
	public void clientTick()
	{
		
		//this.userCount = this.storageContainers.size();
		int userCount = this.getUserCount();
		//LightmansCurrency.LOGGER.info("Freezer Usercount: " + userCount);
		
		this.prevDoorAngle = this.doorAngle;
		//Play the opening sound
		if (userCount > 0 && this.doorAngle == 0.0F) {
			this.level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
		}
		
		if(userCount > 0 && this.doorAngle < 1f)
		{
			this.doorAngle += 0.1f;
		}
		else if(userCount == 0 && doorAngle > 0f)
		{
			this.doorAngle -= 0.1f;
			if (this.doorAngle < 0.5F && this.prevDoorAngle >= 0.5F) {
				this.level.playSound(null, worldPosition, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
			}
		}
		if(this.doorAngle > 1f)
			this.doorAngle = 1f;
		else if(this.doorAngle < 0f)
			this.doorAngle = 0f;
		
	}

}