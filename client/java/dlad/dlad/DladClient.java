package dlad.dlad;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;                 
import org.jetbrains.annotations.NotNull;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import java.util.Locale;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

import java.io.IOException;
import java.net.URI;                             
import java.net.http.HttpClient;                
import java.net.http.HttpRequest;               
import java.net.http.HttpResponse;              
import java.time.Duration;                    
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class DladClient implements ClientModInitializer {
	private static boolean wasDown = false;

	@Override
	public void onInitializeClient() {
		Config.load();

		VersionCheck.start();

		ClientPlayConnectionEvents.JOIN.register((ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) -> {
			if (ServerGuard.OBFUSCATED(handler, client)) {
				ModGate.clearLatch();
			} else {
				ModGate.latchOff();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			// Any disconnect/lobby/kick -> permanently off until rejoin
			ModGate.latchOff();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ModGate.update(client);
			boolean isDown = InputUtil.isKeyPressed(client.getWindow().getHandle(), Config.getKeyCode())
					&& Screen.hasShiftDown();
			if (!wasDown && isDown) {
				if (client.currentScreen instanceof GameMenuScreen
						|| client.currentScreen instanceof TitleScreen) {
					client.setScreen(new ChangeKeybindScreen());
				}
			}
			wasDown = isDown;
		});
		EscapeDetector.register();
		AutoFisher.register();
		registerHud();
		GuardDetector.register();
		SausageFinder.register();
		MeteorDetector.register();
	}

	static final class ModGate {
		private static volatile boolean active = false;
		private static volatile boolean latchedDisabled = true; // start disabled until validated

		static void update(MinecraftClient client) {
			if (latchedDisabled) { active = false; return; }
			if (client == null || client.player == null || client.getNetworkHandler() == null) {
				active = false; return;
			}
			boolean OBFUSCATED = ServerGuard.OBFUSCATED(client.getNetworkHandler(), client);
			boolean OBFUSCATED = ServerGuard.OBFUSCATED(client);
			boolean OBFUSCATED = ServerGuard.OBFUSCATED(client);
			active = OBFUSCATED && OBFUSCATED && OBFUSCATED; //Reason for obfuscation: would expose sensitive information if seen by staff, allowing them to replicate in singleplayer world and reverse engineer features
		}
		static void latchOff() { latchedDisabled = true; active = false; }
		static void clearLatch() { latchedDisabled = false; }
		static boolean isNotActive() { return !active; }
	}

	static final class ServerGuard {

		// Works with handler when connected; falls back to currentServerEntry if available
		static boolean OBFUSCATED(ClientPlayNetworkHandler handler, MinecraftClient client) {
			//This has been obfuscated as it's one of the checks to see if in mlum
		}

		static boolean OBFUSCATED(MinecraftClient client) {
			//Obfuscated for the same reasons as above
		}

		static boolean OBFUSCATED(MinecraftClient client) {
			//Same as above
		}

	}


	// ===================== Version checker =====================
	static final class VersionCheck {
		private static final int CURRENT_VERSION = 1; //!!!!!!!!!!!!!!!!!!!!!!!! EDIT THIS BEFORE PUSHING FOR A NEW VERSION. IT MUST BE SYNCHRONIZED WITH VERSION.TXT IN THE WEBSITE

		private static volatile boolean updateAvailable = false;
		private static volatile boolean done = false;

		static void start() {
			Thread t = new Thread(() -> {
				try {
					HttpClient http = HttpClient.newBuilder()
							.followRedirects(HttpClient.Redirect.NORMAL)
							.connectTimeout(Duration.ofSeconds(5))
							.build();

					HttpRequest req = HttpRequest.newBuilder(URI.create(
									"https://dladmodmc.github.io/dladmodmc/internal/version.txt"))
							.timeout(Duration.ofSeconds(5))
							.GET()
							.build();

					HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
					String body = resp.body() == null ? "" : resp.body().trim();

					String digits = body.replaceAll("[^0-9]", "");
					if (!digits.isEmpty()) {
						int remote = Integer.parseInt(digits);
						updateAvailable = remote > CURRENT_VERSION;
					}
				} catch (Exception ignored) {
					// On error no update
				} finally {
					done = true;
				}
			}, "DLAD-VersionCheck");
			t.setDaemon(true);
			t.start();
		}

		static boolean isUpdateAvailable() { return updateAvailable; }
		static boolean isDone() { return done; }
	}
	// ==========================================================================

	static class Config {
		private static final Path CONFIG_PATH = FabricLoader.getInstance()
				.getConfigDir().resolve("dlad-config.json");
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
		private static Data data = new Data();

		static void load() {
			try {
				if (Files.exists(CONFIG_PATH)) {
					data = GSON.fromJson(Files.readString(CONFIG_PATH), Data.class);
				} else {
					save();
				}
			} catch (IOException ignored) {
			}
		}

		static void save() {
			try {
				Files.createDirectories(CONFIG_PATH.getParent());
				Files.writeString(CONFIG_PATH, GSON.toJson(data));
			} catch (IOException ignored) {
			}
		}

		public static int getKeyCode() { return data.keyCode; }
		public static void setKeyCode(int kc) { data.keyCode = kc; }

		public static boolean getFeatureState(int idx) {
			return idx >= 0 && idx < data.states.length && data.states[idx];
		}
		public static void setFeatureState(int idx, boolean st) {
			if (idx >= 0 && idx < data.states.length) data.states[idx] = st;
		}
		public static int getSausageRadius() {
			return data.sausageRadius;
		}
		public static void setSausageRadius(int r) {
			data.sausageRadius = MathHelper.clamp(r, 5, 50);
		}

		public static boolean isHudEnabled()        { return data.hudEnabled; }
		public static void    setHudEnabled(boolean e) { data.hudEnabled = e; }
		public static int     getHudPosIndex()      { return data.hudPosIndex; }
		public static void    setHudPosIndex(int i) { data.hudPosIndex = i; }
		public static int  getEscapeSize() { return MathHelper.clamp(data.escapeSize, 5, 20); }
		public static void setEscapeSize(int v) { data.escapeSize = MathHelper.clamp(v, 5, 20); }
		public static int  getGuardSize() { return MathHelper.clamp(data.guardSize, 5, 20); }
		public static void setGuardSize(int v) { data.guardSize = MathHelper.clamp(v, 5, 20); }
		public static int  getSausageSize() { return MathHelper.clamp(data.sausageSize, 5, 20); }
		public static void setSausageSize(int v) { data.sausageSize = MathHelper.clamp(v, 5, 20); }
		public static int  getMeteorSize() { return MathHelper.clamp(data.meteorSize, 5, 20); }
		public static void setMeteorSize(int v) { data.meteorSize = MathHelper.clamp(v, 5, 20); }

		private static class Data {
			int keyCode = InputUtil.GLFW_KEY_PERIOD;
			boolean[] states  = new boolean[]{false,false,false,false,false, false};
			boolean   hudEnabled  = false;
			int       hudPosIndex = 0;
			int sausageRadius = 10;
			int escapeSize  = 8;
			int guardSize   = 8;
			int sausageSize = 10;
			int meteorSize  = 10;
		}
	}

	private static class ChangeKeybindScreen extends Screen {
		private ButtonWidget keyButton, hudButton, posButton;
		private TextFieldWidget sizeEscapeField, sizeGuardField, sizeSausageField, sizeMeteorField;
		private ButtonWidget updateButton;

		private boolean listening;
		static final String[] FEATURES = {"Escape Detector","Guard Finder","Autofish","Sausage Finder", "Meteor Finder"};
		private static final String[] TOOLTIPS = {
				"Alerts of escapes and indicates the location via a marker",
				"Marks nearby guards/detectives, the color depends on the role and distance",
				"Point your bobber towards the water and get to work! 'Don't forget your toggle crouch!' -EVC",
				"Marks sausage:tm: signs near you. Radius: 5-50 (10-20 recommended as lag may be present at larger numbers)",
				"Shows the location of nearby meteors"
		};
		private static final String[] ARROWS = {"↗","↘","↙","↖"};
		private ButtonWidget[] featureButtons;

		protected ChangeKeybindScreen() {
			super(Text.of(""));
		}

		@Override
		protected void init() {
			int w = this.width;
			int centerX = (w - 200)/2;
			int topY = 50;

			int left = 10;
			int top  = 10;
			String updateText = "New version available [LINK]";
			int btnW = Math.max(180, this.textRenderer.getWidth(updateText) + 16);
			int btnH = 20;
			updateButton = addDrawableChild(ButtonWidget.builder(
							Text.literal(updateText).formatted(Formatting.YELLOW, Formatting.UNDERLINE),
							btn -> Util.getOperatingSystem().open(
									URI.create("https://dladmodmc.github.io/dladmodmc/downloads.html"))
					)
					.dimensions(left, top, btnW, btnH)
					.build());
			updateButton.visible = VersionCheck.isUpdateAvailable(); // may flip true later

			keyButton = addDrawableChild(ButtonWidget.builder(
							Text.of("Key: " + InputUtil.fromKeyCode(Config.getKeyCode(),0).getLocalizedText().getString()),
							btn -> { listening=true; btn.setMessage(Text.of("Press a key...")); }
					)
					.dimensions(centerX, topY, 200, 20)
					.build());

			int y2 = topY + 26;
			int baseY = y2 + 26;
			hudButton = addDrawableChild(ButtonWidget.builder(
							Text.literal("HUD: ").append(Text.literal(Config.isHudEnabled()?"ON":"OFF").formatted(Config.isHudEnabled()?Formatting.GREEN:Formatting.RED)),
							btn -> {
								boolean next = !Config.isHudEnabled();
								Config.setHudEnabled(next); Config.save();
								btn.setMessage(Text.literal("HUD: ").append(Text.literal(next?"ON":"OFF").formatted(next?Formatting.GREEN:Formatting.RED)));
							}
					)
					.dimensions(centerX, y2, 100, 20)
					.build());

			posButton = addDrawableChild(ButtonWidget.builder(
							Text.of(ARROWS[Config.getHudPosIndex()]),
							btn -> {
								int next = (Config.getHudPosIndex()+1)%ARROWS.length;
								Config.setHudPosIndex(next); Config.save();
								btn.setMessage(Text.of(ARROWS[next]));
							}
					)
					.dimensions(centerX+105, y2, 20, 20)
					.build());

			featureButtons = new ButtonWidget[FEATURES.length];
			int baseY = y2 + 26;
			for(int i=0;i<FEATURES.length;i++){
				int idx=i;
				featureButtons[i] = addDrawableChild(ButtonWidget.builder(
								Text.literal(FEATURES[idx] + ": ")
										.append(Text.literal(Config.getFeatureState(idx)?"ON":"OFF").formatted(Config.getFeatureState(idx)?Formatting.GREEN:Formatting.RED)),
								btn -> {
									boolean ns=!Config.getFeatureState(idx);
									Config.setFeatureState(idx,ns); Config.save();
									btn.setMessage(Text.literal(FEATURES[idx] + ": ")
											.append(Text.literal(ns?"ON":"OFF").formatted(ns?Formatting.GREEN:Formatting.RED)));
								}
						)
						.dimensions(centerX, baseY+i*26,200,20)
						.build());
			}
			TextFieldWidget radiusField = getTextFieldWidget();
			this.addDrawableChild(radiusField);
						int gap = 5;
			int fieldW = 40, fieldH = 20;
			int fieldX = centerX - gap - fieldW;
			// Escape Detector (index 0)
			sizeEscapeField = new TextFieldWidget(this.textRenderer, fieldX, baseY, fieldW, fieldH, Text.of("Size"));
			sizeEscapeField.setText(String.valueOf(Config.getEscapeSize()));
			sizeEscapeField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,2}")); // digits only
			sizeEscapeField.setChangedListener(s -> {
				try {
					int v = Integer.parseInt(s);
					if (v >= 5 && v <= 20) { Config.setEscapeSize(v); Config.save(); }
				} catch (NumberFormatException ignored) {}
			});
			this.addDrawableChild(sizeEscapeField);

// Guard Finder (index 1)
			sizeGuardField = new TextFieldWidget(this.textRenderer, fieldX, baseY + 26, fieldW, fieldH, Text.of("Size"));
			sizeGuardField.setText(String.valueOf(Config.getGuardSize()));
			sizeGuardField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,2}"));
			sizeGuardField.setChangedListener(s -> {
				try {
					int v = Integer.parseInt(s);
					if (v >= 5 && v <= 20) { Config.setGuardSize(v); Config.save(); }
				} catch (NumberFormatException ignored) {}
			});
			this.addDrawableChild(sizeGuardField);

// Sausage finder (index 3)
			sizeSausageField = new TextFieldWidget(this.textRenderer, fieldX, baseY + 3*26, fieldW, fieldH, Text.of("Size"));
			sizeSausageField.setText(String.valueOf(Config.getSausageSize()));
			sizeSausageField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,2}"));
			sizeSausageField.setChangedListener(s -> {
				try {
					int v = Integer.parseInt(s);
					if (v >= 5 && v <= 20) { Config.setSausageSize(v); Config.save(); }
				} catch (NumberFormatException ignored) {}
			});
			this.addDrawableChild(sizeSausageField);
						// Meteor Detector (index 4)
			sizeMeteorField = new TextFieldWidget(this.textRenderer, fieldX, baseY + 4*26, fieldW, fieldH, Text.of("Size"));
			sizeMeteorField.setText(String.valueOf(Config.getMeteorSize()));
			sizeMeteorField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,2}"));
			sizeMeteorField.setChangedListener(s -> {
				try {
					int v = Integer.parseInt(s);
					if (v >= 5 && v <= 20) { Config.setMeteorSize(v); Config.save(); }
				} catch (NumberFormatException ignored) {}
			});
			this.addDrawableChild(sizeMeteorField);
		}

		@Override
		public void tick() {
			super.tick();
			if (updateButton != null) {
				updateButton.visible = VersionCheck.isUpdateAvailable();
			}
		}

		private @NotNull TextFieldWidget getTextFieldWidget() {
			TextFieldWidget radiusField = new TextFieldWidget(
					this.textRenderer,
					featureButtons[3].getX() + featureButtons[3].getWidth() + 5,
					featureButtons[3].getY(),
					40, 20,
					Text.of("Radius")
			);
			radiusField.setText(String.valueOf(Config.getSausageRadius()));
			radiusField.setChangedListener(s -> {
				try {
					int val = Integer.parseInt(s);
					Config.setSausageRadius(val);
					Config.save();
				} catch (NumberFormatException ignored) {
					// if invalid, do nothing
				}
			});
			return radiusField;
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int mods) {
			if (listening) {
				// Reject Enter or Escape
				if (keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_ESCAPE) {
					// cancel without changing binding
					listening = false;
					keyButton.setMessage(Text.of("Shift + " + InputUtil.fromKeyCode(keyCode,0).getLocalizedText().getString()));
					return true;
				}
				Config.setKeyCode(keyCode);
				Config.save();
				keyButton.setMessage(Text.of("Shift + " + InputUtil.fromKeyCode(keyCode,0).getLocalizedText().getString()));
				listening = false;
				return true;
			}
			return super.keyPressed(keyCode, scanCode, mods);
		}

		@Override
		public void render(DrawContext ctx, int mx, int my, float delta) {
			super.render(ctx, mx, my, delta);
			if (hudButton.isHovered())
				ctx.drawTooltip(this.textRenderer, Collections.singletonList(Text.of("Toggle HUD")), mx, my);
			if (posButton.isHovered())
				ctx.drawTooltip(this.textRenderer, Collections.singletonList(Text.of("HUD position")), mx, my);
			if (sizeEscapeField != null && sizeEscapeField.isMouseOver(mx, my)) {
				ctx.drawTooltip(this.textRenderer, Collections.singletonList(Text.of("Change size (5–20)")), mx, my);
			}
			if (sizeGuardField != null && sizeGuardField.isMouseOver(mx, my)) {
				ctx.drawTooltip(this.textRenderer, Collections.singletonList(Text.of("Change size (5–20)")), mx, my);
			}
			if (sizeSausageField != null && sizeSausageField.isMouseOver(mx, my)) {
				ctx.drawTooltip(this.textRenderer, Collections.singletonList(Text.of("Change size (5–20)")), mx, my);
			}
			if (sizeMeteorField != null && sizeMeteorField.isMouseOver(mx, my)) {
				ctx.drawTooltip(this.textRenderer, Collections.singletonList(Text.of("Change size (5–20)")), mx, my);
			}
			for (int i = 0; i < featureButtons.length; i++) {
				if (featureButtons[i].isHovered()) {
					String tooltipText = TOOLTIPS[i];
					int maxWidth = this.width / 2;
					TextRenderer tr = this.textRenderer;
					java.util.List<Text> wrapped = wrapText(tooltipText, maxWidth, tr);
					ctx.drawTooltip(tr, wrapped, mx, my);
				}
			}

			TextRenderer tr = this.textRenderer;
			String title = "dlad";
			ctx.getMatrices().push();
			ctx.getMatrices().scale(2f, 2f, 1f);
			ctx.drawText(tr, Text.of(title), (this.width / 2 - tr.getWidth(title)) / 2, 5, 0xFFFFFF, false);
			ctx.getMatrices().pop();
			if (ModGate.isNotActive()) {
				String line1 = "Features disabled";
				String line2 = "Restricted to only mlum";
				int pad = 6;

				int w1 = tr.getWidth(line1);
				int w2 = tr.getWidth(line2);
				int boxW = Math.max(w1, w2) + pad*2;
				int boxH = tr.fontHeight*2 + pad*3;

				int x = this.width - 10 - boxW;
				int y = 10;

				ctx.fill(x, y, x + boxW, y + boxH, 0xC0_8B0000);
				ctx.drawBorder(x, y, boxW, boxH, 0xFF_B22222);

				int tx = x + pad;
				int ty = y + pad;
				ctx.drawText(tr, Text.literal(line1).formatted(Formatting.RED), tx, ty, 0xFFFF0000, false);
				ctx.drawText(tr, Text.literal(line2).formatted(Formatting.RED), tx, ty + tr.fontHeight + pad/2, 0xFFFF0000, false);
			}
			if (!VersionCheck.isDone()) {
				ctx.drawText(tr, Text.literal("Checking for updates…").formatted(Formatting.DARK_GRAY),
						10, this.height - tr.fontHeight - 10, 0xFFAAAAAA, false);
			}
		}

		private java.util.List<Text> wrapText(String text, int maxWidth, TextRenderer tr) {
			java.util.List<Text> lines = new java.util.ArrayList<>();
			String[] words = text.split(" ");
			StringBuilder line = new StringBuilder();
			for (String word : words) {
				String testLine = line.isEmpty() ? word : line + " " + word;
				if (tr.getWidth(testLine) > maxWidth) {
					lines.add(Text.of(line.toString()));
					line = new StringBuilder(word);
				} else {
					if (!line.isEmpty()) line.append(" ");
					line.append(word);
				}
			}
			if (!line.isEmpty()) lines.add(Text.of(line.toString()));
			return lines;
		}
	}

	public static void registerHud() {
		HudRenderCallback.EVENT.register((ctx, partial) -> {
			if (ModGate.isNotActive()) return;
			if (!Config.isHudEnabled()) return;
			MinecraftClient mc = MinecraftClient.getInstance();
			TextRenderer tr = mc.textRenderer;
			int w = mc.getWindow().getScaledWidth();
			int h = mc.getWindow().getScaledHeight();

			int lines = 0;
			for (int i = 0; i < ChangeKeybindScreen.FEATURES.length; i++) {
				if (Config.getFeatureState(i)) lines++;
			}
			int lineH = tr.fontHeight + 2;
			int totalH = lines * lineH;

			int pos = Config.getHudPosIndex();
			int y;
			if (pos == 1 || pos == 2) {
				y = h - totalH - 10;
			} else {
				y = 10;
			}

			for (int i = 0; i < ChangeKeybindScreen.FEATURES.length; i++) {
				if (!Config.getFeatureState(i)) continue;
				String label = ChangeKeybindScreen.FEATURES[i];
				int textW = tr.getWidth(label);

				int x;
				if (pos == 0 || pos == 1) {
					x = w - textW - 10;
				} else {
					x = 10;
				}

				ctx.drawText(tr, Text.literal(label).formatted(Formatting.RED), x, y, 0xFFFF0000, true);

				y += lineH;
			}
		});
	}
}
