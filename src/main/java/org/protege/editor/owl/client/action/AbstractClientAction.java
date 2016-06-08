package org.protege.editor.owl.client.action;

import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.SynchronizationException;
import org.protege.editor.owl.server.versioning.api.VersionedOWLOntology;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public abstract class AbstractClientAction extends ProtegeOWLAction {

    private static final long serialVersionUID = 8677318010907902600L;

    private ClientSession clientSession;

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
        clientSession = ClientSession.getInstance(getOWLEditorKit());
    }

    @Override
    public void dispose() throws Exception {
        clientSession.dispose();
    }

    protected ClientSession getClientSession() {
        return clientSession;
    }

    protected Client getClient() {
        return clientSession.getActiveClient();
    }

    protected Optional<VersionedOWLOntology> getOntologyResource() {
        return Optional.ofNullable(clientSession.getActiveVersionOntology());
    }

    protected VersionedOWLOntology getActiveVersionOntology() throws SynchronizationException {
        if (getOntologyResource().isPresent()) {
            return getOntologyResource().get();
        }
        throw new SynchronizationException("The current active ontology does not link to the server");
    }

    protected Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    protected ScheduledFuture<?> submit(Runnable task, long delay) {
        return executorService.schedule(task, delay, TimeUnit.SECONDS);
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
