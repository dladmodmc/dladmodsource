package dlad.dlad.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
//requiered as fuckass minecraft keeps getFov private for whatever reason
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    double dlad$invokeGetFov(Camera camera, float tickDelta, boolean changingFov);
}
