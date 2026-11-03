package com.regionobjectmarkers;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
}
