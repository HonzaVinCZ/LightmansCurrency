package io.github.lightman314.lightmanscurrency.client.renderer.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.blockentity.ItemTraderBlockEntity;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

public class ItemTraderBlockEntityRenderer implements BlockEntityRenderer<ItemTraderBlockEntity>{
	
	public ItemTraderBlockEntityRenderer(BlockEntityRendererProvider.Context dispatcher) { }
	
	@Override
	public void render(ItemTraderBlockEntity blockEntity, float partialTicks, PoseStack pose, MultiBufferSource buffer, int lightLevel, int id)
	{
		renderItems(blockEntity, partialTicks, pose, buffer, lightLevel, id);
	}
	
	public static List<ItemStack> GetRenderItems(ItemTradeData trade) {
		List<ItemStack> result = new ArrayList<>();
		for(int i = 0; i < 2; ++i)
		{
			ItemStack item = trade.getSellItem(i);
			if(!item.isEmpty())
				result.add(item);
		}
		return result;
	}
	
	public static void renderItems(ItemTraderBlockEntity blockEntity, float partialTicks, PoseStack pose, MultiBufferSource buffer, int lightLevel, int id)
	{
		for(int tradeSlot = 0; tradeSlot < blockEntity.getTradeCount() && tradeSlot < blockEntity.maxRenderIndex(); tradeSlot++)
		{
			
			ItemTradeData trade = blockEntity.getTrade(tradeSlot);
			List<ItemStack> renderItems = GetRenderItems(trade);
			if(renderItems.size() > 0)
			{
				
				//Get positions
				List<Vector3f> positions = blockEntity.GetStackRenderPos(tradeSlot, renderItems.size() > 1);
				
				//Get rotation
				List<Quaternion> rotation = blockEntity.GetStackRenderRot(tradeSlot, partialTicks);
				
				//Get scale
				float scale = blockEntity.GetStackRenderScale(tradeSlot);

				for(int pos = 0; pos < Config.CLIENT.traderRenderType.get().renderLimit && pos < positions.size() && pos < blockEntity.getTradeStock(tradeSlot); pos++)
				{
					
					pose.pushPose();
					
					Vector3f position = positions.get(pos);
					
					//Translate, rotate, and scale the matrix stack
					pose.translate(position.x(), position.y(), position.z());
					for(Quaternion rot : rotation)
					{
						pose.mulPose(rot);
					}
					pose.scale(scale, scale, scale);
					
					//Render the item
					if(renderItems.size() > 1)
					{
						//Render first item
						pose.pushPose();
						
						//Don't base translation off of scale, as we've already been scaled down.
						pose.translate(0.25, 0.25, 0d);
						pose.scale(0.5f, 0.5f, 0.5f);
						
						Minecraft.getInstance().getItemRenderer().renderStatic(renderItems.get(0),  ItemTransforms.TransformType.FIXED, lightLevel, OverlayTexture.NO_OVERLAY, pose, buffer, id);
						
						pose.popPose();
						
						//Render second item
						pose.pushPose();
						
						//Slightly offset in the Z to prevent z-fighting if there's an overlap
						pose.translate(-0.25, -0.25, 0.001d);
						pose.scale(0.5f, 0.5f, 0.5f);
						
						Minecraft.getInstance().getItemRenderer().renderStatic(renderItems.get(1),  ItemTransforms.TransformType.FIXED, lightLevel, OverlayTexture.NO_OVERLAY, pose, buffer, id);
						
						pose.popPose();
					}
					else
						Minecraft.getInstance().getItemRenderer().renderStatic(renderItems.get(0),  ItemTransforms.TransformType.FIXED, lightLevel, OverlayTexture.NO_OVERLAY, pose, buffer, id);
				
					pose.popPose();
					
				}
				
			}
			
		}
	}

	
}
