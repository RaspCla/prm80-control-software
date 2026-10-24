/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package f4fez.prm80x0.gui;

import f4fez.prm80x0.Controler.Controler;
import java.util.function.IntFunction;
import javax.swing.Timer;

/**
 * Scan done by the program (the PRM80 firmware has no serial command to
 * start its own scan).
 * <p>
 * Memory scan: channel after channel ("N" command), channels with "scan lock"
 * are skipped. VFO scan: frequency range with a step ("R" command).
 * <p>
 * On every channel / frequency the scanner waits the scan speed of the PRM80
 * (Configuration, time per channel), then reads the state ("E" command).
 * Squelch open: the scan stops there while the signal is received and the
 * hold time after it. Transmitting stops the scan.
 * <p>
 * All methods are called in the GUI thread; the communication runs in the
 * background worker, between the steps the worker is free for commands of
 * the user.
 */
public class Scanner {

    /** Callbacks to the main window (GUI thread). */
    public interface Listener {
        /** A new channel / frequency was set or the state was read. */
        void scanUpdated();
        /** Scanning started, stopped or started / stopped holding on a signal. */
        void scanStateChanged();
        /** Scanning stopped by itself (signal while transmitting, error ...). */
        void scanStopped(String reason);
    }

    /** Poll interval while a signal is received */
    private static final int HOLD_POLL = 300;

    private final PrmWorker worker;
    private final Listener listener;
    private final Timer timer;

    private Controler controler;
    private boolean running;
    private boolean holding;
    private boolean vfo;
    /** increased on every start / stop: answers of an old scan are ignored */
    private int generation;
    private Runnable timerAction;

    private int dwell;              // ms per channel
    private int hold;               // ms after the signal
    private long holdUntil;         // end of the hold time, 0 = signal still there

    // memory scan
    private int channel;
    private int lockedInRow;

    // VFO scan
    private int from;
    private int to;
    private int step;
    private int frequency;
    private IntFunction<int[]> vfoFrequencies;

    public Scanner(PrmWorker worker, Listener listener) {
        this.worker = worker;
        this.listener = listener;
        this.timer = new Timer(100, e -> {
            Runnable action = this.timerAction;
            this.timerAction = null;
            if (action != null)
                action.run();
        });
        this.timer.setRepeats(false);
    }

    public boolean isRunning() {
        return this.running;
    }

    /** @return true while the scan stays on a signal */
    public boolean isHolding() {
        return this.running && this.holding;
    }

    public boolean isVfoScan() {
        return this.vfo;
    }

    public int getFrom() {
        return this.from;
    }

    public int getTo() {
        return this.to;
    }

    public int getStep() {
        return this.step;
    }

    /**
     * Start the memory scan with the channel after the current one.
     * @param c the PRM80
     * @param holdMs time to stay after a signal has ended
     */
    public void startMemory(Controler c, int holdMs) {
        this.stop();
        this.vfo = false;
        this.begin(c, holdMs);
        this.worker.background(() -> {
            int[] r = {dwellOf(c), c.getCurrentChannel(), c.getMaxChan()};
            return r;
        }, r -> {
            if (!this.isCurrent(c))
                return;
            this.dwell = r[0];
            this.channel = r[1];
            this.lockedInRow = 0;
            this.nextChannel();
        }, this.failed());
    }

    /**
     * Start the VFO scan.
     * @param c the PRM80
     * @param fromHz first frequency (base frequency, as shown in VFO mode)
     * @param toHz last frequency
     * @param stepHz step
     * @param startHz current frequency: the scan continues after it if it is in the range
     * @param frequencies base frequency -> {RX, TX} (shift fields of the main window)
     * @param holdMs time to stay after a signal has ended
     */
    public void startVfo(Controler c, int fromHz, int toHz, int stepHz, int startHz,
            IntFunction<int[]> frequencies, int holdMs) {
        this.stop();
        this.vfo = true;
        this.from = Math.min(fromHz, toHz);
        this.to = Math.max(fromHz, toHz);
        this.step = stepHz;
        this.vfoFrequencies = frequencies;
        boolean inRange = startHz >= this.from && startHz <= this.to
                && (startHz - this.from) % this.step == 0;
        this.frequency = inRange ? startHz : this.from - this.step;
        this.begin(c, holdMs);
        this.worker.background(() -> dwellOf(c), d -> {
            if (!this.isCurrent(c))
                return;
            this.dwell = d;
            this.nextFrequency();
        }, this.failed());
    }

    private void begin(Controler c, int holdMs) {
        this.controler = c;
        this.hold = Math.max(0, holdMs);
        this.running = true;
        this.holding = false;
        this.generation++;
        this.listener.scanStateChanged();
    }

    /** Stop scanning (the PRM80 stays on the current channel / frequency). */
    public void stop() {
        if (!this.running)
            return;
        this.running = false;
        this.holding = false;
        this.generation++;
        this.timer.stop();
        this.timerAction = null;
        this.listener.scanStateChanged();
    }

    private void stopWith(String reason) {
        this.stop();
        this.listener.scanStopped(reason);
    }

    private static int dwellOf(Controler c) throws Exception {
        int ms = c.supportsScanSpeed() ? c.getScanSpeed() : 400;
        return Math.max(50, Math.min(ms, 12750));
    }

    /** @return true if the answer belongs to the running scan */
    private boolean isCurrent(Controler c) {
        return this.running && this.controler == c;
    }

    private void later(int ms, Runnable action) {
        final int gen = this.generation;
        this.timerAction = () -> {
            if (this.running && gen == this.generation)
                action.run();
        };
        this.timer.setInitialDelay(Math.max(1, ms));
        this.timer.restart();
    }

    // ---- steps ---------------------------------------------------------------

    private void nextChannel() {
        final Controler c = this.controler;
        final int gen = this.generation;
        this.worker.background(() -> {
            int max = c.getMaxChan();
            int next = this.channel + 1 > max ? 0 : this.channel + 1;
            c.setCurrentChannel(next);
            return new int[] {next, c.getChannelState(), max};
        }, r -> {
            if (gen != this.generation)
                return;
            this.channel = r[0];
            this.listener.scanUpdated();
            if (r[1] >= 0 && (r[1] & 8) != 0) {
                // scan lock: next channel at once
                this.lockedInRow++;
                if (this.lockedInRow > r[2] + 1)
                    this.stopWith("All channels are locked for scanning (scan lock)");
                else
                    this.nextChannel();
                return;
            }
            this.lockedInRow = 0;
            this.later(this.dwell, this::check);
        }, this.failed());
    }

    private void nextFrequency() {
        final Controler c = this.controler;
        final int gen = this.generation;
        int next = this.frequency + this.step;
        if (next > this.to || next < this.from)
            next = this.from;
        final int base = next;
        final int[] f = this.vfoFrequencies.apply(base);
        this.worker.background(() -> {
            c.setFrequencies(f[0], f[1]);
            return null;
        }, r -> {
            if (gen != this.generation)
                return;
            this.frequency = base;
            this.listener.scanUpdated();
            this.later(this.dwell, this::check);
        }, this.failed());
    }

    /** Read the state: signal or not? */
    private void check() {
        final Controler c = this.controler;
        final int gen = this.generation;
        this.worker.background(() -> {
            c.refreshState();
            return new boolean[] {c.isSquelchOpen(), c.isTransmitting(), c.isStateValid()};
        }, r -> {
            if (gen != this.generation)
                return;
            this.listener.scanUpdated();
            if (!r[2]) {
                this.stopWith("No answer from the PRM80 - scan stopped");
                return;
            }
            if (r[1]) {
                this.stopWith("Transmitting - scan stopped");
                return;
            }
            if (r[0]) {
                // signal: stay here
                this.holdUntil = 0;
                this.setHolding(true);
                this.later(HOLD_POLL, this::check);
            } else if (this.holding) {
                long now = System.currentTimeMillis();
                if (this.holdUntil == 0)
                    this.holdUntil = now + this.hold;
                if (now >= this.holdUntil) {
                    this.setHolding(false);
                    this.next();
                } else {
                    this.later((int) Math.min(HOLD_POLL, this.holdUntil - now), this::check);
                }
            } else {
                this.next();
            }
        }, this.failed());
    }

    private void next() {
        if (this.vfo)
            this.nextFrequency();
        else
            this.nextChannel();
    }

    private void setHolding(boolean holding) {
        if (this.holding != holding) {
            this.holding = holding;
            this.listener.scanStateChanged();
        }
    }

    /** Error handler for the running scan (errors of an older scan are ignored). */
    private java.util.function.Consumer<Exception> failed() {
        final int gen = this.generation;
        return ex -> {
            if (!this.running || gen != this.generation)
                return;
            String msg = ex.getMessage();
            this.stopWith("Scan stopped: " + (msg == null || msg.isEmpty() ? "no valid answer from the PRM80" : msg));
        };
    }
}
