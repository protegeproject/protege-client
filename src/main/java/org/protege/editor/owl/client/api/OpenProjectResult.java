package org.protege.editor.owl.client.api;

import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.util.Optional;

public class OpenProjectResult {
	public ServerDocument serverDocument;
	public Optional<String> snapshotChecksum;

	public OpenProjectResult(ServerDocument serverDocument, String snapshotChecksum) {
		this.serverDocument = serverDocument;
		this.snapshotChecksum = Optional.ofNullable(snapshotChecksum);
	}
}