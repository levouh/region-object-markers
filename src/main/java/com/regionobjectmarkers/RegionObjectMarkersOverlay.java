package com.regionobjectmarkers;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import java.util.Map;
import net.runelite.api.TileObject;
import net.runelite.api.GameObject;
import net.runelite.api.WallObject;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GroundObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class RegionObjectMarkersOverlay extends Overlay
{
	private static final BasicStroke[] OUTLINE_STROKES = outlineStrokes();
	private final RegionObjectMarkersPlugin plugin;

	@Inject
	RegionObjectMarkersOverlay(RegionObjectMarkersPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (Map.Entry<TileObject, List<ObjectMarker>> match : plugin.getMatchedObjects().entrySet())
		{
			for (ObjectMarker marker : match.getValue()) renderMarker(graphics, match.getKey(), marker);
		}
		return null;
	}

	private void renderMarker(Graphics2D graphics, TileObject object, ObjectMarker marker)
	{
		Shape shape = marker.getStyle() == MarkerStyle.TILE ? object.getCanvasTilePoly()
			: marker.getStyle() == MarkerStyle.CLICKBOX ? object.getClickbox() : convexHull(object);
		if (shape == null) return;
		if (marker.getStyle() == MarkerStyle.OUTLINE)
		{
			if (marker.getOutlineWidth() == 0) return;
			graphics.setColor(marker.getOutlineColor());
			graphics.setStroke(outlineStroke(marker));
			graphics.draw(shape);
			return;
		}
		if (marker.getOutlineWidth() == 0)
		{
			graphics.setColor(marker.getFillColor());
			graphics.fill(shape);
			return;
		}
		// OverlayUtil takes the outline colour first and the fill colour second.
		OverlayUtil.renderPolygon(graphics, shape, marker.getOutlineColor(), marker.getFillColor(), outlineStroke(marker));
	}

	private static BasicStroke outlineStroke(ObjectMarker marker)
	{
		return OUTLINE_STROKES[Math.max(1, Math.min(10, marker.getOutlineWidth())) - 1];
	}

	private static BasicStroke[] outlineStrokes()
	{
		BasicStroke[] strokes = new BasicStroke[10];
		for (int width = 1; width <= strokes.length; width++) strokes[width - 1] = new BasicStroke(width);
		return strokes;
	}

	private Shape convexHull(TileObject object)
	{
		if (object instanceof GameObject) return ((GameObject) object).getConvexHull();
		if (object instanceof WallObject) return ((WallObject) object).getConvexHull();
		if (object instanceof DecorativeObject) return ((DecorativeObject) object).getConvexHull();
		if (object instanceof GroundObject) return ((GroundObject) object).getConvexHull();
		return null;
	}
}
