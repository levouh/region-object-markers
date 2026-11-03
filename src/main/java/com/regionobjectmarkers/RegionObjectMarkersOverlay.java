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
	private static final BasicStroke OUTLINE_STROKE = new BasicStroke(2);
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
			graphics.setColor(marker.getOutlineColor());
			graphics.setStroke(OUTLINE_STROKE);
			graphics.draw(shape);
			return;
		}
		// OverlayUtil takes the outline colour first and the fill colour second.
		OverlayUtil.renderPolygon(graphics, shape, marker.getOutlineColor(), marker.getFillColor(), OUTLINE_STROKE);
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
