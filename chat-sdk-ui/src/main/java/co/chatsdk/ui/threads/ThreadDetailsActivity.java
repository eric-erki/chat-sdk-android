/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.ui.threads;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;

import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.interfaces.ThreadType;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.core.utils.Strings;
import co.chatsdk.ui.R;
import co.chatsdk.ui.chat.ChatActivity;
import co.chatsdk.ui.contacts.ContactsFragment;
import co.chatsdk.ui.helpers.ProfilePictureChooserOnClickListener;
import co.chatsdk.ui.main.BaseActivity;

/**
 * Created by braunster on 24/11/14.
 */
public class ThreadDetailsActivity extends BaseActivity {

    /** Set true if you want slide down animation for this context exit. */
    protected boolean animateExit = false;

    protected ActionBar actionBar;
    protected MenuItem settingsItem;
    protected SimpleDraweeView threadImageView;

    protected Thread thread;
    protected ContactsFragment contactsFragment;
    protected boolean startingSettingsActivity;

    protected final DisposableList disposableList = new DisposableList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            getDataFromBundle(savedInstanceState);
        }
        else {
            if (getIntent().getExtras() != null) {
                getDataFromBundle(getIntent().getExtras());
            }
            else {
                finish();
            }
        }

        setContentView(activityLayout());

        initViews();
    }

    protected @LayoutRes int activityLayout() {
        return R.layout.chat_sdk_activity_thread_details;
    }

    protected void initViews() {
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(Strings.nameForThread(thread));
            actionBar.setHomeButtonEnabled(true);
        }

        threadImageView = findViewById(R.id.chat_sdk_thread_image_view);
    }

    protected void updateMetaData() {
        actionBar.setTitle(Strings.nameForThread(thread));
    }

    protected void loadData () {
        ThreadImageBuilder.load(threadImageView, thread);

        // CoreThread users bundle
        contactsFragment = new ContactsFragment();
        contactsFragment.setInflateMenu(false);
        contactsFragment.setLoadingMode(ContactsFragment.MODE_LOAD_THREAD_USERS);
        contactsFragment.setExtraData(thread.getEntityID());
        contactsFragment.setClickMode(ContactsFragment.CLICK_MODE_SHOW_PROFILE);

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_thread_users, contactsFragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startingSettingsActivity = false;

        loadData();
        updateMetaData();

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.ThreadMetaUpdated))
                .subscribe(networkEvent -> updateMetaData()));

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.threadUsersUpdated())
                .subscribe(networkEvent -> loadData()));

        // Only if the current user is the admin of this thread.
        if (StringUtils.isNotBlank(thread.getCreatorEntityId()) && thread.getCreatorEntityId().equals(ChatSDK.currentUserID())) {
            //threadImageView.setOnClickListener(ChatSDKIntentClickListener.getPickImageClickListener(this, THREAD_PIC));
            threadImageView.setOnClickListener(new ProfilePictureChooserOnClickListener(this));
        }


    }

    @Override
    protected void onPause() {
        disposableList.dispose();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        setResult(AppCompatActivity.RESULT_OK);

        finish(); // Finish needs to be called before animate exit
        if (animateExit) {
            overridePendingTransition(R.anim.dummy, R.anim.slide_top_bottom_out);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // TODO: Enable thread images
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getDataFromBundle(intent.getExtras());
    }

    protected void getDataFromBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        animateExit = bundle.getBoolean(ChatActivity.ANIMATE_EXIT, animateExit);

        String threadEntityID = bundle.getString(Keys.THREAD_ENTITY_ID);

        if (threadEntityID != null && !threadEntityID.isEmpty()) {
            thread = ChatSDK.db().fetchThreadWithEntityID(threadEntityID);
        }
        else {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Keys.THREAD_ENTITY_ID, thread.getEntityID());
        outState.putBoolean(ChatActivity.ANIMATE_EXIT, animateExit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (thread.getCreatorEntityId().equals(ChatSDK.currentUserID())) {
            settingsItem = menu.add(Menu.NONE, R.id.action_chat_sdk_settings, 12, getString(R.string.action_settings));
            settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            settingsItem.setIcon(R.drawable.icn_24_settings);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        if (item.getItemId() == R.id.action_chat_sdk_settings) {
            if (startingSettingsActivity) return true;
            startingSettingsActivity = true;
            if (thread.typeIs(ThreadType.Public)) {
                ChatSDK.ui().startPublicThreadEditDetailsActivity(ChatSDK.shared().context(), thread.getEntityID());
            } else {
                ChatSDK.ui().startPrivateThreadEditDetailsActivity(ChatSDK.shared().context(), thread.getEntityID());
            }
        }
        return true;
    }

}
