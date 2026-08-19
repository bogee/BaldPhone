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

package com.bald.uriah.baldphone.screenshots;

import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.contacts.ContactsActivity;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ContactsActivityScreenshot extends BaseScreenshotTakerTest<ContactsActivity> {

    public void test() {
        mActivityTestRule.launchActivity(new Intent());
        getInstrumentation().waitForIdleSync();
        new Handler(mActivityTestRule.getActivity().getMainLooper())
                .post(() -> {
                    final ContactsActivity dis = mActivityTestRule.getActivity();
                    final String[] names = dis.getResources().getStringArray(R.array.names_for_screenshots);
                    final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                    for (final String name : names) {
                        final int rawContactInsertIndex = operations.size();
                        operations.add(ContentProviderOperation
                                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                .build());
                        operations.add(ContentProviderOperation
                                .newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(
                                        ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                                .build());
                    }
                    try {
                        dis.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
                    } catch (RemoteException | OperationApplicationException e) {
                        throw new AssertionError("Could not create screenshot contacts", e);
                    }
                    dis.applyFilter();

                });
        getInstrumentation().waitForIdleSync();

    }

    @Override
    protected Class<ContactsActivity> activity() {
        return ContactsActivity.class;
    }

}
