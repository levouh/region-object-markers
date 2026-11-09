package com.regionobjectmarkers;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(RegionObjectMarkersPlugin.CONFIG_GROUP)
public interface RegionObjectMarkersConfig extends Config
{
	@ConfigItem(keyName = "defaultStyle", name = "Default highlight style", description = "Highlight style used by newly added object markers")
	default MarkerStyle defaultStyle()
	{
		return MarkerStyle.HULL;
	}

	@Alpha
	@ConfigItem(keyName = "defaultFillColor", name = "Default fill color", description = "Fill color used by newly added object markers")
	default Color defaultFillColor()
	{
		return new Color(0, 255, 0, 50);
	}

	@ConfigItem(keyName = "defaultOutlineColor", name = "Default outline color", description = "Outline color used by newly added object markers")
	default Color defaultOutlineColor()
	{
		return Color.GREEN;
	}

	@Range(min = 0, max = 10)
	@ConfigItem(keyName = "defaultOutlineWidth", name = "Default outline width", description = "Outline width used by newly added object markers; 0 disables the outline")
	default int defaultOutlineWidth()
	{
		return 2;
	}

	@ConfigItem(keyName = "defaultInteractableOnly", name = "Default interactable-only", description = "Only match objects with an available object action when creating new markers")
	default boolean defaultInteractableOnly()
	{
		return false;
	}

	@ConfigItem(keyName = "debugTextColor", name = "Debug text color", description = "Text color used by hover information and scene object labels")
	default Color debugTextColor()
	{
		return Color.GREEN;
	}
}
