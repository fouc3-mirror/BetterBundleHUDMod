package betterbundle;

import betterbundle.gui.BundleCategory;
import net.fabricmc.api.ClientModInitializer;

public class BetterBundleMod implements ClientModInitializer {
    public static final String MOD_ID = "better-bundle";

    @Override
    public void onInitializeClient() {
        BundleCategory.registerCategoryItems();
    }
}
