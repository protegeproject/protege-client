package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * @author Timothy Redmond <tredmond@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public abstract class AbstractClientAction extends ProtegeOWLAction {

    private static final long serialVersionUID = 8677318010907902600L;

    private static ClientSession clientSession;

    private static ScheduledExecutorService executorService = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r, "Client-Server Communications");
                    th.setDaemon(true);
                    return th;
                }
            });

    @Override
    public void initialise() throws Exception {
        if (clientSession == null) {
            clientSession = ClientSession.getInstance(getOWLEditorKit());
        }
    }

    @Override
    public void dispose() throws Exception {
        if (clientSession != null) {
            clientSession.dispose();
        }
    }

    protected ClientSession getClientSession() {
        return clientSession;
    }

    protected Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    protected ScheduledFuture<?> submit(Runnable task, long delay) {
        return executorService.schedule(task, delay, TimeUnit.SECONDS);
    }

    protected ScheduledFuture<?> submitPeriodic(Runnable task, long period) {
        return executorService.scheduleAtFixedRate(task, period, period, TimeUnit.SECONDS);
    }
    protected Future<?> submit(Callable<?> task) {
        return executorService.submit(task);
    }

    protected ScheduledFuture<?> submit(Callable<?> task, long delay) {
        return executorService.schedule(task, delay, TimeUnit.SECONDS);
    }

    protected void showErrorDialog(String title, String message, Throwable t) {
        JOptionPaneEx.showConfirmDialog(getOWLEditorKit().getWorkspace(), title, new JLabel(message),
                JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
    }

    protected void showWarningDialog(String title, String message) {
        JOptionPaneEx.showConfirmDialog(getOWLEditorKit().getWorkspace(), title, new JLabel(message),
                JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
    }

    protected void showInfoDialog(String title, String message) {
        JOptionPaneEx.showConfirmDialog(getOWLEditorKit().getWorkspace(), title, new JLabel(message),
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
    }
}
