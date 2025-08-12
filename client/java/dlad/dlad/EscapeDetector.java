package dlad.dlad;

import dlad.dlad.mixin.client.GameRendererAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.network.PlayerListEntry;
import java.util.*;

public class EscapeDetector {
    private static int tickCounter = 0;
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
        //MUST be obfuscated if we need this shitty to be working for more than 3 days
        });

        HudRenderCallback.EVENT.register((ctx, partial) -> {
            if (!DladClient.Config.getFeatureState(0)) return;
            if (client.player == null || client.world == null) return;

            int w = client.getWindow().getScaledWidth();
            int h = client.getWindow().getScaledHeight();
            Vec3d camPos = client.gameRenderer.getCamera().getPos();

            float tickDelta = partial.getTickDelta(true);
            double fovDeg = ((GameRendererAccessor) client.gameRenderer)
                    .dlad$invokeGetFov(client.gameRenderer.getCamera(), tickDelta, true);
            float fovRad = (float) Math.toRadians(fovDeg);
            float tanFOV = (float) Math.tan(fovRad / 2f);
            float aspect = (float) w / h;

            long now = System.currentTimeMillis();

            for (Map.Entry<LivingEntity, Long> ent : lastDetected.entrySet()) {
                LivingEntity e = ent.getKey();
                if (now - ent.getValue() > 5000) continue;
                Vec3d eyePos = e.getCameraPosVec(partial.getTickDelta(true));
                Vec3d delta  = eyePos.subtract(camPos);
                float dx = (float)delta.x, dy = (float)delta.y, dz = (float)delta.z;

                float yaw = (float)Math.toRadians(client.gameRenderer.getCamera().getYaw());
                float pitch = (float)Math.toRadians(client.gameRenderer.getCamera().getPitch());
                float cosY = (float)Math.cos(-yaw), sinY = (float)Math.sin(-yaw);
                float x1 = dx * cosY - dz * sinY;
                float z1 = dx * sinY + dz * cosY;
                float cosP = (float)Math.cos(-pitch), sinP = (float)Math.sin(-pitch);
                float y2 = dy * cosP - z1 * sinP;
                float z2 = dy * sinP + z1 * cosP;
                if (z2 <= 0) continue;

                float ndcX = (x1 / z2) / (tanFOV * aspect);
                float ndcY = (y2 / z2) / tanFOV;
                int sx = (int)(((1f - ndcX) * 0.5f) * w);
                int sy = (int)(((1f - ndcY) * 0.5f) * h);

                int size = 8;
                ctx.fill(
                        sx - size, sy - size,
                        sx + size, sy + size,
                        0x8000FF00  // semi-transparent blue
                );
                MinecraftClient mc = MinecraftClient.getInstance();
                TextRenderer tr = mc.textRenderer;
                String ex = "!!!!!";
                int wex = tr.getWidth(ex);
                int hex = tr.fontHeight;
                ctx.drawText(
                        tr,
                        ex,
                        sx - wex/2,
                        sy - hex/2,
                        0xFFFF0000,
                        true
                );
            }
        });
    }

}
