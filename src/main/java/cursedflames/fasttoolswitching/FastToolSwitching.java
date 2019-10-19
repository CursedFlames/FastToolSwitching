package cursedflames.fasttoolswitching;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Properties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod(FastToolSwitching.MODID)
public class FastToolSwitching {
	public static final String MODID = "fasttoolswitching";
	// Directly reference a log4j logger.
	public static final Logger logger = LogManager.getLogger();

	public FastToolSwitching() {
		MinecraftForge.EVENT_BUS.register(FastToolSwitching.class);
		PacketHandler.registerMessages();
	}
	
	// have to do this since cooldown reset is only applied after the equipment change event is fired
	private static final Map<UUID, Integer> entitiesToReset = new HashMap<>();

	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (event.getSlot() == EquipmentSlotType.MAINHAND) {
			LivingEntity entity = event.getEntityLiving();
			if (!(entity instanceof PlayerEntity))
				return;
			PlayerEntity player = (PlayerEntity) entity;
			entitiesToReset.put(player.getUniqueID(), player.ticksSinceLastSwing);
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
					new PacketUpdateToolCooldown(player.ticksSinceLastSwing));
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent event) {
		PlayerEntity player = event.player;
		if (event.phase == Phase.END && !player.world.isRemote) {
			UUID toRemove = null;
			for (Entry<UUID, Integer> pair : entitiesToReset.entrySet()) {
				if (pair.getKey().equals(player.getUniqueID())) {
					toRemove = pair.getKey();
					player.ticksSinceLastSwing = pair.getValue();
					break;
				}
			}
			if (toRemove != null)
				entitiesToReset.remove(toRemove);
		}
	}
}