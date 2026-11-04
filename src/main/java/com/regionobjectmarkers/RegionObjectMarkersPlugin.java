package com.regionobjectmarkers;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(name = "Region Object Markers", description = "Highlights named objects in selected map regions", tags = {"object", "marker", "region", "highlight"})
public class RegionObjectMarkersPlugin extends Plugin
{
	public static final String CONFIG_GROUP = "region-object-markers";
	private static final String CONFIG_KEY = "sections";
	private static final String DEBUG_HOVER_KEY = "debugHoverInfo";
	private static final String DEBUG_LABELS_KEY = "debugObjectLabels";
	private static final String DEBUG_REGIONS_KEY = "debugRegions";
	private static final String LEGACY_DEBUG_KEY = "debugUnderMouse";

	@Getter private final List<RegionMarkerSection> sections = new ArrayList<>();
	@Getter private final Map<TileObject, List<ObjectMarker>> matchedObjects = new IdentityHashMap<>();
	@Getter private final Map<TileObject, String> debugObjects = new IdentityHashMap<>();
	private final Map<String, List<ObjectMarker>> activeMarkersByName = new HashMap<>();
	private final List<WildcardMarker> activeWildcardMarkers = new ArrayList<>();
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ConfigManager configManager;
	@Inject private Gson gson;
	@Inject private ClientToolbar clientToolbar;
	@Inject private OverlayManager overlayManager;
	@Inject private RegionObjectMarkersOverlay overlay;
	@Inject private RegionObjectMarkersDebugOverlay debugOverlay;
	@Inject private RegionObjectMarkersConfig config;
	@Getter @Inject private ColorPickerManager colourPickerManager;
	private RegionObjectMarkersPluginPanel pluginPanel;
	private NavigationButton navigationButton;
	private int playerRegionId = -1;

	@Provides
	RegionObjectMarkersConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(RegionObjectMarkersConfig.class);
	}

	@Override protected void startUp()
	{
		loadSections();
		overlayManager.add(overlay);
		overlayManager.add(debugOverlay);
		pluginPanel = new RegionObjectMarkersPluginPanel(client, this, clientThread);
		pluginPanel.rebuild();
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navigationButton = NavigationButton.builder().tooltip("Region Object Markers").icon(icon).priority(5).panel(pluginPanel).build();
		clientToolbar.addNavigation(navigationButton);
		clientThread.invokeLater(this::rebuildTrackedObjects);
	}

	@Override protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(debugOverlay);
		matchedObjects.clear();
		debugObjects.clear();
		activeMarkersByName.clear();
		sections.clear();
		clientToolbar.removeNavigation(navigationButton);
		pluginPanel = null;
		navigationButton = null;
	}

	@Subscribe public void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()) && CONFIG_KEY.equals(event.getKey()))
		{
			String expected = sections.isEmpty() ? null : gson.toJson(sections);
			String persisted = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
			if (java.util.Objects.equals(expected, persisted)) return;
			loadSections();
			clientThread.invokeLater(this::rebuildTrackedObjects);
			rebuild();
		}
	}
	@Subscribe public void onGameStateChanged(net.runelite.api.events.GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			playerRegionId = currentPlayerRegionId();
			rebuildTrackedObjects();
		}
	}
	@Subscribe public void onGameTick(GameTick event)
	{
		int currentRegionId = currentPlayerRegionId();
		if (currentRegionId != playerRegionId)
		{
			playerRegionId = currentRegionId;
			rebuildTrackedObjects();
			rebuild();
		}
	}
	@Subscribe public void onGameObjectSpawned(GameObjectSpawned e) { track(e.getGameObject()); }
	@Subscribe public void onGameObjectDespawned(GameObjectDespawned e) { untrack(e.getGameObject()); }
	@Subscribe public void onWallObjectSpawned(WallObjectSpawned e) { track(e.getWallObject()); }
	@Subscribe public void onWallObjectDespawned(WallObjectDespawned e) { untrack(e.getWallObject()); }
	@Subscribe public void onDecorativeObjectSpawned(DecorativeObjectSpawned e) { track(e.getDecorativeObject()); }
	@Subscribe public void onDecorativeObjectDespawned(DecorativeObjectDespawned e) { untrack(e.getDecorativeObject()); }
	@Subscribe public void onGroundObjectSpawned(GroundObjectSpawned e) { track(e.getGroundObject()); }
	@Subscribe public void onGroundObjectDespawned(GroundObjectDespawned e) { untrack(e.getGroundObject()); }

	void addMarkerSection()
	{
		sections.add(new RegionMarkerSection("Marker " + (sections.size() + 1)));
		saveSections();
		rebuild();
	}
	ObjectMarker newObjectMarker()
	{
		ObjectMarker marker = new ObjectMarker();
		marker.setStyle(config.defaultStyle());
		marker.setFillColor(config.defaultFillColor());
		marker.setOutlineColor(config.defaultOutlineColor());
		return marker;
	}
	void removeSection(RegionMarkerSection section) { sections.remove(section); saveSections(); rebuild(); }
	void saveSections()
	{
		saveSections(true);
	}
	void saveSections(boolean refreshTrackedObjects)
	{
		if (sections.isEmpty()) configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY);
		else configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, gson.toJson(sections));
		if (refreshTrackedObjects) clientThread.invokeLater(this::rebuildTrackedObjects);
	}
	String copySections(String searchTerm, Filter filter)
	{
		List<RegionMarkerSection> matching = new ArrayList<>();
		String normalizedSearch = searchTerm == null ? "" : searchTerm.trim().toLowerCase();
		for (RegionMarkerSection section : sections)
		{
			if (section.getName().toLowerCase().contains(normalizedSearch) && matchesFilter(section, filter))
			{
				matching.add(section);
			}
		}
		return matching.isEmpty() ? null : gson.toJson(matching);
	}

	boolean pasteSections(String json)
	{
		if (Strings.isNullOrEmpty(json)) return false;
		try
		{
			List<RegionMarkerSection> imported = gson.fromJson(json, new TypeToken<ArrayList<RegionMarkerSection>>() {}.getType());
			if (imported == null) return false;
			imported.removeIf(section -> section == null || Strings.isNullOrEmpty(section.getName()));
			for (RegionMarkerSection section : imported)
			{
				section.normalizeRegionIds();
				if (section.getObjects() == null) section.setObjects(new ArrayList<>());
			}
			if (imported.isEmpty()) return false;
			sections.addAll(imported);
			saveSections();
			rebuild();
			return true;
		}
		catch (IllegalStateException | JsonSyntaxException ex)
		{
			JOptionPane.showMessageDialog(pluginPanel, "The imported region object markers are malformed.", "Region Object Markers", JOptionPane.WARNING_MESSAGE);
			return false;
		}
	}

	boolean isPlayerInSectionRegion(RegionMarkerSection section)
	{
		return section.isAllRegions() || section.getRegionIds().contains(currentPlayerRegionId());
	}
	private int currentPlayerRegionId()
	{
		return client.getLocalPlayer() == null ? -1 : client.getLocalPlayer().getWorldLocation().getRegionID();
	}
	boolean isDebugHoverInfo() { return debugEnabled(DEBUG_HOVER_KEY, true); }
	boolean isDebugObjectLabels() { return debugEnabled(DEBUG_LABELS_KEY, true); }
	boolean isDebugRegions() { return debugEnabled(DEBUG_REGIONS_KEY, false); }
	Color getDebugTextColor() { return config.debugTextColor(); }
	void setDebugHoverInfo(boolean enabled)
	{
		configManager.setConfiguration(CONFIG_GROUP, DEBUG_HOVER_KEY, enabled);
	}
	void setDebugObjectLabels(boolean enabled)
	{
		configManager.setConfiguration(CONFIG_GROUP, DEBUG_LABELS_KEY, enabled);
		clientThread.invokeLater(() ->
		{
			if (enabled)
			{
				rebuildTrackedObjects();
			}
			else
			{
				debugObjects.clear();
			}
		});
	}
	void setDebugRegions(boolean enabled)
	{
		configManager.setConfiguration(CONFIG_GROUP, DEBUG_REGIONS_KEY, enabled);
	}
	private boolean debugEnabled(String key, boolean legacyDefault)
	{
		String value = configManager.getConfiguration(CONFIG_GROUP, key);
		if (value != null) return Boolean.parseBoolean(value);
		return legacyDefault && Boolean.parseBoolean(configManager.getConfiguration(CONFIG_GROUP, LEGACY_DEBUG_KEY));
	}
	boolean matchesFilter(RegionMarkerSection section, Filter filter)
	{
		switch (filter)
		{
			case REGION: return isPlayerInSectionRegion(section);
			case VISIBLE: return section.isVisible() || section.getObjects().stream().anyMatch(ObjectMarker::isVisible);
			case INVISIBLE: return !section.isVisible() || section.getObjects().stream().anyMatch(marker -> !marker.isVisible());
			default: return true;
		}
	}

	private void loadSections()
	{
		sections.clear();
		String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
		if (Strings.isNullOrEmpty(json)) return;
		try
		{
			List<RegionMarkerSection> loaded = gson.fromJson(json, new TypeToken<ArrayList<RegionMarkerSection>>() {}.getType());
			if (loaded == null) return;
			loaded.removeIf(section -> section == null || Strings.isNullOrEmpty(section.getName()));
			for (RegionMarkerSection section : loaded)
			{
				if (section.getObjects() == null) section.setObjects(new ArrayList<>());
				section.normalizeRegionIds();
				sections.add(section);
			}
		}
		catch (IllegalStateException | JsonSyntaxException ex)
		{
			JOptionPane.showMessageDialog(pluginPanel, "The saved region object markers are malformed.", "Region Object Markers", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void rebuildTrackedObjects()
	{
		matchedObjects.clear();
		debugObjects.clear();
		rebuildActiveMarkerIndex();
		if (client.getGameState() != GameState.LOGGED_IN || client.getScene() == null) return;
		for (Tile[][] plane : client.getScene().getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null) continue;
					for (GameObject object : tile.getGameObjects()) if (object != null) track(object);
					if (tile.getWallObject() != null) track(tile.getWallObject());
					if (tile.getDecorativeObject() != null) track(tile.getDecorativeObject());
					if (tile.getGroundObject() != null) track(tile.getGroundObject());
				}
			}
		}
	}
	private void rebuildActiveMarkerIndex()
	{
		activeMarkersByName.clear();
		activeWildcardMarkers.clear();
		for (RegionMarkerSection section : sections)
		{
			if (!section.isVisible() || !isPlayerInSectionRegion(section)) continue;
			for (ObjectMarker marker : section.getObjects())
			{
				if (!marker.isVisible() || Strings.isNullOrEmpty(marker.getName())) continue;
				String name = marker.getName().trim().toLowerCase(java.util.Locale.ROOT);
				if (name.isEmpty()) continue;
				if (name.indexOf('*') >= 0) activeWildcardMarkers.add(new WildcardMarker(Pattern.compile(Pattern.quote(name).replace("*", "\\E.*\\Q"), Pattern.CASE_INSENSITIVE), marker));
				else activeMarkersByName.computeIfAbsent(name, ignored -> new ArrayList<>()).add(marker);
			}
		}
	}
	private void track(TileObject object)
	{
		matchedObjects.remove(object);
		String objectName = client.getObjectDefinition(object.getId()).getName();
		if (Strings.isNullOrEmpty(objectName) || "null".equalsIgnoreCase(objectName)) return;
		if (isDebugObjectLabels())
		{
			WorldPoint sourcePoint = WorldPoint.fromLocalInstance(client, object.getLocalLocation(), object.getPlane());
			debugObjects.put(object, objectName + " [region " + sourcePoint.getRegionID() + "]");
		}
		if (activeMarkersByName.isEmpty() && activeWildcardMarkers.isEmpty()) return;
		String normalizedName = objectName.toLowerCase(java.util.Locale.ROOT);
		List<ObjectMarker> matches = new ArrayList<>();
		List<ObjectMarker> exactMatches = activeMarkersByName.get(normalizedName);
		if (exactMatches != null) matches.addAll(exactMatches);
		for (WildcardMarker wildcard : activeWildcardMarkers) if (wildcard.pattern.matcher(normalizedName).matches()) matches.add(wildcard.marker);
		if (!matches.isEmpty()) matchedObjects.put(object, matches);
	}
	private void untrack(TileObject object)
	{
		matchedObjects.remove(object);
		debugObjects.remove(object);
	}
	void rebuild() { if (pluginPanel != null) SwingUtilities.invokeLater(pluginPanel::rebuild); }

	private static final class WildcardMarker
	{
		private final Pattern pattern;
		private final ObjectMarker marker;
		private WildcardMarker(Pattern pattern, ObjectMarker marker) { this.pattern = pattern; this.marker = marker; }
	}

}
