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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Runs the communication with the PRM80 in a background thread, one command
 * after the other. The user interface stays responsive while the PRM80
 * answers (4800 baud, one answer can take some 100 ms).
 * <p>
 * The callbacks (done / error / busy listener) are called in the GUI thread.
 */
public class PrmWorker {

    /** A piece of work talking to the PRM80. */
    public interface Task<T> {
        T run() throws Exception;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PRM80 communication");
        t.setDaemon(true);
        return t;
    });

    /** number of commands from the user that are waiting or running */
    private final AtomicInteger pending = new AtomicInteger();
    /** number of background polls waiting or running */
    private final AtomicInteger polls = new AtomicInteger();
    private final Runnable busyListener;

    /**
     * @param busyListener called in the GUI thread when {@link #isBusy()} may have changed
     */
    public PrmWorker(Runnable busyListener) {
        this.busyListener = busyListener;
    }

    /** @return true while a command of the user is waiting or running */
    public boolean isBusy() {
        return this.pending.get() > 0;
    }

    /** @return true if nothing at all is waiting or running */
    public boolean isIdle() {
        return this.pending.get() == 0 && this.polls.get() == 0;
    }

    /**
     * Run a command of the user. The busy indicator is shown until it is done.
     * @param task the work, runs in the background
     * @param done called with the result in the GUI thread, may be null
     * @param error called with the exception in the GUI thread, may be null
     */
    public <T> void run(Task<T> task, Consumer<T> done, Consumer<Exception> error) {
        this.pending.incrementAndGet();
        this.fireBusy();
        this.executor.execute(() -> {
            try {
                T result = task.run();
                if (done != null)
                    SwingUtilities.invokeLater(() -> done.accept(result));
            } catch (Exception ex) {
                Logger.getLogger(PrmWorker.class.getName()).log(Level.WARNING, "PRM80 command failed", ex);
                if (error != null)
                    SwingUtilities.invokeLater(() -> error.accept(ex));
            } finally {
                this.pending.decrementAndGet();
                this.fireBusy();
            }
        });
    }

    /** Run a command of the user without result. */
    public void run(Task<?> task, Consumer<Exception> error) {
        this.run(task, null, error);
    }

    /**
     * Run a background poll (no busy indicator). Skipped if anything else
     * is waiting, so polls never delay the commands of the user.
     */
    public void poll(Task<?> task, Consumer<Exception> error) {
        if (!this.isIdle())
            return;
        this.polls.incrementAndGet();
        this.executor.execute(() -> {
            try {
                if (this.pending.get() == 0)
                    task.run();
            } catch (Exception ex) {
                if (error != null)
                    SwingUtilities.invokeLater(() -> error.accept(ex));
            } finally {
                this.polls.decrementAndGet();
            }
        });
    }

    /**
     * Run work of the program itself (e.g. the scanner) without busy
     * indicator. Unlike {@link #poll} it is never skipped; regular polls are
     * skipped while it is waiting or running.
     * @param task the work, runs in the background
     * @param done called with the result in the GUI thread, may be null
     * @param error called with the exception in the GUI thread, may be null
     */
    public <T> void background(Task<T> task, Consumer<T> done, Consumer<Exception> error) {
        this.polls.incrementAndGet();
        this.executor.execute(() -> {
            try {
                T result = task.run();
                if (done != null)
                    SwingUtilities.invokeLater(() -> done.accept(result));
            } catch (Exception ex) {
                Logger.getLogger(PrmWorker.class.getName()).log(Level.WARNING, "PRM80 background work failed", ex);
                if (error != null)
                    SwingUtilities.invokeLater(() -> error.accept(ex));
            } finally {
                this.polls.decrementAndGet();
            }
        });
    }

    private void fireBusy() {
        if (this.busyListener != null)
            SwingUtilities.invokeLater(this.busyListener);
    }
}
