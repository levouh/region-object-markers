package com.regionobjectmarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

final class RegionObjectMarkerSectionPanel extends JPanel
{
	private static final ImageIcon PENCIL_ICON;
	private static final ImageIcon RIGHT_ICON;
	private static final ImageIcon DOWN_ICON;
	private static final ImageIcon PAINT_ICON;
	private static final ImageIcon VISIBLE_ICON;
	private static final ImageIcon INVISIBLE_ICON;
	static
	{
		BufferedImage pencil = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "settings_icon.png");
		PENCIL_ICON = new ImageIcon(pencil); PAINT_ICON = new ImageIcon(pencil);
		BufferedImage arrow = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "arrow_right.png");
		RIGHT_ICON = new ImageIcon(arrow); DOWN_ICON = new ImageIcon(ImageUtil.rotateImage(arrow, Math.PI / 2));
		VISIBLE_ICON = new ImageIcon(ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "visible_icon.png"));
		INVISIBLE_ICON = new ImageIcon(ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "invisible_icon.png"));
	}
	private final RegionObjectMarkersPlugin plugin;
	private final RegionMarkerSection section;
	private final JPanel content = new JPanel();
	private final JPanel objectRows = new JPanel();
	private final JButton expand = new JButton();
	private final FlatTextField sectionName = new FlatTextField();

	RegionObjectMarkerSectionPanel(RegionObjectMarkersPlugin plugin, RegionMarkerSection section, Filter filter)
	{
		this.plugin = plugin; this.section = section;
		setLayout(new BorderLayout()); setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(header(), BorderLayout.NORTH);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); content.setBackground(ColorScheme.DARKER_GRAY_COLOR); content.setBorder(BorderFactory.createEmptyBorder(6, 7, 7, 7));
		content.add(groupLabel("REGION SELECTION")); content.add(Box.createRigidArea(new Dimension(0, 4))); content.add(regionRow());
		content.add(Box.createRigidArea(new Dimension(0, 2))); JSeparator divider = new JSeparator(); divider.setForeground(ColorScheme.DARK_GRAY_COLOR); divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); content.add(divider); content.add(Box.createRigidArea(new Dimension(0, 4))); content.add(groupLabel("OBJECT MARKERS"));
		objectRows.setLayout(new BoxLayout(objectRows, BoxLayout.Y_AXIS)); objectRows.setBackground(ColorScheme.DARKER_GRAY_COLOR); objectRows.setAlignmentX(LEFT_ALIGNMENT);
		for (ObjectMarker object : section.getObjects()) if (filter == Filter.ALL || filter == Filter.REGION || (filter == Filter.VISIBLE && object.isVisible()) || (filter == Filter.INVISIBLE && !object.isVisible())) { objectRows.add(Box.createRigidArea(new Dimension(0, 5))); objectRows.add(new ObjectRow(object)); }
		content.add(objectRows); content.add(Box.createRigidArea(new Dimension(0, 5))); JButton add = new JButton("+ Add object"); add.setToolTipText("Add an object-name highlight to this section"); add.addActionListener(e -> { section.getObjects().add(plugin.newObjectMarker()); plugin.saveSections(); plugin.rebuild(); }); content.add(add); add(content, BorderLayout.CENTER);
		setExpanded(!section.isCollapsed(), false);
	}
	private JPanel header()
	{
		JPanel header = new JPanel(new BorderLayout()); header.setBackground(ColorScheme.DARKER_GRAY_COLOR); header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR));
		expand.setPreferredSize(new Dimension(22, 24)); SwingUtil.removeButtonDecorations(expand); expand.addActionListener(e -> setExpanded(!content.isVisible(), true)); header.add(expand, BorderLayout.WEST);
		sectionName.setText(section.getName()); sectionName.setToolTipText("Object marker section name"); sectionName.setEditable(false); sectionName.setBackground(ColorScheme.DARKER_GRAY_COLOR); sectionName.getTextField().setForeground(Color.WHITE); sectionName.getTextField().setFont(FontManager.getRunescapeSmallFont()); header.add(sectionName, BorderLayout.CENTER);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel rename = new JLabel(PENCIL_ICON); rename.setToolTipText("Edit section name"); JLabel save = saveIcon("Save section name"); save.setVisible(false);
		rename.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { sectionName.setEditable(true); sectionName.getTextField().requestFocusInWindow(); sectionName.getTextField().selectAll(); rename.setVisible(false); save.setVisible(true); actions.revalidate(); } });
		save.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { String value = sectionName.getText().trim(); if (!value.isEmpty()) { section.setName(value); plugin.saveSections(); } sectionName.setEditable(false); save.setVisible(false); rename.setVisible(true); actions.revalidate(); } }); actions.add(rename); actions.add(save);
		actions.add(visibility(section::isVisible, section::setVisible, "region section"));
		JButton delete = new JButton("×"); delete.setForeground(Color.WHITE); delete.setToolTipText("Delete region section"); SwingUtil.removeButtonDecorations(delete); delete.addActionListener(e -> plugin.removeSection(section)); actions.add(delete); header.add(actions, BorderLayout.EAST); return header;
	}
	private JPanel regionRow()
	{
		JPanel rows = new JPanel(); rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS)); rows.setBackground(ColorScheme.DARKER_GRAY_COLOR); rows.setAlignmentX(LEFT_ALIGNMENT);
		JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); modeRow.setBackground(ColorScheme.DARKER_GRAY_COLOR); modeRow.setAlignmentX(LEFT_ALIGNMENT);
		modeRow.add(label("Scope"));
		JCheckBox allRegions = new JCheckBox("All regions", section.isAllRegions()); allRegions.setToolTipText("Match this section in every region; saved Region IDs are retained"); allRegions.setForeground(ColorScheme.LIGHT_GRAY_COLOR); allRegions.setBackground(ColorScheme.DARKER_GRAY_COLOR); allRegions.setFont(FontManager.getRunescapeSmallFont()); allRegions.setBorder(BorderFactory.createEmptyBorder()); allRegions.setFocusPainted(false);
		modeRow.add(allRegions); modeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, modeRow.getPreferredSize().height)); rows.add(modeRow);
		JPanel row = new JPanel(new BorderLayout(6, 0)); row.setBackground(ColorScheme.DARKER_GRAY_COLOR); row.setAlignmentX(LEFT_ALIGNMENT); JLabel label = label("Region IDs"); row.add(label, BorderLayout.WEST);
		FlatTextField regions = new FlatTextField(); regions.setText(section.isAllRegions() ? "" : formatRegionIds()); regions.setBackground(ColorScheme.DARK_GRAY_COLOR); regions.setToolTipText("Comma-separated region IDs, for example: 9265, 9266"); regions.setEditable(false); regions.setEnabled(!section.isAllRegions());
		JPanel regionActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); regionActions.setBackground(ColorScheme.DARKER_GRAY_COLOR); JLabel edit = new JLabel(PENCIL_ICON); edit.setToolTipText("Edit region IDs"); JLabel save = saveIcon("Save region IDs"); save.setVisible(false); regionActions.add(edit); regionActions.add(save);
		edit.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { if (!section.isAllRegions()) { regions.setEditable(true); regions.getTextField().requestFocusInWindow(); edit.setVisible(false); save.setVisible(true); regionActions.revalidate(); } } });
		save.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { if (saveRegionIds(regions)) { regions.setEditable(false); save.setVisible(false); edit.setVisible(true); regionActions.revalidate(); } } });
		allRegions.addActionListener(e -> { section.setAllRegions(allRegions.isSelected()); regions.setEnabled(!section.isAllRegions()); regions.setEditable(false); regions.setText(section.isAllRegions() ? "" : formatRegionIds()); save.setVisible(false); edit.setVisible(!section.isAllRegions()); plugin.saveSections(); });
		row.add(regions, BorderLayout.CENTER); row.add(regionActions, BorderLayout.EAST); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height)); rows.add(row); rows.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows.getPreferredSize().height)); return rows;
	}
	private String formatRegionIds()
	{
		StringBuilder result = new StringBuilder();
		for (Integer regionId : section.getRegionIds()) { if (result.length() > 0) result.append(", "); result.append(regionId); }
		return result.toString();
	}
	private boolean saveRegionIds(FlatTextField field)
	{
		if (section.isAllRegions()) return false;
		java.util.List<Integer> parsed = new java.util.ArrayList<>(); String value = field.getText().trim();
		try
		{
			if (!value.isEmpty()) for (String part : value.split(",")) { int id = Integer.parseInt(part.trim()); if (id < 0) throw new NumberFormatException(); if (!parsed.contains(id)) parsed.add(id); }
		}
		catch (NumberFormatException ex)
		{
			field.getTextField().setForeground(Color.RED); field.setToolTipText("Use comma-separated non-negative region IDs, for example: 9265, 9266"); return false;
		}
		field.getTextField().setForeground(Color.WHITE); section.setRegionIds(parsed); plugin.saveSections(); return true;
	}
	private void setExpanded(boolean expanded, boolean persist) { content.setVisible(expanded); expand.setIcon(expanded ? DOWN_ICON : RIGHT_ICON); expand.setToolTipText(expanded ? "Collapse section" : "Expand section"); if (persist) { section.setCollapsed(!expanded); plugin.saveSections(false); } revalidate(); }
	private JLabel visibility(java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set, String subject)
	{
		JLabel icon = new JLabel(get.getAsBoolean() ? VISIBLE_ICON : INVISIBLE_ICON); icon.setToolTipText(get.getAsBoolean() ? "Hide " + subject : "Show " + subject);
		icon.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { set.accept(!get.getAsBoolean()); plugin.saveSections(); plugin.rebuild(); } }); return icon;
	}
	private JLabel label(String text) { JLabel label = new JLabel(text); label.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker()); label.setFont(FontManager.getRunescapeSmallFont()); return label; }
	private JLabel groupLabel(String text) { JLabel label = label(text); label.setForeground(ColorScheme.LIGHT_GRAY_COLOR); label.setAlignmentX(LEFT_ALIGNMENT); return label; }
	private JLabel saveIcon(String tooltip) { JLabel icon = new JLabel("✓"); icon.setForeground(Color.GREEN); icon.setFont(FontManager.getRunescapeSmallFont()); icon.setToolTipText(tooltip); return icon; }

	private final class ObjectRow extends JPanel
	{
		private final ObjectMarker marker;
		ObjectRow(ObjectMarker marker)
		{
			this.marker = marker; setLayout(new BorderLayout(0, 4)); setBackground(ColorScheme.DARKER_GRAY_COLOR); setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR), BorderFactory.createEmptyBorder(5, 0, 5, 0)));
			FlatTextField name = new FlatTextField(); name.setText(marker.getName()); name.setToolTipText("Object name; * matches any characters"); name.setBackground(ColorScheme.DARK_GRAY_COLOR); name.getTextField().setForeground(Color.WHITE); name.setEditable(false);
			JPanel nameRow = new JPanel(new BorderLayout(6, 0)); nameRow.setBackground(ColorScheme.DARKER_GRAY_COLOR); nameRow.add(name, BorderLayout.CENTER); JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); actions.setBackground(ColorScheme.DARKER_GRAY_COLOR); JLabel edit = new JLabel(PENCIL_ICON); edit.setToolTipText("Edit object name"); JLabel save = saveIcon("Save object name"); save.setVisible(false); edit.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { name.setEditable(true); name.getTextField().requestFocusInWindow(); name.getTextField().selectAll(); edit.setVisible(false); save.setVisible(true); actions.revalidate(); } }); save.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { marker.setName(name.getText().trim()); plugin.saveSections(); name.setEditable(false); save.setVisible(false); edit.setVisible(true); actions.revalidate(); } }); actions.add(edit); actions.add(save); actions.add(visibility(marker::isVisible, marker::setVisible, "object marker")); JButton delete = new JButton("×"); delete.setForeground(Color.WHITE); delete.setToolTipText("Delete this object marker"); SwingUtil.removeButtonDecorations(delete); delete.addActionListener(e -> { section.getObjects().remove(marker); plugin.saveSections(); plugin.rebuild(); }); actions.add(delete); nameRow.add(actions, BorderLayout.EAST); add(nameRow, BorderLayout.NORTH);
			JPanel controls = new JPanel(); controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS)); controls.setBackground(ColorScheme.DARKER_GRAY_COLOR); JPanel styleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); styleRow.setBackground(ColorScheme.DARKER_GRAY_COLOR); styleRow.add(label("Style")); JComboBox<MarkerStyle> style = new JComboBox<>(MarkerStyle.values()); style.setToolTipText("Choose Hull, Clickbox, Tile, or Outline rendering"); style.setSelectedItem(marker.getStyle()); style.setPreferredSize(new Dimension(90, 22)); style.addActionListener(e -> { marker.setStyle((MarkerStyle) style.getSelectedItem()); plugin.saveSections(); }); styleRow.add(style); JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2)); colorRow.setBackground(ColorScheme.DARKER_GRAY_COLOR); colorRow.add(label("Fill")); colorRow.add(color(true)); colorRow.add(label("Outline")); colorRow.add(color(false)); controls.add(styleRow); controls.add(colorRow); add(controls, BorderLayout.CENTER);
		}
		private JLabel color(boolean fill)
		{
			JLabel button = new JLabel(PAINT_ICON); Color initial = fill ? marker.getFillColor() : marker.getOutlineColor(); button.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, previewColor(initial))); button.setToolTipText(fill ? "Edit fill color" : "Edit outline color"); button.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mousePressed(java.awt.event.MouseEvent e) { pick(fill, button); } }); return button;
		}
		private void pick(boolean fill, JLabel button)
		{
			Color current = fill ? marker.getFillColor() : marker.getOutlineColor(); RuneliteColorPicker picker = plugin.getColourPickerManager().create(SwingUtilities.windowForComponent(this), current, fill ? "Object fill color" : "Object outline color", false); picker.setOnColorChange(color -> { if (fill) marker.setFillColor(color); else marker.setOutlineColor(color); button.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, previewColor(color))); button.repaint(); plugin.saveSections(); }); picker.setVisible(true);
		}
		private Color previewColor(Color color) { return new Color(color.getRed(), color.getGreen(), color.getBlue()); }
	}
}
