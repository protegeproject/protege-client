package org.protege.editor.owl.client.util;

import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;

public class ServerUtils {

    public static Registry getRmiRegistry(String serverLocation, Optional<Integer> registryPort) throws RemoteException {
        URI serverLocationUri = URI.create(serverLocation);
        return getRmiRegistry(serverLocationUri, registryPort);
    }

    public static Registry getRmiRegistry(URI serverLocation, Optional<Integer> registryPort) throws RemoteException {
        String host = serverLocation.getHost();
        /*
         * Use the same port number as the server port if no registry port was specified
         */
        int port = (registryPort.isPresent()) ? registryPort.get() : serverLocation.getPort();
        return LocateRegistry.getRegistry(host, port);
    }

    public static Remote getRemoteService(String serverLocation, int registryPort, String serviceName) throws RemoteException {
        try {
            return getRmiRegistry(serverLocation, Optional.of(registryPort)).lookup(serviceName);
        }
        catch (NotBoundException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    public static Remote getRemoteService(String serverLocation, String serviceName) throws RemoteException {
        try {
            return getRmiRegistry(serverLocation, Optional.empty()).lookup(serviceName);
        }
        catch (NotBoundException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }
}
