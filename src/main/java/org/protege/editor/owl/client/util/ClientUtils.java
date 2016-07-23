package org.protege.editor.owl.client.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.SessionRecorder;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.model.ChangeListMinimizer;
import org.protege.editor.owl.model.history.HistoryManager;
import org.protege.editor.owl.server.versioning.ChangeHistoryUtils;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.RevisionMetadata;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ClientUtils {

    /**
     * Perform logout from the Protege client-server application.
     *
     * @param clientSession
     *          The existing client session
     * @param client
     *          The client to log out
     */
    public static void performLogout(ClientSession clientSession, Client client) throws Exception
    {
        if (client instanceof ClientSessionListener) {
            clientSession.removeListener((ClientSessionListener) client);
        }
        clientSession.clear();
    }

   

    public static List<OWLOntologyChange> getUncommittedChanges(HistoryManager man, OWLOntology ontology, ChangeHistory baseline) {
    	
    	return ((SessionRecorder) man).getUncommittedChanges();    	
    	
    }

    /**
     * Create a commit object by specifying the <code>author</code>, <code>comment</code> string and
     * the list of <code>changes</code>.
     *
     * @param author
     *          The committer
     * @param comment
     *          The commit comment
     * @param changes
     *          The list of changes inside a commit
     * @return A commit object
     */
    public static Commit createCommit(Client author, String comment, List<OWLOntologyChange> changes) {
        RevisionMetadata metadata = new RevisionMetadata(
                author.getUserInfo().getId(),
                author.getUserInfo().getName(),
                author.getUserInfo().getEmailAddress(), comment);
        return new Commit(metadata, changes);
    }

    
    
   
    /*
     * Private utility methods
     */

    public static void updateOntology(OWLOntology placeholder, ChangeHistory changeHistory, OWLOntologyManager manager) {
    	
    	System.out.println("Loaded ontology, now updating from server");

        List<OWLOntologyChange> changes = ChangeHistoryUtils.getOntologyChanges(changeHistory, placeholder);
        
        manager.applyChanges(changes);
        fixMissingImports(placeholder, changes, manager);
    }

    private static void fixMissingImports(OWLOntology ontology, List<OWLOntologyChange> changes, OWLOntologyManager manager) {
        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
        configuration = configuration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        configuration = configuration.setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH);
        
        final Set<OWLImportsDeclaration> declaredImports = ontology.getImportsDeclarations();
        Set<OWLImportsDeclaration> missingImports = new TreeSet<OWLImportsDeclaration>();
        for (OWLOntologyChange change : changes) {
            if (change instanceof AddImport) {
                OWLImportsDeclaration importDecl = ((AddImport) change).getImportDeclaration();
                if (declaredImports.contains(importDecl) && manager.getImportedOntology(importDecl) == null) {
                    missingImports.add(importDecl);
                }
            }
        }
        for (OWLImportsDeclaration importDecl : missingImports) {
            manager.makeLoadImportRequest(importDecl, configuration);
        }
    }
}
