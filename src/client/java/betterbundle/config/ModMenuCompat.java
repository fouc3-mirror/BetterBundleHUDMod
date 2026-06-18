package betterbundle.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * ModMenu integration for BetterBundleHUD.
 * Provides a config screen through ModMenu.
 */
public class ModMenuCompat implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createConfigScreen;
    }
    
    private Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.better-bundle.title"));
        
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.better-bundle.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        BetterBundleConfig config = BetterBundleConfig.getInstance();
        
        // Show stack overlay option (includes count and durability bar)
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.better-bundle.option.showStackOverlay"),
                config.showStackOverlay()
        )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.better-bundle.option.showStackOverlay.tooltip"))
                .setSaveConsumer(value -> config.setShowStackOverlay(value))
                .build());
        
        // Stack same NBT items option
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.better-bundle.option.stackSameNbt"),
                config.stackSameNbt()
        )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.better-bundle.option.stackSameNbt.tooltip"))
                .setSaveConsumer(value -> config.setStackSameNbt(value))
                .build());
        
        // Toggle button position option
        general.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("config.better-bundle.option.toggleButtonPosition"),
                BetterBundleConfig.ToggleButtonPosition.class,
                config.toggleButtonPosition()
        )
                .setDefaultValue(BetterBundleConfig.ToggleButtonPosition.RIGHT)
                .setEnumNameProvider(pos -> Text.translatable("config.better-bundle.option.toggleButtonPosition." + pos.name().toLowerCase()))
                .setTooltip(Text.translatable("config.better-bundle.option.toggleButtonPosition.tooltip"))
                .setSaveConsumer(value -> config.setToggleButtonPosition(value))
                .build());
        
        // Panel visibility option
        general.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("config.better-bundle.option.panelVisible"),
                config.panelVisible()
        )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.better-bundle.option.panelVisible.tooltip"))
                .setSaveConsumer(value -> config.setPanelVisible(value))
                .build());
        
        // Bundle in bundle mode option
        general.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("config.better-bundle.option.bundleInBundleMode"),
                BetterBundleConfig.BundleInBundleMode.class,
                config.bundleInBundleMode()
        )
                .setDefaultValue(BetterBundleConfig.BundleInBundleMode.NOT_ALLOWED)
                .setEnumNameProvider(mode -> Text.translatable("config.better-bundle.option.bundleInBundleMode." + mode.name().toLowerCase()))
                .setTooltip(Text.translatable("config.better-bundle.option.bundleInBundleMode.tooltip"))
                .setSaveConsumer(value -> config.setBundleInBundleMode(value))
                .build());
        
        return builder.build();
    }
}