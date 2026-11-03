package com.regionobjectmarkers;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
class RegionMarkerSection
{
	private String name;
	/** Legacy single-value field retained only so existing saved sections migrate cleanly. */
	private Integer regionId;
	private List<Integer> regionIds = new ArrayList<>();
	private boolean allRegions;
	private boolean visible = true;
	private boolean collapsed;
	private List<ObjectMarker> objects = new ArrayList<>();

	RegionMarkerSection(String name)
	{
		this.name = name;
	}

	void normalizeRegionIds()
	{
		if (regionIds == null)
		{
			regionIds = new ArrayList<>();
		}
		if (regionIds.isEmpty() && regionId != null)
		{
			regionIds.add(regionId);
		}
		regionId = null;
	}
}
