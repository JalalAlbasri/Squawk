package com.jalbasri.squawk;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.gms.maps.MapFragment;

/**
 * Created by jalal on 16/09/13.
 */
public class ActionBarManager {
    private static final String TAG = ActionBarManager.class.getSimpleName();

    private MainActivity mActivity;
    private ActionBar mActionBar;
    private TabListener<StatusListFragment> mListTabListener;
    private TabListener<StatusMapFragment> mMapTabListener;

    public ActionBarManager(MainActivity activity) {
        mActivity = activity;
        mActionBar = mActivity.getActionBar();
    }

    public void initActionBar() {

        mActionBar.setDisplayUseLogoEnabled(false);
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        //TODO: Enable up navigation on icon in actionbar
        //mActionBar.setDisplayHomeAsUpEnabled(true);

        View fragmentContainer = mActivity.findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;

        if (!tabletLayout) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            ActionBar.Tab listTab = mActionBar.newTab();
            mListTabListener = new TabListener<StatusListFragment>
                    (mActivity, StatusListFragment.class, R.id.fragment_container);
            listTab
//                    .setText("List")
                    .setIcon(R.drawable.collections_view_as_list)
                    .setContentDescription("List of Status Updates")
                    .setTabListener(mListTabListener);

            mActionBar.addTab(listTab);

            ActionBar.Tab mapTab = mActionBar.newTab();
            mMapTabListener = new TabListener<StatusMapFragment>
                    (mActivity, StatusMapFragment.class, R.id.fragment_container);
            mapTab
//                    .setText("Map")
                    .setIcon(R.drawable.location_map)
                    .setContentDescription("Map of Status Updates")
                    .setTabListener(mMapTabListener);

            mActionBar.addTab(mapTab);
        }
    }

    public void saveTabState(){
        View fragmentContainer = mActivity.findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {
            int actionBarIndex = mActionBar.getSelectedTab().getPosition();
            SharedPreferences.Editor editor = mActivity.getPreferences(Activity.MODE_PRIVATE).edit();
            editor.putInt(mActivity.KEY_ACTION_BAR_INDEX, actionBarIndex);
            editor.commit();
            FragmentTransaction transaction = mActivity.getFragmentManager().beginTransaction();
            if (mListTabListener.fragment != null) {
                transaction.detach(mListTabListener.fragment);
            }
            if (mMapTabListener.fragment != null) {
                transaction.detach(mMapTabListener.fragment);
            }
            transaction.commit();
        }
    }

    public void restoreTabState() {
        View fragmentContainer = mActivity.findViewById(R.id.fragment_container);
        boolean tabletLayout = fragmentContainer == null;
        if (!tabletLayout) {
            mListTabListener.fragment = mActivity.getFragmentManager()
                    .findFragmentByTag(StatusListFragment.class.getName());
            mMapTabListener.fragment = mActivity.getFragmentManager()
                    .findFragmentByTag(MapFragment.class.getName());

            SharedPreferences pref = mActivity.getPreferences(Activity.MODE_PRIVATE);
            int actionBarIndex = pref.getInt(mActivity.KEY_ACTION_BAR_INDEX, 0);
            mActionBar.setSelectedNavigationItem(actionBarIndex);
        }
    }

    private class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment fragment;
        private Activity activity;
        private Class<T> fragmentClass;
        private int fragmentContainer;

        public TabListener(Activity activity, Class<T> fragmentClass, int fragmentContainer) {
            this.activity = activity;
            this.fragmentClass = fragmentClass;
            this.fragmentContainer = fragmentContainer;
        }

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction transaction) {

            if (fragment == null) {
                String fragmentName = fragmentClass.getName();
                fragment = Fragment.instantiate(activity, fragmentName);
                transaction.add(fragmentContainer, fragment, fragmentName);
            } else {
                transaction.attach(fragment);
            }
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction transaction) {
            if (fragment != null) {
                transaction.detach(fragment);
            }
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction transaction) {
            if (fragment != null) {
                transaction.attach(fragment);
            }
        }
    }


}
