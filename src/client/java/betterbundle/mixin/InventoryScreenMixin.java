package betterbundle.mixin;

import betterbundle.gui.BundlePanelRenderer;
import betterbundle.util.MatrixOperations;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        BundlePanelRenderer.render(graphics,
                self.x,
                self.y,
                self.backgroundHeight,
                mouseX, mouseY);

        // Enlarge the bundle icon for the hovered panel item
        int bundleSlot = BundlePanelRenderer.getHoveredBundleSlot();
        if (bundleSlot >= 0) {
            Slot slot = self.getScreenHandler().getSlot(bundleSlot);
            if (slot != null && slot.hasStack()) {
                int sx = self.x + slot.x;
                int sy = self.y + slot.y;
                MatrixOperations.pushMatrix(graphics);
                MatrixOperations.translateMatrix(graphics, sx + 8, sy + 8);
                float scale = 19f / 16f;
                MatrixOperations.scaleMatrix(graphics, scale, scale);
                MatrixOperations.translateMatrix(graphics, -8, -8);
                graphics.drawItem(slot.getStack(), 0, 0);
                MatrixOperations.popMatrix(graphics);
            }
        }

        renderToggleButton(graphics, self.x + self.backgroundWidth, self.y + 5, mouseX, mouseY);
    }

    private static void renderToggleButton(DrawContext graphics, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
        if (hovered) graphics.fill(x, y, x + 20, y + 20, 0x40FFFFFF);
        if (BundlePanelRenderer.visible) graphics.fill(x + 2, y + 17, x + 18, y + 18, 0xFF80B0FF);
        ItemStack icon = new ItemStack(Items.BUNDLE);
        graphics.drawItem(icon, x + 1, y + 2);
    }
}