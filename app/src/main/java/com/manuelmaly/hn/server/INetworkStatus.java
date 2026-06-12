package com.manuelmaly.hn.server;

/**
 * Abstraction over device connectivity, replacing the static
 * {@link ConnectivityUtils#isDeviceOnline}. Lets the network layer be tested
 * without an Android {@code Context}.
 */
public interface INetworkStatus {

    boolean isOnline();

}
