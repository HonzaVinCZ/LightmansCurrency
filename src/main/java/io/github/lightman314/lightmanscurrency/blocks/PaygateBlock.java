package io.github.lightman314.lightmanscurrency.blocks;

import java.util.List;

import javax.annotation.Nullable;

import io.github.lightman314.lightmanscurrency.tileentity.PaygateTileEntity;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil;
import io.github.lightman314.lightmanscurrency.util.TileEntityUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class PaygateBlock extends RotatableBlock{
	
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	
	public PaygateBlock(Properties properties)
	{
		super(properties);
		this.setDefaultState(
			this.stateContainer.getBaseState()
				.with(POWERED, false)
		);
	}
	
	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}
	
	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return new PaygateTileEntity();
	}
	
	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result)
	{
		if(!world.isRemote())
		{
			//Open UI
			TileEntity tileEntity = world.getTileEntity(pos);
			if(tileEntity instanceof PaygateTileEntity)
			{
				PaygateTileEntity trader = (PaygateTileEntity)tileEntity;
				//Update the owner
				if(trader.isOwner(playerEntity))
				{
					//CurrencyMod.LOGGER.info("Updating the owner name.");
					trader.setOwner(playerEntity);
				}
				TileEntityUtil.sendUpdatePacket(tileEntity);
				NetworkHooks.openGui((ServerPlayerEntity)playerEntity, (INamedContainerProvider) tileEntity, pos);
			}
		}
		return ActionResultType.SUCCESS;
	}
	
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity player, ItemStack stack)
	{
		if(!worldIn.isRemote())
		{
			PaygateTileEntity tileEntity = (PaygateTileEntity)worldIn.getTileEntity(pos);
			if(tileEntity != null)
			{
				tileEntity.setOwner(player);
				if(stack.hasDisplayName())
					tileEntity.setCustomName(stack.getDisplayName());
			}
		}
	}
	
	@Override
	public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player)
	{
		
		if(worldIn.getTileEntity(pos) instanceof PaygateTileEntity)
		{
			PaygateTileEntity tileEntity = (PaygateTileEntity)worldIn.getTileEntity(pos);
			List<ItemStack> coins = MoneyUtil.getCoinsOfValue(tileEntity.getStoredMoney());
			InventoryUtil.dumpContents(worldIn, pos, coins);
		}
		
		super.onBlockHarvested(worldIn, pos, state, player);
		
	}
	
	@Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    {
        super.fillStateContainer(builder);
        builder.add(POWERED);
    }
	
	@Override
	public boolean canProvidePower(BlockState state)
	{
		return true;
	}
	
	@Override
	public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
		
		if(blockState.get(POWERED))
			return 15;
		return 0;
		
	}
	
	
	
}