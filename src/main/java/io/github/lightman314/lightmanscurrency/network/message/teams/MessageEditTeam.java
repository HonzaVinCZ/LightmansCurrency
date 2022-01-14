package io.github.lightman314.lightmanscurrency.network.message.teams;

import java.util.UUID;
import java.util.function.Supplier;

import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class MessageEditTeam {
	
	UUID teamID;
	String playerName;
	String category;
	
	public MessageEditTeam(UUID teamID, String playerName, String category)
	{
		this.teamID = teamID;
		this.playerName = playerName;
		this.category = category;
	}
	
	public static void encode(MessageEditTeam message, FriendlyByteBuf buffer) {
		buffer.writeUUID(message.teamID);
		buffer.writeUtf(message.playerName, 16);
		buffer.writeUtf(message.category);
	}

	public static MessageEditTeam decode(FriendlyByteBuf buffer) {
		return new MessageEditTeam(buffer.readUUID(), buffer.readUtf(16), buffer.readUtf());
	}

	public static void handle(MessageEditTeam message, Supplier<Context> supplier) {
		supplier.get().enqueueWork(() -> {
			Team team = TradingOffice.getTeam(message.teamID);
			if(team != null)
			{
				team.changeAny(supplier.get().getSender(), message.playerName, message.category);
			}
		});
		supplier.get().setPacketHandled(true);
	}

}
