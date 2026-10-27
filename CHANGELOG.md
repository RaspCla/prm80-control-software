# Changelog

## Not released yet

- Tooltips: "Shift Freq." 0 with "Shift Enabled" means the default shift of
  the configuration (channel manager column and cells, configuration dialog)
- Configuration dialog: "Shift" renamed to "Default shift"
- Windows installer: fixed upgrade code, a new version replaces the installed
  one (before, every version was a separate program and had to be uninstalled
  first). Versions up to 3.0.0 still have to be uninstalled once.

## 3.0.0 – 2026-10-27

Needs PRM80 firmware **V5.1** or newer
([RaspCla/prm80](https://github.com/RaspCla/prm80)).

- Firmware check when connecting: firmware older than V5.0 is refused with a
  message, with V5.0 the program warns that some functions do not work
  correctly (reverse, scan of the PRM80, power switch in VFO mode, new
  configuration only after a restart) and recommends V5.1
- About: version taken from pom.xml (was always "2.0"), F4FEZ as vendor,
  DC0CM as contributor (modernisation and new functions), homepage on GitHub
- Virtual PRM80 simulates firmware V5.1
- Channel manager: **Insert**, **Delete**, **Move up/down** (also with the keys
  Insert, Del, Alt+Up/Down), **Undo** (Ctrl+Z, up to 50 steps) and **Redo**
  (Ctrl+Y or Ctrl+Shift+Z)
- Channel manager: **Verify** compares the list shown with a channel file
  (channel by channel: frequency, shift, scan lock, shift +/-, reverse,
  enabled; comments separately). "Read from PRM80" first, then Verify,
  checks the radio against a file.

- Main window: in memory mode the shift fields only show the shift of the
  channel and are greyed (they only act in VFO mode).
- VFO mode: a channel key pressed at the radio (or a scan started there)
  brings the program back to memory mode with the channel of the radio.
  Before, the program stayed in VFO mode and "MEM" set the channel the
  program had used before instead of the one of the radio.
- Switching to VFO mode sets the current frequencies with "R" at once, so the
  radio hides its channel number at the same time as the program.

- Writing channels ("P" command): the channel state is sent as hex number as
  the firmware expects it. Before, states from 10 on were wrong, e.g. scan
  lock together with positive shift (12) arrived as 0x12.

## 2.1.0 – 2026-10

### Scan
- **Scan** button: scan of the memory channels (channels with scan lock are
  skipped without delay, the scan lock is taken from the channel list of the
  PRM80) or, in VFO mode, of a frequency range with a step
- Stops on a signal (squelch open, "HOLD" in the display) and continues after
  an adjustable hold time; transmitting stops the scan
- Tools > **Scan settings**: VFO range (stored for 144 and 430 MHz), step,
  hold time; time per channel = scan speed of the PRM80 (Configuration)
- The scan runs in the program and works with the existing firmware V5
  (the firmware has no serial command for its own scan)
- Virtual PRM80 simulates some stations for testing the scan

### Channel manager
- A value changed in one of several selected rows can be set for all
  selected rows (after a question), e.g. scan lock or shift for many channels
- Changing only a comment no longer marks the list as different from the PRM80

### Packages
- `package-windows.bat` / `package-linux.sh` use the `jpackage` of the PATH
  if `JAVA_HOME` points to an old Java without `jpackage` (e.g. Java 8)
- Windows installer: start menu folder "PRM80"
- The GitHub workflow also builds the Windows installer (`.msi`, WiX Toolset 3.14)
- **Program icon** (display of the PRM80 with "PRM80" in the LCD font) for the
  window, the Windows installer, start menu, desktop and the Linux package;
  `packaging/make-icon.py` draws it

### Display
- The comment of the current channel (from the channel manager list) is
  shown in red below the channel number (full text as tooltip); only if the
  channel in the list has the same frequency as in the PRM80

## 2.0.0 – 2026-10

Modernisation of the PRM80 control software: runs on Windows and Linux,
new build system, much easier to use.

### Build and platform
- Maven project (`pom.xml`) instead of the NetBeans/Ant project; the libraries
  are downloaded automatically, `lib/`, `build.xml` and `nbproject/` removed
- Java 21 or newer (tested with 21 and 25)
- Serial port with **jSerialComm** instead of RXTX (no native libraries to
  install, works on Windows, Linux, macOS, ARM)
- Plain Swing with **FlatLaf** instead of the obsolete Swing Application
  Framework, swing-layout and swingworker
- One jar with all libraries (maven-shade-plugin), no build warnings
- Stand-alone packages with own Java runtime: `package-windows.bat`
  (folder or `.msi`), `package-linux.sh` (folder, `.deb`, `.rpm`)
- GitHub workflow builds the packages and creates releases
- NetBeans Run/Debug without "restricted method" warnings (`nbactions.xml`)

### User interface
- Whole window zoomable (resize with the mouse, View menu, Ctrl + / − / 0),
  zoom factor remembered
- Communication runs in the background: the window stays responsive,
  "BUSY" in the display while the PRM80 is working, state read every 1.5 s
- Serial port selection and Connect/Disconnect button in the main window
- All texts in English (were partly French)
- Dialogs always in front of the main window
- Rx/Tx/Chnl labels on the baseline of the frequencies, more space between
  RX and TX
- Expert mode as checkbox in the Tools menu (Options dialog removed)

### Shift and VFO
- Shift fields (enabled, positive, reverse, frequency) show the shift of
  the current channel in memory mode
- In VFO mode the shift fields set the TX frequency at once, reverse swaps
  RX and TX (as the firmware does for channels)
- **Write to MEM**: store the VFO setting in a memory channel; dialog stays
  open, Undo, comment, uses the list of the channel manager
- Configuration dialog: PLL step, default shift, scan speed

### Channel manager
- Read from / Write to PRM80, Read / Save / Save As of CSV files (UTF-8),
  the last file is opened automatically
- Comments are kept (also when the channels are read from the PRM80)
- Columns for shift enabled, positive/negative, reverse, scan lock
- Limited to 100 channels (0–99)
- List shared with "Write to MEM" (asks before a different list is replaced)
- **Export A51**: channel list as firmware configuration
  (`src/inc_144.a51` / `inc_430.a51`) in the format of firmware V5 with
  `shift_list` and all state bits, values taken from the connected PRM80

### Communication
- Reliable serial communication (timeouts, resynchronisation after errors,
  escape handling), no more jumping display
- Channel state, default shift and scan speed read from firmware V5

### Virtual PRM80 (expert mode)
- Simulates a PRM8060 144 MHz with firmware V5.0 as it is meant to work:
  RAM layout with checksums, EEPROM, factory channels, shift per channel,
  reverse, scan lock, configuration

## 1.x – 2007 to 2021

- Original software by Florian MAZEN (F4FEZ), 2007–2008
- 2021, RaspCla: shift functions in VFO mode, reading and writing the
  channel list, channel import/export, squelch fix
