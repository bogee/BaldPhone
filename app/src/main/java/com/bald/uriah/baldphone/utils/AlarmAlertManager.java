/*
 * Copyright 2019 Uriah Shaul Mandel
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

package com.bald.uriah.baldphone.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.alarms.AlarmScreenActivity;
import com.bald.uriah.baldphone.activities.pills.PillScreenActivity;
import com.bald.uriah.baldphone.databases.alarms.Alarm;
import com.bald.uriah.baldphone.databases.reminders.Reminder;

/** Creates the user-visible, lock-screen alerts required by modern Android. */
public final class AlarmAlertManager {
    private static final String TAG = AlarmAlertManager.class.getSimpleName();
    private static final String ALARM_CHANNEL_ID = "baldphone_alarms_v2";
    private static final String REMINDER_CHANNEL_ID = "baldphone_reminders_v2";
    private static final int ALARM_NOTIFICATION_BASE = 0x0A100000;
    private static final int REMINDER_NOTIFICATION_BASE = 0x0B100000;

    private AlarmAlertManager() {
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return true;
        final AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    public static boolean canUseFullScreenIntents(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            return true;
        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager != null && notificationManager.canUseFullScreenIntent();
    }

    public static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;

        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;

        final AudioAttributes alarmAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        final NotificationChannel alarmChannel = new NotificationChannel(
                ALARM_CHANNEL_ID,
                context.getString(R.string.alarm_notifications),
                NotificationManager.IMPORTANCE_HIGH);
        alarmChannel.setDescription(context.getString(R.string.alarm_notifications_subtext));
        alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        alarmChannel.enableVibration(true);
        alarmChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAttributes);

        final AudioAttributes reminderAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        final NotificationChannel reminderChannel = new NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(R.string.pill_reminder_notifications),
                NotificationManager.IMPORTANCE_HIGH);
        reminderChannel.setDescription(context.getString(R.string.pill_reminder_notifications_subtext));
        reminderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        reminderChannel.enableVibration(true);
        reminderChannel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                reminderAttributes);

        notificationManager.createNotificationChannel(alarmChannel);
        notificationManager.createNotificationChannel(reminderChannel);
    }

    public static void showAlarm(Context context, Alarm alarm) {
        final String name = alarm.getName();
        final CharSequence title = TextUtils.isEmpty(name)
                ? context.getText(R.string.alarm)
                : name;
        showAlert(
                context,
                ALARM_CHANNEL_ID,
                alarmNotificationId(alarm.getKey()),
                title,
                context.getText(R.string.tap_to_open),
                new Intent(context, AlarmScreenActivity.class)
                        .putExtra(Alarm.ALARM_KEY_VIA_INTENTS, alarm.getKey()),
                RingtoneManager.TYPE_ALARM);
    }

    public static void showReminder(Context context, Reminder reminder) {
        final String content = reminder.getTextualContent();
        showAlert(
                context,
                REMINDER_CHANNEL_ID,
                reminderNotificationId(reminder.getId()),
                context.getText(R.string.pills),
                TextUtils.isEmpty(content) ? context.getText(R.string.tap_to_open) : content,
                new Intent(context, PillScreenActivity.class)
                        .putExtra(Reminder.REMINDER_KEY_VIA_INTENTS, reminder.getId()),
                RingtoneManager.TYPE_NOTIFICATION);
    }

    public static void cancelAlarm(Context context, int key) {
        NotificationManagerCompat.from(context).cancel(alarmNotificationId(key));
    }

    public static void cancelReminder(Context context, int id) {
        NotificationManagerCompat.from(context).cancel(reminderNotificationId(id));
    }

    @SuppressLint("MissingPermission")
    private static void showAlert(
            Context context,
            String channelId,
            int notificationId,
            CharSequence title,
            CharSequence text,
            Intent screenIntent,
            int legacySoundType) {
        if (!hasNotificationPermission(context)) {
            Log.e(TAG, "Cannot show an alarm alert without notification permission");
            return;
        }

        createNotificationChannels(context);
        screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                notificationId,
                screenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(fullScreenIntent)
                .setFullScreenIntent(fullScreenIntent, true)
                .setOngoing(true)
                .setAutoCancel(false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            builder.setSound(RingtoneManager.getDefaultUri(legacySoundType));

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private static int alarmNotificationId(int key) {
        return ALARM_NOTIFICATION_BASE + (key & 0xFFFFF);
    }

    private static int reminderNotificationId(int id) {
        return REMINDER_NOTIFICATION_BASE + (id & 0xFFFFF);
    }
}
