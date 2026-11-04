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

## Debug controls

The icon row below the panel header has independent debug controls:

- **Hover** shows the hovered tile outline, source world location, hovered region, and player region.
- **Objects** draws object names with their source-region labels above scene objects.
- **Regions** draws visible region boundaries and labels each region. In instances, it draws the 8×8 source chunks and labels connected chunks from the same source region.

Use Hover to find a region ID, Objects to confirm an object name, and Regions when a busy scene makes region boundaries unclear.

## Settings

The normal RuneLite Settings panel only controls defaults for new object markers:

- **Default highlight style**
- **Default fill color**
- **Default outline color**
- **Debug text color**

Existing markers keep their own values. All section and object configuration is stored in the active RuneLite profile and is primarily managed through the side panel.

## RuneLite guideline compatibility

Objects are configured and matched by their in-game names, not object IDs. Region IDs only limit where a section is active; they do not identify the objects being marked. The plugin does not accept object IDs as input, add game actions, or automate input.
