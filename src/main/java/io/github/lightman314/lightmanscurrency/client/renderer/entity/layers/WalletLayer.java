package io.github.lightman314.lightmanscurrency.client.renderer.entity.layers;

import io.github.lightman314.lightmanscurrency.client.ModLayerDefinitions;
import io.github.lightman314.lightmanscurrency.client.model.ModelWallet;
import io.github.lightman314.lightmanscurrency.common.capability.IWalletHandler;
import io.github.lightman314.lightmanscurrency.common.capability.WalletCapability;
import io.github.lightman314.lightmanscurrency.items.WalletItem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class WalletLayer<T extends Player, M extends HumanoidModel<T>> extends RenderLayer<T,M>{

	private ModelWallet<T> model;
	
	public WalletLayer(RenderLayerParent<T,M> renderer)
	{
		super(renderer);
		this.model = new ModelWallet<T>(Minecraft.getInstance().getEntityModels().bakeLayer(ModLayerDefinitions.WALLET));
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float limbSwing,
			float limbSwingAmount,
			float partialTicks,
			float ageInTicks,
			float netHeadYaw,
			float headPitch) {
		
		IWalletHandler handler = WalletCapability.getWalletHandler(entity).orElse(null);
		if(handler == null || !handler.visible())
			return;
		
		ItemStack wallet = handler.getWallet();
		if(wallet.getItem() instanceof WalletItem)
		{
			
			WalletItem walletItem = (WalletItem)wallet.getItem();
			this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
			this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			this.getParentModel().copyPropertiesTo(this.model);
			VertexConsumer vertexConsumer = ItemRenderer
					.getFoilBuffer(bufferSource, this.model.renderType(walletItem.getModelTexture()), false, wallet.hasFoil());
			this.model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
			
		}
		
	}
	
	public static LayerDefinition createLayer() {
		CubeDeformation cube = CubeDeformation.NONE;
		MeshDefinition mesh = HumanoidModel.createMesh(cube, 0.0f);
		PartDefinition part = mesh.getRoot();
		part.addOrReplaceChild("wallet", CubeListBuilder.create().texOffs(0, 0).addBox(4f, 11.5f, -2f, 2f, 4f, 4f, cube),
				PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 0f, 0f));
		return LayerDefinition.create(mesh, 32, 16);
	}
	
}
