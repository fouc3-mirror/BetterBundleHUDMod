package betterbundle.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.BundleContentsComponent;
import org.apache.commons.lang3.math.Fraction;

public final class BundleContentsComponentHelper {

    private BundleContentsComponentHelper() {}

    public static BundleContentsComponent getContents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.get(DataComponentTypes.BUNDLE_CONTENTS);
    }

    public static boolean isNonEmptyBundle(ItemStack stack) {
        BundleContentsComponent contents = getContents(stack);
        return contents != null && !contents.isEmpty();
    }

    /** True if this stack is a Bundle item (even if empty/unused). */
    public static boolean isBundle(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof BundleItem;
    }

    /** Check if the given item can still fit into this bundle. Works for empty bundles too. */
    public static boolean canFitItem(ItemStack bundleStack, ItemStack toInsert) {
        if (!isBundle(bundleStack) || toInsert.isEmpty()) return false;

        BundleContentsComponent contents = getContents(bundleStack);
        Fraction currentWeight;
        if (contents != null) {
            currentWeight = contents.getOccupancy();
        } else {
            currentWeight = Fraction.ZERO; // empty/unused bundle
        }

        // Calculate item weight
        // For bundle items, the weight is the occupancy of its contents (empty bundle = 0)
        // This matches vanilla behavior where bundles can contain other bundles
        Fraction itemWeight;
        if (toInsert.getItem() instanceof BundleItem) {
            // Bundle as item: weight = occupancy of its contents
            BundleContentsComponent toInsertContents = getContents(toInsert);
            if (toInsertContents != null && !toInsertContents.isEmpty()) {
                itemWeight = toInsertContents.getOccupancy();
            } else {
                itemWeight = Fraction.ZERO; // empty bundle has weight 0
            }
        } else {
            // Normal item: weight = count / maxCount
            itemWeight = Fraction.getFraction(toInsert.getCount(), toInsert.getMaxCount());
        }
        
        return currentWeight.add(itemWeight).compareTo(Fraction.ONE) <= 0;
    }
}
