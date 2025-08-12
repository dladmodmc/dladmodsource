package dlad.dlad;

import dlad.dlad.mixin.client.GameRendererAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Objects;

public class GuardDetector {
    public static void register() {
        MinecraftClient client = MinecraftClient.getInstance();

        HudRenderCallback.EVENT.register((ctx, partialTicks) -> {
            if (!DladClient.Config.getFeatureState(1)) return;
            if (client.player == null || client.world == null) return;
            if (DladClient.ModGate.isNotActive()) return;
            int w = client.getWindow().getScaledWidth();
            int h = client.getWindow().getScaledHeight();

            Vec3d camPos = client.gameRenderer.getCamera().getPos();
            float tickDelta = partialTicks.getTickDelta(true);
            double fovDeg = ((GameRendererAccessor) client.gameRenderer)
                    .dlad$invokeGetFov(client.gameRenderer.getCamera(), tickDelta, true);
            float fovRad = (float) Math.toRadians(fovDeg);
            float tanFOV = (float) Math.tan(fovRad / 2f);
            float aspect = (float) w / h;

            for (PlayerEntity e : client.world.getPlayers()) {
                if (e == client.player) continue;
                //Obfuscated, we can't disclose what we check here to prevent to see who's players are guard

                Vec3d eyePos = e.getCameraPosVec(partialTicks.getTickDelta(true));
                Vec3d delta = eyePos.subtract(camPos);
                float dx = (float) delta.x, dy = (float) delta.y, dz = (float) delta.z;
                float yaw   = (float) Math.toRadians(client.gameRenderer.getCamera().getYaw());
                float pitch = (float) Math.toRadians(client.gameRenderer.getCamera().getPitch());
                float cosY = (float) Math.cos(-yaw), sinY = (float) Math.sin(-yaw);
                float x1 = dx * cosY - dz * sinY;
                float z1 = dx * sinY + dz * cosY;
                float cosP = (float) Math.cos(-pitch), sinP = (float) Math.sin(-pitch);
                float y2 = dy * cosP - z1 * sinP;
                float z2 = dy * sinP + z1 * cosP;
                if (z2 <= 0) continue;

                float ndcX = (x1 / z2) / (tanFOV * aspect);
                float ndcY = (y2 / z2) / tanFOV;
                int sx = (int) (((1f - ndcX) * 0.5f) * w);
                int sy = (int) (((1f - ndcY) * 0.5f) * h);

                float dist = (float) Math.sqrt(client.player.squaredDistanceTo(e));
                float t = Math.min(dist / 30f, 1f);

                int colorInt = getColorInt(style, t);

                int size = 8;
                ctx.fill(
                        sx - size, sy - size,
                        sx + size, sy + size,
                        colorInt
                );


                int alpha = Math.round(128 + t * (255 - 128));
                int textColor = (alpha << 24) | 0x00FF0000;


                TextRenderer tr = client.textRenderer;
                String ex = "⚠";
                int wex = tr.getWidth(ex);
                int hex = tr.fontHeight;
                ctx.drawText(
                        tr, Text.literal(ex).formatted(Formatting.WHITE),
                        sx - wex/2, sy - hex/2,
                        textColor, false
                );
            }
        });
    }

    private static int getColorInt(String style, float t) {
        int farR, farG, farB;
        if ("blue".equals(style)) {
            farR = 0;     farG = 0;   farB = 255;
        } else { // dark_aqua
            farR = 0;     farG = 170; farB = 170;
        }
        int nearR = 255, nearG = 0, nearB = 0;

        // interpolate each channel
        int r = (int)((1 - t) * nearR + t * farR);
        int g = (int)((1 - t) * nearG + t * farG);
        int b = (int)((1 - t) * nearB + t * farB);
        int a = (int)((1 - t) *  64 + t * 128);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
