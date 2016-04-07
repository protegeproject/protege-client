package org.protege.editor.owl.client.action;

import org.protege.editor.owl.client.ClientRegistry;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;
import org.protege.owl.server.changes.api.VersionedOntologyDocument;

import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class AbstractClientAction extends ProtegeOWLAction {

    private static final long serialVersionUID = 8677318010907902600L;

    private ClientRegistry clientRegistry;

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
        clientRegistry = ClientRegistry.getInstance(getOWLEditorKit());
    }

    @Override
    public void dispose() throws Exception {
        clientRegistry.dispose();
    }

    protected ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    protected Client getClient() {
        return clientRegistry.getActiveClient();
    }

    protected Optional<VersionedOntologyDocument> getOntologyResource() {
        OWLOntology ontology = getOWLEditorKit().getModelManager().getActiveOntology();
        return Optional.ofNullable(clientRegistry.getVersionedOntology(ontology));
    }
    
    protected Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    protected ScheduledFuture<?> submit(Runnable task, long delay) {
        return executorService.scheduleWithFixedDelay(task, delay, delay, TimeUnit.SECONDS);
    }
}
