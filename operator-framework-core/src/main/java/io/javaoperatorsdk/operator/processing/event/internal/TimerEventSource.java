package io.javaoperatorsdk.operator.processing.event.internal;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerEventSource extends AbstractEventSource {

  private Logger log = LoggerFactory.getLogger(TimerEventSource.class);

  private final Timer timer = new Timer();

  private final Map<String, EventProducerTimeTask> onceTasks = new ConcurrentHashMap<>();
  private final Map<String, EventProducerTimeTask> timerTasks = new ConcurrentHashMap<>();

  public void schedule(CustomResource customResource, long delay, long period) {
    String resourceUid = KubernetesResourceUtils.getUID(customResource);
    if (timerTasks.containsKey(resourceUid)) {
      return;
    }
    EventProducerTimeTask task = new EventProducerTimeTask(resourceUid);
    timerTasks.put(resourceUid, task);
    timer.schedule(task, delay, period);
  }

  public void scheduleOnce(CustomResource customResource, long delay) {
    String resourceUid = KubernetesResourceUtils.getUID(customResource);
    if (onceTasks.containsKey(resourceUid)) {
      cancelOnceSchedule(resourceUid);
    }
    EventProducerTimeTask task = new EventProducerTimeTask(resourceUid);
    onceTasks.put(resourceUid, task);
    timer.schedule(task, delay);
  }

  @Override
  public void eventSourceDeRegisteredForResource(String customResourceUid) {
    cancelSchedule(customResourceUid);
    cancelOnceSchedule(customResourceUid);
  }

  public void cancelSchedule(String customResourceUid) {
    TimerTask timerTask = timerTasks.remove(customResourceUid);
    if (timerTask != null) {
      timerTask.cancel();
    }
  }

  public void cancelOnceSchedule(String customResourceUid) {
    TimerTask timerTask = onceTasks.remove(customResourceUid);
    if (timerTask != null) {
      timerTask.cancel();
    }
  }

  public class EventProducerTimeTask extends TimerTask {

    protected final String customResourceUid;

    public EventProducerTimeTask(String customResourceUid) {
      this.customResourceUid = customResourceUid;
    }

    @Override
    public void run() {
      log.debug("Producing event for custom resource id: {}", customResourceUid);
      eventHandler.handleEvent(new TimerEvent(customResourceUid, TimerEventSource.this));
    }
  }
}
