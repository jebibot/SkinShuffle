/*
 * ALL RIGHTS RESERVED
 *
 * Copyright (c) 2024 Calum H. (IMB11) and enjarai
 *
 * THE SOFTWARE IS PROVIDED "AS IS," WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.mineblock11.skinshuffle.client.gui.cursed;

import com.mineblock11.skinshuffle.client.SkinShuffleClient;
import com.mineblock11.skinshuffle.client.config.SkinShuffleConfig;
import com.mineblock11.skinshuffle.client.skin.Skin;
import com.mineblock11.skinshuffle.compat.ETFCompat;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GuiEntityRenderer {
    public static void drawEntity(MatrixStack matrices, int x, int y, int size, float rotation, double mouseX, double mouseY, Skin skin, SkinShuffleConfig.SkinRenderStyle style) {
        float yaw = (float) (Math.atan2(mouseX, 120.0F));
        float pitch = (float) (Math.atan2(-mouseY, 120.0F));

        Quaternionf entityRotation = new Quaternionf().rotateY(rotation * 0.025f);

        if (style == SkinShuffleConfig.SkinRenderStyle.CURSOR) {
            Quaternionf pitchRotation = new Quaternionf().rotateX(pitch * 10.0F * 0.017453292F);
            Quaternionf yawRotation = new Quaternionf().rotateY(yaw * 10.0F * 0.017453292F);
            entityRotation.mul(pitchRotation);
            entityRotation.mul(yawRotation);
        }

        setupModelViewStack();
        setupMatrices(matrices, x, y, size, entityRotation);

        renderEntity(matrices, yaw, pitch, skin, SkinShuffleClient.TOTAL_TICK_DELTA);

        cleanupMatrices(matrices);
        cleanupModelViewStack();
    }

    private static void setupModelViewStack() {
        //? if >=1.20.5 {
        /*Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.translate(0.0f, 0.0f, 1000.0f);
        RenderSystem.applyModelViewMatrix();
        *///?} else {
        MatrixStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        modelViewStack.translate(0.0, 0.0, 1000.0);
        RenderSystem.applyModelViewMatrix();
        //?}
    }

    private static void setupMatrices(MatrixStack matrices, int x, int y, int size, Quaternionf entityRotation) {
        matrices.push();
        matrices.translate(x, y, -950.0);
        matrices.multiplyPositionMatrix(new Matrix4f().scaling(size, size, -size));
        matrices.translate(0, -1, 0);
        matrices.multiply(entityRotation);
        matrices.translate(0, -1, 0);
        DiffuseLighting.method_34742();
    }

    private static void renderEntity(MatrixStack matrices, float yaw, float pitch, Skin skin, float totalTickDelta) {
        var modelData = PlayerEntityModel.getTexturedModelData(Dilation.NONE, skin.getModel().equals("slim"));
        NoEntityPlayerModel model = new NoEntityPlayerModel(TexturedModelData.of(modelData, 64, 64).createModel(), skin.getModel().equals("slim"));

        model.swingArmsGently(totalTickDelta);
        model.setHeadPos(yaw, pitch);
        model.waveCapeGently(totalTickDelta);

        if (FabricLoader.getInstance().isModLoaded("entity_texture_features")) {
            ETFCompat.preventRenderLayerIssue();
        }

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        model.render(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(skin.getTexture())),
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                //? if >=1.21 {
                /*0xFFFFFFFF
                *///?} else {
                1f, 1f, 1f, 1f
                //?}
        );

        //? if =1.20.1 {
        /*MinecraftClient client = MinecraftClient.getInstance();
        var hasCape = client.getSkinProvider().getTextures(client.getSession().getProfile()).containsKey(MinecraftProfileTexture.Type.CAPE);
        if (hasCape && SkinShuffleConfig.get().showCapeInPreview) {
        *///?} else {
        if (skin.getSkinTextures().capeTexture() != null && SkinShuffleConfig.get().showCapeInPreview) {
        //?}
            matrices.push();
            matrices.translate(0.0F, 0.0F, 0.2F);
            model.renderCape(
                    matrices,
                    //? if !=1.20.1 {
                    vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(skin.getSkinTextures().capeTexture())),
                    //?} else {
                    /*vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentCull(client.getSkinProvider().loadSkin(client.getSkinProvider().getTextures(client.getSession().getProfile()).get(MinecraftProfileTexture.Type.CAPE), MinecraftProfileTexture.Type.CAPE))),
                    *///?}
                    0,
                    OverlayTexture.DEFAULT_UV
            );
            matrices.pop();
        }
        vertexConsumers.draw();
    }

    private static void cleanupMatrices(MatrixStack matrices) {
        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    private static void cleanupModelViewStack() {
        //? if >=1.20.5 {
        /*Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        *///?} else {
        MatrixStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pop();
        RenderSystem.applyModelViewMatrix();
        //?}
    }

    public static class NoEntityPlayerModel extends PlayerEntityModel {
        public NoEntityPlayerModel(ModelPart root, boolean thinArms) {
            super(root, thinArms);
            this.child = false;
        }

        public void swingArmsGently(float totalDeltaTick) {
            float f = MathHelper.sin(totalDeltaTick * 0.067F) * 0.05F;
            this.rightArm.roll = f + 0.06F;
            this.rightSleeve.roll = f + 0.06F;
            this.leftArm.roll = -f - 0.06F;
            this.leftSleeve.roll = -f - 0.06F;
        }

        public void setHeadPos(float headYaw, float headPitch) {
            this.head.yaw = headYaw;
            this.hat.yaw = headYaw;
            this.head.pitch = headPitch;
            this.hat.pitch = headPitch;
        }

        public void waveCapeGently(float totalDeltaTick) {
            float f = MathHelper.sin(totalDeltaTick * 0.067F) * 0.05F;
            this.cloak.pitch = f;
        }
    }
}
