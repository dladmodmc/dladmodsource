package dlad.dlad;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.Random;

public class AutoFisher {
    private static final Random RANDOM = new Random();
    private static boolean isFishing = false;
    private static boolean fishCaught = false;
    private static int tickCounter = 0;
    private static int waitTicks = 0;
    private static boolean readyToReelIn = false;
    private static int bobberSettleTicks = 0;

    public static void register() {

        boolean[] wasEnabled = {false};

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try {
                // Check if the feature is enabled
                if (DladClient.ModGate.isNotActive()) return;
                boolean isEnabled = DladClient.Config.getFeatureState(2);

                // Log when the feature is toggled
                if (isEnabled != wasEnabled[0]) {
                    wasEnabled[0] = isEnabled;
                }

                if (!isEnabled) return;
                if (c.player == null || c.world == null) return;


                //This part of the code has been removed to prevent patches, please understand that we can't release this fully as it would expose sensitive information if seen by staff 



            } catch (Exception e) {
                // Log the error but don't crash the game
            }
        });
    }


    private static void simulateRightClick(MinecraftClient client) {
        try {
            if (client.player != null) {
                assert client.interactionManager != null;
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            }
        } catch (Exception ignored){}
    }

    private static boolean isFishBiting(FishingBobberEntity bobber) {
        if (bobber == null) return false;
        //This part of the code has been removed to prevent patches, please understand that we can't release this fully as it would expose sensitive information if seen by staff 
    }
}