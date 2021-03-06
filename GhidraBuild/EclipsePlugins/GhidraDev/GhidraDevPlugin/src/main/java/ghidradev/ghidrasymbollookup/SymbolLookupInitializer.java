/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidradev.ghidrasymbollookup;

import java.io.IOException;
import java.net.ServerSocket;

import ghidradev.*;
import ghidradev.ghidrasymbollookup.preferences.GhidraSymbolLookupPreferences;

/**
 * High level driver for initializing the Ghidra Symbol Lookup subcomponent.  Should get called 
 * by the startup extension.  This subcomponent is responsible for receiving symbol lookup requests
 * from Ghidra to Eclipse over a socket.
 * 
 * @see GhidraDevStartup
 */
public class SymbolLookupInitializer {

	private static ServerSocket serverSocket;

	/**
	 * Initializes the Ghidra Symbol Lookup subcomponent.  Nothing in the package should be
	 * used until this initialization happens.  Should be called during Eclipse startup.
	 * 
	 * @param firstTimeConsent True if the user has just consented to opening ports; otherwise, 
	 *   false.
	 * @see GhidraDevStartup
	 */
	public static void init(boolean firstTimeConsent) {
		if (firstTimeConsent) {
			GhidraSymbolLookupPreferences.setSymbolLookupEnabled(true);
		}
		listen(GhidraSymbolLookupPreferences.getSymbolLookupPort());
	}

	/**
	 * Listens for socket connections on the given port.  If there is a problem listening,
	 * a popup is displayed for the user.
	 * 
	 * @param port The port to listen on.  If the port is -1, this method doesn't do anything.
	 */
	private static void listen(int port) {

		if (!GhidraSymbolLookupPreferences.isSymbolLookupEnabled()) {
			EclipseMessageUtils.info(
				Activator.PLUGIN_ID + " Symbol Lookup port listening is disabled in preferences.");
			return;
		}

		if (port == -1) {
			EclipseMessageUtils.info(Activator.PLUGIN_ID +
				" Symbol Lookup port listening is disabled, port not set in preferences.");
			return;
		}

		try {
			serverSocket = new ServerSocket(port);
			EclipseMessageUtils.info(
				Activator.PLUGIN_ID + " Symbol Lookup is listening on port " + port);
			Activator.getDefault().registerCloseable(serverSocket);
		}
		catch (IOException e) {
			EclipseMessageUtils.showErrorDialog(Activator.PLUGIN_ID + " Symbol Lookup",
				"Failed to listen for connections on port " + port +
					".  The Symbol Lookup features will be disabled until a valid port is selected in preferences.");
			return;
		}

		new Thread(new SocketSetupRunnable(serverSocket)).start();
	}

	/**
	 * Called when the symbol lookup preferences change.
	 * 
	 * @param enabledWasChanged True if the enablement was changed to a new value.
	 * @param portWasChanged True if the port preferences was changed to a new value.
	 */
	public static void preferencesChanged(boolean enabledWasChanged, boolean portWasChanged) {
		if (!enabledWasChanged && !portWasChanged) {
			return;
		}

		// Close the old server socket.  Its job is done.
		try {
			if (serverSocket != null) {
				serverSocket.close();
				Activator.getDefault().unregisterCloseable(serverSocket);
				serverSocket = null;
			}
		}
		catch (IOException e) {
			// Oh well, we tried.  This port probably won't work next time they pick it.
		}

		// Listen on the new port
		listen(GhidraSymbolLookupPreferences.getSymbolLookupPort());
	}
}
