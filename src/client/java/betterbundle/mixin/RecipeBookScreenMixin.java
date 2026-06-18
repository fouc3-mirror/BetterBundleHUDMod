package betterbundle.mixin;

import betterbundle.config.BetterBundleConfig;
import betterbundle.config.BetterBundleConfig.ToggleButtonPosition;
import betterbundle.gui.BundleCategory;
import betterbundle.gui.BundlePanelRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;

        // Toggle button - handle based on position config
        ToggleButtonPosition position = BetterBundleConfig.getInstance().toggleButtonPosition();
        if (position != ToggleButtonPosition.OFF) {
            int bx, by;
            switch (position) {
                case LEFT:
                    bx = self.x - 20;
                    by = self.y + 5;
                    break;
                case TOP:
                    bx = self.x + self.backgroundWidth - 20;
                    by = self.y - 20;
                    break;
                case RIGHT:
                default:
                    bx = self.x + self.backgroundWidth;
                    by = self.y + 5;
                    break;
            }
            if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by && mouseY < by + 20) {
                BundlePanelRenderer.toggleVisible();
                cir.setReturnValue(true);
                return;
            }
        }

        // Category button
        if (BundlePanelRenderer.isEffectivelyVisible()) {
            BundleCategory cat = BundlePanelRenderer.getCategoryAt(mouseX, mouseY, self.x, self.y, self.backgroundHeight);
            if (cat != null) {
                BundlePanelRenderer.currentCategory = cat;
                BundlePanelRenderer.searchQuery = "";
                BundlePanelRenderer.scrollToTop();
                cir.setReturnValue(true);
                return;
            }
        }

        // Search bar click
        if (BundlePanelRenderer.isEffectivelyVisible()
                && BundlePanelRenderer.isInsideSearchBar(mouseX, mouseY, self.x, self.y, self.backgroundHeight)) {
            BundlePanelRenderer.searchFocused = true;
            cir.setReturnValue(true);
            return;
        }

        BundlePanelRenderer.searchFocused = false;
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (BundlePanelRenderer.searchFocused) {
            BundlePanelRenderer.onCharTyped(codePoint);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (BundlePanelRenderer.searchFocused) {
            BundlePanelRenderer.onSearchKeyPress(keyCode);
            cir.setReturnValue(true);
        }
    }
}