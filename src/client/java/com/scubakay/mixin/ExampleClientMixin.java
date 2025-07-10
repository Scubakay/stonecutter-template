package com.scubakay.mixin;

import com.scubakay.TemplateMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnusedMixin")
@Mixin(MinecraftClient.class)
public class ExampleClientMixin {
	@Inject(at = @At("HEAD"), method = "run")
	private void run(CallbackInfo info) {
		// This code is injected into the start of MinecraftClient.run()V
		TemplateMod.LOGGER.info("Running client!");
	}
}