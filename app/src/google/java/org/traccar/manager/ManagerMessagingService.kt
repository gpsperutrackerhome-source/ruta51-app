/*
 * Copyright 2018 - 2021 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.manager

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ManagerMessagingService : FirebaseMessagingService() {

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val intent = Intent(this, MainActivity::class.java)
        remoteMessage.data["eventId"]?.let { intent.putExtra("eventId", it) }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        
        // El cuerpo del mensaje viene de la data si el servidor lo envía como comando
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Alerta de RUTA 51"

        val builder = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Asegura que aparezca arriba
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Activa sonido y vibración

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(remoteMessage.hashCode(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // REFUERZO: Guardamos el token en las preferencias compartidas para que no se pierda
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.edit().putString(MainFragment.KEY_TOKEN, token).apply()

        // Notificamos a la App que hay un nuevo token disponible
        val intent = Intent(MainFragment.EVENT_TOKEN)
        intent.putExtra(MainFragment.KEY_TOKEN, token)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        // Mantenemos la función original si existe GoogleMainApplication
        (application as? GoogleMainApplication)?.broadcastToken(token)
    }
}