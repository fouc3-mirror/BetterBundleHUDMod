package betterbundle.mixin;

import betterbundle.gui.BundlePanelRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @SuppressWarnings("DataFlowIssue")
    private InventoryScreenMixin() {
        super(null, null, null);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void onExtractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        BundlePanelRenderer.render(graphics,
                this.leftPos,
                this.topPos,
                this.imageHeight,
                mouseX, mouseY);

        renderToggleButton(graphics, this.leftPos + this.imageWidth, this.topPos + 5, mouseX, mouseY);
    }

    private static void renderToggleButton(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
        if (hovered) {
            graphics.fill(x, y, x + 20, y + 20, 0x40FFFFFF);
        }
        if (BundlePanelRenderer.visible) {
            graphics.fill(x + 2, y + 17, x + 18, y + 18, 0xFF80B0FF);
        }
        ItemStack icon = new ItemStack(Items.BUNDLE);
        graphics.item(icon, x + 1, y + 2);
    }
}
