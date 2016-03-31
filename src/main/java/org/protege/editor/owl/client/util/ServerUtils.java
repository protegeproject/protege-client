package org.protege.editor.owl.client.util;

import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerUtils {

    public static Registry getRmiRegistry(String serverLocation) throws RemoteException {
        URI serverLocationUri = URI.create(serverLocation);
        return getRmiRegistry(serverLocationUri);
    }

    public static Registry getRmiRegistry(URI serverLocation) throws RemoteException {
        String host = serverLocation.getHost();
        int port = serverLocation.getPort();
        return LocateRegistry.getRegistry(host, port);
    }

    public static Remote getRemoteService(String serverLocation, String serviceName) throws RemoteException {
        try {
            return getRmiRegistry(serverLocation).lookup(serviceName);
        }
        catch (NotBoundException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    public static Remote getRemoteService(URI serverLocation, String serviceName) throws RemoteException {
        try {
            return getRmiRegistry(serverLocation).lookup(serviceName);
        }
        catch (NotBoundException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }
}
