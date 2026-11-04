package com.regionobjectmarkers;

import com.google.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class RegionObjectMarkersDebugOverlay extends Overlay
{
	private static final int REGION_SIZE = 64;
	private static final int CHUNK_SIZE = 8;
	private static final int REGION_HEIGHT_OFFSET = 32;
	private static final BasicStroke HOVER_STROKE = new BasicStroke(2);
	private static final BasicStroke REGION_STROKE = new BasicStroke(1);
	private final Client client;
	private final RegionObjectMarkersPlugin plugin;
	private final List<RegionGeometry> cachedRegionGeometry = new ArrayList<>();
	private Scene cachedRegionScene;
	private int cachedRegionPlane = -1;
	private int cachedRegionBaseX;
	private int cachedRegionBaseY;
	private int cachedRegionWidth;
	private int cachedRegionHeight;
	private boolean cachedRegionInstance;
	private int cachedInstanceTemplateFingerprint;

	@Inject
	RegionObjectMarkersDebugOverlay(Client client, RegionObjectMarkersPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean hoverInfo = plugin.isDebugHoverInfo();
		boolean objectLabels = plugin.isDebugObjectLabels();
		boolean regions = plugin.isDebugRegions();
		if (!hoverInfo && !objectLabels && !regions)
		{
			return null;
		}

		if (regions)
		{
			renderRegions(graphics);
		}
		if (objectLabels)
		{
			renderObjectLabels(graphics);
		}
		if (hoverInfo)
		{
			renderHoverInfo(graphics);
		}
		return null;
	}

	private void renderHoverInfo(Graphics2D graphics)
	{
		Tile tile = client.getSelectedSceneTile();
		if (tile == null || tile.getWorldLocation() == null)
		{
			return;
		}

		WorldPoint point = WorldPoint.fromLocalInstance(client, tile.getLocalLocation(), tile.getPlane());
		List<String> lines = new ArrayList<>();
		lines.add("World location: " + point.getX() + ", " + point.getY() + ", " + point.getPlane());
		lines.add("Hovered region: " + point.getRegionID() + " location: " + (point.getX() & 63) + ", " + (point.getY() & 63));
		if (client.getLocalPlayer() != null)
		{
			WorldPoint playerPoint = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
			lines.add("Player region: " + playerPoint.getRegionID());
		}

		Shape tilePolygon = Perspective.getCanvasTilePoly(client, tile.getLocalLocation(), point.getPlane());
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int x = mouse.getX() + 12;
		int y = mouse.getY() + 14;
		if (tilePolygon != null)
		{
			graphics.setColor(plugin.getDebugTextColor());
			graphics.setStroke(HOVER_STROKE);
			graphics.draw(tilePolygon);
			java.awt.Rectangle bounds = tilePolygon.getBounds();
			x = bounds.x + 4;
			y = bounds.y + bounds.height + 14;
		}

		FontMetrics metrics = graphics.getFontMetrics();
		for (String line : lines)
		{
			graphics.setColor(Color.BLACK);
			graphics.drawString(line, x + 1, y + 1);
			graphics.setColor(plugin.getDebugTextColor());
			graphics.drawString(line, x, y);
			y += metrics.getHeight();
		}
	}

	private void renderObjectLabels(Graphics2D graphics)
	{
		for (Map.Entry<TileObject, String> entry : plugin.getDebugObjects().entrySet())
		{
			net.runelite.api.Point location = entry.getKey().getCanvasTextLocation(graphics, entry.getValue(), 0);
			if (location != null)
			{
				OverlayUtil.renderTextLocation(graphics, location, entry.getValue(), plugin.getDebugTextColor());
			}
		}
	}

	private void renderRegions(Graphics2D graphics)
	{
		Scene scene = client.getScene();
		Tile[][][] tiles = scene == null ? null : scene.getTiles();
		int plane = client.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length || tiles[plane].length == 0)
		{
			return;
		}
		int elevation = regionElevation(tiles[plane], plane);

		List<RegionGeometry> regions = regionGeometry(scene, tiles[plane], plane);
		graphics.setColor(plugin.getDebugTextColor());
		graphics.setStroke(REGION_STROKE);
		for (RegionGeometry region : regions)
		{
			for (RegionEdge edge : region.edges)
			{
				renderRegionEdge(graphics, scene, edge.startX, edge.startY, edge.endX, edge.endY, elevation);
			}
			renderRegionLabel(graphics, scene, region.labelX, region.labelY, elevation, region.label);
		}
	}

	private List<RegionGeometry> regionGeometry(Scene scene, Tile[][] tiles, int plane)
	{
		int templateFingerprint = scene.isInstance() ? instanceTemplateFingerprint(scene.getInstanceTemplateChunks(), plane) : 0;
		if (scene == cachedRegionScene && scene.isInstance() == cachedRegionInstance && plane == cachedRegionPlane && scene.getBaseX() == cachedRegionBaseX
			&& scene.getBaseY() == cachedRegionBaseY && tiles.length == cachedRegionWidth
			&& tiles[0].length == cachedRegionHeight && templateFingerprint == cachedInstanceTemplateFingerprint)
		{
			return cachedRegionGeometry;
		}

		cachedRegionGeometry.clear();
		if (scene.isInstance())
		{
			buildInstanceRegionGeometry(scene, tiles, plane, cachedRegionGeometry);
		}
		else
		{
			buildWorldRegionGeometry(scene, tiles, plane, cachedRegionGeometry);
		}
		cachedRegionScene = scene;
		cachedRegionPlane = plane;
		cachedRegionBaseX = scene.getBaseX();
		cachedRegionBaseY = scene.getBaseY();
		cachedRegionWidth = tiles.length;
		cachedRegionHeight = tiles[0].length;
		cachedRegionInstance = scene.isInstance();
		cachedInstanceTemplateFingerprint = templateFingerprint;
		return cachedRegionGeometry;
	}

	private int instanceTemplateFingerprint(int[][][] templateChunks, int plane)
	{
		if (templateChunks == null || plane < 0 || plane >= templateChunks.length || templateChunks[plane] == null)
		{
			return 0;
		}
		int fingerprint = 1;
		for (int[] column : templateChunks[plane])
		{
			fingerprint = 31 * fingerprint + (column == null ? 0 : column.length);
			if (column != null)
			{
				for (int chunk : column) fingerprint = 31 * fingerprint + chunk;
			}
		}
		return fingerprint;
	}

	private int regionElevation(Tile[][] tiles, int plane)
	{
		if (client.getLocalPlayer() != null)
		{
			return Perspective.getTileHeight(client, client.getLocalPlayer().getLocalLocation(), plane) - REGION_HEIGHT_OFFSET;
		}
		for (Tile[] column : tiles)
		{
			for (Tile tile : column)
			{
				if (tile != null)
				{
					return Perspective.getTileHeight(client, tile.getLocalLocation(), plane) - REGION_HEIGHT_OFFSET;
				}
			}
		}
		return -REGION_HEIGHT_OFFSET;
	}

	private void buildWorldRegionGeometry(Scene scene, Tile[][] tiles, int plane, List<RegionGeometry> regions)
	{
		int firstX = -(scene.getBaseX() & (REGION_SIZE - 1));
		int firstY = -(scene.getBaseY() & (REGION_SIZE - 1));
		for (int x = firstX; x < tiles.length; x += REGION_SIZE)
		{
			int startX = Math.max(0, x);
			int width = Math.min(tiles.length, x + REGION_SIZE) - startX;
			for (int y = firstY; y < tiles[startX].length; y += REGION_SIZE)
			{
				int startY = Math.max(0, y);
				int height = Math.min(tiles[startX].length, y + REGION_SIZE) - startY;
				if (width <= 0 || height <= 0)
				{
					continue;
				}
				WorldPoint point = WorldPoint.fromLocalInstance(client, LocalPoint.fromScene(startX, startY, scene), plane);
				regions.add(RegionGeometry.rectangle(startX, startY, width, height, "Region " + point.getRegionID()));
			}
		}
	}

	private void buildInstanceRegionGeometry(Scene scene, Tile[][] tiles, int plane, List<RegionGeometry> regions)
	{
		int chunksX = tiles.length / CHUNK_SIZE;
		int chunksY = tiles[0].length / CHUNK_SIZE;
		int[][][] templateChunks = scene.getInstanceTemplateChunks();
		int[][] regionIds = new int[chunksX][chunksY];
		for (int[] column : regionIds)
		{
			Arrays.fill(column, -1);
		}

		for (int x = 0; x < chunksX; x++)
		{
			for (int y = 0; y < chunksY; y++)
			{
				if (!isMappedInstanceChunk(templateChunks, plane, x, y))
				{
					continue;
				}
				regionIds[x][y] = WorldPoint.fromLocalInstance(client, LocalPoint.fromScene(x * CHUNK_SIZE, y * CHUNK_SIZE, scene), plane).getRegionID();
			}
		}

		boolean[][] visited = new boolean[chunksX][chunksY];
		for (int x = 0; x < chunksX; x++)
		{
			for (int y = 0; y < chunksY; y++)
			{
				if (visited[x][y] || regionIds[x][y] == -1)
				{
					continue;
				}
				int regionId = regionIds[x][y];
				ArrayDeque<int[]> pending = new ArrayDeque<>();
				pending.add(new int[] {x, y});
				visited[x][y] = true;
				int centerX = 0;
				int centerY = 0;
				int count = 0;
				List<int[]> component = new ArrayList<>();
				while (!pending.isEmpty())
				{
					int[] chunk = pending.removeFirst();
					component.add(chunk);
					centerX += chunk[0] * CHUNK_SIZE + CHUNK_SIZE / 2;
					centerY += chunk[1] * CHUNK_SIZE + CHUNK_SIZE / 2;
					count++;
					addMatchingChunk(pending, visited, regionIds, chunk[0] - 1, chunk[1], regionId);
					addMatchingChunk(pending, visited, regionIds, chunk[0] + 1, chunk[1], regionId);
					addMatchingChunk(pending, visited, regionIds, chunk[0], chunk[1] - 1, regionId);
					addMatchingChunk(pending, visited, regionIds, chunk[0], chunk[1] + 1, regionId);
				}
				regions.add(instanceRegionGeometry(regionIds, component, centerX / count, centerY / count, "Region " + regionId));
			}
		}
	}

	private boolean isMappedInstanceChunk(int[][][] templateChunks, int plane, int x, int y)
	{
		return templateChunks != null && plane >= 0 && plane < templateChunks.length
			&& templateChunks[plane] != null && x >= 0 && x < templateChunks[plane].length
			&& templateChunks[plane][x] != null && y >= 0 && y < templateChunks[plane][x].length
			&& templateChunks[plane][x][y] != -1;
	}

	private RegionGeometry instanceRegionGeometry(int[][] regionIds, List<int[]> component, int labelX, int labelY, String label)
	{
		List<RegionEdge> edges = new ArrayList<>();
		for (int[] chunk : component)
		{
			int x = chunk[0];
			int y = chunk[1];
			int startX = x * CHUNK_SIZE;
			int startY = y * CHUNK_SIZE;
			int regionId = regionIds[x][y];
			if (!hasSameRegion(regionIds, x, y - 1, regionId)) edges.add(new RegionEdge(startX, startY, startX + CHUNK_SIZE, startY));
			if (!hasSameRegion(regionIds, x + 1, y, regionId)) edges.add(new RegionEdge(startX + CHUNK_SIZE, startY, startX + CHUNK_SIZE, startY + CHUNK_SIZE));
			if (!hasSameRegion(regionIds, x, y + 1, regionId)) edges.add(new RegionEdge(startX + CHUNK_SIZE, startY + CHUNK_SIZE, startX, startY + CHUNK_SIZE));
			if (!hasSameRegion(regionIds, x - 1, y, regionId)) edges.add(new RegionEdge(startX, startY + CHUNK_SIZE, startX, startY));
		}
		return new RegionGeometry(edges, labelX, labelY, label);
	}

	private boolean hasSameRegion(int[][] regionIds, int x, int y, int regionId)
	{
		return x >= 0 && y >= 0 && x < regionIds.length && y < regionIds[0].length && regionIds[x][y] == regionId;
	}

	private void addMatchingChunk(ArrayDeque<int[]> pending, boolean[][] visited, int[][] regionIds, int x, int y, int regionId)
	{
		if (x >= 0 && y >= 0 && x < regionIds.length && y < regionIds[0].length && !visited[x][y] && regionIds[x][y] == regionId)
		{
			visited[x][y] = true;
			pending.add(new int[] {x, y});
		}
	}

	private void renderRegionEdge(Graphics2D graphics, Scene scene, int startX, int startY, int endX, int endY, int elevation)
	{
		int segments = Math.max(1, Math.max(Math.abs(endX - startX), Math.abs(endY - startY)) / CHUNK_SIZE);
		net.runelite.api.Point previous = projectRegionPoint(scene, startX, startY, elevation);
		for (int segment = 1; segment <= segments; segment++)
		{
			int x = startX + (endX - startX) * segment / segments;
			int y = startY + (endY - startY) * segment / segments;
			net.runelite.api.Point current = projectRegionPoint(scene, x, y, elevation);
			if (previous != null && current != null)
			{
				graphics.drawLine(previous.getX(), previous.getY(), current.getX(), current.getY());
			}
			previous = current;
		}
	}

	private net.runelite.api.Point projectRegionPoint(Scene scene, int x, int y, int elevation)
	{
		LocalPoint point = LocalPoint.fromScene(x, y, scene);
		return Perspective.localToCanvas(client, point.getX(), point.getY(), elevation);
	}

	private void renderRegionLabel(Graphics2D graphics, Scene scene, int x, int y, int elevation, String label)
	{
		net.runelite.api.Point location = projectRegionPoint(scene, x, y, elevation);
		if (location != null)
		{
			OverlayUtil.renderTextLocation(graphics, location, label, plugin.getDebugTextColor());
		}
	}

	private static final class RegionGeometry
	{
		private final List<RegionEdge> edges;
		private final int labelX;
		private final int labelY;
		private final String label;

		private RegionGeometry(List<RegionEdge> edges, int labelX, int labelY, String label)
		{
			this.edges = edges;
			this.labelX = labelX;
			this.labelY = labelY;
			this.label = label;
		}

		private static RegionGeometry rectangle(int x, int y, int width, int height, String label)
		{
			List<RegionEdge> edges = new ArrayList<>(4);
			edges.add(new RegionEdge(x, y, x + width, y));
			edges.add(new RegionEdge(x + width, y, x + width, y + height));
			edges.add(new RegionEdge(x + width, y + height, x, y + height));
			edges.add(new RegionEdge(x, y + height, x, y));
			return new RegionGeometry(edges, x + width / 2, y + height / 2, label);
		}
	}

	private static final class RegionEdge
	{
		private final int startX;
		private final int startY;
		private final int endX;
		private final int endY;

		private RegionEdge(int startX, int startY, int endX, int endY)
		{
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
		}
	}

}
