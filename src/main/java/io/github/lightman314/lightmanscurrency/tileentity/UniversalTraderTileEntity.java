package io.github.lightman314.lightmanscurrency.tileentity;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalTraderData;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.MessageRequestNBT;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil;
import io.github.lightman314.lightmanscurrency.util.TileEntityUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public abstract class UniversalTraderTileEntity extends TileEntity implements IOwnableTileEntity{

	
	UUID traderID = null;
	
	protected UniversalTraderData getData()
	{
		if(this.traderID != null)
			return TradingOffice.getData(this.traderID);
		LightmansCurrency.LogError("Trader ID is null. Cannot get the data (" + (this.world.isRemote ? "client" : "server"));
		return null;
	}
	
	public UniversalTraderTileEntity(TileEntityType<?> type)
	{
		super(type);
	}
	
	public UUID getTraderID()
	{
		return this.traderID;
	}
	
	public void updateOwner(Entity player)
	{
		if(this.getData() != null && player.getUniqueID().equals(this.getData().getOwnerID()))
		{
			this.getData().updateOwnerName(player.getDisplayName().getString());
		}
		else if(this.getData() == null)
			LightmansCurrency.LogError("Trader Data for trader of id '" + this.traderID + "' is null (tileEntity.updateOwner," + (this.world.isRemote ? "client" : "server" ) + ").");
	}
	
	public boolean isOwner(PlayerEntity player)
	{
		if(this.getData() == null)
		{
			LightmansCurrency.LogError("Trader Data for trader of id '" + this.traderID + "' is null (tileEntity.isOwner," + (this.world.isRemote ? "client" : "server" ) + ").");
			return true;
		}
		return this.getData().isOwner(player);
	}
	
	public boolean hasPermissions(PlayerEntity player)
	{
		if(this.getData() == null)
		{
			LightmansCurrency.LogError("Trader Data for trader of id '" + this.traderID + "' is null (tileEntity.isOwner," + (this.world.isRemote ? "client" : "server" ) + ").");
			return true;
		}
		return this.getData().hasPermissions(player);
	}
	
	public boolean canBreak(PlayerEntity player)
	{
		return this.isOwner(player);
	}
	
	public void init(PlayerEntity owner)
	{
		this.init(owner, null);
	}
	
	public void init(PlayerEntity owner, String customName)
	{
		if(this.world.isRemote)
			return;
		if(this.traderID == null)
		{
			this.traderID = UUID.randomUUID();
			UniversalTraderData traderData = createInitialData(owner);
			if(customName != null)
			{
				traderData.setName(customName);
			}
			TradingOffice.registerTrader(this.traderID, traderData, owner);
			TileEntityUtil.sendUpdatePacket(this);
		}
	}
	
	protected abstract UniversalTraderData createInitialData(Entity owner);
	
	@Override
	public void onLoad()
	{
		if(world.isRemote)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageRequestNBT(this));
		}
	}
	
	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		if(this.traderID != null)
			compound.putUniqueId("ID", this.traderID);
		
		return super.write(compound);
		
	}
	
	@Override
	public void read(BlockState state, CompoundNBT compound)
	{
		if(compound.contains("ID"))
			this.traderID = compound.getUniqueId("ID");
		
		super.read(state, compound);
	}
	
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket()
	{
		return new SUpdateTileEntityPacket(this.pos, 0, this.write(new CompoundNBT()));
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
	{
		CompoundNBT compound = pkt.getNbtCompound();
		this.read(this.getBlockState(), compound);
	}
	
	public void openStorageMenu(PlayerEntity player)
	{
		if(!this.world.isRemote && this.getData() != null)
			this.getData().openStorageMenu((ServerPlayerEntity)player);
	}
	
	public void onDestroyed()
	{
		if(this.world.isRemote)
			return;
		UniversalTraderData data = this.getData();
		//Remove the data from the register
		TradingOffice.removeTrader(this.traderID);
		//Dump the inventory contents
		if(data != null)
		{
			this.dumpContents(data);
		}
	}
	
	protected void dumpContents(@Nonnull UniversalTraderData data)
	{
		InventoryUtil.dumpContents(this.world, this.pos, MoneyUtil.getCoinsOfValue(data.getStoredMoney()));
	}
	
}