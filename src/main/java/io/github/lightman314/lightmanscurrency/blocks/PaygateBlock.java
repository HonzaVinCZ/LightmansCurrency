package io.github.lightman314.lightmanscurrency.blocks;

import java.util.List;

import javax.annotation.Nullable;

import io.github.lightman314.lightmanscurrency.blockentity.PaygateBlockEntity;
import io.github.lightman314.lightmanscurrency.blocks.traderblocks.templates.TraderBlockRotatable;
import io.github.lightman314.lightmanscurrency.items.TooltipItem;
import io.github.lightman314.lightmanscurrency.items.tooltips.LCTooltips;
import io.github.lightman314.lightmanscurrency.trader.common.TradeContext;
import io.github.lightman314.lightmanscurrency.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class PaygateBlock extends TraderBlockRotatable {
	
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	
	public PaygateBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(
			this.defaultBlockState()
				.setValue(POWERED, false)
		);
	}
	
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result)
	{
		if(!level.isClientSide)
		{
			//Get the item in the players hand
			BlockEntity tileEntity = level.getBlockEntity(pos);
			if(tileEntity instanceof PaygateBlockEntity)
			{
				PaygateBlockEntity paygate = (PaygateBlockEntity)tileEntity;
				int tradeIndex = paygate.getValidTicketTrade(player, player.getItemInHand(hand));
				if(tradeIndex >= 0)
				{
					//Emulate the trade execution
					paygate.ExecuteTrade(TradeContext.create(paygate, player).build(), tradeIndex);
					return InteractionResult.SUCCESS;
				}
			}
		}
		return super.use(state, level, pos, player, hand, result);
	}
	
	@Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }
	
	@Override
	public boolean isSignalSource(BlockState state)
	{
		return true;
	}
	
	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		
		if(state.getValue(POWERED))
			return 15;
		return 0;
		
	}
	
	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flagIn)
	{
		TooltipItem.addTooltip(tooltip, LCTooltips.PAYGATE);
		super.appendHoverText(stack, level, tooltip, flagIn);
	}

	@Override
	protected BlockEntity makeTrader(BlockPos pos, BlockState state) { return new PaygateBlockEntity(pos, state); }

	@Override
	protected BlockEntityType<?> traderType() { return ModBlockEntities.PAYGATE.get(); }
	
}
