# Region Object Markers

Highlights named RuneScape objects inside configured map regions.

Create a section, choose all regions or enter one or more region IDs, then add object names to highlight. Each object marker has its own style, fill color, outline color, and visibility toggle. Object names support `*` wildcards.

## Using the side panel

Most configuration is done from the **Object Markers** side panel:

- Click the green `+` to add a section.
- Give the section a name with its pencil icon, then save with the green checkmark.
- Under **Region Selection**, enable **All regions** or edit the comma-separated **Region IDs** list. Saved IDs are retained while All regions is enabled.
- Under **Object Markers**, add one or more object names. Use the pencil/checkmark controls to edit and save a name.
- Choose a highlight style: **Hull**, **Clickbox**, **Tile**, or **Outline**. Fill and Outline controls set the corresponding colors.
- Eye icons show or hide a whole section or one object marker. The top filter cycles through all, current-region, shown, and hidden sections.

The import/export icons copy or restore the currently searched and filtered sections. Search matches section names.

## Hover information

Click the region-information icon in the panel header to toggle hover information. It shows the hovered tile outline, world location, hovered region, player region, and object names/IDs under the cursor. It is useful for finding a region ID or confirming an object name before adding a marker.

## Settings

The normal RuneLite Settings panel only controls defaults for new object markers:

- **Default highlight style**
- **Default fill color**
- **Default outline color**

Existing markers keep their own values. All section and object configuration is stored in the active RuneLite profile and is primarily managed through the side panel.

## RuneLite guideline compatibility

Objects are configured and matched by their in-game names, not object IDs. Region IDs only limit where a section is active; they do not identify the objects being marked. The plugin does not accept object IDs as input, add game actions, or automate input.
