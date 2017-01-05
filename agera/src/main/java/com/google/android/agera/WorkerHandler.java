package com.google.android.agera;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import java.lang.ref.WeakReference;

/**
 * Shared per-thread worker Handler behind internal logic of various Agera classes.
 */
final class WorkerHandler extends Handler {
  static final int MSG_FIRST_ADDED = 0;
  static final int MSG_LAST_REMOVED = 1;
  static final int MSG_UPDATE = 2;
  static final int MSG_CALL_UPDATABLE = 3;
  static final int MSG_CALL_MAYBE_START_FLOW = 4;
  static final int MSG_CALL_ACKNOWLEDGE_CANCEL = 5;
  private static final ThreadLocal<WeakReference<WorkerHandler>> handlers = new ThreadLocal<>();
  @NonNull
  private final IdentityMultimap<Updatable, Object> scheduledUpdatables;

  @NonNull
  static WorkerHandler workerHandler() {
    final WeakReference<WorkerHandler> handlerReference = handlers.get();
    WorkerHandler handler = handlerReference != null ? handlerReference.get() : null;
    if (handler == null) {
      handler = new WorkerHandler();
      handlers.set(new WeakReference<>(handler));
    }
    return handler;
  }

  private WorkerHandler() {
    this.scheduledUpdatables = new IdentityMultimap<>();
  }

  synchronized void removeUpdatable(@NonNull final Updatable updatable,
      @NonNull final Object token) {
    scheduledUpdatables.removeKeyValuePair(updatable, token);
  }

  synchronized void update(@NonNull final Updatable updatable, @NonNull final Object token) {
    if (scheduledUpdatables.addKeyValuePair(updatable, token)) {
      obtainMessage(WorkerHandler.MSG_CALL_UPDATABLE, updatable).sendToTarget();
    }
  }

  @Override
  public void handleMessage(final Message message) {
    switch (message.what) {
      case MSG_UPDATE:
        ((BaseObservable) message.obj).sendUpdate();
        break;
      case MSG_FIRST_ADDED:
        ((BaseObservable) message.obj).observableActivated();
        break;
      case MSG_LAST_REMOVED:
        ((BaseObservable) message.obj).observableDeactivated();
        break;
      case MSG_CALL_UPDATABLE:
        final Updatable updatable = (Updatable) message.obj;
        if (scheduledUpdatables.removeKey(updatable)) {
          updatable.update();
        }
        break;
      case MSG_CALL_MAYBE_START_FLOW:
        ((CompiledRepository) message.obj).maybeStartFlow();
        break;
      case MSG_CALL_ACKNOWLEDGE_CANCEL:
        ((CompiledRepository) message.obj).acknowledgeCancel();
        break;
      default:
    }
  }
}
