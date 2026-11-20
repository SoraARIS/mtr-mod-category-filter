package com.soraaris.mtrcategoryfilter.mixin;

import java.util.Set;

public interface TagSearchItem {
	Set<String> mtrCategoryFilter$getTags();

	String mtrCategoryFilter$getPackName();
}
