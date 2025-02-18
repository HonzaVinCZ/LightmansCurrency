package io.github.lightman314.lightmanscurrency.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.capability.CurrencyCapabilities;
import io.github.lightman314.lightmanscurrency.common.capability.SpawnTrackerCapability;
import io.github.lightman314.lightmanscurrency.core.ModItems;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.storage.loot.LootPool.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;

@Mod.EventBusSubscriber
public class LootManager {

	public static final float LOOTING_MODIFIER = 0.01f;
	
	public enum PoolLevel
	{
		COPPER(0, true),
		IRON(1, true),
		GOLD(2, true),
		EMERALD(3, true),
		DIAMOND(4, true),
		NETHERITE(5, true),
		BOSS_COPPER(6, false),
		BOSS_IRON(7, false),
		BOSS_GOLD(8, false),
		BOSS_EMERALD(9, false),
		BOSS_DIAMOND(10, false),
		BOSS_NETHERITE(11, false);
		
		public final int level;
		private final boolean requiresPlayerKill;
		public final boolean requiresPlayerKill() { return this.requiresPlayerKill; }
		
		private PoolLevel(int level, boolean requiresPlayerKill) { this.level = level; this.requiresPlayerKill = requiresPlayerKill; }
		
	};
	
	public static boolean isValidSpawnReason(String reasonString)
	{
		for(MobSpawnType reason : MobSpawnType.values())
		{
			if(reason.toString() == reasonString)
				return true;
		}
		return false;
	}
	
	public static MobSpawnType deserializeSpawnReason(String reasonString)
	{
		return deserializeSpawnReason(reasonString, MobSpawnType.NATURAL);
	}
	
	public static MobSpawnType deserializeSpawnReason(String reasonString, MobSpawnType defaultReason)
	{
		for(MobSpawnType reason : MobSpawnType.values())
		{
			if(reason.toString().contentEquals(reasonString))
				return reason;
		}
		LightmansCurrency.LogWarning("Reason string \"" + reasonString + "\" could not be properly deserialized. Returning the default spawn reason.");
		return defaultReason;
	}
	
	public static boolean containsReason(List<? extends String> reasonList, MobSpawnType reason)
	{
		for(int i = 0; i < reasonList.size(); ++i)
		{
			if(reason.toString().contentEquals(reasonList.get(i)))
				return true;
		}
		return false;
	}
	
	public static String getSpawnReasonList()
	{
		StringBuffer output = new StringBuffer();
		for(MobSpawnType reason : MobSpawnType.values())
		{
			if(output.length() > 0)
				output.append(", ");
			output.append(reason);
		}
		return output.toString();
	}
	
	private static final String ENTITY = "minecraft:";
	private static final String CHEST = "minecraft:chests/";
	
	public static final List<String> ENTITY_COPPER_DROPLIST = ImmutableList.of(ENTITY + "slime", ENTITY + "silverfish");
	public static final List<String> ENTITY_IRON_DROPLIST = ImmutableList.of(ENTITY + "zombie", ENTITY + "skeleton", ENTITY + "creeper", ENTITY + "spider", ENTITY + "cave_spider", ENTITY + "husk", ENTITY + "stray", ENTITY + "magma_cube", ENTITY + "zombie_villager", ENTITY + "drowned");
	public static final List<String> ENTITY_GOLD_DROPLIST = ImmutableList.of(ENTITY + "guardian", ENTITY + "elder_guardian", ENTITY + "phantom", ENTITY + "blaze", ENTITY + "ghast", ENTITY + "witch", ENTITY + "hoglin", ENTITY + "piglin_brute", ENTITY + "piglin", ENTITY + "zombified_piglin");
	public static final List<String> ENTITY_EMERALD_DROPLIST = ImmutableList.of(ENTITY + "enderman", ENTITY + "evoker", ENTITY + "vindicator", ENTITY + "pillager", ENTITY + "ravager", ENTITY + "shulker");
	public static final List<String> ENTITY_DIAMOND_DROPLIST = ImmutableList.of(ENTITY + "wither_skeleton");
	public static final List<String> ENTITY_NETHERITE_DROPLIST = ImmutableList.of();
	
	public static final List<String> ENTITY_BOSS_COPPER_DROPLIST = ImmutableList.of();
	public static final List<String> ENTITY_BOSS_IRON_DROPLIST = ImmutableList.of();
	public static final List<String> ENTITY_BOSS_GOLD_DROPLIST = ImmutableList.of();
	public static final List<String> ENTITY_BOSS_EMERALD_DROPLIST = ImmutableList.of(ENTITY + "warden");
	public static final List<String> ENTITY_BOSS_DIAMOND_DROPLIST = ImmutableList.of(ENTITY + "ender_dragon");
	public static final List<String> ENTITY_BOSS_NETHERITE_DROPLIST = ImmutableList.of(ENTITY + "wither");
	
	public static final List<String> CHEST_COPPER_DROPLIST = ImmutableList.of(CHEST + "underwater_ruin_small", CHEST + "underwater_ruin_big");
	public static final List<String> CHEST_IRON_DROPLIST = ImmutableList.of();
	public static final List<String> CHEST_GOLD_DROPLIST = ImmutableList.of(CHEST + "jungle_temple", CHEST + "nether_bridge", CHEST + "simple_dungeon", CHEST + "ruined_portal");
	public static final List<String> CHEST_EMERALD_DROPLIST = ImmutableList.of(CHEST + "stronghold_crossing", CHEST + "stronghold_corridor", CHEST + "stronghold_library", CHEST + "ancient_city");
	public static final List<String> CHEST_DIAMOND_DROPLIST = ImmutableList.of(CHEST + "buried_treasure", CHEST + "bastion_hoglin_stable", CHEST + "bastion_bridge", CHEST + "bastion_other", CHEST + "bastion_treasure", CHEST + "end_city_treasure");
	public static final List<String> CHEST_NETHERITE_DROPLIST = ImmutableList.of();
	
	private static final Map<String,PoolLevel> EXTERNAL_ENTITY_ENTRIES = new HashMap<>();
	private static final Map<String,PoolLevel> EXTERNAL_CHEST_ENTRIES = new HashMap<>();
	
	private static boolean lootTablesBuilt = false;
	
	//Normal entity loot
	private static Builder ENTITY_LOOT_COPPER = null;
	private static Builder ENTITY_LOOT_IRON = null;
	private static Builder ENTITY_LOOT_GOLD = null;
	private static Builder ENTITY_LOOT_EMERALD = null;
	private static Builder ENTITY_LOOT_DIAMOND = null;
	private static Builder ENTITY_LOOT_NETHERITE = null;
	
	//Boss loot
	private static List<Builder> ENTITY_LOOT_BOSS_COPPER = null;
	private static List<Builder> ENTITY_LOOT_BOSS_IRON = null;
	private static List<Builder> ENTITY_LOOT_BOSS_GOLD = null;
	private static List<Builder> ENTITY_LOOT_BOSS_EMERALD = null;
	private static List<Builder> ENTITY_LOOT_BOSS_DIAMOND = null;
	private static List<Builder> ENTITY_LOOT_BOSS_NETHERITE = null;
	
	//Chest loot
	private static Builder CHEST_LOOT_COPPER = null;
	private static Builder CHEST_LOOT_IRON = null;
	private static Builder CHEST_LOOT_GOLD = null;
	private static Builder CHEST_LOOT_EMERALD = null;
	private static Builder CHEST_LOOT_DIAMOND = null;
	private static Builder CHEST_LOOT_NETHERITE = null;
	
	private static void generateLootTables() {
		if(lootTablesBuilt)
			return;
		
		//Normal Loot
		ENTITY_LOOT_COPPER = GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 1, 10, 0.75f, "lightmanscurrency:entityloot_copper", true);
		ENTITY_LOOT_IRON = GenerateEntityCoinPool(ModItems.COIN_IRON.get(), 1, 5, 0.5f, "lightmanscurrency:entityloot_iron", true);
		ENTITY_LOOT_GOLD = GenerateEntityCoinPool(ModItems.COIN_GOLD.get(), 1, 5, 0.25f, "lightmanscurrency:entityloot_gold", true);
		ENTITY_LOOT_EMERALD = GenerateEntityCoinPool(ModItems.COIN_EMERALD.get(), 1, 3, 0.1f, "lightmanscurrency:entityloot_emerald", true);
		ENTITY_LOOT_DIAMOND = GenerateEntityCoinPool(ModItems.COIN_DIAMOND.get(), 1, 3, 0.05f, "lightmanscurrency:entityloot_diamond", true);
		ENTITY_LOOT_NETHERITE = GenerateEntityCoinPool(ModItems.COIN_NETHERITE.get(), 1, 3, 0.025F, "lightmanscurrency:entityloot_netherite", true);
		
		//Boss loot
		ENTITY_LOOT_BOSS_COPPER = ImmutableList.of(GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 10,30,1.0f,"lightmanscurrency:entityloot_boss_copper", false));
		ENTITY_LOOT_BOSS_IRON = ImmutableList.of(GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 10,30,1.0f,"lightmanscurrency:entityloot_boss_copper", false),GenerateEntityCoinPool(ModItems.COIN_IRON.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_iron", false));
		ENTITY_LOOT_BOSS_GOLD = ImmutableList.of(GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 10,30,1.0f,"lightmanscurrency:entityloot_boss_copper", false),GenerateEntityCoinPool(ModItems.COIN_IRON.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_iron", false), GenerateEntityCoinPool(ModItems.COIN_GOLD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_gold", false));
		ENTITY_LOOT_BOSS_EMERALD = ImmutableList.of(GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 10,30,1.0f,"lightmanscurrency:entityloot_boss_copper", false),GenerateEntityCoinPool(ModItems.COIN_IRON.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_iron", false), GenerateEntityCoinPool(ModItems.COIN_GOLD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_gold", false), GenerateEntityCoinPool(ModItems.COIN_EMERALD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_emerald", false));
		ENTITY_LOOT_BOSS_DIAMOND = ImmutableList.of(GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 10,30,1.0f,"lightmanscurrency:entityloot_boss_copper", false),GenerateEntityCoinPool(ModItems.COIN_IRON.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_iron", false), GenerateEntityCoinPool(ModItems.COIN_GOLD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_gold", false), GenerateEntityCoinPool(ModItems.COIN_EMERALD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_emerald", false),GenerateEntityCoinPool(ModItems.COIN_DIAMOND.get(), 10, 30, 1.0f, "lightmanscurrency:coinloot_boss_diamond", false));
		ENTITY_LOOT_BOSS_NETHERITE = ImmutableList.of(GenerateEntityCoinPool(ModItems.COIN_COPPER.get(), 10,30,1.0f,"lightmanscurrency:entityloot_boss_copper", false),GenerateEntityCoinPool(ModItems.COIN_IRON.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_iron", false), GenerateEntityCoinPool(ModItems.COIN_GOLD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_gold", false), GenerateEntityCoinPool(ModItems.COIN_EMERALD.get(), 10,30,1.0f,"lightmanscurrency:coinloot_boss_emerald", false),GenerateEntityCoinPool(ModItems.COIN_DIAMOND.get(), 10, 30, 1.0f, "lightmanscurrency:coinloot_boss_diamond", false),GenerateEntityCoinPool(ModItems.COIN_NETHERITE.get(), 1, 5, 1.0f, "lightmanscurrency:coinloot_boss_netherite", false));
		
		//Chest loot
		CHEST_LOOT_COPPER = GenerateChestCoinPool(new ChestLootEntryData[] {ChestLootEntryData.COPPER}, 1, 5, "lightmanscurrency:chestloot_copper");
		CHEST_LOOT_IRON = GenerateChestCoinPool(new ChestLootEntryData[] {ChestLootEntryData.COPPER, ChestLootEntryData.IRON}, 1, 5, "lightmanscurrency:chestloot_iron");
		CHEST_LOOT_GOLD = GenerateChestCoinPool(new ChestLootEntryData[] {ChestLootEntryData.COPPER, ChestLootEntryData.IRON, ChestLootEntryData.GOLD}, 2, 6, "lightmanscurrency:chestloot_gold");
		CHEST_LOOT_EMERALD = GenerateChestCoinPool(new ChestLootEntryData[] {ChestLootEntryData.COPPER, ChestLootEntryData.IRON, ChestLootEntryData.GOLD, ChestLootEntryData.EMERALD}, 3, 6, "lightmanscurrency:chestloot_emerald");
		CHEST_LOOT_DIAMOND = GenerateChestCoinPool(new ChestLootEntryData[] {ChestLootEntryData.COPPER, ChestLootEntryData.IRON, ChestLootEntryData.GOLD, ChestLootEntryData.EMERALD, ChestLootEntryData.DIAMOND}, 3, 6, "lightmanscurrency:chestloot_diamond");
		CHEST_LOOT_NETHERITE = GenerateChestCoinPool(new ChestLootEntryData[] {ChestLootEntryData.COPPER, ChestLootEntryData.IRON, ChestLootEntryData.GOLD, ChestLootEntryData.EMERALD, ChestLootEntryData.DIAMOND, ChestLootEntryData.NETHERITE}, 3, 6, "lightmanscurrency:chestloot_netherite");
		
		lootTablesBuilt = true;
		
	}

	private static String getValueList(ConfigValue<List<? extends String>> config) {
		StringBuffer buffer = new StringBuffer();
		List<? extends String> list = config.get();
		for(String value : list)
		{
			if(buffer.length() > 0)
				buffer.append(", ");
			buffer.append("\"").append(value).append("\"");
		}
		return buffer.toString();
	}

	public static void debugLootConfigs() {

		LightmansCurrency.LogDebug("Lightman's Currency common configs have been loaded. Coin loot values are as follows.");
		//Chests
		LightmansCurrency.LogDebug("Chest Copper: " + getValueList(Config.COMMON.copperChestDrops));
		LightmansCurrency.LogDebug("Chest Iron: " + getValueList(Config.COMMON.ironChestDrops));
		LightmansCurrency.LogDebug("Chest Gold: " + getValueList(Config.COMMON.goldChestDrops));
		LightmansCurrency.LogDebug("Chest Emerald: " + getValueList(Config.COMMON.emeraldChestDrops));
		LightmansCurrency.LogDebug("Chest Diamond: " + getValueList(Config.COMMON.diamondChestDrops));
		LightmansCurrency.LogDebug("Chest Netherite: " + getValueList(Config.COMMON.netheriteChestDrops));

		//Entity (normal)
		LightmansCurrency.LogDebug("Entity Copper (Normal): " + getValueList(Config.COMMON.copperEntityDrops));
		LightmansCurrency.LogDebug("Entity Iron (Normal): " + getValueList(Config.COMMON.ironEntityDrops));
		LightmansCurrency.LogDebug("Entity Gold (Normal): " + getValueList(Config.COMMON.goldEntityDrops));
		LightmansCurrency.LogDebug("Entity Emerald (Normal): " + getValueList(Config.COMMON.emeraldEntityDrops));
		LightmansCurrency.LogDebug("Entity Diamond (Normal): " + getValueList(Config.COMMON.diamondEntityDrops));
		LightmansCurrency.LogDebug("Entity Netherite (Normal): " + getValueList(Config.COMMON.netheriteEntityDrops));

		//Entity (boss)
		LightmansCurrency.LogDebug("Entity Copper (Boss): " + getValueList(Config.COMMON.bossCopperEntityDrops));
		LightmansCurrency.LogDebug("Entity Iron (Boss): " + getValueList(Config.COMMON.bossIronEntityDrops));
		LightmansCurrency.LogDebug("Entity Gold (Boss): " + getValueList(Config.COMMON.bossGoldEntityDrops));
		LightmansCurrency.LogDebug("Entity Emerald (Boss): " + getValueList(Config.COMMON.bossEmeraldEntityDrops));
		LightmansCurrency.LogDebug("Entity Diamond (Boss): " + getValueList(Config.COMMON.bossDiamondEntityDrops));
		LightmansCurrency.LogDebug("Entity Netherite (Boss): " + getValueList(Config.COMMON.bossNetheriteEntityDrops));

	}
	
	@SubscribeEvent
	public static void onEntitySpawned(LivingSpawnEvent.SpecialSpawn event)
	{
		LivingEntity entity = event.getEntity();
		if(entity instanceof Player)
			return;
		
		SpawnTrackerCapability.getSpawnerTracker(entity).ifPresent(spawnerTracker -> spawnerTracker.setSpawnReason(event.getSpawnReason()));
		if(!SpawnTrackerCapability.getSpawnerTracker(entity).isPresent())
			LightmansCurrency.LogWarning(entity.getName().getString() + " does not have a ISpawnerTracker attached. Unable to flag it's SpawnReason.");
		
	}
	
	@SubscribeEvent
	public static void attachSpawnTrackerCapability(AttachCapabilitiesEvent<Entity> event)
	{
		//Attach the spawn trader capability to all LivingEntities that aren't players
		if(event.getObject() instanceof LivingEntity && !(event.getObject() instanceof Player))
		{
			event.addCapability(CurrencyCapabilities.ID_SPAWN_TRACKER, SpawnTrackerCapability.createProvider((LivingEntity)event.getObject()));
		}
	}
	
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event)
	{
		//Check if this is the server
		if(event.getEntity().level.isClientSide)
			return;
		
		if(!Config.COMMON.enableSpawnerEntityDrops.get())
		{
			//Spawner drops aren't allowed. Check if the entity was spawner-spawned
			AtomicReference<MobSpawnType> spawnReason = new AtomicReference<>();
			SpawnTrackerCapability.getSpawnerTracker(event.getEntity()).ifPresent(spawnerTracker -> spawnReason.set(spawnerTracker.spawnReason()));
			if(spawnReason.get() == MobSpawnType.SPAWNER)
			{
				LightmansCurrency.LogDebug(event.getEntity().getName().getString() + " did not drop coins, as it was spawned by a spawner.");
				return;
			}
		}
		
		String name = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();
		
		if(event.getSource().getDirectEntity() instanceof Player || event.getSource().getEntity() instanceof Player)
		{
			//Assign the player that killed it
			final Player player = event.getSource().getDirectEntity() instanceof Player ? (Player)event.getSource().getDirectEntity() : (Player)event.getSource().getEntity();
			
			
			if(Config.COMMON.copperEntityDrops.get().contains(name))
	    	{
				DropEntityLoot(event.getEntity(), player, PoolLevel.COPPER);
	    	}
	    	else if(Config.COMMON.ironEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.IRON);
	    	}
	    	else if(Config.COMMON.goldEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.GOLD);
	    	}
	    	else if(Config.COMMON.emeraldEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.EMERALD);
	    	}
	    	else if(Config.COMMON.diamondEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.DIAMOND);
	    	}
	    	else if(Config.COMMON.netheriteEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.NETHERITE);
	    	}
	    	else if(Config.COMMON.bossCopperEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.BOSS_COPPER);
	    	}
	    	else if(Config.COMMON.bossIronEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.BOSS_IRON);
	    	}
	    	else if(Config.COMMON.bossGoldEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.BOSS_GOLD);
	    	}
	    	else if(Config.COMMON.bossEmeraldEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.BOSS_EMERALD);
	    	}
	    	else if(Config.COMMON.bossDiamondEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.BOSS_DIAMOND);
	    	}
	    	else if(Config.COMMON.bossNetheriteEntityDrops.get().contains(name))
	    	{
	    		DropEntityLoot(event.getEntity(), player, PoolLevel.BOSS_NETHERITE);
	    	}
	    	else
	    	{
	    		EXTERNAL_ENTITY_ENTRIES.forEach((entity,level) -> {
	    			if(entity.equals(name))
	    			{
	    				DropEntityLoot(event.getEntity(), player, level);
	    				return;
	    			}
	    		});
	    	}
			return;
		}
		//Boss deaths don't require a player kill to drop coins
    	if(Config.COMMON.bossCopperEntityDrops.get().contains(name))
    	{
    		DropEntityLoot(event.getEntity(), null, PoolLevel.BOSS_COPPER);
    	}
    	else if(Config.COMMON.bossIronEntityDrops.get().contains(name))
    	{
    		DropEntityLoot(event.getEntity(), null, PoolLevel.BOSS_IRON);
    	}
    	else if(Config.COMMON.bossGoldEntityDrops.get().contains(name))
    	{
    		DropEntityLoot(event.getEntity(), null, PoolLevel.BOSS_GOLD);
    	}
    	else if(Config.COMMON.bossEmeraldEntityDrops.get().contains(name))
    	{
    		DropEntityLoot(event.getEntity(), null, PoolLevel.BOSS_EMERALD);
    	}
    	else if(Config.COMMON.bossDiamondEntityDrops.get().contains(name))
    	{
    		DropEntityLoot(event.getEntity(), null, PoolLevel.BOSS_DIAMOND);
    	}
    	else if(Config.COMMON.bossNetheriteEntityDrops.get().contains(name))
    	{
    		DropEntityLoot(event.getEntity(), null, PoolLevel.BOSS_NETHERITE);
    	}
    	else
    	{
    		EXTERNAL_ENTITY_ENTRIES.forEach((entity,level) -> {
    			if(entity.equals(name) && !level.requiresPlayerKill)
    			{
    				DropEntityLoot(event.getEntity(), null, level);
    			}
    		});
    	}
	}
	
	public static void validateEntityDropList()
	{
		validateDropList(Config.COMMON.copperEntityDrops);
		validateDropList(Config.COMMON.ironEntityDrops);
		validateDropList(Config.COMMON.goldEntityDrops);
		validateDropList(Config.COMMON.emeraldEntityDrops);
		validateDropList(Config.COMMON.diamondEntityDrops);
		validateDropList(Config.COMMON.netheriteEntityDrops);
		validateDropList(Config.COMMON.bossCopperEntityDrops);
		validateDropList(Config.COMMON.bossIronEntityDrops);
		validateDropList(Config.COMMON.bossGoldEntityDrops);
		validateDropList(Config.COMMON.bossEmeraldEntityDrops);
		validateDropList(Config.COMMON.bossDiamondEntityDrops);
		validateDropList(Config.COMMON.bossNetheriteEntityDrops);
	}
	
	private static void validateDropList(ConfigValue<List<? extends String>> config)
	{
		List<? extends String> configList = config.get();
		List<String> list = Lists.newArrayList();
		configList.forEach(value -> list.add(value));
		boolean modified = false;
		for(int i = 0; i < list.size(); ++i)
		{
			String value = list.get(i);
			if(value.contains("entities/"))
			{
				value = value.replace("entities/", "");
				list.set(i, value);
				modified = true;
			}
			if(value.contains("entities\\"))
			{
				value = value.replace("entities\\", "");
				list.set(i, value);
				modified = true;
			}
		}
		if(modified)
			config.set(list);
	}
	
	private static void DropEntityLoot(Entity entity, Player player, PoolLevel coinPool)
	{
		
		if(!Config.COMMON.enableEntityDrops.get())
			return;
		
		generateLootTables();
		
		//LightmansCurrency.LOGGER.info("Dropping entity loot level " + coinPool);
		
		LootTable table = LootTable.lootTable().build();
		LootContext.Builder contextBuilder = new LootContext.Builder((ServerLevel)entity.level);
		//Add the KilledByPlayer condition to the Loot Context
		if(player != null)
			contextBuilder.withParameter(LootContextParams.KILLER_ENTITY, player)
			.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
		
		LootContext context = contextBuilder.create(new LootContextParamSet.Builder().optional(LootContextParams.LAST_DAMAGE_PLAYER).optional(LootContextParams.KILLER_ENTITY).build());
		
		try {
			
			//Boss loot done separately due to loops and exclusiveness.
			if(coinPool == PoolLevel.BOSS_COPPER)
			{
				//Drop copper boss loot
				for(Builder builder : ENTITY_LOOT_BOSS_COPPER)
				{
					table.addPool(builder.build());
				}
				//Generate the loot 
				SpawnLootDrops(entity, table.getRandomItems(context));
				return;
			}
			else if(coinPool == PoolLevel.BOSS_IRON)
			{
				//Drop iron boss loot
				for(Builder builder : ENTITY_LOOT_BOSS_IRON)
				{
					table.addPool(builder.build());
				}
				//Generate the loot 
				SpawnLootDrops(entity, table.getRandomItems(context));
				return;
			}
			else if(coinPool == PoolLevel.BOSS_GOLD)
			{
				//Drop gold boss loot
				for(Builder builder : ENTITY_LOOT_BOSS_GOLD)
				{
					table.addPool(builder.build());
				}
				//Generate the loot 
				SpawnLootDrops(entity, table.getRandomItems(context));
				return;
			}
			else if(coinPool == PoolLevel.BOSS_EMERALD)
			{
				//Drop emerald boss loot
				for(Builder builder : ENTITY_LOOT_BOSS_EMERALD)
				{
					table.addPool(builder.build());
				}
				//Generate the loot 
				SpawnLootDrops(entity, table.getRandomItems(context));
				return;
			}
			else if(coinPool == PoolLevel.BOSS_DIAMOND)
			{
				//Drop diamond boss loot
				for(Builder builder : ENTITY_LOOT_BOSS_DIAMOND)
				{
					table.addPool(builder.build());
				}
				//Generate the loot 
				SpawnLootDrops(entity, table.getRandomItems(context));
				return;
			}
			else if(coinPool == PoolLevel.BOSS_NETHERITE)
			{
				//Drop netherite boss loot
				for(Builder builder : ENTITY_LOOT_BOSS_NETHERITE)
				{
					table.addPool(builder.build());
				}
				//Generate the loot 
				SpawnLootDrops(entity, table.getRandomItems(context));
				return;
			}
			
			//LightmansCurrency.LOGGER.debug("Added " + coinPool + " level entity loot to the " + name + " loot entry.");
			table.addPool(ENTITY_LOOT_COPPER.build());
			if(coinPool != PoolLevel.COPPER)
			{
				table.addPool(ENTITY_LOOT_IRON.build());
				if(coinPool != PoolLevel.IRON)
				{
					table.addPool(ENTITY_LOOT_GOLD.build());
					if(coinPool != PoolLevel.GOLD)
					{
						table.addPool(ENTITY_LOOT_EMERALD.build());
						if(coinPool != PoolLevel.EMERALD)
						{
							table.addPool(ENTITY_LOOT_DIAMOND.build());
							if(coinPool != PoolLevel.DIAMOND)
								table.addPool(ENTITY_LOOT_NETHERITE.build());
						}
					}
				}
			}
			
			SpawnLootDrops(entity, table.getRandomItems(context));
		
		} catch(Exception e) { LightmansCurrency.LogError("Error spawning coin drops!", e); }
		
	}
	
	public static List<ItemStack> GetRandomChestLoot(PoolLevel coinPool, LootContext context) {
		
		generateLootTables();
		
		try {
			
			if(coinPool == PoolLevel.COPPER)
			{
				LootTable table = LootTable.lootTable().build();
				table.addPool(CHEST_LOOT_COPPER.build());
				return safelyGetResults(table, context);
			}
			else if(coinPool == PoolLevel.IRON)
			{
				LootTable table = LootTable.lootTable().build();
				table.addPool(CHEST_LOOT_IRON.build());
				return safelyGetResults(table, context);
			}
			else if(coinPool == PoolLevel.GOLD)
			{
				LootTable table = LootTable.lootTable().build();
				table.addPool(CHEST_LOOT_GOLD.build());
				return safelyGetResults(table, context);
			}
			else if(coinPool == PoolLevel.EMERALD)
			{
				LootTable table = LootTable.lootTable().build();
				table.addPool(CHEST_LOOT_EMERALD.build());
				return safelyGetResults(table, context);
			}
			else if(coinPool == PoolLevel.DIAMOND)
			{
				LootTable table = LootTable.lootTable().build();
				table.addPool(CHEST_LOOT_DIAMOND.build());
				return safelyGetResults(table, context);
			}
			else if(coinPool == PoolLevel.NETHERITE)
			{
				LootTable table = LootTable.lootTable().build();
				table.addPool(CHEST_LOOT_NETHERITE.build());
				return safelyGetResults(table, context);
			}
			else
			{
				LightmansCurrency.LogError("Attempting to get random chest loot from an invalid chest pool level of '" + (coinPool == null ? "NULL" : coinPool.toString()) + "'");
				return new ArrayList<>();
			}
			
		} catch(Exception e) {
			LightmansCurrency.LogError("Error spawning chest coin drops!", e);
			return new ArrayList<>();
		}
	}
	
	@SuppressWarnings("deprecation")
	private static List<ItemStack> safelyGetResults(LootTable table, LootContext context) {
		List<ItemStack> results = new ArrayList<>();
		//Call getRandomItems(LootContext,Consumer<ItemStack>) to keep it from being modified by the GLM's and getting stuck in an infinite loop.
		table.getRandomItems(context, results::add);
		return results;
	}
	
	public static PoolLevel GetChestPoolLevel(String lootTable) {
		if(Config.COMMON.copperChestDrops.get().contains(lootTable))
			return PoolLevel.COPPER;
		if(Config.COMMON.ironChestDrops.get().contains(lootTable))
			return PoolLevel.IRON;
		if(Config.COMMON.goldChestDrops.get().contains(lootTable))
			return PoolLevel.GOLD;
		if(Config.COMMON.emeraldChestDrops.get().contains(lootTable))
			return PoolLevel.EMERALD;
		if(Config.COMMON.diamondChestDrops.get().contains(lootTable))
			return PoolLevel.DIAMOND;
		if(Config.COMMON.netheriteChestDrops.get().contains(lootTable))
			return PoolLevel.NETHERITE;
		
		if(EXTERNAL_CHEST_ENTRIES.containsKey(lootTable))
			return EXTERNAL_CHEST_ENTRIES.get(lootTable);
		
		return null;
	}
	
	private static void SpawnLootDrops(Entity entity, List<ItemStack> lootDrops)
	{
		//LightmansCurrency.LOGGER.info("Spawning " + lootDrops.size() + " coin drops.");
		InventoryUtil.dumpContents(entity.level, entity.blockPosition(), lootDrops);
	}
	
	/**
	 * Adds the given entity's loot table to the list so that it will drop coins in addition to its already given loot.
	 * @param resource String format of the loot tables ResourceLocation (e.g. "minecraft:entities/zombie"), or of the entities id (e.g. "minecraft:sheep")
	 * @param coinPool The highest level coin that the entity should be allowed to drop.
	 */
	public static void AddEntityCoinPoolToTable(String resource, PoolLevel coinPool)
	{
		EXTERNAL_ENTITY_ENTRIES.put(resource, coinPool);
	}
	
	/**
	 * Adds the given chest's loot table to the list so that it will spawn coins in addition to its already given loot.
	 * @param resource String format of the loot tables ResourceLocation (e.g. "minecraft:chests/buried_treasure")
	 * @param coinPool The highest level coin that the chest should spawn. Should not include the BOSS pool levels, as those are for entities only.
	 */
	public static void AddChestCoinPoolToTable(String resource, PoolLevel coinPool)
	{
		if(coinPool.level > PoolLevel.NETHERITE.level)
		{
			LightmansCurrency.LogError("Attempted to add a chest to the coin pool at level " + coinPool.name() + ", but that level is not valid for chests.");
			return;
		}
		EXTERNAL_CHEST_ENTRIES.put(resource, coinPool);
	}
	
	
	private static Builder GenerateEntityCoinPool(Item item, float min, float max, float chance, String name, boolean requirePlayerKill)
	{
		
		Builder lootPoolBuilder = LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1))
				.add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))).apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0f, 1f))))
				.name(name);
		
		//Require that the player killed it (usually only disabled for bosses)
		if(requirePlayerKill)
			lootPoolBuilder.when(LootItemKilledByPlayerCondition.killedByPlayer());
		//Add a random chance to the loot (if applicable, usually only disabled for bosses)
		if(chance < 1.0f)
			lootPoolBuilder.when(LootItemRandomChanceWithLootingCondition.randomChanceAndLootingBoost(chance, LOOTING_MODIFIER));
		
		return lootPoolBuilder;
		
	}
	
	private static Builder GenerateChestCoinPool(ChestLootEntryData[] lootEntries, float minRolls, float maxRolls, String name)
	{
		
		Builder lootPoolBuilder = LootPool.lootPool()
				.setRolls(UniformGenerator.between(minRolls, maxRolls))
				.name(name);
		
		//Add each loot entry
		for(ChestLootEntryData entry : lootEntries)
		{
			lootPoolBuilder.add(LootItem.lootTableItem(entry.item).apply(SetItemCountFunction.setCount(UniformGenerator.between(entry.minCount, entry.maxCount))).setWeight(entry.weight));
		}
		
		return lootPoolBuilder;
		
	}
	
	private static class ChestLootEntryData
	{
		public final Item item;
		public final float minCount;
		public final float maxCount;
		public final int weight;
		
		public ChestLootEntryData(Item item, float minCount, float maxCount, int weight)
		{
			this.item = item;
			this.minCount = minCount;
			this.maxCount = maxCount;
			this.weight = weight;
		}
		
		public static ChestLootEntryData COPPER = new ChestLootEntryData(ModItems.COIN_COPPER.get(), 1, 10, 1);
		public static ChestLootEntryData IRON = new ChestLootEntryData(ModItems.COIN_IRON.get(), 1, 10, 2);
		public static ChestLootEntryData GOLD = new ChestLootEntryData(ModItems.COIN_GOLD.get(), 1, 10, 3);
		public static ChestLootEntryData EMERALD = new ChestLootEntryData(ModItems.COIN_EMERALD.get(), 1, 10, 4);
		public static ChestLootEntryData DIAMOND = new ChestLootEntryData(ModItems.COIN_DIAMOND.get(), 1, 8, 5);
		public static ChestLootEntryData NETHERITE = new ChestLootEntryData(ModItems.COIN_NETHERITE.get(), 1, 3, 6);
		
	}
	
	
}
