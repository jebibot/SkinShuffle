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

package com.mineblock11.skinshuffle.client.skin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface Skin {
    /*? if >=1.20.5 {*/
    /*Map<Identifier, MapCodec<? extends Skin>> TYPES = Map.of(
    *//*?} else {*/
    Map<Identifier, Codec<? extends Skin>> TYPES = Map.of(
    /*?}*/
            UrlSkin.SERIALIZATION_ID, UrlSkin.CODEC,
            ResourceSkin.SERIALIZATION_ID, ResourceSkin.CODEC,
            ConfigSkin.SERIALIZATION_ID, ConfigSkin.CODEC,
            FileSkin.SERIALIZATION_ID, FileSkin.CODEC,
            UsernameSkin.SERIALIZATION_ID, UsernameSkin.CODEC,
            UUIDSkin.SERIALIZATION_ID, UUIDSkin.CODEC
    );
    Codec<Skin> CODEC = Identifier.CODEC.dispatch("type", Skin::getSerializationId, TYPES::get);

    @Nullable Identifier getTexture();

    /*? if >=1.20.4 {*/
    default net.minecraft.client.util.SkinTextures getSkinTextures() {
            MinecraftClient client = MinecraftClient.getInstance();
            net.minecraft.client.util.SkinTextures clientTexture = client.getSkinProvider().getSkinTextures(client.getGameProfile());
            return new net.minecraft.client.util.SkinTextures(this.getTexture(), null, clientTexture.capeTexture(), clientTexture.elytraTexture(), net.minecraft.client.util.SkinTextures.Model.fromName(this.getModel()), false);
    }
    /*?}*/

    boolean isLoading();

    String getModel();

    Identifier getSerializationId();

    /**
     * Saves this skin to the config and returns a new reference to it.
     * THIS METHOD CAN AND WILL THROW, MAKE SURE TO CATCH IT!
     *
     * @throws RuntimeException If the skin could not be saved for whatever reason.
     * @return A new reference to this skin.
     */
    ConfigSkin saveToConfig();

    void setModel(String value);

//    static ResourceSkin randomDefaultSkin() {
//        var uuid = UUID.randomUUID();
//        var txt = DefaultSkinHelper.getSkinTextures(uuid);
//        return new ResourceSkin(txt.texture(), txt.model().getName());
//    }

    /*? if <1.20.4 {*/
    /*static ResourceSkin randomDefaultSkin() {
        var uuid = UUID.randomUUID();
        return new ResourceSkin(DefaultSkinHelper.getTexture(uuid), DefaultSkinHelper.getModel(uuid));
    }
    *//*?} else {*/
    static ResourceSkin randomDefaultSkin() {
        var uuid = UUID.randomUUID();
        var txt = DefaultSkinHelper.getSkinTextures(uuid);
        return new ResourceSkin(txt.texture(), txt.model().getName());
    }
    /*?}*/
}
