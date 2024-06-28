package com.drimoz.punchthemall;

import appeng.api.ids.AETags;
import com.drimoz.punchthemall.core.event.EventHandler;
import com.drimoz.punchthemall.core.model.Interaction;
import com.drimoz.punchthemall.core.registry.InteractionLoader;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import com.drimoz.punchthemall.core.util.PTALoggers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import org.slf4j.Logger;

import java.util.Set;

@Mod(PunchThemAll.MOD_ID)
public class PunchThemAll
{
    public static final String MOD_ID = "pta";
    public static final String MOD_NAME = "PunchThemAll";
    public static final String FILE_DESTINATION = "punchthemall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PunchThemAll()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        PTALoggers.infoModCompleted();
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        PTALoggers.infoRegisteredModule("Common Setup");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        PTALoggers.infoRegisteredModule("Client Setup");
    }

    @SubscribeEvent
    public void onServerStartup(ServerStartingEvent event) {
        InteractionLoader.initInteractions();

        PTALoggers.infoRegisteredModule("Server Starting");

    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftForge.EVENT_BUS.register(EventHandler.class);

        PTALoggers.infoRegisteredModule("Server Started");

    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            ItemStack itemStack = event.getItemStack();
            displayItemTags(itemStack);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void displayItemTags(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            Item item = itemStack.getItem();
            Set<ResourceLocation> tagKeys = item.builtInRegistryHolder().tags().map(TagKey::location).collect(java.util.stream.Collectors.toSet());
            if (tagKeys.isEmpty()) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("No tags found for this item"), false);
            } else {
                for (ResourceLocation tag : tagKeys) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("Tag: " + tag.toString()), false);
                }
            }
        }
    }
}
