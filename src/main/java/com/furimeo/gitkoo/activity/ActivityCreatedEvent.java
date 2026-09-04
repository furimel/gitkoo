package com.furimeo.gitkoo.activity;

import org.springframework.context.ApplicationEvent;

/**
 * Published after an {@link Activity} is recorded, so other domains (e.g. notifications)
 * can react without being wired into every call site (, §116).
 */
public class ActivityCreatedEvent extends ApplicationEvent {

    private final Activity activity;

    public ActivityCreatedEvent(Object source, Activity activity) {
        super(source);
        this.activity = activity;
    }

    public Activity getActivity() {
        return activity;
    }
}
