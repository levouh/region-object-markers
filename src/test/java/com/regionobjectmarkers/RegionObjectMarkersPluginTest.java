package com.regionobjectmarkers;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RegionObjectMarkersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RegionObjectMarkersPlugin.class);
		RuneLite.main(args);
	}
}