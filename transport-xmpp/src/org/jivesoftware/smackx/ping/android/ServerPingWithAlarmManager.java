/**
 *
 * Copyright 2014 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.ping.android;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.ping.PingManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

public class ServerPingWithAlarmManager extends Manager {

	private static final Logger LOGGER = Logger.getLogger(ServerPingWithAlarmManager.class
			.getName());

	private static final String PING_ALARM_ACTION = "org.igniterealtime.smackx.ping.ACTION";

	private static final Map<XMPPConnection, ServerPingWithAlarmManager> INSTANCES = Collections
			.synchronizedMap(new WeakHashMap<XMPPConnection, ServerPingWithAlarmManager>());

	public static synchronized ServerPingWithAlarmManager getInstanceFor(XMPPConnection connection) {
		ServerPingWithAlarmManager serverPingWithAlarmManager = INSTANCES.get(connection);
		if (serverPingWithAlarmManager == null) {
			serverPingWithAlarmManager = new ServerPingWithAlarmManager(connection);
			INSTANCES.put(connection, serverPingWithAlarmManager);
		}
		return serverPingWithAlarmManager;
	}

	private static boolean mEnabled = true;

	private ServerPingWithAlarmManager(XMPPConnection connection) {
		super(connection);
	}

	public void setEnabled(boolean enabled) {
		mEnabled = enabled;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	private static BroadcastReceiver sAlarmBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			LOGGER.fine("Ping Alarm broadcast received");
			Iterator<XMPPConnection> it = INSTANCES.keySet().iterator();
			while (it.hasNext()) {
				XMPPConnection connection = it.next();
				if (ServerPingWithAlarmManager.getInstanceFor(connection).isEnabled()) {
					LOGGER.fine("Calling pingServerIfNecessary for connection "
							+ connection.getConnectionCounter());
					PingManager.getInstanceFor(connection).pingServerIfNecessary();
				} else {
					LOGGER.fine("NOT calling pingServerIfNecessary (disabled) on connection "
							+ connection.getConnectionCounter());
				}
			}
		}
	};

	private static Context sContext;
	private static PendingIntent sPendingIntent;
	private static AlarmManager sAlarmManager;

	public static void onCreate(Context context) {
		sContext = context;
		context.registerReceiver(sAlarmBroadcastReceiver, new IntentFilter(PING_ALARM_ACTION));
		sAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		sPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(PING_ALARM_ACTION), 0);
		sAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HALF_HOUR,
				AlarmManager.INTERVAL_HALF_HOUR, sPendingIntent);
	}

	public static void onDestory() {
		sContext.unregisterReceiver(sAlarmBroadcastReceiver);
		sAlarmManager.cancel(sPendingIntent);
	}
}
