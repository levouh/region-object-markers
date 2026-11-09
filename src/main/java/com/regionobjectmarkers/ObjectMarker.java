package com.regionobjectmarkers;

import java.awt.Color;
import lombok.Data;

@Data
class ObjectMarker
{
	private String name = "Object name";
	private boolean visible = true;
	private MarkerStyle style = MarkerStyle.HULL;
	private Color fillColor = new Color(0, 255, 0, 50);
	private Color outlineColor = Color.GREEN;
	private int outlineWidth = 2;
	private boolean interactableOnly;
}
