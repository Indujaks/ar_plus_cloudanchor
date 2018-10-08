/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.ar.core.examples.java.helloar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/** Helper class for Firebase storage of cloud anchor IDs. */
public class StorageManager {

  /** Listener for a new Cloud Anchor ID from the Firebase Database. */
  interface CloudAnchorIdListener {
    void onCloudAnchorIdAvailable(String cloudAnchorId);
  }

  /** Listener for a new short code from the Firebase Database. */
  interface ShortCodeListener {
    void onShortCodeAvailable(Integer shortCode);
  }

    /** Listener for getting all short codes from firebase DB */
    interface GetShortCodesListener {
        void onAllShortCodesAvailable(ArrayList<String> anchorcodes);
    }
  private static final String TAG = StorageManager.class.getName();
  private static final String KEY_ROOT_DIR = "shared_anchor_codelab_root_helloAR_app";
  private static final String KEY_NEXT_SHORT_CODE = "next_short_code";
  private static final String KEY_PREFIX = "anchor;";
  private static final int INITIAL_SHORT_CODE = 350;
  private final DatabaseReference rootRef;

  /** Constructor that initializes the Firebase connection. */
  StorageManager(Context context) {
    FirebaseApp firebaseApp = FirebaseApp.initializeApp(context);
    rootRef = FirebaseDatabase.getInstance(firebaseApp).getReference().child(KEY_ROOT_DIR);
    DatabaseReference.goOnline();
  }

  /** Gets a new short code that can be used to store the anchor ID. */
  void nextShortCode(ShortCodeListener listener) {
    // Run a transaction on the node containing the next short code available. This increments the
    // value in the database and retrieves it in one atomic all-or-nothing operation.
    rootRef
        .child(KEY_NEXT_SHORT_CODE)
        .runTransaction(
            new Transaction.Handler() {
              @Override
              public Transaction.Result doTransaction(MutableData currentData) {
                Integer shortCode = currentData.getValue(Integer.class);
                if (shortCode == null) {
                  // Set the initial short code if one did not exist before.
                  shortCode = INITIAL_SHORT_CODE - 1;
                }
                currentData.setValue(shortCode + 1);
                return Transaction.success(currentData);
              }

              @Override
              public void onComplete(
                  DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (!committed) {
                  Log.e(TAG, "Firebase Error", error.toException());
                  listener.onShortCodeAvailable(null);
                } else {
                  listener.onShortCodeAvailable(currentData.getValue(Integer.class));
                }
              }
            });
  }

  /** Stores the cloud anchor ID in the configured Firebase Database. */
  void storeUsingShortCode(int shortCode, String cloudAnchorId) {
    rootRef.child(KEY_PREFIX + shortCode).setValue(cloudAnchorId);
  }
    /**
     * Retrieves the cloud anchor ID using a short code. Returns an empty string if a cloud anchor ID
     * was not stored for this short code.
     */
    String getCloudAnchorID(Activity activity, int shortCode) {
        SharedPreferences sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPrefs.getString(KEY_PREFIX + shortCode, "");
    }
    String getshortcodes(){

        return null;
    }
  /**
   * Retrieves the cloud anchor ID using a short code. Returns an empty string if a cloud anchor ID
   * was not stored for this short code.
   */
  void getCloudAnchorId(int shortCode, CloudAnchorIdListener listener) {
    rootRef
        .child(KEY_PREFIX + shortCode)
        .addListenerForSingleValueEvent(
            new ValueEventListener() {
              @Override
              public void onDataChange(DataSnapshot dataSnapshot) {
                // Listener invoked when the data is successfully read from Firebase.
                listener.onCloudAnchorIdAvailable(String.valueOf(dataSnapshot.getValue()));
              }

              @Override
              public void onCancelled(DatabaseError error) {
                Log.e(
                    TAG,
                    "The Firebase operation for getCloudAnchorId was cancelled.",
                    error.toException());
                listener.onCloudAnchorIdAvailable(null);
              }
            });
  }
  void removedata(){

      rootRef.removeValue();
  }

    /**
     * Retrieves all the stored short codes. Return null if failed or cancelled.
     **/
    ArrayList<String> getAllShortCodes(GetShortCodesListener listener){

        Log.e("indu:","Inside getAllSHorCO");
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference ref = database.child(KEY_ROOT_DIR);
        final ArrayList<String> shortcode = new ArrayList<String>();
        Query itemsQuery = ref.orderByChild("type");
        itemsQuery.addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(DataSnapshot dataSnapshot) {
           Log.e("indu:","ondatachange");
           if (dataSnapshot.exists()) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                   if (singleSnapshot.getValue().toString().length() > 6){
                       shortcode.add(singleSnapshot.getKey().substring(7));
                     Log.e("indu:","add item"+singleSnapshot.getKey().substring(7));
                     }

                 }
                listener.onAllShortCodesAvailable(shortcode);
                }
            }
            @Override
             public void onCancelled(DatabaseError databaseError) {

            }

        });
        return shortcode;
    }
/*vaibhav*/
        /*rootRef
                .child(KEY_ROOT_DIR)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                            ArrayList<String> shortcode = new ArrayList<>();//[(int)dataSnapshot.getChildrenCount()-1];
                            for (DataSnapshot d: dataSnapshot.getChildren()
                                    ) {
                                Log.e("APEKS:","SC="+d.getKey());
                                if (d.getValue().toString().length() > 6)
                                    shortcode.add(d.getKey());
                                Log.e(TAG,"indu shortcode"+d.getKey());
                            }
                            listener.onAllShortCodesAvailable(shortcode.toString());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(
                                TAG,
                                "APEKS:The Firebase operation for getCloudAnchorId was cancelled.",
                                databaseError.toException());
                        listener.onAllShortCodesAvailable(null);
                    }
                });*/
    //}
}
