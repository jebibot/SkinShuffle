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

package com.mineblock11.skinshuffle.client.gui;

import com.mineblock11.skinshuffle.SkinShuffle;
import com.mineblock11.skinshuffle.client.SkinShuffleClient;import com.mineblock11.skinshuffle.client.config.SkinPresetManager;
import com.mineblock11.skinshuffle.client.config.SkinShuffleConfig;
import com.mineblock11.skinshuffle.client.gui.cursed.GuiEntityRenderer;
import com.mineblock11.skinshuffle.client.gui.widgets.IconButtonWidget;
import com.mineblock11.skinshuffle.client.gui.widgets.preset.PresetWidget;
import com.mineblock11.skinshuffle.client.preset.SkinPreset;
import com.mineblock11.skinshuffle.client.skin.*;
import com.mineblock11.skinshuffle.util.ToastHelper;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.validator.routines.UrlValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PresetEditScreen extends SpruceScreen {
    public static final int MAX_WIDTH = 400;

    private final CarouselScreen parent;
    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});
    private final SkinPreset originalPreset;
    private final SkinPreset preset;
    private final PresetWidget<?> presetWidget;
    private TabNavigationWidget tabNavigation;
    private SkinSourceTab skinSourceTab;
    private SkinCustomizationTab skinCustomizationTab;
    private boolean isValid = true;
    private GridWidget grid;
    private ButtonWidget exitButton;
    private int sideMargins;

    public PresetEditScreen(PresetWidget<?> presetWidget, CarouselScreen parent, SkinPreset preset) {
        super(Text.translatable("skinshuffle.edit.title"));
        this.presetWidget = presetWidget;
        this.preset = preset.copy();
        this.originalPreset = preset;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.skinSourceTab = new SkinSourceTab();
        this.skinCustomizationTab = new SkinCustomizationTab();
        this.tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width)
                .tabs(skinSourceTab, skinCustomizationTab).build();
        this.addDrawableChild(this.tabNavigation);
        this.tabNavigation.selectTab(0, false);

        this.grid = new GridWidget().setColumnSpacing(10);
        GridWidget.Adder adder = this.grid.createAdder(2);
        adder.add(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
            this.close();
        }).build());

        this.exitButton = ButtonWidget.builder(ScreenTexts.OK, (button) -> {
            if (this.skinSourceTab.currentSourceType != SourceType.UNCHANGED) {
                if (this.skinSourceTab.currentSourceType == SourceType.FILE) {
                    // Expand ~ to the user's home directory
                    String pathStr = this.skinSourceTab.textFieldWidget.getText();
                    if (pathStr.startsWith("~")) {
                        String home = System.getProperty("user.home");
                        pathStr = home + pathStr.substring(1);
                    }

                    this.skinSourceTab.textFieldWidget.setText(pathStr);
                }

                if (this.skinSourceTab.currentSourceType == SourceType.FILE || this.skinSourceTab.currentSourceType == SourceType.URL) {
                    // Remove quotes from around the path
                    String sre = this.skinSourceTab.textFieldWidget.getText();
                    if (sre.startsWith("\"") && sre.endsWith("\"")) {
                        sre = sre.substring(1, sre.length() - 1);
                    }

                    this.skinSourceTab.textFieldWidget.setText(sre);
                }
            }

            this.originalPreset.copyFrom(this.preset);
            // We try to save the skin to config, but if it fails, it's safe to ignore.
            try {
                this.originalPreset.setSkin(this.preset.getSkin().saveToConfig());
            } catch (Exception ignored) {
            }

            SkinPresetManager.savePresets();
            parent.hasEditedPreset = true;
            this.close();
        }).build();

        adder.add(exitButton);
        this.grid.forEachChild((child) -> {
            child.setNavigationOrder(1);
            this.addDrawableChild(child);
        });

        this.initTabNavigation();
    }

    @Override
    protected void initTabNavigation() {
        this.sideMargins = Math.max(this.width - MAX_WIDTH, 0) / 2;

        if (this.tabNavigation != null && this.grid != null) {
            this.tabNavigation.setWidth(this.width);
            this.tabNavigation.init();

            this.grid.refreshPositions();
            SimplePositioningWidget.setPos(this.grid, 0, this.height - 36, this.width, 36);

            int i = this.tabNavigation.getNavigationFocus().getBottom();
            ScreenRect screenRect = new ScreenRect(0, i, this.width, this.grid.getY() - i);
            this.tabManager.setTabArea(screenRect);
        }

        updateValidity();
    }

    private boolean isValidPngFilePath(String pathStr) {
        // Trim leading and trailing spaces
        pathStr = pathStr.trim();

        // Remove surrounding quotation marks if present
        if (pathStr.startsWith("\"") && pathStr.endsWith("\"")) {
            pathStr = pathStr.substring(1, pathStr.length() - 1);
        }

        // Allow ~ as a shortcut for the user's home directory
        if (pathStr.startsWith("~")) {
            String home = System.getProperty("user.home");
            pathStr = home + pathStr.substring(1);
        }

        // Resolve the path
        Path path = Paths.get(pathStr);

        // Check if the file exists, follows symlinks, and is a regular file
        if (Files.exists(path) && Files.isRegularFile(path)) {
            // Check if the file has a .png extension (case insensitive)
            String fileName = path.getFileName().toString().toLowerCase();
            return fileName.endsWith(".png");
        }

        return false;
    }

    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isValidUsername(String username) {
        return username.matches("([a-zA-Z0-9]|_)*") && username.length() >= 3 && username.length() <= 16;
    }

    public boolean validate() {
        if (this.skinCustomizationTab != null && this.skinSourceTab != null) {
            SourceType type = this.skinSourceTab.currentSourceType;
            TextFieldWidget widget = this.skinSourceTab.textFieldWidget;
            // URL
            switch (type) {
                case URL -> {
                    return urlValidator.isValid(widget.getText());
                }
                case FILE -> {
                    try {
                        return isValidPngFilePath(widget.getText());
                    } catch (Exception ignored) {
                        return false;
                    }
                }
                case RESOURCE_LOCATION -> {
                    /*? if <1.21 {*/
                    if (Identifier.isValid(widget.getText())) {
                        return client.getResourceManager().getResource(new Identifier(widget.getText())).isPresent();
                    } else return false;
                    /*?} else {*/
                    /*if (Identifier.validate(widget.getText()).isSuccess()) {
                        return client.getResourceManager().getResource(Identifier.tryParse(widget.getText())).isPresent();
                    } else return false;
                    *//*?}*/
                }
                case USERNAME -> {
                    return isValidUsername(widget.getText());
                }
                case UUID -> {
                    return isValidUUID(widget.getText());
                }
                default -> {
                    return false;
                }
            }
        } else return false;
    }

    private void updateValidity() {
        if (skinSourceTab.currentSourceType != SourceType.UNCHANGED) {
            this.isValid = this.validate();
            if (!this.isValid) {
                this.skinSourceTab.errorLabel.setMessage(skinSourceTab.currentSourceType.getInvalidInputText());
            } else {
                this.skinSourceTab.errorLabel.setMessage(Text.empty());
            }
            this.grid.refreshPositions();
        }

        this.skinSourceTab.textFieldWidget.setVisible(skinSourceTab.currentSourceType != SourceType.UNCHANGED);
        this.skinSourceTab.loadButton.visible = skinSourceTab.currentSourceType != SourceType.UNCHANGED;
        this.skinSourceTab.loadButton.active = skinSourceTab.currentSourceType != SourceType.UNCHANGED && isValid;
        this.skinSourceTab.skinModelButton.visible = skinSourceTab.currentSourceType != SourceType.UNCHANGED;
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        int ratioMulTen = 16;
        int topBottomMargin = 40;
        int leftRightMargin = 20;
        int previewSpanX = MAX_WIDTH / 6 - leftRightMargin;
        int previewSpanY = Math.min(this.height - topBottomMargin * 2, previewSpanX * 2 * ratioMulTen / 10) / 2;
        int previewCenterX = MAX_WIDTH / 6 + this.sideMargins;
        int previewCenterY = Math.max(height / 4 + previewSpanY / 2, 120); // Math.min(this.height / 2, topBottomMargin + previewSpanY * 2);
        graphics.drawBorder(previewCenterX - previewSpanX, previewCenterY - previewSpanY,
                previewSpanX * 2, previewSpanY * 2, 0xDF000000);
        graphics.fill(previewCenterX - previewSpanX + 1, previewCenterY - previewSpanY + 1,
                previewCenterX + previewSpanX - 1, previewCenterY + previewSpanY - 1, 0x7F000000);

        if (!this.preset.getSkin().isLoading()) {
            var entityX = previewCenterX;
            var entityY = (previewCenterY + previewSpanY / 10 * 8) + 30;

            float followX = entityX - mouseX;
            float followY = entityY - previewSpanY * 1.2f - mouseY - 10f;
            float rotation = 0;

            SkinShuffleConfig.SkinRenderStyle renderStyle = SkinShuffleConfig.get().presetEditScreenRenderStyle;

            if (renderStyle.equals(SkinShuffleConfig.SkinRenderStyle.ROTATION)) {
                followX = 0;
                followY = 0;
                rotation = getEntityRotation() * SkinShuffleConfig.get().rotationMultiplier;
            }

            if (!this.preset.getSkin().isLoading() && !this.skinSourceTab.loading) {
                graphics.getMatrices().push();
                GuiEntityRenderer.drawEntity(
                        graphics.getMatrices(), entityX, entityY, previewSpanY / 10 * 8,
                        rotation, followX, followY, this.preset.getSkin(), renderStyle
                );
                graphics.getMatrices().pop();
            } else {
                var txt = Text.translatable("skinshuffle.edit.loading");
                int textWidth = this.textRenderer.getWidth(txt);
                float totalDeltaTick = SkinShuffleClient.TOTAL_TICK_DELTA * 5f;
                float hue = (totalDeltaTick % 360) / 360;
                float saturation = 0.75f;
                float lightness = 1f;
                int color = java.awt.Color.HSBtoRGB(hue, saturation, lightness);
                color = (color & 0x00FFFFFF) | 0xFF000000;
                graphics.drawTextWithShadow(this.textRenderer, txt, previewCenterX - (textWidth / 2), previewCenterY - (this.textRenderer.fontHeight), color);
            }
        } else {
            // We call getTexture() anyway to make sure the texture is being loaded in the background.
            this.preset.getSkin().getTexture();
        }

        this.exitButton.active = !this.preset.equals(this.originalPreset);
    }

    /*? if <1.20.6 {*/
    public void renderBackgroundTexture(DrawContext context) {
        // If we don't explicitly have this, the background color will be slightly off from the tab color.
        context.drawTexture(net.minecraft.client.gui.screen.world.CreateWorldScreen.LIGHT_DIRT_BACKGROUND_TEXTURE, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
    }
    /*?}*/

    private float getEntityRotation() {
        return (float) GlfwUtil.getTime() * 35f;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    private enum SourceType {
        UNCHANGED,
        USERNAME,
        UUID,
        URL,
        RESOURCE_LOCATION,
        FILE;

        public Text getInvalidInputText() {
            return Text.translatable("skinshuffle.edit.source.invalid_" + name().toLowerCase());
        }

        public Text getTranslation() {
            return Text.translatable("skinshuffle.edit.source." + name().toLowerCase());
        }
    }

    private class SkinSourceTab extends GridScreenTab {
        private final TextFieldWidget textFieldWidget;
        private final MultilineTextWidget errorLabel;
        public PresetEditScreen.SourceType currentSourceType;
        private final CyclingButtonWidget<String> skinModelButton;
        private final ButtonWidget loadButton;
        private boolean loading = false;

        private SkinSourceTab() {
            super(Text.translatable("skinshuffle.edit.source.title"));

            this.grid.getMainPositioner().marginLeft(MAX_WIDTH / 3 + sideMargins).marginRight(sideMargins).alignHorizontalCenter();
            var gridAdder = this.grid.setRowSpacing(4).createAdder(1);

            this.currentSourceType = SourceType.UNCHANGED;

            this.textFieldWidget = new TextFieldWidget(textRenderer, 0, 0, 230, 20, Text.empty());
            this.textFieldWidget.setMaxLength(2048);

            this.errorLabel = new MultilineTextWidget(0, 0, Text.empty(), textRenderer) {
                @Override
                public int getHeight() {
                    int minHeight = textRenderer.fontHeight * 5;
                    return Math.max(super.getHeight(), minHeight);
                }
            };

            loadButton = new IconButtonWidget(0, 0, 20, 20,
                    0, 0, 0, 2,
                    32, 16, 16, 16, 48,
                    SkinShuffle.id("textures/gui/reload-button-icon.png"),
                    button -> {
                        if (currentSourceType != SourceType.UNCHANGED) {
                            if (currentSourceType == SourceType.FILE) {
                                // Expand ~ to the user's home directory
                                String pathStr = textFieldWidget.getText();
                                if (pathStr.startsWith("~")) {
                                    String home = System.getProperty("user.home");
                                    pathStr = home + pathStr.substring(1);
                                }

                                textFieldWidget.setText(pathStr);
                            }

                            if (currentSourceType == SourceType.FILE || currentSourceType == SourceType.URL) {
                                // Remove quotes from around the path
                                String sre = textFieldWidget.getText();
                                if (sre.startsWith("\"") && sre.endsWith("\"")) {
                                    sre = sre.substring(1, sre.length() - 1);
                                }

                                textFieldWidget.setText(sre);
                            }

                            loadSkin();
                        }
                    }
            );

            this.textFieldWidget.setChangedListener(str -> updateValidity());

            skinModelButton = new CyclingButtonWidget.Builder<String>(Text::of)
                    .values("classic", "slim")
                    .build(0, 0, 192, 20, Text.translatable("skinshuffle.edit.source.skin_model"), (widget, val) -> {
                        SkinPreset preset = PresetEditScreen.this.preset;
                        preset.getSkin().setModel(val);
                    });

            if (currentSourceType != null) {
                gridAdder.add(new CyclingButtonWidget<>(0,
                        0,
                        192,
                        20,
                        Text.translatable("skinshuffle.edit.source.cycle_prefix").append(": ").append(currentSourceType.getTranslation()),
                        Text.translatable("skinshuffle.edit.source.cycle_prefix"),
                        Arrays.stream(SourceType.values()).toList().indexOf(this.currentSourceType),
                        currentSourceType,
                        CyclingButtonWidget.Values.of(List.of(PresetEditScreen.SourceType.values())),
                        PresetEditScreen.SourceType::getTranslation,
                        sourceTypeCyclingButtonWidget -> Text.of("").copy(),
                        (button, value) -> {
                            this.currentSourceType = value;
                            this.errorLabel.setMessage(Text.empty());

                            updateValidity();
                        },
                        value -> null,
                        false), grid.copyPositioner().marginTop(Math.min(height / 2 - 60, 20)));
            } else {
                ToastHelper.showEditorFailToast();
                close();
            }

            gridAdder.add(skinModelButton);

            var subGrid = new GridWidget();
            var subGridAdder = subGrid.setColumnSpacing(4).createAdder(2);
            gridAdder.add(subGrid, grid.copyPositioner().marginTop(6).marginBottom(6));
            subGridAdder.add(textFieldWidget);
            subGridAdder.add(loadButton);

            gridAdder.add(errorLabel, grid.copyPositioner().alignLeft());
        }

        private void loadSkin() {
            this.loading = true;
            String skinSource = textFieldWidget.getText();
            String model = skinModelButton.getValue();

            if (!skinSource.isEmpty() && currentSourceType != SourceType.UNCHANGED) {
                Skin skin = switch (currentSourceType) {
                    case URL -> new UrlSkin(skinSource, model);
                    case FILE -> new FileSkin(Path.of(skinSource), model);
                    case UUID -> new UUIDSkin(UUID.fromString(skinSource), model);
                    case USERNAME -> new UsernameSkin(skinSource, model);
                    case RESOURCE_LOCATION -> new ResourceSkin(Identifier.tryParse(skinSource), model);
                    default -> Skin.randomDefaultSkin();
                };

                CompletableFuture.runAsync(() -> {
                    preset.setSkin(skin.saveToConfig());
                    skin.getTexture();
                    while (preset.getSkin().isLoading()) {
                        Thread.onSpinWait();
                    }
                    this.loading = false;
                }, Util.getIoWorkerExecutor());
            }
        }
    }

    private class SkinCustomizationTab extends GridScreenTab {

        public SkinCustomizationTab() {
            super(Text.translatable("skinshuffle.edit.customize.title"));

            this.grid.getMainPositioner().marginLeft(parent.width / 3).alignHorizontalCenter();
            var gridAdder = this.grid.setRowSpacing(8).createAdder(1);

            var presetNameField = new TextFieldWidget(textRenderer, 0, 0, 256, 20, Text.empty());
            presetNameField.setText(preset.getName());
            presetNameField.setChangedListener(preset::setName);
            presetNameField.setMaxLength(2048);

            gridAdder.add(new TextWidget(Text.translatable("skinshuffle.edit.customize.preset_name"), textRenderer));
            gridAdder.add(presetNameField);
        }
    }
}

