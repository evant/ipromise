package me.tatarka.ipromise.android;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import static me.tatarka.ipromise.android.AsyncManager.FRAGMENT_TAG;

public final class AsyncManagerCompat {
    private AsyncManagerCompat() {

    }

    /**
     * Get an instance of {@code AsyncManager} that is tied to the lifecycle of the given {@link
     * android.support.v4.app.FragmentActivity}.
     *
     * @param activity the activity
     * @return the {@code AsyncManager}
     */
    public static AsyncManager get(FragmentActivity activity) {
        AsyncManagerSupportFragment fragment = (AsyncManagerSupportFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new AsyncManagerSupportFragment();
            activity.getSupportFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(fragment);
    }

    /**
     * Get an instance of {@code AsyncManager} that is tied to the lifecycle of the given {@link
     * android.support.v4.app.Fragment}.
     *
     * @param fragment the fragment
     * @return the {@code AsyncManager}
     */
    public static AsyncManager get(Fragment fragment) {
        AsyncManagerSupportFragment manager = (AsyncManagerSupportFragment) fragment.getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (manager == null) {
            manager = new AsyncManagerSupportFragment();
            fragment.getChildFragmentManager().beginTransaction().add(fragment, FRAGMENT_TAG).commit();
        }
        return new AsyncManager(manager);
    }

}
