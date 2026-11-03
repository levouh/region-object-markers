package com.regionobjectmarkers;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class RegionObjectMarkersDebugOverlay extends Overlay
{
	private final Client client;
	private final RegionObjectMarkersPlugin plugin;
	@Inject RegionObjectMarkersDebugOverlay(Client client, RegionObjectMarkersPlugin plugin)
	{
		this.client = client; this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE);
	}
	@Override public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isDebugUnderMouse()) return null;
		Tile tile = client.getSelectedSceneTile();
		if (tile == null || tile.getWorldLocation() == null) return null;
		WorldPoint point = tile.getWorldLocation();
		List<String> lines = new ArrayList<>();
		lines.add("World location: " + point.getX() + ", " + point.getY() + ", " + point.getPlane());
		lines.add("Hovered region: " + point.getRegionID() + " location: " + (point.getX() & 63) + ", " + (point.getY() & 63));
		if (client.getLocalPlayer() != null) lines.add("Player region: " + client.getLocalPlayer().getWorldLocation().getRegionID());
		Shape tilePolygon = Perspective.getCanvasTilePoly(client, tile.getLocalLocation(), point.getPlane());
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		java.awt.Point canvasMouse = new java.awt.Point(mouse.getX(), mouse.getY());
		Set<TileObject> hoveredObjects = new LinkedHashSet<>();
		addNearbyHoveredObjects(hoveredObjects, tile, canvasMouse);
		for (TileObject object : hoveredObjects) addName(lines, object);
		for (Integer objectId : plugin.getHoveredMenuObjectIds()) addName(lines, objectId);
		int x = mouse.getX() + 12;
		int y = mouse.getY() + 14;
		if (tilePolygon != null)
		{
			graphics.setColor(Color.GREEN); graphics.setStroke(new BasicStroke(2)); graphics.draw(tilePolygon);
			java.awt.Rectangle bounds = tilePolygon.getBounds(); x = bounds.x + 4; y = bounds.y + bounds.height + 14;
		}
		FontMetrics metrics = graphics.getFontMetrics();
		for (String line : lines)
		{
			graphics.setColor(Color.BLACK); graphics.drawString(line, x + 1, y + 1);
			graphics.setColor(Color.WHITE); graphics.drawString(line, x, y); y += metrics.getHeight();
		}
		return null;
	}
	private void addName(List<String> lines, TileObject object)
	{
		if (object == null) return;
		String name = client.getObjectDefinition(object.getId()).getName();
		if (name != null && !"null".equalsIgnoreCase(name)) lines.add("Object: " + name + " (" + object.getId() + ")");
	}
	private void addName(List<String> lines, int objectId)
	{
		String name = client.getObjectDefinition(objectId).getName();
		String line = name == null || "null".equalsIgnoreCase(name) ? "Object ID: " + objectId : "Object: " + name + " (" + objectId + ")";
		if (!lines.contains(line)) lines.add(line);
	}
	private void addIfHovered(Set<TileObject> objects, TileObject object, java.awt.Point mouse)
	{
		if (object == null) return;
		Shape clickbox = object.getClickbox();
		Shape hull = convexHull(object);
		if ((clickbox != null && clickbox.contains(mouse)) || (hull != null && hull.contains(mouse))) objects.add(object);
	}
	private void addNearbyHoveredObjects(Set<TileObject> objects, Tile hoveredTile, java.awt.Point mouse)
	{
		int plane = client.getPlane();
		Tile[][][] tiles = client.getScene().getTiles();
		if (plane < 0 || plane >= tiles.length) return;
		net.runelite.api.Point scenePoint = hoveredTile.getSceneLocation();
		for (int x = Math.max(0, scenePoint.getX() - 2); x <= Math.min(tiles[plane].length - 1, scenePoint.getX() + 2); x++)
		{
			for (int y = Math.max(0, scenePoint.getY() - 2); y <= Math.min(tiles[plane][x].length - 1, scenePoint.getY() + 2); y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null) continue;
				addIfHovered(objects, tile.getWallObject(), mouse); addIfHovered(objects, tile.getDecorativeObject(), mouse); addIfHovered(objects, tile.getGroundObject(), mouse);
				for (GameObject object : tile.getGameObjects()) addIfHovered(objects, object, mouse);
			}
		}
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
