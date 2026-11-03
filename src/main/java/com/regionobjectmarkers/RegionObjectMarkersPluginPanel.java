package com.regionobjectmarkers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;

class RegionObjectMarkersPluginPanel extends PluginPanel
{
	private static final ImageIcon COPY_ICON;
	private static final ImageIcon COPY_HOVER_ICON;
	private static final ImageIcon PASTE_ICON;
	private static final ImageIcon PASTE_HOVER_ICON;
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;
	private static final ImageIcon HOVER_INFO_ICON;
	private static final ImageIcon HOVER_INFO_ACTIVE_ICON;

	private static final ImageIcon[] FILTER_ICONS;
	private static final String[] FILTER_TEXT = {"ALL", "R", "", ""};
	private static final String[] FILTER_DESCRIPTIONS;

	private final JLabel copyMarkers = new JLabel(COPY_ICON);
	private final JLabel pasteMarkers = new JLabel(PASTE_ICON);
	private final JLabel markerAdd = new JLabel(ADD_ICON);
	private final JLabel debugUnderMouse = new JLabel(HOVER_INFO_ICON);
	private final JLabel filterButton = new JLabel(FILTER_ICONS[0]);
	private final IconTextField searchBar = new IconTextField();
	private final PluginErrorPanel noMarkersPanel = new PluginErrorPanel();
	private final JPanel markerView = new JPanel();
	private final JPanel searchPanel = new JPanel(new BorderLayout());

	private final Client client;
	private final RegionObjectMarkersPlugin plugin;
	private final ClientThread clientThread;

	@Getter
	private Filter filter = Filter.ALL;

	static
	{
		final BufferedImage copyIcon = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "copy_icon.png");
		COPY_ICON = new ImageIcon(copyIcon);
		COPY_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(copyIcon, 0.53f));

		final BufferedImage pasteIcon = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "paste_icon.png");
		PASTE_ICON = new ImageIcon(pasteIcon);
		PASTE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(pasteIcon, 0.53f));
		final BufferedImage addIcon = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "add_icon.png");
		ADD_ICON = new ImageIcon(addIcon);
		ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));

		final BufferedImage visibleImg = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "visible_icon.png");
		final BufferedImage invisibleImg = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "invisible_icon.png");
		final BufferedImage regionIcon = ImageUtil.loadImageResource(RegionObjectMarkersPlugin.class, "region_icon.png");
		HOVER_INFO_ICON = new ImageIcon(ImageUtil.alphaOffset(regionIcon, 0.20f));
		HOVER_INFO_ACTIVE_ICON = new ImageIcon(regionIcon);

		FILTER_ICONS = new ImageIcon[]
		{
			new ImageIcon(ImageUtil.alphaOffset(visibleImg, 0.0f)),
			new ImageIcon(regionIcon),
			new ImageIcon(visibleImg),
			new ImageIcon(invisibleImg)
		};

		FILTER_DESCRIPTIONS = new String[]
		{
			"<html>Filter:<br>Listing all markers</html>",
			"<html>Filter:<br>Listing only markers in the current region</html>",
			"<html>Filter:<br>Listing only visible markers</html>",
			"<html>Filter:<br>Listing only hidden markers</html>"
		};
	}

	public RegionObjectMarkersPluginPanel(Client client, RegionObjectMarkersPlugin plugin, ClientThread clientThread)
	{
		this.client = client;
		this.plugin = plugin;
		this.clientThread = clientThread;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(new EmptyBorder(1, 0, 10, 0));

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(1, 3, 10, 7));

		JPanel markerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
		markerButtons.setPreferredSize(new Dimension(100, 24));

		searchPanel.setBorder(new EmptyBorder(1, 0, 0, 0));

		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 43 - filterButton.getWidth(), 24));
		searchBar.addActionListener(e -> rebuild());
		searchBar.addClearListener(this::rebuild);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent event) { rebuild(); }
			@Override public void removeUpdate(DocumentEvent event) { rebuild(); }
			@Override public void changedUpdate(DocumentEvent event) { rebuild(); }
		});
		searchBar.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					searchBar.setText("");
					rebuild();
				}
			}
		});

		filterButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		filterButton.setOpaque(true);
		filterButton.setPreferredSize(new Dimension(28, 24));
		filterButton.setText(FILTER_TEXT[0]);
		filterButton.setToolTipText(FILTER_DESCRIPTIONS[0]);
		filterButton.setHorizontalTextPosition(JLabel.CENTER);
		filterButton.setFont(FontManager.getRunescapeSmallFont());
		filterButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				filter = filter.next();
				filterButton.setText(FILTER_TEXT[filter.ordinal()]);
				filterButton.setIcon(FILTER_ICONS[filter.ordinal()]);
				filterButton.setToolTipText(FILTER_DESCRIPTIONS[filter.ordinal()]);
				rebuild();
			}

			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				filterButton.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				filterButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});

		JLabel title = new JLabel();
		title.setText("Object Markers");
		title.setForeground(Color.WHITE);

		titlePanel.add(title, BorderLayout.WEST);
		titlePanel.add(markerButtons, BorderLayout.EAST);

		searchPanel.add(searchBar, BorderLayout.WEST);
		searchPanel.add(filterButton, BorderLayout.EAST);

		northPanel.add(titlePanel, BorderLayout.NORTH);
		northPanel.add(searchPanel, BorderLayout.CENTER);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		markerView.setLayout(new BoxLayout(markerView, BoxLayout.Y_AXIS));
		markerView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		noMarkersPanel.setVisible(false);

		markerView.add(noMarkersPanel);

		copyMarkers.setToolTipText("Export sections matching the current search and filter to your clipboard");
		copyMarkers.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				copyMarkers();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				copyMarkers.setIcon(COPY_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				copyMarkers.setIcon(COPY_ICON);
			}
		});

		pasteMarkers.setToolTipText("Import region object marker sections from your clipboard");
		pasteMarkers.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				pasteMarkers();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				pasteMarkers.setIcon(PASTE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				pasteMarkers.setIcon(PASTE_ICON);
			}
		});

		markerAdd.setToolTipText("Add a new object marker section");
		markerAdd.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				plugin.addMarkerSection();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				markerAdd.setIcon(ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				markerAdd.setIcon(ADD_ICON);
			}
		});

		debugUnderMouse.setToolTipText("Show information about the hovered tile and objects");
		debugUnderMouse.setIcon(plugin.isDebugUnderMouse() ? HOVER_INFO_ACTIVE_ICON : HOVER_INFO_ICON);
		debugUnderMouse.addMouseListener(new MouseAdapter()
		{
			@Override public void mousePressed(MouseEvent e)
			{
				plugin.toggleDebugUnderMouse();
				debugUnderMouse.setIcon(plugin.isDebugUnderMouse() ? HOVER_INFO_ACTIVE_ICON : HOVER_INFO_ICON);
			}
		});

		markerButtons.add(pasteMarkers);
		markerButtons.add(copyMarkers);
		markerButtons.add(debugUnderMouse);
		markerButtons.add(markerAdd);

		centerPanel.add(markerView, BorderLayout.NORTH);

		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
	}

	public void rebuild()
	{
		clientThread.invokeLater(() -> {
			markerView.removeAll();

			for (final RegionMarkerSection section : plugin.getSections())
			{
				if (section.getName().toLowerCase().contains(getSearchText().toLowerCase()) && plugin.matchesFilter(section, filter))
				{
					markerView.add(new RegionObjectMarkerSectionPanel(plugin, section, filter));
					markerView.add(Box.createRigidArea(new Dimension(0, 10)));
				}
			}

			boolean empty = markerView.getComponentCount() == 0;
			noMarkersPanel.setContent("Region Object Markers", "Click the green '+' button to add a region section.");
			noMarkersPanel.setVisible(empty);
			searchPanel.setVisible(!empty);
			if (empty && plugin.getSections().size() > 0)
			{
				noMarkersPanel.setContent("Region Object Markers",
					"No object marker sections are available for the current search term and/or selected filter.");
				searchPanel.setVisible(true);
			}

			markerView.add(noMarkersPanel);

			repaint();
			revalidate();
		});
	}

	public String getSearchText()
	{
		return searchBar.getText();
	}

	private void copyMarkers()
	{
		clientThread.invokeLater(() -> {
			String content = plugin.copySections(getSearchText(), filter);
			if (content != null) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
		});
	}

	private void pasteMarkers()
	{
		final String clipboardText;
		try
		{
			clipboardText = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString();
		}
		catch (IOException | UnsupportedFlavorException ignore)
		{
			return;
		}
		if (plugin.pasteSections(clipboardText))
		{
			noMarkersPanel.setVisible(false);
			searchPanel.setVisible(true);
			rebuild();
		}
	}
}
