package de.cxlina.capefix.mixin;

import de.cxlina.capefix.util.Fix;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
import net.minecraftcapes.config.MinecraftCapesConfig;
import net.minecraftcapes.player.PlayerHandler;
import net.minecraftcapes.player.render.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class MixinCapeLayer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private float capeRotateX, capeRotateY, capeRotateZ;

    protected MixinCapeLayer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    public void fixCapeStutters(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, AbstractClientPlayerEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ci.cancel();
        if (MinecraftCapesConfig.isCapeVisible() || entitylivingbaseIn.getCapeTexture() != null) {
            PlayerHandler playerHandler = PlayerHandler.getFromPlayer(entitylivingbaseIn);
            if (playerHandler.getShowCape() && !entitylivingbaseIn.isInvisible() && (entitylivingbaseIn.getCapeTexture() != null || playerHandler.getCapeLocation() != null)) {
                ItemStack itemStack = entitylivingbaseIn.getEquippedStack(EquipmentSlot.CHEST);
                if (itemStack.getItem() != Items.ELYTRA || playerHandler.getForceHideElytra() && !playerHandler.getForceShowElytra()) {
                    matrixStackIn.push();
                    matrixStackIn.translate(0.0, 0.0, 0.125);
                    double d0 = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevCapeX, entitylivingbaseIn.capeX) - MathHelper.lerp(partialTicks, entitylivingbaseIn.prevX, entitylivingbaseIn.getX());
                    double d1 = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevCapeY, entitylivingbaseIn.capeY) - MathHelper.lerp(partialTicks, entitylivingbaseIn.prevY, entitylivingbaseIn.getY());
                    double d2 = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevCapeZ, entitylivingbaseIn.capeZ) - MathHelper.lerp(partialTicks, entitylivingbaseIn.prevZ, entitylivingbaseIn.getZ());
                    float f = entitylivingbaseIn.prevBodyYaw + (entitylivingbaseIn.bodyYaw - entitylivingbaseIn.prevBodyYaw);
                    double d3 = MathHelper.sin(f * 0.017453292F);
                    double d4 = -MathHelper.cos(f * 0.017453292F);
                    float f1 = (float) d1 * 10.0F;
                    f1 = MathHelper.clamp(f1, -6.0F, 32.0F);
                    float f2 = (float) (d0 * d3 + d2 * d4) * 100.0F;
                    f2 = MathHelper.clamp(f2, 0.0F, 150.0F);
                    float f3 = (float) (d0 * d4 - d2 * d3) * 100.0F;
                    f3 = MathHelper.clamp(f3, -20.0F, 20.0F);
                    if (f2 < 0.0F) f2 = 0.0F;
                    float f4 = MathHelper.lerp(partialTicks, entitylivingbaseIn.prevStrideDistance, entitylivingbaseIn.strideDistance);
                    f1 += MathHelper.sin(MathHelper.lerp(partialTicks, entitylivingbaseIn.prevHorizontalSpeed, entitylivingbaseIn.horizontalSpeed) * 6.0F) * 32.0F * f4;
                    if (entitylivingbaseIn.isInSneakingPose()) f1 += 25.0F;

                    //--------------------------This is causing the Cape-Stuttering - Honestly i have no idea why--------------------------
                    //matrixStackIn.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(6.0F + f2 / 2.0F + f1));
                    //matrixStackIn.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(f3 / 2.0F));
                    //matrixStackIn.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F - f3 / 2.0F));

                    //--------------------------This is used instead to fix the Stuttering--------------------------
                    float f5 = Fix.limit(Fix.getAverageFrameTimeSec() * 20.0F, 0.02F, 1.0F);
                    this.capeRotateX = MathHelper.lerp(f5, capeRotateX, 6.0F + f2 / 2.0F + f1);
                    this.capeRotateZ = MathHelper.lerp(f5, capeRotateZ, f3 / 2.0F);
                    this.capeRotateY = MathHelper.lerp(f5, capeRotateY, 180.0F - f3 / 2.0F);
                    matrixStackIn.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(capeRotateX));
                    matrixStackIn.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(capeRotateZ));
                    matrixStackIn.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(capeRotateY));
                    //-----------------------------------------------------------------------------------
                    VertexConsumer vertexConsumer;
                    if (MinecraftCapesConfig.isCapeVisible() && playerHandler.getCapeLocation() != null)
                        vertexConsumer = ItemRenderer.getItemGlintConsumer(bufferIn, RenderLayer.getEntityTranslucent(playerHandler.getCapeLocation()), false, playerHandler.getHasCapeGlint());
                    else
                        vertexConsumer = ItemRenderer.getItemGlintConsumer(bufferIn, RenderLayer.getEntityTranslucent(entitylivingbaseIn.getCapeTexture()), false, false);
                    this.getContextModel().renderCape(matrixStackIn, vertexConsumer, packedLightIn, OverlayTexture.DEFAULT_UV);
                    matrixStackIn.pop();
                }
            }
        }
    }
}
