package com.qwert2603.vkmutualgroups.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.qwert2603.vkmutualgroups.Listener;
import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.AbstractVkListFragment;
import com.qwert2603.vkmutualgroups.fragments.ConfirmationDialogFragment;
import com.qwert2603.vkmutualgroups.fragments.SendMessageDialogFragment;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Activity, отображающая фрагмент-список (друзей или групп).
 * Это самая базовая Activity.
 * Она позволяет отправлять сообщения, удалять из друзей и выходить из групп, вступать в группы.
 * Также она предоставлят доступ к элементам UI: TextView-ошибка, RefreshLayout, ActionButton.
 */
public abstract class AbstractVkListActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    public static final String TAG = "AbstractVkListActivity";

    private static final int REQUEST_SEND_MESSAGE = 1;
    private static final int REQUEST_DELETE_FRIEND = 2;
    private static final int REQUEST_LEAVE_GROUP = 3;
    private static final int REQUEST_JOIN_GROUP = 4;

    private static final String friendToDeleteId = "friendToDeleteId";
    private static final String groupToLeaveId = "groupToLeaveId";
    private static final String groupToJoin = "groupToJoin";

    private DataManager mDataManager;

    private Bundle mArgs;

    private SwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vk_list);

        mDataManager = DataManager.get(this);
        mArgs = new Bundle();

        if (getSupportActionBar() != null && NavUtils.getParentActivityName(this) != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setListFragment(null);

        mRefreshLayout = getRefreshLayout();
    }

    protected void setListFragment(AbstractVkListFragment fragment) {
        Fragment newFragment = fragment;
        if (newFragment == null) {
            // Чтобы mRefreshLayout нормально отображался.
            // Просто пустой фрагмент для фона.
            ListFragment fakeListFragment = new ListFragment();
            fakeListFragment.setListAdapter(new ArrayAdapter<>(this, 0, new String[]{}));
            newFragment = fakeListFragment;
        }
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, newFragment).commitAllowingStateLoss();
    }

    protected Fragment getListFragment() {
        return getFragmentManager().findFragmentById(R.id.fragment_container);
    }

    /**
     * Уведомляет о том, что операция (удаления из друзей или выхода из группы) успешно завершилась.
     */
    @CallSuper
    protected void notifyOperationCompleted() {
        Fragment fragment = getListFragment();
        if (fragment instanceof AbstractVkListFragment) {
            ((AbstractVkListFragment) fragment).notifyDataSetChanged();
        }
    }

    protected TextView getErrorTextView() {
        return (TextView) findViewById(R.id.error_text_view);
    }

    protected CoordinatorLayout getCoordinatorLayout() {
        return (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    protected SwipeRefreshLayout getRefreshLayout() {
        return (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
    }

    protected FloatingActionButton getActionButton() {
        return (FloatingActionButton) findViewById(R.id.action_button);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (NavUtils.getParentActivityName(this) != null) {
            getMenuInflater().inflate(R.menu.navigable_activity, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_to_the_begin:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sendMessage(int friendId) {
        SendMessageDialogFragment sendMessageDialogFragment = SendMessageDialogFragment.newInstance(friendId);
        sendMessageDialogFragment.setTargetFragment(mTargetFragment, REQUEST_SEND_MESSAGE);
        sendMessageDialogFragment.show(getFragmentManager(), SendMessageDialogFragment.TAG);
    }

    public void deleteFriend(VKApiUserFull friend) {
        mArgs.putInt(friendToDeleteId, friend.id);

        String title = getString(R.string.friend_name, friend.first_name, friend.last_name);
        String question = getString(R.string.delete_friend) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_DELETE_FRIEND);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public void leaveGroup(VKApiCommunityFull group) {
        mArgs.putInt(groupToLeaveId, group.id);

        String title = group.name;
        String question = getString(R.string.leave_group) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_LEAVE_GROUP);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    public void joinGroup(VKApiCommunityFull group) {
        mArgs.putParcelable(groupToJoin, group);

        String title = group.name;
        String question = getString(R.string.join_group) + "?";
        ConfirmationDialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(title, question);
        dialogFragment.setTargetFragment(mTargetFragment, REQUEST_JOIN_GROUP);
        dialogFragment.show(getFragmentManager(), ConfirmationDialogFragment.TAG);
    }

    private Fragment mTargetFragment = new Fragment() {
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SEND_MESSAGE:
                        showSnackbar(R.string.message_sent);
                        break;
                    case REQUEST_DELETE_FRIEND:
                        mDataManager.deleteFriend(mArgs.getInt(friendToDeleteId), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                showSnackbar(R.string.friend_deleted_successfully);
                                notifyOperationCompleted();
                            }

                            @Override
                            public void onError(String e) {
                                showSnackbar(R.string.friend_deleting_error);
                            }
                        });
                        break;
                    case REQUEST_LEAVE_GROUP:
                        mDataManager.leaveGroup(mArgs.getInt(groupToLeaveId), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                showSnackbar(R.string.group_left_successfully);
                                notifyOperationCompleted();
                            }

                            @Override
                            public void onError(String e) {
                                showSnackbar(R.string.group_leaving_error);
                            }
                        });
                        break;
                    case REQUEST_JOIN_GROUP:
                        mRefreshLayout.post(() -> mRefreshLayout.setRefreshing(true));
                        mDataManager.joinGroup(mArgs.getParcelable(groupToJoin), new Listener<Void>() {
                            @Override
                            public void onCompleted(Void aVoid) {
                                showSnackbar(R.string.group_join_successfully);
                                notifyOperationCompleted();
                                mRefreshLayout.setRefreshing(false);
                            }

                            @Override
                            public void onError(String e) {
                                showSnackbar(R.string.group_joining_error);
                                mRefreshLayout.setRefreshing(false);
                            }
                        });
                        break;
                }
            }
        }
    };

    private void showSnackbar(int stringRes) {
        Snackbar.make(getCoordinatorLayout(), stringRes, Snackbar.LENGTH_SHORT).show();
    }

}
