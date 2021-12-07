package io.github.lightman314.lightmanscurrency.blocks.traderblocks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import io.github.lightman314.lightmanscurrency.blockentity.ItemTraderBlockEntity;
import io.github.lightman314.lightmanscurrency.blocks.templates.interfaces.IRotatableBlock;
import io.github.lightman314.lightmanscurrency.blocks.traderblocks.interfaces.IItemTraderBlock;
import io.github.lightman314.lightmanscurrency.blocks.traderblocks.templates.TraderBlockRotatable;
import io.github.lightman314.lightmanscurrency.blocks.util.LazyShapes;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ShelfBlock extends TraderBlockRotatable implements IItemTraderBlock{
	
	public static final int TRADECOUNT = 1;
	
	private static final VoxelShape SHAPE_NORTH = box(0d,0d,0d,16d,16d,5d);
	private static final VoxelShape SHAPE_SOUTH = box(0d,0d,11d,16d,16d,16d);
	private static final VoxelShape SHAPE_EAST = box(11d,0d,0d,16d,16d,16d);
	private static final VoxelShape SHAPE_WEST = box(0d,0d,0d,5d,16d,16d);
	
	public ShelfBlock(Properties properties)
	{
		super(properties, LazyShapes.lazyDirectionalShape(SHAPE_NORTH, SHAPE_EAST, SHAPE_SOUTH, SHAPE_WEST));
	}

	@Override
	public BlockEntity makeTrader(BlockPos pos, BlockState state) { return new ItemTraderBlockEntity(pos, state, TRADECOUNT); }
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public List<Vector3f> GetStackRenderPos(int tradeSlot, BlockState state, boolean isBlock) {
		List<Vector3f> posList = new ArrayList<Vector3f>(1);
		Direction facing = this.getFacing(state);
		//Define directions for easy positional handling
		Vector3f forward = IRotatableBlock.getForwardVect(facing);
		Vector3f right = IRotatableBlock.getRightVect(facing);
		Vector3f up = Vector3f.YP;
		Vector3f offset = IRotatableBlock.getOffsetVect(facing);
		
		if(tradeSlot == 0)
		{
			Vector3f firstPosition = MathUtil.VectorAdd(offset, MathUtil.VectorMult(right, 0.5f), MathUtil.VectorMult(forward, 14.5f/16f), MathUtil.VectorMult(up, 9f/16f));
			posList.add(firstPosition); 
			for(float distance = isBlock ? -3.2f : -1f; distance >= -3f; distance -= isBlock ? 3.2f : 1f)
			{
				posList.add(MathUtil.VectorAdd(firstPosition, MathUtil.VectorMult(forward, distance/16f)));
			}
		}
		
		return posList;
	}
	
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public List<Quaternion> GetStackRenderRot(int tradeSlot, BlockState state, boolean isBlock)
	{
		//Return null for automatic rotation
		List<Quaternion> rotation = new ArrayList<>();
		int facing = this.getFacing(state).get2DDataValue();
		rotation.add(Vector3f.YP.rotationDegrees(facing * -90f));
		return rotation;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public Vector3f GetStackRenderScale(int tradeSlot, BlockState state, boolean isBlock){
		return new Vector3f(14f/16f, 14f/16f, 14f/16f);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public int maxRenderIndex()
	{
		return TRADECOUNT;
	}
	
}