package dlad.dlad;
import dlad.dlad.mixin.client.GameRendererAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SausageFinder {
    public static void register() {
        MinecraftClient client = MinecraftClient.getInstance();

        HudRenderCallback.EVENT.register((ctx, partialTicks) -> {
            if (!DladClient.Config.getFeatureState(3)) return;
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

            BlockPos base = BlockPos.ofFloored(camPos);
            int radiusH = DladClient.Config.getSausageRadius(), radiusV = DladClient.Config.getSausageRadius();

            for (int dx = -radiusH; dx <= radiusH; dx++) {
                for (int dz = -radiusH; dz <= radiusH; dz++) {
                    for (int dy = -radiusV; dy <= radiusV; dy++) {
                        //Obfuscated, yup, I'm aware this is a bruteforce of a check, but it works. Can't disclose what we check or how we check it


                        Vec3d signPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        Vec3d delta = signPos.subtract(camPos);
                        float dx1 = (float) delta.x, dy1 = (float) delta.y, dz1 = (float) delta.z;
                        float yaw   = (float) Math.toRadians(client.gameRenderer.getCamera().getYaw());
                        float pitch = (float) Math.toRadians(client.gameRenderer.getCamera().getPitch());
                        float cosY = (float) Math.cos(-yaw), sinY = (float) Math.sin(-yaw);
                        float x1 = dx1 * cosY - dz1 * sinY;
                        float z1 = dx1 * sinY + dz1 * cosY;
                        float cosP = (float) Math.cos(-pitch), sinP = (float) Math.sin(-pitch);
                        float y2 = dy1 * cosP - z1 * sinP;
                        float z2 = dy1 * sinP + z1 * cosP;
                        if (z2 <= 0) continue;

                        float ndcX = (x1 / z2) / (tanFOV * aspect);
                        float ndcY = (y2 / z2) / tanFOV;
                        int sx = (int) (((1f - ndcX) * 0.5f) * w);
                        int sy = (int) (((1f - ndcY) * 0.5f) * h);

                        int size = DladClient.Config.getSausageSize();
                        ctx.fill(
                                sx - size, sy - size,
                                sx + size, sy + size,
                                0x80FFFFFF
                        );

                        String label = ":tm:";
                        int textW = client.textRenderer.getWidth(label);
                        int textH = client.textRenderer.fontHeight;
                        ctx.drawText(
                                client.textRenderer,
                                Text.literal(label).formatted(Formatting.WHITE),
                                sx - textW/2,
                                sy - textH/2,
                                0xFF000000,
                                false
                        );
                    }
                }
            }
        });
    }
}
