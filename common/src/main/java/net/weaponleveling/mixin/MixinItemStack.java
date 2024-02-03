package net.weaponleveling.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.weaponleveling.WLPlatformGetter;
import net.weaponleveling.util.ModUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    @Shadow public abstract boolean hurt(int i, RandomSource randomSource, @Nullable ServerPlayer serverPlayer);

    @Inject(
            method = "Lnet/minecraft/world/item/ItemStack;getAttributeModifiers(Lnet/minecraft/world/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;",
            at = @At(value = "RETURN"), cancellable = true)
    private void injectModifier(EquipmentSlot equipmentSlot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        HashMultimap<Attribute, AttributeModifier> hashmap = HashMultimap.create(cir.getReturnValue());
        ItemStack stack = ((ItemStack) ((Object) this));
        Attribute attackDamage = Attributes.ATTACK_DAMAGE;
        Attribute attackSpeed = Attributes.ATTACK_SPEED;
        Attribute armor = Attributes.ARMOR;
        Attribute armor_toughness = Attributes.ARMOR_TOUGHNESS;
        if (ModUtils.isLevelableItem(stack) && ModUtils.isAcceptedMeleeWeaponStack(stack) && stack.getTag() != null) {
            ModUtils.modifyAttributeModifier(hashmap,attackDamage, ModUtils.getWeaponDamagePerLevel(stack) * stack.getTag().getInt("level"));
            ModUtils.modifyAttributeModifier(hashmap,attackSpeed, 0.0d );
        }
        if(ModUtils.isBroken(stack)) {
            ModUtils.removeAttributeModifier(hashmap, armor);
            ModUtils.removeAttributeModifier(hashmap, armor_toughness);
        }
        cir.setReturnValue(hashmap);
    }

    @Inject(
            method = "useOn",
            at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private void preventInteract(UseOnContext useOnContext, CallbackInfoReturnable<InteractionResult> cir) {
        if(ModUtils.isBroken(useOnContext.getItemInHand())) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(
            method = "hurtAndBreak",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private <T extends LivingEntity> void preventBreak(int i, T livingEntity, Consumer<T> consumer, CallbackInfo ci) {
        ItemStack stack = ((ItemStack) ((Object) this));
        if(livingEntity instanceof ServerPlayer player) {
            if(this.hurt(i, livingEntity.getRandom(), player)) {
                if(WLPlatformGetter.getBrokenItemsDontVanish() && ModUtils.shouldBeUnbreakable(stack)) {
                    if(Platform.isForge()) {
                        //TODO Play sound for break
                    }
                    CompoundTag tag = stack.getTag();
                    tag.putBoolean("isBroken", true);
                    stack.setTag(tag);
                    ci.cancel();
                }
            }
        }
    }
}
