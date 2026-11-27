#!/usr/bin/env python3
"""Drive the demo app on an emulator and capture a screenshot tour."""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = sys.argv[1] if len(sys.argv) > 1 else "shots"
os.makedirs(OUT, exist_ok=True)


def run(args, **kwargs):
    return subprocess.run(args, check=True, text=True, **kwargs)


def adb(*args, **kwargs):
    return run(["adb", *args], **kwargs)


def wait(seconds=1.2):
    time.sleep(seconds)


def shot(name):
    path = os.path.join(OUT, name)
    with open(path, "wb") as f:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=f, check=True)
    print("captured", path)


def dump():
    for attempt in range(3):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
            xml = subprocess.check_output(["adb", "shell", "cat", "/sdcard/ui.xml"], text=True)
            start = xml.find("<?xml")
            if start > 0:
                xml = xml[start:]
            return ET.fromstring(xml)
        except Exception as exc:  # noqa: BLE001
            print("dump failed, retrying:", exc)
            time.sleep(1.0)
    raise RuntimeError("uiautomator dump failed")


def find(root, text=None, desc=None, cls=None):
    for node in root.iter("node"):
        if text is not None and node.get("text") != text:
            continue
        if desc is not None and node.get("content-desc") != desc:
            continue
        if cls is not None and cls not in (node.get("class") or ""):
            continue
        return node
    return None


def center(node):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    if not m:
        raise RuntimeError("bad bounds: " + node.get("bounds", ""))
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_node(node, label):
    x, y = center(node)
    print("tap", label, "at", x, y)
    adb("shell", "input", "tap", str(x), str(y))
    wait()


def tap_text(text, fallback=None):
    node = find(dump(), text=text)
    if node is None:
        if fallback:
            print("text not found, tapping fallback", fallback)
            adb("shell", "input", "tap", str(fallback[0]), str(fallback[1]))
            wait()
            return
        raise RuntimeError("text not found: " + text)
    tap_node(node, text)


def tap_desc(desc):
    node = find(dump(), desc=desc)
    if node is None:
        raise RuntimeError("content-desc not found: " + desc)
    tap_node(node, desc)


TAB_XY = {}


def cache_tabs():
    root = dump()
    for name in ("Chat", "Map", "Mission", "Usage"):
        node = find(root, text=name)
        if node is not None:
            TAB_XY[name] = center(node)
    print("tabs:", TAB_XY)


def tab(name):
    if name in TAB_XY:
        x, y = TAB_XY[name]
    else:
        x, y = {"Chat": (135, 360), "Map": (405, 360),
                "Mission": (675, 360), "Usage": (945, 360)}[name]
    adb("shell", "input", "tap", str(x), str(y))
    wait(1.6)


def back():
    adb("shell", "input", "keyevent", "4")
    wait()


print("launch")
adb("shell", "am", "start", "-W", "-n", "dev.neko.chatdemo/.MainActivity")
wait(5)
print(adb("shell", "wm", "size").stdout.strip())
print(adb("shell", "wm", "density").stdout.strip())
print(adb("shell", "logcat", "-d", "-s", "NekoInsets:I").stdout)
cache_tabs()
shot("01-chat.png")

# Tabs: use the tab bounds captured before the map starts animating.
tab("Map")
wait(2)
shot("02-map.png")
tab("Mission")
wait(1.6)
shot("03-mission.png")
tab("Usage")
wait(1.6)
shot("04-usage.png")
tab("Chat")
wait(1.5)

# Attachments sheet.
tap_desc("Add attachment")
shot("05-attachments.png")
tap_text("Drafts")
shot("06-drafts.png")
back()
back()
wait(1.2)

# Model picker, opened from the split button chevron.
tap_desc("Model and reasoning effort")
wait(1.5)
shot("07-models.png")
tap_text("Anthropic")
shot("08-models-anthropic.png")
tap_text("Claude Opus 4.5")
wait(1.5)
shot("09-reasoning.png")
back()
wait(1.2)

# Sidebar.
tap_desc("Expand sidebar")
wait(1.5)
shot("10-sidebar.png")
back()
wait(1.2)

# Composer + fullscreen input.
node = find(dump(), cls="android.widget.EditText")
if node is None:
    raise RuntimeError("input field not found")
tap_node(node, "input field")
adb("shell", "input", "text", "Keep%sthe%sdock%swarm.")
wait(1.2)
adb("shell", "input", "keyevent", "4")
wait(1.2)
shot("11-composer-filled.png")
tap_desc("Full screen input")
wait(1.5)
shot("12-fullscreen.png")
back()
wait(1.2)

# Dark mode.
adb("shell", "cmd", "uimode", "night", "yes")
wait(3.5)
shot("13-dark-chat.png")
tab("Mission")
wait(1.6)
shot("14-dark-mission.png")
tab("Chat")
wait(1.0)

print("done")
