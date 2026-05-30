package betterbundle.mixin;

import betterbundle.gui.BundlePanelInteraction;
import betterbundle.gui.BundlePanelRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        // Space+Click works on ALL container screens (chests, furnaces, etc.)
        Slot hovered = self.hoveredSlot;
        if (hovered != null && hovered.hasItem()) {
            boolean handled = BundlePanelInteraction.handleSpaceClick(hovered);
            if (handled) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Panel interactions only on InventoryScreen
        if (!(((Object) this) instanceof InventoryScreen)) return;
        if (!BundlePanelRenderer.isEffectivelyVisible()) return;

        double mouseX = event.x();
        double mouseY = event.y();

        if (BundlePanelInteraction.isInsidePanel(mouseX, mouseY,
                self.leftPos, self.topPos, self.imageHeight)) {

            boolean handled = BundlePanelInteraction.handlePanelClick(mouseX, mouseY, event.button(), event.modifiers(),
                    self.leftPos, self.topPos);
            if (handled) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!(((Object) this) instanceof InventoryScreen)) return;
        if (!BundlePanelRenderer.isEffectivelyVisible()) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        if (BundlePanelInteraction.isInsidePanel(event.x(), event.y(),
                self.leftPos, self.topPos, self.imageHeight)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!(((Object) this) instanceof InventoryScreen)) return;
        if (!BundlePanelRenderer.isEffectivelyVisible()) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        if (BundlePanelInteraction.isInsidePanel(mouseX, mouseY,
                self.leftPos, self.topPos, self.imageHeight)) {

            boolean handled = BundlePanelInteraction.handleScroll(mouseX, mouseY, scrollY,
                    self.leftPos, self.topPos, self.imageHeight);
            if (handled) {
                cir.setReturnValue(true);
            }
        }
    }
}
