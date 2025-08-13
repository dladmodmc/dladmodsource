package dlad.dlad;

import dlad.dlad.mixin.client.GameRendererAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public class MeteorDetector {
    public static void register() {
        MinecraftClient client = MinecraftClient.getInstance();

        HudRenderCallback.EVENT.register((ctx, partialTicks) -> {
            if (!DladClient.Config.getFeatureState(4)) return;
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

            var area = client.player.getBoundingBox().expand(256.0);
            for (InteractionEntity e : client.world.getEntitiesByClass(InteractionEntity.class, area, ent -> true)) {
                if (e.getBoundingBox().getLengthX() != 3.0f && e.getBoundingBox().getLengthY() != 4.0f) continue;
                Vec3d target = e.getPos().add(0.0, e.getBoundingBox().getLengthY() * 0.5, 0.0);
                Vec3d delta  = target.subtract(camPos);
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

                int size = DladClient.Config.getMeteorSize();
                ctx.fill(
                        sx - size, sy - size,
                        sx + size, sy + size,
                        0x80FF0000
                );

                TextRenderer tr = client.textRenderer;
                String icon = "☄";
                int tw = tr.getWidth(icon);
                int th = tr.fontHeight;

                ctx.drawText(
                        tr,
                        Text.literal(icon).formatted(Formatting.WHITE),
                        sx - tw / 2,
                        sy - th / 2,
                        0xFFFF0000, // solid red glyph color
                        false
                );
            }
        });
    }
}
