package dev.neko.chatdemo.ui;

import dev.neko.chatdemo.R;

/** Demo session data shared by the sidebar panel and the day group page. */
public final class SidebarData {

    public static final int[] GROUP_TITLES = {
            R.string.group_today, R.string.group_yesterday, R.string.group_earlier
    };

    public static final String[][] SESSIONS = {
            {"Late night refactor", "Sheet motion study", "Palette pass"},
            {"Dock clipping", "Wavy progress", "Map caption"},
            {"Apr 12 · build log", "Mar 30 · insets", "Mar 21 · tabs"}
    };

    public static int groupCount() {
        return SESSIONS.length;
    }

    public static String[] sessions(int group) {
        return SESSIONS[group];
    }

    public static int titleRes(int group) {
        return GROUP_TITLES[group];
    }

    private SidebarData() {
    }
}
