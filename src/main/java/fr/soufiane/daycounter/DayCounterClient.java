package fr.soufiane.daycounter;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/**
 * Le texte est dessiné en haute qualité (Java2D, vrai contour vectoriel, ombre adoucie)
 * à la résolution exacte de l'écran, puis affiché pixel pour pixel : aucun étirement, donc aucun flou.
 * L'image n'est régénérée que quand le jour ou l'échelle d'interface change.
 */
public class DayCounterClient implements ClientModInitializer {
	public static final String MOD_ID = "daycounter";

	// ===== Réglages (unités = pixels d'interface Minecraft) =====
	private static final int POS_X = 12;
	private static final int POS_Y = 12;
	private static final float FONT_SIZE = 20f;          // taille du texte
	private static final float OUTLINE_WIDTH = 1.5f;     // épaisseur du contour
	private static final float SHADOW_OFFSET = 1.5f;     // décalage de l'ombre
	private static final float SHADOW_BLUR = 0f;         // flou de l'ombre (0 = ombre nette)
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final Color OUTLINE_COLOR = new Color(135, 206, 235);      // bleu ciel
	private static final Color SHADOW_COLOR = new Color(0, 0, 0, 160);        // noir ~63 %
	private static final float SHARPNESS = 1.6f;         // netteté des bords (1 = lissage normal, plus haut = plus net)
	private static final String LABEL = "Jour ";
	// ============================================================

	private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "day_counter_label");

	private static java.awt.Font baseFont;
	private static String cachedText;
	private static int cachedScale = -1;
	private static int guiWidth;
	private static int guiHeight;
	private static boolean textureReady;

	@Override
	public void onInitializeClient() {
		try (InputStream in = DayCounterClient.class.getResourceAsStream("/assets/daycounter/font/montserrat_black.ttf")) {
			baseFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
		} catch (Exception e) {
			throw new RuntimeException("[DayCounter] Impossible de charger Montserrat Black", e);
		}

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "day_counter"),
				DayCounterClient::extract);
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		long day = mc.level.getOverworldClockTime() / 24000L + 1L;
		String text = LABEL + day;
		int scale = Math.max(1, (int) Math.round(mc.getWindow().getGuiScale()));

		if (!text.equals(cachedText) || scale != cachedScale) {
			rebuildTexture(mc, text, scale);
			cachedText = text;
			cachedScale = scale;
		}

		if (textureReady) {
			// Taille affichée = taille de l'image / échelle → 1 pixel d'image = 1 pixel d'écran
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, POS_X, POS_Y, 0f, 0f,
					guiWidth, guiHeight, guiWidth, guiHeight);
		}
	}

	private static void rebuildTexture(Minecraft mc, String text, int scale) {
		float px = scale; // pixels écran par pixel d'interface
		java.awt.Font font = baseFont.deriveFont(FONT_SIZE * px);
		FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
		TextLayout layout = new TextLayout(text, font, frc);
		Rectangle2D bounds = layout.getBounds();

		float stroke = OUTLINE_WIDTH * px;
		float shadow = Math.round(SHADOW_OFFSET * px);
		float blur = SHADOW_BLUR * px;
		float margin = stroke + shadow + blur * 2 + 2;

		// Dimensions arrondies à un multiple de l'échelle pour un rendu pixel-perfect
		guiWidth = (int) Math.ceil((bounds.getWidth() + margin * 2) / px);
		guiHeight = (int) Math.ceil((bounds.getHeight() + margin * 2) / px);
		int w = guiWidth * scale;
		int h = guiHeight * scale;

		double ox = Math.round(margin - bounds.getX());
		double oy = Math.round(margin - bounds.getY());
		Shape glyphs = layout.getOutline(AffineTransform.getTranslateInstance(ox, oy));
		BasicStroke outlineStroke = new BasicStroke(stroke * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Shape outlined = outlineStroke.createStrokedShape(glyphs);

		// 1) Ombre (forme texte + contour), décalée puis adoucie
		BufferedImage shadowImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gs = shadowImg.createGraphics();
		hints(gs);
		gs.translate(shadow, shadow);
		gs.setColor(new Color(SHADOW_COLOR.getRed(), SHADOW_COLOR.getGreen(), SHADOW_COLOR.getBlue()));
		gs.fill(glyphs);
		gs.fill(outlined);
		gs.dispose();
		if (blur >= 1f) {
			shadowImg = boxBlur(shadowImg, Math.round(blur));
		}

		// 2) Contour + texte par-dessus
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		hints(g);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SHADOW_COLOR.getAlpha() / 255f));
		g.drawImage(shadowImg, 0, 0, null);
		g.setComposite(AlphaComposite.SrcOver);
		g.setColor(OUTLINE_COLOR);
		g.fill(outlined);
		g.setColor(TEXT_COLOR);
		g.fill(glyphs);
		g.dispose();
		sharpenEdges(img, SHARPNESS);

		// 3) Envoi vers une texture Minecraft
		NativeImage nativeImage = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				nativeImage.setPixel(x, y, img.getRGB(x, y)); // ARGB
			}
		}

		if (textureReady) {
			mc.getTextureManager().release(TEXTURE_ID);
		}
		DynamicTexture texture = new DynamicTexture(() -> "daycounter_label", nativeImage);
		mc.getTextureManager().register(TEXTURE_ID, texture);
		texture.upload();
		textureReady = true;
	}

	private static void hints(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
	}

	/** Resserre l'anticrénelage : les pixels de bord semi-transparents deviennent plus francs. */
	private static void sharpenEdges(BufferedImage img, float k) {
		if (k <= 1f) {
			return;
		}
		int w = img.getWidth();
		int h = img.getHeight();
		int[] px = img.getRGB(0, 0, w, h, null, 0, w);
		for (int i = 0; i < px.length; i++) {
			int c = px[i];
			float a = ((c >>> 24) & 0xFF) / 255f;
			if (a <= 0f || a >= 1f) {
				continue;
			}
			float na = Math.min(1f, Math.max(0f, (a - 0.5f) * k + 0.5f));
			px[i] = (Math.round(na * 255f) << 24) | (c & 0x00FFFFFF);
		}
		img.setRGB(0, 0, w, h, px, 0, w);
	}

	/** Flou simple (2 passes horizontale + verticale) pour adoucir l'ombre. */
	private static BufferedImage boxBlur(BufferedImage src, int r) {
		int w = src.getWidth();
		int h = src.getHeight();
		int[] in = src.getRGB(0, 0, w, h, null, 0, w);
		int[] tmp = new int[in.length];
		int[] out = new int[in.length];
		blurPass(in, tmp, w, h, r, true);
		blurPass(tmp, out, w, h, r, false);
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		dst.setRGB(0, 0, w, h, out, 0, w);
		return dst;
	}

	private static void blurPass(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
		int n = r * 2 + 1;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int a = 0, rr = 0, gg = 0, bb = 0;
				for (int k = -r; k <= r; k++) {
					int sx = horizontal ? Math.min(w - 1, Math.max(0, x + k)) : x;
					int sy = horizontal ? y : Math.min(h - 1, Math.max(0, y + k));
					int c = in[sy * w + sx];
					a += (c >>> 24) & 0xFF;
					rr += (c >>> 16) & 0xFF;
					gg += (c >>> 8) & 0xFF;
					bb += c & 0xFF;
				}
				out[y * w + x] = ((a / n) << 24) | ((rr / n) << 16) | ((gg / n) << 8) | (bb / n);
			}
		}
	}
}
