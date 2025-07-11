package com.scubakay;

import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entrypoint
public class TemplateMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("template");

    @Override
    public void onInitialize() {
        LOGGER.info("Hello world!");
    }
}