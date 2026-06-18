package betterbundle.mixin;

import betterbundle.gui.BundleCategory;
import betterbundle.gui.BundlePanelInteraction;
import betterbundle.gui.BundlePanelRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;

        // Bulk-insert: space+left anywhere starts the timer (0.05s to activate)
        if (button == 0 && isSpaceDown()) {
            BundlePanelInteraction.startBulkInsert();
        }

        // Space+Click works on ALL container screens
        Slot hovered = self.focusedSlot;
        if (hovered != null && hovered.hasStack()) {
            boolean handled = BundlePanelInteraction.handleSpaceClick(hovered);
            if (handled) { cir.setReturnValue(true); return; }
        }

        // For non-recipe-book screens: handle toggle, category, search bar
        if (!(((Object) this) instanceof RecipeBookScreen)) {
            int bx = self.x + self.backgroundWidth;
            int by = self.y + 5;
            if (mouseX >= bx && mouseX < bx + 20 && mouseY >= by && mouseY < by + 20) {
                BundlePanelRenderer.toggleVisible();
                cir.setReturnValue(true);
                return;
            }

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

            if (BundlePanelRenderer.isEffectivelyVisible()
                    && BundlePanelRenderer.isInsideSearchBar(mouseX, mouseY, self.x, self.y, self.backgroundHeight)) {
                BundlePanelRenderer.searchFocused = true;
                cir.setReturnValue(true);
                return;
            }

            BundlePanelRenderer.searchFocused = false;
        }

        if (!BundlePanelRenderer.isEffectivelyVisible()) return;

        // Cursor has items + click anywhere in panel (except category buttons) → insert
        ItemStack cursor = self.getScreenHandler().getCursorStack();
        if (!cursor.isEmpty() && isInsidePanelBounds(mouseX, mouseY, self.x, self.y, self.backgroundHeight)) {
            BundleCategory cat = BundlePanelRenderer.getCategoryAt(mouseX, mouseY, self.x, self.y, self.backgroundHeight);
            if (cat == null) {
                boolean handled = BundlePanelInteraction.handlePanelInsert(button);
                if (handled) cir.setReturnValue(true);
            }
        }

        if (BundlePanelInteraction.isInsidePanel(mouseX, mouseY, self.x, self.y, self.backgroundHeight)) {
            if (cursor.isEmpty()) {
                // In 1.21.2, we need to get modifiers from GLFW directly
                int modifiers = getModifiers();
                boolean handled = BundlePanelInteraction.handlePanelClick(
                        mouseX, mouseY, button, modifiers, self.x, self.y, self);
                if (handled) cir.setReturnValue(true);
            }
        }
    }

    private static int getModifiers() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        int modifiers = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            modifiers |= 1;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
            modifiers |= 2;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) {
            modifiers |= 4;
        }
        return modifiers;
    }

    private static boolean isInsidePanelBounds(double mx, double my, int leftPos, int topPos, int imageHeight) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        int panelY = topPos;
        int searchH = BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3;
        int gridH = BundlePanelRenderer.PADDING * 2
                + BundlePanelRenderer.VISIBLE_ROWS * BundlePanelRenderer.SLOT_SIZE
                + (BundlePanelRenderer.VISIBLE_ROWS - 1) * BundlePanelRenderer.SLOT_SPACING;
        int panelH = Math.min(imageHeight, searchH + gridH) + 24;
        return mx >= panelX && mx <= panelX + pw && my >= panelY && my <= panelY + panelH;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        BundlePanelInteraction.stopBulkInsert();
        lastBulkSlot = -1;
        if (!BundlePanelRenderer.isEffectivelyVisible()) return;
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (BundlePanelInteraction.isInsidePanel(mouseX, mouseY,
                self.x, self.y, self.backgroundHeight)) {
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

    private int lastBulkSlot = -1;

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double dx, double dy,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (!BundlePanelInteraction.isBulkInsertActive()) return;
        if (!isSpaceDown()) { BundlePanelInteraction.stopBulkInsert(); return; }

        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        Slot hovered = self.focusedSlot;
        if (hovered != null && hovered.hasStack() && hovered.getIndex() != lastBulkSlot) {
            lastBulkSlot = hovered.getIndex();
            BundlePanelInteraction.handleSpaceClick(hovered);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!BundlePanelRenderer.isEffectivelyVisible()) return;
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (BundlePanelInteraction.isInsidePanel(mouseX, mouseY,
                self.x, self.y, self.backgroundHeight)) {
            boolean handled = BundlePanelInteraction.handleScroll(mouseX, mouseY, scrollY,
                    self.x, self.y, self.backgroundHeight);
            if (handled) cir.setReturnValue(true);
        }
    }

    private static boolean isSpaceDown() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
    }
}