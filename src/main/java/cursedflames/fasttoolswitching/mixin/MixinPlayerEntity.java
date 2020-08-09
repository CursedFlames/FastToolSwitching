package cursedflames.fasttoolswitching.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
	// Janky hack to block `resetLastAttackedTicks` calls from `tick()` without using redirects
	private boolean fts_isInTick = false;

	@Inject(method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/player/PlayerEntity;resetLastAttackedTicks()V"
			))
	private void beforeResetLastAttackedTicks(CallbackInfo ci) {
		fts_isInTick = true;
	}

	@Inject(method = "resetLastAttackedTicks",
			at = @At("HEAD"),
			cancellable = true)
	private void onResetLastAttackedTicks(CallbackInfo ci) {
		if (fts_isInTick) {
			// Shouldn't be necessary here as well, but just in case
			fts_isInTick = false;
			ci.cancel();
		}
	}

	@Inject(method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/entity/player/PlayerEntity;resetLastAttackedTicks()V",
					shift = At.Shift.AFTER
			))
	private void afterResetLastAttackedTicks(CallbackInfo ci) {
		fts_isInTick = false;
	}
}
