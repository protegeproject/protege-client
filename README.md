Protege Ontology Client
======================

Provides client functionality for the Protege Desktop application to connect to an [OWL Ontology Server][], allowing end users to collaboratively edit collections of OWL ontologies.

  [OWL Ontology Server]: http://github.com/protegeproject/org.protege.owl.server
  
Compatible with Protege Desktop versions 5.0 and above.

This branch differs from metaproject-integration in several ways:

 * Uses HTTP instead of RMI. The client APIs are almost identical, but
   the operational semantics are somewhat different. The metaproject
   config file is fetched when a connection is made. Operations that
   modify the metaproject file are performed on the client. The
   metaproject is then pushed back to the server

 * Metaproject GUI - minor tweaks - Added save/cancel buttons to the
   JSON serialization page that allows the metaproject to be pushed
   back to the server. Currently the server restarts on this
   save. Also modified how autoupdate and update occur in terms of
   processing changes from the server.

 * Uses the `HistoryManager` built into Protege as the basis for
   uncommitted changes. This requires a [protege branch][1], which adds
   some workarounds to the `HistoryManager`.

 * When a new project is created a snapshot of the ontology is made
   and pushed to the server. When a project is opened the first time
   the snapshot is fetched and the history changes are applied on
   top. This gives a significant speedup over the prior approach of
   creating an initial large commit of the entire ontology as a list
   of changes.

Some suggested next TODOs:

 * More testing

 * Run another listener on an admin port, to be used insode the
   firewall to remotely administer the web server (sleep, restart,
   update the config, check logs..)

 * Check all prompt diff functionality

 * Add endpoints that record complex operation history

 * HTTPs support - this is there but commented out, need to obtain
   certs and test

 * Provide locking mechanism for the metaproject, only allow one
   client to edit/update the metaproject at any one time

 * Add ability to periodically create new snapshots, perhaps from the
   prompt diff panel. Each snapshot would include the previous one
   plus all the current history. This is similar to how commits are
   squashed in Git.





----
[1]: https://github.com/bdionne/protege/commits/5.0.1-history-search
 
