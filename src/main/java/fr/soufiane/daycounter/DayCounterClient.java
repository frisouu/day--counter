package fr.soufiane.daycounter;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class DayCounterClient implements ClientModInitializer {
	public static final String MOD_ID = "daycounter";

	// ===== Réglages =====
	private static final int POS_X = 8;
	private static final int POS_Y = 8;
	private static final int TEXT_COLOR    = 0xFFFFFFFF; // blanc (ARGB)
	private static final int OUTLINE_COLOR = 0xFF87CEEB; // bleu ciel (ARGB)
	private static final int SHADOW_COLOR  = 0x99000000; // noir 60 %
	private static final int OUTLINE_SIZE  = 1;          // épaisseur contour (px GUI)
	private static final int SHADOW_OFFSET = 2;          // décalage ombre (px GUI)
	private static final String LABEL = "Jour ";
	// ====================

	private static final Style MONTSERRAT = Style.EMPTY.withFont(
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "montserrat")));

	@Override
	public void onInitializeClient() {
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

		// 24 000 ticks = 1 jour Minecraft. +1 pour commencer à "Jour 1".
		long day = mc.level.getOverworldClockTime() / 24000L + 1L;
		Component text = Component.literal(LABEL + day).withStyle(MONTSERRAT);
		Font font = mc.font;

		// 1) Ombre (décalée, sous le contour)
		for (int dx = -OUTLINE_SIZE; dx <= OUTLINE_SIZE; dx++) {
			for (int dy = -OUTLINE_SIZE; dy <= OUTLINE_SIZE; dy++) {
				graphics.text(font, text, POS_X + SHADOW_OFFSET + dx, POS_Y + SHADOW_OFFSET + dy, SHADOW_COLOR, false);
			}
		}

		// 2) Contour bleu ciel (8 directions)
		for (int dx = -OUTLINE_SIZE; dx <= OUTLINE_SIZE; dx++) {
			for (int dy = -OUTLINE_SIZE; dy <= OUTLINE_SIZE; dy++) {
				if (dx != 0 || dy != 0) {
					graphics.text(font, text, POS_X + dx, POS_Y + dy, OUTLINE_COLOR, false);
				}
			}
		}

		// 3) Texte principal
		graphics.text(font, text, POS_X, POS_Y, TEXT_COLOR, false);
	}
}
