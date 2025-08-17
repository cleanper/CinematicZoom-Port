package mix.cinematiczoom.integrations;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import mix.cinematiczoom.ZoomConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }

    private static class ConfigScreen extends Screen {
        private final Screen parent;
        private final ZoomConfig cfg;

        protected ConfigScreen(Screen parent) {
            super(Text.translatable("cinematiczoom.config.title"));
            this.parent = parent;
            this.cfg = ZoomConfig.INSTANCE;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int buttonWidth = 200;
            int buttonHeight = 20;
            int y = 40;
            int spacing = 25;

            this.addDrawableChild(new SliderWidget(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                    Text.translatable("cinematiczoom.option.bars_percent", (int)cfg.barsPercent),
                    cfg.barsPercent / 50.0f) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.translatable("cinematiczoom.option.bars_percent",
                            String.format("%.0f%%", this.value * 50.0f)));
                }

                @Override
                protected void applyValue() {
                    cfg.barsPercent = (float)(this.value * 50.0f);
                }
            });
            y += spacing;

            this.addDrawableChild(new SliderWidget(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                    Text.translatable("cinematiczoom.option.smooth_ms", cfg.smoothMs),
                    cfg.smoothMs / 2000.0f) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.translatable("cinematiczoom.option.smooth_ms",
                            String.format("%dms", (int)(this.value * 2000.0f))));
                }

                @Override
                protected void applyValue() {
                    cfg.smoothMs = (int)(this.value * 2000.0f);
                }
            });
            y += spacing;

            this.addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initially(cfg.mouseWheelEnabled)
                    .build(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                            Text.translatable("cinematiczoom.option.mouse_wheel_enabled"),
                            (button, value) -> cfg.mouseWheelEnabled = value));
            y += spacing;

            this.addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initially(cfg.hideHudDuringZoom)
                    .build(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                            Text.translatable("cinematiczoom.option.hide_hud"),
                            (button, value) -> cfg.hideHudDuringZoom = value));
            y += spacing;

            this.addDrawableChild(CyclingButtonWidget.onOffBuilder()
                    .initially(cfg.enableCinematicCamera)
                    .build(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                            Text.translatable("cinematiczoom.option.cinematic_cam"),
                            (button, value) -> cfg.enableCinematicCamera = value));
            y += spacing;

            this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
                cfg.clamp();
                cfg.save();
                MinecraftClient.getInstance().setScreen(parent);
            }).position(centerX - buttonWidth / 2, y).size(buttonWidth, buttonHeight).build());
        }

        @Override
        public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
        }
    }
}
